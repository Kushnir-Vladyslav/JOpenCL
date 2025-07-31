package EventTests.EventPublisherTests;

import EventTests.TestsEvents.DoubleEventTest;
import EventTests.TestsEvents.IntEventTest;
import EventTests.TestsEvents.StringEventTest;
import com.jopencl.Event.EventManager;
import com.jopencl.Event.EventPriority;
import com.jopencl.Event.EventPublishers.AsyncEventPublisher;
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

public class AsyncEventPublisherTest {
    private AsyncEventPublisher publisher;
    private AsyncEventSubscriber subscriber;
    private final EventManager eventManager = EventManager.getInstance();

    @BeforeEach
    void setUp() {
        publisher = new AsyncEventPublisher();
        subscriber = new AsyncEventSubscriber();
    }

    @Test
    void testPublisherInitialStatus() {
        assertEquals(Status.RUNNING, publisher.getStatus());
    }

    @Test
    void testPublishSingleEvent() throws InterruptedException {
        StringEventTest event = new StringEventTest("async test message");
        final AtomicInteger handlerCount = new AtomicInteger(0);
        final AtomicReference<String> receivedData = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        subscriber.subscribeEvent(StringEventTest.class, (StringEventTest e) -> {
            handlerCount.incrementAndGet();
            receivedData.set(e.getData());
            latch.countDown();
        });
        subscriber.run();

        publisher.publish(event);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(1, handlerCount.get());
        assertEquals("async test message", receivedData.get());
    }

    @Test
    void testPublishMultipleEvents() throws InterruptedException {
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

        publisher.publish(new StringEventTest("test1"));
        publisher.publish(new StringEventTest("test2"));
        publisher.publish(new IntEventTest(42));
        publisher.publish(new DoubleEventTest(3.14));
        publisher.publish(new StringEventTest("test3"));

        assertTrue(latch.await(3, TimeUnit.SECONDS));

        assertEquals(3, stringCount.get());
        assertEquals(1, intCount.get());
        assertEquals(1, doubleCount.get());
    }

