package org.openscience.sherlock.core.utils.elucidation;

import casekit.io.FileSystem;
import casekit.nmr.elucidation.lsd.PyLSDInputFileBuilder;
import casekit.nmr.elucidation.model.Detections;
import casekit.nmr.elucidation.model.Grouping;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.nmrium.Correlation;
import casekit.nmr.utils.Utils;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.MDLV3000Reader;
import org.openscience.sherlock.core.model.exchange.Transfer;
import org.openscience.sherlock.core.utils.detection.Detection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PyLSD {
    private final static String pathToPyLSDExecutableFolder = "/data/lsd/PyLSD/Variant/";
    private final static String pathToPyLSDInputFileFolder = "/data/lsd/PyLSD/Variant/";
    private final static String pathToPyLSDResultFileFolder = "/data/lsd/PyLSD/Variant/";
    private final static String pathToNeighborsFilesFolder = "/data/lsd/PyLSD/Variant/";
    private final static String pathToFragmentsFilesFolder = "/data/lsd/PyLSD/Variant/";
    private final static String[] directoriesToCheck = new String[]{pathToPyLSDInputFileFolder,
                                                                    pathToPyLSDResultFileFolder,
                                                                    pathToNeighborsFilesFolder,
                                                                    pathToFragmentsFilesFolder};


    public static ResponseEntity<Transfer> runPyLSD(final Transfer requestTransfer,
                                                    final WebClient.Builder webClientBuilder,
                                                    final ExchangeStrategies exchangeStrategies) {
        // build PyLSD input file
        requestTransfer.getElucidationOptions()
                       .setPathToNeighborsFiles(pathToNeighborsFilesFolder
                                                        + requestTransfer.getRequestID()
                                                        + "_neighbor");
        requestTransfer.getElucidationOptions()
                       .setPathToFragmentFiles(pathToFragmentsFilesFolder
                                                       + requestTransfer.getRequestID()
                                                       + "_fragment");
        final Transfer queryResultTransfer = createPyLSDInputFiles(webClientBuilder, requestTransfer);
        final Transfer responseTransfer = new Transfer();
        responseTransfer.setRequestID(requestTransfer.getRequestID());
        responseTransfer.setElucidationOptions(requestTransfer.getElucidationOptions());
        responseTransfer.setCorrelations(queryResultTransfer.getCorrelations());
        responseTransfer.setDetections(queryResultTransfer.getDetections());
        responseTransfer.setDetected(queryResultTransfer.getDetected());
        responseTransfer.setGrouping(queryResultTransfer.getGrouping());
        responseTransfer.setDetectionOptions(queryResultTransfer.getDetectionOptions());
        responseTransfer.setPredictionOptions(queryResultTransfer.getPredictionOptions());


        System.out.println("\n ---> file content list size to process: "
                                   + queryResultTransfer.getPyLSDInputFileContentList()
                                                        .size()
                                   + "\n");
        String pyLSDInputFileContent, pathToPyLSDInputFile, requestID;
        ProcessBuilder processBuilder;
        Process process;
        final List<DataSet> dataSetList = new ArrayList<>();
        boolean pyLSDRunWasSuccessful;
        ResponseEntity<Transfer> transferResponseEntity;
        boolean stop = false;
        for (int i = 0; i
                < queryResultTransfer.getPyLSDInputFileContentList()
                                     .size(); i++) {
            pyLSDInputFileContent = queryResultTransfer.getPyLSDInputFileContentList()
                                                       .get(i);
            requestID = responseTransfer.getRequestID()
                    + "_"
                    + i;
            System.out.println("\n----------------------\n -> i: "
                                       + i
                                       + " -> \n"
                                       + pyLSDInputFileContent
                                       + "\n----------------------\n");

            pathToPyLSDInputFile = pathToPyLSDInputFileFolder
                    + requestID
                    + ".pylsd";

            // run PyLSD if file was written successfully
            if (FileSystem.writeFile(pathToPyLSDInputFile, pyLSDInputFileContent)) {
                //                System.out.println("--> has been written successfully: "
                //                                           + pathToPyLSDInputFile);
                try {
                    // try to execute PyLSD
                    processBuilder = new ProcessBuilder();
                    processBuilder.directory(new File(pathToPyLSDExecutableFolder))
                                  .redirectError(new File(pathToPyLSDInputFileFolder
                                                                  + requestID
                                                                  + "_error.txt"))
                                  .redirectOutput(new File(pathToPyLSDInputFileFolder
                                                                   + requestID
                                                                   + "_log.txt"))
                                  .command("python2.7", pathToPyLSDExecutableFolder
                                          + "lsd.py", pathToPyLSDInputFile);
                    process = processBuilder.start();
                    pyLSDRunWasSuccessful = process.waitFor(responseTransfer.getElucidationOptions()
                                                                            .getTimeLimitTotal(), TimeUnit.MINUTES);
                    if (pyLSDRunWasSuccessful) {
                        System.out.println("\n\n--> run was successful");
                        //                        System.out.println("\n\n--> parse and predict ("
                        //                                                   + (responseTransfer.getPredictionOptions()
                        //                                                                      .isPredictWithStereo()
                        //                                                      ? "stereo activated"
                        //                                                      : "stereo not activated")
                        //                                                   + ") ...");
                        System.out.println("\n\n--> parse and predict ...");
                        final String pathToSmilesFile = pathToPyLSDResultFileFolder
                                + requestID
                                + "_0.smiles";

                        transferResponseEntity = Prediction.parseAndPredictFromSmilesFile(
                                responseTransfer.getCorrelations(), responseTransfer.getPredictionOptions(),
                                responseTransfer.getDetections(),
                                //                                hoseCodeDBEntriesMap,
                                pathToSmilesFile, webClientBuilder, exchangeStrategies);
                        if (transferResponseEntity.getStatusCode()
                                                  .isError()) {
                            return transferResponseEntity;
                        }
                        System.out.println("\n\n--> parse and prediction was successful");
                        for (final DataSet dataSet : transferResponseEntity.getBody()
                                                                           .getDataSetList()) {
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
                                "--> reached time limit -> run was NOT successful -> killing PyLSD run if it is still running");
                        responseTransfer.setErrorMessage(cancel(true).getBody()
                                                                     .getErrorMessage());
                        stop = true;
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                    responseTransfer.setPyLSDRunWasSuccessful(false);
                    stop = true;
                }
                // cleanup of created files and folder
                FileSystem.cleanup(directoriesToCheck, requestID);
                if (stop) {
                    break;
                }
            } else {
                //                System.out.println("--> input file creation failed at "
                //                                           + pathToPyLSDInputFile);
                responseTransfer.setErrorMessage("PyLSD input file creation failed at "
                                                         + pathToPyLSDInputFile);
                return new ResponseEntity<>(responseTransfer, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        System.out.println("\n\n ---> total count of unique parsed and ranked structures: "
                                   + dataSetList.size());
        responseTransfer.setDataSetList(dataSetList);

        return new ResponseEntity<>(responseTransfer, stop
                                                      ? HttpStatus.INTERNAL_SERVER_ERROR
                                                      : HttpStatus.OK);
    }

    public static Transfer createPyLSDInputFiles(final WebClient.Builder webClientBuilder,
                                                 final Transfer requestTransfer) {
        System.out.println("-> detection was already done?: "
                                   + (requestTransfer.getDetected()
                != null
                && requestTransfer.getDetected()));
        //        System.out.println(requestTransfer.getDetections());

        if (requestTransfer.getDetected()
                == null
                || !requestTransfer.getDetected()
                || requestTransfer.getDetections()
                == null) {
            final Transfer detectionTransfer = Detection.detect(webClientBuilder, requestTransfer);
            requestTransfer.setDetected(detectionTransfer.getDetected());
            requestTransfer.setDetections(detectionTransfer.getDetections());
            requestTransfer.setElucidationOptions(detectionTransfer.getElucidationOptions());
            System.out.println(" -> new detections: "
                                       + requestTransfer.getDetections());
        }
        System.out.println("-> grouping was already given?: "
                                   + (requestTransfer.getGrouping()
                != null));
        System.out.println(requestTransfer.getGrouping());
        if (requestTransfer.getGrouping()
                == null) {
            requestTransfer.setGrouping(Detection.detectGroups(requestTransfer.getCorrelations()));
            System.out.println(" -> new grouping: "
                                       + requestTransfer.getGrouping());
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
        if (requestTransfer.getElucidationOptions()
                           .isUseFilterLsdRing3()) {
            filterList.add(pathToFilterRing3);
        }
        if (requestTransfer.getElucidationOptions()
                           .isUseFilterLsdRing4()) {
            filterList.add(pathToFilterRing4);
        }
        requestTransfer.getElucidationOptions()
                       .setFilterPaths(filterList.toArray(String[]::new));

        if (requestTransfer.getDetections()
                != null
                && requestTransfer.getDetections()
                                  .getFragments()
                != null) {
            // check for manual added custom fragment and build atom container
            DataSet fragmentDataSet, newFragmentDataSet;
            MDLV3000Reader mdlv3000Reader;
            IAtomContainer fragment;
            for (int i = 0; i
                    < requestTransfer.getDetections()
                                     .getFragments()
                                     .size(); i++) {
                fragmentDataSet = requestTransfer.getDetections()
                                                 .getFragments()
                                                 .get(i);
                if (fragmentDataSet.getAttachment()
                                   .containsKey("custom")
                        && (boolean) fragmentDataSet.getAttachment()
                                                    .get("custom")
                        && fragmentDataSet.getStructure()
                        == null) {
                    try {
                        mdlv3000Reader = new MDLV3000Reader(new StringReader(fragmentDataSet.getMeta()
                                                                                            .get("molfile")));
                        fragment = mdlv3000Reader.read(new AtomContainer());
                        newFragmentDataSet = Utils.atomContainerToDataSet(fragment, true);
                        newFragmentDataSet.addAttachment("custom", true);
                        newFragmentDataSet.addAttachment("include", fragmentDataSet.getAttachment()
                                                                                   .get("include"));
                        newFragmentDataSet.addMetaInfo("molfile", fragmentDataSet.getMeta()
                                                                                 .get("molfile"));
                        requestTransfer.getDetections()
                                       .getFragments()
                                       .set(i, newFragmentDataSet);
                    } catch (final CDKException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        final Detections detectionsToUse = new Detections(new HashMap<>(), new HashMap<>(), new HashMap<>(),
                                                          new HashMap<>(), requestTransfer.getDetections()
                                                                                   != null
                                                                                   && requestTransfer.getDetections()
                                                                                                     .getFixedNeighbors()
                != null
                                                                           ? requestTransfer.getDetections()
                                                                                            .getFixedNeighbors()
                                                                           : new HashMap<>(),
                                                          requestTransfer.getDetections()
                                                                  != null
                                                                  && requestTransfer.getDetections()
                                                                                    .getFragments()
                                                                  != null
                                                          ? requestTransfer.getDetections()
                                                                           .getFragments()
                                                          : new ArrayList<>());
        // check for allowed detection usage
        if (requestTransfer.getDetectionOptions()
                           .isUseHybridizationDetections()) {
            detectionsToUse.setDetectedHybridizations(requestTransfer.getDetections()
                                                                     .getDetectedHybridizations());
        } else {
            for (final Correlation correlation : requestTransfer.getCorrelations()
                                                                .getValues()) {
                correlation.setHybridization(new ArrayList<>());
            }
        }
        if (requestTransfer.getDetectionOptions()
                           .isUseNeighborDetections()) {
            detectionsToUse.setDetectedConnectivities(requestTransfer.getDetections()
                                                                     .getDetectedConnectivities());
            detectionsToUse.setForbiddenNeighbors(requestTransfer.getDetections()
                                                                 .getForbiddenNeighbors());
            detectionsToUse.setSetNeighbors(requestTransfer.getDetections()
                                                           .getSetNeighbors());
        }

        // define default bond distances
        final Map<String, Integer[]> defaultBondDistances = new HashMap<>();
        defaultBondDistances.put("hmbc", new Integer[]{2, 3});
        defaultBondDistances.put("cosy", new Integer[]{3, 4});

        requestTransfer.setPyLSDInputFileContentList(
                PyLSDInputFileBuilder.buildPyLSDInputFileContentList(requestTransfer.getCorrelations(),
                                                                     requestTransfer.getMf(), detectionsToUse,
                                                                     requestTransfer.getElucidationOptions()
                                                                                    .isUseCombinatorics()
                                                                     ? requestTransfer.getGrouping()
                                                                     : new Grouping(new HashMap<>(), new HashMap<>(),
                                                                                    new HashMap<>()),
                                                                     requestTransfer.getElucidationOptions(),
                                                                     defaultBondDistances));
        return requestTransfer;
    }

    private static boolean isStillRunning(final ProcessHandle processHandle) {
        return processHandle.isAlive()
                && (processHandle.info()
                                 .command()
                                 .orElse("unknown")
                                 .contains("python2.7")
                || processHandle.info()
                                .command()
                                .orElse("unknown")
                                .contains("LSD/lsd"));
    }

    public static ResponseEntity<Transfer> cancel(final boolean reachedTimeLimit) {
        final Transfer responseTransfer = new Transfer();

        FileSystem.cleanup(directoriesToCheck, ".lsd");

        final List<String> errorMessageList = new ArrayList<>();
        if (reachedTimeLimit) {
            errorMessageList.add("Time limit reached -> elucidation request was canceled!!!");
        }

        while (ProcessHandle.allProcesses()
                            .anyMatch(PyLSD::isStillRunning)) {
            ProcessHandle.allProcesses()
                         .filter(PyLSD::isStillRunning)
                         .findFirst()
                         .ifPresent(processHandleLSD -> {
                             System.out.println("-> killing PID "
                                                        + processHandleLSD.pid()
                                                        + ": "
                                                        + processHandleLSD.info()
                                                                          .command()
                                                                          .orElse("unknown"));
                             while (!processHandleLSD.destroyForcibly()) {
                                 try {
                                     TimeUnit.SECONDS.sleep(1);
                                 } catch (final InterruptedException e) {
                                     e.printStackTrace();
                                     errorMessageList.add(e.getMessage());
                                 }
                             }
                         });
        }

        if (!errorMessageList.isEmpty()) {
            responseTransfer.setErrorMessage(String.join("\n&\n", errorMessageList));
        }

        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
    }

    public static ResponseEntity<Transfer> detection(final Transfer requestTransfer,
                                                     final WebClient.Builder webClientBuilder) {
        return new ResponseEntity<>(Detection.detect(webClientBuilder, requestTransfer), HttpStatus.OK);
    }
}
