package org.openscience.sherlock.dbservice.dataset.db.service.jpa;

import org.openscience.sherlock.dbservice.dataset.config.DatasetJpaConfig;
import org.openscience.sherlock.dbservice.dataset.utils.BitUtilities;
import org.openscience.sherlock.dbservice.dataset.utils.QueryUtilities;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
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
                return this.entityManager
                                .createNativeQuery(QueryUtilities.buildFindBySetBitsQuery(nucleus, bitString))
                                .getResultList();
        }

        @Transactional
        @Override
        public void createFragmentsTable() {
                this.entityManager.createNativeQuery(QueryUtilities.buildCreateFragmentsTable())
                                .executeUpdate();
        }

        @Transactional
        @Override
        public void createBitsTable(final int nBits) {
                this.entityManager.createNativeQuery(QueryUtilities.buildCreateBitsTable(nBits))
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
                String queryString = QueryUtilities.buildInsertIntoFragmentsTable();
                final long fragmentRecordId = ((Number) this.entityManager.createNativeQuery(queryString)
                                .setParameter(1, nucleus)
                                .setParameter(2, subDataSetString)
                                .getSingleResult()).longValue();
                // Insert into bits table
                queryString = QueryUtilities.buildInsertIntoBitsTable();
                final Query query = this.entityManager.createNativeQuery(queryString);
                query.setParameter(1, fragmentRecordId)
                                .setParameter(2, nucleus)
                                .setParameter(3, nSetBits)
                                .setParameter(4, nBits);
                // set bits parameters
                query.setParameter(5, BitUtilities.extractSetBitsIndices(bitString));
                query.executeUpdate();
        }

        @Transactional
        @Override
        public void createIndices(final int nBits) {
                // create indices on fragments table
                // create index on id
                System.out.println("Creating indices for fragments and bits table...");
                final String idIndexQuery = "CREATE INDEX idx_fragment_id"
                                + " ON "
                                + DatasetJpaConfig.FRAGMENT_TABLE_NAME
                                + " (id);";
                this.entityManager.createNativeQuery(idIndexQuery)
                                .executeUpdate();
                // create indices on bits table
                // create index on n_set_bits
                final String nSetBitsIndexQuery = "CREATE INDEX idx_n_set_bits"
                                + " ON "
                                + DatasetJpaConfig.BITS_TABLE_NAME
                                + " (n_set_bits);";
                this.entityManager.createNativeQuery(nSetBitsIndexQuery)
                                .executeUpdate();
                // create index on n_bits
                final String nBitsIndexQuery = "CREATE INDEX idx_n_bits"
                                + " ON "
                                + DatasetJpaConfig.BITS_TABLE_NAME
                                + " (n_bits);";
                this.entityManager.createNativeQuery(nBitsIndexQuery)
                                .executeUpdate();
                // create index on nucleus
                final String nucleusIndexQuery = "CREATE INDEX idx_nucleus"
                                + " ON "
                                + DatasetJpaConfig.BITS_TABLE_NAME
                                + " (nucleus);";
                this.entityManager.createNativeQuery(nucleusIndexQuery)
                                .executeUpdate();
                final String bitArrayIndexQuery = "CREATE INDEX idx_bit_array"
                                + " ON "
                                + DatasetJpaConfig.BITS_TABLE_NAME
                                + " USING GIN (bit_array);";
                this.entityManager.createNativeQuery(bitArrayIndexQuery)
                                .executeUpdate();
                System.out.println("Creating indices for fragments and bits table... DONE.");
        }
}
