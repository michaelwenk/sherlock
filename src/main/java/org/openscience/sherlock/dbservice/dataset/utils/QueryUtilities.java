package org.openscience.sherlock.dbservice.dataset.utils;

import org.openscience.sherlock.dbservice.dataset.config.DatasetJpaConfig;

public class QueryUtilities {

    public static String buildFindByTableName() {
        return "SELECT sub_data_set_string FROM "
                + DatasetJpaConfig.FRAGMENT_TABLE_NAME
                + ";";
    }

    public static String buildFindBySetBitsQuery(final String nucleus, final String bitString) {
        final int[] setBitsIndices = BitUtilities.extractSetBitsIndices(bitString);
        final StringBuilder bitStringBuilder = new StringBuilder();
        for (int i = 0; i < setBitsIndices.length; i++) {
            bitStringBuilder.append(setBitsIndices[i]);
            if (i < setBitsIndices.length - 1) {
                bitStringBuilder.append(", ");
            }
        }
        final String query = "SELECT sub_data_set_string FROM "
                + DatasetJpaConfig.FRAGMENT_TABLE_NAME
                + " WHERE id IN (SELECT id FROM "
                + DatasetJpaConfig.BITS_TABLE_NAME
                + " WHERE nucleus = '" + nucleus + "'"
                + " AND n_set_bits <= " + setBitsIndices.length
                + " AND bit_array <@ ARRAY["
                + bitStringBuilder.toString()
                + "]);";

        System.out.println("Built query: " + query);

        return query;
    }

    public static String buildCreateFragmentsTable() {
        return "CREATE TABLE "
                + DatasetJpaConfig.FRAGMENT_TABLE_NAME
                + "(id SERIAL PRIMARY KEY NOT NULL, nucleus VARCHAR(5) NOT NULL, sub_data_set_string TEXT);";
    }

    public static String buildCreateBitsTable(final int nBits) {
        return "CREATE TABLE "
                + DatasetJpaConfig.BITS_TABLE_NAME
                + "(id SERIAL REFERENCES " + DatasetJpaConfig.FRAGMENT_TABLE_NAME
                + "(id), nucleus VARCHAR(5) NOT NULL, n_set_bits INTEGER NOT NULL, n_bits INTEGER NOT NULL, bit_array INTEGER[] NOT NULL, PRIMARY KEY (id, nucleus));";

    }

    public static String buildInsertIntoFragmentsTable() {
        return "INSERT INTO "
                + DatasetJpaConfig.FRAGMENT_TABLE_NAME
                + " (nucleus, sub_data_set_string) VALUES (?, ?\\:\\:TEXT) RETURNING id;";
    }

    public static String buildInsertIntoBitsTable() {
        return "INSERT INTO "
                + DatasetJpaConfig.BITS_TABLE_NAME
                + " (id, nucleus, n_set_bits, n_bits, bit_array) VALUES (?, ?, ?, ?, ?);";

    }

    public static String buildDropTable(final String tableName) {
        return "DROP TABLE IF EXISTS "
                + tableName
                + ";";
    }
}
