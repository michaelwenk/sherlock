package org.openscience.webcase.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class WebcaseGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebcaseGatewayApplication.class, args);
    }

}
