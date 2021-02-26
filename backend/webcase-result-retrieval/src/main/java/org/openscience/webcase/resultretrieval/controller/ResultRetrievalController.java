package org.openscience.webcase.resultretrieval.controller;

import org.openscience.webcase.resultretrieval.model.DataSet;
import org.openscience.webcase.resultretrieval.model.exchange.Transfer;
import org.openscience.webcase.resultretrieval.utils.FileSystem;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/")
public class ResultRetrievalController {

    @GetMapping(value = "retrieveResultFromFile")
    public ResponseEntity<Transfer> retrieveResultFromFile(@RequestParam final String pathToResultsFile){
        final Transfer resultTransfer = new Transfer();
        final List<DataSet> dataSetList = new ArrayList<>();
        final List<String> smilesList = this.getSmilesListFromFile(pathToResultsFile);

        DataSet dataSet;
        Map<String, String> meta;
        for (final String smiles : smilesList){
            meta = new HashMap<>();
            meta.put("smiles", smiles);
            dataSet = new DataSet();
            dataSet.setMeta(meta);

            dataSetList.add(dataSet);
        }

        resultTransfer.setDataSetList(dataSetList);
        return new ResponseEntity<>(resultTransfer, HttpStatus.OK);
    }

    private List<String> getSmilesListFromFile(final String pathToSmilesFile){
        final List<String> smilesList = new ArrayList<>();
        try {
            final BufferedReader bufferedReader = FileSystem.readFile(pathToSmilesFile);
            if (bufferedReader != null) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    smilesList.add(line);
                }
                bufferedReader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return smilesList;
    }
}
