package com.github.rbuck.dash.common;

import org.isomorphism.util.TokenBucket;
import org.isomorphism.util.TokenBuckets;

import java.util.concurrent.TimeUnit;

import static java.lang.System.getProperties;

public class ConstantLimiter implements Limiter {

    private static final String MIX_RATES_LIMIT = "dash.driver.rates.limit";
    private static final String MIX_RATES_BURST = "dash.driver.rates.burst";

    private final TokenBucket tokenBucket;

    public ConstantLimiter() {
        TokenBuckets.Builder builder = new TokenBuckets.Builder()
                .withCapacity(getBurstRate())
                .withFixedIntervalRefillStrategy(getLimitRate(), 1, TimeUnit.SECONDS)
                .withYieldingSleepStrategy();
        tokenBucket = builder.build();
    }

    private long getLimitRate() {
        return PropertiesHelper.getLongProperty(getProperties(), MIX_RATES_LIMIT, 2000);
    }

    private long getBurstRate() {
        return PropertiesHelper.getLongProperty(getProperties(), MIX_RATES_BURST, getLimitRate());
    }

    @Override
    public void consume(int tokens) {
        tokenBucket.consume(tokens);
    }
}
