package com.github.rbuck.dash.services.cloud;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.rbuck.dash.common.*;
import com.github.rbuck.dash.services.AbstractService;
import com.github.rbuck.dash.services.Context;
import com.github.rbuck.retry.FixedInterval;
import com.github.rbuck.retry.SqlRetryPolicy;

import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import static com.github.rbuck.dash.common.PropertiesHelper.getBooleanProperty;
import static java.lang.System.getProperties;

/**
 * A cloud style workload runner that illustrates:
 * <p>
 * - best practices
 * - scale out and resiliency
 */
public class CloudService extends AbstractService {

    private final MetricRegistry metricRegistry = new MetricRegistry();
    private final MetricsService metricsService = new MetricsService(metricRegistry);

    private SqlRetryPolicy<Boolean> retryPolicy;
    private HashMap<String, Timer> meters;
    private Random random;

    private Dialect dialect;
    private Mix mix;

    public CloudService() {
    }

    class CloudContext implements Context {

        final AtomicLong counter;
        final String identity;

        CloudContext() {
            this.counter = new AtomicLong(0);
            this.identity = SyntheticData.genRandString(15);
        }
    }

    private void createAccount(CloudContext context, Connection connection) throws SQLException {
        try (PreparedStatement putUser = connection.prepareStatement(dialect.getSqlStatement("PUT_ACCOUNT"))) {
            putUser.setString(1, getNextUrn(context)); // unique
            putUser.setString(2, SyntheticData.genRandString(20)); // name
            putUser.setString(3, SyntheticData.genRandString(35)); // description
            putUser.execute();
        }
    }

    private void createContainer(CloudContext context, Connection connection) throws SQLException {
        long time = System.currentTimeMillis();
        String urn = getRandUrn(context);
        if (urn != null) {
            try (PreparedStatement countsPs = connection.prepareStatement(dialect.getSqlStatement("GET_CONTAINER_COUNTS"))) {
                countsPs.setString(1, urn);
                try (ResultSet rs = countsPs.executeQuery()) {
                    if (rs.next()) {
                        long accountId = rs.getLong(1);
                        long permitted = rs.getLong(2);
                        long currently = rs.getLong(3);
                        if (currently < permitted) {
                            try (PreparedStatement insertPs = connection.prepareStatement(dialect.getSqlStatement("PUT_CONTAINER"))) {
                                insertPs.setLong(1, accountId);
                                insertPs.setString(2, genRandContainer(urn));
                                insertPs.setTimestamp(3, new Timestamp(time));
                                insertPs.execute();
                            }
                        }
                    }
                }
            }
        }
    }

