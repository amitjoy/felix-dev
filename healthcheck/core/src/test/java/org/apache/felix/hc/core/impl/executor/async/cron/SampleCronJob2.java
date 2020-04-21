package org.apache.felix.hc.core.impl.executor.async.cron;

public class SampleCronJob2 extends SampleCronJob1 {
    public SampleCronJob2(final Callback callback) {
        super(callback);
    }

    @Override
    public String name() {
        return "SampleCronJob2";
    }
}