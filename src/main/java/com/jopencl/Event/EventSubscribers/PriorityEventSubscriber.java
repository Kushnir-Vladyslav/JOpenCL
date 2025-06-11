package com.jopencl.Event.EventSubscribers;

import com.jopencl.Event.Event;
import com.jopencl.Event.EventHandler;
import com.jopencl.Event.EventPriority;
import com.jopencl.Event.EventSubscriber;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

public class PriorityEventSubscriber extends EventSubscriber {
    protected Map<Class<? extends Event>, EventHandler<?>> handlers = new ConcurrentHashMap<>();
    protected Map<Class<? extends Event>, EventPriority> priority = new ConcurrentHashMap<>();

    protected final PriorityBlockingQueue<Event> PriorityEventsQueue;

    private Thread dispatchThread;

    public PriorityEventSubscriber (boolean autoRun) {
        PriorityEventsQueue = new PriorityBlockingQueue<>(10, this::priorityComparator);

        if (autoRun) {
            run();
        }
    }

    public PriorityEventSubscriber () {
        this(false);
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

    public <T extends Event> void subscribeEvent(Class<T> eventType, EventPriority eventPriority, EventHandler<T> handler) {
        handlers.put(eventType, handler);
        priority.put(eventType, eventPriority);
    }

    private int priorityComparator (Event event1, Event event2) {
        EventPriority priority1 = priority.get(event1.getClass());
        EventPriority priority2 = priority.get(event2.getClass());

        return priority2.getValue() - priority1.getValue();
    }

    @Override
    public void onEvent(Event event) {
        PriorityEventsQueue.put(event);
    }

    @SuppressWarnings("unchecked")
    private void processEvents () {
        while (isRunning) {
            try {
                Event event = PriorityEventsQueue.take();
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

    public <T extends Event> void unsubscribeEvent(Class<T> ... eventsType) {
        for (Class<?> eventType : eventsType) {
            handlers.remove(eventType);
            priority.remove(eventType);
        }
    }

    public void clearSubscribeEvents() {
        handlers.clear();
        priority.clear();
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
