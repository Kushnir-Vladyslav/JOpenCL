package EventTests.EventSubscriberTests;

import EventTests.TestsEvents.DoubleEventTest;
import EventTests.TestsEvents.IntEventTest;
import EventTests.TestsEvents.StringEventTest;
import com.jopencl.Event.EventManager;
import com.jopencl.Event.EventPriority;
import com.jopencl.Event.EventSubscribers.AsyncEventSubscriber;
import com.jopencl.Event.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AsyncEventSubscriberTests {
    private AsyncEventSubscriber subscriber;
    private final EventManager eventManager = EventManager.getInstance();

    @BeforeEach
    void setUp() {
        subscriber = new AsyncEventSubscriber();
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
    void subscribeEventTest () throws InterruptedException {
        StringEventTest SET = new StringEventTest("test");
        IntEventTest IET = new IntEventTest(0);
        DoubleEventTest DET = new DoubleEventTest(0.);

        final AtomicInteger count = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);

        subscriber.run();
        subscriber.onEvent(SET);
        subscriber.onEvent(IET);
        subscriber.onEvent(DET);
        Thread.sleep(100);
        assertEquals(count.get(), 0);

        subscriber.subscribeEvent(StringEventTest.class, (StringEventTest eventTest) -> {
            count.incrementAndGet();
            latch.countDown();
        });
        subscriber.subscribeEvent(IntEventTest.class, (IntEventTest eventTest) -> {
            count.incrementAndGet();
            latch.countDown();
        });
        subscriber.subscribeEvent(DoubleEventTest.class, (DoubleEventTest eventTest) -> {
            count.incrementAndGet();
            latch.countDown();
        });

        subscriber.onEvent(SET);
        subscriber.onEvent(IET);
        subscriber.onEvent(DET);
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(count.get(), 3);

        CountDownLatch latch2 = new CountDownLatch(1);

        subscriber.subscribeEvent(IntEventTest.class, (IntEventTest eventTest) -> {
            count.incrementAndGet();
            latch2.countDown();
        });
        subscriber.unsubscribeEvent(StringEventTest.class, DoubleEventTest.class);

        subscriber.onEvent(SET);
        subscriber.onEvent(IET);
        subscriber.onEvent(DET);
        assertTrue(latch2.await(2, TimeUnit.SECONDS));
        assertEquals(count.get(), 4);
    }

    @Test
    void testPauseResumePreservesQueue() throws InterruptedException {
        StringEventTest event = new StringEventTest("test");
        CountDownLatch latch = new CountDownLatch(3);

        subscriber.subscribeEvent(StringEventTest.class, e -> latch.countDown());
        subscriber.run();

        subscriber.onEvent(event);
        subscriber.onEvent(event);

        subscriber.pause();
        assertEquals(Status.PAUSED, subscriber.getStatus());

        subscriber.run();
        subscriber.onEvent(event);
        assertTrue(latch.await(2, TimeUnit.SECONDS));
    }

    @Test
    void testStopClearsState() throws InterruptedException {
        StringEventTest event = new StringEventTest("test");
        final AtomicInteger count = new AtomicInteger(0);

        subscriber.subscribeEvent(StringEventTest.class, e -> count.incrementAndGet());
        subscriber.run();

        subscriber.subscribeEvent(IntEventTest.class, e -> {
            throw new RuntimeException("Test error");
        });
        subscriber.onEvent(new IntEventTest(1));
        Thread.sleep(200);
        assertTrue(subscriber.getTotalErrorCount() > 0);

        subscriber.pause(); //queue filling simulation

        subscriber.subscribeEvent(StringEventTest.class, e -> count.incrementAndGet());

        subscriber.onEvent(event);
        subscriber.onEvent(event);

        subscriber.stop();
        assertEquals(Status.STOPPED, subscriber.getStatus());
        assertEquals(0, subscriber.getTotalErrorCount());

        subscriber.run();

        Thread.sleep(200);

        assertEquals(0, count.get());
    }

    @Test
    void subscribersTest () {
        assertEquals(Status.CREATED, subscriber.getStatus());
        assertEquals(eventManager.getSubscriberCount(), 0);

        subscriber.run();
        assertEquals(eventManager.getSubscriberCount(), 1);

        AsyncEventSubscriber anotherSubscriber = new AsyncEventSubscriber(true);
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
    void testProcessEventsWithHandlers() throws InterruptedException {
        StringEventTest SET = new StringEventTest("test text");
        IntEventTest IET = new IntEventTest(450);
        DoubleEventTest DET = new DoubleEventTest(0.015);

        CountDownLatch latch = new CountDownLatch(3);

        subscriber.subscribeEvent(StringEventTest.class, (StringEventTest eventTest) -> {
            assertEquals("test text", eventTest.getData());
            latch.countDown();
        });
        subscriber.subscribeEvent(IntEventTest.class, (IntEventTest eventTest) -> {
            assertEquals(450, eventTest.getData());
            latch.countDown();
        });
        subscriber.subscribeEvent(DoubleEventTest.class, (DoubleEventTest eventTest) -> {
            assertEquals(0.015, eventTest.getData());
            latch.countDown();
        });

        subscriber.run();
        subscriber.onEvent(SET);
        subscriber.onEvent(IET);
        subscriber.onEvent(DET);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
    }

    @Test
    void testProcessEventsMultipleSameType() throws InterruptedException {
        IntEventTest IET = new IntEventTest(0);

        int num = 100;
        final AtomicInteger count = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(num);

        subscriber.subscribeEvent(IntEventTest.class, (IntEventTest eventTest) -> {
            count.incrementAndGet();
            latch.countDown();
        });

        subscriber.run();

        for (int i = 0; i < num; i++) {
            subscriber.onEvent(IET);
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(num, count.get());
    }

    @Test
    void testProcessEventsInPriorityOrder() throws InterruptedException {
        StringBuilder processOrder = new StringBuilder();

        CountDownLatch latch = new CountDownLatch(3);

        subscriber.subscribeEvent(StringEventTest.class, (StringEventTest eventTest) ->{
                processOrder.append(eventTest.getData());
                latch.countDown();
        });

        subscriber.onEvent(new StringEventTest("L", EventPriority.LOW));
        subscriber.onEvent(new StringEventTest("H", EventPriority.HIGH));
        subscriber.onEvent(new StringEventTest("M", EventPriority.MEDIUM));

        subscriber.run();

        assertTrue(latch.await(2, TimeUnit.SECONDS));

        assertEquals("HML", processOrder.toString());
    }

    @Test
    void testAsyncProcessing() throws InterruptedException {
        final String currentThreadName = Thread.currentThread().getName();
        final StringBuilder processingThreadName = new StringBuilder();

        CountDownLatch latch = new CountDownLatch(1);

        subscriber.subscribeEvent(StringEventTest.class, (StringEventTest eventTest) ->{
            processingThreadName.append(Thread.currentThread().getName());
            latch.countDown();
        });

        subscriber.run();
        subscriber.onEvent(new StringEventTest("test"));

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotEquals(currentThreadName, processingThreadName.toString());
    }

    @Test
    void testStopInterruptsProcessing() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch stopLatch = new CountDownLatch(1);

        subscriber.subscribeEvent(StringEventTest.class, (StringEventTest eventTest) ->{
            startLatch.countDown();

            try {
                Thread.sleep(1000);
                stopLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        subscriber.onEvent(new StringEventTest("test"));
        subscriber.run();

        assertTrue(startLatch.await(1, TimeUnit.SECONDS));

        subscriber.shutdown();

        assertFalse(stopLatch.await(500, TimeUnit.MILLISECONDS));
        assertEquals(Status.SHUTDOWN, subscriber.getStatus());
    }

    @Test
    void testConstructorWithAutoRunFalse() {
        AsyncEventSubscriber testSubscriber = new AsyncEventSubscriber(false);

        assertEquals(Status.CREATED, testSubscriber.getStatus());
        assertEquals(0, eventManager.getSubscriberCount());

        testSubscriber.shutdown();
    }

    @Test
    void testConstructorWithAutoRunTrue() {
        AsyncEventSubscriber testSubscriber = new AsyncEventSubscriber(true);

        assertEquals(Status.RUNNING, testSubscriber.getStatus());
        assertEquals(1, eventManager.getSubscriberCount());

        testSubscriber.shutdown();
    }

    @Test
    void testDefaultConstructor() {
        assertEquals(Status.CREATED, subscriber.getStatus());
        assertEquals(0, eventManager.getSubscriberCount());
    }

    @Test
    void testSubscribeEventWithErrorHandler() throws InterruptedException {
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final AtomicInteger errorHandlerCallCount = new AtomicInteger(0);
        final AtomicReference<Exception> caughtException = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(2);

        subscriber.subscribeEvent(StringEventTest.class,
                (StringEventTest event) -> {
                    handlerCallCount.incrementAndGet();
                    latch.countDown();
                    if ("throw".equals(event.getData())) {
                        throw new RuntimeException("Async test exception");
                    }
                },
                (event, exception) -> {
                    errorHandlerCallCount.incrementAndGet();
                    caughtException.set(exception);
                }
        );

        subscriber.run();

        subscriber.onEvent(new StringEventTest("success"));
        subscriber.onEvent(new StringEventTest("throw"));

        assertTrue(latch.await(2, TimeUnit.SECONDS));

        Thread.sleep(100);

        assertEquals(2, handlerCallCount.get());
        assertEquals(1, errorHandlerCallCount.get());
        assertEquals(1, subscriber.getTotalErrorCount());
        assertEquals("Async test exception", caughtException.get().getMessage());
    }

    @Test
    void testAsyncErrorStatisticsTracking() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);

        subscriber.subscribeEvent(StringEventTest.class,
                (StringEventTest event) -> {
                    latch.countDown();
                    throw new RuntimeException("Async error " + event.getData());
                }
        );

        subscriber.run();

        StringEventTest event1 = new StringEventTest("1");
        StringEventTest event2 = new StringEventTest("2");

        subscriber.onEvent(event1);
        subscriber.onEvent(event2);

        assertTrue(latch.await(2, TimeUnit.SECONDS));

        Thread.sleep(50);

        assertEquals(2, subscriber.getTotalErrorCount());
        assertEquals("Async error 2", subscriber.getLastException().getMessage());
        assertSame(event2, subscriber.getLastFailedEvent());
    }

    @Test
    void testAsyncProcessingContinuesAfterError() throws InterruptedException {
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);

        subscriber.subscribeEvent(StringEventTest.class,
                (StringEventTest event) -> {
                    if ("throw".equals(event.getData())) {
                        latch.countDown();
                        throw new RuntimeException("Async processing error");
                    } else {
                        successCount.incrementAndGet();
                        latch.countDown();
                    }
                },
                (event, exception) -> errorCount.incrementAndGet()
        );

        subscriber.run();

        subscriber.onEvent(new StringEventTest("success1"));
        subscriber.onEvent(new StringEventTest("throw"));
        subscriber.onEvent(new StringEventTest("success2"));

        assertTrue(latch.await(3, TimeUnit.SECONDS));

        Thread.sleep(200);

        assertEquals(2, successCount.get());
        assertEquals(1, errorCount.get());
        assertEquals(1, subscriber.getTotalErrorCount());
    }

    @AfterEach
    void tearDown() {
        if(Status.SHUTDOWN != subscriber.getStatus()) {
            subscriber.shutdown();
        }
    }
}
