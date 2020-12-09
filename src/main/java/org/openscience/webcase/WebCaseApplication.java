package org.openscience.webcase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableMongoRepositories
@SpringBootApplication()
public class WebCaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebCaseApplication.class, args);
    }

}
