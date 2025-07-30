package EventTests.EventSubscriberTests;

import EventTests.TestsEvents.DoubleEventTest;
import EventTests.TestsEvents.IntEventTest;
import EventTests.TestsEvents.StringEventTest;
import com.jopencl.Event.EventManager;
import com.jopencl.Event.EventPriority;
import com.jopencl.Event.EventSubscribers.SyncEventSubscriber;
import com.jopencl.Event.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class SyncEventSubscriberTest {
    private SyncEventSubscriber subscriber;
    private final EventManager eventManager = EventManager.getInstance();

    @BeforeEach
    void setUp() {
        subscriber = new SyncEventSubscriber();
    }

    @Test
    void testInitialStatus() {
        assertEquals(Status.CREATED, subscriber.getStatus());
    }

    @Test
    void testStatusTransitions() {
        // CREATED -> RUNNING
        subscriber.run();
        assertEquals(Status.RUNNING, subscriber.getStatus());

        // RUNNING -> PAUSED
        subscriber.pause();
        assertEquals(Status.PAUSED, subscriber.getStatus());

        // PAUSED -> RUNNING
        subscriber.run();
        assertEquals(Status.RUNNING, subscriber.getStatus());

        // RUNNING -> STOPPED
        subscriber.stop();
        assertEquals(Status.STOPPED, subscriber.getStatus());

        // STOPPED -> RUNNING
        subscriber.run();
        assertEquals(Status.RUNNING, subscriber.getStatus());
    }

    @Test
    void testOperationsAfterShutdown() {
        subscriber.shutdown();

        assertThrows(IllegalStateException.class, () -> subscriber.run());
        assertThrows(IllegalStateException.class, () -> subscriber.pause());
        assertThrows(IllegalStateException.class, () -> subscriber.stop());
        assertThrows(IllegalStateException.class, () -> subscriber.shutdown());
    }

    @Test
    void subscribeEventTest () {
        StringEventTest SET = new StringEventTest("test");
        IntEventTest IET = new IntEventTest(0);
        DoubleEventTest DET = new DoubleEventTest(0.);

        final Integer[] count = {0};

        subscriber.onEvent(SET);
        subscriber.onEvent(IET);
        subscriber.onEvent(DET);
        subscriber.processEvents();
        assertEquals(count[0], 0);

        subscriber.subscribeEvent(StringEventTest.class, (StringEventTest eventTest) -> count[0]++);
        subscriber.subscribeEvent(IntEventTest.class, (IntEventTest eventTest) -> count[0]++);
        subscriber.subscribeEvent(DoubleEventTest.class, (DoubleEventTest eventTest) -> count[0]++);

        subscriber.onEvent(SET);
        subscriber.onEvent(IET);
        subscriber.onEvent(DET);
        assertEquals(count[0], 0);
        subscriber.processEvents();
        assertEquals(count[0], 3);

        subscriber.unsubscribeEvent(StringEventTest.class, DoubleEventTest.class);
        subscriber.onEvent(SET);
        subscriber.onEvent(IET);
        subscriber.onEvent(DET);
        assertEquals(count[0], 3);
        subscriber.processEvents();
        assertEquals(count[0], 4);
    }

    @Test
    void testPauseResumePreservesQueue() {
        StringEventTest event = new StringEventTest("test");
        final AtomicInteger count = new AtomicInteger(0);

        subscriber.subscribeEvent(StringEventTest.class, e -> count.incrementAndGet());
        subscriber.run();

        subscriber.onEvent(event);
        subscriber.onEvent(event);

        subscriber.pause();
        assertEquals(Status.PAUSED, subscriber.getStatus());

        subscriber.run();
        subscriber.onEvent(event);
        subscriber.processEvents();

        assertEquals(3, count.get());
    }

    @Test
    void testStopClearsState() {
        StringEventTest event = new StringEventTest("test");
        final AtomicInteger count = new AtomicInteger(0);

        subscriber.subscribeEvent(StringEventTest.class, e -> count.incrementAndGet());
        subscriber.run();

        subscriber.subscribeEvent(IntEventTest.class, e -> {
            throw new RuntimeException("Test error");
        });
        subscriber.onEvent(new IntEventTest(1));
        subscriber.processEvents();
        assertTrue(subscriber.getTotalErrorCount() > 0);

        subscriber.onEvent(event);
        subscriber.onEvent(event);

        subscriber.stop();
        assertEquals(Status.STOPPED, subscriber.getStatus());
        assertEquals(0, subscriber.getTotalErrorCount());

        subscriber.run();
        subscriber.processEvents();
        assertEquals(0, count.get());
    }

    @Test
    void subscribersTest () {
        assertEquals(Status.CREATED, subscriber.getStatus());
        assertEquals(eventManager.getSubscriberCount(), 0);

        subscriber.run();
        assertEquals(eventManager.getSubscriberCount(), 1);

        SyncEventSubscriber anotherSubscriber = new SyncEventSubscriber(true);
        assertEquals(eventManager.getSubscriberCount(), 2);

        subscriber.shutdown();
        anotherSubscriber.shutdown();
        assertEquals(eventManager.getSubscriberCount(), 0);
    }

    @Test
    void testRunMultipleTimes() {
        assertDoesNotThrow(() -> {
            subscriber.run();
            subscriber.run();
            subscriber.run();
        });

        assertEquals(Status.RUNNING, subscriber.getStatus());
        assertEquals(1, eventManager.getSubscriberCount());
    }

    @Test
    void testPauseMultipleTimes() {
        subscriber.run();

        assertDoesNotThrow(() -> {
            subscriber.pause();
            subscriber.pause();
            subscriber.pause();
        });

        assertEquals(Status.PAUSED, subscriber.getStatus());
        assertEquals(0, eventManager.getSubscriberCount());
    }

    @Test
    void testStopMultipleTimes() {
        subscriber.run();

        assertDoesNotThrow(() -> {
            subscriber.stop();
            subscriber.stop();
            subscriber.stop();
        });

        assertEquals(Status.STOPPED, subscriber.getStatus());
        assertEquals(0, eventManager.getSubscriberCount());
    }

    @Test
    void testProcessEventsWithHandlers() {
        StringEventTest SET = new StringEventTest("test text");
        IntEventTest IET = new IntEventTest(450);
        DoubleEventTest DET = new DoubleEventTest(0.015);

        subscriber.subscribeEvent(StringEventTest.class, (StringEventTest eventTest) -> assertEquals("test text", eventTest.getData()));
        subscriber.subscribeEvent(IntEventTest.class, (IntEventTest eventTest) -> assertEquals(450, eventTest.getData()));
        subscriber.subscribeEvent(DoubleEventTest.class, (DoubleEventTest eventTest) -> assertEquals(0.015, eventTest.getData()));

        subscriber.onEvent(SET);
        subscriber.onEvent(IET);
        subscriber.onEvent(DET);
        subscriber.processEvents();
    }

    @Test
    void testProcessEventsMultipleSameType() {
        IntEventTest IET = new IntEventTest(0);

        int num = 100;
        final Integer[] count = {0};

        subscriber.subscribeEvent(IntEventTest.class, (IntEventTest eventTest) -> count[0]++);

        for (int i = 0; i < num; i++) {
            subscriber.onEvent(IET);
        }

        assertEquals(count[0], 0);
        subscriber.processEvents();
        assertEquals(count[0], num);
    }

    @Test
    void testProcessEventsInPriorityOrder() {
        StringBuilder processOrder = new StringBuilder();

        subscriber.subscribeEvent(StringEventTest.class, (StringEventTest eventTest) ->
                processOrder.append(eventTest.getData()));

        subscriber.onEvent(new StringEventTest("L", EventPriority.LOW));
        subscriber.onEvent(new StringEventTest("H", EventPriority.HIGH));
        subscriber.onEvent(new StringEventTest("M", EventPriority.MEDIUM));

        subscriber.processEvents();

        assertEquals("HML", processOrder.toString());
    }

    @Test
    void testProcessEventsEmptyQueue() {
        assertDoesNotThrow(() -> subscriber.processEvents());
    }

    @Test
    void testSubscribeEventWithErrorHandler() {
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final AtomicInteger errorHandlerCallCount = new AtomicInteger(0);
        final AtomicReference<Exception> caughtException = new AtomicReference<>();

        subscriber.subscribeEvent(StringEventTest.class,
                (StringEventTest event) -> {
                    handlerCallCount.incrementAndGet();
                    if ("throw".equals(event.getData())) {
                        throw new RuntimeException("Test exception");
                    }
                },
                (event, exception) -> {
                    errorHandlerCallCount.incrementAndGet();
                    caughtException.set(exception);
                }
        );

        subscriber.onEvent(new StringEventTest("success"));
        subscriber.processEvents();
        assertEquals(1, handlerCallCount.get());
        assertEquals(0, errorHandlerCallCount.get());

        subscriber.onEvent(new StringEventTest("throw"));
        subscriber.processEvents();
        assertEquals(2, handlerCallCount.get());
        assertEquals(1, errorHandlerCallCount.get());
        assertEquals(1, subscriber.getTotalErrorCount());
        assertNotNull(caughtException.get());
        assertEquals("Test exception", caughtException.get().getMessage());
    }

    @Test
    void testErrorStatisticsTracking() {
        subscriber.subscribeEvent(StringEventTest.class,
                (StringEventTest event) -> {
                    throw new RuntimeException("Error " + event.getData());
                },
                null
        );

        StringEventTest event1 = new StringEventTest("1");
        StringEventTest event2 = new StringEventTest("2");

        subscriber.onEvent(event1);
        subscriber.processEvents();
        assertEquals(1, subscriber.getTotalErrorCount());
        assertEquals("Error 1", subscriber.getLastException().getMessage());
        assertSame(event1, subscriber.getLastFailedEvent());

        subscriber.onEvent(event2);
        subscriber.processEvents();
        assertEquals(2, subscriber.getTotalErrorCount());
        assertEquals("Error 2", subscriber.getLastException().getMessage());
        assertSame(event2, subscriber.getLastFailedEvent());
    }

    @Test
    void testClearErrorStatistics() {
        subscriber.subscribeEvent(StringEventTest.class,
                (StringEventTest event) -> {
                    throw new RuntimeException("Test error");
                }
        );

        subscriber.onEvent(new StringEventTest("test"));
        subscriber.processEvents();

        assertEquals(1, subscriber.getTotalErrorCount());
        assertNotNull(subscriber.getLastException());
        assertNotNull(subscriber.getLastFailedEvent());

        subscriber.clearErrorStatistics();

        assertEquals(0, subscriber.getTotalErrorCount());
        assertNull(subscriber.getLastException());
        assertNull(subscriber.getLastFailedEvent());
    }

    @Test
    void testUnsubscribeEventRemovesErrorHandler() {
        final AtomicInteger errorCount = new AtomicInteger(0);

        subscriber.subscribeEvent(StringEventTest.class,
                (StringEventTest event) -> {
                    throw new RuntimeException("Test error");
                },
                (event, exception) -> errorCount.incrementAndGet()
        );

        subscriber.onEvent(new StringEventTest("test1"));
        subscriber.processEvents();
        assertEquals(1, errorCount.get());

        subscriber.unsubscribeEvent(StringEventTest.class);

        subscriber.onEvent(new StringEventTest("test2"));
        subscriber.processEvents();
        assertEquals(1, errorCount.get());
        assertEquals(1, subscriber.getTotalErrorCount());
    }

    @Test
    void testClearSubscribeEventsRemovesErrorHandlers() {
        final AtomicInteger errorCount = new AtomicInteger(0);

        subscriber.subscribeEvent(StringEventTest.class,
                (StringEventTest event) -> {
                    throw new RuntimeException("Test error");
                },
                (event, exception) -> errorCount.incrementAndGet()
        );

        subscriber.subscribeEvent(IntEventTest.class,
                (IntEventTest event) -> {
                    throw new RuntimeException("Int error");
                },
                (event, exception) -> errorCount.incrementAndGet()
        );

        subscriber.onEvent(new StringEventTest("test"));
        subscriber.onEvent(new IntEventTest(42));
        subscriber.processEvents();
        assertEquals(2, errorCount.get());

        subscriber.clearSubscribeEvents();

        subscriber.onEvent(new StringEventTest("test2"));
        subscriber.onEvent(new IntEventTest(43));
        subscriber.processEvents();
        assertEquals(2, errorCount.get());
        assertEquals(2, subscriber.getTotalErrorCount());
    }

    @AfterEach
    void tearDown() {
        if(Status.SHUTDOWN != subscriber.getStatus()) {
            subscriber.shutdown();
        }
    }
}
