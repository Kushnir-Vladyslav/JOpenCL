package com.jopencl.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages and controls a list of futures with shared scheduler.
 * Provides functionality to track, monitor and control scheduled tasks across multiple instances.
 *
 * <p>Features:
 * <ul>
 * <li>Shared scheduler across all instances for resource efficiency</li>
 * <li>Periodic cleanup of completed futures</li>
 * <li>Thread-safe operations</li>
 * <li>Configurable cleanup period</li>
 * </ul>
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-07-31
 */
public class ControlledListFuture {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControlledListFuture.class);

    private final List<Future<?>> futureList = Collections.synchronizedList(new ArrayList<>());
    private ScheduledFuture<?> scheduledFuture;

    private static ScheduledExecutorService scheduler;
    private static final AtomicInteger counterUser = new AtomicInteger(0);
    private static final Object LOCK = new Object();

    long period = 1;
    TimeUnit timeUnit = TimeUnit.SECONDS;
    Status status = Status.CREATED;

    /**
     * Creates a new ControlledListFuture with specified cleanup period.
     *
     * @param period the period between cleanup operations
     * @param timeUnit the time unit of the period
     * @throws IllegalArgumentException if timeUnit is null or period is not positive
     */
    public ControlledListFuture(long period, TimeUnit timeUnit) {
        if (timeUnit == null) {
            LOGGER.error("Attempted to create with null TimeUnit");
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }
        if (period <= 0) {
            LOGGER.error("Attempted to create with invalid period: {}", period);
            throw new IllegalArgumentException("Period must be positive");
        }
        this.period = period;
        this.timeUnit = timeUnit;
        start();
    }

    /**
     * Creates a new ControlledListFuture with default cleanup period (1 second).
     */
    public ControlledListFuture() {
        start();
    }

    /**
     * Updates the cleanup period.
     *
     * @param period the new period between cleanup operations
     * @param timeUnit the time unit of the period
     * @throws IllegalArgumentException if timeUnit is null or period is not positive
     */
    public void setPeriod(long period, TimeUnit timeUnit) {
        if (timeUnit == null) {
            LOGGER.error("Attempted to set null TimeUnit");
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }
        if (period <= 0) {
            LOGGER.error("Attempted to set invalid period: {}", period);
            throw new IllegalArgumentException("Period must be positive");
        }

        LOGGER.debug("Updating period to {} {}", period, timeUnit);
        this.period = period;
        this.timeUnit = timeUnit;

        if (status == Status.RUNNING) {
            start();
        }
    }

    private void start () {
        synchronized (LOCK) {
            if(scheduler == null) {
                LOGGER.debug("Initializing shared scheduler");
                scheduler = Executors.newScheduledThreadPool(1);
            }

            if (scheduledFuture == null) {
                counterUser.incrementAndGet();
                LOGGER.debug("New instance created, total users: {}", counterUser.get());
            } else {
                LOGGER.debug("Restarting cleanup task");
                scheduledFuture.cancel(true);
            }
        }

        scheduledFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                futureList.removeIf(future -> future.isDone() || future.isCancelled());
            } catch (Exception e) {
                LOGGER.error("Error during futures cleanup: {}", e.getMessage());
            }
        }, 0, period, timeUnit);

        status = Status.RUNNING;
        LOGGER.debug("Started cleanup task with period {} {}", period, timeUnit);
    }

    /**
     * Restarts the cleanup process if stopped.
     */
    public void startProcess() {
        if (status == Status.STOPPED) {
            LOGGER.info("Restarting stopped process");
            start();
        }
    }

    /**
     * Restarts the cleanup process with new period settings.
     *
     * @param period the new period between cleanup operations
     * @param timeUnit the time unit of the period
     * @throws IllegalArgumentException if timeUnit is null or period is not positive
     */
    public void startProcess(long period, TimeUnit timeUnit) {
        if (timeUnit == null) {
            LOGGER.error("Attempted to restart with null TimeUnit");
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }
        if (period <= 0) {
            LOGGER.error("Attempted to restart with invalid period: {}", period);
            throw new IllegalArgumentException("Period must be positive");
        }

        LOGGER.info("Restarting process with period {} {}", period, timeUnit);
        this.period = period;
        this.timeUnit = timeUnit;
        start();
    }

    private void checkNotStop() {
        if(status == Status.STOPPED) {
            LOGGER.error("Attempted to operate on stopped ControlledListFuture");
            throw new IllegalStateException("ControlledListFuture was already stopped.");
        }
    }

    /**
     * Gets current status.
     *
     * @return current status
     */
    public Status getStatus () {
        return status;
    }

    /**
     * Gets current cleanup period.
     *
     * @return cleanup period
     */
    public long getPeriod() {
        return period;
    }

    /**
     * Gets current time unit.
     *
     * @return time unit
     */
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    /**
     * Adds a future to be monitored.
     *
     * @param future the future to monitor
     * @throws IllegalArgumentException if future is null
     * @throws IllegalStateException if stopped
     */
    public void add(Future<?> future) {
        if (future == null) {
            LOGGER.error("Attempted to add null future");
            throw new IllegalArgumentException("Future cannot be null");
        }

        checkNotStop();

        futureList.add(future);
        LOGGER.debug("Added new future, total futures: {}", futureList.size());
    }

    /**
     * Gets a copy of the monitored futures list.
     *
     * @return list of monitored futures
     * @throws IllegalStateException if stopped
     */
    public List<Future<?>> getFutures() {
        checkNotStop();
        return new ArrayList<>(futureList);
    }

    /**
     * Cancels and removes all monitored futures.
     *
     * @throws IllegalStateException if stopped
     */
    public void stopAll () {
        checkNotStop();

        LOGGER.info("Stopping all futures (count: {})", futureList.size());
        futureList.forEach(future -> future.cancel(true));
        futureList.clear();
    }

    /**
     * Stops monitoring and shuts down if last instance.
     *
     * @throws IllegalStateException if already stopped
     */
    public void stopControlAndShutdown() {
        LOGGER.info("Stopping control and shutting down");

        checkNotStop();

        stopAll();
        scheduledFuture.cancel(true);
        scheduledFuture = null;

        synchronized (LOCK) {
            if(counterUser.decrementAndGet() == 0){
                LOGGER.debug("Decreased user count, remaining: {}", counterUser.decrementAndGet());

                scheduler.shutdownNow();
                scheduler = null;
            }
        }
        status = Status.STOPPED;
    }

    /**
     * Stops monitoring but preserves futures list.
     *
     * @return list of monitored futures
     * @throws IllegalStateException if already stopped
     */
    public List<Future<?>> stopControl() {
        LOGGER.info("Stopping control");

        checkNotStop();

        List<Future<?>> list = new ArrayList<>(futureList);
        scheduledFuture.cancel(true);
        scheduledFuture = null;

        synchronized (LOCK) {
            if(counterUser.decrementAndGet() == 0){
                LOGGER.debug("Decreased user count, remaining: {}", counterUser.decrementAndGet());

                scheduler.shutdownNow();
                scheduler = null;
            }
        }
        status = Status.STOPPED;
        return list;
    }
}
