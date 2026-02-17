package org.openscience.sherlock.dbservice.dataset.db.service.jpa;

import org.openscience.sherlock.dbservice.dataset.config.DatasetJpaConfig;
import org.openscience.sherlock.dbservice.dataset.utils.BitUtilities;
import org.openscience.sherlock.dbservice.dataset.utils.QueryUtilities;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class CustomFragmentRepositoryImplementation
                implements CustomFragmentRepository {

        @PersistenceContext
        private final EntityManager entityManager;

        public CustomFragmentRepositoryImplementation(final EntityManager entityManager) {
                this.entityManager = entityManager;
        }

        @Override
        public List<String> findBySetBits(final String nucleus, final String bitString) {
                final List<Integer> resultIDs = this.entityManager
                                .createNativeQuery(QueryUtilities.buildFindBySetBitsQuery(nucleus, bitString))
                                .getResultList();

                System.out.println("Query result IDs: " + resultIDs.size());

                final List<String> subDataSetStringList = Collections.synchronizedList(new ArrayList<>());
                resultIDs.parallelStream()
                                .forEach(id -> {
                                        final String subDataSetString = (String) this.entityManager
                                                        .createNativeQuery(QueryUtilities.buildFindByIdQuery(id))
                                                        .getSingleResult();
                                        subDataSetStringList.add(subDataSetString);
                                });

                System.out.println("Retrieved subDataSetStringList size: " + subDataSetStringList.size());

                return subDataSetStringList;
        }

        @Transactional
        @Override
        public void createFragmentsTable(final int nBits) {
                this.entityManager.createNativeQuery(QueryUtilities.buildCreateFragmentsTable(nBits))
                                .executeUpdate();
        }

        @Transactional
        @Override
        public void dropTable(final String tableName) {
                this.entityManager.createNativeQuery(QueryUtilities.buildDropTable(tableName))
                                .executeUpdate();
        }

        @Transactional
        @Override
        public void insertIntoTable(final String nucleus, final String bitString, final int nBits,
                        final String subDataSetString) {
                // insert into fragment table
                final int nSetBits = BitUtilities.countSetBits(bitString);
                String queryString = QueryUtilities.buildInsertIntoFragmentsTable(nBits);
                this.entityManager.createNativeQuery(queryString)
                                .setParameter(1, nucleus)
                                .setParameter(2, subDataSetString)
                                .setParameter(3, nSetBits)
                                .setParameter(4, nBits)
                                .setParameter(5, bitString)
                                .executeUpdate();
        }

        @Transactional
        @Override
        public void createIndicesAndAnalyze() {
                // analyze table first time
                System.out.println(
                                "Analyzing '" + DatasetJpaConfig.FRAGMENT_TABLE_NAME + "' table before indexing.");
                final String analyzeQuery = "ANALYZE " + DatasetJpaConfig.FRAGMENT_TABLE_NAME + ";";
                this.entityManager.createNativeQuery(analyzeQuery)
                                .executeUpdate();
                System.out.println(
                                "Analyzing '" + DatasetJpaConfig.FRAGMENT_TABLE_NAME + "' table -> DONE.");

                // create indices on fragments table
                System.out.println("Creating indices for '" + DatasetJpaConfig.FRAGMENT_TABLE_NAME + "' table...");
                // create index on id, nucleus, n_set_bits and bit_string
                final String idNucleusNSetBitsBitStringIndexQuery = "CREATE INDEX idx_id_nucleus_n_set_bits_bit_string"
                                + " ON "
                                + DatasetJpaConfig.FRAGMENT_TABLE_NAME
                                + " (id, nucleus, n_set_bits, bit_string);";
                this.entityManager.createNativeQuery(idNucleusNSetBitsBitStringIndexQuery)
                                .executeUpdate();
                // create index on id and sub_data_set_string to improve retrieval of
                // sub_data_set_string by id
                final String idSubDataSetStringIndexQuery = "CREATE INDEX idx_id_sub_data_set_string"
                                + " ON "
                                + DatasetJpaConfig.FRAGMENT_TABLE_NAME
                                + " (id, sub_data_set_string);";
                this.entityManager.createNativeQuery(idSubDataSetStringIndexQuery)
                                .executeUpdate();
                System.out.println(
                                "Creating indices for '" + DatasetJpaConfig.FRAGMENT_TABLE_NAME + "' table -> DONE.");

                // analyze table after creating indices
                System.out.println(
                                "Analyzing '" + DatasetJpaConfig.FRAGMENT_TABLE_NAME + "' table after indexing.");
                this.entityManager.createNativeQuery(analyzeQuery)
                                .executeUpdate();
                System.out.println(
                                "Analyzing '" + DatasetJpaConfig.FRAGMENT_TABLE_NAME + "' table -> DONE.");
        }
}
