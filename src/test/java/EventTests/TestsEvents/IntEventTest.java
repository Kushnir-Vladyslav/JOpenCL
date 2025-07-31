package EventTests.TestsEvents;

import com.jopencl.Event.Event;
import com.jopencl.Event.EventPriority;

public class IntEventTest extends Event<Integer> {
    public IntEventTest (Integer num) {
        data = num;
    }

    public IntEventTest (Integer num, EventPriority priority) {
        data = num;
        this.priority = priority;
    }
}
