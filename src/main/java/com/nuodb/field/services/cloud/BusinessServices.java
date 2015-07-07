package com.nuodb.field.services.cloud;

import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.nuodb.field.common.*;
import com.nuodb.field.common.metrics.TimerConsoleReporter;
import com.nuodb.field.services.AbstractService;
import com.nuodb.field.services.Context;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.nuodb.field.common.Resources.loadResource;
import static com.nuodb.field.common.SyntheticData.genRandString;
import static com.nuodb.field.common.SyntheticData.genRandUuid;
import static java.lang.System.getProperties;

/**
 * A cloud style workload runner that illustrates:
 * <p/>
 * - best practices
 * - scale out and resiliency
 */
public class BusinessServices extends AbstractService {

    private MetricRegistry metricRegistry;
    private HashMap<String, Timer> meters;
    private TimerConsoleReporter consoleReporter;
    private CsvReporter csvReporter;
    private Random random;

    private Dialect dialect;
    private DataSource dataSource;
    private Mix mix;

    public interface SqlCallable<V> {
        V call(Connection connection) throws SQLException;
    }

    class BusinessContext implements Context {

        final AtomicLong counter;
        final String identity;

        BusinessContext() {
            this.counter = new AtomicLong(0);
            this.identity = genRandString(15);
        }
    }

    private void createAccount(BusinessContext context, Connection connection) throws SQLException {
        try (PreparedStatement putUser = connection.prepareStatement(dialect.getSqlStatement("PUT_ACCOUNT"))) {
            putUser.setString(1, getNextUrn(context)); // unique
            putUser.setString(2, genRandString(20)); // name
            putUser.setString(3, genRandString(35)); // description
            putUser.execute();
        }
    }

