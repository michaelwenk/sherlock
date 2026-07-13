package org.openscience.sherlock.utils.elucidation.job;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class JobSnapshot {
    private final String jobId;
    private final JobState state;
    private final String errorMessage;
    private final Long processId;

}
