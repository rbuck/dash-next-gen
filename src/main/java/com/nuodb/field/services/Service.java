package com.nuodb.field.services;

/**
 * Service interface for a continuous test.
 */
public interface Service {

    /**
     * Create the service, do expensive operations etc.
     *
     * @throws Exception see cause for reason
     */
    void create() throws Exception;

    /**
     * Start the service, create is already called.
     *
     * @throws Exception see cause for reason
     */
    void start() throws Exception;

    /**
     * Stop the service.
     */
    void stop();

    /**
     * Destroy the service, tear down.
     */
    void destroy();
}
