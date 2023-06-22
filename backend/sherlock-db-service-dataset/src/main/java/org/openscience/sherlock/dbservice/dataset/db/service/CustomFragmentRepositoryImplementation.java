package org.openscience.sherlock.dbservice.dataset.db.service;

import org.openscience.sherlock.dbservice.dataset.utils.QueryUtilities;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.List;

@Service
public class CustomFragmentRepositoryImplementation
        implements CustomFragmentRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    public CustomFragmentRepositoryImplementation(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    //    @Override
    //    public List<Integer> findByWITH(final List<String> singleBitList) {
    //        return this.entityManager.createNativeQuery(QueryUtilities.buildFindByWITHQuery(singleBitList))
    //                                 .getResultList();
    //    }
    //
    //    @Override
    //    public List<String> findAllSubDataSetStringsById(final Collection<Integer> ids) {
    //        return this.entityManager.createNativeQuery(QueryUtilities.buildFindAllSubDataSetStringsByIdQuery(ids))
    //                                 .getResultList();
    //    }
    //
    //    @Override
    //    public List<String> findBySetBits(final String setBitsString) {
    //        return this.entityManager.createNativeQuery(QueryUtilities.buildFindBySingleBitsQuery(setBitsString))
    //                                 .getResultList();
    //    }

    @Override
    public List<String> findByTableName(final String tableName) {
        return this.entityManager.createNativeQuery(QueryUtilities.buildFindByTableName(tableName))
                                 .getResultList();
    }

    @Override
    public List<String> findBySetBits(final String tableName, final String setBitsString) {
        return this.entityManager.createNativeQuery(QueryUtilities.buildFindBySingleBitsQuery(tableName, setBitsString))
                                 .getResultList();
    }

    @Transactional
    @Override
    public void createTable(final String tableName, final int nBits) {
        this.entityManager.createNativeQuery(QueryUtilities.buildCreateTable(tableName, nBits))
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
    public void renameTable(final String tableName, final String newTableName) {
        this.entityManager.createNativeQuery(QueryUtilities.buildRenameTable(tableName, newTableName))
                          .executeUpdate();
    }

    @Transactional
    @Override
    public void insertIntoTable(final String tableName, final String nucleus, final String setBits, final int nBits,
                                final String subDataSetString) {

        //        String query = QueryUtilities.buildInsertIntoTable(tableName, nucleus, setBits, nBits, subDataSetString);
        final String query = "INSERT INTO "
                + tableName
                + " (nucleus, set_bits, n_bits, sub_data_set_string) VALUES (?, ?\\:\\:BIT("
                + nBits
                + "), ?, ?\\:\\:TEXT)";
        //        query = query.replaceAll(":", "\\:");

        this.entityManager.createNativeQuery(query)
                          .setParameter(1, nucleus)
                          .setParameter(2, setBits)
                          .setParameter(3, nBits)
                          .setParameter(4, subDataSetString)
                          .executeUpdate();

    }
}
