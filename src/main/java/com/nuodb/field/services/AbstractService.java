package com.nuodb.field.services;

import java.text.SimpleDateFormat;
import java.util.concurrent.CountDownLatch;

/**
 * An abstraction for a concurrent business service simulation.
 */
public abstract class AbstractService implements Service {

    enum Status {
        CREATED,
        STARTED,
        STOPPED,
        DESTROYED
    }

    private Thread[] threads;
    private CountDownLatch threadLatch;

    private volatile Status status;

    // P R O P E R T I E S

    protected int getThreadCount() {
        return Runtime.getRuntime().availableProcessors() * 4;
    }

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
        System.out.println("[" + now() + "] created");

        threadLatch = new CountDownLatch(getThreadCount());

        // thread creation...
        Context context = createContext();
        threads = new Thread[getThreadCount()];
        for (int i = 0; i < getThreadCount(); i++) {
            final Context threadContext = context;
            threads[i] = new Thread() {
                @Override
                public void run() {
                    try {
                        while (isExecutable() && !isInterrupted()) {
                            execute(threadContext);
                        }
                    } catch (Error e) {
                        AbstractService.this.stop();
                        throw e;
                    } finally {
                        threadLatch.countDown();
                    }
                }
            };
            context = createContext();
        }

        status = Status.CREATED;
    }

    /**
     * Starts dependent services and threads.
     *
     * @throws Exception if an error occurs during start see its cause for details
     */
    @Override
    public void start() throws Exception {
        System.out.println("[" + now() + "] started");
        for (int i = 0; i < getThreadCount(); i++) {
            threads[i].start();
        }
        status = Status.STARTED;
    }

    @Override
    public void stop() {
        System.out.println("[" + now() + "] stopped");
        for (int i = 0; i < getThreadCount(); i++) {
            threads[i].interrupt();
        }
        status = Status.STOPPED;
    }

    @Override
    public void destroy() {
        System.out.println("[" + now() + "] destroyed");
        try {
            threadLatch.await();
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
        status = Status.DESTROYED;
    }

    // S E R V I C E   M E T H O D S   A N D   H O O K S

    /**
     * Create a concurrency context.
     *
     * @return the context for a thread
     */
    protected abstract Context createContext();

    /**
     * Execute a business service providing the associated context.
     *
     * @param context the business context for the associated activity and thread
     */
    protected abstract void execute(Context context);

    // U T I L I T Y   M E T H O D S

    protected String now() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new java.util.Date());
    }

    protected boolean isExecutable() {
        return status.ordinal() < Status.STOPPED.ordinal();
    }
}
