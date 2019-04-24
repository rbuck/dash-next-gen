package com.github.rbuck.dash.services.ping;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.rbuck.dash.common.*;
import com.github.rbuck.dash.services.AbstractService;
import com.github.rbuck.dash.services.Context;
import com.github.rbuck.retry.FixedInterval;
import com.github.rbuck.retry.SqlRetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;

import static java.lang.System.getProperties;

public class PingService extends AbstractService {

    private static Logger logger = LoggerFactory.getLogger(PingService.class);

    private final MetricRegistry metricRegistry = new MetricRegistry();
    private final MetricsService metricsService = new MetricsService(metricRegistry);

    private SqlRetryPolicy<Boolean> retryPolicy;
    private HashMap<String, Timer> meters;

    private Dialect dialect;
    private Mix mix;

    @Override
    protected Context createContext() {
        return new PingContext();
    }

    @Override
    public void create() throws Exception {
        super.create();

        // configuration...

        Properties properties = new Properties();
        Resources.loadResource(PingService.class, "application.properties", properties);
        properties.putAll(getProperties());

        // dialect and data sources...

        try {
            dialect = new Dialect(PingService.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid dialect.");
        }

        retryPolicy = new SqlRetryPolicy<>(
                new FixedInterval(1, 100),
                new DataSourceContext());

        // reporting services...

        mix = new Mix(properties);
        meters = new HashMap<>();
        for (Mix.Type type : mix) {
            meters.put(type.getTag(), metricRegistry.timer(type.getTag()));
        }
    }

    @Override
    public void start() throws Exception {
        super.start();
        metricsService.start();
    }

    @Override
    public void stop() {
        try {
            metricsService.close();
        } catch (IOException e) {
            // ignore
        }
        super.stop();
    }

    private void pingServer(PingContext ignore, Connection connection) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(dialect.getProperty("PING"))) {
            try (ResultSet pingResult = query.executeQuery()) {
                if (pingResult.next()) {
                    pingResult.getLong(1);
                }
            }
        }
    }

    @Override
    protected void execute(Context context) {
        final PingService.PingContext pingContext = (PingService.PingContext) context;
        Mix.Type type = mix.next();
        Timer timer = meters.get(type.getTag());
        try (Timer.Context ignore = timer.time()) {
            switch (type.getTag()) {
                case "PING": {
                    retryPolicy.action(connection -> {
                        pingServer(pingContext, connection);
                        return true;
                    });
                }
                break;
                default: {
                    throw new Error("Unknown tag: " + type.getTag());
                }
            }
        } catch (Exception e) {
            warn(e);
        }
    }

    private void warn(Exception re) {
        // don't warn on shutdown!
        if (!(re instanceof InterruptedException)) {
            logger.warn(Exceptions.toStringAllCauses(re));
        }
    }

    // U T I L I T I E S

    class PingContext implements Context {
        PingContext() {
        }
    }

}
