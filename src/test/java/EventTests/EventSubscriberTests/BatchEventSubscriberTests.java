package EventTests.EventSubscriberTests;

import EventTests.TestsEvents.IntEventTest;
import EventTests.TestsEvents.StringEventTest;
import com.jopencl.Event.EventManager;
import com.jopencl.Event.EventPriority;
import com.jopencl.Event.EventSubscribers.BatchEventSubscriber;
import com.jopencl.Event.Events.ListEvents;
import com.jopencl.Event.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class BatchEventSubscriberTests {
    private BatchEventSubscriber subscriber;
    private final EventManager eventManager = EventManager.getInstance();
    private final int batchSize = 3; // batch size of 3 for testing

    @BeforeEach
    void setUp() {
        subscriber = new BatchEventSubscriber(batchSize);
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
        assertThrows(IllegalStateException.class, () -> subscriber.flush());
    }

    @Test
    void testConstructorWithInvalidBatchSize() {
        assertThrows(RuntimeException.class, () -> new BatchEventSubscriber(-1));
        assertThrows(RuntimeException.class, () -> new BatchEventSubscriber(0));
    }

    @Test
    void testBatchSizeProcessing() throws InterruptedException {
        StringEventTest event1 = new StringEventTest("event1");
        StringEventTest event2 = new StringEventTest("event2");
        StringEventTest event3 = new StringEventTest("event3");
        StringEventTest event4 = new StringEventTest("event4");

        final AtomicInteger batchCount = new AtomicInteger(0);
        final AtomicInteger eventCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        subscriber.subscribeEvent(StringEventTest.class, (ListEvents<StringEventTest> events) -> {
            batchCount.incrementAndGet();
            eventCount.addAndGet(events.getData().size());
            latch.countDown();
        });

        subscriber.run();

        subscriber.onEvent(event1);
        subscriber.onEvent(event2);
        subscriber.onEvent(event3);

        assertTrue(latch.await(2, TimeUnit.SECONDS));

        assertEquals(1, batchCount.get());
        assertEquals(3, eventCount.get());

        subscriber.onEvent(event4);
        Thread.sleep(100);
        assertEquals(1, batchCount.get());
    }

    @Test
    void testMultipleEventTypesBatching() throws InterruptedException {
        StringEventTest event1 = new StringEventTest("event1");
        IntEventTest event2 = new IntEventTest(42);

        final AtomicInteger processedCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        subscriber.subscribeEvent(StringEventTest.class, (ListEvents<StringEventTest> events) -> {
            processedCount.addAndGet(events.getData().size());
            latch.countDown();
        });
        subscriber.subscribeEvent(IntEventTest.class, (ListEvents<IntEventTest> events) -> {
            processedCount.addAndGet(events.getData().size());
            latch.countDown();
        });

        subscriber.run();

        for (int i = 0; i < 3; i++) {
            subscriber.onEvent(event1);
            subscriber.onEvent(event2);
        }

        Thread.sleep(50);

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(6, processedCount.get());
    }

    @Test
    void testFlushFunctionality() throws InterruptedException {
        StringEventTest event1 = new StringEventTest("event1");
        IntEventTest event2 = new IntEventTest(42);

        final AtomicInteger processedCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        subscriber.subscribeEvent(StringEventTest.class, (ListEvents<StringEventTest> events) -> {
            processedCount.addAndGet(events.getData().size());
            latch.countDown();
        });
        subscriber.subscribeEvent(IntEventTest.class, (ListEvents<IntEventTest> events) -> {
            processedCount.addAndGet(events.getData().size());
            latch.countDown();
        });

        subscriber.run();

        subscriber.onEvent(event1);
        subscriber.onEvent(event2);

        Thread.sleep(50);

        subscriber.flush();

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(2, processedCount.get());
    }


    @Test
    void testPauseResumePreservesBatch() throws InterruptedException {
        StringEventTest event = new StringEventTest("test");
        final AtomicInteger count = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        subscriber.subscribeEvent(StringEventTest.class, (ListEvents<StringEventTest> events) -> {
            count.addAndGet(events.getData().size());
            latch.countDown();
        });
        subscriber.run();

        subscriber.onEvent(event);
        subscriber.onEvent(event);

        subscriber.pause();
        assertEquals(Status.PAUSED, subscriber.getStatus());

        subscriber.run();
        subscriber.onEvent(event);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(3, count.get());
    }

    @Test
    void testStopClearsState() throws InterruptedException {
        StringEventTest event = new StringEventTest("test");
        final AtomicInteger count = new AtomicInteger(0);

        subscriber.subscribeEvent(StringEventTest.class, (ListEvents<StringEventTest> events) -> {
            count.addAndGet(events.getData().size());
        });
        subscriber.run();

        subscriber.subscribeEvent(IntEventTest.class, (ListEvents<IntEventTest> events) -> {
            throw new RuntimeException("Test error");
        });
        subscriber.onEvent(new IntEventTest(1));
        Thread.sleep(50);
        subscriber.flush();
        Thread.sleep(50);
        assertTrue(subscriber.getTotalErrorCount() > 0);

        subscriber.onEvent(event);
        subscriber.onEvent(event);

        subscriber.stop();
        assertEquals(Status.STOPPED, subscriber.getStatus());
        assertEquals(0, subscriber.getTotalErrorCount());

        subscriber.run();
        Thread.sleep(150);
        assertEquals(0, count.get());
    }

    @Test
    void subscribersTest() {
        assertEquals(Status.CREATED, subscriber.getStatus());
        assertEquals(0, eventManager.getSubscriberCount());

        subscriber.run();
        assertEquals(1, eventManager.getSubscriberCount());

        BatchEventSubscriber anotherSubscriber = new BatchEventSubscriber(5, true);
        assertEquals(2, eventManager.getSubscriberCount());

        subscriber.shutdown();
        anotherSubscriber.shutdown();
        assertEquals(0, eventManager.getSubscriberCount());
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
    void testBatchContentVerification() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        subscriber.subscribeEvent(StringEventTest.class, (ListEvents<StringEventTest> batch) -> {
            List<StringEventTest> events = batch.getData();
            assertEquals(3, events.size());
            assertEquals("test0", events.get(0).getData());
            assertEquals("test1", events.get(1).getData());
            assertEquals("test2", events.get(2).getData());
            latch.countDown();
        });

        subscriber.run();

        for (int i = 0; i < 3; i++) {
            subscriber.onEvent(new StringEventTest("test" + i));
            Thread.sleep(50);
        }

        assertTrue(latch.await(2, TimeUnit.SECONDS));
    }

    @Test
    void testProcessEventsInPriorityOrder() throws InterruptedException {
        StringBuilder processOrder = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);

        subscriber.subscribeEvent(StringEventTest.class, (ListEvents<StringEventTest> events) -> {
            for (StringEventTest event : events.getData()) {
                processOrder.append(event.getData());
            }
            latch.countDown();
        });

        subscriber.onEvent(new StringEventTest("L", EventPriority.LOW));
        subscriber.onEvent(new StringEventTest("H", EventPriority.HIGH));
        subscriber.onEvent(new StringEventTest("M", EventPriority.MEDIUM));

        subscriber.run();

        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
        assertEquals("HML", processOrder.toString());
    }

    @Test
    void testAsyncBatchProcessing() throws InterruptedException {
        final String currentThreadName = Thread.currentThread().getName();
        final StringBuilder processingThreadName = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);

        subscriber.subscribeEvent(StringEventTest.class, (ListEvents<StringEventTest> batch) -> {
            processingThreadName.append(Thread.currentThread().getName());
            latch.countDown();
        });

        subscriber.run();

        for (int i = 0; i < 3; i++) {
            subscriber.onEvent(new StringEventTest("test" + i));
        }

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotEquals(currentThreadName, processingThreadName.toString());
    }

    @Test
    void testConstructorWithAutoRunFalse() {
        BatchEventSubscriber testSubscriber = new BatchEventSubscriber(5, false);

        assertEquals(Status.CREATED, testSubscriber.getStatus());
        assertEquals(0, eventManager.getSubscriberCount());

        testSubscriber.shutdown();
    }

    @Test
    void testConstructorWithAutoRunTrue() {
        BatchEventSubscriber testSubscriber = new BatchEventSubscriber(5, true);

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
    void testBatchWithErrorHandler() throws InterruptedException {
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final AtomicInteger errorHandlerCallCount = new AtomicInteger(0);
        final AtomicReference<Exception> caughtException = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(2);

        subscriber.subscribeEvent(StringEventTest.class,
                (ListEvents<StringEventTest> batch) -> {
                    handlerCallCount.incrementAndGet();
                    latch.countDown();
                    if (batch.getData().get(0).getData().equals("throw")) {
                        throw new RuntimeException("Batch test exception");
                    }
                },
                (event, exception) -> {
                    errorHandlerCallCount.incrementAndGet();
                    caughtException.set(exception);
                }
        );

        subscriber.run();

        for (int i = 0; i < 3; i++) {
            subscriber.onEvent(new StringEventTest("success" + i));
        }

        for (int i = 0; i < 3; i++) {
            subscriber.onEvent(new StringEventTest(i == 0 ? "throw" : "test" + i));
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        Thread.sleep(100);

        assertEquals(2, handlerCallCount.get());
        assertEquals(1, errorHandlerCallCount.get());
        assertEquals(1, subscriber.getTotalErrorCount());
        assertEquals("Batch test exception", caughtException.get().getMessage());
    }

    @Test
    void testBatchErrorStatisticsTracking() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);

        subscriber.subscribeEvent(StringEventTest.class,
                (ListEvents<StringEventTest> batch) -> {
                    latch.countDown();
                    throw new RuntimeException("Batch error " + batch.getData().get(0).getData());
                }
        );

        subscriber.run();

        for (int i = 0; i < 3; i++) {
            subscriber.onEvent(new StringEventTest("batch1_" + i));
        }

        for (int i = 0; i < 3; i++) {
            subscriber.onEvent(new StringEventTest("batch2_" + i));
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS));

        Thread.sleep(50);

        assertEquals(2, subscriber.getTotalErrorCount());
        assertTrue(subscriber.getLastException().getMessage().contains("Batch error"));
        assertNotNull(subscriber.getLastFailedEvent());
    }

    @Test
    void testBatchProcessingContinuesAfterError() throws InterruptedException {
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);

        subscriber.subscribeEvent(StringEventTest.class,
                (ListEvents<StringEventTest> batch) -> {
                    if (batch.getData().get(0).getData().equals("throw")) {
                        latch.countDown();
                        throw new RuntimeException("Batch processing error");
                    } else {
                        successCount.incrementAndGet();
                        latch.countDown();
                    }
                },
                (event, exception) -> errorCount.incrementAndGet()
        );

        subscriber.run();

        for (int i = 0; i < 3; i++) {
            subscriber.onEvent(new StringEventTest("success1_" + i));
            Thread.sleep(50);
        }

        for (int i = 0; i < 3; i++) {
            subscriber.onEvent(new StringEventTest(i == 0 ? "throw" : "error_" + i));
            Thread.sleep(50);
        }

        for (int i = 0; i < 3; i++) {
            subscriber.onEvent(new StringEventTest("success2_" + i));
            Thread.sleep(50);
        }

        assertTrue(latch.await(4, TimeUnit.SECONDS));
        Thread.sleep(200);

        assertEquals(2, successCount.get());
        assertEquals(1, errorCount.get());
        assertEquals(1, subscriber.getTotalErrorCount());
    }

    @Test
    void testFlushEmptyBatches() {
        subscriber.run();

        assertDoesNotThrow(() -> subscriber.flush());
    }

    @Test
    void testUnsubscribeEventRemovesErrorHandler() throws InterruptedException {
        final AtomicInteger errorCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        subscriber.subscribeEvent(StringEventTest.class,
                (ListEvents<StringEventTest> batch) -> {
                    latch.countDown();
                    throw new RuntimeException("Test error");
                },
                (event, exception) -> errorCount.incrementAndGet()
        );

        subscriber.run();

        for (int i = 0; i < 3; i++) {
            subscriber.onEvent(new StringEventTest("test" + i));
        }

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        Thread.sleep(100);
        assertEquals(1, errorCount.get());

        subscriber.unsubscribeEvent(StringEventTest.class);

        for (int i = 0; i < 3; i++) {
            subscriber.onEvent(new StringEventTest("test" + i));
        }

        Thread.sleep(200);
        assertEquals(1, errorCount.get());
    }

    @Test
    void testClearSubscribeEventsRemovesErrorHandlers() throws InterruptedException {
        final AtomicInteger errorCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        subscriber.subscribeEvent(StringEventTest.class,
                (ListEvents<StringEventTest> batch) -> {
                    latch.countDown();
                    throw new RuntimeException("Test error");
                },
                (event, exception) -> errorCount.incrementAndGet()
        );

        subscriber.run();

        for (int i = 0; i < 3; i++) {
            subscriber.onEvent(new StringEventTest("test" + i));
        }

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        Thread.sleep(100);
        assertEquals(1, errorCount.get());

        subscriber.clearSubscribeEvents();

        for (int i = 0; i < 3; i++) {
            subscriber.onEvent(new StringEventTest("test" + i));
        }

        Thread.sleep(200);
        assertEquals(1, errorCount.get());
    }

    @AfterEach
    void tearDown() {
        if(Status.SHUTDOWN != subscriber.getStatus()) {
            subscriber.shutdown();
        }
    }
}
