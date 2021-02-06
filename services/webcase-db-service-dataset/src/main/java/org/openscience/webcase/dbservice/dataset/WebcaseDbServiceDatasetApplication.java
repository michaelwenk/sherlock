package org.openscience.webcase.dbservice.dataset;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class WebcaseDbServiceDatasetApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebcaseDbServiceDatasetApplication.class, args);
    }

}
