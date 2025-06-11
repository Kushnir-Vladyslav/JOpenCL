package com.jopencl.Event.EventSubscribers;

import com.jopencl.Event.Event;
import com.jopencl.Event.EventsHandler;
import com.jopencl.Event.EventSubscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BatchEventSubscriber extends EventSubscriber {
    private Thread dispatchThread;
    private final int batchSize;

    protected Map<Class<? extends Event>, EventsHandler<?>> handlers = new ConcurrentHashMap<>();
    protected Map<Class<? extends Event>, List<Event>> bathes = new ConcurrentHashMap<>();

    public BatchEventSubscriber (int batchSize, boolean autoRun) {
        if (batchSize <= 0) {
            throw new RuntimeException("negative size");
        }
        this.batchSize = batchSize;

        if (autoRun) {
            run();
        }
    }

    public BatchEventSubscriber(int batchSize) {
        this(batchSize, true);
    }

    @Override
    public void run() {
        if (!isRunning) {
            dispatchThread = new Thread(this::processEvents);

            isRunning = true;
            subscribe();
            dispatchThread.start();
        }
    }

    private void processEvents () {
        while (isRunning) {
            try {
                Event event = subscriberQueue.take();

                if (handlers.containsKey(event.getClass())) {
                    List<Event> batch = bathes.computeIfAbsent(event.getClass(), k -> new ArrayList<>());

                    batch.add(event);

                    if (batch.size() >= batchSize) {
                        processBatch(event.getClass(), batch);
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Event> void processBatch(Class<T> eventType, List<Event> batch) {
        EventsHandler<Event> handler = (EventsHandler<Event>) handlers.get(eventType);
        handler.handle(batch);

        batch.clear();
    }

    public <T extends Event> void subscribeEvent (Class<T> eventType, EventsHandler<T> handler) {
        handlers.put(eventType, handler);
    }

    public <T extends Event> void unsubscribeEvent (Class<T> ... eventsType) {
        for (Class<?> eventType : eventsType) {
            handlers.remove(eventType);
        }
    }

    @Override
    public void stop() {
        if (isRunning) {
            isRunning = false;
            unsubscribe();
            dispatchThread.interrupt();

            for (Map.Entry<Class<? extends Event>, List<Event>> event : bathes.entrySet()) {
                processBatch(event.getKey(), event.getValue());
            }
        }
    }
}