    private void createObject(CloudContext context, Connection connection) throws SQLException {
        long time = System.currentTimeMillis();
        String urn = getRandUrn(context);
        if (urn != null) {
            try (PreparedStatement countsPs = connection.prepareStatement(dialect.getSqlStatement("GET_CONTAINER_COUNTS"))) {
                countsPs.setString(1, urn);
                try (ResultSet rsContainerCounts = countsPs.executeQuery()) {
                    if (rsContainerCounts.next()) {
                        long accountId = rsContainerCounts.getLong(1);
                        long permitted = rsContainerCounts.getLong(2);
                        long currently = rsContainerCounts.getLong(3);
                        if (currently > 0) {
                            try (PreparedStatement getContainerIdPs = connection.prepareStatement(dialect.getSqlStatement("GET_RAND_CONTAINER"))) {
                                getContainerIdPs.setLong(1, accountId);
                                try (ResultSet rsCid = getContainerIdPs.executeQuery()) {
                                    if (rsCid.next()) {
                                        long cId = rsContainerCounts.getLong(1);
                                        try (PreparedStatement insertPs = connection.prepareStatement(dialect.getSqlStatement("PUT_OBJECT"))) {
                                            insertPs.setLong(1, cId);
                                            insertPs.setString(2, SyntheticData.genRandUuid());
                                            insertPs.setTimestamp(3, new Timestamp(time));
                                            insertPs.setLong(4, random.nextInt(Integer.MAX_VALUE));
                                            insertPs.setString(5, "application/binary");
                                            insertPs.setString(6, Long.toHexString(random.nextLong()));
                                            insertPs.execute();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void calculateMeanObjectSize(Context context, Connection connection) throws SQLException {
        try (PreparedStatement sizePs = connection.prepareStatement(dialect.getSqlStatement("GET_OBJECT_SIZE"))) {
            try (ResultSet sizeRs = sizePs.executeQuery()) {
                sizeRs.setFetchSize(1000);
                int size = 0;
                double mean = 0;
                while (sizeRs.next()) {
                    int curr = sizeRs.getInt(1);
                    mean += (curr - mean) / size;
                    ++size;
                }
            }
        }
    }

    private void listContainers(CloudContext context, Connection connection) throws SQLException {
        String urn = getRandUrn(context);
        if (urn != null) {
            try (PreparedStatement userIdPs = connection.prepareStatement(dialect.getSqlStatement("GET_ACCOUNT_ID"))) {
                userIdPs.setString(1, urn);
                try (ResultSet userIdResult = userIdPs.executeQuery()) {
                    if (userIdResult.next()) {
                        long accountId = userIdResult.getLong(1);
                        try (PreparedStatement statement = connection.prepareStatement(dialect.getSqlStatement("GET_CONTAINER_LIST"))) {
                            statement.setLong(1, accountId);
                            try (ResultSet rs = statement.executeQuery()) {
                                while (rs.next()) {
                                    rs.getLong(1); // id
                                    rs.getString(2); // name
                                    rs.getTimestamp(3);
                                    rs.getTimestamp(4);
                                    rs.getTimestamp(5); // status
                                    rs.getLong(6); // object count
                                    rs.getLong(7); // bytes used
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void listObjects(CloudContext context, Connection connection) throws SQLException {
        String urn = getRandUrn(context);
        if (urn != null) {
            try (PreparedStatement userIdPs = connection.prepareStatement(dialect.getSqlStatement("GET_ACCOUNT_ID"))) {
                userIdPs.setString(1, urn);
                try (ResultSet userIdResult = userIdPs.executeQuery()) {
                    if (userIdResult.next()) {
                        long accountId = userIdResult.getLong(1);
                        try (PreparedStatement getContainerIdPs = connection.prepareStatement(dialect.getSqlStatement("GET_RAND_CONTAINER"))) {
                            getContainerIdPs.setLong(1, accountId);
                            try (ResultSet rsCid = getContainerIdPs.executeQuery()) {
                                if (rsCid.next()) {
                                    long cId = rsCid.getLong(1);
                                    try (PreparedStatement statement = connection.prepareStatement(dialect.getSqlStatement("GET_OBJECT_LIST"))) {
                                        statement.setLong(1, cId);
                                        try (ResultSet rs = statement.executeQuery()) {
                                            while (rs.next()) {
                                                rs.getLong(1); // id
                                                rs.getString(2); // name
                                                rs.getString(3); // metadata
                                                rs.getLong(4); // size
                                                rs.getString(5); // content_type
                                                rs.getString(6); // etag
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private String getNextUrn(CloudContext context) {
        return context.identity + ":" + context.counter.incrementAndGet();
    }

    private String getRandUrn(CloudContext context) {
        int count = context.counter.intValue();
        return count > 0 ? context.identity + ":" + random.nextInt(context.counter.intValue()) : null;
    }

    private String genRandContainer(String urn) {
        return urn + ":" + SyntheticData.genRandUuid();
    }

    @Override
    public void create() throws Exception {
        super.create();

        // configuration...

        Properties properties = new Properties();
        Resources.loadResource(CloudService.class, "application.properties", properties);
        properties.putAll(getProperties());

        // operational state...

        random = new Random();

        // dialect and data sources...

        try {
            dialect = new Dialect(CloudService.class);
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
            StringBuilder builder = Resources.loadResource(CloudService.class, sqlFile, new StringBuilder());

            // n.b. a new sql script splitter that overcomes issues with
            // existing ones online, and deficiencies in those hard-coded
            // to work with only one database technology.
            SqlScriptSplitter splitter = new SqlScriptSplitter();
            final List<String> statements = splitter.splitStatements(builder.toString());

            try {
                retryPolicy.action(
                        connection -> {
                            for (String sql : statements) {
                                try (Statement statement = connection.createStatement()) {
                                    statement.execute(sql);
                                }
                            }
                            return true;
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
        final CloudContext cloudContext = (CloudContext) context;
        Mix.Type type = mix.next();
        Timer timer = meters.get(type.getTag());
        try (Timer.Context ignore = timer.time()) {
            switch (type.getTag()) {
                case "OLTP_C1": {
                    retryPolicy.action(connection -> {
                        createAccount(cloudContext, connection);
                        return true;
                    });
                }
                break;
                case "OLTP_C2": {
                    retryPolicy.action(connection -> {
                        createContainer(cloudContext, connection);
                        return true;
                    });
                }
                break;
                case "OLTP_C3": {
                    retryPolicy.action(connection -> {
                        createObject(cloudContext, connection);
                        return true;
                    });
                }
                break;
                case "OLTP_R2": {
                    retryPolicy.action(connection -> {
                        listContainers(cloudContext, connection);
                        return true;
                    });
                }
                break;
                case "OLTP_R3": {
                    retryPolicy.action(connection -> {
                        listObjects(cloudContext, connection);
                        return true;
                    });
                }
                break;
                case "OLAP_R3": {
                    retryPolicy.action(connection -> {
                        calculateMeanObjectSize(cloudContext, connection);
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

    @Override
    protected Context createContext() {
        return new CloudContext();
    }

    // U T I L I T I E S

    private void warn(Exception re) {
        System.err.println(Exceptions.toStringAllCauses(re));
    }

    protected int getThreadCount() {
        return super.getThreadCount();
    }
}