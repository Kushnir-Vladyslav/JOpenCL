package EventTests.EventManagerTests.TestEventSubscriber;

import com.jopencl.Event.EventSubscriber;
import com.jopencl.Event.Status;

public class MockEventSubscriber extends EventSubscriber {
    @Override
    public void run() {
        status = Status.RUNNING;
    }

    @Override
    public void pause() {
        status = Status.PAUSED;
    }

    @Override
    public void shutdown() {
        status = Status.SHUTDOWN;
    }
}
