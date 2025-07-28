package com.jopencl.Event;

import java.util.concurrent.PriorityBlockingQueue;

public abstract class EventSubscriber {
    protected static final EventManager eventManger;
    protected final PriorityBlockingQueue<Event<?>> subscriberQueue = new PriorityBlockingQueue<>(10, Event::priorityComparator);

    protected volatile Status status = Status.CREATED;

    static {
        eventManger = EventManager.getInstance();
    }

    public abstract void run();

    protected void subscribe() {
        eventManger.subscribe(this);
    }

    public void onEvent(Event<?> event) {
        subscriberQueue.put(event);
    }

    protected void clearQueue() {
        subscriberQueue.clear();
    }

    public Status getStatus () {
        return status;
    }

    public abstract void pause();

    public void stop() {
        subscriberQueue.clear();
        unsubscribe();
        status = Status.STOPPED;
    }

    public abstract void shutdown();

    protected void unsubscribe(){
        eventManger.unsubscribe(this);
    }
}
