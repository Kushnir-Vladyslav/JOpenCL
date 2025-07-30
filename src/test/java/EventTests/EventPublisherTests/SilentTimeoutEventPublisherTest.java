package EventTests.EventPublisherTests;

import EventTests.TestsEvents.DoubleEventTest;
import EventTests.TestsEvents.IntEventTest;
import EventTests.TestsEvents.StringEventTest;
import com.jopencl.Event.EventManager;
import com.jopencl.Event.EventPriority;
import com.jopencl.Event.EventPublishers.PeriodicEventPublisher;
import com.jopencl.Event.EventPublishers.SilentTimeoutEventPublisher;
import com.jopencl.Event.EventPublishers.SyncEventPublisher;
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

public class SilentTimeoutEventPublisherTest {
    private SilentTimeoutEventPublisher publisher;
    private AsyncEventSubscriber subscriber;
    private final EventManager eventManager = EventManager.getInstance();

    @BeforeEach
    void setUp() {
        publisher = new SilentTimeoutEventPublisher();
        subscriber = new AsyncEventSubscriber();
    }

    @Test
    void testPublisherInitialStatus() {
        assertEquals(Status.RUNNING, publisher.getStatus());
    }

    @Test
    void testConstructorWithTimeUnit() {
        SilentTimeoutEventPublisher customPublisher = new SilentTimeoutEventPublisher(TimeUnit.SECONDS);
        assertEquals(Status.RUNNING, customPublisher.getStatus());
        customPublisher.shutdown();
    }

    @Test
    void testConstructorWithNullTimeUnit() {
        assertThrows(IllegalArgumentException.class, () -> new SilentTimeoutEventPublisher(null));
    }

    @Test
    void testPublishSingleEventWithTimeout() throws InterruptedException {
        StringEventTest event = new StringEventTest("silent timeout test message");
        final AtomicInteger handlerCount = new AtomicInteger(0);
        final AtomicReference<String> receivedData = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        subscriber.subscribeEvent(StringEventTest.class, (StringEventTest e) -> {
            handlerCount.incrementAndGet();
            receivedData.set(e.getData());
            latch.countDown();
        });
        subscriber.run();

        Future<?> future = publisher.publish(event, 1000, TimeUnit.MILLISECONDS);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(1, handlerCount.get());
        assertEquals("silent timeout test message", receivedData.get());

        assertDoesNotThrow(() -> future.get(100, TimeUnit.MILLISECONDS));
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
    }

    @Test
    void testPublishSingleEventWithDefaultTimeout() throws InterruptedException {
        StringEventTest event = new StringEventTest("default timeout test");
        final AtomicInteger handlerCount = new AtomicInteger(0);
        final AtomicReference<String> receivedData = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        subscriber.subscribeEvent(StringEventTest.class, (StringEventTest e) -> {
            handlerCount.incrementAndGet();
            receivedData.set(e.getData());
            latch.countDown();
        });
        subscriber.run();

        Future<?> future = publisher.publish(event, 1000);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(1, handlerCount.get());
        assertEquals("default timeout test", receivedData.get());
        assertTrue(future.isDone());
    }

    @Test
    void testPublishReturnsValidFuture() {
        StringEventTest event = new StringEventTest("future test");

        Future<?> future = publisher.publish(event, 1000);

        assertNotNull(future);
    }

    @Test
    void testPublishMultipleEventsWithTimeout() throws InterruptedException {
        final AtomicInteger stringCount = new AtomicInteger(0);
        final AtomicInteger intCount = new AtomicInteger(0);
        final AtomicInteger doubleCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(5);
        List<Future<?>> futures = new ArrayList<>();

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

        futures.add(publisher.publish(new StringEventTest("test1"), 2000));
        futures.add(publisher.publish(new StringEventTest("test2"), 2000));
        futures.add(publisher.publish(new IntEventTest(42), 2000));
        futures.add(publisher.publish(new DoubleEventTest(3.14), 2000));
        futures.add(publisher.publish(new StringEventTest("test3"), 2000));

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertEquals(3, stringCount.get());
        assertEquals(1, intCount.get());
        assertEquals(1, doubleCount.get());

        for (Future<?> future : futures) {
            assertTrue(future.isDone());
            assertFalse(future.isCancelled());
        }
    }

