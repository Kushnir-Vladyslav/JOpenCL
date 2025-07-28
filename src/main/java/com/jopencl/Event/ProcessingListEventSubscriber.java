package com.jopencl.Event;

import com.jopencl.Event.Events.ListEvents;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public abstract class ProcessingListEventSubscriber extends EventSubscriber {
    protected Map<Class<? extends Event<?>>, ListEventsHandler<? extends Event<?>>> handlers = new ConcurrentHashMap<>();

    public <T extends Event<?>> void subscribeEvent(Class<T> eventType, ListEventsHandler<T> handler) {
        handlers.put(eventType, handler);
    }

    protected <T extends Event<?>> void processEvent(ListEvents<T> event) {
        Class<T> eventType = event.getEventType();
        @SuppressWarnings("unchecked")
        ListEventsHandler<T> handler = (ListEventsHandler<T>) handlers.get(eventType);
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
