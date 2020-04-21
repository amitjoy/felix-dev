package org.apache.felix.hc.core.impl.executor.async.cron;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.felix.hc.core.impl.executor.HealthCheckExecutorThreadPool;
import org.osgi.service.component.annotations.Component;

@Component(service = HealthCheckCronScheduler.class)
public final class HealthCheckCronScheduler {

    private static final Duration INIT_DELAY = Duration.ofSeconds(10);
    private static final Duration CHECK_INTERVAL = Duration.ofSeconds(3);
    private static final String HC_SCHEDULER_NAME = "felix.hc.scheduler";

    private final CronScheduler scheduler;

    public HealthCheckCronScheduler(final HealthCheckExecutorThreadPool healthCheckExecutorThreadPool) {
        int poolSize = healthCheckExecutorThreadPool.getPoolSize();
        final ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        scheduler = new CronScheduler(executor, INIT_DELAY, CHECK_INTERVAL, HC_SCHEDULER_NAME);
    }

    public void shutdown() {
        scheduler.shutdown();
    }

    public CronScheduler getScheduler() {
        return scheduler;
    }

}