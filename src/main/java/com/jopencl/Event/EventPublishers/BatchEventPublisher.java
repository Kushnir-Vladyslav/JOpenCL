package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.Event;
import com.jopencl.Event.EventPublisher;

import java.util.ArrayList;
import java.util.List;

public class BatchEventPublisher extends EventPublisher {
    private final int batchSize;
    private List<Event> batch = new ArrayList<>();

    public BatchEventPublisher (int batchSize) {
        if (batchSize <= 0) {
            throw new RuntimeException("negative size");
        }
        this.batchSize = batchSize;
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

        Thread thread = new Thread( () -> {
           for (Event event : fullBatch) {
               try {
                   publishEvent(event);
               } catch (Exception e) {
                   //log
               }
           }
        });
        thread.start();
    }

}
