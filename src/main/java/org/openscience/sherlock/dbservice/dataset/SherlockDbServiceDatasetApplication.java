package org.openscience.sherlock.dbservice.dataset;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SherlockDbServiceDatasetApplication {

    public static final String PATH_TO_NMRSHIFTDB = "/data/nmrshiftdb/nmrshiftdb.sdf";
    public static String[] PATHS_TO_COCONUT = new String[] { "/data/coconut/acd_coconut_0.sdf",
            "/data/coconut/acd_coconut_1.sdf", "/data/coconut/acd_coconut_2.sdf",
            "/data/coconut/acd_coconut_3.sdf" };

    public static void main(final String[] args) {
        SpringApplication.run(SherlockDbServiceDatasetApplication.class, args);
    }
}
