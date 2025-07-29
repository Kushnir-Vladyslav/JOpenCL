package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.Event;
import com.jopencl.Event.ExecuteEventPublisher;
import com.jopencl.Event.Status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class BatchEventPublisher extends ExecuteEventPublisher {
    private final int batchSize;
    private List<Event<?>> batch = new ArrayList<>();

    public BatchEventPublisher (int batchSize) {
        if (batchSize <= 0) {
            throw new RuntimeException("negative size");
        }
        this.batchSize = batchSize;

        executor = Executors.newFixedThreadPool(1);
    }

    public void publish (Event<?> event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        checkNotShutdown();
        batch.add(event);
        if (batch.size() >= batchSize) {
            flush();
        }
    }

    public void flush () {
        checkNotShutdown();

        List<Event<?>> fullBatch = batch;
        batch = new ArrayList<>();

        executor.submit(() -> {
            for (Event<?> event : fullBatch) {
                try {
                    publishEvent(event);
                } catch (Exception e) {
                    //log
                }
            }
        });
    }
}
