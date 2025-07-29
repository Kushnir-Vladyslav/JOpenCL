package EventTests.EventPublisherTests;

import EventTests.TestsEvents.IntEventTest;
import EventTests.TestsEvents.StringEventTest;
import com.jopencl.Event.EventManager;
import com.jopencl.Event.EventPriority;
import com.jopencl.Event.EventPublishers.ConditionalAsyncEventPublisher;
import com.jopencl.Event.EventSubscribers.SyncSingleEventSubscriber;
import com.jopencl.Event.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class ConditionalAsyncEventPublisherTest {
    private ConditionalAsyncEventPublisher publisher;
    private SyncSingleEventSubscriber subscriber;
    private final EventManager eventManager = EventManager.getInstance();

    @BeforeEach
    void setUp() {
        publisher = new ConditionalAsyncEventPublisher(event -> true);
        subscriber = new SyncSingleEventSubscriber();
    }

    @Test
    void testConstructorValidation() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConditionalAsyncEventPublisher(null));
    }

    @Test
    void testSetConditionValidation() {
        assertThrows(IllegalArgumentException.class,
                () -> publisher.setCondition(null));
    }

    @Test
    void testPublisherInitialStatus() {
        assertEquals(Status.RUNNING, publisher.getStatus());
    }

    @Test
    void testPublishWithAcceptAllCondition() throws InterruptedException {
        final AtomicInteger handlerCount = new AtomicInteger(0);
        final AtomicReference<String> receivedData = new AtomicReference<>();

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            handlerCount.incrementAndGet();
            receivedData.set(e.getData());
        });
        subscriber.run();

        StringEventTest event = new StringEventTest("test message");
        publisher.publish(event);

        Thread.sleep(50);
        subscriber.processEvents();

        assertEquals(1, handlerCount.get());
        assertEquals("test message", receivedData.get());
    }

    @Test
    void testPublishWithRejectAllCondition() throws InterruptedException {
        publisher.setCondition(event -> false); // Reject all events

        final AtomicInteger handlerCount = new AtomicInteger(0);

        subscriber.subscribeEvent(StringEventTest.class, e -> handlerCount.incrementAndGet());
        subscriber.run();

        publisher.publish(new StringEventTest("rejected message"));

        Thread.sleep(50);
        subscriber.processEvents();

        assertEquals(0, handlerCount.get());
    }

    @Test
    void testPublishWithSpecificCondition() throws InterruptedException {
        // Only accept events with data containing "important"
        publisher.setCondition(event -> {
            if (event instanceof StringEventTest) {
                return ((StringEventTest) event).getData().contains("important");
            }
            return false;
        });

        final AtomicInteger handlerCount = new AtomicInteger(0);
        final List<String> receivedMessages = new ArrayList<>();

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            handlerCount.incrementAndGet();
            receivedMessages.add(e.getData());
        });
        subscriber.run();

        publisher.publish(new StringEventTest("regular message"));
        publisher.publish(new StringEventTest("important message"));
        publisher.publish(new StringEventTest("another regular message"));
        publisher.publish(new StringEventTest("very important update"));

        Thread.sleep(50);
        subscriber.processEvents();

        assertEquals(2, handlerCount.get());
        assertTrue(receivedMessages.contains("important message"));
        assertTrue(receivedMessages.contains("very important update"));
        assertFalse(receivedMessages.contains("regular message"));
        assertFalse(receivedMessages.contains("another regular message"));
    }

    @Test
    void testPublishWithEventTypeCondition() throws InterruptedException {
        // Only accept StringEventTest events
        publisher.setCondition(event -> event instanceof StringEventTest);

        final AtomicInteger stringCount = new AtomicInteger(0);
        final AtomicInteger intCount = new AtomicInteger(0);

        subscriber.subscribeEvent(StringEventTest.class, e -> stringCount.incrementAndGet());
        subscriber.subscribeEvent(IntEventTest.class, e -> intCount.incrementAndGet());
        subscriber.run();

        publisher.publish(new StringEventTest("accepted"));
        publisher.publish(new IntEventTest(42)); // Should be rejected
        publisher.publish(new StringEventTest("also accepted"));

        Thread.sleep(50);
        subscriber.processEvents();

        assertEquals(2, stringCount.get());
        assertEquals(0, intCount.get());
    }

    @Test
    void testPublishWithPriorityCondition() throws InterruptedException {
        // Only accept high priority events
        publisher.setCondition(event -> event.getPriority() == EventPriority.HIGH);

        StringBuilder processOrder = new StringBuilder();

        subscriber.subscribeEvent(StringEventTest.class, event -> processOrder.append(event.getData()));
        subscriber.run();

        publisher.publish(new StringEventTest("L", EventPriority.LOW));
        publisher.publish(new StringEventTest("H", EventPriority.HIGH));
        publisher.publish(new StringEventTest("M", EventPriority.MEDIUM));
        publisher.publish(new StringEventTest("H2", EventPriority.HIGH));

        Thread.sleep(50);
        subscriber.processEvents();

        assertEquals("HH2", processOrder.toString());
    }

    @Test
    void testPublishNullEvent() {
        assertThrows(IllegalArgumentException.class, () -> publisher.publish(null));
    }

    @Test
    void testConditionChangeAtRuntime() throws InterruptedException {
        final AtomicInteger handlerCount = new AtomicInteger(0);
        final List<String> receivedMessages = new ArrayList<>();

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            handlerCount.incrementAndGet();
            receivedMessages.add(e.getData());
        });
        subscriber.run();

        // Initially accept all events
        publisher.publish(new StringEventTest("message1"));

        Thread.sleep(50);
        subscriber.processEvents();

        assertEquals(1, handlerCount.get());

        // Change condition to reject all
        publisher.setCondition(event -> false);
        publisher.publish(new StringEventTest("message2"));

        Thread.sleep(50);
        subscriber.processEvents();

        assertEquals(1, handlerCount.get()); // Should still be 1

        // Change condition to accept all again
        publisher.setCondition(event -> true);
        publisher.publish(new StringEventTest("message3"));

        Thread.sleep(50);
        subscriber.processEvents();

        assertEquals(2, handlerCount.get());
        assertEquals(new ArrayList<>(Arrays.asList("message1", "message3")), receivedMessages);
    }

    @Test
    void testPublishWithComplexCondition() throws InterruptedException {
        // Accept events that are either high priority OR contain "critical"
        publisher.setCondition(event -> {
            boolean isHighPriority = event.getPriority() == EventPriority.HIGH;
            boolean isCritical = event instanceof StringEventTest &&
                    ((StringEventTest) event).getData().contains("critical");
            return isHighPriority || isCritical;
        });

        final AtomicInteger handlerCount = new AtomicInteger(0);
        final List<String> receivedMessages = new ArrayList<>();

        subscriber.subscribeEvent(StringEventTest.class, e -> {
            handlerCount.incrementAndGet();
            receivedMessages.add(e.getData());
        });
        subscriber.run();

        publisher.publish(new StringEventTest("normal message", EventPriority.LOW));
        publisher.publish(new StringEventTest("critical error", EventPriority.LOW));
        publisher.publish(new StringEventTest("high priority message", EventPriority.HIGH));
        publisher.publish(new StringEventTest("medium message", EventPriority.MEDIUM));

        Thread.sleep(50);
        subscriber.processEvents();

        assertEquals(2, handlerCount.get());
        assertTrue(receivedMessages.contains("critical error"));
        assertTrue(receivedMessages.contains("high priority message"));
    }

    @Test
    void testPublishWithExceptionInCondition() throws InterruptedException {
        // Condition that throws exception for specific events
        publisher.setCondition(event -> {
            if (event instanceof StringEventTest &&
                    "throw".equals(((StringEventTest) event).getData())) {
                throw new RuntimeException("Condition error");
            }
            return true;
        });

        final AtomicInteger handlerCount = new AtomicInteger(0);

        subscriber.subscribeEvent(StringEventTest.class, e -> handlerCount.incrementAndGet());
        subscriber.run();

        // This should not crash the publisher, but the event might not be processed
        assertThrows(RuntimeException.class,
                () -> publisher.publish(new StringEventTest("throw")));

        // Normal events should still work
        assertDoesNotThrow(() -> publisher.publish(new StringEventTest("normal")));

        Thread.sleep(50);
        subscriber.processEvents();

        assertEquals(1, handlerCount.get());
    }

    @Test
    void testPublishAfterShutdown() {
        publisher.shutdown();
        assertEquals(Status.SHUTDOWN, publisher.getStatus());

        assertThrows(IllegalStateException.class,
                () -> publisher.publish(new StringEventTest("after shutdown")));
    }

    @Test
    void testPublisherShutdown() {
        assertEquals(Status.RUNNING, publisher.getStatus());

        assertDoesNotThrow(() -> publisher.shutdown());
        assertEquals(Status.SHUTDOWN, publisher.getStatus());

        assertThrows(IllegalStateException.class, () -> publisher.shutdown());
    }

    @Test
    void testEventIntegrity() throws InterruptedException {
        final List<StringEventTest> receivedEvents = new ArrayList<>();

        subscriber.subscribeEvent(StringEventTest.class, e -> receivedEvents.add(e));
        subscriber.run();

        StringEventTest originalEvent = new StringEventTest("integrity test");
        publisher.publish(originalEvent);

        Thread.sleep(50);
        subscriber.processEvents();

        assertEquals(1, receivedEvents.size());
        StringEventTest receivedEvent = receivedEvents.get(0);

        assertEquals(originalEvent.getData(), receivedEvent.getData());
        assertEquals(originalEvent.getPriority(), receivedEvent.getPriority());
        assertEquals(originalEvent.getCreatedTimeMillis(), receivedEvent.getCreatedTimeMillis());
    }

    @Test
    void testMultipleSubscribersWithCondition() throws InterruptedException {
        publisher.setCondition(event -> event instanceof StringEventTest);

        SyncSingleEventSubscriber subscriber2 = new SyncSingleEventSubscriber();
        final AtomicInteger subscriber1Count = new AtomicInteger(0);
        final AtomicInteger subscriber2Count = new AtomicInteger(0);

        try {
            subscriber.subscribeEvent(StringEventTest.class, e -> subscriber1Count.incrementAndGet());
            subscriber2.subscribeEvent(StringEventTest.class, e -> subscriber2Count.incrementAndGet());

            subscriber.run();
            subscriber2.run();

            publisher.publish(new StringEventTest("accepted"));
            publisher.publish(new IntEventTest(42)); // Should be rejected

            Thread.sleep(50);
            subscriber.processEvents();
            subscriber2.processEvents();

            assertEquals(1, subscriber1Count.get());
            assertEquals(1, subscriber2Count.get());
        } finally {
            subscriber2.shutdown();
        }
    }

    @Test
    void testLargeNumberOfEventsWithCondition() throws InterruptedException {
        // Accept only even numbers
        publisher.setCondition(event -> {
            if (event instanceof IntEventTest) {
                return ((IntEventTest) event).getData() % 2 == 0;
            }
            return false;
        });

        final AtomicInteger eventCount = new AtomicInteger(0);
        final int totalEvents = 100;

        subscriber.subscribeEvent(IntEventTest.class, e -> eventCount.incrementAndGet());
        subscriber.run();

        for (int i = 0; i < totalEvents; i++) {
            publisher.publish(new IntEventTest(i));
        }

        Thread.sleep(100);
        subscriber.processEvents();

        assertEquals(50, eventCount.get()); // Only even numbers (0, 2, 4, ..., 98)
    }

    @Test
    void testConditionWithTimestamp() throws InterruptedException {
        long cutoffTime = System.currentTimeMillis();

        // Accept only events created after cutoff time
        publisher.setCondition(event -> event.getCreatedTimeMillis() > cutoffTime);

        final AtomicInteger handlerCount = new AtomicInteger(0);

        subscriber.subscribeEvent(StringEventTest.class, e -> handlerCount.incrementAndGet());
        subscriber.run();

        Thread.sleep(10); // Ensure some time passes

        publisher.publish(new StringEventTest("after cutoff"));

        Thread.sleep(50);
        subscriber.processEvents();

        assertEquals(1, handlerCount.get());
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
