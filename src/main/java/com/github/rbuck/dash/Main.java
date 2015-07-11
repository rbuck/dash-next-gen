package com.github.rbuck.dash;

import com.github.rbuck.dash.common.Exceptions;
import com.github.rbuck.dash.common.YamlEnv;
import com.github.rbuck.dash.services.Container;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static java.lang.System.getProperty;

/**
 * Server application main that configures and runs the dash demo.
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
        loadEnvironment(args);

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
            panic(Exceptions.toStringAllCauses(e), e);
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

    private static void loadEnvironment(String[] args) {
        final String defaultConfDir = getProperty("dash.application.conf.dir");
        final String defaultConfFile = (defaultConfDir != null ? defaultConfDir + File.separatorChar : "") + "conf.yml";

        ArgumentParser parser = ArgumentParsers.newArgumentParser("Dash", true)
                .description("Runs a performance test mix.")
                .version("${prog} " + Version.id())
                .epilog("Dash is a free software under Apache License Version 2.0");

        parser.addArgument("-c", "--conf")
                .help("the config file containing the test specification to run (default: ../conf/conf.yml")
                .required(false)
                .setDefault(new File(defaultConfFile))
                .type(File.class);

        parser.addArgument("-t", "--test")
                .help("the name of the test to run")
                .required(true)
                .type(String.class);

        parser.addArgument("-v", "--version")
                .help("print the version number")
                .action(Arguments.version());

        try {
            YamlEnv yamlEnv = new YamlEnv();
            Namespace namespace = parser.parseArgs(args);

            @SuppressWarnings("unchecked")
            HashMap<String, Object> env =
                    (HashMap<String, Object>) yamlEnv.loadProperties((File) namespace.get("conf"));

            String test = namespace.getString("test");
            @SuppressWarnings("unchecked") HashMap<String, Object> testSpec =
                    (HashMap<String, Object>) env.get(test);

            if (testSpec == null) {
                System.err.println("Test spec (" + test + ") does not exist in the config file.");
                throw new Error(); // todo: message or log or exit
            }

            for (Map.Entry<String, Object> entry : testSpec.entrySet()) {
                if (entry.getValue() != null) {
                    System.setProperty(entry.getKey(), entry.getValue().toString());
                }
            }

        } catch (ArgumentParserException e) {
            parser.handleError(e);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
