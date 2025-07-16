package com.jopencl.Event.EventSubscribers;

import com.jopencl.Event.Event;
import com.jopencl.Event.EventHandler;
import com.jopencl.Event.ProcessingEventSubscriber;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncSubscriberEventSubscriber extends ProcessingEventSubscriber {
    private final ExecutorService executor;

    public AsyncSubscriberEventSubscriber(boolean autoRun) {
        executor = Executors.newFixedThreadPool(1);

        if (autoRun) {
            run();
        }
    }

    public AsyncSubscriberEventSubscriber() {
        this(false);
    }

    @Override
    public void run() {
        if (!isRunning) {
            isRunning = true;
            subscribe();
            executor.submit(this::processEvents);
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
            executor.shutdown();
        }
    }
}
