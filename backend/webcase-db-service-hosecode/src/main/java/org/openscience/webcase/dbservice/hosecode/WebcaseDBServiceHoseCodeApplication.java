package org.openscience.webcase.dbservice.hosecode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
@EnableEurekaClient
public class WebcaseDBServiceHoseCodeApplication {

    @Bean
    public WebClient.Builder getWebClientBuilder() {
        return WebClient.builder();
    }

    public static void main(final String[] args) {
        SpringApplication.run(WebcaseDBServiceHoseCodeApplication.class, args);
    }
}
