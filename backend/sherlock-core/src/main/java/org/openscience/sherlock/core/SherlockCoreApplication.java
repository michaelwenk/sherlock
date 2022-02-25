package org.openscience.sherlock.core;

import casekit.io.FileSystem;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.openscience.sherlock.core.model.db.HOSECode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@EnableEurekaClient
public class SherlockCoreApplication {

    final Gson gson = new GsonBuilder().setLenient()
                                       .create();

    public static void main(final String[] args) {
        SpringApplication.run(SherlockCoreApplication.class, args);
    }

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

    @Bean
    public Map<String, Map<String, Double[]>> buildHoseCodeDBEntriesMap() {
        final Map<String, Map<String, Double[]>> hoseCodeDBEntriesMap = new HashMap<>();
        this.fillHOSECodeDBEntriesMap(hoseCodeDBEntriesMap);

        return hoseCodeDBEntriesMap;
    }

    private String loopMethod(final String pathToHOSECodesFile, final int waitingDuration,
                              final int totalWaitingDuration, int currentWaitingDuration) {
        final String fileContent = FileSystem.getFileContent(pathToHOSECodesFile);
        if (fileContent
                == null) {
            try {
                System.out.println(" -> could not read HOSE codes from file: \""
                                           + pathToHOSECodesFile
                                           + "\" -> trying again in "
                                           + waitingDuration
                                           + " ms");
                Thread.sleep(waitingDuration);
                currentWaitingDuration += waitingDuration;
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
            if (currentWaitingDuration
                    < totalWaitingDuration) {
                return this.loopMethod(pathToHOSECodesFile, waitingDuration, totalWaitingDuration,
                                       currentWaitingDuration);
            }
        }

        return fileContent;
    }

    private void fillHOSECodeDBEntriesMap(final Map<String, Map<String, Double[]>> hoseCodeDBEntriesMap) {
        System.out.println("\nloading DB entries map...");
        final String pathToHOSECodesFile = "/data/hosecode/hosecodes.json";
        final int waitingDuration = 30; // seconds
        final int totalWaitingDuration = 300; // seconds
        final String fileContent = this.loopMethod(pathToHOSECodesFile, waitingDuration
                * 1000, totalWaitingDuration
                                                           * 1000, 0);
        if (fileContent
                != null) {
            final List<HOSECode> hoseCodeObjectList = this.gson.fromJson(fileContent, new TypeToken<List<HOSECode>>() {
            }.getType());
            for (final HOSECode hoseCodeObject : hoseCodeObjectList) {
                hoseCodeDBEntriesMap.put(hoseCodeObject.getHOSECode(), hoseCodeObject.getValues());
            }
            System.out.println(" -> done: "
                                       + hoseCodeDBEntriesMap.size());
        } else {
            System.out.println(" -> could not read HOSE codes from file: \""
                                       + pathToHOSECodesFile
                                       + "\" !!!");
        }
    }
}
