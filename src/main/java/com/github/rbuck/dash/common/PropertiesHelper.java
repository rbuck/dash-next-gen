package com.github.rbuck.dash.common;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.primitives.Ints;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.github.rbuck.dash.common.TypeValidator.*;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Provides property helper methods.
 */
public class PropertiesHelper {

    /**
     * Get a string property from the application properties file.
     *
     * @param property     the property file key
     * @param defaultValue the default value for the property
     * @return the parsed property
     */
    public static String getStringProperty(Properties properties, String property, String defaultValue) {
        return properties.getProperty(property, defaultValue);
    }

    /**
     * Get an integer property from the application properties file, providing
     * an appropriate error message if it is not an integer.
     *
     * @param property     the property file key
     * @param defaultValue the default value for the property
     * @return the parsed property
     */
    public static int getIntegerProperty(Properties properties, String property, int defaultValue) {
        final String candidate = properties.getProperty(property, Integer.toString(defaultValue));
        checkArgument(isInteger(candidate), "The " + property + " property (" +
                candidate + ") is not an integer; please fix your property declaration.");
        return Integer.parseInt(candidate);
    }

    /**
     * Get a long property from the application properties file, providing
     * an appropriate error message if it is not a long.
     *
     * @param property     the property file key
     * @param defaultValue the default value for the property
     * @return the parsed property
     */
    public static long getLongProperty(Properties properties, String property, long defaultValue) {
        final String candidate = properties.getProperty(property, Long.toString(defaultValue));
        checkArgument(isLong(candidate), "The " + property + " property (" +
                candidate + ") is not an integer; please fix your property declaration.");
        return Long.parseLong(candidate);
    }

    /**
     * Get a double property from the application properties file, providing
     * an appropriate error message if it is not an double.
     *
     * @param property     the property file key
     * @param defaultValue the default value for the property
     * @return the parsed property
     */
    public static double getDoubleProperty(Properties properties, String property, double defaultValue) {
        final String candidate = properties.getProperty(property, Double.toString(defaultValue));
        checkArgument(isDouble(candidate), "The " + property + " property (" +
                candidate + ") is not a double; please fix your property declaration.");
        return Double.parseDouble(candidate);
    }

    /**
     * Get a boolean property from the application properties file, providing
     * an appropriate error message if it is not an boolean.
     *
     * @param property     the property file key
     * @param defaultValue the default value for the property
     * @return the parsed property
     */
    public static boolean getBooleanProperty(Properties properties, String property, boolean defaultValue) {
        final String candidate = properties.getProperty(property, Boolean.toString(defaultValue));
        checkArgument(isBoolean(candidate), "The " + property + " property (" +
                candidate + ") is not a boolean; please fix your property declaration.");
        return Boolean.parseBoolean(candidate);
    }

    /**
     * Get an array of integers from the application properties file, providing
     * an appropriate error message if an element is not an integer.
     *
     * @param properties   the properties list to search
     * @param property     the property file key
     * @param defaultValue the default value for the property
     * @return the parsed property
     */
    public static int[] getIntegerArrayProperty(Properties properties, String property, int[] defaultValue) {
        List<Integer> ints = new ArrayList<>();
        String candidate = properties.getProperty(property, "[" + Ints.join(",", defaultValue) + "]");
        if (candidate.startsWith("[") && candidate.endsWith("]")) {
            candidate = candidate.substring(1, candidate.length() - 1);
        }
        for (String token : Splitter.on(',').split(candidate)) {
            token = token.trim();
            checkArgument(isInteger(token), "The " + property + " property (" +
                    token + ") is not an integer; please fix your property declaration.");
            ints.add(Integer.valueOf(token));
        }
        return Ints.toArray(ints);
    }

    /**
     * Get an array of strings from the application properties file, providing
     * an appropriate error message if an element is not an integer.
     *
     * @param properties   the properties list to search
     * @param property     the property file key
     * @param defaultValue the default value for the property
     * @return the parsed property
     */
    public static String[] getStringArrayProperty(Properties properties, String property, String[] defaultValue) {
        List<String> strings = new ArrayList<>();
        String candidate = properties.getProperty(property, "[" + Joiner.on(",").join(defaultValue) + "]");
        if (candidate.startsWith("[") && candidate.endsWith("]")) {
            candidate = candidate.substring(1, candidate.length() - 1);
        }
        for (String token : candidate.split(",")) {
            strings.add(token.trim());
        }
        String[] empty = {};
        return strings.toArray(empty);
    }

}
