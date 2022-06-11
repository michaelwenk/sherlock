package org.openscience.sherlock.dbservice.dataset.db.service;

import java.util.Collection;
import java.util.List;

public interface CustomFragmentRepository {

    List<Integer> findByWITH(final List<String> singleBitList);

    List<String> findAllSubDataSetStringsById(final Collection<Integer> ids);

    List<String> findBySetBits(final String setBitsString);
}
