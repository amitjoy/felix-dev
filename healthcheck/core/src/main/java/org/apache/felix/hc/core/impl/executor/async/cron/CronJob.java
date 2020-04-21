package org.apache.felix.hc.core.impl.executor.async.cron;

public interface CronJob {

    void run() throws Exception;

    String cron();

    String name();

}