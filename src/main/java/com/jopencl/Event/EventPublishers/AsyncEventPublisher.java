package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.Event;
import com.jopencl.Event.EventPublisher;
import com.jopencl.Event.ExecuteEventPublisher;
import com.jopencl.Event.Status;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncEventPublisher extends ExecuteEventPublisher {
    public AsyncEventPublisher () {
        executor = Executors.newFixedThreadPool(1);
    }

    public void publish(Event<?> event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        checkNotShutdown();
        executor.submit(() -> {
            try {
                publishEvent(event);
            } catch (Exception e) {
                //log
            }

        });
    }
}
