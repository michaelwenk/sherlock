package org.openscience.webcase.casekit.controller;

import casekit.nmr.model.Assignment;
import casekit.nmr.model.DataSet;
import casekit.nmr.utils.Match;
import org.openscience.webcase.casekit.model.exchange.Transfer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/ranking")
public class RankingController {

    @PostMapping(value = "rankBySpectralSimilarity", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Transfer> rankBySpectralSimilarity(@RequestBody final Transfer requestTransfer) {
        final Transfer resultTransfer = new Transfer();
        List<DataSet> dataSetList = requestTransfer.getDataSetList();

        final double shiftTolerance = requestTransfer.getDereplicationOptions()
                                                     .getShiftTolerances()
                                                     .get("C");
        final boolean checkMultiplicity = requestTransfer.getDereplicationOptions()
                                                         .isCheckMultiplicity();
        final boolean checkEquivalencesCount = requestTransfer.getDereplicationOptions()
                                                              .isCheckEquivalencesCount();

        if (requestTransfer.getQuerySpectrum()
                           .getNDim()
                == 1
                && requestTransfer.getQuerySpectrum()
                                  .getNuclei()[0].equals("13C")) {
            // @TODO get shift tolerance as arguments
            dataSetList = dataSetList.stream()
                                     .filter(dataSet -> {
                                         final Assignment matchAssignment = Match.matchSpectra(dataSet.getSpectrum(),
                                                                                               requestTransfer.getQuerySpectrum(),
                                                                                               0, 0, shiftTolerance,
                                                                                               checkMultiplicity,
                                                                                               checkEquivalencesCount);
                                         return checkEquivalencesCount
                                                ? matchAssignment.getSetAssignmentsCountWithEquivalences(0)
                                                        == requestTransfer.getQuerySpectrum()
                                                                          .getSignalCountWithEquivalences()
                                                : matchAssignment.getSetAssignmentsCount(0)
                                                        == requestTransfer.getQuerySpectrum()
                                                                          .getSignalCount();
                                     })
                                     .collect(Collectors.toList());

            dataSetList.forEach(dataSet -> {
                final Double rmsd = Match.calculateRMSD(dataSet.getSpectrum(), requestTransfer.getQuerySpectrum(), 0, 0,
                                                        shiftTolerance, checkMultiplicity, checkEquivalencesCount);
                final Float tanimoto = Match.calculateTanimotoCoefficient(dataSet.getSpectrum(),
                                                                          requestTransfer.getQuerySpectrum(), 0, 0);
                dataSet.addMetaInfo("rmsd", String.valueOf(rmsd));
                dataSet.addMetaInfo("tanimoto", String.valueOf(tanimoto));
            });
            dataSetList.sort((dataSet1, dataSet2) -> {
                if (Double.parseDouble(dataSet1.getMeta()
                                               .get("rmsd"))
                        < Double.parseDouble(dataSet2.getMeta()
                                                     .get("rmsd"))) {
                    return -1;
                } else if (Double.parseDouble(dataSet1.getMeta()
                                                      .get("rmsd"))
                        > Double.parseDouble(dataSet2.getMeta()
                                                     .get("rmsd"))) {
                    return 1;
                }
                return 0;
            });
        }

        resultTransfer.setDataSetList(dataSetList);
        return new ResponseEntity<>(resultTransfer, HttpStatus.OK);
    }

}
