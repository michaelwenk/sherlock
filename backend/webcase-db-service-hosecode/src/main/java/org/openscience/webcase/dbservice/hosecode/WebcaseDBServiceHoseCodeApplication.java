package org.openscience.webcase.dbservice.hosecode;

import org.openscience.webcase.dbservice.hosecode.service.model.HOSECode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@EnableEurekaClient
public class WebcaseDBServiceHoseCodeApplication {

    @Bean
    public WebClient.Builder getWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public Map<String, HOSECode> getHoseCodeDBEntriesMap() {
        return new HashMap<>();
    }

    public static void main(final String[] args) {
        SpringApplication.run(WebcaseDBServiceHoseCodeApplication.class, args);
    }
}
