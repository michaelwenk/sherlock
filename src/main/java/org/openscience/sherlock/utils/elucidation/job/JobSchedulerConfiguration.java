package org.openscience.sherlock.utils.elucidation.job;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JobSchedulerConfiguration {

    @Value("${sherlock.job.pool-size:4}")
    private int poolSize;

    @Value("${sherlock.job.queue-size:1000}")
    private int queueSize;

    @PostConstruct
    public void configureGlobalScheduler() {
        GlobalJobScheduler.configure(poolSize, queueSize);
    }
}
