package com.jopencl.Event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public abstract class EventProcessing extends EventSubscriber {
    protected Map<Class<? extends Event>, EventHandler<?>> handlers = new ConcurrentHashMap<>();

    public <T extends Event> void subscribeEvent(Class<T> eventType, EventHandler<T> handler) {
        handlers.put(eventType, handler);
    }

    public <T extends Event> void unsubscribeEvent(Class<T> ... eventsType) {
        for (Class<?> eventType : eventsType) {
            handlers.remove(eventType);
        }
    }

    public void clearSubscribeEvents() {
        handlers.clear();
    }

}
