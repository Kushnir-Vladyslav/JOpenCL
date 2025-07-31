package EventTests.TestsEvents;

import com.jopencl.Event.Event;
import com.jopencl.Event.EventPriority;

public class StringEventTest extends Event<String> {
    public StringEventTest (String string) {
        data = string;
    }

    public StringEventTest (String string, EventPriority priority) {
        data = string;
        this.priority = priority;
    }
}
