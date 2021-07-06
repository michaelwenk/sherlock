package org.openscience.webcase.casekit.controller;

import casekit.nmr.lsd.RankedResultSDFParser;
import casekit.nmr.model.DataSet;
import org.openscience.cdk.exception.CDKException;
import org.openscience.webcase.casekit.model.exchange.Transfer;
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

    @PostMapping(value = "/parseRankedSdf")
    public ResponseEntity<Transfer> parseRankedSDF(@RequestBody final Transfer requestTransfer) {
        final Transfer resultTransfer = new Transfer();
        List<DataSet> dataSetList = new ArrayList<>();

        try {
            dataSetList = RankedResultSDFParser.parseRankedResultSDFileContent(requestTransfer.getFileContent(),
                                                                               requestTransfer.getNucleus(),
                                                                               requestTransfer.getMaxAverageDeviation());
        } catch (final CDKException e) {
            e.printStackTrace();
        }

        resultTransfer.setDataSetList(dataSetList);
        return new ResponseEntity<>(resultTransfer, HttpStatus.OK);
    }
}
