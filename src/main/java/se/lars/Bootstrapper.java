package se.lars;

import com.codahale.metrics.*;
import com.codahale.metrics.jvm.*;
import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.logging.LoggingService;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import metrics_influxdb.InfluxdbReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.channels.Pipe;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Bootstrapper
{
    private static Logger _log = LoggerFactory.getLogger(Bootstrapper.class);
    private Vertx _vertx;

    private void run(String[] args)
        throws FileNotFoundException
    {
        String profile = args.length == 0 ? "dev" : args[1];


        XmlConfigBuilder builder = new XmlConfigBuilder("src/main/resources/conf/" + profile + "-cluster.xml");

        VertxOptions vertxOptions = new VertxOptions()
            .setMetricsOptions(new DropwizardMetricsOptions()
                                   .setJmxEnabled(true)
                                   .setRegistryName("my-registry"))
            .setClusterManager(new HazelcastClusterManager(builder.build()));

        Vertx.clusteredVertx(vertxOptions, result -> {
            if (result.succeeded()) {
                _vertx = result.result();
                _vertx.eventBus()
                      .registerDefaultCodec(SomeBean.class, new KryoCodec<>(SomeBean.class));

                Buffer buffer = _vertx.fileSystem()
                                      .readFileBlocking("src/main/resources/" + profile + "-config.json");
                JsonObject json = new JsonObject(buffer.toString());

                // Deploy Rest service
                DeploymentOptions options = new DeploymentOptions();
                options.setConfig(json);
                options.setInstances(Runtime.getRuntime().availableProcessors());
                _vertx.deployVerticle(RestVerticle.class.getName(), options);

                startReport(SharedMetricRegistries.getOrCreate("my-registry"));
            } else {
                _log.error("Failed starting clustered vertex.");
            }
        });
    }

    private static void startReport(MetricRegistry registry)
    {
        registerAll("gc", new GarbageCollectorMetricSet(), registry);
        //registerAll("buffers", new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()), registry);
        registerAll("memory", new MemoryUsageGaugeSet(), registry);
        registerAll("threads", new ThreadStatesGaugeSet(), registry);


        ScheduledReporter reporter = InfluxdbReporter.forRegistry(registry)
                                                     .prefixedWith("lars")
                                                     .tag("server", "server-1")
                                                     .build();
        reporter.start(1, TimeUnit.SECONDS);
    }

    private static void registerAll(String prefix, MetricSet metricSet, MetricRegistry registry)
    {
        for (Map.Entry<String, Metric> entry : metricSet.getMetrics().entrySet()) {
            if (entry.getValue() instanceof MetricSet) {
                registerAll(prefix + "." + entry.getKey(), (MetricSet)entry.getValue(), registry);
            } else {
                registry.register(prefix + "." + entry.getKey(), entry.getValue());
            }
        }
    }

    public static void main(String args[])
        throws Exception
    {
        System.setProperty("vertx.logger-delegate-factory-class-name",
                           "io.vertx.core.logging.SLF4JLogDelegateFactory");
        displayBanner();
        _log.info("Running on JVM {}", System.getProperty("java.version"));
        new Bootstrapper().run(args);
    }

    private static void displayBanner()
        throws IOException
    {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(Bootstrapper.class.getResourceAsStream("/banner.txt")))) {
            System.out.println(buffer.lines().collect(Collectors.joining("\n")));
        }
    }
}
