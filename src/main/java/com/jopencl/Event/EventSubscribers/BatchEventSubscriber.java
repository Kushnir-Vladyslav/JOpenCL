package com.jopencl.Event.EventSubscribers;

import com.jopencl.Event.Event;
import com.jopencl.Event.Events.ListEvents;
import com.jopencl.Event.ProcessingListEventErrorSubscriber;
import com.jopencl.Event.Status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BatchEventSubscriber extends ProcessingListEventErrorSubscriber {
    private final ExecutorService executor;
    private volatile Future<?> dispatchTask;
    private final int batchSize;
    
    protected Map<Class<? extends Event<?>>, List<Event<?>>> batches = new ConcurrentHashMap<>();

    public BatchEventSubscriber (int batchSize, boolean autoRun) {
        if (batchSize <= 0) {
            throw new RuntimeException("negative size");
        }
        this.batchSize = batchSize;

        executor = Executors.newFixedThreadPool(2);

        if (autoRun) {
            run();
        }
    }

    public BatchEventSubscriber (int batchSize) {
        this(batchSize, false);
    }
    

    @Override
    public void run() {
        if (status == Status.CREATED || status == Status.PAUSED || status == Status.STOPPED) {
            status = Status.RUNNING;
            subscribe();
            dispatchTask = executor.submit(this::processEvents);
        } else if (status == Status.SHUTDOWN) {
            throw new IllegalStateException("BatchEventSubscriber was already disabled.");
        }
    }

    private void processEvents () {
        while (status == Status.RUNNING) {
            try {
                Event<?> event = subscriberQueue.take();

                @SuppressWarnings("unchecked")
                Class<Event<?>> eventType = (Class<Event<?>>) event.getClass();

                if (handlers.containsKey(eventType)) {
                    List<Event<?>> batch = batches.computeIfAbsent(eventType, k -> new ArrayList<>());

                    batch.add(event);

                    if (batch.size() >= batchSize) {
                        batch = batches.remove(eventType);
                        processEvent(new ListEvents<>(eventType, batch));
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void flush() {
        if (status != Status.SHUTDOWN) {
            for (Class<? extends Event<?>> eventType : batches.keySet()) {
                List listEvent = batches.remove(eventType);
                executor.submit(() -> processEvent(new ListEvents<>(eventType, listEvent)));
            }
        } else if (status == Status.SHUTDOWN) {
            throw new IllegalStateException("BatchEventSubscriber was already disabled.");
        }
    }

    @Override
    public void pause() {
        if (status == Status.RUNNING) {
            status = Status.PAUSED;
            unsubscribe();
            dispatchTask.cancel(true);
        } else if (status == Status.SHUTDOWN) {
            throw new IllegalStateException("BatchEventSubscriber was already disabled.");
        }
    }

    @Override
    public void stop() {
        if (status == Status.RUNNING || status == Status.PAUSED) {
            dispatchTask.cancel(true);
            super.stop();
        } else if (status == Status.SHUTDOWN) {
            throw new IllegalStateException("BatchEventSubscriber was already disabled.");
        }
    }

    @Override
    public void shutdown() {
        if (status != Status.SHUTDOWN) {
            status = Status.SHUTDOWN;
            unsubscribe();
            executor.shutdownNow();
        } else {
            throw new IllegalStateException("BatchEventSubscriber was already disabled.");
        }
    }
}
