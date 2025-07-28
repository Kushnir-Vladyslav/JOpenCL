package com.jopencl.Event;

import java.time.Duration;
import java.time.Instant;

public abstract class Event <T> {
    protected final Instant createdTime = Instant.now();
    protected EventPriority priority = EventPriority.MEDIUM;
    protected T data;

    public T getData() {
        return data;
    }

    public EventPriority getPriority () {
        return priority;
    }

    public Instant getCreatedTime() {
        return createdTime;
    }

    public long getCreatedTimeMillis() {
        return createdTime.toEpochMilli();
    }

    public long getExistingTimeMillis() {
        return Duration.between(createdTime, Instant.now()).toMillis();
    }

    @Override
    public String toString() {
        return "Event{" +
                "createdTime=" + createdTime +
                ", priority=" + priority +
                ", data=" + data +
                '}';
    }

    static public int priorityComparator (Event<?> event1, Event<?> event2) {
        EventPriority priority1 = event1.getPriority();
        EventPriority priority2 = event2.getPriority();

        return priority2.getValue() - priority1.getValue();
    }
}
