package EventTests.EventPublisherTests;

import EventTests.TestsEvents.DoubleEventTest;
import EventTests.TestsEvents.IntEventTest;
import EventTests.TestsEvents.StringEventTest;
import com.jopencl.Event.EventManager;
import com.jopencl.Event.EventPriority;
import com.jopencl.Event.EventPublishers.BatchEventPublisher;
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class BatchEventPublisherTest {
    private BatchEventPublisher publisher;
    private AsyncEventSubscriber subscriber;
    private final EventManager eventManager = EventManager.getInstance();

    @BeforeEach
    void setUp() {
        publisher = new BatchEventPublisher(3); // Default batch size of 3
        subscriber = new AsyncEventSubscriber();
    }

    @Test
    void testPublisherInitialStatus() {
        assertEquals(Status.RUNNING, publisher.getStatus());
    }

    @Test
    void testConstructorValidation() {
        assertThrows(RuntimeException.class, () -> new BatchEventPublisher(0));
        assertThrows(RuntimeException.class, () -> new BatchEventPublisher(-1));
        assertDoesNotThrow(() -> new BatchEventPublisher(1));
        assertDoesNotThrow(() -> new BatchEventPublisher(10));
    }

    @Test
    void testPublishBatchWhenSizeReached() throws InterruptedException {
        final AtomicInteger eventCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            eventCount.incrementAndGet();
            latch.countDown();
        });
        subscriber.run();

        publisher.publish(new StringEventTest("event1"));
        publisher.publish(new StringEventTest("event2"));
        publisher.publish(new StringEventTest("event3"));

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(3, eventCount.get());
    }

    @Test
    void testPublishPartialBatchNoAutoFlush() throws InterruptedException {
        final AtomicInteger eventCount = new AtomicInteger(0);

        subscriber.subscribeEvent(StringEventTest.class, e -> eventCount.incrementAndGet());
        subscriber.run();

        publisher.publish(new StringEventTest("event1"));
        publisher.publish(new StringEventTest("event2"));

        Thread.sleep(50);
        assertEquals(0, eventCount.get());
    }

    @Test
    void testManualFlush() throws InterruptedException {
        final AtomicInteger eventCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            eventCount.incrementAndGet();
            latch.countDown();
        });
        subscriber.run();

        publisher.publish(new StringEventTest("event1"));
        publisher.publish(new StringEventTest("event2"));
        publisher.flush();

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(2, eventCount.get());
    }

    @Test
    void testMultipleBatches() throws InterruptedException {
        final AtomicInteger eventCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(7);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            eventCount.incrementAndGet();
            latch.countDown();
        });
        subscriber.run();

        for (int i = 1; i <= 7; i++) {
            publisher.publish(new StringEventTest("event" + i));
        }

        Thread.sleep(200);
        assertEquals(6, eventCount.get());

        publisher.flush();
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(7, eventCount.get());
    }

    @Test
    void testPublishNullEvent() {
        assertThrows(IllegalArgumentException.class, () -> publisher.publish(null));
    }

    @Test
    void testPublishDifferentEventTypes() throws InterruptedException {
        final AtomicInteger stringCount = new AtomicInteger(0);
        final AtomicInteger intCount = new AtomicInteger(0);
        final AtomicInteger doubleCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(6);

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

        publisher.publish(new StringEventTest("test1"));
        publisher.publish(new IntEventTest(42));
        publisher.publish(new DoubleEventTest(3.14));

        publisher.publish(new StringEventTest("test2"));
        publisher.publish(new StringEventTest("test3"));
        publisher.publish(new IntEventTest(100));

        assertTrue(latch.await(3, TimeUnit.SECONDS));

        assertEquals(3, stringCount.get());
        assertEquals(2, intCount.get());
        assertEquals(1, doubleCount.get());
    }

    @Test
    void testBatchEventPriority() throws InterruptedException {
        SyncEventSubscriber syncSubscriber = new SyncEventSubscriber();
        StringBuilder processOrder = new StringBuilder();

        syncSubscriber.subscribeEvent(StringEventTest.class, event ->
                processOrder.append(event.getData()));
        syncSubscriber.run();

        try {
            publisher.publish(new StringEventTest("L", EventPriority.LOW));
            publisher.publish(new StringEventTest("H", EventPriority.HIGH));
            publisher.publish(new StringEventTest("M", EventPriority.MEDIUM));

            Thread.sleep(100);
            syncSubscriber.processEvents();

            assertEquals("HML", processOrder.toString());
        } finally {
            syncSubscriber.shutdown();
        }
    }

    @Test
    void testPublishAfterShutdown() {
        publisher.shutdown();
        assertEquals(Status.SHUTDOWN, publisher.getStatus());

        assertThrows(IllegalStateException.class,
                () -> publisher.publish(new StringEventTest("after shutdown")));
    }

    @Test
    void testFlushAfterShutdown() {
        publisher.shutdown();

        assertThrows(IllegalStateException.class, () -> publisher.flush());
    }

    @Test
    void testPublisherShutdown() {
        assertEquals(Status.RUNNING, publisher.getStatus());

        assertDoesNotThrow(() -> publisher.shutdown());
        assertEquals(Status.SHUTDOWN, publisher.getStatus());

        assertThrows(IllegalStateException.class, () -> publisher.shutdown());
    }

    @Test
    void testBatchIntegrity() throws InterruptedException {
        final List<StringEventTest> receivedEvents = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(3);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            receivedEvents.add(e);
            latch.countDown();
        });
        subscriber.run();

        StringEventTest event1 = new StringEventTest("integrity1");
        StringEventTest event2 = new StringEventTest("integrity2");
        StringEventTest event3 = new StringEventTest("integrity3");

        publisher.publish(event1);
        publisher.publish(event2);
        publisher.publish(event3);

        assertTrue(latch.await(2, TimeUnit.SECONDS));

        assertEquals(3, receivedEvents.size());

        Set<String> expectedData = new HashSet<>(Arrays.asList("integrity1", "integrity2", "integrity3"));
        Set<String> actualData = receivedEvents.stream()
                .map(StringEventTest::getData)
                .collect(Collectors.toSet());

        assertEquals(expectedData, actualData);
    }

    @Test
    void testLargeBatchSize() throws InterruptedException {
        BatchEventPublisher largeBatchPublisher = new BatchEventPublisher(100);
        final AtomicInteger eventCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(100);

        try {
            subscriber.subscribeEvent(IntEventTest.class, e -> {
                eventCount.incrementAndGet();
                latch.countDown();
            });
            subscriber.run();

            for (int i = 0; i < 100; i++) {
                largeBatchPublisher.publish(new IntEventTest(i));
            }

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertEquals(100, eventCount.get());
        } finally {
            largeBatchPublisher.shutdown();
        }
    }

    @Test
    void testBatchEventTimestamp() throws InterruptedException {
        final List<Long> timestamps = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(3);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            timestamps.add(e.getCreatedTimeMillis());
            latch.countDown();
        });
        subscriber.run();

        long beforePublish = System.currentTimeMillis();
        publisher.publish(new StringEventTest("time1"));
        publisher.publish(new StringEventTest("time2"));
        publisher.publish(new StringEventTest("time3"));
        long afterPublish = System.currentTimeMillis();

        assertTrue(latch.await(2, TimeUnit.SECONDS));

        assertEquals(3, timestamps.size());
        for (Long timestamp : timestamps) {
            assertTrue(timestamp >= beforePublish && timestamp <= afterPublish);
        }
    }

    @Test
    void testConcurrentPublishing() throws InterruptedException {
        final AtomicInteger eventCount = new AtomicInteger(0);
        final int threadsCount = 3;
        final int eventsPerThread = 10;
        final int totalEvents = threadsCount * eventsPerThread;
        CountDownLatch latch = new CountDownLatch(totalEvents);

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
                        publisher.publish(new IntEventTest(threadId * eventsPerThread + j));
                    }
                });
            }

            Thread.sleep(500);

            publisher.flush();

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertEquals(totalEvents, eventCount.get());
        } finally {
            testExecutor.shutdown();
        }
    }

    @Test
    void testFlushEmptyBatch() throws InterruptedException {
        final AtomicInteger eventCount = new AtomicInteger(0);

        subscriber.subscribeEvent(StringEventTest.class, e -> eventCount.incrementAndGet());
        subscriber.run();

        assertDoesNotThrow(() -> publisher.flush());

        Thread.sleep(100);
        assertEquals(0, eventCount.get());
    }

    @Test
    void testMultipleFlushCalls() throws InterruptedException {
        final AtomicInteger eventCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            eventCount.incrementAndGet();
            latch.countDown();
        });
        subscriber.run();

        publisher.publish(new StringEventTest("event1"));
        publisher.publish(new StringEventTest("event2"));

        publisher.flush();
        publisher.flush();
        publisher.flush();

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(2, eventCount.get());
    }

    @Test
    void testBatchEventWithErrorInSubscriber() throws InterruptedException {
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);

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

        publisher.publish(new StringEventTest("success1"));
        publisher.publish(new StringEventTest("error"));
        publisher.publish(new StringEventTest("success2"));

        assertTrue(latch.await(3, TimeUnit.SECONDS));

        assertEquals(2, successCount.get());
        assertEquals(1, errorCount.get());
        assertEquals(1, subscriber.getTotalErrorCount());
    }

    @Test
    void testShutdownWithPartialBatch() throws InterruptedException {
        final AtomicInteger eventCount = new AtomicInteger(0);

        subscriber.subscribeEvent(StringEventTest.class, e -> eventCount.incrementAndGet());
        subscriber.run();

        publisher.publish(new StringEventTest("event1"));
        publisher.publish(new StringEventTest("event2"));

        Thread.sleep(100);
        assertEquals(0, eventCount.get());

        publisher.shutdown();

        Thread.sleep(100);
        assertEquals(0, eventCount.get());
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
