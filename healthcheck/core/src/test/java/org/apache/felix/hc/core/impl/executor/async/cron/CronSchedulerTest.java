package org.apache.felix.hc.core.impl.executor.async.cron;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.hc.core.impl.executor.async.cron.SampleCronJob1.Callback;
import org.junit.Assert;
import org.junit.Test;

public class CronSchedulerTest extends Assert {

    private static final Object SYNC_OBJECT = new Object();

    @Test
    public void checkSchedulerCheckSingleThread() throws InterruptedException {
        try (final CronScheduler scheduler = new CronScheduler(Executors.newFixedThreadPool(1), 0, 1, TimeUnit.SECONDS, "T1")) {
            final int nIterations = 3;

            final SampleCronJob1 service = new SampleCronJob2(s -> {
                System.out.println("Iteration: " + s.counter);
                if (s.counter == nIterations) {
                    scheduler.shutdown();
                    System.out.println("Scheduler is shut down!");
                    synchronized (SYNC_OBJECT) {
                        SYNC_OBJECT.notify();
                    }
                }
            });
            assertEquals(0, service.counter);
            final long t0 = System.currentTimeMillis();
            scheduler.schedule(service);

            synchronized (SYNC_OBJECT) {
                SYNC_OBJECT.wait(5_000L);
            }
            final int counter = service.counter;
            final long totalTime = System.currentTimeMillis() - t0;

            assertEquals(nIterations, counter);
            assertTrue(scheduler.isShutdown());
            assertTrue(totalTime < 4_000);

            Thread.sleep(2_000L);
            assertEquals(counter, service.counter);
        }
    }

    @Test
    public void checkSchedulerCheckMultipleThreads() throws InterruptedException {
        try (CronScheduler scheduler = new CronScheduler(Executors.newFixedThreadPool(10), 0, 1, TimeUnit.SECONDS, "T2")) {
            SampleCronJob1.staticCounter = 0;
            final int nIterations = 3;

            final Callback c = s -> {
                try {
                    Thread.sleep(3000);
                } catch (final InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (SampleCronJob1.staticCounter == nIterations) {
                    synchronized (SYNC_OBJECT) {
                        SYNC_OBJECT.notify();
                    }
                }
            };
            synchronized (SYNC_OBJECT) {
                for (int i = 0; i < 10; i++) {
                    scheduler.schedule(new SampleCronJob1(c, "abc" + i));
                }
                SYNC_OBJECT.wait(5 * 1000L);
            }
            scheduler.shutdown();
            assertTrue("Number of iterations: " + SampleCronJob1.staticCounter + ">=" + nIterations,
                    SampleCronJob1.staticCounter >= nIterations);
        }
    }

    @Test
    public void checkScheduledPeriodsOverlap() throws InterruptedException {
        final int callCostInSeconds = 3;
        final AtomicInteger count = new AtomicInteger();
        try (CronScheduler scheduler = new CronScheduler(Executors.newFixedThreadPool(1), 0, 200, TimeUnit.MILLISECONDS,
                "T10")) {
            final CronJob scheduled = new CronJob() {
                @Override
                public void run() throws Exception {
                    final int cycle = count.incrementAndGet();
                    System.out.println("cycle: " + cycle + " started");
                    Thread.sleep(callCostInSeconds * 1000);
                    System.out.println("cycle: " + count.get() + " finished");
                }

                @Override
                public String cron() {
                    return "* * * * * *";
                }

                @Override
                public String name() {
                    return "test";
                }
            };
            scheduler.schedule(scheduled);

            final int nCyclesToCheck = 5;
            Thread.sleep(nCyclesToCheck * callCostInSeconds * 1000L);
            final int minCyclesExpected = nCyclesToCheck - 1;
            Assert.assertTrue(
                    "At least " + minCyclesExpected + " cycles must be started at this point, actual: " + count.get(),
                    count.get() == nCyclesToCheck || count.get() == nCyclesToCheck - 1);
        }
    }
}