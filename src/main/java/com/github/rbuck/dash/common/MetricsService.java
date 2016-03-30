package com.github.rbuck.dash.common;

import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import org.elasticsearch.metrics.ElasticsearchReporter;
import org.elasticsearch.metrics.JsonMetrics;
import org.elasticsearch.metrics.percolation.Notifier;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.github.rbuck.dash.common.PropertiesHelper.getStringArrayProperty;

/**
 * A metrics reporting service for the Coda Hale Metrics package.
 */
public class MetricsService implements Closeable {

    private static class SystemOutNotifier implements Notifier {
        @Override
        public void notify(JsonMetrics.JsonMetric jsonMetric, String id) {
            System.out.println(String.format(
                    "Metric %s of type %s at date %s matched with percolation id %s",
                    jsonMetric.name(), jsonMetric.type(), jsonMetric.timestampAsDate(), id));
        }
    }

    private final MetricRegistry metricRegistry;
    private final List<ScheduledReporter> reporters = new ArrayList<>();

    public MetricsService(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
        setup();
    }

    private static final String[] defaultReporters = {"console", "csv"};

    private void setup() {
        List<String> reporterNames = Arrays.asList(getStringArrayProperty(
                System.getProperties(), "dash.metrics.service.reporters",
                defaultReporters));
        if (reporterNames.contains("console")) {
            final TimerConsoleReporter reporter = TimerConsoleReporter.forRegistry(metricRegistry)
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS).build();
            reporters.add(reporter);
        }
        if (reporterNames.contains("csv")) {
            final CsvReporter reporter = CsvReporter.forRegistry(metricRegistry)
                    .formatFor(Locale.US)
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .build(new File(System.getProperty("dash.log.dir")));
            reporters.add(reporter);
        }
        if (reporterNames.contains("elasticsearch")) {
            try {
                final String[] defaultHosts = {"localhost:9200"};
                final String[] elasticHosts = getStringArrayProperty(System.getProperties(),
                        "dash.metrics.service.elasticsearch.hosts", defaultHosts);
                final String bindAddress = getBindAddress(elasticHosts);
                Map<String, Object> additionalFields = new LinkedHashMap<>();
                additionalFields.put("host", bindAddress);
                final ElasticsearchReporter reporter = ElasticsearchReporter.forRegistry(metricRegistry)
                        .hosts(elasticHosts)
                        .index("dash-metrics")
                        .indexDateFormat("yyyy-MM-dd")
                        .percolationFilter(MetricFilter.ALL)
                        .percolationNotifier(new SystemOutNotifier())
                        .additionalFields(additionalFields)
                        .build();
                reporters.add(reporter);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void start() throws IOException {
        for (ScheduledReporter reporter : reporters) {
            reporter.start(5, TimeUnit.SECONDS);
        }
    }

    @Override
    public void close() throws IOException {
        for (ScheduledReporter reporter : reporters) {
            reporter.close();
        }
    }

    private String getBindAddress(String[] addresses) {
        String nodeId = null;
        for (String address : addresses) {
            try {
                URI uri = new URI("tcp://" + address);
                SocketAddress sockaddr = new InetSocketAddress(uri.getHost(), uri.getPort());
                try (Socket s = new Socket()) {
                    s.connect(sockaddr, 1000);
                    nodeId = s.getLocalAddress().getHostAddress();
                    break;
                } catch (IOException e) {
                    // ignore
                }
            } catch (URISyntaxException e) {
                throw new Error("Elasticsearch bind address is not valid: " + address, e);
            }
        }
        return nodeId;
    }
}
