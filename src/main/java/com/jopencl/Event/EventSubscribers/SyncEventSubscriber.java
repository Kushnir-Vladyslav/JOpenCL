package com.jopencl.Event.EventSubscribers;

import com.jopencl.Event.Event;
import com.jopencl.Event.EventHandler;
import com.jopencl.Event.EventProcessing;

public class SyncEventSubscriber extends EventProcessing {

    public SyncEventSubscriber (boolean autoRun) {
        if (autoRun) {
            run();
        }
    }

    public SyncEventSubscriber () {
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
