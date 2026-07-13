package org.openscience.sherlock.dbservice.job.repository;

import java.util.Collection;
import java.util.List;

import org.openscience.sherlock.dbservice.job.model.JobRecord;
import org.openscience.sherlock.utils.elucidation.job.JobState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRecordRepository extends JpaRepository<JobRecord, String> {
    List<JobRecord> findAllByState(JobState state);

    List<JobRecord> findAllByStateIn(Collection<JobState> states);

    List<JobRecord> findAll();

    JobRecord findByJobId(String jobId);
}