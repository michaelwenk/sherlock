package org.openscience.sherlock.dbservice.result.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;

@Configuration
@EnableReactiveMongoRepositories(basePackages = "org.openscience.sherlock.dbservice.result", reactiveMongoTemplateRef = "resultMongoTemplate")
public class ResultMongoConfig {

    @Value("${mongodb.result.uri}")
    private String mongoUri;

    @Primary
    @Bean(name = "resultMongoClient")
    public MongoClient resultMongoClient() {
        return MongoClients.create(mongoUri);
    }

    @Bean(name = "resultMongoTemplate")
    public ReactiveMongoTemplate resultMongoTemplate() {
        return new ReactiveMongoTemplate(resultMongoClient(), "result");
    }
}