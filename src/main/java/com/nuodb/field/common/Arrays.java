package com.nuodb.field.common;

import java.util.List;

/**
 * Basic arrays manipulation.
 */
public class Arrays {

    public static int[] toIntArray(List<Integer> list) {
        int[] ret = new int[list.size()];
        for (int i = 0; i < ret.length; i++)
            ret[i] = list.get(i);
        return ret;
    }

}
