package org.openscience.webcase.dbservice.hosecode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class WebcaseDBServiceHoseCodeApplication {

    public static void main(final String[] args) {
        SpringApplication.run(WebcaseDBServiceHoseCodeApplication.class, args);
    }
}
