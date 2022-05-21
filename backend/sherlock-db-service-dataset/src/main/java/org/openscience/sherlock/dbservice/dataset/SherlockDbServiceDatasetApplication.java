package org.openscience.sherlock.dbservice.dataset;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class SherlockDbServiceDatasetApplication {

    public static final String PATH_TO_NMRSHIFTDB = "/data/nmrshiftdb/nmrshiftdb.sdf";
    public static String[] PATHS_TO_COCONUT = new String[]{"/data/coconut/acd_coconut_1.sdf",
                                                           "/data/coconut/acd_coconut_2.sdf",
                                                           "/data/coconut/acd_coconut_3.sdf",
                                                           "/data/coconut/acd_coconut_4.sdf",
                                                           "/data/coconut/acd_coconut_5.sdf",
                                                           "/data/coconut/acd_coconut_6.sdf",
                                                           "/data/coconut/acd_coconut_7.sdf",
                                                           "/data/coconut/acd_coconut_8.sdf",
                                                           "/data/coconut/acd_coconut_9.sdf",
                                                           "/data/coconut/acd_coconut_10.sdf",
                                                           "/data/coconut/acd_coconut_11.sdf",
                                                           "/data/coconut/acd_coconut_12.sdf",
                                                           "/data/coconut/acd_coconut_13.sdf",
                                                           "/data/coconut/acd_coconut_14.sdf",
                                                           "/data/coconut/acd_coconut_15.sdf",
                                                           "/data/coconut/acd_coconut_16.sdf",
                                                           "/data/coconut/acd_coconut_17.sdf",
                                                           "/data/coconut/acd_coconut_18.sdf"};

    public static void main(final String[] args) {
        SpringApplication.run(SherlockDbServiceDatasetApplication.class, args);
    }
}
