package EventTests.EventPublisherTests;

import com.jopencl.Event.ControlledListFuture;
import com.jopencl.Event.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;

public class ControlledListFutureTest {
    private ControlledListFuture controller;
    private ExecutorService testExecutor;

    @BeforeEach
    void setUp() {
        testExecutor = Executors.newFixedThreadPool(3);
        controller = new ControlledListFuture();
    }

    @Test
    void testDefaultConstructor() {
        assertEquals(Status.RUNNING, controller.getStatus());
        assertEquals(1, controller.getPeriod());
        assertEquals(TimeUnit.SECONDS, controller.getTimeUnit());
    }

    @Test
    void testParameterizedConstructor() {
        controller.stopControlAndShutdown();
        controller = new ControlledListFuture(500, TimeUnit.MILLISECONDS);
        assertEquals(Status.RUNNING, controller.getStatus());
        assertEquals(500, controller.getPeriod());
        assertEquals(TimeUnit.MILLISECONDS, controller.getTimeUnit());
    }

    @Test
    void testConstructorValidation() {
        assertThrows(IllegalArgumentException.class,
                () -> new ControlledListFuture(0, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class,
                () -> new ControlledListFuture(-1, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class,
                () -> new ControlledListFuture(1, null));
    }

    @Test
    void testAddFuture() {
        Future<?> future = testExecutor.submit(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        controller.add(future);
        List<Future<?>> futures = controller.getFutures();

        assertEquals(1, futures.size());
        assertTrue(futures.contains(future));
    }

    @Test
    void testAddNullFuture() {
        assertThrows(IllegalArgumentException.class, () -> controller.add(null));
    }

    @Test
    void testGetFutures(){
        Future<?> future1 = testExecutor.submit(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        Future<?> future2 = testExecutor.submit(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        controller.add(future1);
        controller.add(future2);

        List<Future<?>> futures = controller.getFutures();
        assertEquals(2, futures.size());
        assertTrue(futures.contains(future1));
        assertTrue(futures.contains(future2));

        futures.clear();
        assertEquals(2, controller.getFutures().size());
    }

    @Test
    void testAutomaticCleanup() throws InterruptedException {
        controller.stopControlAndShutdown();
        controller = new ControlledListFuture(100, TimeUnit.MILLISECONDS);

        for (int i = 0; i < 5; i++) {
            Future<?> future = testExecutor.submit(() -> {

            });
            controller.add(future);
        }

        Thread.sleep(50);
        assertEquals(5, controller.getFutures().size());

        Thread.sleep(200);

        assertTrue(controller.getFutures().size() < 5);
    }

    @Test
    void testStopAll() throws InterruptedException {
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Future<?> future = testExecutor.submit(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            futures.add(future);
            controller.add(future);
        }

        assertEquals(3, controller.getFutures().size());

        controller.stopAll();

        Thread.sleep(50);
        for (Future<?> future : futures) {
            assertTrue(future.isCancelled());
        }

        assertEquals(0, controller.getFutures().size());
    }

    @Test
    void testStopControl(){
        Future<?> future = testExecutor.submit(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        controller.add(future);

        assertEquals(Status.RUNNING, controller.getStatus());

        List<Future<?>> returnedFutures = controller.stopControl();

        assertEquals(Status.STOPPED, controller.getStatus());
        assertEquals(1, returnedFutures.size());
        assertTrue(returnedFutures.contains(future));

        assertThrows(IllegalStateException.class, () -> controller.add(future));
        assertThrows(IllegalStateException.class, () -> controller.getFutures());
        assertThrows(IllegalStateException.class, () -> controller.stopAll());
    }

    @Test
    void testStopControlAndShutdown() throws InterruptedException {
        Future<?> future = testExecutor.submit(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        controller.add(future);

        assertEquals(Status.RUNNING, controller.getStatus());

        controller.stopControlAndShutdown();

        assertEquals(Status.STOPPED, controller.getStatus());

        Thread.sleep(50);
        assertTrue(future.isCancelled());

        assertThrows(IllegalStateException.class, () -> controller.add(future));
    }

    @Test
    void testStartProcess() {
        controller.stopControl();

        assertEquals(Status.STOPPED, controller.getStatus());

        controller.startProcess();

        assertEquals(Status.RUNNING, controller.getStatus());

        Future<?> future = testExecutor.submit(() -> {});
        assertDoesNotThrow(() -> controller.add(future));
    }

    @Test
    void testStartProcessWithParameters() {
        controller.stopControl();

        assertEquals(Status.STOPPED, controller.getStatus());

        controller.startProcess(250, TimeUnit.MILLISECONDS);

        assertEquals(Status.RUNNING, controller.getStatus());
        assertEquals(250, controller.getPeriod());
        assertEquals(TimeUnit.MILLISECONDS, controller.getTimeUnit());
    }

    @Test
    void testStartProcessParameterValidation() {
        controller.stopControl();

        assertThrows(IllegalArgumentException.class,
                () -> controller.startProcess(0, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class,
                () -> controller.startProcess(-1, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class,
                () -> controller.startProcess(1, null));
    }

    @Test
    void testSetPeriod(){
        controller.stopControlAndShutdown();
        controller = new ControlledListFuture(1, TimeUnit.SECONDS);

        assertEquals(1, controller.getPeriod());
        assertEquals(TimeUnit.SECONDS, controller.getTimeUnit());

        controller.setPeriod(500, TimeUnit.MILLISECONDS);

        assertEquals(500, controller.getPeriod());
        assertEquals(TimeUnit.MILLISECONDS, controller.getTimeUnit());
        assertEquals(Status.RUNNING, controller.getStatus());
    }

    @Test
    void testSetPeriodValidation() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.setPeriod(0, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class,
                () -> controller.setPeriod(-1, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class,
                () -> controller.setPeriod(1, null));
    }

    @Test
    void testMultipleControllers() {
        ControlledListFuture controller1 = new ControlledListFuture(100, TimeUnit.MILLISECONDS);
        ControlledListFuture controller2 = new ControlledListFuture(200, TimeUnit.MILLISECONDS);

        try {
            assertEquals(Status.RUNNING, controller1.getStatus());
            assertEquals(Status.RUNNING, controller2.getStatus());

            Future<?> future1 = testExecutor.submit(() -> {});
            Future<?> future2 = testExecutor.submit(() -> {});

            controller1.add(future1);
            controller2.add(future2);

            assertEquals(1, controller1.getFutures().size());
            assertEquals(1, controller2.getFutures().size());

            controller1.stopControl();
            assertEquals(Status.STOPPED, controller1.getStatus());
            assertEquals(Status.RUNNING, controller2.getStatus());

            assertDoesNotThrow(() -> controller2.add(testExecutor.submit(() -> {})));

        } finally {
            controller2.stopControlAndShutdown();
        }
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        controller.stopControlAndShutdown();
        controller = new ControlledListFuture(50, TimeUnit.MILLISECONDS);
        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            testExecutor.submit(() -> {
                try {
                    for (int j = 0; j < 10; j++) {
                        Future<?> future = testExecutor.submit(() -> {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        });
                        controller.add(future);
                        Thread.sleep(5);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertTrue(controller.getFutures().size() <= 100);
    }

    @Test
    void testOperationsAfterStop() {
        controller.stopControlAndShutdown();

        assertEquals(Status.STOPPED, controller.getStatus());

        Future<?> future = testExecutor.submit(() -> {});

        assertThrows(IllegalStateException.class, () -> controller.add(future));
        assertThrows(IllegalStateException.class, () -> controller.getFutures());
        assertThrows(IllegalStateException.class, () -> controller.stopAll());
        assertThrows(IllegalStateException.class, () -> controller.stopControl());
        assertThrows(IllegalStateException.class, () -> controller.stopControlAndShutdown());
    }

    @Test
    void testStartProcessWhenRunning() {
        assertEquals(Status.RUNNING, controller.getStatus());

        controller.startProcess();
        assertEquals(Status.RUNNING, controller.getStatus());

        controller.startProcess(300, TimeUnit.MILLISECONDS);
        assertEquals(Status.RUNNING, controller.getStatus());
        assertEquals(300, controller.getPeriod());
        assertEquals(TimeUnit.MILLISECONDS, controller.getTimeUnit());
    }

    @Test
    void testFutureListIsolation() {
        Future<?> future1 = testExecutor.submit(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        Future<?> future2 = testExecutor.submit(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        controller.add(future1);
        controller.add(future2);

        List<Future<?>> futures1 = controller.getFutures();
        List<Future<?>> futures2 = controller.getFutures();

        assertNotSame(futures1, futures2);
        assertEquals(futures1.size(), futures2.size());

        futures1.clear();
        assertEquals(2, controller.getFutures().size());
    }

    @AfterEach
    void tearDown() {
        if (controller != null && controller.getStatus() != Status.STOPPED) {
            controller.stopControlAndShutdown();
        }
        if (testExecutor != null) {
            testExecutor.shutdownNow();
        }
    }
}