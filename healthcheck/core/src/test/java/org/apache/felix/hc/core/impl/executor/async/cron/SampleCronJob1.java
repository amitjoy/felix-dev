package org.apache.felix.hc.core.impl.executor.async.cron;

import org.apache.felix.hc.core.impl.executor.async.cron.CronJob;

public class SampleCronJob1 implements CronJob {

    public static volatile int staticCounter = 0;
    public volatile int counter = 0;

    private final String name;
    private final Callback callback;

    public SampleCronJob1(final Callback callback) {
        this.callback = callback;
        name = "SampleCronJob1";
    }

    public SampleCronJob1(final Callback callback, final String name) {
        this.callback = callback;
        this.name = name;
    }

    @Override
    public void run() throws Exception {
        synchronized (SampleCronJob1.class) {
            counter++;
            staticCounter++;
        }
        callback.callback(this);
    }

    public interface Callback {
        void callback(SampleCronJob1 service);
    }

    @Override
    public String cron() {
        return "* * * * * *";
    }

    @Override
    public String name() {
        return name;
    }
}