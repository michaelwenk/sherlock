package org.openscience.webcase.ranking.spectralsimilarity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class WebcaseRankingSpectralSimilarityApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebcaseRankingSpectralSimilarityApplication.class, args);
    }

}
