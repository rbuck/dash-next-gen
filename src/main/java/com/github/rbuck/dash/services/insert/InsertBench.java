package com.github.rbuck.dash.services.insert;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.rbuck.dash.common.*;
import com.github.rbuck.dash.services.AbstractService;
import com.github.rbuck.dash.services.Context;
import com.github.rbuck.retry.FixedInterval;
import com.github.rbuck.retry.SqlCallable;
import com.github.rbuck.retry.SqlRetryPolicy;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import static com.github.rbuck.dash.common.PropertiesHelper.getBooleanProperty;
import static com.github.rbuck.dash.common.PropertiesHelper.getIntegerProperty;
import static java.lang.System.getProperties;

/**
 * A cloud style workload runner that illustrates:
 * <p>
 * - best practices
 * - scale out and resiliency
 */
public class InsertBench extends AbstractService {

    private final MetricRegistry metricRegistry = new MetricRegistry();
    private final MetricsService metricsService = new MetricsService(metricRegistry);

    private SqlRetryPolicy<Boolean> retryPolicy;
    private HashMap<String, Timer> meters;

    private Dialect dialect;
    private Mix mix;
    private int recordLimit;

    class SelfContext implements Context {

        final AtomicLong counter;

        SelfContext() {
            this.counter = new AtomicLong(0);
        }
    }

    private long getNextId(SelfContext context) {
        return context.counter.getAndIncrement();
    }

    @Override
    public void create() throws Exception {
        super.create();

        // configuration...

        Properties properties = new Properties();
        Resources.loadResource(InsertBench.class, "application.properties", properties);
        properties.putAll(getProperties());

        // operational state...

        recordLimit = getIntegerProperty(properties, "dash.driver.record.limit", -1);

        // dialect and data sources...

        try {
            dialect = new Dialect(InsertBench.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid dialect.");
        }

        retryPolicy = new SqlRetryPolicy<>(
                new FixedInterval(1, 100),
                new DataSourceContext());

        if (!getBooleanProperty(properties, "dash.db.skip.init", false)) {
            loadDataModel();
        }

        // reporting services...

        mix = new Mix(properties);
        meters = new HashMap<>();
        for (Mix.Type type : mix) {
            meters.put(type.getTag(), metricRegistry.timer(type.getTag()));
        }
    }

    private void loadDataModel() {
        String sqlFile = dialect.getName() + "-dialect-install.sql";
        try {
            StringBuilder builder = Resources.loadResource(InsertBench.class, sqlFile, new StringBuilder());

            // n.b. a new sql script splitter that overcomes issues with
            // existing ones online, and deficiencies in those hard-coded
            // to work with only one database technology.
            SqlScriptSplitter splitter = new SqlScriptSplitter();
            final List<String> statements = splitter.splitStatements(builder.toString());

            try {
                retryPolicy.action(
                        new SqlCallable<Boolean>() {
                            @Override
                            public Boolean call(Connection connection) throws SQLException {
                                for (String sql : statements) {
                                    try (Statement statement = connection.createStatement()) {
                                        statement.execute(sql);
                                    }
                                }
                                return true;
                            }
                        }
                );
            } catch (Exception e) {
                throw new Error("Failed to setup cloud dash.database.", e);
            }
        } catch (IOException e) {
            throw new Error("Failed to find " + sqlFile + " on classpath.", e);
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

    @Override
    public void destroy() {
        super.destroy();
    }

    // T H R E A D E D   S E R V I C E   I N T E R F A C E S

    @Override
    protected void execute(Context context) {
        final SelfContext selfContext = (SelfContext) context;
        Mix.Type type = mix.next();
        Timer timer = meters.get(type.getTag());
        try (Timer.Context ignore = timer.time()) {
            switch (type.getTag()) {
                case "INSERT": {
                    retryPolicy.action(new SqlCallable<Boolean>() {
                        @Override
                        public Boolean call(Connection connection) throws SQLException {
                            doInsert(selfContext, connection);
                            return true;
                        }
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

    private void doInsert(SelfContext selfContext, Connection connection) throws SQLException {
        try (PreparedStatement putUser = connection.prepareStatement(dialect.getSqlStatement("SQL_INSERT"))) {
            final int batchSize = 1000;
            for (int i = 0; i < batchSize; i++) {
                long id = getNextId(selfContext);
                if (id < recordLimit) {
                    putUser.setLong(1, id);
                    putUser.setString(2, SyntheticData.genRandString(32));
                    putUser.setString(3, SyntheticData.genRandString(64));
                    putUser.setString(4, SyntheticData.genRandString(128));
                    putUser.setString(5, SyntheticData.genRandString(256));
                    putUser.setString(6, SyntheticData.genRandString(512));
                    putUser.setString(7, SyntheticData.genRandString(1024));
                    putUser.addBatch();
                } else {
                    break;
                }
            }
            putUser.executeBatch();
        }
    }

    @Override
    protected Context createContext() {
        return new SelfContext();
    }

    // U T I L I T I E S

    private void warn(Exception re) {
        System.err.println(Exceptions.toStringAllCauses(re));
    }

    protected int getThreadCount() {
        return super.getThreadCount();
    }
}