package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.Event;
import com.jopencl.Event.EventPublisher;

import java.util.function.Predicate;

public class ConditionalEventPublisher extends EventPublisher {
    private Predicate<Event> condition;

    public ConditionalEventPublisher(Predicate<Event> condition) {
        this.condition = condition;
    }

    public void setCondition(Predicate<Event> condition) {
        this.condition = condition;
    }

    public void publish(Event event) {
        if (condition.test(event)) {
            publishEvent(event);
        }
    }

    @Override
    public void shutdown() {}
}
