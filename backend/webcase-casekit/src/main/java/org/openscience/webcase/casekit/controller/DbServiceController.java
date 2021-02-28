package org.openscience.webcase.casekit.controller;

import casekit.nmr.dbservice.NMRShiftDB;
import casekit.nmr.model.DataSet;
import org.openscience.cdk.exception.CDKException;
import org.openscience.webcase.casekit.model.exchange.Transfer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping(value = "/dbservice")
public class DbServiceController {

    @GetMapping(value = "getDataSetsFromNMRShiftDB", produces = "application/json")
    public ResponseEntity<Transfer> getDataSetsFromNMRShiftDB(@RequestParam  final String pathToNMRShiftDB,
                                                             @RequestParam final String[] nuclei){
        final Transfer resultTransfer = new Transfer();
        List<DataSet> dataSetList = new ArrayList<>();
        try {
            dataSetList = NMRShiftDB.getDataSetsFromNMRShiftDB(pathToNMRShiftDB, nuclei);
        } catch (final IOException | CDKException e){
            e.printStackTrace();
        }
        resultTransfer.setDataSetList(dataSetList);

        return new ResponseEntity<>(resultTransfer, HttpStatus.OK);
    }
}
