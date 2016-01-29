package com.github.rbuck.dash.services.woocommerce;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.rbuck.dash.common.Exceptions;
import com.github.rbuck.dash.common.MetricsService;
import com.github.rbuck.dash.common.Mix;
import com.github.rbuck.dash.common.Resources;
import com.github.rbuck.dash.services.AbstractService;
import com.github.rbuck.dash.services.Context;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.rbuck.dash.common.PropertiesHelper.*;
import static java.lang.String.format;
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
    private int tenants;

    private Random random = new Random(31);

    private AtomicInteger pages;
    private boolean isNameBased;

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

        // name | addr | port
        host = getStringProperty(properties, "woocommerce.host", "172.16.228.138");
        port = getIntegerProperty(properties, "woocommerce.port", 8080);
        tenants = getIntegerProperty(properties, "woocommerce.tenants", 1);
        pages = new AtomicInteger(getIntegerProperty(properties, "woocommerce.pages", 3));
        isNameBased = getBooleanProperty(properties, "woocommerce.scheme.namebased", false);

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
                    Response response = Request.Get("http://" + getRandomHost() + ":" + getRandomPort() + "/?s=" + term)
                            .execute();
                    HttpResponse httpResponse = response.returnResponse();
                    if ((httpResponse.getStatusLine().getStatusCode() != 200)) {
                        System.err.println("[WOO_FIND]" + httpResponse.getStatusLine());
                    }
                }
                break;
                case "WOO_PNXT": {
                    // implements going to the next page of results where page
                    // numbers range from 1..3.
                    Random random = new Random(31);
                    int page = random.nextInt(pages.get()) + 1;
                    Response response = Request.Get("http://" + getRandomHost() + ":" + getRandomPort() + "/?post_type=product&paged=" + page)
                            .execute();
                    HttpResponse httpResponse = response.returnResponse();
                    if ((httpResponse.getStatusLine().getStatusCode() != 200)) {
                        System.err.println("[WOO_PNXT]" + "(" + pages + "): " + httpResponse.getStatusLine());
                    }
                }
                break;
                case "WOO_PLIM": {
                    // implements going to the next page of results where page
                    // numbers range from 1..3.
                    Random random = new Random(31);
                    int expected = random.nextInt() + 2;
                    Response response = Request.Get("http://" + getRandomHost() + ":" + getRandomPort() + "/?post_type=product&paged=" + (expected + 1))
                            .execute();
                    HttpResponse httpResponse = response.returnResponse();
                    if ((httpResponse.getStatusLine().getStatusCode() == 200)) {
                        pages.compareAndSet(expected, expected + 1);
                    } else {
                        if (httpResponse.getStatusLine().getStatusCode() != 404) {
                            System.err.println("[WOO_PLIM]" + httpResponse.getStatusLine());
                        }
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

    private String getRandomHost() {
        String value = host;
        if (isNameBased) {
            value = format(host, random.nextInt(tenants) + 1);
        }
        return value;
    }

    private int getRandomPort() {
        int value = port;
        if (!isNameBased) {
            value = port + random.nextInt(tenants);
        }
        return value;
    }
}