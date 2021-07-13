package org.openscience.webcase.dbservice.hosecode.controller;

import casekit.nmr.model.DataSet;
import casekit.nmr.utils.SDFParser;
import org.openscience.cdk.exception.CDKException;
import org.openscience.webcase.dbservice.hosecode.model.exchange.Transfer;
import org.openscience.webcase.dbservice.hosecode.utils.PyLSDResultsRanker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(value = "/fileParser")
public class FileParserController {

    @Autowired
    private PyLSDResultsRanker pyLSDResultsRanker;

    @PostMapping(value = "/parseAndRankResultSDFile")
    public ResponseEntity<Transfer> parseResultSDF(@RequestBody final Transfer requestTransfer) {
        final Transfer resultTransfer = new Transfer();
        List<DataSet> dataSetList = new ArrayList<>();
        try {
            requestTransfer.setDataSetList(SDFParser.parseSDFileContent(requestTransfer.getFileContent()));

            dataSetList = this.pyLSDResultsRanker.rankPyLSDResults(requestTransfer);
        } catch (final CDKException e) {
            e.printStackTrace();
        }

        resultTransfer.setDataSetList(dataSetList);
        return new ResponseEntity<>(resultTransfer, HttpStatus.OK);
    }
}
