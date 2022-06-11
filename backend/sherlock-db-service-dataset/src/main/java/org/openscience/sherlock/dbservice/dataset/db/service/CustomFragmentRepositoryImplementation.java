package org.openscience.sherlock.dbservice.dataset.db.service;

import org.openscience.sherlock.dbservice.dataset.utils.QueryUtilities;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Collection;
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
    public List<Integer> findByWITH(final List<String> singleBitList) {
        return this.entityManager.createNativeQuery(QueryUtilities.buildFindByWITHQuery(singleBitList))
                                 .getResultList();
    }

    @Override
    public List<String> findAllSubDataSetStringsById(final Collection<Integer> ids) {
        return this.entityManager.createNativeQuery(QueryUtilities.buildFindAllSubDataSetStringsByIdQuery(ids))
                                 .getResultList();
    }

    @Override
    public List<String> findBySetBits(final String setBitsString) {
        return this.entityManager.createNativeQuery(QueryUtilities.buildFindBySingleBitsQuery(setBitsString))
                                 .getResultList();
    }
}
