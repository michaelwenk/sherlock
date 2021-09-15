package org.openscience.webcase.dbservice.statistics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
@EnableEurekaClient
public class WebcaseDbServiceStatisticsApplication {

    @Bean
    public WebClient.Builder getWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public ExchangeStrategies getExchangeStrategies() {
        // set ExchangeSettings
        final int maxInMemorySizeMB = 1000;
        return ExchangeStrategies.builder()
                                 .codecs(configurer -> configurer.defaultCodecs()
                                                                 .maxInMemorySize(maxInMemorySizeMB
                                                                                          * 1024
                                                                                          * 1024))
                                 .build();
    }

    public static void main(final String[] args) {
        SpringApplication.run(WebcaseDbServiceStatisticsApplication.class, args);
    }
}
