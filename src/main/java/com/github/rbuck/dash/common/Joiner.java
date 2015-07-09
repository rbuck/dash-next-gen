package com.github.rbuck.dash.common;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Basic joining utilities.
 */
public class Joiner {

    public static <T> String join(String separator, T[] values) {
        StringBuilder builder = new StringBuilder();
        Iterable parts = (Iterable) Arrays.asList(values);
        Iterator partsIterator = parts.iterator();
        if (partsIterator.hasNext()) {
            builder.append(partsIterator.next());

            while (partsIterator.hasNext()) {
                builder.append(separator);
                builder.append(partsIterator.next());
            }
        }
        return builder.toString();
    }

    public static String join(String separator, int[] array) {
        if (array.length == 0) {
            return "";
        } else {
            StringBuilder builder = new StringBuilder(array.length * 5);
            builder.append(array[0]);
            for (int i = 1; i < array.length; ++i) {
                builder.append(separator).append(array[i]);
            }
            return builder.toString();
        }
    }

}
