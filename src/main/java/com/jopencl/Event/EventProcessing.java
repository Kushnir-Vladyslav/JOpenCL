package com.jopencl.Event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class EventProcessing extends EventSubscriber {
    protected Map<Class<? extends Event>, EventHandler<?>> handlers = new ConcurrentHashMap<>();

    public <T extends Event> void subscribe (Class<T> eventType, EventHandler<T> handler) {
        handlers.put(eventType, handler);
    }

    public <T extends Event> void unsubscribe (Class<T> eventType) {
        handlers.remove(eventType);
    }
}
