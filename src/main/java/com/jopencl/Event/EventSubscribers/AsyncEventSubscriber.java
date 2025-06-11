package com.jopencl.Event.EventSubscribers;

import com.jopencl.Event.Event;
import com.jopencl.Event.EventHandler;
import com.jopencl.Event.EventProcessing;

public class AsyncEventSubscriber extends EventProcessing {
    private final Thread dispatchThread;

    public AsyncEventSubscriber (boolean autoRun) {
        dispatchThread = new Thread(this::processEvents);
        if (autoRun) {
            run();
        }
    }

    public AsyncEventSubscriber () {
        this(false);
    }

    @Override
    public void run() {
        if (!isRunning) {
            isRunning = true;
            subscribe();
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

    @Override
    public void stop() {
        if (isRunning) {
            isRunning = false;
            unsubscribe();
            dispatchThread.interrupt();
        }
    }
}