    @Test
    void testPublishEventWithPriority() throws InterruptedException {
        StringBuilder processOrder = new StringBuilder();

        SyncEventSubscriber subscriber2 = new SyncEventSubscriber();

        subscriber2.subscribeEvent(StringEventTest.class, (StringEventTest event) -> processOrder.append(event.getData()));

        subscriber2.run();

        publisher.publish(new StringEventTest("L", EventPriority.LOW));
        publisher.publish(new StringEventTest("H", EventPriority.HIGH));
        publisher.publish(new StringEventTest("M", EventPriority.MEDIUM));

        Thread.sleep(50);

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
            publisher.publish(event);

            assertTrue(latch.await(2, TimeUnit.SECONDS));


            assertEquals(1, subscriber1Count.get());
            assertEquals(1, subscriber2Count.get());
        } finally {
            subscriber2.shutdown();
        }
    }

    @Test
    void testPublishWithNoSubscribers() throws InterruptedException {
        StringEventTest event = new StringEventTest("no subscribers");

        assertDoesNotThrow(() -> publisher.publish(event));

        Thread.sleep(100);
    }

    @Test
    void testPublishNullEvent() {
        assertThrows(IllegalArgumentException.class, () -> publisher.publish(null));
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

        publisher.publish(new StringEventTest("thread test"));

        assertTrue(latch.await(2, TimeUnit.SECONDS));

        assertNotEquals(currentThreadName, processingThreadName.toString());
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

        publisher.publish(stringEvent);
        publisher.publish(intEvent);
        publisher.publish(doubleEvent);

        assertTrue(latch.await(2, TimeUnit.SECONDS));

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
        publisher.publish(new StringEventTest("timestamp test"));
        long afterPublish = System.currentTimeMillis();

        assertTrue(latch.await(2, TimeUnit.SECONDS));

        long timestamp = eventTimestamp.get();
        assertTrue(timestamp >= beforePublish && timestamp <= afterPublish);
    }

    @Test
    void testPublishLargeNumberOfEvents() throws InterruptedException {
        final AtomicInteger eventCount = new AtomicInteger(0);
        final int totalEvents = 100;
        CountDownLatch latch = new CountDownLatch(totalEvents);

        subscriber.subscribeEvent(IntEventTest.class, e -> {
            eventCount.incrementAndGet();
            latch.countDown();
        });
        subscriber.run();

        for (int i = 0; i < totalEvents; i++) {
            publisher.publish(new IntEventTest(i));
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));

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

        publisher.publish(new StringEventTest("before shutdown"));
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(1, handlerCount.get());

        subscriber.shutdown();

        publisher.publish(new StringEventTest("after shutdown"));
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
                () -> publisher.publish(new StringEventTest("after publisher shutdown")));
    }

    @Test
    void testPublishAfterShutdownDoesNotProcessEvents() throws InterruptedException {
        final AtomicInteger handlerCount = new AtomicInteger(0);

        subscriber.subscribeEvent(StringEventTest.class, e -> handlerCount.incrementAndGet());
        subscriber.run();

        publisher.shutdown();

        assertThrows(IllegalStateException.class,
                () -> publisher.publish(new StringEventTest("ignored")));

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
        publisher.publish(originalEvent);

        assertTrue(latch.await(2, TimeUnit.SECONDS));

        assertEquals(1, receivedEvents.size());
        StringEventTest receivedEvent = receivedEvents.get(0);

        assertEquals(originalEvent.getData(), receivedEvent.getData());
        assertEquals(originalEvent.getPriority(), receivedEvent.getPriority());
        assertEquals(originalEvent.getCreatedTimeMillis(), receivedEvent.getCreatedTimeMillis());
    }

    @Test
    void testPublishEventWithErrorInSubscriber() throws InterruptedException {
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);

        subscriber.subscribeEvent(StringEventTest.class,
                (StringEventTest event) -> {
                    if ("error".equals(event.getData())) {
                        latch.countDown();
                        throw new RuntimeException("Test error in subscriber");
                    } else {
                        successCount.incrementAndGet();
                        latch.countDown();
                    }
                },
                (event, exception) -> {
                    errorCount.incrementAndGet();
                }
        );
        subscriber.run();

        publisher.publish(new StringEventTest("success"));

        publisher.publish(new StringEventTest("error"));

        publisher.publish(new StringEventTest("success2"));

        assertTrue(latch.await(3, TimeUnit.SECONDS));

        assertEquals(2, successCount.get());
        assertEquals(1, errorCount.get());
        assertEquals(1, subscriber.getTotalErrorCount());
    }

    @Test
    void testConcurrentPublishing() throws InterruptedException {
        final AtomicInteger eventCount = new AtomicInteger(0);
        final int threadsCount = 5;
        final int eventsPerThread = 20;
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
                        publisher.publish(new IntEventTest(threadId * eventsPerThread + j));
                    }
                });
            }

            assertTrue(latch.await(5, TimeUnit.SECONDS));

            assertEquals(threadsCount * eventsPerThread, eventCount.get());
        } finally {
            testExecutor.shutdown();
        }
    }

    @Test
    void testPublishOrderWithSamePriority() throws InterruptedException {
        final List<Integer> processedOrder = new ArrayList<>();
        final Object lock = new Object();
        final int eventCount = 10;
        CountDownLatch latch = new CountDownLatch(eventCount);

        subscriber.subscribeEvent(IntEventTest.class, e -> {
            synchronized (lock) {
                processedOrder.add(e.getData());
                latch.countDown();
            }
        });
        subscriber.run();

        for (int i = 0; i < eventCount; i++) {
            publisher.publish(new IntEventTest(i));
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS));

        assertEquals(eventCount, processedOrder.size());
        Set<Integer> expectedValues = new HashSet<>();
        Set<Integer> actualValues = new HashSet<>(processedOrder);
        for (int i = 0; i < eventCount; i++) {
            expectedValues.add(i);
        }
        assertEquals(expectedValues, actualValues);
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

        publisher.publish(new StringEventTest("same data"));
        publisher.publish(new StringEventTest("same data"));
        publisher.publish(new StringEventTest("same data"));

        assertTrue(latch.await(2, TimeUnit.SECONDS));

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
                for (int i = 0; i < 100; i++) {
                    publisher.publish(new StringEventTest("event" + i));
                    publishedCount.incrementAndGet();
                    Thread.sleep(10);
                }
            } catch (IllegalStateException e) {
                // Expected when publisher is shut down
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread.sleep(200);
        publisher.shutdown();

        Thread.sleep(300);

        assertTrue(publishedCount.get() > 0);
        assertTrue(publishedCount.get() < 100);

        testExecutor.shutdown();
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
