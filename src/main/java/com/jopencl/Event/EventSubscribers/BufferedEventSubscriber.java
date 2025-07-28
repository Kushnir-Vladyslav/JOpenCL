package com.jopencl.Event.EventSubscribers;

import com.jopencl.Event.Event;
import com.jopencl.Event.EventSubscriber;
import com.jopencl.Event.Status;

import java.util.*;

public class BufferedEventSubscriber extends EventSubscriber {
    protected Set<Class<?>> eventFilter = new HashSet<>();

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
        if (status == Status.CREATED || status == Status.PAUSED || status == Status.STOPPED) {
            status = Status.RUNNING;
            subscribe();
        } else if (status == Status.SHUTDOWN) {
            throw new IllegalStateException("BufferedEventSubscriber was already disabled.");
        }
    }

    public void subscribeEvent (Class<? extends Event<?>> ... eventTypes) {
        if (eventTypes == null) {
            return;
        }
        for (Class<? extends Event<?>> event : eventTypes) {
            if (event != null) {
                eventFilter.add(event);
            }
        }
    }

    public void unsubscribeEvent (Class<? extends Event<?>> ... eventTypes) {
        if (eventTypes == null) {
            return;
        }
        for (Class<? extends Event<?>> event : eventTypes) {
            if (event != null) {
                eventFilter.remove(event);
            }
        }
    }

    public List<Event<?>> collectAllEvents () {
        List<Event<?>> events = new ArrayList<>();

        subscriberQueue.drainTo(events);

        return events;
    }

    public List<Event<?>> collectEvents () {
        List<Event<?>> events = new ArrayList<>();

        Event<?> event;

        while ((event = subscriberQueue.poll()) != null) {
            if (eventFilter.contains(event.getClass())) {
                events.add(event);
            }
        }

        return events;
    }

    @Override
    public void pause() {
        if (status == Status.RUNNING) {
            status = Status.PAUSED;
            unsubscribe();
        } else if (status == Status.SHUTDOWN) {
            throw new IllegalStateException("BufferedEventSubscriber was already disabled.");
        }
    }

    @Override
    public void stop() {
        if (status == Status.RUNNING || status == Status.PAUSED) {
            super.stop();
        } else if (status == Status.SHUTDOWN) {
            throw new IllegalStateException("BufferedEventSubscriber was already disabled.");
        }
    }

    @Override
    public void shutdown() {
        if(status != Status.SHUTDOWN) {
            status = Status.SHUTDOWN;
            unsubscribe();
        } else {
            throw new IllegalStateException("BufferedEventSubscriber was already disabled.");
        }
    }
}
