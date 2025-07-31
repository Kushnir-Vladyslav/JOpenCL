package com.jopencl.Event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for event publishers that use ScheduledExecutorService.
 * Provides shared timeUnit field for subclasses and handles scheduler shutdown.
 *
 * <p>Subclasses must initialize the scheduler field in their constructor.
 * The scheduler should never be null or changed during the object's lifetime.
 *
 * <p>Note: This class performs immediate shutdown via shutdownNow().
 * Responsibility for graceful task completion lies with the scheduler users.
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-06-11
 * @see EventPublisher
 * @see ScheduledExecutorService
 */
public abstract class ScheduleEventPublisher extends EventPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleEventPublisher.class);

    /** ScheduledExecutorService for subclass scheduling operations - must be initialized in constructor */
    protected ScheduledExecutorService scheduler;

    /** Shared TimeUnit for subclass task scheduling */
    protected TimeUnit timeUnit;

    /**
     * Shuts down this publisher and its ScheduledExecutorService.
     * Performs immediate shutdown of the scheduler using shutdownNow().
     *
     * @throws IllegalStateException if the scheduler was not properly initialized
     * @throws IllegalStateException if this publisher is already shut down
     */
    @Override
    public void shutdown () {
        checkNotShutdown();

        if (scheduler == null) {
            String message = "Scheduler is null in " + this.getClass().getSimpleName() +
                    " - this indicates a programming error in constructor initialization";
            LOGGER.error(message);
            throw new IllegalStateException(message);
        }

        LOGGER.info("Shutting down ScheduleEventPublisher: {}", this.getClass().getSimpleName());

        scheduler.shutdownNow();
        LOGGER.debug("Scheduler shutdown completed for {}", this.getClass().getSimpleName());

        setStatus(Status.SHUTDOWN);
    }
}
