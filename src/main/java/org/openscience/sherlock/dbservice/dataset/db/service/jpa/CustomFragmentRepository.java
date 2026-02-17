package org.openscience.sherlock.dbservice.dataset.db.service.jpa;

import java.util.List;

public interface CustomFragmentRepository {

    List<String> findBySetBits(final String nucleus, final String bitString);

    void createFragmentsTable(final int nBits);

    void dropTable(String tableName);

    void insertIntoTable(final String nucleus, final String bitString, final int nBits,
            final String subDataSetString);

    void createIndicesAndAnalyze();
}
