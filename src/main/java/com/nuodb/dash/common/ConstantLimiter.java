package com.nuodb.dash.common;

import org.isomorphism.util.TokenBucket;
import org.isomorphism.util.TokenBuckets;

import java.util.concurrent.TimeUnit;

import static java.lang.System.getProperties;

/**
 * Created by rbuck on 7/8/15.
 */
public class ConstantLimiter implements Limiter {

    protected static final String MIX_RATES_LIMIT = "dash.workload.rates.limit";
    protected static final String MIX_RATES_BURST = "dash.workload.rates.burst";

    private final TokenBucket tokenBucket;

    public ConstantLimiter() {
        TokenBuckets.Builder builder = new TokenBuckets.Builder()
                .withCapacity(getBurstRate())
                .withFixedIntervalRefillStrategy(getLimitRate(), 1, TimeUnit.SECONDS)
                .withYieldingSleepStrategy();
        tokenBucket = builder.build();
    }

    public long getLimitRate() {
        return PropertiesHelper.getLongProperty(getProperties(), MIX_RATES_LIMIT, 2000);
    }

    public long getBurstRate() {
        return PropertiesHelper.getLongProperty(getProperties(), MIX_RATES_BURST, getLimitRate());
    }

    @Override
    public void consume(int tokens) {
        tokenBucket.consume(tokens);
    }
}
