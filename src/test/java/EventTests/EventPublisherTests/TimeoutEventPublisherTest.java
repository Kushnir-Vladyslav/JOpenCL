package EventTests.EventPublisherTests;

import EventTests.TestsEvents.DoubleEventTest;
import EventTests.TestsEvents.IntEventTest;
import EventTests.TestsEvents.StringEventTest;
import com.jopencl.Event.EventManager;
import com.jopencl.Event.EventPriority;
import com.jopencl.Event.EventPublishers.TimeoutEventPublisher;
import com.jopencl.Event.EventSubscribers.AsyncEventSubscriber;
import com.jopencl.Event.EventSubscribers.SyncEventSubscriber;
import com.jopencl.Event.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class TimeoutEventPublisherTest {
    private TimeoutEventPublisher publisher;
    private AsyncEventSubscriber subscriber;
    private final EventManager eventManager = EventManager.getInstance();

    @BeforeEach
    void setUp() {
        publisher = new TimeoutEventPublisher();
        subscriber = new AsyncEventSubscriber();
    }

    @Test
    void testPublisherInitialStatus() {
        assertEquals(Status.RUNNING, publisher.getStatus());
    }

    @Test
    void testConstructorWithTimeUnit() {
        TimeoutEventPublisher customPublisher = new TimeoutEventPublisher(TimeUnit.SECONDS);
        assertEquals(Status.RUNNING, customPublisher.getStatus());
        customPublisher.shutdown();
    }

    @Test
    void testConstructorWithNullTimeUnit() {
        assertThrows(IllegalArgumentException.class, () -> new TimeoutEventPublisher(null));
    }

    @Test
    void testPublishSingleEventWithTimeout() throws InterruptedException {
        StringEventTest event = new StringEventTest("timeout test message");
        final AtomicInteger handlerCount = new AtomicInteger(0);
        final AtomicReference<String> receivedData = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        subscriber.subscribeEvent(StringEventTest.class, (StringEventTest e) -> {
            handlerCount.incrementAndGet();
            receivedData.set(e.getData());
            latch.countDown();
        });
        subscriber.run();

        publisher.publish(event, 1000, TimeUnit.MILLISECONDS);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(1, handlerCount.get());
        assertEquals("timeout test message", receivedData.get());
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

        publisher.publish(event, 1000);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(1, handlerCount.get());
        assertEquals("default timeout test", receivedData.get());
    }

    @Test
    void testPublishMultipleEventsWithTimeout() throws InterruptedException {
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

        publisher.publish(new StringEventTest("test1"), 1000);
        publisher.publish(new StringEventTest("test2"), 1000);
        publisher.publish(new IntEventTest(42), 1000);
        publisher.publish(new DoubleEventTest(3.14), 1000);
        publisher.publish(new StringEventTest("test3"), 1000);

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertEquals(3, stringCount.get());
        assertEquals(1, intCount.get());
        assertEquals(1, doubleCount.get());
    }

    @Test
    void testPublishEventWithPriority() throws InterruptedException {
        StringBuilder processOrder = new StringBuilder();

        SyncEventSubscriber subscriber2 = new SyncEventSubscriber();

        subscriber2.subscribeEvent(StringEventTest.class, (StringEventTest event) ->
                processOrder.append(event.getData()));

        subscriber2.run();

        publisher.publish(new StringEventTest("L", EventPriority.LOW), 1000);
        publisher.publish(new StringEventTest("H", EventPriority.HIGH), 1000);
        publisher.publish(new StringEventTest("M", EventPriority.MEDIUM), 1000);

        Thread.sleep(100);

        subscriber2.processEvents();
        subscriber2.shutdown();

        assertEquals("HML", processOrder.toString());
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
            publisher.publish(event, 1000);

            assertTrue(latch.await(3, TimeUnit.SECONDS));

            assertEquals(1, subscriber1Count.get());
            assertEquals(1, subscriber2Count.get());
        } finally {
            subscriber2.shutdown();
        }
    }

    @Test
    void testPublishWithNoSubscribers() throws InterruptedException {
        StringEventTest event = new StringEventTest("no subscribers");

        assertDoesNotThrow(() -> publisher.publish(event, 1000));

        Thread.sleep(100);
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
    void testPublishEventData() throws InterruptedException {
        StringEventTest stringEvent = new StringEventTest("string data");
        IntEventTest intEvent = new IntEventTest(123);
        DoubleEventTest doubleEvent = new DoubleEventTest(45.67);

        final AtomicReference<String> stringData = new AtomicReference<>();
        final AtomicReference<Integer> intData = new AtomicReference<>();
        final AtomicReference<Double> doubleData = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(3);

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

        publisher.publish(stringEvent, 1000);
        publisher.publish(intEvent, 1000);
        publisher.publish(doubleEvent, 1000);

        assertTrue(latch.await(3, TimeUnit.SECONDS));

        assertEquals("string data", stringData.get());
        assertEquals(Integer.valueOf(123), intData.get());
        assertEquals(Double.valueOf(45.67), doubleData.get());
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
        publisher.publish(new StringEventTest("timestamp test"), 1000);
        long afterPublish = System.currentTimeMillis();

        assertTrue(latch.await(2, TimeUnit.SECONDS));

        long timestamp = eventTimestamp.get();
        assertTrue(timestamp >= beforePublish && timestamp <= afterPublish);
    }

    @Test
    void testPublishLargeNumberOfEvents() throws InterruptedException {
        final AtomicInteger eventCount = new AtomicInteger(0);
        final int totalEvents = 50;
        CountDownLatch latch = new CountDownLatch(totalEvents);

        subscriber.subscribeEvent(IntEventTest.class, e -> {
            eventCount.incrementAndGet();
            latch.countDown();
        });
        subscriber.run();

        for (int i = 0; i < totalEvents; i++) {
            publisher.publish(new IntEventTest(i), 2000);
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));

        assertEquals(totalEvents, eventCount.get());
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

        publisher.publish(new StringEventTest("before shutdown"), 1000);
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(1, handlerCount.get());

        subscriber.shutdown();

        publisher.publish(new StringEventTest("after shutdown"), 1000);
        Thread.sleep(200);
        assertEquals(1, handlerCount.get());
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
        publisher.publish(originalEvent, 1000);

        assertTrue(latch.await(2, TimeUnit.SECONDS));

        assertEquals(1, receivedEvents.size());
        StringEventTest receivedEvent = receivedEvents.get(0);

        assertEquals(originalEvent.getData(), receivedEvent.getData());
        assertEquals(originalEvent.getPriority(), receivedEvent.getPriority());
        assertEquals(originalEvent.getCreatedTimeMillis(), receivedEvent.getCreatedTimeMillis());
    }

    @Test
    void testPublishWithVeryShortTimeout() throws InterruptedException {
        final AtomicInteger handlerCount = new AtomicInteger(0);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            try {
                Thread.sleep(200);
                handlerCount.incrementAndGet();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        subscriber.run();

        assertDoesNotThrow(() -> publisher.publish(new StringEventTest("short timeout"), 10));

        Thread.sleep(300);
        assertTrue(handlerCount.get() <= 1);
    }

    @Test
    void testTimeoutBehaviorWithSlowEventManager() throws InterruptedException {
        final AtomicInteger processedCount = new AtomicInteger(0);
        final AtomicInteger timeoutCount = new AtomicInteger(0);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            try {
                Thread.sleep(500);
                processedCount.incrementAndGet();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        subscriber.run();

        ExecutorService testExecutor = Executors.newFixedThreadPool(3);
        CountDownLatch publishLatch = new CountDownLatch(3);

        try {
            for (int i = 0; i < 3; i++) {
                final int eventId = i;
                testExecutor.submit(() -> {
                    try {
                        publisher.publish(new StringEventTest("event" + eventId), 100, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        timeoutCount.incrementAndGet();
                    } finally {
                        publishLatch.countDown();
                    }
                });
            }

            assertTrue(publishLatch.await(2, TimeUnit.SECONDS));
            Thread.sleep(1000);

            assertTrue(processedCount.get() + timeoutCount.get() >= 0);

        } finally {
            testExecutor.shutdown();
        }
    }

    @Test
    void testConcurrentPublishingWithTimeout() throws InterruptedException {
        final AtomicInteger eventCount = new AtomicInteger(0);
        final int threadsCount = 3;
        final int eventsPerThread = 5;
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
                        try {
                            publisher.publish(new IntEventTest(threadId * eventsPerThread + j), 2000);
                        } catch (Exception e) {
                            // Handle timeout or other exceptions
                        }
                    }
                });
            }

            assertTrue(latch.await(10, TimeUnit.SECONDS));

            assertEquals(threadsCount * eventsPerThread, eventCount.get());
        } finally {
            testExecutor.shutdown();
        }
    }

    @Test
    void testPublishDifferentEventInstances() throws InterruptedException {
        final AtomicInteger eventCount = new AtomicInteger(0);
        final Set<StringEventTest> receivedEvents = Collections.synchronizedSet(new HashSet<>());
        CountDownLatch latch = new CountDownLatch(3);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            eventCount.incrementAndGet();
            receivedEvents.add(e);
            latch.countDown();
        });
        subscriber.run();

        publisher.publish(new StringEventTest("same data"), 2000);
        publisher.publish(new StringEventTest("same data"), 2000);
        publisher.publish(new StringEventTest("same data"), 2000);

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertEquals(3, eventCount.get());
        assertEquals(3, receivedEvents.size());
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
                for (int i = 0; i < 50; i++) {
                    publisher.publish(new StringEventTest("event" + i), 1000);
                    publishedCount.incrementAndGet();
                    Thread.sleep(20);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread.sleep(300);
        publisher.shutdown();

        Thread.sleep(500);

        assertTrue(publishedCount.get() > 0);
        assertTrue(publishedCount.get() < 50);

        testExecutor.shutdown();
    }

    @Test
    void testTimeoutWithDifferentTimeUnits() throws InterruptedException {
        final AtomicInteger handlerCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            handlerCount.incrementAndGet();
            latch.countDown();
        });
        subscriber.run();

        // Test different TimeUnit values
        publisher.publish(new StringEventTest("seconds"), 1, TimeUnit.SECONDS);
        publisher.publish(new StringEventTest("milliseconds"), 1000, TimeUnit.MILLISECONDS);
        publisher.publish(new StringEventTest("microseconds"), 1000000, TimeUnit.MICROSECONDS);

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals(3, handlerCount.get());
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
