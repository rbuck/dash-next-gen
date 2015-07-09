package com.github.rbuck.dash.common;

/**
 * Interface for driver limit and burst rates.
 * // prescription:
 * // * constant-rate (min),
 * // * uniform (frequency/percentile + spike-size) [100*0, 5*1000, max],
 * // * control how quickly come back to base rate
 */
public interface Limiter {
    void consume(int tokens);
}