    @Test
    void testPublishEventWithPriority() throws InterruptedException {
        StringBuilder processOrder = new StringBuilder();
        List<Future<?>> futures = new ArrayList<>();

        SyncEventSubscriber subscriber2 = new SyncEventSubscriber();

        subscriber2.subscribeEvent(StringEventTest.class, (StringEventTest event) ->
                processOrder.append(event.getData()));

        subscriber2.run();

        futures.add(publisher.publish(new StringEventTest("L", EventPriority.LOW), 2000));
        futures.add(publisher.publish(new StringEventTest("H", EventPriority.HIGH), 2000));
        futures.add(publisher.publish(new StringEventTest("M", EventPriority.MEDIUM), 2000));

        Thread.sleep(100);

        subscriber2.processEvents();
        subscriber2.shutdown();

        assertEquals("HML", processOrder.toString());

        for (Future<?> future : futures) {
            assertTrue(future.isDone());
        }
    }

    @Test
    void testPublishToMultipleSubscribers() throws InterruptedException {
        AsyncEventSubscriber subscriber2 = new AsyncEventSubscriber();
        final AtomicInteger subscriber1Count = new AtomicInteger(0);
        final AtomicInteger subscriber2Count = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        try {
            subscriber.subscribeEvent(StringEventTest.class, e -> {
                subscriber1Count.incrementAndGet();
                latch.countDown();
            });
            subscriber2.subscribeEvent(StringEventTest.class, e -> {
                subscriber2Count.incrementAndGet();
                latch.countDown();
            });

            subscriber.run();
            subscriber2.run();

            StringEventTest event = new StringEventTest("broadcast test");
            Future<?> future = publisher.publish(event, 2000);

            assertTrue(latch.await(3, TimeUnit.SECONDS));

            assertEquals(1, subscriber1Count.get());
            assertEquals(1, subscriber2Count.get());
            assertTrue(future.isDone());
            assertFalse(future.isCancelled());
        } finally {
            subscriber2.shutdown();
        }
    }

    @Test
    void testPublishWithNoSubscribers() throws InterruptedException {
        StringEventTest event = new StringEventTest("no subscribers");

        Future<?> future = assertDoesNotThrow(() -> publisher.publish(event, 1000));

        Thread.sleep(200);
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
    }

    @Test
    void testPublishNullEvent() {
        assertThrows(IllegalArgumentException.class, () -> publisher.publish(null, 1000));
    }

    @Test
    void testPublishWithNegativeTimeout() {
        StringEventTest event = new StringEventTest("negative timeout test");
        assertThrows(IllegalArgumentException.class, () -> publisher.publish(event, -1));
    }

    @Test
    void testPublishWithNullTimeUnit() {
        StringEventTest event = new StringEventTest("null timeunit test");
        assertThrows(IllegalArgumentException.class, () -> publisher.publish(event, 1000, null));
    }

