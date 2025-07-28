package com.jopencl.Event;

@FunctionalInterface
public interface SingleEventHandler<T extends Event<?>> extends BaseEventHandler<T> {
}
