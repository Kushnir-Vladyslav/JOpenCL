package com.jopencl.Event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public abstract class ProcessingSingleEventErrorSubscriber extends ProcessingSingleEventSubscriber {
    protected Map<Class<? extends Event<?>>, EventErrorHandler> errorHandlers = new ConcurrentHashMap<>();

    protected final AtomicLong totalErrorCounter = new AtomicLong (0);
    protected volatile Exception lastException;
    protected volatile Event<?> lastFailedEvent;

    public <T extends Event<?>> void subscribeEvent(Class<T> eventType, SingleEventHandler<T> handler, EventErrorHandler errorHandler) {
        if (errorHandler != null) {
            errorHandlers.put(eventType, errorHandler);
        }
        super.subscribeEvent(eventType, handler);
    }

    @Override
    protected <T extends Event<?>> void processEvent(T event) {
        try {
            super.processEvent(event);
        } catch (Exception e) {
            handleException(event, e);
        }
    }

    @Override
    public void unsubscribeEvent(Class<?> ... eventsType) {
        if (eventsType == null) {
            return;
        }
        for (Class<?> eventType : eventsType) {
            errorHandlers.remove(eventType);
        }
        super.unsubscribeEvent(eventsType);
    }

    @Override
    public void clearSubscribeEvents() {
        errorHandlers.clear();
        super.clearSubscribeEvents();
    }

    protected void handleException(Event<?> event, Exception exception) {
        //log

        totalErrorCounter.incrementAndGet();
        lastException = exception;
        lastFailedEvent = event;

        Class<?> eventType = event.getClass();

        EventErrorHandler errorHandler = errorHandlers.get(eventType);

        if (errorHandler != null) {
            try {
                errorHandler.handle(event, exception);
            } catch (Exception e) {
                //log
            }
        }
    }

    public long getTotalErrorCount() {
        return totalErrorCounter.get();
    }

    public Event<?> getLastFailedEvent() {
        return lastFailedEvent;
    }

    public Exception getLastException() {
        return lastException;
    }

    public void clearErrorStatistics() {
        totalErrorCounter.set(0);
        lastException = null;
        lastFailedEvent = null;
    }

    @Override
    public void stop() {
        clearErrorStatistics();
        clearSubscribeEvents();
        super.stop();
    }
}
