package com.jopencl.Event;

@FunctionalInterface
public interface EventHandler <T extends Event> {
    void handle(T event);
}
