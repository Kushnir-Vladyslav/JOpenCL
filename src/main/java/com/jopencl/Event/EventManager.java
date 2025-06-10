package com.jopencl.Event;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public class EventManager {
    private final BlockingQueue<Event> eventQueue;
    private final List<EventSubscriber> subscribers;
    private final Thread dispatchThread;
    private volatile boolean isRunning;

    private static class EventManagerHolder {
        private static final EventManager INSTANCE = new EventManager();
    }
    private EventManager() {
        this.eventQueue = new LinkedBlockingQueue<>();
        this.subscribers = new CopyOnWriteArrayList<>();

        this.dispatchThread = new Thread(this::dispatchEvents);
    }

    public static EventManager getEventManager () {
        return EventManagerHolder.INSTANCE;
    }

    public void run() {
        this.isRunning = true;

        this.dispatchThread.setName("EventBus-Dispatcher");
        this.dispatchThread.start();
    }


    public void publish(Event event) {
        try {
            eventQueue.put(event);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    public void subscribe(EventSubscriber subscriber) {
        subscribers.add(subscriber);
    }

    public void unsubscribe(EventSubscriber subscriber) {
        subscribers.remove(subscriber);
    }

    private void dispatchEvents() {
        while (isRunning) {
            try {
                Event event = eventQueue.take();
                for (EventSubscriber subscriber : subscribers) {
                    try {
                        subscriber.onEvent(event);
                    } catch (Exception e) {
                        // log
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void shutdown() {
        isRunning = false;
        dispatchThread.interrupt();
    }
}
