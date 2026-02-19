package org.openscience.sherlock.dbservice.dataset.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

import com.mongodb.reactivestreams.client.MongoClients;

@Configuration
@EnableReactiveMongoRepositories(basePackages = "org.openscience.sherlock.dbservice.dataset.db.service.mongo", reactiveMongoTemplateRef = "datasetMongoTemplate")
public class DatasetMongoConfig {

    public static final String DATASET_INDEX_NAME_NUCLEI = "dataSet.spectrum.nuclei_1";
    public static final String DATASET_INDEX_NAME_MF = "dataSet.meta.mf_1";

    @Value("${mongodb.dataset.uri}")
    private String mongoUri;

    @Primary
    @Bean(name = "datasetMongoTemplate")
    public ReactiveMongoTemplate reactiveMongoTemplate() {
        return new ReactiveMongoTemplate(MongoClients.create(mongoUri), "dataset");
    }
}
