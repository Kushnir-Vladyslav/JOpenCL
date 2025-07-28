package com.jopencl.Event;

@FunctionalInterface
public interface EventErrorHandler {
    void handle(Event<?> event, Exception e);
}
