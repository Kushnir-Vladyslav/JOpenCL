package EventTests.EventPublisherTests;

import EventTests.TestsEvents.DoubleEventTest;
import EventTests.TestsEvents.IntEventTest;
import EventTests.TestsEvents.StringEventTest;
import com.jopencl.Event.EventManager;
import com.jopencl.Event.EventPriority;
import com.jopencl.Event.EventPublishers.PeriodicEventPublisher;
import com.jopencl.Event.EventSubscribers.AsyncEventSubscriber;
import com.jopencl.Event.EventSubscribers.SyncEventSubscriber;
import com.jopencl.Event.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class PeriodicEventPublisherTest {
    private PeriodicEventPublisher publisher;
    private AsyncEventSubscriber subscriber;
    private final EventManager eventManager = EventManager.getInstance();

    @BeforeEach
    void setUp() {
        publisher = new PeriodicEventPublisher();
        subscriber = new AsyncEventSubscriber();
    }

    @Test
    void testDefaultConstructor() {
        PeriodicEventPublisher defaultPublisher = new PeriodicEventPublisher();
        assertEquals(Status.RUNNING, defaultPublisher.getStatus());
        defaultPublisher.shutdown();
    }

    @Test
    void testConstructorWithTimeUnit() {
        PeriodicEventPublisher secondsPublisher = new PeriodicEventPublisher(TimeUnit.SECONDS);
        assertEquals(Status.RUNNING, secondsPublisher.getStatus());
        secondsPublisher.shutdown();
    }

    @Test
    void testConstructorValidation() {
        assertThrows(IllegalArgumentException.class,
                () -> new PeriodicEventPublisher(null));
    }

    @Test
    void testPublisherInitialStatus() {
        assertEquals(Status.RUNNING, publisher.getStatus());
    }

    @Test
    void testPublishPeriodicEvent() throws InterruptedException {
        final AtomicInteger handlerCount = new AtomicInteger(0);
        final AtomicReference<String> receivedData = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(3);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            handlerCount.incrementAndGet();
            receivedData.set(e.getData());
            latch.countDown();
        });
        subscriber.run();

        StringEventTest event = new StringEventTest("periodic message");
        ScheduledFuture<?> future = publisher.publish(event, "test-task", 100, TimeUnit.MILLISECONDS);

        assertNotNull(future);
        assertFalse(future.isDone());

        assertTrue(latch.await(1, TimeUnit.SECONDS));

        future.cancel(false);

        assertEquals(3, handlerCount.get());
        assertEquals("periodic message", receivedData.get());
    }

    @Test
    void testPublishWithDefaultTimeUnit() throws InterruptedException {
        final AtomicInteger handlerCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            handlerCount.incrementAndGet();
            latch.countDown();
        });
        subscriber.run();

        ScheduledFuture<?> future = publisher.publish(
                new StringEventTest("default timeunit"), "default-task", 150);

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        future.cancel(false);

        assertTrue(handlerCount.get() == 2);
    }

    @Test
    void testPublishWithDifferentTimeUnits() throws InterruptedException {
        PeriodicEventPublisher secondsPublisher = new PeriodicEventPublisher(TimeUnit.SECONDS);

        try {
            final AtomicInteger handlerCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(2);

            subscriber.subscribeEvent(StringEventTest.class, e -> {
                handlerCount.incrementAndGet();
                latch.countDown();
            });
            subscriber.run();

            ScheduledFuture<?> future = secondsPublisher.publish(
                    new StringEventTest("seconds test"), "seconds-task", 1);

            assertTrue(latch.await(3, TimeUnit.SECONDS));
            future.cancel(false);

            assertEquals(2, handlerCount.get());
        } finally {
            secondsPublisher.shutdown();
        }
    }

    @Test
    void testMultiplePeriodicTasks() throws InterruptedException {
        final AtomicInteger task1Count = new AtomicInteger(0);
        final AtomicInteger task2Count = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(6);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            if ("task1".equals(e.getData())) {
                task1Count.incrementAndGet();
            } else if ("task2".equals(e.getData())) {
                task2Count.incrementAndGet();
            }
            latch.countDown();
        });
        subscriber.run();

        ScheduledFuture<?> future1 = publisher.publish(
                new StringEventTest("task1"), "task1", 150, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> future2 = publisher.publish(
                new StringEventTest("task2"), "task2", 100, TimeUnit.MILLISECONDS);

        assertTrue(latch.await(2, TimeUnit.SECONDS));

        future1.cancel(false);
        future2.cancel(false);

        assertTrue(task1Count.get() >= 3);
        assertTrue(task2Count.get() >= 3);
    }

    @Test
    void testPublishValidation() {
        assertThrows(IllegalArgumentException.class,
                () -> publisher.publish(null, "test", 100, TimeUnit.MILLISECONDS));

        assertThrows(IllegalArgumentException.class,
                () -> publisher.publish(new StringEventTest("test"), null, 100, TimeUnit.MILLISECONDS));

        assertThrows(IllegalArgumentException.class,
                () -> publisher.publish(new StringEventTest("test"), "test", -1, TimeUnit.MILLISECONDS));

        assertThrows(IllegalArgumentException.class,
                () -> publisher.publish(new StringEventTest("test"), "test", 100, null));

        assertThrows(IllegalArgumentException.class,
                () -> publisher.publish(null, "test", 100));

        assertThrows(IllegalArgumentException.class,
                () -> publisher.publish(new StringEventTest("test"), null, 100));

        assertThrows(IllegalArgumentException.class,
                () -> publisher.publish(new StringEventTest("test"), "test", -1));
    }

    @Test
    void testReplaceExistingTask() throws InterruptedException {
        final AtomicInteger handlerCount = new AtomicInteger(0);
        final AtomicReference<String> lastMessage = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(4);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            handlerCount.incrementAndGet();
            lastMessage.set(e.getData());
            latch.countDown();
        });
        subscriber.run();

        ScheduledFuture<?> future1 = publisher.publish(
                new StringEventTest("first"), "same-id", 100, TimeUnit.MILLISECONDS);

        Thread.sleep(250);

        ScheduledFuture<?> future2 = publisher.publish(
                new StringEventTest("second"), "same-id", 100, TimeUnit.MILLISECONDS);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        future2.cancel(false);

        assertEquals("second", lastMessage.get());
        assertTrue(future1.isCancelled() || future1.isDone());
        assertNotSame(future1, future2);
    }

    @Test
    void testCancelPeriodicTask() throws InterruptedException {
        final AtomicInteger handlerCount = new AtomicInteger(0);
        CountDownLatch initialLatch = new CountDownLatch(2);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            handlerCount.incrementAndGet();
            initialLatch.countDown();
        });
        subscriber.run();

        ScheduledFuture<?> future = publisher.publish(
                new StringEventTest("cancellable"), "cancel-test", 100, TimeUnit.MILLISECONDS);

        assertTrue(initialLatch.await(1, TimeUnit.SECONDS));

        int countBeforeCancel = handlerCount.get();
        assertTrue(countBeforeCancel >= 2);

        publisher.cancel("cancel-test");
        assertTrue(future.isCancelled());

        Thread.sleep(300);
        assertEquals(countBeforeCancel, handlerCount.get());
    }

    @Test
    void testCancelNonExistentTask() {
        assertDoesNotThrow(() -> publisher.cancel("non-existent"));
    }

    @Test
    void testCancelValidation() {
        assertThrows(IllegalArgumentException.class, () -> publisher.cancel(null));
    }

    @Test
    void testPublishAfterShutdown() {
        publisher.shutdown();
        assertEquals(Status.SHUTDOWN, publisher.getStatus());

        assertThrows(IllegalStateException.class,
                () -> publisher.publish(new StringEventTest("after shutdown"), "test", 100, TimeUnit.MILLISECONDS));

        assertThrows(IllegalStateException.class,
                () -> publisher.publish(new StringEventTest("after shutdown"), "test", 100));
    }

    @Test
    void testCancelAfterShutdown() {
        publisher.shutdown();

        assertThrows(IllegalStateException.class, () -> publisher.cancel("test"));
    }

    @Test
    void testPublisherShutdown() {
        assertEquals(Status.RUNNING, publisher.getStatus());

        assertDoesNotThrow(() -> publisher.shutdown());
        assertEquals(Status.SHUTDOWN, publisher.getStatus());

        assertThrows(IllegalStateException.class, () -> publisher.shutdown());
    }

    @Test
    void testShutdownCancelsAllTasks() throws InterruptedException {
        final AtomicInteger handlerCount = new AtomicInteger(0);

        subscriber.subscribeEvent(StringEventTest.class, e -> handlerCount.incrementAndGet());
        subscriber.run();

        ScheduledFuture<?> future1 = publisher.publish(
                new StringEventTest("task1"), "task1", 100, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> future2 = publisher.publish(
                new StringEventTest("task2"), "task2", 100, TimeUnit.MILLISECONDS);

        Thread.sleep(150);
        int countBeforeShutdown = handlerCount.get();

        publisher.shutdown();

        assertTrue(future1.isCancelled() || future1.isDone());
        assertTrue(future2.isCancelled() || future2.isDone());

        Thread.sleep(300);
        assertEquals(countBeforeShutdown, handlerCount.get());
    }

    @Test
    void testEventIntegrity() throws InterruptedException {
        final List<StringEventTest> receivedEvents = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(3);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            receivedEvents.add(e);
            latch.countDown();
        });
        subscriber.run();

        StringEventTest originalEvent = new StringEventTest("integrity test");
        ScheduledFuture<?> future = publisher.publish(originalEvent, "integrity", 100, TimeUnit.MILLISECONDS);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        future.cancel(false);

        assertTrue(receivedEvents.size() >= 3);

        for (StringEventTest receivedEvent : receivedEvents) {
            assertEquals(originalEvent.getData(), receivedEvent.getData());
            assertEquals(originalEvent.getPriority(), receivedEvent.getPriority());
            assertEquals(originalEvent.getCreatedTimeMillis(), receivedEvent.getCreatedTimeMillis());
        }
    }

    @Test
    void testPeriodicAccuracy() throws InterruptedException {
        final List<Long> executionTimes = Collections.synchronizedList(new ArrayList<>());
        final long period = 200;
        CountDownLatch latch = new CountDownLatch(3);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            executionTimes.add(System.currentTimeMillis());
            latch.countDown();
        });
        subscriber.run();

        long startTime = System.currentTimeMillis();
        ScheduledFuture<?> future = publisher.publish(
                new StringEventTest("timing test"), "timing", period, TimeUnit.MILLISECONDS);

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        future.cancel(false);

        assertEquals(3, executionTimes.size());

        assertTrue(executionTimes.get(0) - startTime < 100);

        for (int i = 1; i < executionTimes.size(); i++) {
            long interval = executionTimes.get(i) - executionTimes.get(i - 1);
            assertTrue(interval >= period - 50,
                    "Interval too short: " + interval + "ms, expected: " + period + "ms");
            assertTrue(interval <= period + 100,
                    "Interval too long: " + interval + "ms, expected: " + period + "ms");
        }
    }

    @Test
    void testZeroPeriod() {
        final AtomicInteger handlerCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(10);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            handlerCount.incrementAndGet();
            latch.countDown();
        });
        subscriber.run();

        assertThrows(IllegalArgumentException.class, () ->
                publisher.publish(new StringEventTest("immediate"), "immediate", 0, TimeUnit.MILLISECONDS));
    }

    @Test
    void testPublishDifferentEventTypes() throws InterruptedException {
        final AtomicInteger stringCount = new AtomicInteger(0);
        final AtomicInteger intCount = new AtomicInteger(0);
        final AtomicInteger doubleCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(6); // 2 executions for each type

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            stringCount.incrementAndGet();
            latch.countDown();
        });
        subscriber.subscribeEvent(IntEventTest.class, e -> {
            intCount.incrementAndGet();
            latch.countDown();
        });
        subscriber.subscribeEvent(DoubleEventTest.class, e -> {
            doubleCount.incrementAndGet();
            latch.countDown();
        });
        subscriber.run();

        ScheduledFuture<?> future1 = publisher.publish(
                new StringEventTest("test1"), "string-task", 150, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> future2 = publisher.publish(
                new IntEventTest(42), "int-task", 150, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> future3 = publisher.publish(
                new DoubleEventTest(3.14), "double-task", 150, TimeUnit.MILLISECONDS);

        assertTrue(latch.await(2, TimeUnit.SECONDS));

        future1.cancel(false);
        future2.cancel(false);
        future3.cancel(false);

        assertTrue(stringCount.get() >= 2);
        assertTrue(intCount.get() >= 2);
        assertTrue(doubleCount.get() >= 2);
    }

    @Test
    void testEventWithPriority() throws InterruptedException {
        SyncEventSubscriber syncSubscriber = new SyncEventSubscriber();
        final List<String> processOrder = Collections.synchronizedList(new ArrayList<>());

        syncSubscriber.subscribeEvent(StringEventTest.class, event -> {
            processOrder.add(event.getData());
        });
        syncSubscriber.run();

        try {
            ScheduledFuture<?> future1 = publisher.publish(
                    new StringEventTest("L", EventPriority.LOW), "low", 200, TimeUnit.MILLISECONDS);
            ScheduledFuture<?> future2 = publisher.publish(
                    new StringEventTest("H", EventPriority.HIGH), "high", 200, TimeUnit.MILLISECONDS);
            ScheduledFuture<?> future3 = publisher.publish(
                    new StringEventTest("M", EventPriority.MEDIUM), "medium", 200, TimeUnit.MILLISECONDS);


            Thread.sleep(200);

            future1.cancel(false);
            future2.cancel(false);
            future3.cancel(false);

            syncSubscriber.processEvents();

            assertTrue(processOrder.stream().anyMatch(s -> s.equals("H")));
            assertTrue(processOrder.stream().anyMatch(s -> s.equals("M")));
            assertTrue(processOrder.stream().anyMatch(s -> s.equals("L")));
        } finally {
            syncSubscriber.shutdown();
        }
    }

    @Test
    void testLargeNumberOfPeriodicTasks() throws InterruptedException {
        final AtomicInteger eventCount = new AtomicInteger(0);
        final int tasksCount = 10;
        final int expectedExecutions = tasksCount * 2;
        CountDownLatch latch = new CountDownLatch(expectedExecutions);

        subscriber.subscribeEvent(IntEventTest.class, e -> {
            eventCount.incrementAndGet();
            latch.countDown();
        });
        subscriber.run();

        List<ScheduledFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < tasksCount; i++) {
            ScheduledFuture<?> future = publisher.publish(new IntEventTest(i),
                    "task-" + i, 100, TimeUnit.MILLISECONDS);
            futures.add(future);
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        for (ScheduledFuture<?> future : futures) {
            future.cancel(false);
        }

        assertTrue(eventCount.get() >= expectedExecutions);
    }

    @Test
    void testPeriodicTaskWithErrorInSubscriber() throws InterruptedException {
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(6);

        subscriber.subscribeEvent(StringEventTest.class,
                event -> {
                    if ("error".equals(event.getData())) {
                        latch.countDown();
                        throw new RuntimeException("Test error in subscriber");
                    } else {
                        successCount.incrementAndGet();
                        latch.countDown();
                    }
                },
                (event, exception) -> errorCount.incrementAndGet()
        );
        subscriber.run();

        ScheduledFuture<?> future1 = publisher.publish(
                new StringEventTest("success"), "success-task", 150, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> future2 = publisher.publish(
                new StringEventTest("error"), "error-task", 150, TimeUnit.MILLISECONDS);

        assertTrue(latch.await(3, TimeUnit.SECONDS));

        future1.cancel(false);
        future2.cancel(false);

        assertTrue(successCount.get() >= 2);
        assertTrue(errorCount.get() >= 2);
        assertTrue(subscriber.getTotalErrorCount() >= 2);
    }

    @Test
    void testPeriodicTaskSurvivesSubscriberShutdown() throws InterruptedException {
        final AtomicInteger handlerCount = new AtomicInteger(0);
        CountDownLatch initialLatch = new CountDownLatch(2);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            handlerCount.incrementAndGet();
            initialLatch.countDown();
        });
        subscriber.run();

        ScheduledFuture<?> future = publisher.publish(
                new StringEventTest("survivor"), "survivor", 100, TimeUnit.MILLISECONDS);

        assertTrue(initialLatch.await(1, TimeUnit.SECONDS));
        assertEquals(2, handlerCount.get());

        subscriber.shutdown();

        assertFalse(future.isCancelled());
        assertFalse(future.isDone());

        Thread.sleep(300);

        assertEquals(2, handlerCount.get());

        future.cancel(false);
    }

    @Test
    void testMultipleCancellations() throws InterruptedException {
        final AtomicInteger handlerCount = new AtomicInteger(0);

        subscriber.subscribeEvent(StringEventTest.class, e -> handlerCount.incrementAndGet());
        subscriber.run();

        ScheduledFuture<?> future = publisher.publish(
                new StringEventTest("multi-cancel"), "multi-cancel", 100, TimeUnit.MILLISECONDS);

        Thread.sleep(150);
        int countAfterStart = handlerCount.get();
        assertTrue(countAfterStart >= 1);

        assertDoesNotThrow(() -> publisher.cancel("multi-cancel"));
        assertDoesNotThrow(() -> publisher.cancel("multi-cancel"));
        assertDoesNotThrow(() -> publisher.cancel("multi-cancel"));

        assertTrue(future.isCancelled());

        Thread.sleep(200);
        assertEquals(countAfterStart, handlerCount.get());
    }

    @AfterEach
    void tearDown() {
        if (subscriber != null && subscriber.getStatus() != Status.SHUTDOWN) {
            subscriber.shutdown();
        }
        if (publisher != null && publisher.getStatus() != Status.SHUTDOWN) {
            publisher.shutdown();
        }
    }
}
