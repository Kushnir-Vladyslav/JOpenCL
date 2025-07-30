package EventTests.EventPublisherTests;

import EventTests.TestsEvents.DoubleEventTest;
import EventTests.TestsEvents.IntEventTest;
import EventTests.TestsEvents.StringEventTest;
import com.jopencl.Event.EventManager;
import com.jopencl.Event.EventPriority;
import com.jopencl.Event.EventPublishers.DelayedEventPublisher;
import com.jopencl.Event.EventSubscribers.AsyncEventSubscriber;
import com.jopencl.Event.EventSubscribers.SyncEventSubscriber;
import com.jopencl.Event.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class DelayedEventPublisherTest {
    private DelayedEventPublisher publisher;
    private AsyncEventSubscriber subscriber;
    private final EventManager eventManager = EventManager.getInstance();

    @BeforeEach
    void setUp() {
        publisher = new DelayedEventPublisher();
        subscriber = new AsyncEventSubscriber();
    }

    @Test
    void testDefaultConstructor() {
        DelayedEventPublisher defaultPublisher = new DelayedEventPublisher();
        assertEquals(Status.RUNNING, defaultPublisher.getStatus());
        defaultPublisher.shutdown();
    }

    @Test
    void testConstructorWithTimeUnit() {
        DelayedEventPublisher secondsPublisher = new DelayedEventPublisher(TimeUnit.SECONDS);
        assertEquals(Status.RUNNING, secondsPublisher.getStatus());
        secondsPublisher.shutdown();
    }

    @Test
    void testConstructorValidation() {
        assertThrows(IllegalArgumentException.class,
                () -> new DelayedEventPublisher(null));
    }

    @Test
    void testPublisherInitialStatus() {
        assertEquals(Status.RUNNING, publisher.getStatus());
    }

    @Test
    void testPublishWithDelayAndTimeUnit() throws InterruptedException {
        final AtomicInteger handlerCount = new AtomicInteger(0);
        final AtomicReference<String> receivedData = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            handlerCount.incrementAndGet();
            receivedData.set(e.getData());
            latch.countDown();
        });
        subscriber.run();

        long startTime = System.currentTimeMillis();
        ScheduledFuture<?> future = publisher.publish(
                new StringEventTest("delayed message"), 200, TimeUnit.MILLISECONDS);

        assertNotNull(future);
        assertFalse(future.isDone());

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        long endTime = System.currentTimeMillis();

        assertEquals(1, handlerCount.get());
        assertEquals("delayed message", receivedData.get());
        assertTrue(future.isDone());
        assertTrue(endTime - startTime >= 200);
    }

    @Test
    void testPublishWithDefaultTimeUnit() throws InterruptedException {
        final AtomicInteger handlerCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            handlerCount.incrementAndGet();
            latch.countDown();
        });
        subscriber.run();

        long startTime = System.currentTimeMillis();
        ScheduledFuture<?> future = publisher.publish(new StringEventTest("default timeunit"), 150);

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        long endTime = System.currentTimeMillis();

        assertEquals(1, handlerCount.get());
        assertTrue(future.isDone());
        assertTrue(endTime - startTime >= 150);
    }

    @Test
    void testPublishWithDifferentTimeUnits() throws InterruptedException {
        DelayedEventPublisher secondsPublisher = new DelayedEventPublisher(TimeUnit.SECONDS);

        try {
            final AtomicInteger handlerCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(1);

            subscriber.subscribeEvent(StringEventTest.class, e -> {
                handlerCount.incrementAndGet();
                latch.countDown();
            });
            subscriber.run();

            long startTime = System.currentTimeMillis();
            ScheduledFuture<?> future = secondsPublisher.publish(
                    new StringEventTest("seconds test"), 1);

            assertTrue(latch.await(2, TimeUnit.SECONDS));
            long endTime = System.currentTimeMillis();

            assertEquals(1, handlerCount.get());
            assertTrue(future.isDone());
            assertTrue(endTime - startTime >= 1000);
        } finally {
            secondsPublisher.shutdown();
        }
    }

    @Test
    void testPublishMultipleDelayedEvents() throws InterruptedException {
        final AtomicInteger handlerCount = new AtomicInteger(0);
        final List<String> receivedOrder = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(3);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            handlerCount.incrementAndGet();
            receivedOrder.add(e.getData());
            latch.countDown();
        });
        subscriber.run();

        publisher.publish(new StringEventTest("third"), 300, TimeUnit.MILLISECONDS);
        publisher.publish(new StringEventTest("first"), 100, TimeUnit.MILLISECONDS);
        publisher.publish(new StringEventTest("second"), 200, TimeUnit.MILLISECONDS);

        assertTrue(latch.await(2, TimeUnit.SECONDS));

        assertEquals(3, handlerCount.get());
        assertEquals(Arrays.asList("first", "second", "third"), receivedOrder);
    }

    @Test
    void testPublishWithZeroDelay() throws InterruptedException {
        final AtomicInteger handlerCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            handlerCount.incrementAndGet();
            latch.countDown();
        });
        subscriber.run();

        ScheduledFuture<?> future = publisher.publish(
                new StringEventTest("immediate"), 0, TimeUnit.MILLISECONDS);

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(1, handlerCount.get());
        assertTrue(future.isDone());
    }

    @Test
    void testPublishValidation() {
        assertThrows(IllegalArgumentException.class,
                () -> publisher.publish(null, 100, TimeUnit.MILLISECONDS));

        assertThrows(IllegalArgumentException.class,
                () -> publisher.publish(new StringEventTest("test"), -1, TimeUnit.MILLISECONDS));

        assertThrows(IllegalArgumentException.class,
                () -> publisher.publish(new StringEventTest("test"), 100, null));

        assertThrows(IllegalArgumentException.class,
                () -> publisher.publish(null, 100));

        assertThrows(IllegalArgumentException.class,
                () -> publisher.publish(new StringEventTest("test"), -1));
    }

    @Test
    void testCancelScheduledEvent() throws InterruptedException {
        final AtomicInteger handlerCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            handlerCount.incrementAndGet();
            latch.countDown();
        });
        subscriber.run();

        ScheduledFuture<?> future = publisher.publish(
                new StringEventTest("cancelled"), 500, TimeUnit.MILLISECONDS);

        assertFalse(future.isDone());
        assertFalse(future.isCancelled());

        boolean cancelled = future.cancel(false);
        assertTrue(cancelled);
        assertTrue(future.isCancelled());

        assertFalse(latch.await(700, TimeUnit.MILLISECONDS));
        assertEquals(0, handlerCount.get());
    }

    @Test
    void testScheduledFutureStatus() throws InterruptedException {
        ScheduledFuture<?> future = publisher.publish(
                new StringEventTest("status test"), 100, TimeUnit.MILLISECONDS);

        assertFalse(future.isDone());
        assertFalse(future.isCancelled());

        Thread.sleep(200);

        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
    }

    @Test
    void testPublishAfterShutdown() {
        publisher.shutdown();
        assertEquals(Status.SHUTDOWN, publisher.getStatus());

        assertThrows(IllegalStateException.class,
                () -> publisher.publish(new StringEventTest("after shutdown"), 100, TimeUnit.MILLISECONDS));

        assertThrows(IllegalStateException.class,
                () -> publisher.publish(new StringEventTest("after shutdown"), 100));
    }

    @Test
    void testPublisherShutdown() {
        assertEquals(Status.RUNNING, publisher.getStatus());

        assertDoesNotThrow(() -> publisher.shutdown());
        assertEquals(Status.SHUTDOWN, publisher.getStatus());

        assertThrows(IllegalStateException.class, () -> publisher.shutdown());
    }

    @Test
    void testShutdownCancelsPendingEvents() throws InterruptedException {
        final AtomicInteger handlerCount = new AtomicInteger(0);

        subscriber.subscribeEvent(StringEventTest.class, e -> handlerCount.incrementAndGet());
        subscriber.run();

        ScheduledFuture<?> future1 = publisher.publish(
                new StringEventTest("pending1"), 1000, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> future2 = publisher.publish(
                new StringEventTest("pending2"), 2000, TimeUnit.MILLISECONDS);

        assertFalse(future1.isDone());
        assertFalse(future2.isDone());

        publisher.shutdown();

        Thread.sleep(100);

        assertTrue(future1.isCancelled() || future1.isDone());
        assertTrue(future2.isCancelled() || future2.isDone());

        Thread.sleep(200);
        assertEquals(0, handlerCount.get());
    }

    @Test
    void testEventIntegrity() throws InterruptedException {
        final List<StringEventTest> receivedEvents = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            receivedEvents.add(e);
            latch.countDown();
        });
        subscriber.run();

        StringEventTest originalEvent = new StringEventTest("integrity test");
        publisher.publish(originalEvent, 50, TimeUnit.MILLISECONDS);

        assertTrue(latch.await(1, TimeUnit.SECONDS));

        assertEquals(1, receivedEvents.size());
        StringEventTest receivedEvent = receivedEvents.get(0);

        assertEquals(originalEvent.getData(), receivedEvent.getData());
        assertEquals(originalEvent.getPriority(), receivedEvent.getPriority());
        assertEquals(originalEvent.getCreatedTimeMillis(), receivedEvent.getCreatedTimeMillis());
    }

    @Test
    void testDelayAccuracy() throws InterruptedException {
        final AtomicLong actualDelay = new AtomicLong(0);
        final long expectedDelay = 200;
        CountDownLatch latch = new CountDownLatch(1);

        long startTime = System.currentTimeMillis();

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            actualDelay.set(System.currentTimeMillis() - startTime);
            latch.countDown();
        });
        subscriber.run();

        publisher.publish(new StringEventTest("timing test"), expectedDelay, TimeUnit.MILLISECONDS);

        assertTrue(latch.await(1, TimeUnit.SECONDS));

        long measured = actualDelay.get();
        assertTrue(measured >= expectedDelay - 50,
                "Delay was too short: " + measured + "ms, expected: " + expectedDelay + "ms");
        assertTrue(measured <= expectedDelay + 100,
                "Delay was too long: " + measured + "ms, expected: " + expectedDelay + "ms");
    }

    @Test
    void testConcurrentDelayedPublishing() throws InterruptedException {
        final AtomicInteger eventCount = new AtomicInteger(0);
        final int threadsCount = 5;
        final int eventsPerThread = 4;
        CountDownLatch latch = new CountDownLatch(threadsCount * eventsPerThread);

        subscriber.subscribeEvent(IntEventTest.class, e -> {
            eventCount.incrementAndGet();
            latch.countDown();
        });
        subscriber.run();

        ExecutorService testExecutor = Executors.newFixedThreadPool(threadsCount);

        try {
            for (int i = 0; i < threadsCount; i++) {
                final int threadId = i;
                testExecutor.submit(() -> {
                    for (int j = 0; j < eventsPerThread; j++) {
                        publisher.publish(new IntEventTest(threadId * eventsPerThread + j),
                                50 + (j * 10), TimeUnit.MILLISECONDS);
                    }
                });
            }

            assertTrue(latch.await(3, TimeUnit.SECONDS));
            assertEquals(threadsCount * eventsPerThread, eventCount.get());
        } finally {
            testExecutor.shutdown();
        }
    }

    @Test
    void testEventWithPriority() throws InterruptedException {
        SyncEventSubscriber syncSubscriber = new SyncEventSubscriber();
        StringBuilder processOrder = new StringBuilder();

        syncSubscriber.subscribeEvent(StringEventTest.class, event ->
                processOrder.append(event.getData()));
        syncSubscriber.run();

        try {
            publisher.publish(new StringEventTest("L", EventPriority.LOW), 100, TimeUnit.MILLISECONDS);
            publisher.publish(new StringEventTest("H", EventPriority.HIGH), 110, TimeUnit.MILLISECONDS);
            publisher.publish(new StringEventTest("M", EventPriority.MEDIUM), 120, TimeUnit.MILLISECONDS);

            Thread.sleep(300);
            syncSubscriber.processEvents();

            assertEquals("HML", processOrder.toString());
        } finally {
            syncSubscriber.shutdown();
        }
    }

    @Test
    void testLargeNumberOfDelayedEvents() throws InterruptedException {
        final AtomicInteger eventCount = new AtomicInteger(0);
        final int totalEvents = 50;
        CountDownLatch latch = new CountDownLatch(totalEvents);

        subscriber.subscribeEvent(IntEventTest.class, e -> {
            eventCount.incrementAndGet();
            latch.countDown();
        });
        subscriber.run();

        List<ScheduledFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < totalEvents; i++) {
            ScheduledFuture<?> future = publisher.publish(new IntEventTest(i),
                    10 + (i % 20), TimeUnit.MILLISECONDS);
            futures.add(future);
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(totalEvents, eventCount.get());

        for (ScheduledFuture<?> future : futures) {
            assertTrue(future.isDone());
        }
    }

    @Test
    void testPublishDifferentEventTypes() throws InterruptedException {
        final AtomicInteger stringCount = new AtomicInteger(0);
        final AtomicInteger intCount = new AtomicInteger(0);
        final AtomicInteger doubleCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(5);

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

        publisher.publish(new StringEventTest("test1"), 50, TimeUnit.MILLISECONDS);
        publisher.publish(new IntEventTest(42), 60, TimeUnit.MILLISECONDS);
        publisher.publish(new DoubleEventTest(3.14), 70, TimeUnit.MILLISECONDS);
        publisher.publish(new StringEventTest("test2"), 80, TimeUnit.MILLISECONDS);
        publisher.publish(new StringEventTest("test3"), 90, TimeUnit.MILLISECONDS);

        assertTrue(latch.await(2, TimeUnit.SECONDS));

        assertEquals(3, stringCount.get());
        assertEquals(1, intCount.get());
        assertEquals(1, doubleCount.get());
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
