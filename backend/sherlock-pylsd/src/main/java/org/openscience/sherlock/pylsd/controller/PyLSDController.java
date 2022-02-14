package org.openscience.sherlock.pylsd.controller;

import casekit.io.FileSystem;
import casekit.nmr.model.DataSet;
import org.openscience.sherlock.pylsd.model.exchange.Transfer;
import org.openscience.sherlock.pylsd.utils.InputFileBuilder;
import org.openscience.sherlock.pylsd.utils.ParserAndPrediction;
import org.openscience.sherlock.pylsd.utils.detection.Detection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(value = "/")
public class PyLSDController {
    final String pathToPyLSDExecutableFolder = "/data/lsd/PyLSD/Variant/";
    final String pathToPyLSDInputFileFolder = "/data/lsd/PyLSD/Variant/";
    final String pathToPyLSDResultFileFolder = "/data/lsd/PyLSD/Variant/";
    final String pathToNeighborsFilesFolder = "/data/lsd/PyLSD/Variant/";

    private final WebClient.Builder webClientBuilder;
    private final ExchangeStrategies exchangeStrategies;
    private final ParserAndPrediction parserAndPrediction;

    @Autowired
    public PyLSDController(final WebClient.Builder webClientBuilder, final ExchangeStrategies exchangeStrategies) {
        this.webClientBuilder = webClientBuilder;
        this.exchangeStrategies = exchangeStrategies;
        this.parserAndPrediction = new ParserAndPrediction(this.webClientBuilder, this.exchangeStrategies);
    }


