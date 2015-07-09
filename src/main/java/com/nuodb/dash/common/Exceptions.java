/*
 * @(#)Exceptions.java  May 30, 2008 4:56:28 PM
 * Copyright 2008 Robert J. Buck All rights reserved.
 */

package com.nuodb.dash.common;

/**
 * Provides general exception utility methods.
 *
 * @author Robert J. Buck
 */
public final class Exceptions {

    /**
     * Prepares an exception for display by concatenating all causation messages
     * into one string. This method could be used for logs where stack traces
     * are to be avoided.
     *
     * @param what the exception for which to prepare a concatenated message
     * @return the concatenated causation message
     */
    public static String toStringAllCauses(Throwable what) {
        return toStringAllCauses(what, Integer.MAX_VALUE);
    }

    /**
     * Prepares an exception for display by concatenating all causation messages
     * into one string. This method could be used for logs where stack traces
     * are to be avoided.
     *
     * @param what  the exception for which to prepare a concatenated message
     * @param depth the stack depth to which causation messages are concatenated
     * @return the concatenated causation message
     */
    public static String toStringAllCauses(Throwable what, int depth) {
        String message = what.getMessage();
        if (depth > 0) {
            Throwable cause = what.getCause();
            if (cause != null) {
                final String reason = toStringAllCauses(cause, depth - 1);
                if (reason != null) {
                    message = message + ": " + reason;
                }
            }
        }
        return message;
    }

}