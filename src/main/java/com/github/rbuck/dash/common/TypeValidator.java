package com.github.rbuck.dash.common;

/**
 * Validates string representations of values are convertible to target types.
 */
public class TypeValidator {

    /**
     * Determine if the designated string is null.
     *
     * @param str the designated string to test
     * @return true if it is null, false otherwise
     */
    public static boolean isNull(String str) {
        return str == null;
    }

    /**
     * Determine if the designated string is an integer.
     *
     * @param str the designated string to test
     * @return true if it is an integer, false otherwise
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    /**
     * Determine if the designated string is a long integer.
     *
     * @param str the designated string to test
     * @return true if it is a long integer, false otherwise
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean isLong(String str) {
        try {
            Long.parseLong(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    /**
     * Determine if the designated string is a double.
     *
     * @param str the designated string to test
     * @return true if it is an double, false otherwise
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean isDouble(String str) {
        try {
            Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    /**
     * Determine if the designated string is a boolean.
     *
     * @param str the designated string to test
     * @return true if it is a boolean, false otherwise
     */
    public static boolean isBoolean(String str) {
        return "true".equalsIgnoreCase(str) || "false".equalsIgnoreCase (str);
    }
}