    @PostMapping(value = "/runPyLSD")
    public ResponseEntity<Transfer> runPyLSD(@RequestBody final Transfer requestTransfer) {
        // build PyLSD input file
        requestTransfer.getElucidationOptions()
                       .setPathsToNeighborsFiles(new String[]{this.pathToNeighborsFilesFolder
                                                                      + requestTransfer.getRequestID()
                                                                      + "_forbidden.deff",
                                                              this.pathToNeighborsFilesFolder
                                                                      + requestTransfer.getRequestID()
                                                                      + "_set.deff"});
        final Transfer queryResultTransfer = InputFileBuilder.createPyLSDInputFile(this.webClientBuilder,
                                                                                   requestTransfer);
        final Transfer responseTransfer = new Transfer();
        responseTransfer.setRequestID(requestTransfer.getRequestID());
        responseTransfer.setElucidationOptions(requestTransfer.getElucidationOptions());
        responseTransfer.setCorrelations(queryResultTransfer.getCorrelations());
        responseTransfer.setDetections(queryResultTransfer.getDetections());
        responseTransfer.setGrouping(queryResultTransfer.getGrouping());
        responseTransfer.setDetectionOptions(queryResultTransfer.getDetectionOptions());

        final String[] directoriesToCheck = new String[]{this.pathToPyLSDInputFileFolder,
                                                         this.pathToPyLSDResultFileFolder,
                                                         this.pathToNeighborsFilesFolder};

        System.out.println("\n ---> file content list processing: "
                                   + queryResultTransfer.getPyLSDInputFileContentList()
                                                        .size()
                                   + "\n");
        String pyLSDInputFileContent, pathToPyLSDInputFile, requestID;
        ProcessBuilder processBuilder;
        Process process;
        final List<DataSet> dataSetList = new ArrayList<>();
        boolean pyLSDRunWasSuccessful;
        ResponseEntity<Transfer> transferResponseEntity;
        for (int i = 0; i
                < queryResultTransfer.getPyLSDInputFileContentList()
                                     .size(); i++) {
            pyLSDInputFileContent = queryResultTransfer.getPyLSDInputFileContentList()
                                                       .get(i);
            requestID = responseTransfer.getRequestID()
                    + "_"
                    + i;
            //            System.out.println("\n----------------------\n -> i: "
            //                                       + i
            //                                       + " -> \n"
            //                                       + pyLSDInputFileContent
            //                                       + "\n----------------------\n");

            pathToPyLSDInputFile = this.pathToPyLSDInputFileFolder
                    + requestID
                    + ".pylsd";

            // run PyLSD if file was written successfully
            if (FileSystem.writeFile(pathToPyLSDInputFile, pyLSDInputFileContent)) {
                //                System.out.println("--> has been written successfully: "
                //                                           + pathToPyLSDInputFile);
                try {
                    // try to execute PyLSD
                    processBuilder = new ProcessBuilder();
                    processBuilder.directory(new File(this.pathToPyLSDExecutableFolder))
                                  .redirectError(new File(this.pathToPyLSDInputFileFolder
                                                                  + requestID
                                                                  + "_error.txt"))
                                  .redirectOutput(new File(this.pathToPyLSDInputFileFolder
                                                                   + requestID
                                                                   + "_log.txt"))
                                  .command("python2.7", this.pathToPyLSDExecutableFolder
                                          + "lsd.py", pathToPyLSDInputFile);
                    process = processBuilder.start();
                    pyLSDRunWasSuccessful = process.waitFor(responseTransfer.getElucidationOptions()
                                                                            .getTimeLimitTotal(), TimeUnit.MINUTES);
                    if (pyLSDRunWasSuccessful) {
                        //                        System.out.println("--> run was successful");
                        final String pathToSmilesFile = this.pathToPyLSDResultFileFolder
                                + requestID
                                + "_0.smiles";
                        responseTransfer.setPathToSmilesFile(pathToSmilesFile);

                        transferResponseEntity = this.parserAndPrediction.parseAndPredictFromSmilesFile(
                                responseTransfer);
                        if (transferResponseEntity.getStatusCode()
                                                  .isError()) {
                            return transferResponseEntity;
                        }
                        //                        System.out.println("--> path to smiles file: "
                        //                                                   + pathToSmilesFile
                        //                                                   + "\n-->  number of parsed and ranked structures: "
                        //                                                   + transferResponseEntity.getBody()
                        //                                                                           .getDataSetList()
                        //                                                                           .size());
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
                        //                        System.out.println("--> run was NOT successful -> killing PyLSD run if it still exist");
                        this.cancel();
                        break;
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                    responseTransfer.setPyLSDRunWasSuccessful(false);
                    break;
                }
                // cleanup of created files and folder
                FileSystem.cleanup(directoriesToCheck, requestID);
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

        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
    }

    @PostMapping(value = "/detect")
    public ResponseEntity<Transfer> detection(@RequestBody final Transfer requestTransfer) {
        return new ResponseEntity<>(Detection.detect(this.webClientBuilder, requestTransfer), HttpStatus.OK);
    }

    @GetMapping(value = "/cancel")
    public ResponseEntity<Transfer> cancel() {
        final Transfer responseTransfer = new Transfer();

        while (ProcessHandle.allProcesses()
                            .anyMatch(processHandle -> processHandle.isAlive()
                                    && processHandle.info()
                                                    .command()
                                                    .orElse("unknown")
                                                    .contains("LSD/lsd"))) {
            ProcessHandle.allProcesses()
                         .filter(processHandle -> processHandle.isAlive()
                                 && processHandle.info()
                                                 .command()
                                                 .orElse("unknown")
                                                 .contains("LSD/lsd"))
                         .findFirst()
                         .ifPresent(processHandleLSD -> {
                             System.out.println("-> killing PID "
                                                        + processHandleLSD.pid()
                                                        + ": "
                                                        + processHandleLSD.info()
                                                                          .command()
                                                                          .orElse("unknown"));
                             processHandleLSD.destroy();

                             while (processHandleLSD.isAlive()) {
                                 try {
                                     TimeUnit.SECONDS.sleep(1);
                                 } catch (final InterruptedException e) {
                                     e.printStackTrace();
                                     responseTransfer.setErrorMessage(e.getMessage());
                                 }
                             }
                         });
            if (responseTransfer.getErrorMessage()
                    != null) {
                return new ResponseEntity<>(responseTransfer, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
    }
}
