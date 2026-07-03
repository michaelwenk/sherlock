package org.openscience.sherlock.utils.elucidation;

import casekit.io.FileSystem;
import casekit.nmr.elucidation.lsd.PyLSDInputFileBuilder;
import casekit.nmr.elucidation.model.Detections;
import casekit.nmr.elucidation.model.Grouping;
import casekit.nmr.filterandrank.FilterAndRank;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.SpectrumCompact;
import casekit.nmr.model.nmrium.Correlation;
import casekit.nmr.model.nmrium.Correlations;
import casekit.nmr.utils.Utils;

import org.bson.types.ObjectId;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.MDLV3000Reader;
import org.openscience.sherlock.controller.ResultController;
import org.openscience.sherlock.dbservice.result.model.ResultRecord;
import org.openscience.sherlock.model.DetectionOptions;
import org.openscience.sherlock.model.ElucidationOptions;
import org.openscience.sherlock.model.exchange.RequestResult;
import org.openscience.sherlock.model.exchange.Transfer;
import org.openscience.sherlock.utils.Utilities;
import org.openscience.sherlock.utils.detection.Detection;
import org.openscience.sherlock.utils.elucidation.job.GlobalJobScheduler;
import org.openscience.sherlock.utils.elucidation.job.Job;
import org.openscience.sherlock.utils.elucidation.job.JobScheduler.JobSnapshot;
import org.openscience.sherlock.utils.elucidation.job.JobScheduler.JobState;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class PyLSD {
        private final static String pathToPyLSDExecutableFolder = "/data/lsd/PyLSD/Variant/";
        private final static String pathToPyLSDInputFileFolder = "/data/lsd/PyLSD/Variant/";
        private final static String pathToPyLSDResultFileFolder = "/data/lsd/PyLSD/Variant/";
        private final static String pathToNeighborsFilesFolder = "/data/lsd/PyLSD/Variant/";
        private final static String pathToFragmentsFilesFolder = "/data/lsd/PyLSD/Variant/";
        private final static String[] directoriesToCheck = new String[] { pathToPyLSDInputFileFolder,
                        pathToPyLSDResultFileFolder,
                        pathToNeighborsFilesFolder,
                        pathToFragmentsFilesFolder };

        private final Detection detection;
        private final Prediction prediction;
        private final ResultController resultController;

        public PyLSD(final Detection detection, final Prediction prediction, final ResultController resultController) {
                this.detection = detection;
                this.prediction = prediction;
                this.resultController = resultController;
        }

        private final Map<String, RequestResult> asyncResults = new ConcurrentHashMap<>();

        private Transfer createQueryTransfer(final String requestId, final String taskName,
                        final Correlations correlations,
                        final SpectrumCompact querySpectrum, final boolean detected,
                        final DetectionOptions detectionOptions, final Detections detections,
                        final Grouping grouping, final ElucidationOptions elucidationOptions) {

                final Transfer requestTransfer = new Transfer();
                requestTransfer.setRequestId(requestId);
                requestTransfer.setTaskName(taskName);
                requestTransfer.setCorrelations(correlations);
                requestTransfer.setQuerySpectrum(querySpectrum.toSpectrum());
                requestTransfer.setDetected(detected);
                requestTransfer.setDetectionOptions(detectionOptions);
                requestTransfer.setDetections(detections);
                requestTransfer.setGrouping(grouping);
                requestTransfer.setElucidationOptions(elucidationOptions);

                final String mf = Utilities.getMolecularFormulaFromCorrelations(correlations);
                requestTransfer.setMf(mf);

                return requestTransfer;
        }

        public ResponseEntity<RequestResult> runPyLSD(final String requestId,
                        final String taskName, final Correlations correlations, final SpectrumCompact querySpectrum,
                        final boolean detected, final DetectionOptions detectionOptions, final Detections detections,
                        final Grouping grouping, final ElucidationOptions elucidationOptions) {

                final Transfer requestTransfer = createQueryTransfer(requestId, taskName, correlations, querySpectrum,
                                detected, detectionOptions, detections, grouping, elucidationOptions);

                return executePyLSD(null, requestTransfer, false);
        }

        public void schedulePyLSD(final String requestId,
                        final String taskName, final Correlations correlations, final SpectrumCompact querySpectrum,
                        final boolean detected, final DetectionOptions detectionOptions, final Detections detections,
                        final Grouping grouping, final ElucidationOptions elucidationOptions) {

                GlobalJobScheduler.get()
                                .scheduleJob(new Job(requestId,
                                                "PyLSD" + "_" + (taskName != null
                                                                && !taskName.isEmpty()
                                                                                ? taskName
                                                                                : requestId)) {
                                        @Override
                                        public void run() {
                                                final Transfer requestTransfer = createQueryTransfer(requestId,
                                                                taskName, correlations, querySpectrum, detected,
                                                                detectionOptions, detections, grouping,
                                                                elucidationOptions);

                                                final ResponseEntity<RequestResult> responseEntity = executePyLSD(
                                                                this, requestTransfer, true);
                                                RequestResult responseBody = responseEntity.getBody();
                                                if (responseBody == null) {
                                                        responseBody = new RequestResult();
                                                        responseBody.setRequestId(requestId);
                                                        responseBody.setErrorMessage("PyLSD job returned no body");
                                                }
                                                if (responseBody.getRequestId() == null
                                                                || responseBody.getRequestId().isBlank()) {
                                                        responseBody.setRequestId(requestId);
                                                }

                                                asyncResults.put(requestId, responseBody);

                                                if (responseEntity.getStatusCode().isError()) {
                                                        final String message = responseBody != null
                                                                        && responseBody.getErrorMessage() != null
                                                                                        ? responseBody.getErrorMessage()
                                                                                        : "PyLSD job with requestId "
                                                                                                        + requestId
                                                                                                        + " failed";
                                                        setErrorMessage(message);
                                                        throw new IllegalStateException(message);
                                                }
                                        }
                                });

        }

        public ResponseEntity<RequestResult> getAsyncResult(final String requestId) {
                final RequestResult result = asyncResults.get(requestId);
                if (result != null) {
                        return new ResponseEntity<>(result, HttpStatus.OK);
                }

                final JobSnapshot snapshot = GlobalJobScheduler.get().getJobSnapshot(requestId);
                if (snapshot == null) {
                        final RequestResult responseTransfer = new RequestResult();
                        responseTransfer.setRequestId(requestId);
                        responseTransfer.setErrorMessage("No job found for request id: " + requestId);
                        return new ResponseEntity<>(responseTransfer, HttpStatus.NOT_FOUND);
                }

                final RequestResult responseTransfer = new RequestResult();
                responseTransfer.setRequestId(requestId);

                if (snapshot.getState() == JobState.QUEUED || snapshot.getState() == JobState.RUNNING) {
                        responseTransfer.setErrorMessage("Job is still running");
                        return new ResponseEntity<>(responseTransfer, HttpStatus.ACCEPTED);
                }

                if (snapshot.getState() == JobState.CANCELLED) {
                        responseTransfer.setErrorMessage("Job was cancelled");
                        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
                }

                if (snapshot.getState() == JobState.ERROR) {
                        responseTransfer.setErrorMessage(
                                        snapshot.getErrorMessage() == null || snapshot.getErrorMessage().isBlank()
                                                        ? "Job failed"
                                                        : snapshot.getErrorMessage());
                        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
                }

                responseTransfer.setErrorMessage("Job is done but result is not available");
                return new ResponseEntity<>(responseTransfer, HttpStatus.NOT_FOUND);
        }

        private ResponseEntity<RequestResult> executePyLSD(final Job job, final Transfer requestTransfer,
                        final boolean storeResult) {

                // build PyLSD input file
                requestTransfer.getElucidationOptions()
                                .setPathToNeighborsFiles(pathToNeighborsFilesFolder
                                                + requestTransfer.getRequestId()
                                                + "_neighbor");
                requestTransfer.getElucidationOptions()
                                .setPathToFragmentFiles(pathToFragmentsFilesFolder
                                                + requestTransfer.getRequestId()
                                                + "_fragment");

                final Transfer queryResultTransfer = createPyLSDInputFiles(requestTransfer);
                final RequestResult requestResult = new RequestResult();
                requestResult.setRequestId(requestTransfer.getRequestId());

                String pyLSDInputFileContent, pathToPyLSDInputFile, requestIdTemp;
                ProcessBuilder processBuilder;
                Process process = null;
                final List<DataSet> dataSetList = new ArrayList<>();
                boolean pyLSDRunWasSuccessful;
                List<DataSet> dataSetListTemp;
                boolean stop = false;
                for (int i = 0; i < queryResultTransfer.getPyLSDInputFileContentList()
                                .size(); i++) {
                        pyLSDInputFileContent = queryResultTransfer.getPyLSDInputFileContentList()
                                        .get(i);
                        requestIdTemp = requestResult.getRequestId()
                                        + "_"
                                        + i;
                        System.out.println("\n----------------------\n -> i: "
                                        + i
                                        + " -> \n"
                                        + pyLSDInputFileContent
                                        + "\n----------------------\n");

                        pathToPyLSDInputFile = pathToPyLSDInputFileFolder
                                        + requestIdTemp
                                        + ".pylsd";

                        // run PyLSD if file was written successfully
                        if (FileSystem.writeFile(pathToPyLSDInputFile, pyLSDInputFileContent)) {
                                // System.out.println("--> has been written successfully: "
                                // + pathToPyLSDInputFile);
                                try {
                                        // try to execute PyLSD
                                        processBuilder = new ProcessBuilder();
                                        processBuilder.directory(new File(pathToPyLSDExecutableFolder))
                                                        .redirectError(new File(pathToPyLSDInputFileFolder
                                                                        + requestIdTemp
                                                                        + "_error.txt"))
                                                        .redirectOutput(new File(pathToPyLSDInputFileFolder
                                                                        + requestIdTemp
                                                                        + "_log.txt"))
                                                        .command("python", pathToPyLSDExecutableFolder
                                                                        + "lsd.py", pathToPyLSDInputFile);
                                        process = processBuilder.start();
                                        if (job != null) {
                                                job.attachProcess(process);
                                        }
                                        pyLSDRunWasSuccessful = process.waitFor(
                                                        requestTransfer.getElucidationOptions().getTimeLimitTotal(),
                                                        TimeUnit.MINUTES);
                                        if (pyLSDRunWasSuccessful) {
                                                if (job != null) {
                                                        job.clearProcess();
                                                }
                                                System.out.println("\n\n--> run was successful");
                                                final String pathToSmilesFile = pathToPyLSDResultFileFolder
                                                                + requestIdTemp
                                                                + "_0.smiles";

                                                dataSetListTemp = prediction.parseAndPredictFromSmilesFile(
                                                                requestTransfer.getCorrelations(),
                                                                requestTransfer.getElucidationOptions(),
                                                                requestTransfer.getDetections(),
                                                                pathToSmilesFile);
                                                if (dataSetListTemp == null) {
                                                        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                                                }
                                                System.out.println("\n\n--> parse and prediction was successful");
                                                for (final DataSet dataSet : dataSetListTemp) {
                                                        if (dataSetList.stream()
                                                                        .noneMatch(ds -> ds.getMeta()
                                                                                        .get("smiles")
                                                                                        .equals(dataSet.getMeta()
                                                                                                        .get("smiles")))) {
                                                                dataSetList.add(dataSet);
                                                        }
                                                }
                                        } else {
                                                System.out.println(
                                                                "--> " + requestResult.getRequestId()
                                                                                + ": reached time limit "
                                                                                + requestTransfer
                                                                                                .getElucidationOptions()
                                                                                                .getTimeLimitTotal()
                                                                                + " -> run was NOT successful -> killing PyLSD run if it is still running");
                                                stopProcessTree(job, process);
                                                if (job != null) {
                                                        job.clearProcess();
                                                }
                                                requestResult.setErrorMessage(
                                                                requestResult.getRequestId()
                                                                                + ": Time limit reached ("
                                                                                + requestTransfer
                                                                                                .getElucidationOptions()
                                                                                                .getTimeLimitTotal()
                                                                                + ") -> elucidation request was canceled!!!");
                                                stop = true;
                                        }
                                } catch (final InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                        stopProcessTree(job, process);
                                        requestResult.setErrorMessage("PyLSD execution was interrupted and canceled");
                                        if (job != null) {
                                                job.clearProcess();
                                        }
                                        stop = true;
                                } catch (final Exception e) {
                                        e.printStackTrace();
                                        stopProcessTree(job, process);
                                        requestResult.setErrorMessage(e.getMessage());
                                        if (job != null) {
                                                job.clearProcess();
                                        }
                                        stop = true;
                                }
                                // cleanup of created files and folder
                                FileSystem.cleanup(directoriesToCheck, requestIdTemp);
                                if (stop) {
                                        return new ResponseEntity<>(requestResult, HttpStatus.INTERNAL_SERVER_ERROR);
                                }
                        } else {
                                // System.out.println("--> input file creation failed at "
                                // + pathToPyLSDInputFile);
                                requestResult.setErrorMessage("PyLSD input file creation failed at "
                                                + pathToPyLSDInputFile);
                                return new ResponseEntity<>(requestResult, HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                }
                System.out.println("\n\n ---> total count of unique parsed structures: "
                                + dataSetList.size());

                final ResponseEntity<ResultRecord> resultRecordResponseEntity = rankAndStore(dataSetList,
                                requestTransfer, storeResult);
                if (resultRecordResponseEntity.getStatusCode().isError()) {
                        requestResult.setErrorMessage(resultRecordResponseEntity.getBody() != null
                                        ? "Elucidation was successful but storing of results failed: "
                                                        + resultRecordResponseEntity.getBody().getId()
                                                        + " -> "
                                                        + resultRecordResponseEntity.getBody().getDataSetListSize()
                                        : "Elucidation was successful but storing of results failed and no details are available");
                        return new ResponseEntity<>(requestResult, HttpStatus.INTERNAL_SERVER_ERROR);
                }

                requestResult.setResultRecord(resultRecordResponseEntity.getBody());

                return new ResponseEntity<>(requestResult, HttpStatus.OK);
        }

        private static void stopProcessTree(final Job job, final Process process) {
                if (job != null) {
                        job.destroyProcess();
                        return;
                }

                if (process == null) {
                        return;
                }

                final ProcessHandle root = process.toHandle();
                root.descendants()
                                .sorted(Comparator.comparingLong(ProcessHandle::pid).reversed())
                                .forEach(handle -> {
                                        if (handle.isAlive()) {
                                                handle.destroyForcibly();
                                        }
                                });

                if (root.isAlive()) {
                        root.destroyForcibly();
                }

                try {
                        process.waitFor(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                }
        }

        private ResponseEntity<ResultRecord> rankAndStore(final List<DataSet> dataSetList,
                        final Transfer requestTransfer, final boolean storeResult) {
                final ResultRecord resultRecord = new ResultRecord();
                try {
                        // add MOL files, store results in DB if not empty and update resultRecord
                        if (!dataSetList.isEmpty()) {
                                Utilities.addMolFileToDataSets(dataSetList);

                                rank(dataSetList);

                                resultRecord.setDate(LocalDateTime.now().atZone(ZoneId.systemDefault())
                                                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                                .toString());
                                final List<DataSet> cutDataSetList = new ArrayList<>();
                                for (int i = 0; i < 100; i++) {
                                        if (i >= dataSetList.size()) {
                                                break;
                                        }
                                        cutDataSetList.add(dataSetList.get(i));
                                }
                                resultRecord.setDataSetList(cutDataSetList);
                                resultRecord.setDataSetListSize(cutDataSetList.size());
                                resultRecord.setPreviewDataSet(cutDataSetList.get(0));

                                resultRecord.setRequestId(requestTransfer.getRequestId());
                                resultRecord.setName(requestTransfer.getTaskName());
                                resultRecord.setCorrelations(requestTransfer.getCorrelations());
                                resultRecord.setQuerySpectrum(new SpectrumCompact(requestTransfer.getQuerySpectrum()));
                                resultRecord.setDetectionOptions(requestTransfer.getDetectionOptions());
                                resultRecord.setDetections(requestTransfer.getDetections());
                                resultRecord.setDetected(requestTransfer.getDetected());
                                resultRecord.setGrouping(requestTransfer.getGrouping());
                                resultRecord.setElucidationOptions(requestTransfer.getElucidationOptions());

                                if (storeResult) {
                                        try {
                                                final ObjectId resultStorageEntity = resultController
                                                                .insert(resultRecord)
                                                                .block();
                                                if (resultStorageEntity == null) {
                                                        System.out.println(
                                                                        "--> storing of result record failed -> resultStorageEntity is null");
                                                        return new ResponseEntity<>(resultRecord,
                                                                        HttpStatus.INTERNAL_SERVER_ERROR);
                                                }
                                                System.out.println(
                                                                "--> storing of result record was successful -> resultStorageEntity: "
                                                                                + resultStorageEntity.toString());
                                                resultRecord.setId(resultStorageEntity.toString());
                                        } catch (final Exception e) {
                                                e.printStackTrace();
                                                return new ResponseEntity<>(resultRecord, HttpStatus.NOT_FOUND);
                                        }
                                }
                        } else {
                                resultRecord.setDataSetList(new ArrayList<>());
                                resultRecord.setDataSetListSize(0);
                                resultRecord.setPreviewDataSet(null);
                                resultRecord.setId(null);
                        }
                } catch (final CDKException e) {
                        e.printStackTrace();
                }

                return new ResponseEntity<>(resultRecord, HttpStatus.OK);
        }

        private void rank(final List<DataSet> dataSetList) {
                FilterAndRank.rank(dataSetList);
        }

        public Transfer createPyLSDInputFiles(final Transfer requestTransfer) {
                System.out.println("-> detection was already done?: "
                                + (requestTransfer.getDetected() != null
                                                && requestTransfer.getDetected()));
                // System.out.println(requestTransfer.getDetections());

                final Transfer responseTransfer = new Transfer();
                responseTransfer.setRequestId(requestTransfer.getRequestId());
                responseTransfer.setTaskName(requestTransfer.getTaskName());
                responseTransfer.setCorrelations(requestTransfer.getCorrelations());
                responseTransfer.setQuerySpectrum(requestTransfer.getQuerySpectrum());
                responseTransfer.setDetected(requestTransfer.getDetected());
                responseTransfer.setDetectionOptions(requestTransfer.getDetectionOptions());
                responseTransfer.setDetections(requestTransfer.getDetections());
                responseTransfer.setGrouping(requestTransfer.getGrouping());
                responseTransfer.setElucidationOptions(requestTransfer.getElucidationOptions());
                responseTransfer.setMf(requestTransfer.getMf());

                if (responseTransfer.getDetected() == null
                                || !responseTransfer.getDetected()
                                || responseTransfer.getDetections() == null) {
                        final Transfer detectionTransfer = detection.detect(responseTransfer);
                        responseTransfer.setDetected(detectionTransfer.getDetected());
                        responseTransfer.setDetections(detectionTransfer.getDetections());
                        responseTransfer.setElucidationOptions(detectionTransfer.getElucidationOptions());
                        System.out.println(" -> new detections: "
                                        + responseTransfer.getDetections());
                }
                System.out.println("-> grouping was already given?: "
                                + (responseTransfer.getGrouping() != null));
                if (responseTransfer.getGrouping() == null) {
                        responseTransfer.setGrouping(detection.detectGroups(responseTransfer.getCorrelations()));
                        System.out.println(" -> new grouping: "
                                        + responseTransfer.getGrouping());
                }

                // add (custom) filters to elucidation options
                final String pathToFilterRing3 = "/data/lsd/PyLSD/LSD/Filters/ring3";
                final String pathToFilterRing4 = "/data/lsd/PyLSD/LSD/Filters/ring4";
                final Path pathToCustomFilters = Paths.get("/data/lsd/filters/");
                List<String> filterList = new ArrayList<>();
                try {
                        filterList = Files.walk(pathToCustomFilters)
                                        .filter(path -> !Files.isDirectory(path))
                                        .map(path -> path.toFile()
                                                        .getAbsolutePath())
                                        .collect(Collectors.toList());
                } catch (final IOException e) {
                        e.printStackTrace();
                }
                if (responseTransfer.getElucidationOptions()
                                .isUseFilterLsdRing3()) {
                        filterList.add(pathToFilterRing3);
                }
                if (responseTransfer.getElucidationOptions()
                                .isUseFilterLsdRing4()) {
                        filterList.add(pathToFilterRing4);
                }
                responseTransfer.getElucidationOptions()
                                .setFilterPaths(filterList.toArray(String[]::new));

                if (responseTransfer.getDetections() != null
                                && responseTransfer.getDetections()
                                                .getFragments() != null) {
                        // check for manual added custom fragment and build atom container
                        DataSet fragmentDataSet, newFragmentDataSet;
                        MDLV3000Reader mdlv3000Reader;
                        IAtomContainer fragment;
                        for (int i = 0; i < responseTransfer.getDetections()
                                        .getFragments()
                                        .size(); i++) {
                                fragmentDataSet = responseTransfer.getDetections()
                                                .getFragments()
                                                .get(i);
                                if (fragmentDataSet.getAttachment()
                                                .containsKey("custom")
                                                && (boolean) fragmentDataSet.getAttachment()
                                                                .get("custom")
                                                && fragmentDataSet.getStructure() == null) {
                                        try {
                                                mdlv3000Reader = new MDLV3000Reader(
                                                                new StringReader(fragmentDataSet.getMeta()
                                                                                .get("molfile")));
                                                fragment = mdlv3000Reader.read(new AtomContainer());
                                                newFragmentDataSet = Utils.atomContainerToDataSet(fragment, true);
                                                newFragmentDataSet.addAttachment("custom", true);
                                                newFragmentDataSet.addAttachment("include",
                                                                fragmentDataSet.getAttachment()
                                                                                .get("include"));
                                                newFragmentDataSet.addMetaInfo("molfile", fragmentDataSet.getMeta()
                                                                .get("molfile"));
                                                responseTransfer.getDetections()
                                                                .getFragments()
                                                                .set(i, newFragmentDataSet);
                                        } catch (final CDKException e) {
                                                e.printStackTrace();
                                        }
                                }
                        }
                }

                final Detections detectionsToUse = new Detections(new HashMap<>(), new HashMap<>(), new HashMap<>(),
                                new HashMap<>(), responseTransfer.getDetections() != null
                                                && responseTransfer.getDetections()
                                                                .getFixedNeighbors() != null
                                                                                ? responseTransfer.getDetections()
                                                                                                .getFixedNeighbors()
                                                                                : new HashMap<>(),
                                responseTransfer.getDetections() != null
                                                && responseTransfer.getDetections()
                                                                .getFragments() != null
                                                                                ? responseTransfer.getDetections()
                                                                                                .getFragments()
                                                                                : new ArrayList<>());
                // check for allowed detection usage
                if (responseTransfer.getDetectionOptions()
                                .isUseHybridizationDetections()) {
                        detectionsToUse.setDetectedHybridizations(responseTransfer.getDetections()
                                        .getDetectedHybridizations());
                } else {
                        for (final Correlation correlation : responseTransfer.getCorrelations()
                                        .getValues()) {
                                correlation.setHybridization(new ArrayList<>());
                        }
                }
                if (responseTransfer.getDetectionOptions()
                                .isUseNeighborDetections()) {
                        detectionsToUse.setDetectedConnectivities(responseTransfer.getDetections()
                                        .getDetectedConnectivities());
                        detectionsToUse.setForbiddenNeighbors(responseTransfer.getDetections()
                                        .getForbiddenNeighbors());
                        detectionsToUse.setSetNeighbors(responseTransfer.getDetections()
                                        .getSetNeighbors());
                }

                // define default bond distances
                final Map<String, Integer[]> defaultBondDistances = new HashMap<>();
                defaultBondDistances.put("hmbc", new Integer[] { 2, 3 });
                defaultBondDistances.put("cosy", new Integer[] { 3, 4 });

                responseTransfer.setPyLSDInputFileContentList(
                                PyLSDInputFileBuilder.buildPyLSDInputFileContentList(responseTransfer.getCorrelations(),
                                                responseTransfer.getMf(), detectionsToUse,
                                                responseTransfer.getElucidationOptions()
                                                                .isUseCombinatorics()
                                                                                ? responseTransfer.getGrouping()
                                                                                : new Grouping(new HashMap<>(),
                                                                                                new HashMap<>(),
                                                                                                new HashMap<>()),
                                                responseTransfer.getElucidationOptions(),
                                                defaultBondDistances));

                System.out.println("\n\n--> created PyLSD input file content list: "
                                + responseTransfer.getPyLSDInputFileContentList()
                                + "\n");

                return responseTransfer;
        }

        public static ResponseEntity<Transfer> cancel(final String requestId, final boolean reachedTimeLimit) {
                final Transfer responseTransfer = new Transfer();

                final List<String> errorMessageList = new ArrayList<>();
                if (reachedTimeLimit) {
                        errorMessageList.add("Time limit reached -> elucidation request was canceled!!!");
                }

                GlobalJobScheduler.get().cancelJob(requestId);
                if (GlobalJobScheduler.get().isCancelled(requestId)) {
                        errorMessageList.add("PyLSD run with request ID: " + requestId
                                        + " has been cancelled successfully.");
                } else {
                        errorMessageList.add("PyLSD run with request ID: " + requestId
                                        + " could not be cancelled or was already finished.");
                }

                // cleanup of created files and folder
                FileSystem.cleanup(directoriesToCheck, requestId);

                if (!errorMessageList.isEmpty()) {
                        responseTransfer.setErrorMessage(String.join("\n&\n", errorMessageList));
                }

                return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
        }

        public ResponseEntity<Transfer> detection(final Transfer requestTransfer) {
                return new ResponseEntity<>(
                                detection.detect(requestTransfer),
                                HttpStatus.OK);
        }
}
