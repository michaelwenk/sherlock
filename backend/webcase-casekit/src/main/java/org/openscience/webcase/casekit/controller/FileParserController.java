package org.openscience.webcase.casekit.controller;

import casekit.nmr.lsd.RankedResultSDFParser;
import casekit.nmr.model.DataSet;
import org.openscience.cdk.exception.CDKException;
import org.openscience.webcase.casekit.model.exchange.Transfer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(value = "/fileParser")
public class FileParserController {

    @GetMapping(value = "/parseRankedSdf")
    public ResponseEntity<Transfer> parseRankedSDF(@RequestParam final String pathToRankedSDFile) {
        final Transfer resultTransfer = new Transfer();
        List<DataSet> dataSetList = new ArrayList<>();

        try {
            dataSetList = RankedResultSDFParser.parseRankedResultSDF(pathToRankedSDFile, "13C");
        } catch (final CDKException | FileNotFoundException e) {
            e.printStackTrace();
        }

        resultTransfer.setDataSetList(dataSetList);
        return new ResponseEntity<>(resultTransfer, HttpStatus.OK);
    }
}
