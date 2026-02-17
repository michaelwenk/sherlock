package org.openscience.sherlock.dbservice.dataset.utils;

import org.openscience.sherlock.dbservice.dataset.config.DatasetJpaConfig;

public class QueryUtilities {

    public static String buildFindByTableName() {
        return "SELECT sub_data_set_string FROM "
                + DatasetJpaConfig.FRAGMENT_TABLE_NAME
                + ";";
    }

    public static String buildFindByIdQuery(final long id) {

        return "SELECT sub_data_set_string FROM "
                + DatasetJpaConfig.FRAGMENT_TABLE_NAME
                + " WHERE id = " + id + ";";
    }

    public static String buildFindBySetBitsQuery(final String nucleus, final String bitString) {
        final int[] setBitsIndices = BitUtilities.extractSetBitsIndices(bitString);
        final String query = "SELECT id FROM "
                + DatasetJpaConfig.FRAGMENT_TABLE_NAME
                + " WHERE nucleus = '" + nucleus + "'"
                + " AND n_set_bits <= " + setBitsIndices.length
                + " AND bit_string & B'" + bitString + "' = bit_string;";

        System.out.println("Built query: " + query);

        return query;
    }

    public static String buildCreateFragmentsTable(final int nBits) {
        return "CREATE TABLE "
                + DatasetJpaConfig.FRAGMENT_TABLE_NAME
                + "(id SERIAL PRIMARY KEY NOT NULL, nucleus VARCHAR(5) NOT NULL, sub_data_set_string TEXT NOT NULL, n_set_bits INTEGER NOT NULL, n_bits INTEGER NOT NULL, bit_string BIT("
                + nBits + ") NOT NULL);";
    }

    public static String buildInsertIntoFragmentsTable(final int nBits) {
        return "INSERT INTO "
                + DatasetJpaConfig.FRAGMENT_TABLE_NAME
                + " (nucleus, sub_data_set_string, n_set_bits, n_bits, bit_string) VALUES (?, ?\\:\\:TEXT, ?, ?, ?\\:\\:BIT("
                + nBits
                + "));";
    }

    public static String buildDropTable(final String tableName) {
        return "DROP TABLE IF EXISTS "
                + tableName
                + ";";
    }
}
