package org.openscience.webcase.dbservicehybridizations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class WebcaseDbServiceHybridizationsApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebcaseDbServiceHybridizationsApplication.class, args);
    }

}
