package com.jopencl.Event;

import com.jopencl.Event.Events.ListEvents;

@FunctionalInterface
public interface ListEventsHandler<T extends Event<?>> extends BaseEventHandler<ListEvents<T>> {
}
