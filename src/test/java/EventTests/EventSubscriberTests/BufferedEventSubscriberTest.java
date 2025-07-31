package EventTests.EventSubscriberTests;

import EventTests.TestsEvents.IntEventTest;
import EventTests.TestsEvents.StringEventTest;
import com.jopencl.Event.Event;
import com.jopencl.Event.EventManager;
import com.jopencl.Event.EventPriority;
import com.jopencl.Event.EventSubscribers.BufferedEventSubscriber;
import com.jopencl.Event.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BufferedEventSubscriberTest {
    private BufferedEventSubscriber subscriber;
    private final EventManager eventManager = EventManager.getInstance();

    @BeforeEach
    void setUp() {
        subscriber = new BufferedEventSubscriber(false); // Не запускати автоматично
    }

    @Test
    void testInitialStatus() {
        assertEquals(Status.CREATED, subscriber.getStatus());
        assertEquals(0, eventManager.getSubscriberCount());
    }

    @Test
    void testStatusTransitions() {
        // CREATED -> RUNNING
        subscriber.run();
        assertEquals(Status.RUNNING, subscriber.getStatus());
        assertEquals(1, eventManager.getSubscriberCount());

        // RUNNING -> PAUSED
        subscriber.pause();
        assertEquals(Status.PAUSED, subscriber.getStatus());
        assertEquals(0, eventManager.getSubscriberCount());

        // PAUSED -> RUNNING
        subscriber.run();
        assertEquals(Status.RUNNING, subscriber.getStatus());
        assertEquals(1, eventManager.getSubscriberCount());

        // RUNNING -> STOPPED
        subscriber.stop();
        assertEquals(Status.STOPPED, subscriber.getStatus());
        assertEquals(0, eventManager.getSubscriberCount());

        // STOPPED -> RUNNING
        subscriber.run();
        assertEquals(Status.RUNNING, subscriber.getStatus());
        assertEquals(1, eventManager.getSubscriberCount());

        // RUNNING -> SHUTDOWN
        subscriber.shutdown();
        assertEquals(Status.SHUTDOWN, subscriber.getStatus());
        assertEquals(0, eventManager.getSubscriberCount());
    }

    @Test
    void testOperationsAfterShutdown() {
        subscriber.shutdown();
        assertEquals(Status.SHUTDOWN, subscriber.getStatus());

        assertThrows(IllegalStateException.class, () -> subscriber.run());
        assertThrows(IllegalStateException.class, () -> subscriber.pause());
        assertThrows(IllegalStateException.class, () -> subscriber.stop());
        assertThrows(IllegalStateException.class, () -> subscriber.shutdown());
    }

    @Test
    void testCollectAllEvents() {
        StringEventTest event1 = new StringEventTest("test1");
        StringEventTest event2 = new StringEventTest("test2");
        IntEventTest event3 = new IntEventTest(42);

        subscriber.run();
        subscriber.onEvent(event1);
        subscriber.onEvent(event2);
        subscriber.onEvent(event3);

        List<?> events = subscriber.collectAllEvents();
        assertEquals(3, events.size());
        assertTrue(events.contains(event1));
        assertTrue(events.contains(event2));
        assertTrue(events.contains(event3));

        assertEquals(0, subscriber.collectAllEvents().size());
    }

    @Test
    void testCollectEventsWithFilter() {
        StringEventTest event1 = new StringEventTest("test1");
        StringEventTest event2 = new StringEventTest("test2");
        IntEventTest event3 = new IntEventTest(42);

        subscriber.subscribeEvent(StringEventTest.class);
        subscriber.run();
        subscriber.onEvent(event1);
        subscriber.onEvent(event2);
        subscriber.onEvent(event3);

        List<?> events = subscriber.collectEvents();
        assertEquals(2, events.size());
        assertTrue(events.contains(event1));
        assertTrue(events.contains(event2));
        assertFalse(events.contains(event3));

        assertEquals(0, subscriber.collectEvents().size());
    }

    @Test
    void testCollectEventsWithEmptyFilter() {
        StringEventTest event1 = new StringEventTest("test1");
        IntEventTest event2 = new IntEventTest(42);

        subscriber.run();
        subscriber.onEvent(event1);
        subscriber.onEvent(event2);

        List<?> events = subscriber.collectEvents();
        assertEquals(0, events.size());

        subscriber.onEvent(event1);
        subscriber.onEvent(event2);

        events = subscriber.collectAllEvents();
        assertEquals(2, events.size());
        assertTrue(events.contains(event1));
        assertTrue(events.contains(event2));
    }

    @Test
    void testAddAndRemoveEventTypes() {
        StringEventTest event1 = new StringEventTest("test1");
        IntEventTest event2 = new IntEventTest(42);

        subscriber.subscribeEvent(StringEventTest.class, IntEventTest.class);
        subscriber.run();
        subscriber.onEvent(event1);
        subscriber.onEvent(event2);

        List<?> events = subscriber.collectEvents();
        assertEquals(2, events.size());
        assertTrue(events.contains(event1));
        assertTrue(events.contains(event2));

        subscriber.unsubscribeEvent(StringEventTest.class);
        subscriber.onEvent(event1);
        subscriber.onEvent(event2);

        events = subscriber.collectEvents();
        assertEquals(1, events.size());
        assertFalse(events.contains(event1));
        assertTrue(events.contains(event2));
    }


    @Test
    void testStopClearsQueue() {
        StringEventTest event1 = new StringEventTest("test1");
        StringEventTest event2 = new StringEventTest("test2");

        subscriber.subscribeEvent(StringEventTest.class);
        subscriber.run();
        subscriber.onEvent(event1);
        subscriber.onEvent(event2);

        subscriber.stop();
        assertEquals(Status.STOPPED, subscriber.getStatus());
        assertEquals(0, subscriber.collectAllEvents().size()); // Черга очищена
    }

    @Test
    void testMultipleRunCalls() {
        assertDoesNotThrow(() -> {
            subscriber.run();
            subscriber.run();
            subscriber.run();
        });
        assertEquals(Status.RUNNING, subscriber.getStatus());
        assertEquals(1, eventManager.getSubscriberCount());
    }

    @Test
    void testMultiplePauseCalls() {
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
    void testMultipleStopCalls() {
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
    void testEventPriorityOrder() {
        StringEventTest high = new StringEventTest("high", EventPriority.HIGH);
        StringEventTest medium = new StringEventTest("medium", EventPriority.MEDIUM);
        StringEventTest low = new StringEventTest("low", EventPriority.LOW);

        subscriber.subscribeEvent(StringEventTest.class);
        subscriber.run();
        subscriber.onEvent(low);
        subscriber.onEvent(high);
        subscriber.onEvent(medium);

        List<Event<?>> events = subscriber.collectEvents();
        assertEquals(3, events.size());
        assertEquals("high", events.get(0).getData());
        assertEquals("medium", events.get(1).getData());
        assertEquals("low", events.get(2).getData());
    }

    @Test
    void testNullEventTypesHandling() {
        assertDoesNotThrow(() -> subscriber.subscribeEvent(null));
        assertDoesNotThrow(() -> subscriber.subscribeEvent(StringEventTest.class, null));
        assertDoesNotThrow(() -> subscriber.unsubscribeEvent(null));
        assertDoesNotThrow(() -> subscriber.unsubscribeEvent(StringEventTest.class, null));
    }

    @Test
    void testEmptyQueueCollection() {
        assertDoesNotThrow(() -> subscriber.collectAllEvents());
        assertDoesNotThrow(() -> subscriber.collectEvents());
        assertEquals(0, subscriber.collectAllEvents().size());
        assertEquals(0, subscriber.collectEvents().size());
    }

    @AfterEach
    void tearDown() {
        if (subscriber.getStatus() != Status.SHUTDOWN) {
            subscriber.shutdown();
        }
    }
}