package org.openscience.sherlock.dbservice.statistics.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;

@Configuration
@EnableReactiveMongoRepositories(basePackages = "org.openscience.sherlock.dbservice.statistics.service", reactiveMongoTemplateRef = "statisticsMongoTemplate")
public class StatisticsMongoConfig {

    @Value("${mongodb.statistics.uri}")
    private String mongoUri;

    @Bean(name = "statisticsMongoClient")
    public MongoClient statisticsMongoClient() {
        return MongoClients.create(mongoUri);
    }

    @Bean(name = "statisticsMongoTemplate")
    public ReactiveMongoTemplate statisticsMongoTemplate() {
        return new ReactiveMongoTemplate(statisticsMongoClient(), "statistics");
    }
}