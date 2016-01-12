package com.github.rbuck.dash.services;

import com.github.rbuck.dash.common.ConstantLimiter;
import com.github.rbuck.dash.common.Limiter;
import com.github.rbuck.dash.common.PropertiesHelper;

import java.text.SimpleDateFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.System.getProperties;

/**
 * An abstraction for a concurrent business service simulation.
 *
 * @see Service
 */
public abstract class AbstractService implements Service {

    protected static final String WORKER_THREADS = "dash.driver.threads";

    enum Status {
        CREATED,
        STARTED,
        STOPPED,
        DESTROYED
    }

    private static final ThreadGroup threadGroup;

    static {
        threadGroup = new ThreadGroup("dash-service-threads");
        threadGroup.setDaemon(true);
    }

    private Thread[] threads;
    private CountDownLatch threadLatch;

    private AtomicReference<Status> status = new AtomicReference<>(Status.DESTROYED);

    // P R O P E R T I E S

    // L I F E C Y C L E   M E T H O D S

    /**
     * Services do not follow the RAII (Resource Acquisition is Initialization)
     * idiom; all major allocations initialization occurs within the create
     * method, all service dependencies are established in this method. Here
     * is where data sources are allocated, where child threads are created,
     * where metrics reporting services are allocated.
     * <p/>
     * The one thing that does not occur here are starting resources such as
     * thread, or starting metrics reporting.
     *
     * @throws Exception if an error occurs during create see its cause for details
     */
    @Override
    public void create() throws Exception {
        if (status.compareAndSet(Status.DESTROYED, Status.CREATED)) {
            System.out.println("[" + now() + "] created");
            threadLatch = new CountDownLatch(getThreadCount());
            threads = new Thread[getThreadCount()];
        }
    }

    /**
     * Starts dependent services and threads.
     *
     * @throws Exception if an error occurs during start see its cause for details
     */
    @Override
    public void start() throws Exception {
        if (status.compareAndSet(Status.CREATED, Status.STARTED)) {
            System.out.println("[" + now() + "] started");

            Context context = createContext();
            final Limiter limiter = createLimiter();
            for (int i = 0; i < getThreadCount(); i++) {
                final Context localContext = context;
                threads[i] = new Thread(threadGroup, Integer.toString(i)) {
                    @Override
                    public void run() {
                        try {
                            while (isExecutable() && !isInterrupted()) {
                                int tokenCount = getTokenCount(localContext, limiter);
                                if (tokenCount > 0) {
                                    limiter.consume(tokenCount); // number of events...
                                    while (tokenCount-- > 0) {
                                        execute(localContext); // represents one event
                                    }
                                }
                            }
                        } catch (Error e) {
                            panic(e);
                        } finally {
                            threadLatch.countDown();
                        }
                    }
                };
                threads[i].start();
                context = createContext();
            }
        }
    }

    void panic(Error e) {
        stop();
        throw e;
    }

    @Override
    public void stop() {
        if (status.compareAndSet(Status.STARTED, Status.STOPPED)) {
            System.out.println("[" + now() + "] stopped");
            for (int i = 0; i < getThreadCount(); i++) {
                threads[i].interrupt();
                threads[i] = null;
            }
        }
    }

    @Override
    public void destroy() {
        stop();
        if (status.compareAndSet(Status.STOPPED, Status.DESTROYED)) {
            System.out.println("[" + now() + "] destroyed");
            try {
                threadLatch.await();
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        }
    }

    // S E R V I C E   M E T H O D S   A N D   H O O K S

    /**
     * Get the count of driver threads to start.
     *
     * @return the driver thread count
     */
    protected int getThreadCount() {
        return PropertiesHelper.getIntegerProperty(getProperties(), WORKER_THREADS, Runtime.getRuntime().availableProcessors() * 4);
    }

    /**
     * Create a concurrency context.
     *
     * @return the context for a thread
     * @see Context
     */
    protected abstract Context createContext();

    /**
     * Execute a business service providing the associated context.
     *
     * @param context the business context for the associated activity and thread
     * @see Context
     */
    protected abstract void execute(Context context);

    /**
     * Creates a rate limiter to governor the driver execution rate.
     *
     * @return the constructed rate limiter
     */
    protected Limiter createLimiter() {
        return new ConstantLimiter();
    }

    protected int getTokenCount(Context context, Limiter limiter) {
        return 1; // constant-rate
    }

    // U T I L I T Y   M E T H O D S

    protected String now() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new java.util.Date());
    }

    protected boolean isExecutable() {
        return status.get().ordinal() < Status.STOPPED.ordinal();
    }
}
