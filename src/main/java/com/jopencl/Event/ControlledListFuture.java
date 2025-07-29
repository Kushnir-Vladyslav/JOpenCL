package com.jopencl.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ControlledListFuture {
    private final List<Future<?>> futureList = Collections.synchronizedList(new ArrayList<>());
    private ScheduledFuture<?> scheduledFuture;

    private static ScheduledExecutorService scheduler;
    private static final AtomicInteger counterUser = new AtomicInteger(0);
    private static final Object LOCK = new Object();

    long period = 1;
    TimeUnit timeUnit = TimeUnit.SECONDS;
    Status status = Status.CREATED;

    public ControlledListFuture(long period, TimeUnit timeUnit) {
        if (timeUnit == null) {
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }
        if (period <= 0) {
            throw new IllegalArgumentException("Period must be positive");
        }
        this.period = period;
        this.timeUnit = timeUnit;
        start();
    }

    public ControlledListFuture() {
        start();
    }

    public void setPeriod(long period, TimeUnit timeUnit) {
        if (timeUnit == null) {
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }
        if (period <= 0) {
            throw new IllegalArgumentException("Period must be positive");
        }
        this.period = period;
        this.timeUnit = timeUnit;
        if (status == Status.RUNNING) {
            start();
        }
    }

    private void start () {
        synchronized (LOCK) {
            if(scheduler == null) {
                scheduler = Executors.newScheduledThreadPool(1);
            }

            if (scheduledFuture == null) {
                counterUser.incrementAndGet();
            } else {
                scheduledFuture.cancel(true);
            }
        }

        scheduledFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                futureList.removeIf(future -> future.isDone() || future.isCancelled());
                //log
            } catch (Exception e) {
                //log
            }
        }, 0, period, timeUnit);

        status = Status.RUNNING;
    }

    public void startProcess() {
        if (status == Status.STOPPED) {
            start();
        }
    }

    public void startProcess(long period, TimeUnit timeUnit) {
        if (timeUnit == null) {
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }
        if (period <= 0) {
            throw new IllegalArgumentException("Period must be positive");
        }
        this.period = period;
        this.timeUnit = timeUnit;
        start();
    }

    private void checkSNotStop() {
        if(status == Status.STOPPED) {
            //log
            throw new IllegalStateException("ControlledListFuture was already stopped.");
        }
    }

    public Status getStatus () {
        return status;
    }

    public long getPeriod() {
        return period;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void add(Future<?> future) {
        if (future == null) {
            throw new IllegalArgumentException("Future cannot be null");
        }
        checkSNotStop();
        futureList.add(future);
    }

    public List<Future<?>> getFutures() {
        checkSNotStop();
        return new ArrayList<>(futureList);
    }

    public void stopAll () {
        checkSNotStop();
        futureList.forEach(future -> future.cancel(true));
        futureList.clear();
    }

    public void stopControlAndShutdown() {
        //log
        checkSNotStop();
        stopAll();
        scheduledFuture.cancel(true);
        scheduledFuture = null;
        synchronized (LOCK) {
            if(counterUser.decrementAndGet() == 0){
                //log
                scheduler.shutdownNow();
                scheduler = null;
            }
        }
        status = Status.STOPPED;
    }

    public List<Future<?>> stopControl() {
        //log
        checkSNotStop();
        List<Future<?>> list = new ArrayList<>(futureList);
        scheduledFuture.cancel(true);
        scheduledFuture = null;
        synchronized (LOCK) {
            if(counterUser.decrementAndGet() == 0){
                //log
                scheduler.shutdownNow();
                scheduler = null;
            }
        }
        status = Status.STOPPED;
        return list;
    }
}
