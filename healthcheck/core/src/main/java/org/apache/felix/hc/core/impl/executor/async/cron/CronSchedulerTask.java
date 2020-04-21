package org.apache.felix.hc.core.impl.executor.async.cron;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CronSchedulerTask implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public final CronJob cronJob;
    public final CronScheduler scheduler;
    public final CronParser sequenceGenerator;

    volatile boolean executing;
    volatile long lastExecutingTime = 0;
    volatile long nextExecutingTime = 0;

    public CronSchedulerTask(final CronScheduler scheduler, final CronJob cronJob,
            final CronParser sequenceGenerator) {
        this.scheduler = scheduler;
        this.cronJob = cronJob;
        this.sequenceGenerator = sequenceGenerator;
    }

    @Override
    public void run() {
        try {
        	cronJob.run();
        } catch (final Exception e) {
            logger.error("Exception while executing cron task", e);
        } finally {
            nextExecutingTime = sequenceGenerator.next(System.currentTimeMillis());
            executing = false;
        }
    }

    public long getLastExecutingTime() {
        return lastExecutingTime;
    }

    public long getNextExecutingTime() {
        return nextExecutingTime;
    }

    public boolean isExecuting() {
        return executing;
    }

    @Override
    public String toString() {
        return "SchedulerTask [" + cronJob.name() + "]";
    }
}