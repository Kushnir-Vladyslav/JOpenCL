package EventTests.EventManagerTests;

import com.jopencl.Event.EventManager;
import com.jopencl.Event.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EventManagerShutdownTest {
    private EventManager eventManager;

    @BeforeEach
    void setUp() {
        eventManager = EventManager.getInstance();

        if (eventManager.getStatus() != Status.RUNNING) {
            eventManager.run();
        }
    }

    @Test
    void testShutdownPreventsRestart() {
        eventManager.shutdown();
        assertEquals(Status.SHUTDOWN, eventManager.getStatus());

        assertThrows(IllegalStateException.class, () -> eventManager.run());
        assertThrows(IllegalStateException.class, () -> eventManager.pause());
        assertThrows(IllegalStateException.class, () -> eventManager.stop());

        assertThrows(IllegalStateException.class, () -> eventManager.shutdown());
    }
}
