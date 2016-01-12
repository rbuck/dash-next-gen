package com.github.rbuck.dash.services.woocommerce;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.rbuck.dash.common.Exceptions;
import com.github.rbuck.dash.common.MetricsService;
import com.github.rbuck.dash.common.Mix;
import com.github.rbuck.dash.common.Resources;
import com.github.rbuck.dash.services.AbstractService;
import com.github.rbuck.dash.services.Context;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;

import static com.github.rbuck.dash.common.PropertiesHelper.getIntegerProperty;
import static com.github.rbuck.dash.common.PropertiesHelper.getStringProperty;
import static java.lang.System.getProperties;

/**
 * A cloud style workload runner that illustrates:
 * <p>
 * - best practices
 * - scale out and resiliency
 */
public class WooCommerce extends AbstractService {

    private final MetricRegistry metricRegistry = new MetricRegistry();
    private final MetricsService metricsService = new MetricsService(metricRegistry);

    private HashMap<String, Timer> meters;
    private Mix mix;

    private String host;
    private int port;

    public WooCommerce() {
    }

    class WebContext implements Context {

//        HttpClient client = new HttpClient();

        WebContext() {
//            try {
//                client.start();
//            } catch (Exception e) {
//                // fail fast...
//                throw new Error(e);
//            }
        }
    }

    @Override
    public void create() throws Exception {
        super.create();

        // configuration...

        Properties properties = new Properties();
        Resources.loadResource(WooCommerce.class, "application.properties", properties);
        properties.putAll(getProperties());

        // operational state...

        host = getStringProperty(properties, "woocommerce.host", "172.16.228.138");
        port = getIntegerProperty(properties, "woocommerce.port", 8080);

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

    @Override
    public void destroy() {
        super.destroy();
    }

    // T H R E A D E D   S E R V I C E   I N T E R F A C E S

    @Override
    protected void execute(Context context) {
        final WebContext webContext = (WebContext) context;
        Mix.Type type = mix.next();
        Timer timer = meters.get(type.getTag());
        try (Timer.Context ignore = timer.time()) {
            switch (type.getTag()) {
                case "WOO_FIND": {
                    // implements searching for an item with a term in its name
                    String[] searchTerms = {"Ninja", "Woo", "Album", "Logo",
                            "Premium", "Quality", "Silhouette"};
                    Random random = new Random(31);
                    String term = searchTerms[random.nextInt(searchTerms.length)];
                    Response response = Request.Get("http://" + host + ":" + port + "/?s=" + term)
                            .execute();
                    if ((response.returnResponse().getStatusLine().getStatusCode() != 200)) {
                        System.err.println(response.returnResponse().getStatusLine());
                    }
                }
                break;
                case "WOO_NEXT": {
                    // implements going to the next page of results where page
                    // numbers range from 1..3.
                    Random random = new Random(31);
                    int page = random.nextInt(3) + 1;
                    Response response = Request.Get("http://" + host + ":" + port + "/shop/page/" + page + "/")
                            .execute();
                    if ((response.returnResponse().getStatusLine().getStatusCode() != 200)) {
                        System.err.println(response.returnResponse().getStatusLine());
                    }
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
        return new WebContext();
    }

    // U T I L I T I E S

    private void warn(Exception re) {
        System.err.println(Exceptions.toStringAllCauses(re));
    }

    protected int getThreadCount() {
        return super.getThreadCount();
    }
}