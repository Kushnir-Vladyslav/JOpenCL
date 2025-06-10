package com.jopencl.Event;

import java.util.List;

@FunctionalInterface
public interface EventsHandler <T extends Event> {
    void handle(List<T> event);
}
