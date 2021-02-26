package org.openscience.webcase.resultretrieval;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class WebcaseResultRetrievalApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebcaseResultRetrievalApplication.class, args);
    }

}
