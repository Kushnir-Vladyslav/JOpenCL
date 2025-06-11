package com.jopencl.Event.EventSubscribers;

import com.jopencl.Event.Event;
import com.jopencl.Event.EventSubscriber;

import java.util.*;

public class BufferedEventSubscriber extends EventSubscriber {
    protected Set<Class<?>> eventFiler = new HashSet<>();

    public BufferedEventSubscriber(boolean autoRun) {
        if (autoRun) {
            run();
        }
    }

    public BufferedEventSubscriber() {
        this(true);
    }

    @Override
    public void run() {
        if (!isRunning) {
            isRunning = true;
            subscribe();
        }
    }

    public <T extends Event> void addEventTypes (Class<T> ... eventTypes) {
        for (Class<T> event : eventTypes) {
            if (event != null) {
                eventFiler.add(event);
            }
        }
    }

    public <T extends Event> void removeEventTypes (Class<T> ... eventTypes) {
        for (Class<T> event : eventTypes) {
            if (event != null) {
                eventFiler.remove(event);
            }
        }
    }

    public List<Event> collectAllEvents () {
        List<Event> events = new ArrayList<>();

        subscriberQueue.drainTo(events);

        return events;
    }

    public List<Event> collectEvents () {
        List<Event> events = new ArrayList<>();

        Event event;

        while ((event = subscriberQueue.poll()) != null) {
            if (eventFiler.contains(event.getClass())) {
                events.add(event);
            }
        }

        return events;
    }

    @Override
    public void stop() {
        if(isRunning) {
            isRunning = false;
            unsubscribe();
        }
    }
}
