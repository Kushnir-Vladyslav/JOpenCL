package com.jopencl.Event;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class EventSubscriber {
    private final BlockingQueue<Event> subscriberQueue;

    protected EventSubscriber() {
        this.subscriberQueue = new LinkedBlockingQueue<>();
    }

    public void onEvent(Event event) {
        try {
            subscriberQueue.put(event);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }



}
