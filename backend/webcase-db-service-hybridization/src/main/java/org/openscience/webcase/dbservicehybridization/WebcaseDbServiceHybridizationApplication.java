package org.openscience.webcase.dbservicehybridization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class WebcaseDbServiceHybridizationApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebcaseDbServiceHybridizationApplication.class, args);
    }

}
