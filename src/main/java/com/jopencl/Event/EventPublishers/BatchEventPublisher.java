package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.Event;
import com.jopencl.Event.EventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BatchEventPublisher extends EventPublisher {
    private final int batchSize;
    private List<Event> batch = new ArrayList<>();

    private final ExecutorService executor;

    public BatchEventPublisher (int batchSize) {
        if (batchSize <= 0) {
            throw new RuntimeException("negative size");
        }
        this.batchSize = batchSize;

        executor = Executors.newFixedThreadPool(1);
    }

    public void publish (Event event) {
        batch.add(event);
        if(batch.size() >= batchSize) {
            flush();
        }
    }

    public void flush () {
        List<Event> fullBatch = batch;
        batch = new ArrayList<>();

        executor.submit( () -> {
           for (Event event : fullBatch) {
               try {
                   publishEvent(event);
               } catch (Exception e) {
                   //log
               }
           }
        });
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }
}
