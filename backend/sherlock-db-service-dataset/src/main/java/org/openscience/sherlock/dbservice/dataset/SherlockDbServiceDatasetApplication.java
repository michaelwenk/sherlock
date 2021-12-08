package org.openscience.sherlock.dbservice.dataset;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class SherlockDbServiceDatasetApplication {

    public static void main(final String[] args) {
        SpringApplication.run(SherlockDbServiceDatasetApplication.class, args);
    }
}
