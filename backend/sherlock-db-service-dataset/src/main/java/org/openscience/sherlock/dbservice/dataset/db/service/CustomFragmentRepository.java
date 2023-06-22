package org.openscience.sherlock.dbservice.dataset.db.service;

import java.util.List;

public interface CustomFragmentRepository {

    //    List<Integer> findByWITH(final List<String> singleBitList);
    //
    //    List<String> findAllSubDataSetStringsById(final Collection<Integer> ids);
    //
    //    List<String> findBySetBits(final String setBitsString);

    List<String> findByTableName(final String tableName);

    List<String> findBySetBits(final String setBitsString, final String tableName);

    void createTable(String tableName, final int nBits);

    void dropTable(String tableName);

    void renameTable(String tableName, String newTableName);

    void insertIntoTable(final String tableName, final String nucleus, final String setBits, final int nBits,
                         final String subDataSetString);
}
