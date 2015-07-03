package com.nuodb.field.services;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A simple container to manage the demo. The container manages service life
 * cycle; it creates, starts, stops, and destroys them. The container itself
 * has one of two states: started and stopped, and may toggle between those.
 */
public class Container {

    private final Lock shutdownLock = new ReentrantLock();
    private final Condition isShutdown = shutdownLock.newCondition();

    public enum Status {
        STARTED,
        STOPPED
    }

    /**
     * Interface for receiving a status change event.
     */
    public interface StatusChangeListener {

        /**
         * This method gets called when a change of status occurs.
         *
         * @param changeEvent The new status
         */
        void onChange(StatusChangeEvent changeEvent);

    }

    /**
     * Represents current container status.
     */
    public class StatusChangeEvent extends java.util.EventObject {

        private final Status status;

        /**
         * Constructs a prototypical Event.
         *
         * @param source The object on which the Event initially occurred.
         * @param status the new container status
         * @throws IllegalArgumentException if source is null.
         */
        public StatusChangeEvent(Object source, Status status) {
            super(source);
            this.status = status;
        }

        @Override
        public Object getSource() {
            return super.getSource();
        }

        public Status getStatus() {
            return status;
        }
    }

    private StatusChangeListener[] statusChangeListeners = new StatusChangeListener[0];

    private Status status;

    private final List<Service> services = new ArrayList<>();

    public Container() {
        setStatus(Status.STOPPED);
    }

    public void start() throws Exception {
        shutdownLock.lock();
        try {
            if (status != Status.STARTED) {
                Service service = loadService();
                service.create();
                service.start();
                setStatus(Status.STARTED);
                // wait till the shutdown condition is signaled...
                isShutdown.await();
            }
        } finally {
            shutdownLock.unlock();
        }
    }

    public void stop() {
        shutdownLock.lock();
        try {
            if (status != Status.STOPPED) {
                for (Service service : services) {
                    service.stop();
                    service.destroy();
                }
                // signal the condition that shutdown has occurred...
                isShutdown.signal();
                setStatus(Status.STOPPED);
            }
        } finally {
            shutdownLock.unlock();
        }
    }

    public synchronized void addStatusChangeListener(StatusChangeListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Attempt to set null container status change event listener");
        }

        // Copy-on-write
        StatusChangeListener[] old = statusChangeListeners;
        statusChangeListeners = new StatusChangeListener[old.length + 1];
        System.arraycopy(old, 0, statusChangeListeners, 0, old.length);
        statusChangeListeners[old.length] = listener;

        startStatusChangeEventDispatchThreadIfNecessary();
    }

    private static Thread statusChangeEventDispatchThread = null;

    private synchronized StatusChangeListener[] statusChangeListeners() {
        return statusChangeListeners;
    }

    private static class EventDispatchThread extends Thread {
        public void run() {
            while (true) {
                // Wait on eventQueue till an event is present
                StatusChangeEvent event;
                synchronized (eventQueue) {
                    try {
                        while (eventQueue.isEmpty()) {
                            eventQueue.wait();
                        }
                        event = eventQueue.remove(0);
                    } catch (InterruptedException e) {
                        // never eat interrupts!
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                // Now we have event & hold no locks; deliver evt to listeners
                Container src = (Container) event.getSource();
                for (StatusChangeListener listener : src.statusChangeListeners()) {
                    listener.onChange(event);
                }
            }
        }
    }

    private static synchronized void startStatusChangeEventDispatchThreadIfNecessary() {
        if (statusChangeEventDispatchThread == null) {
            statusChangeEventDispatchThread = new EventDispatchThread();
            statusChangeEventDispatchThread.setDaemon(true);
            statusChangeEventDispatchThread.start();
        }
    }

    private static final List<StatusChangeEvent> eventQueue = new LinkedList<>();

    private void enqueueStatusChangeEvent(StatusChangeEvent event) {
        if (statusChangeListeners.length != 0) {
            synchronized (eventQueue) {
                eventQueue.add(event);
                eventQueue.notify();
            }
        }
    }

    private void setStatus(Status status) {
        this.status = status;
        enqueueStatusChangeEvent(new StatusChangeEvent(this, status));
    }

    private Service loadService() {
        final String className = System.getProperty("runner.class", "com.nuodb.field.services.cloud.CloudService");
        ServiceLoader<Service> serviceLoader = ServiceLoader.load(Service.class);
        for (Service service : serviceLoader) {
            if (Service.class.isAssignableFrom(service.getClass())) {
                services.add(service);
                return service;
            }
        }
        throw new Error("Could not find class: " + className);
    }

}
