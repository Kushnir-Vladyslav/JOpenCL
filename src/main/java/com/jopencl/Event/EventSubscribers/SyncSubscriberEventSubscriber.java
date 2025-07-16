package com.jopencl.Event.EventSubscribers;

import com.jopencl.Event.Event;
import com.jopencl.Event.EventHandler;
import com.jopencl.Event.ProcessingEventSubscriber;

public class SyncSubscriberEventSubscriber extends ProcessingEventSubscriber {

    public SyncSubscriberEventSubscriber(boolean autoRun) {
        if (autoRun) {
            run();
        }
    }

    public SyncSubscriberEventSubscriber() {
        this(false);
    }

    @Override
    public void run() {
        if (!isRunning) {
            isRunning = true;
            subscribe();
        }
    }

    @SuppressWarnings("unchecked")
    public void processEvents () {
        Event event;
        EventHandler<Event> handler;

        while ((event = subscriberQueue.poll()) != null) {
            if ((handler = (EventHandler<Event>) handlers.get(event.getClass())) != null) {
                handler.handle(event);
            }
        }
    }

    @Override
    public void stop() {
        if(isRunning) {
            isRunning = false;
            unsubscribe();
        }
    }
}
