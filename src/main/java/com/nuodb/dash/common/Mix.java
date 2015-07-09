package com.nuodb.dash.common;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Random;

import static com.nuodb.dash.common.Preconditions.checkArgument;
import static com.nuodb.dash.common.PropertiesHelper.getIntegerArrayProperty;
import static com.nuodb.dash.common.PropertiesHelper.getStringArrayProperty;

/**
 * Prescribes a workload mix and identifies the next operation type to perform.
 */
public class Mix implements Iterable<Mix.Type> {

    public static class Type {

        private final int ordinal;
        private final String tag;
        private final int quantumLimit;

        public Type(int ordinal, String tag, int quantumLimit) {
            this.ordinal = ordinal;
            this.tag = tag;
            this.quantumLimit = quantumLimit;
        }

        public int getOrdinal() {
            return ordinal;
        }

        public String getTag() {
            return tag;
        }

        public int getQuantumLimit() {
            return quantumLimit;
        }

    }

    private final Type[] types;

    private final Random random;

    public Mix(Properties properties) {
        random = new Random();

        int[] workloadMix = getIntegerArrayProperty(properties, "dash.workload.mix", new int[0]);
        String[] workloadTag = getStringArrayProperty(properties, "dash.workload.tag", new String[0]);

        checkArgument(workloadMix.length > 0, "Workload mix count must be greater than zero");
        checkArgument(workloadMix.length == workloadTag.length, "Workload mix count must equal workload tag count");
        checkArgument(getSum(workloadMix) == 100, "Workload mix must sum to 100");
        for (int value : workloadMix) {
            checkArgument(value >= 0, "Mix values must be greater than or equal to zero");
        }

        int quantumLimit = 0;
        types = new Type[workloadMix.length];
        for (int ordinal = 0; ordinal < workloadMix.length; ordinal++) {
            quantumLimit += workloadMix[ordinal];
            types[ordinal] = new Type(ordinal, workloadTag[ordinal], quantumLimit);
        }
    }

    @Override
    public Iterator<Type> iterator() {
        return new Iterator<Type>() {

            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < types.length;
            }

            @Override
            public Type next() {
                if (index >= types.length) {
                    throw new NoSuchElementException("Mix.Type illegal index: " + index);
                }
                return types[index++];
            }

            @Override
            public void remove() {
            }
        };
    }

    public Type next() {
        Type result = null;
        assert types.length > 0;
        int quantum = random.nextInt(101);
        for (Type type : types) {
            result = type;
            if (type.getQuantumLimit() > 0 && quantum <= type.getQuantumLimit()) {
                break;
            }
        }
        assert result != null;
        return result;
    }

    public Type getType(int ordinal) {
        if (ordinal < 0 && ordinal >= types.length) {
            throw new IllegalArgumentException("Illegal mix ordinal: " + ordinal);
        }
        return types[ordinal];
    }

    public Type getType(String tag) {
        for (Type type : types) {
            if (type.getTag().equals(tag)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No such mix tag found: " + tag);
    }

    private int getSum(int[] values) {
        int sum = 0;
        for (int value : values) {
            sum += value;
        }
        return sum;
    }
}
