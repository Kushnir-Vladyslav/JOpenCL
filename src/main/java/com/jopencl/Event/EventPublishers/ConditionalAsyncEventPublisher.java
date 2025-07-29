package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.Event;

import java.util.function.Predicate;

public class ConditionalAsyncEventPublisher extends AsyncEventPublisher {
    private Predicate<Event<?>> condition;

    public ConditionalAsyncEventPublisher(Predicate<Event<?>> condition) {
        if (condition == null) {
            throw new IllegalArgumentException("Condition cannot be null");
        }
        this.condition = condition;
    }

    public void setCondition(Predicate<Event<?>> condition) {
        if (condition == null) {
            throw new IllegalArgumentException("Condition cannot be null");
        }
        checkNotShutdown();
        this.condition = condition;
    }

    public void publish(Event<?> event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (condition.test(event)) {
            super.publish(event);
        }
    }

}
