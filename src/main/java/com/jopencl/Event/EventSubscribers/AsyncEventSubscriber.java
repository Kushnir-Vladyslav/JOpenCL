package com.jopencl.Event.EventSubscribers;

import com.jopencl.Event.Event;
import com.jopencl.Event.ProcessingSingleEventErrorSubscriber;
import com.jopencl.Event.Status;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AsyncEventSubscriber extends ProcessingSingleEventErrorSubscriber {
    private final ExecutorService executor;
    private volatile Future<?> dispatchTask;

    public AsyncEventSubscriber(boolean autoRun) {
        executor = Executors.newFixedThreadPool(1);

        if (autoRun) {
            run();
        }
    }

    public AsyncEventSubscriber() {
        this(false);
    }

    @Override
    public void run() {
        if (status == Status.CREATED || status == Status.PAUSED || status == Status.STOPPED) {
            status = Status.RUNNING;
            subscribe();
            dispatchTask = executor.submit(this::processEvents);
        } else if (status == Status.SHUTDOWN) {
            throw new IllegalStateException("AsyncEventSubscriber was already disabled.");
        }
    }

    @SuppressWarnings("unchecked")
    private void processEvents () {
        while (status == Status.RUNNING) {
            try {
                Event<?> event = subscriberQueue.take();
                processEvent(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public void pause() {
        if (status == Status.RUNNING) {
            status = Status.PAUSED;
            unsubscribe();
            dispatchTask.cancel(true);
        } else if (status == Status.SHUTDOWN) {
            throw new IllegalStateException("AsyncEventSubscriber was already disabled.");
        }
    }

    @Override
    public void stop() {
        if (status == Status.RUNNING || status == Status.PAUSED) {
            dispatchTask.cancel(true);
            super.stop();
        } else if (status == Status.SHUTDOWN) {
            throw new IllegalStateException("AsyncEventSubscriber was already disabled.");
        }
    }

    @Override
    public void shutdown() {
        if (status != Status.SHUTDOWN) {
            status = Status.SHUTDOWN;
            unsubscribe();
            executor.shutdownNow();
        } else {
            throw new IllegalStateException("AsyncEventSubscriber was already disabled.");
        }
    }
}
