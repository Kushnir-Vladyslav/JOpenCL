package com.jopencl.Event;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class EventSubscriber {
    protected static final EventManager eventManger;
    protected final BlockingQueue<Event> subscriberQueue = new LinkedBlockingQueue<>();

    protected boolean isRunning = false;

    static {
        eventManger = EventManager.getInstance();
    }

    public abstract void run();

    protected void subscribe() {
        eventManger.subscribe(this);
    }

    public void onEvent(Event event) {
        try {
            subscriberQueue.put(event);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected void clearQueue() {
        subscriberQueue.clear();
    }

    public abstract void stop();

    protected void unsubscribe(){
        eventManger.unsubscribe(this);
    }
}
