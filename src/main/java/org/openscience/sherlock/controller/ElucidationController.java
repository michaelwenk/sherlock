package org.openscience.sherlock.controller;

import org.openscience.sherlock.model.exchange.Transfer;
import org.openscience.sherlock.utils.elucidation.PyLSD;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/elucidation")
public class ElucidationController {

    // private final Map<String, Map<String, Double[]>> hoseCodeDBEntriesMap;

    // @Autowired
    // public ElucidationController(
    // // final Map<String, Map<String, Double[]>> hoseCodeDBEntriesMap
    // ) {
    // // this.hoseCodeDBEntriesMap = hoseCodeDBEntriesMap;
    // }

    @Autowired
    private PyLSD pyLSD;

    @PostMapping(value = "/elucidate")
    public ResponseEntity<Transfer> elucidate(@RequestBody final Transfer requestTransfer) {
        final Transfer responseTransfer = new Transfer();

        // run PyLSD
        try {
            final ResponseEntity<Transfer> responseEntity = pyLSD.runPyLSD(
                    requestTransfer
            // this.hoseCodeDBEntriesMap,
            );
            if (responseEntity.getStatusCode()
                    .isError()) {
                responseTransfer.setErrorMessage(responseEntity.getBody() != null
                        ? responseEntity.getBody()
                                .getErrorMessage()
                        : "Something went wrong when trying to run PyLSD!!!");
                return new ResponseEntity<>(responseTransfer, HttpStatus.NOT_FOUND);
            }
            final Transfer queryResultTransfer = responseEntity.getBody();
            responseTransfer.setPyLSDRunWasSuccessful(queryResultTransfer.getPyLSDRunWasSuccessful());
            responseTransfer.setDataSetList(queryResultTransfer.getDataSetList());
            responseTransfer.setDetected(queryResultTransfer.getDetected());
            responseTransfer.setDetections(queryResultTransfer.getDetections());
            responseTransfer.setDetectionOptions(queryResultTransfer.getDetectionOptions());
            responseTransfer.setGrouping(queryResultTransfer.getGrouping());
        } catch (final Exception e) {
            responseTransfer.setErrorMessage(e.getMessage());
            return new ResponseEntity<>(responseTransfer, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
    }

    // @GetMapping(value = "/predictBySmiles")
    // public DataSet predict(@RequestParam final String smiles, final String
    // nucleus, final Integer maxSphere) {
    // try {
    // final SmilesParser smilesParser = new
    // SmilesParser(SilentChemObjectBuilder.getInstance());
    // final IAtomContainer structure = smilesParser.parseSmiles(smiles);
    //
    // return Prediction.predict(structure, nucleus
    // != null
    // ? nucleus
    // : "13C", maxSphere
    // != null
    // ? maxSphere
    // : 6, this.hoseCodeDBEntriesMap);
    // } catch (final InvalidSmilesException e) {
    // e.printStackTrace();
    // }
    //
    // return null;
    // }
}
