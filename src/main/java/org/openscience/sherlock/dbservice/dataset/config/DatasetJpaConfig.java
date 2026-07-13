package org.openscience.sherlock.dbservice.dataset.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {
        "org.openscience.sherlock.dbservice.dataset.db.service.jpa",
        "org.openscience.sherlock.dbservice.job.repository"
})
public class DatasetJpaConfig {
    // JPA configuration is auto-configured by Spring Boot

    public static final String FRAGMENT_TABLE_NAME = "fragments";
}