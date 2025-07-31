package EventTests.TestsEvents;

import com.jopencl.Event.Event;
import com.jopencl.Event.EventPriority;

public class DoubleEventTest extends Event<Double> {
    public DoubleEventTest (Double num) {
        data = num;
    }

    public DoubleEventTest (Double num, EventPriority priority) {
        data = num;
        this.priority = priority;
    }
}
