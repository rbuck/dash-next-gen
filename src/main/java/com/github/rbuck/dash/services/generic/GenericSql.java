package com.github.rbuck.dash.services.generic;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.rbuck.dash.common.*;
import com.github.rbuck.dash.common.functions.SqlDate;
import com.github.rbuck.dash.services.AbstractService;
import com.github.rbuck.dash.services.Context;
import com.github.rbuck.retry.FixedInterval;
import com.github.rbuck.retry.SqlRetryPolicy;
import com.github.vincentrussell.json.datagenerator.functions.FunctionRegistry;
import com.github.vincentrussell.json.datagenerator.parser.FunctionParser;
import com.github.vincentrussell.json.datagenerator.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.rbuck.dash.common.Preconditions.checkArgument;
import static com.github.rbuck.dash.common.PropertiesHelper.*;
import static java.lang.System.getProperties;

public class GenericSql extends AbstractService {

    private static Logger logger = LoggerFactory.getLogger(GenericSql.class);

    static {
        FunctionRegistry.getInstance().registerClass(SqlDate.class);
    }

    private final MetricRegistry metricRegistry = new MetricRegistry();
    private final MetricsService metricsService = new MetricsService(metricRegistry);

    private SqlRetryPolicy<Boolean> retryPolicy;
    private HashMap<String, Timer> meters;

    private Dialect dialect;
    private Mix mix;

    @Override
    protected Context createContext() {
        return new SqlContext();
    }

    @Override
    public void create() throws Exception {
        super.create();

        // configuration...

        Properties properties = new Properties();
        Resources.loadResource(GenericSql.class, "application.properties", properties);
        properties.putAll(getProperties());

        // dialect and data sources...

        try {
            dialect = new Dialect(GenericSql.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid dialect.");
        }

        retryPolicy = new SqlRetryPolicy<>(new FixedInterval(1, 100), new DataSourceContext());

//        if (!getBooleanProperty(properties, "dash.db.skip.init", false)) {
//            doPreload();
//        }
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

    private void doPreload() {
        String[] tags = getStringArrayProperty(dialect.getProperties(), "dash.preload.tags", new String[0]);
        int[] cardinalities = getIntegerArrayProperty(dialect.getProperties(), "dash.preload.cardinalities", new int[0]);
        checkArgument(tags.length > 0, "Preload statements count must be greater than zero");
        checkArgument(tags.length == cardinalities.length, "Preload statement count must equal preload cardinalities");
        for (int cardinality : cardinalities) {
            checkArgument(cardinality >= 0, "Preload cardinalities values must be greater than or equal to zero");
        }

        for (int i = 0; i < tags.length; i++) {
            final int cardinality = cardinalities[i];
            final int batchSize = 1000;

            final List<String> parameters = getParameters(tags[i]);
            final String sql = getStatement(tags[i]);

            int remaining = cardinality;
            do {
                int records = remaining / batchSize > 0 ? batchSize : remaining % batchSize;

                System.out.println(records);
                try {
                    retryPolicy.action(connection -> {
                        try (PreparedStatement statement = connection.prepareStatement(sql)) {
                            for (int record = 0; record < records; record++) {
                                System.out.println("record: " + record);
                                for (int pi = 0; pi < parameters.size(); pi++) {
                                    try {
                                        //System.out.println("parameter: " + pi);
                                        //System.out.println("parameter expr: " + parameters.get(pi));
                                        FunctionParser parser = new FunctionParser(
                                                new ByteArrayInputStream(parameters.get(pi).getBytes()));
                                        String value = parser.Parse();
                                        //System.out.println("Bound dynamic parameter: " + value);
//                                        statement.setString(pi + 1, value);
                                    } catch (ParseException e) {
                                        System.err.println(Exceptions.toStringAllCauses(e));
                                        System.exit(1);
                                    }
                                }
                                statement.addBatch();
                            }
                            statement.executeBatch();
                        }
                        return true;
                    });
                } catch (Exception e) {
                    System.err.println(Exceptions.toStringAllCauses(e));
                    System.exit(1);
                }

                remaining -= batchSize;
            } while (remaining > 0);

            System.exit(1);

        }
    }

    private String getStatement(String tag) {
        return dialect.getProperty(tag + ".statement");
    }

    private static final Pattern MUSTACHE_PATTERN = Pattern.compile("\\{\\{([^}]+)}}");

    private List<String> getParameters(String tag) {
        List<String> parameters = new LinkedList<>();
        String line = dialect.getProperty(tag + ".parameters");
        Matcher m = MUSTACHE_PATTERN.matcher(line);
        while (m.find()) {
            //System.out.println("Adding Parameter: " + m.group(1));
            parameters.add(m.group(1));
        }
        return parameters;
    }

    @Override
    protected void execute(Context context) {
        //final GenericSql.PingContext pingContext = (GenericSql.PingContext) context;
        Mix.Type type = mix.next();
        Timer timer = meters.get(type.getTag());
        try (Timer.Context ignore = timer.time()) {
            String tag = type.getTag();
            retryPolicy.action(connection -> {
                //System.out.println(getStatement(tag));
                try (PreparedStatement statement = connection.prepareStatement(getStatement(tag))) {
                    List<String> parameters = getParameters(tag);
                    if (parameters.size() > 0) {
                        //System.out.println("Parameters size: " + parameters.size());
                        for (int i = 0; i < parameters.size(); i++) {
                            String parameter = parameters.get(i);
                            //System.out.println("Using raw parameter: " + parameter);
                            if (parameter.indexOf('(') != -1) {
                                FunctionParser functionParser = new FunctionParser(new ByteArrayInputStream(parameters.get(i).getBytes()));
                                parameter = functionParser.Parse();
                                //System.out.println("Bound dynamic parameter: " + parameter);
                            }
                            statement.setString(i + 1, parameter);
                        }
                    }
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            resultSet.getString(1);
                            //System.out.println("value: " + resultSet.getString(1));
                        }
                    } catch (Exception e) {
                        System.err.println(Exceptions.toStringAllCauses(e));
                    }
                } catch (ParseException e) {
                    System.err.println(Exceptions.toStringAllCauses(e));
                    throw new Error(e);
                }
                return true;
            });
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

    class SqlContext implements Context {
        SqlContext() {
        }
    }

}
