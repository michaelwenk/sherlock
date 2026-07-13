package org.openscience.sherlock.utils.elucidation.job;

public class JobCancelledException extends RuntimeException {
    public JobCancelledException(final String message) {
        super(message);
    }
}