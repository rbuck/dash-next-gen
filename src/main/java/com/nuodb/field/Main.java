package com.nuodb.field;

import com.nuodb.field.services.Container;

import java.util.concurrent.CountDownLatch;

import static com.nuodb.field.common.Exceptions.toStringAllCauses;

/**
 * Server application main that configures and runs the field demo.
 */
public class Main {

    private static void panic(String message, Throwable e) {
        System.err.println(message);
        e.printStackTrace();
    }

    private static void doStop(CountDownLatch latch, Container container) {
        container.stop();
        latch.countDown();
    }

    public static void main(String[] args) {
        final CountDownLatch shutdownCompleteLatch = new CountDownLatch(1);
        final Container container = new Container();
        container.addStatusChangeListener(new Container.StatusChangeListener() {
            @Override
            public void onChange(Container.StatusChangeEvent changeEvent) {
                // if an event occurs that causes the container to shut itself
                // down, also shut down the application. An event could be a
                // failure of some sort, or it could be that the job completed.
                if (Container.Status.STOPPED == changeEvent.getStatus()) {
                    doStop(shutdownCompleteLatch, container);
                }
            }
        });
        Thread shutdownThread = new Thread() {
            @Override
            public void run() {
                // if a control-c occurs, properly halt the container.
                doStop(shutdownCompleteLatch, container);
            }
        };
        // add a control-c hook that stops the container cleanly...
        Runtime.getRuntime().addShutdownHook(shutdownThread);

        try {
            container.start();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception | Error e) {
            panic(toStringAllCauses(e), e);
        } finally {
            try {
                shutdownCompleteLatch.await();
            } catch (InterruptedException e) {
                // ignore
            }
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownThread);
            } catch (IllegalStateException e) {
                // ignore shutdown in progress message
            }
        }
    }
}
