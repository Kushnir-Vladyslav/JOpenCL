package com.jopencl.core.memory.buffer;

import com.jopencl.core.memory.data.Data;
import com.jopencl.util.OpenClContext;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract base class for OpenCL buffer management.
 * Provides common functionality for buffer initialization, configuration and resource management.
 *
 * <p>This class implements a builder pattern for buffer configuration and ensures proper
 * resource cleanup. All buffer implementations should extend this class and implement
 * specific buffer operations.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * AbstractBuffer buffer = new ConcreteBuffer()
 *     .setBufferName("MyBuffer")
 *     .setDataClass(FloatData.class)
 *     .setInitSize(1024)
 *     .setOpenClContext(context)
 *     .init();
 * </pre>
 *
 * @since 1.0
 * @author Vladyslav Kushnir
 */
public abstract class AbstractBuffer {
    private static final Logger logger = LoggerFactory.getLogger(AbstractBuffer.class);
    private static final AtomicInteger counter = new AtomicInteger(0);

    private boolean initiated = false;

    private String bufferName;

    private Class<?> clazz = null;
    protected OpenClContext openClContext;

    protected Data dataObject;

    protected ByteBuffer nativeBuffer = null;

    protected int capacity = -1;

    /**
     * Creates a new AbstractBuffer instance with a default name.
     */
    protected AbstractBuffer () {
        this.bufferName  = "UnnamedBuffer" + counter.getAndIncrement();
    }

    /**
     * Checks if the buffer has already been initiated.
     *
     * @throws IllegalStateException if the buffer has already been initiated
     */
    protected void initCheck () {
        if (initiated) {
            String message = String.format("Buffer '%s' has already been initiated", bufferName);
            logger.error(message);
            throw new IllegalStateException(message);
        }
    }

    /**
     * Sets the name of the buffer.
     *
     * @param name the name to set for the buffer
     * @return this buffer instance for method chaining
     * @throws IllegalStateException if the buffer has already been initiated
     * @throws IllegalArgumentException if the name is null or empty
     */
    public AbstractBuffer setBufferName(String name) {
        initCheck();
        if (name == null || name.trim().isEmpty()) {
            String message = "Buffer name cannot be null or empty";
            logger.error(message);
            throw new IllegalArgumentException(message);
        }
        logger.debug("Setting buffer name from '{}' to '{}'", this.bufferName, name);
        bufferName = name.trim();
        return this;
    }

    /**
     * Sets the OpenCL context for this buffer.
     *
     * @param clContext the OpenCL context to use
     * @return this buffer instance for method chaining
     * @throws IllegalStateException if the buffer has already been initiated
     * @throws IllegalArgumentException if the context is null
     */
    public AbstractBuffer setOpenClContext(OpenClContext clContext) {
        initCheck();
        if (clContext == null) {
            String message = String.format("OpenCL context cannot be null for buffer '%s'", bufferName);
            logger.error(message);
            throw new IllegalArgumentException(message);
        }
        logger.debug("Setting OpenCL context for buffer '{}'", bufferName);
        this.openClContext = clContext;
        return this;
    }

    /**
     * Sets the initial size of the buffer.
     *
     * @param newSize the size to initialize the buffer with
     * @return this buffer instance for method chaining
     * @throws IllegalStateException if the buffer has already been initiated
     * @throws IllegalArgumentException if the size is not positive
     */
    public AbstractBuffer setInitSize(int newSize) {
        initCheck();
        if (newSize <= 0) {
            String message = String.format("Buffer size must be positive, got %d for buffer '%s'", newSize, bufferName);
            logger.error(message);
            throw new IllegalArgumentException(message);
        }
        logger.debug("Setting initial size to {} for buffer '{}'", newSize, bufferName);
        this.capacity = newSize;
        return this;
    }

