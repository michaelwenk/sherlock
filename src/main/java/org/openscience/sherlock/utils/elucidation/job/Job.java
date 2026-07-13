package org.openscience.sherlock.utils.elucidation.job;

import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
public class Job implements Runnable {
    private String id;
    private String name;
    private String errorMessage;
    private String requestData;
    private volatile Long processId;
    private volatile Process process;

    public Job(final String id, final String name) {
        this(id, name, null, null);
    }

    public Job(final String id, final String name, final String errorMessage) {
        this(id, name, errorMessage, null);
    }

    public Job(final String id, final String name, final String errorMessage, final String requestData) {
        this.id = id;
        this.name = name;
        this.errorMessage = errorMessage;
        this.requestData = requestData;
        this.processId = null;
        this.process = null;
    }

    public void attachProcess(final Process process) {
        this.process = process;
        this.processId = process == null ? null : process.pid();
    }

    public boolean destroyProcess() {
        final Process currentProcess = process;
        if (currentProcess == null) {
            return false;
        }

        final ProcessHandle root = currentProcess.toHandle();
        final List<ProcessHandle> descendants = root.descendants()
                .sorted(Comparator.comparingLong((ProcessHandle handle) -> handle.pid()).reversed())
                .toList();

        for (ProcessHandle descendant : descendants) {
            if (descendant.isAlive()) {
                descendant.destroyForcibly();
            }
        }

        if (root.isAlive()) {
            root.destroyForcibly();
        }

        try {
            currentProcess.waitFor(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return !currentProcess.isAlive();
    }

    public void clearProcess() {
        process = null;
    }

    @Override
    public void run() {
        // default implementation does nothing
    }
}
