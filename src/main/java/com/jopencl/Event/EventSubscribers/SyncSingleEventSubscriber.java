package com.jopencl.Event.EventSubscribers;

import com.jopencl.Event.Event;
import com.jopencl.Event.ProcessingSingleEventErrorSubscriber;
import com.jopencl.Event.Status;

public class SyncSingleEventSubscriber extends ProcessingSingleEventErrorSubscriber {
    public SyncSingleEventSubscriber(boolean autoRun) {
        if (autoRun) {
            run();
        }
    }

    public SyncSingleEventSubscriber() {
        this(false);
    }

    @Override
    public void run() {
        if (status == Status.CREATED || status == Status.PAUSED || status == Status.STOPPED) {
            status = Status.RUNNING;
            subscribe();
        } else if (status == Status.SHUTDOWN) {
            throw new IllegalStateException("SyncSingleEventSubscriber was already disabled.");
        }
    }

    @SuppressWarnings("unchecked")
    public void processEvents () {
        Event<?> event;

        while ((event = subscriberQueue.poll()) != null) {
            processEvent(event);
        }
    }

    @Override
    public void pause() {
        if (status == Status.RUNNING) {
            status = Status.PAUSED;
            unsubscribe();
        } else if (status == Status.SHUTDOWN) {
            throw new IllegalStateException("SyncSingleEventSubscriber was already disabled.");
        }
    }

    @Override
    public void stop() {
        if (status == Status.RUNNING || status == Status.PAUSED) {
            super.stop();
        } else if (status == Status.SHUTDOWN) {
            throw new IllegalStateException("SyncSingleEventSubscriber was already disabled.");
        }
    }

    @Override
    public void shutdown() {
        if(status != Status.SHUTDOWN) {
            status = Status.SHUTDOWN;
            unsubscribe();
        } else {
            throw new IllegalStateException("SyncSingleEventSubscriber was already disabled.");
        }
    }
}