    @Test
    void testAsyncProcessing() throws InterruptedException {
        final String currentThreadName = Thread.currentThread().getName();
        final StringBuilder processingThreadName = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            processingThreadName.append(Thread.currentThread().getName());
            latch.countDown();
        });
        subscriber.run();

        Future<?> future = publisher.publish(new StringEventTest("thread test"), 2000);

        assertTrue(latch.await(3, TimeUnit.SECONDS));

        assertNotEquals(currentThreadName, processingThreadName.toString());
        assertTrue(future.isDone());
    }

    @Test
    void testPublishEventData() throws InterruptedException {
        StringEventTest stringEvent = new StringEventTest("string data");
        IntEventTest intEvent = new IntEventTest(123);
        DoubleEventTest doubleEvent = new DoubleEventTest(45.67);

        final AtomicReference<String> stringData = new AtomicReference<>();
        final AtomicReference<Integer> intData = new AtomicReference<>();
        final AtomicReference<Double> doubleData = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(3);
        List<Future<?>> futures = new ArrayList<>();

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            stringData.set(e.getData());
            latch.countDown();
        });
        subscriber.subscribeEvent(IntEventTest.class, e -> {
            intData.set(e.getData());
            latch.countDown();
        });
        subscriber.subscribeEvent(DoubleEventTest.class, e -> {
            doubleData.set(e.getData());
            latch.countDown();
        });
        subscriber.run();

        futures.add(publisher.publish(stringEvent, 2000));
        futures.add(publisher.publish(intEvent, 2000));
        futures.add(publisher.publish(doubleEvent, 2000));

        assertTrue(latch.await(3, TimeUnit.SECONDS));

        assertEquals("string data", stringData.get());
        assertEquals(Integer.valueOf(123), intData.get());
        assertEquals(Double.valueOf(45.67), doubleData.get());

        for (Future<?> future : futures) {
            assertTrue(future.isDone());
            assertFalse(future.isCancelled());
        }
    }

    @Test
    void testPublishEventTimestamp() throws InterruptedException {
        final AtomicLong eventTimestamp = new AtomicLong(0);
        CountDownLatch latch = new CountDownLatch(1);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            eventTimestamp.set(e.getCreatedTimeMillis());
            latch.countDown();
        });
        subscriber.run();

        long beforePublish = System.currentTimeMillis();
        Future<?> future = publisher.publish(new StringEventTest("timestamp test"), 2000);
        long afterPublish = System.currentTimeMillis();

        assertTrue(latch.await(3, TimeUnit.SECONDS));

        long timestamp = eventTimestamp.get();
        assertTrue(timestamp >= beforePublish && timestamp <= afterPublish);
        assertTrue(future.isDone());
    }

    @Test
    void testTimeoutWithDifferentTimeUnits() throws InterruptedException {
        final AtomicInteger handlerCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);
        List<Future<?>> futures = new ArrayList<>();

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            handlerCount.incrementAndGet();
            latch.countDown();
        });
        subscriber.run();

        futures.add(publisher.publish(new StringEventTest("seconds"), 2, TimeUnit.SECONDS));
        futures.add(publisher.publish(new StringEventTest("milliseconds"), 2000, TimeUnit.MILLISECONDS));
        futures.add(publisher.publish(new StringEventTest("microseconds"), 2000000, TimeUnit.MICROSECONDS));

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(3, handlerCount.get());

        for (Future<?> future : futures) {
            assertTrue(future.isDone());
            assertFalse(future.isCancelled());
        }
    }

    @Test
    void testPublishLargeNumberOfEvents() throws InterruptedException {
        final AtomicInteger eventCount = new AtomicInteger(0);
        final int totalEvents = 30;
        CountDownLatch latch = new CountDownLatch(totalEvents);
        List<Future<?>> futures = new ArrayList<>();

        subscriber.subscribeEvent(IntEventTest.class, e -> {
            eventCount.incrementAndGet();
            latch.countDown();
        });
        subscriber.run();

        for (int i = 0; i < totalEvents; i++) {
            futures.add(publisher.publish(new IntEventTest(i), 3000));
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));

        assertEquals(totalEvents, eventCount.get());

        for (Future<?> future : futures) {
            assertTrue(future.isDone());
        }
    }

    @Test
    void testPublishAfterSubscriberShutdown() throws InterruptedException {
        final AtomicInteger handlerCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            handlerCount.incrementAndGet();
            latch.countDown();
        });
        subscriber.run();

        Future<?> future1 = publisher.publish(new StringEventTest("before shutdown"), 2000);
        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals(1, handlerCount.get());
        assertTrue(future1.isDone());

        subscriber.shutdown();

        Future<?> future2 = publisher.publish(new StringEventTest("after shutdown"), 2000);
        Thread.sleep(300);
        assertEquals(1, handlerCount.get());
        assertTrue(future2.isDone()); // Future completes even if no subscribers
    }

    @Test
    void testPublisherShutdown() {
        assertEquals(Status.RUNNING, publisher.getStatus());

        assertDoesNotThrow(() -> publisher.shutdown());
        assertEquals(Status.SHUTDOWN, publisher.getStatus());

        assertThrows(IllegalStateException.class, () -> publisher.shutdown());
    }

    @Test
    void testPublishAfterPublisherShutdown() {
        publisher.shutdown();

        assertThrows(IllegalStateException.class,
                () -> publisher.publish(new StringEventTest("after publisher shutdown"), 1000));
    }

    @Test
    void testPublishAfterShutdownDoesNotProcessEvents() throws InterruptedException {
        final AtomicInteger handlerCount = new AtomicInteger(0);

        subscriber.subscribeEvent(StringEventTest.class, e -> handlerCount.incrementAndGet());
        subscriber.run();

        publisher.shutdown();

        assertThrows(IllegalStateException.class,
                () -> publisher.publish(new StringEventTest("ignored"), 1000));

        Thread.sleep(100);
        assertEquals(0, handlerCount.get());
    }

    @Test
    void testEventIntegrity() throws InterruptedException {
        final List<StringEventTest> receivedEvents = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            receivedEvents.add(e);
            latch.countDown();
        });
        subscriber.run();

        StringEventTest originalEvent = new StringEventTest("integrity test");
        Future<?> future = publisher.publish(originalEvent, 2000);

        assertTrue(latch.await(3, TimeUnit.SECONDS));

        assertEquals(1, receivedEvents.size());
        StringEventTest receivedEvent = receivedEvents.get(0);

        assertEquals(originalEvent.getData(), receivedEvent.getData());
        assertEquals(originalEvent.getPriority(), receivedEvent.getPriority());
        assertEquals(originalEvent.getCreatedTimeMillis(), receivedEvent.getCreatedTimeMillis());
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
    }

    @Test
    void testConcurrentPublishingWithTimeout() throws InterruptedException {
        final AtomicInteger eventCount = new AtomicInteger(0);
        final int threadsCount = 3;
        final int eventsPerThread = 5;
        CountDownLatch latch = new CountDownLatch(threadsCount * eventsPerThread);
        List<Future<?>> allFutures = Collections.synchronizedList(new ArrayList<>());

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
                        Future<?> future = publisher.publish(
                                new IntEventTest(threadId * eventsPerThread + j), 3000);
                        allFutures.add(future);
                    }
                });
            }

            assertTrue(latch.await(10, TimeUnit.SECONDS));

            assertEquals(threadsCount * eventsPerThread, eventCount.get());

            Thread.sleep(500);

            for (Future<?> future : allFutures) {
                assertTrue(future.isDone());
            }
        } finally {
            testExecutor.shutdown();
        }
    }

    @Test
    void testPublishDifferentEventInstances() throws InterruptedException {
        final AtomicInteger eventCount = new AtomicInteger(0);
        final Set<StringEventTest> receivedEvents = Collections.synchronizedSet(new HashSet<>());
        CountDownLatch latch = new CountDownLatch(3);
        List<Future<?>> futures = new ArrayList<>();

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            eventCount.incrementAndGet();
            receivedEvents.add(e);
            latch.countDown();
        });
        subscriber.run();

        futures.add(publisher.publish(new StringEventTest("same data"), 2000));
        futures.add(publisher.publish(new StringEventTest("same data"), 2000));
        futures.add(publisher.publish(new StringEventTest("same data"), 2000));

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertEquals(3, eventCount.get());
        assertEquals(3, receivedEvents.size());

        for (Future<?> future : futures) {
            assertTrue(future.isDone());
        }
    }

    @Test
    void testShutdownInterruptsPublishing() throws InterruptedException {
        final AtomicInteger publishedCount = new AtomicInteger(0);
        final AtomicInteger processedCount = new AtomicInteger(0);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            processedCount.incrementAndGet();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        subscriber.run();

        ExecutorService testExecutor = Executors.newSingleThreadExecutor();
        testExecutor.submit(() -> {
            try {
                for (int i = 0; i < 30; i++) {
                    publisher.publish(new StringEventTest("event" + i), 2000);
                    publishedCount.incrementAndGet();
                    Thread.sleep(50);
                }
            } catch (IllegalStateException e) {
                // Expected when publisher is shut down
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread.sleep(500);
        publisher.shutdown();

        Thread.sleep(500);

        assertTrue(publishedCount.get() > 0);
        assertTrue(publishedCount.get() < 30);

        testExecutor.shutdown();
    }

    @Test
    void testMultipleTimeoutsHandledSilently() throws InterruptedException {
        final AtomicInteger processedCount = new AtomicInteger(0);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            try {
                Thread.sleep(500);
                processedCount.incrementAndGet();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        subscriber.run();

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            futures.add(publisher.publish(new StringEventTest("timeout" + i), 100));
        }

        Thread.sleep(800);

        for (Future<?> future : futures) {
            assertTrue(future.isDone());
        }

        assertTrue(processedCount.get() <= 5);
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
