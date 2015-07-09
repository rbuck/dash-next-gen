package com.nuodb.dash.common.metrics;

import com.codahale.metrics.*;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * A reporter which outputs measurements to a {@link PrintStream}, like {@code System.out}.
 */
public class TimerConsoleReporter extends ScheduledReporter {
    /**
     * Returns a new {@link Builder} for {@link TimerConsoleReporter}.
     *
     * @param registry the registry to report
     * @return a {@link Builder} instance for a {@link TimerConsoleReporter}
     */
    public static Builder forRegistry(MetricRegistry registry) {
        return new Builder(registry);
    }

    /**
     * A builder for {@link TimerConsoleReporter} instances. Defaults to using the default locale and
     * time zone, writing to {@code System.out}, converting rates to events/second, converting
     * durations to milliseconds, and not filtering metrics.
     */
    public static class Builder {
        private final MetricRegistry registry;
        private PrintStream output;
        private Locale locale;
        private Clock clock;
        private TimeZone timeZone;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private MetricFilter filter;

        private Builder(MetricRegistry registry) {
            this.registry = registry;
            this.output = System.out;
            this.locale = Locale.getDefault();
            this.clock = Clock.defaultClock();
            this.timeZone = TimeZone.getDefault();
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.filter = MetricFilter.ALL;
        }

        /**
         * Write to the given {@link PrintStream}.
         *
         * @param output a {@link PrintStream} instance.
         * @return {@code this}
         */
        public Builder outputTo(PrintStream output) {
            this.output = output;
            return this;
        }

        /**
         * Format numbers for the given {@link Locale}.
         *
         * @param locale a {@link Locale}
         * @return {@code this}
         */
        public Builder formattedFor(Locale locale) {
            this.locale = locale;
            return this;
        }

        /**
         * Use the given {@link Clock} instance for the time.
         *
         * @param clock a {@link Clock} instance
         * @return {@code this}
         */
        public Builder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * Use the given {@link TimeZone} for the time.
         *
         * @param timeZone a {@link TimeZone}
         * @return {@code this}
         */
        public Builder formattedFor(TimeZone timeZone) {
            this.timeZone = timeZone;
            return this;
        }

        /**
         * Convert rates to the given time unit.
         *
         * @param rateUnit a unit of time
         * @return {@code this}
         */
        public Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        /**
         * Convert durations to the given time unit.
         *
         * @param durationUnit a unit of time
         * @return {@code this}
         */
        public Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        /**
         * Only report metrics which match the given filter.
         *
         * @param filter a {@link MetricFilter}
         * @return {@code this}
         */
        public Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Builds a {@link TimerConsoleReporter} with the given properties.
         *
         * @return a {@link TimerConsoleReporter}
         */
        public TimerConsoleReporter build() {
            return new TimerConsoleReporter(registry,
                    output,
                    locale,
                    clock,
                    timeZone,
                    rateUnit,
                    durationUnit,
                    filter);
        }
    }

    private static final int CONSOLE_WIDTH = 80;

    private final PrintStream output;
    private final Locale locale;

    private TimerConsoleReporter(MetricRegistry registry,
                                 PrintStream output,
                                 Locale locale,
                                 Clock clock,
                                 TimeZone timeZone,
                                 TimeUnit rateUnit,
                                 TimeUnit durationUnit,
                                 MetricFilter filter) {
        super(registry, "console-reporter", filter, rateUnit, durationUnit);
        this.output = output;
        this.locale = locale;
    }

    private int reports = 0;

    @Override
    public void report(SortedMap<String, Gauge> gauges,
                       SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms,
                       SortedMap<String, com.codahale.metrics.Meter> meters,
                       SortedMap<String, Timer> timers) {
        if (!timers.isEmpty()) {
            if (reports % 10 == 0) {
                printWithBanner(getBanner(), '-');
            }
            for (Map.Entry<String, Timer> entry : timers.entrySet()) {
                printTimer(entry.getKey(), entry.getValue());
            }
            reports++;
        }
        output.flush();
    }

    private String getBanner() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        writer.format(locale, "%-12s%-12s%-12s%-9s%-9s%-9s%-9s%-9s%-9s%-9s%-9s%-9s%-9s",
                "Name", "Count", "Rate", "Min", "Max", "Mean", "Std Dev", "Median", "75%", "95%", "98%", "99%", "99.9%");
        return stringWriter.toString();
    }

    private void printTimer(String name, Timer timer) {
        final Snapshot snapshot = timer.getSnapshot();
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        writer.format(locale, "%-12s%-12d%-12d%-9.2f%-9.2f%-9.2f%-9.2f%-9.2f%-9.2f%-9.2f%-9.2f%-9.2f%-9.2f%n",
                name,
                timer.getCount(),
                (long) convertRate(timer.getMeanRate()),
                convertDuration(snapshot.getMin()),
                convertDuration(snapshot.getMax()),
                convertDuration(snapshot.getMean()),
                convertDuration(snapshot.getStdDev()),
                convertDuration(snapshot.getMedian()),
                convertDuration(snapshot.get75thPercentile()),
                convertDuration(snapshot.get95thPercentile()),
                convertDuration(snapshot.get98thPercentile()),
                convertDuration(snapshot.get99thPercentile()),
                convertDuration(snapshot.get999thPercentile()));
        output.append(stringWriter.toString());
    }

    private void printWithBanner(String s, char c) {
        output.print(s);
        output.print(' ');
        for (int i = 0; i < (CONSOLE_WIDTH - s.length() - 1); i++) {
            output.print(c);
        }
        output.println();
    }
}