    /**
     * Sets the data class for the buffer.
     *
     * @param <T> the type of data
     * @param newClass the class of data to be stored in the buffer
     * @return this buffer instance for method chaining
     * @throws IllegalStateException if the buffer has already been initiated
     * @throws IllegalArgumentException if the class is null
     */
    public <T extends Data> AbstractBuffer setDataClass(Class<T> newClass) {
        initCheck();
        if (newClass == null) {
            String message = String.format("Data class cannot be null for buffer '%s'", bufferName);
            logger.error(message);
            throw new IllegalArgumentException(message);
        }
        logger.debug("Setting data class to {} for buffer '{}'", newClass.getSimpleName(), bufferName);
        this.clazz = newClass;
        return this;
    }

    /**
     * Initializes the buffer with the configured settings.
     *
     * @throws IllegalStateException if any required settings are missing or invalid
     */
    public final void init() {
        initCheck();
        logger.info("Initializing buffer '{}'", bufferName);

        validateInitialization();

        try {
            initializeDataObject();
            registerWithContext();

            if (this instanceof AdditionalLifecycle additionalLifecycle) {
                try {
                    additionalLifecycle.additionalInit();
                } catch (Exception e) {
                    throw new IllegalStateException(
                            String.format("Additional initialization failed for buffer '%s'", bufferName), e
                    );
                }
            }

            initiated = true;
            logger.info("Successfully initialized buffer '{}'", bufferName);
        } catch (Exception e) {
            String message = String.format("Failed to initialize buffer '%s'", bufferName);
            logger.error(message, e);
            cleanup();
            throw new IllegalStateException(message, e);
        }
    }

    private void validateInitialization() {
        if (bufferName == null) {
            throwInitError("Buffer name cannot be null");
        }
        if (clazz == null) {
            throwInitError("Data class must be set");
        }
        if (capacity < 1) {
            throwInitError("Buffer size must be positive");
        }
        if (openClContext == null) {
            throwInitError("OpenCL context must be set");
        }
    }

    private void initializeDataObject() {
        try {
            dataObject = (Data) clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throwInitError("Failed to instantiate data class: " + e.getMessage());
        }
    }

    private void registerWithContext() {
        try {
            openClContext.bufferManager.registerBuffer(this);
        } catch (Exception e) {
            throwInitError("Failed to register buffer with context: " + e.getMessage());
        }
    }

    protected void throwInitError(String message) {
        String fullMessage = String.format("Initialization error for buffer '%s': %s", bufferName, message);
        logger.error(fullMessage);
        throw new IllegalStateException(fullMessage);
    }

    /**
     * Sets kernel arguments for this buffer.
     *
     * @param targetKernel the kernel to set arguments for
     * @param argIndex the index of the argument to set
     */
    protected abstract void setKernelArg (long targetKernel, int argIndex);

    /**
     * Gets the name of the buffer.
     *
     * @return the buffer name
     */
    public String getBufferName () {
        return bufferName;
    }

    /**
     * Checks if this buffer has been initialized.
     *
     * @return true if the buffer is initialized, false otherwise
     */
    public boolean isInitialized() {
        return initiated;
    }

    /**
     * Gets the capacity of the buffer.
     *
     * @return the buffer capacity
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Gets the data class associated with this buffer.
     *
     * @return the data class
     */
    public Class<?> getDataClass () {
        return clazz;
    }

    /**
     * Releases all resources associated with this buffer.
     * This method is idempotent and can be called multiple times safely.
     */
    public void destroy () {
        if (initiated) {
            logger.info("Destroying buffer '{}'", bufferName);

            if (this instanceof AdditionalLifecycle additionalLifecycle) {
                try {
                    additionalLifecycle.additionalCleanup();
                } catch (Exception e) {
                    logger.error("Error during additional cleanup for buffer '{}'", bufferName, e);
                }
            }

            cleanup();
            initiated = false;
            logger.info("Buffer '{}' destroyed", bufferName);
        }
    }

    private void cleanup() {
        if (nativeBuffer != null) {
            try {
                MemoryUtil.memFree(nativeBuffer);
                nativeBuffer = null;
            } catch (Exception e) {
                logger.error("Error freeing native buffer for '{}'", bufferName, e);
            }
        }

        if (openClContext != null && openClContext.bufferManager != null) {
            try {
                openClContext.bufferManager.remove(this);
            } catch (Exception e) {
                logger.error("Error removing buffer '{}' from context", bufferName, e);
            }
        }

        capacity = -1;
    }
}

