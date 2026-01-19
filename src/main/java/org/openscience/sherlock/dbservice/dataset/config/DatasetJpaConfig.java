package org.openscience.sherlock.dbservice.dataset.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "org.openscience.sherlock.dbservice.dataset.db.service.jpa")
public class DatasetJpaConfig {
    // JPA configuration is auto-configured by Spring Boot
}