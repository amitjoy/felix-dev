/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.felix.hc.core.impl.scheduling;

import org.apache.felix.hc.core.impl.executor.async.cron.CronJob;
import org.apache.felix.hc.core.impl.executor.async.cron.CronScheduler;
import org.apache.felix.hc.core.impl.executor.async.cron.HealthCheckCronScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Runs health checks that are configured with a cron expression for asynchronous execution. 
 */
public class AsyncQuartzCronJob extends AsyncJob {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CronJob cronJob;
    protected final HealthCheckCronScheduler healtchCheckScheduler;

    public AsyncQuartzCronJob(Runnable runnable, HealthCheckCronScheduler healtchCheckScheduler, String id, String cronExpression) throws ClassNotFoundException {
        super(runnable);
        this.healtchCheckScheduler = healtchCheckScheduler;
        this.cronJob = new HealthCheckJob(cronExpression, id);
    }

    public boolean schedule() {
        try {
            CronScheduler scheduler = healtchCheckScheduler.getScheduler();
            scheduler.schedule(cronJob);
            logger.info("Scheduled job {} with trigger {}", cronJob.name(), cronJob.cron());
            return true;
        } catch (Exception e) {
            logger.error("Could not schedule job for " + runnable + ": " + e, e);
            return false;
        }

    }

    @Override
    public boolean unschedule() {
    	CronScheduler scheduler = healtchCheckScheduler.getScheduler();
        String name = cronJob.name();
        logger.debug("Unscheduling job {}", name);
        try {
            scheduler.remove(name);
            return true;
        } catch (Exception e) {
            logger.error("Could not unschedule job for " + name + ": " + e, e);
            return false;
        }
    }

    @Override
    public String toString() {
        return "[Async quartz job for " + runnable + "]";
    }
    
    private class HealthCheckJob implements CronJob {
    	
    	private String name;
    	private String cronExpression;
    	
        public HealthCheckJob(String cronExpression, String name) {
        	this.name = name;
        	this.cronExpression = cronExpression;
		}
        
		@Override
        public String cron() {
            return cronExpression;
        }
        @Override
        public String name() {
            return "job-hc-" + name;
        }
        @Override
        public void run() throws Exception {
        	runnable.run();
        }
    }

}