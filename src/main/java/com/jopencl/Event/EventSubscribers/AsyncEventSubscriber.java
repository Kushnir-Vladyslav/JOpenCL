package com.jopencl.Event.EventSubscribers;

import com.jopencl.Event.Event;
import com.jopencl.Event.EventHandler;
import com.jopencl.Event.EventProcessing;

public class AsyncEventSubscriber extends EventProcessing {
    private final Thread dispatchThread;
    private volatile boolean isRunning;

    public AsyncEventSubscriber () {
        dispatchThread = new Thread(this::processEvents);
        run();
    }

    public void run() {
        if (!isRunning) {
            isRunning = true;

            dispatchThread.start();
        }
    }

    @SuppressWarnings("unchecked")
    private void processEvents () {
        while (isRunning) {
            try {
                Event event = subscriberQueue.take();
                EventHandler<Event> handler;
                if ((handler = (EventHandler<Event>) handlers.get(event.getClass())) != null) {
                    handler.handle(event);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void shutdown() {
        if (isRunning) {
            isRunning = false;
            dispatchThread.interrupt();
        }
    }
}