    private void createContainer(BusinessContext context, Connection connection) throws SQLException {
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

    private void createObject(BusinessContext context, Connection connection) throws SQLException {
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
                                            insertPs.setString(2, genRandUuid());
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

    private void listContainers(BusinessContext context, Connection connection) throws SQLException {
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

    private void listObjects(BusinessContext context, Connection connection) throws SQLException {
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

    private String getNextUrn(BusinessContext context) {
        return context.identity + ":" + context.counter.incrementAndGet();
    }

    private String getRandUrn(BusinessContext context) {
        int count = context.counter.intValue();
        return count > 0 ? context.identity + ":" + random.nextInt(context.counter.intValue()) : null;
    }

    private String genRandContainer(String urn) {
        return urn + ":" + genRandUuid();
    }

    @Override
    public void create() throws Exception {
        super.create();

        // configuration...

        Properties properties = new Properties();
        loadResource(BusinessServices.class, "application.properties", properties);
        properties.putAll(getProperties());

        // normalize properties...

        final String dbHost = PropertiesUtil.getStringProperty(properties, "database.host", "localhost");
        final String dbName = PropertiesUtil.getStringProperty(properties, "database.name", "cloud");
        final int dbPort = PropertiesUtil.getIntegerProperty(properties, "database.port", 48004);
        final String dbSchema = PropertiesUtil.getStringProperty(properties, "database.schema", "cloud");
        final String dbUrlString = "jdbc:com.nuodb://" + dbHost + ":" + dbPort + "/" + dbName + "?schema=" + dbSchema;

        properties.put("url", dbUrlString);
        properties.put("user", "" + PropertiesUtil.getStringProperty(properties, "db.user", "dba"));
        properties.put("password", "" + PropertiesUtil.getStringProperty(properties, "db.password", "dba"));
        properties.put("testOnReturn", "" + PropertiesUtil.getBooleanProperty(properties, "db.connections.test_on_return", true));
        properties.put("validationQuery", "" + PropertiesUtil.getStringProperty(properties, "db.connections.validation_query_string", "SELECT 1 FROM DUAL"));
        properties.put("isolation", "" + PropertiesUtil.getStringProperty(properties, "db.connections.isolation", "READ_COMMITTED"));
        properties.put("defaultAutoCommit", "" + PropertiesUtil.getBooleanProperty(properties, "db.connections.default_auto_commit", false));
        properties.put("NuoDB_Bug_defaultAutoCommit", "defaultAutoCommit");
        properties.put("maxAge", "" + PropertiesUtil.getIntegerProperty(properties, "db.connections.maxage", 30000));
        properties.put("workload.mix", PropertiesUtil.getStringProperty(properties, "workload.mix", "[5,15,30,10,40]"));
        properties.put("workload.tag", PropertiesUtil.getStringProperty(properties, "workload.tag", "[OLTP_C1,OLTP_C2,OLTP_C3,OLTP_R2,OLTP_R3]"));

        // operational state...

        random = new Random(31);

        // dialect and data sources...

        try {
            dialect = new Dialect(BusinessServices.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid dialect.");
        }

        dataSource = new com.nuodb.jdbc.DataSource(properties);

        loadDataModel();

        // reporting services...

        metricRegistry = new MetricRegistry();

        mix = new Mix(properties);
        meters = new HashMap<>();
        for (Mix.Type type : mix) {
            meters.put(type.getTag(), metricRegistry.timer(type.getTag()));
        }

        consoleReporter = TimerConsoleReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS).build();
        consoleReporter.start(5, TimeUnit.SECONDS);

        String logDirString = PropertiesUtil.getStringProperty(getProperties(), "test.log.dir", null);
        if (logDirString != null) {
            File logDir = new File(logDirString);
            if (logDir.exists()) {
                csvReporter = CsvReporter.forRegistry(metricRegistry)
                        .formatFor(Locale.US)
                        .convertRatesTo(TimeUnit.SECONDS)
                        .convertDurationsTo(TimeUnit.MILLISECONDS)
                        .build(logDir);
                csvReporter.start(5, TimeUnit.SECONDS);
            }
        }

    }

    private void loadDataModel() {
        try {
            StringBuilder builder = Resources.loadResource(BusinessServices.class, "nuodb-dialect-install.sql", new StringBuilder());
            SqlScriptSplitter splitter = new SqlScriptSplitter();
            List<String> statements = splitter.splitStatements(builder.toString());
            try (Connection connection = dataSource.getConnection()) {
                for (String sql : statements) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute(sql);
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                throw new Error("Failed to setup cloud database.", e);
            }
        } catch (IOException e) {
            throw new Error("Failed to find nuodb-dialect-install.sql on classpath.", e);
        }
    }

    @Override
    public void start() throws Exception {
        super.start();
    }

    @Override
    public void stop() {
        try {
            if (csvReporter != null) {
                csvReporter.close();
            }
        } catch (Exception e) {
            // ignore
        }
        try {
            consoleReporter.close();
        } catch (Exception e) {
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
        final BusinessContext businessContext = (BusinessContext) context;

        Mix.Type type = mix.next();
        Timer timer = meters.get(type.getTag());
        try (Timer.Context ignore = timer.time()) {
            switch (type.getTag()) {
                case "OLTP_C1": {
                    retryCode(new SqlCallable() {
                        @Override
                        public Object call(Connection connection) throws SQLException {
                            createAccount(businessContext, connection);
                            return true;
                        }
                    });
                }
                break;
                case "OLTP_C2": {
                    retryCode(new SqlCallable() {
                        @Override
                        public Object call(Connection connection) throws SQLException {
                            createContainer(businessContext, connection);
                            return true;
                        }
                    });
                }
                break;
                case "OLTP_C3": {
                    retryCode(new SqlCallable() {
                        @Override
                        public Object call(Connection connection) throws SQLException {
                            createObject(businessContext, connection);
                            return true;
                        }
                    });
                }
                break;
                case "OLTP_R2": {
                    retryCode(new SqlCallable() {
                        @Override
                        public Object call(Connection connection) throws SQLException {
                            listContainers(businessContext, connection);
                            return true;
                        }
                    });
                }
                case "OLTP_R3": {
                    retryCode(new SqlCallable() {
                        @Override
                        public Object call(Connection connection) throws SQLException {
                            listObjects(businessContext, connection);
                            return true;
                        }
                    });
                }
            }
        }
    }

    @Override
    protected Context createContext() {
        return new BusinessContext();
    }

    // U T I L I T I E S

    // ########################################################################
    // RETRY
    // ########################################################################

    /**
     * Best Practice:
     * <p/>
     * Implements retry semantics, correctly determines if an exception is
     * transient (may be retried) or is non-transient (cannot be retried).
     * By implementing retry user transactions are never lost nor errors
     * bubble up to users.
     *
     * @param callable the code block to execute
     */
    private void retryCode(SqlCallable callable) {
        Exception re;
        int retries = 10;
        do {
            try (Connection connection = dataSource.getConnection()) {
                try {
                    callable.call(connection);
                    connection.commit();
                    return;
                } catch (SQLException se) { // failed transactions...
                    if (!isSqlStateConnectionException(se)) {
                        try {
                            connection.rollback();
                        } catch (SQLException ignored) {
                        }
                    }
                    throw se;
                }
            } catch (Exception e) { // failed getting connections...
                re = e;
                if (Thread.currentThread().isInterrupted() || isInterruptTransitively(e)) {
                    Thread.currentThread().interrupt();
                    break;
                }
                if (!isTransient(e)) {
                    throw new Error(re);
                }
                warn(re);
            }
            sleepDelay(retries);
        } while (--retries > 0);
    }

    private void warn(Exception re) {
        System.err.println(Exceptions.toStringAllCauses(re));
    }

    protected int getThreadCount() {
        return super.getThreadCount();
    }

    // ########################################################################
    // EXPONENTIAL BACKOFF
    // ########################################################################

    private static final long DEFAULT_MIN_BACKOFF = TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS);
    private static final long DEFAULT_MAX_BACKOFF = TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS);
    private static final long DEFAULT_SLOT_TIME = TimeUnit.MILLISECONDS.convert(2, TimeUnit.SECONDS);

    /**
     * Best Practice:
     * <p/>
     * Implements one possible strategy during retry, that is to perform random
     * binary exponential backoff between attempts.
     */
    private long getRetryDelay(int retryCount) {
        final int MAX_CONTENTION_PERIODS = 10;
        return retryCount == 0 ? 0 : Math.min(DEFAULT_MIN_BACKOFF +
                random.nextInt(2 << Math.min(retryCount, MAX_CONTENTION_PERIODS - 1)) *
                        DEFAULT_SLOT_TIME, DEFAULT_MAX_BACKOFF);
    }

    /**
     * Best Practice:
     * <p/>
     * Sleep between retry attempts.
     */
    private void sleepDelay(int retryCount) {
        try {
            Thread.sleep(getRetryDelay(retryCount));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ########################################################################
    // TRANSIENT EXCEPTION DETECTION
    // ########################################################################

    /**
     * Determines if the SQL exception is a connection exception
     *
     * @param se the exception
     * @return true if it is a code 08nnn, otherwise false
     */
    private static boolean isSqlStateConnectionException(SQLException se) {
        String sqlState = se.getSQLState();
        return sqlState != null && sqlState.startsWith("08");
    }

    private boolean isInterruptTransitively(Throwable e) {
        do {
            if (e instanceof InterruptedException) {
                return true;
            }
            e = e.getCause();
        } while (e != null);
        return false;
    }

    /**
     * Determines if the SQL exception a duplicate value in unique index.
     *
     * @param se the exception
     * @return true if it is a code 23505, otherwise false
     */
    private static boolean isSqlStateDuplicateValueInUniqueIndex(SQLException se) {
        String sqlState = se.getSQLState();
        return sqlState != null && (sqlState.equals("23505") || se.getMessage().contains("duplicate value in unique index"));
    }

    /**
     * Determines if the SQL exception is a rollback exception
     *
     * @param se the exception
     * @return true if it is a code 40nnn, otherwise false
     */
    private static boolean isSqlStateRollbackException(SQLException se) {
        String sqlState = se.getSQLState();
        return sqlState != null && sqlState.startsWith("40");
    }

    /**
     * Best Practice:
     * <p/>
     * Determine whether or not exception related activities are retriable.
     *
     * @param e the exception
     * @return true if the activity may be retried
     */
    private static boolean isTransient(Exception e) {
        if (e instanceof SQLTransientException || e instanceof SQLRecoverableException) {
            return true;
        }
        if (e instanceof SQLNonTransientException) {
            return false;
        }
        if (e instanceof SQLException) {
            SQLException se = (SQLException) e;
            if (isSqlStateConnectionException(se) || isSqlStateRollbackException(se)) {
                return true;
            }
            if (isSqlStateDuplicateValueInUniqueIndex(se)) { // we treat dupes as transient
                return true;
            }
        }
        return false;
    }
}