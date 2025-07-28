package com.jopencl.Event;

@FunctionalInterface
public interface BaseEventHandler <T extends Event<?>>{
    void handle(T event);
}
