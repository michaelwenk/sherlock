package org.openscience.sherlock.dbservice.dataset.utils;

public class QueryUtilities {

    //    public static String buildFindByWITHQuery(final List<String> singleBitList) {
    //        final String excludeListString = "exclude_list_";
    //        final StringBuilder queryStringBuilder = new StringBuilder("WITH ");
    //        final List<String> excludeListStrings = new ArrayList<>();
    //        String singleBitString;
    //        for (int i = 0; i
    //                < singleBitList.size(); i++) {
    //            singleBitString = singleBitList.get(i);
    //            excludeListStrings.add(excludeListString
    //                                           + i);
    //            queryStringBuilder.append(excludeListStrings.get(i))
    //                              .append(" AS (SELECT id FROM fragment_record WHERE set_bits & ")
    //                              .append(singleBitString)
    //                              .append(" = ")
    //                              .append(singleBitString)
    //                              .append(")");
    //            if (i
    //                    < singleBitList.size()
    //                    - 1) {
    //                queryStringBuilder.append(",\n");
    //            }
    //        }
    //        queryStringBuilder.append("\n");
    //        queryStringBuilder.append("SELECT f.id\n");
    //        queryStringBuilder.append("FROM fragment_record f ");
    //        for (final String listString : excludeListStrings) {
    //            queryStringBuilder.append("LEFT JOIN ")
    //                              .append(listString)
    //                              .append(" ON ")
    //                              .append("f.id = ")
    //                              .append(listString)
    //                              .append(".id ");
    //        }
    //        queryStringBuilder.append("\n");
    //        queryStringBuilder.append("WHERE ");
    //        for (int i = 0; i
    //                < excludeListStrings.size(); i++) {
    //            queryStringBuilder.append(excludeListStrings.get(i))
    //                              .append(".id IS NULL");
    //            if (i
    //                    < excludeListStrings.size()
    //                    - 1) {
    //                queryStringBuilder.append(" AND ");
    //            }
    //        }
    //        queryStringBuilder.append(";");
    //        System.out.println("\n -> query: "
    //                                   + queryStringBuilder);
    //
    //        return queryStringBuilder.toString();
    //    }
    //
    //    public static String buildFindAllSubDataSetStringsByIdQuery(final Collection<Integer> ids) {
    //        final StringBuilder queryStringBuilder = new StringBuilder();
    //        queryStringBuilder.append("SELECT sub_data_set_string ")
    //                          .append("FROM fragment_record ")
    //                          .append("WHERE id IN (");
    //        int i = 0;
    //        for (final int id : ids) {
    //            queryStringBuilder.append(id);
    //            if (i
    //                    < ids.size()
    //                    - 1) {
    //                queryStringBuilder.append(", ");
    //            }
    //            i++;
    //        }
    //        queryStringBuilder.append(");");
    //        //        System.out.println("\n -> query: "
    //        //                                   + queryStringBuilder);
    //
    //        return queryStringBuilder.toString();
    //}

    //    public static String buildFindBySingleBitsQuery(final String setBitsString) {
    //        return "SELECT sub_data_set_string FROM fragment_record WHERE set_bits & "
    //                + setBitsString
    //                + " = set_bits;";
    //}

    public static String buildFindByTableName(final String tableName) {
        return "SELECT sub_data_set_string FROM "
                + tableName
                + ";";
    }

    public static String buildFindBySingleBitsQuery(final String tableName, final String setBitsString) {
        return "SELECT sub_data_set_string FROM "
                + tableName
                + " WHERE set_bits & "
                + setBitsString
                + " = set_bits;";
    }

    public static String buildCreateTable(final String tableName, final int nBits) {
        return "CREATE TABLE IF NOT EXISTS "
                + tableName
                + "(id SERIAL PRIMARY KEY NOT NULL, nucleus VARCHAR(5) NOT NULL, set_bits BIT("
                + nBits
                + ") NOT NULL, n_bits INTEGER NOT NULL, sub_data_set_string TEXT);";
    }

    public static String buildInsertIntoTable(final String tableName, final String nucleus, final String setBits,
                                              final int nBits, final String subDataSetString) {
        return "INSERT INTO "
                + tableName
                + "(nucleus, set_bits, n_bits, sub_data_set_string) VALUES ("
                + nucleus
                + ", CAST("
                + setBits
                + " AS BIT("
                + nBits
                + ")), "
                //                + subDataSetString
                + subDataSetString.replaceAll(":", "\\:")
                + ");";
    }

    public static String buildDropTable(final String tableName) {
        return "DROP TABLE IF EXISTS "
                + tableName
                + ";";
    }

    public static String buildRenameTable(final String tableName, final String newTableName) {
        return "ALTER TABLE IF EXISTS "
                + tableName
                + " RENAME TO "
                + newTableName
                + ";";
    }
}
