package com.jopencl.Event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public abstract class ProcessingSingleEventSubscriber extends EventSubscriber {
    protected Map<Class<? extends Event<?>>, SingleEventHandler<? extends Event<?>>> handlers = new ConcurrentHashMap<>();

    public <T extends Event<?>> void subscribeEvent(Class<T> eventType, SingleEventHandler<T> handler) {
        if (eventType == null || handler == null) {
            throw new IllegalArgumentException("Event type and handler cannot be null");
        }
        handlers.put(eventType, handler);
    }

    protected <T extends Event<?>> void processEvent(T event) {
        Class<T> eventType = (Class<T>) event.getClass();
        @SuppressWarnings("unchecked")
        SingleEventHandler<T> handler = (SingleEventHandler<T>) handlers.get(eventType);
        if (handler != null) {
            handler.handle(event);
        }
    }

    public void unsubscribeEvent(Class<?> ... eventsType) {
        if (eventsType == null) {
            return;
        }
        for (Class<?> eventType : eventsType) {
            handlers.remove(eventType);
        }
    }

    public void clearSubscribeEvents() {
        handlers.clear();
    }

    @Override
    public void stop() {
        clearSubscribeEvents();
        super.stop();
    }
}
