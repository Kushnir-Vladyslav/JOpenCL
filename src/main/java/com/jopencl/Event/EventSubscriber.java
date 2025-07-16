package com.jopencl.Event;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public abstract class EventSubscriber {
    protected static final EventManager eventManger;
    protected final PriorityBlockingQueue<Event> subscriberQueue = new PriorityBlockingQueue<>(10, Event::priorityComparator);

    protected boolean isRunning = false;

    static {
        eventManger = EventManager.getInstance();
    }

    public abstract void run();

    protected void subscribe() {
        eventManger.subscribe(this);
    }

    public void onEvent(Event event) {
        subscriberQueue.put(event);
    }

    protected void clearQueue() {
        subscriberQueue.clear();
    }

    public abstract void stop();

    protected void unsubscribe(){
        eventManger.unsubscribe(this);
    }
}
