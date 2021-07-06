package org.openscience.webcase.casekit.controller;

import casekit.nmr.model.Assignment;
import casekit.nmr.model.DataSet;
import casekit.nmr.similarity.Similarity;
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
        final double maxAverageDeviation = requestTransfer.getDereplicationOptions()
                                                          .getMaxAverageDeviation();

        if (requestTransfer.getQuerySpectrum()
                           .getNDim()
                == 1
                && requestTransfer.getQuerySpectrum()
                                  .getNuclei()[0].equals("13C")) {
            // @TODO get shift tolerance as arguments
            dataSetList = dataSetList.stream()
                                     .filter(dataSet -> {
                                         final Assignment matchAssignment = Similarity.matchSpectra(
                                                 dataSet.getSpectrum(), requestTransfer.getQuerySpectrum(), 0, 0,
                                                 shiftTolerance, checkMultiplicity, checkEquivalencesCount, false);

                                         if (checkEquivalencesCount
                                             ? matchAssignment.getSetAssignmentsCountWithEquivalences(0)
                                                     == requestTransfer.getQuerySpectrum()
                                                                       .getSignalCountWithEquivalences()
                                             : matchAssignment.getSetAssignmentsCount(0)
                                                     == requestTransfer.getQuerySpectrum()
                                                                       .getSignalCount()) {

                                             final Double averageDeviation = Similarity.calculateAverageDeviation(
                                                     dataSet.getSpectrum(), requestTransfer.getQuerySpectrum(), 0, 0,
                                                     matchAssignment);
                                             if (averageDeviation
                                                     != null
                                                     && averageDeviation
                                                     <= maxAverageDeviation) {
                                                 dataSet.addMetaInfo("averageDeviation",
                                                                     String.valueOf(averageDeviation));
                                                 final Double rmsd = Similarity.calculateRMSD(dataSet.getSpectrum(),
                                                                                              requestTransfer.getQuerySpectrum(),
                                                                                              0, 0, matchAssignment);
                                                 dataSet.addMetaInfo("rmsd", String.valueOf(rmsd));

                                                 return true;
                                             }
                                             return false;
                                         }

                                         return false;
                                     })
                                     .collect(Collectors.toList());

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
