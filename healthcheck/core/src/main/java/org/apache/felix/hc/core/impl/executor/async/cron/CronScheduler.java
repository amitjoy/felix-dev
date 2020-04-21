package org.apache.felix.hc.core.impl.executor.async.cron;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CronScheduler implements AutoCloseable {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Object used for internal locking/notifications.
     */
    private final Object monitor = new Object();

    /**
     * List of managed tasks.
     */
    private final Map<String, CronSchedulerTask> tasks = new HashMap<>();

    /**
     * Task executor instance.
     */
    public final ExecutorService tasksExecutor;

    /**
     * Thread that makes all scheduling job.
     */
    public final SchedulerThread schedulerThread;

    /**
     * If scheduler is active or not.
     */
    private AtomicBoolean isActive;

    public CronScheduler(final ExecutorService tasksExecutor, final long initialDelay, final long checkInterval,
            final TimeUnit timeUnit, final String schedulerThreadName) {
        schedulerThread = new SchedulerThread(initialDelay, checkInterval, timeUnit, schedulerThreadName);
        this.tasksExecutor = tasksExecutor;
        isActive = new AtomicBoolean(true);
        schedulerThread.start();
    }

    public CronScheduler(final ExecutorService tasksExecutor, final Duration initialDelay, final Duration checkInterval,
            final String schedulerThreadName) {
        schedulerThread = new SchedulerThread(initialDelay.getSeconds(), checkInterval.getSeconds(), SECONDS,
                schedulerThreadName);
        this.tasksExecutor = tasksExecutor;
        isActive = new AtomicBoolean(true);
        schedulerThread.start();
    }

    public void schedule(final CronJob cronJob) {
        synchronized (monitor) {
            tasks.put(cronJob.name(), new CronSchedulerTask(this, cronJob, new CronParser(cronJob.cron())));
        }
    }

    public void remove(final String name) {
        synchronized (monitor) {
            tasks.remove(name);
        }
    }

    public void shutdown() {
        isActive.set(false);
        synchronized (monitor) {
            monitor.notify();
        }
        tasksExecutor.shutdown();
    }

    public boolean isShutdown() {
        return !isActive.get();
    }

    public Collection<CronSchedulerTask> getTasks() {
        return tasks.values();
    }

    @Override
    public void close() {
        shutdown();
    }

    public class SchedulerThread extends Thread {
        public final long initialDelay;
        public final long checkInterval;
        public final TimeUnit timeUnit;

        private SchedulerThread(final long initialDelay, final long checkInterval, final TimeUnit timeUnit,
                final String threadName) {
            if (initialDelay < 0) {
                throw new IllegalArgumentException("initialDelay < 0. Value: " + initialDelay);
            }
            if (checkInterval <= 0) {
                throw new IllegalArgumentException("checkInterval must be > 0. Value: " + checkInterval);
            }
            if (timeUnit == null) {
                throw new IllegalArgumentException("timeUnit is null");
            }
            this.initialDelay = initialDelay;
            this.checkInterval = checkInterval;
            this.timeUnit = timeUnit;
            setName(threadName);
            setDaemon(true);
        }

        @Override
        public void run() {
            if (initialDelay > 0) {
                pause(timeUnit.toMillis(initialDelay));
            }
            final long checkIntervalMillis = timeUnit.toMillis(checkInterval);
            while (isActive.get()) {
                try {
                    checkAndExecute();
                    pause(checkIntervalMillis);
                } catch (final Exception e) {
                    logger.error("Got internal error that must never happen!", e);
                }
            }
        }

        private void pause(final long checkIntervalMillis) {
            try {
                synchronized (monitor) {
                    monitor.wait(checkIntervalMillis);
                }
            } catch (final InterruptedException e) {
                logger.error("Got unexpected interrupted exception! Ignoring", e);
                Thread.currentThread().interrupt();
            }
        }

        private void checkAndExecute() {
            synchronized (monitor) {
                final long currentTime = System.currentTimeMillis();
                for (final Entry<String, CronSchedulerTask> entry : tasks.entrySet()) {
                    final CronSchedulerTask task = entry.getValue();
                    if (task.nextExecutingTime >= currentTime) {
                        continue;
                    }
                    if (task.executing) {
                        task.nextExecutingTime = task.sequenceGenerator.next(currentTime);
                        continue;
                    }
                    try {
                        task.lastExecutingTime = System.currentTimeMillis();
                        tasksExecutor.execute(task);
                        task.executing = true;
                    } catch (final RejectedExecutionException e) {
                        logger.error("Failed to start task: {}", task, e);
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }
}