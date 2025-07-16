package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.Event;
import com.jopencl.Event.EventPublisher;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncEventPublisher extends EventPublisher {
    private final ExecutorService executor;


    public AsyncEventPublisher () {
        executor = Executors.newFixedThreadPool(1);
    }

    public void publish(Event event) {
        executor.submit(() -> {
            publishEvent(event);
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}
