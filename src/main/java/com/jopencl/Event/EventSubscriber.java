package com.jopencl.Event;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class EventSubscriber {
    protected final BlockingQueue<Event> subscriberQueue = new LinkedBlockingQueue<>();

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
}
