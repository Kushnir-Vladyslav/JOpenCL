package com.jopencl.Event.EventSubscribers;

import com.jopencl.Event.Event;
import com.jopencl.Event.EventHandler;
import com.jopencl.Event.EventProcessing;

public class SyncEventSubscriber extends EventProcessing {

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

}
