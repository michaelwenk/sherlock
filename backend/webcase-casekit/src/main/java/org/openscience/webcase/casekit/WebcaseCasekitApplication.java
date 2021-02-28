package org.openscience.webcase.casekit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class WebcaseCasekitApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebcaseCasekitApplication.class, args);
    }

}
