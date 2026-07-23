/*
 * MIT License
 *
 * Copyright (c) 2021 Michael Wenk (https://github.com/michaelwenk)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.openscience.sherlock.controller;

import casekit.nmr.analysis.MultiplicitySectionsBuilder;
import casekit.nmr.filterandrank.FilterAndRank;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;

import org.openscience.sherlock.configuration.OpenApiConfiguration;
import org.openscience.sherlock.model.exchange.RequestData;
import org.openscience.sherlock.model.exchange.RequestResult;
import org.openscience.sherlock.utils.Utilities;
import org.openscience.sherlock.dbservice.dataset.controller.DataSetController;
import org.openscience.sherlock.dbservice.dataset.db.model.DataSetRecord;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.*;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Dereplication", description = "Endpoints for ranking candidate datasets against query spectra.")
@SecurityRequirement(name = OpenApiConfiguration.BASIC_AUTH_SCHEME)
@RestController
@RequestMapping(value = "/dereplication")
public class DereplicationController {

        private final DataSetController dataSetController;

        public DereplicationController(final DataSetController dataSetController) {
                this.dataSetController = dataSetController;
        }

        private final MultiplicitySectionsBuilder multiplicitySectionsBuilder = new MultiplicitySectionsBuilder();

        @Operation(summary = "Run dereplication", description = "Matches the submitted query spectrum against candidate datasets and returns ranked dereplication results.")
        @PostMapping(value = "/dereplicate", consumes = "application/json", produces = "application/json")
        public ResponseEntity<RequestResult> dereplicate(@RequestBody final RequestData requestData) {

                final RequestResult requestResult = Utilities.prepareDefaultRequestResult(requestData);
                if (requestResult.getErrorMessage() != null) {
                        return new ResponseEntity<>(requestResult, HttpStatus.BAD_REQUEST);
                }

                final String mf = Utilities
                                .getMolecularFormulaFromCorrelations(requestData.getCorrelations());
                final Spectrum querySpectrum = requestResult.getResultRecord().getQuerySpectrum().toSpectrum();

                // accept a 1D query spectrum only
                if (querySpectrum.getNuclei().length == 1) {
                        try {
                                final List<DataSetRecord> dataSetRecordList = Utilities.getDataSetRecordFlux(
                                                dataSetController,
                                                querySpectrum,
                                                requestResult.getDereplicationOptions()
                                                                .isUseMF()
                                                                                ? mf
                                                                                : null)
                                                .collectList()
                                                .block();
                                if (dataSetRecordList != null) {
                                        List<DataSet> dataSetList = dataSetRecordList.stream()
                                                        .map(DataSetRecord::getDataSet)
                                                        .collect(Collectors.toList());
                                        final Map<String, int[]> multiplicitySectionsSettings = dataSetController
                                                        .getMultiplicitySectionsSettings();
                                        this.multiplicitySectionsBuilder.setMinLimit(
                                                        multiplicitySectionsSettings
                                                                        .get(querySpectrum.getNuclei()[0])[0]);
                                        this.multiplicitySectionsBuilder.setMaxLimit(
                                                        multiplicitySectionsSettings
                                                                        .get(querySpectrum.getNuclei()[0])[1]);
                                        this.multiplicitySectionsBuilder.setStepSize(
                                                        multiplicitySectionsSettings
                                                                        .get(querySpectrum.getNuclei()[0])[2]);

                                        dataSetList = FilterAndRank.filterAndRank(dataSetList, querySpectrum,
                                                        requestResult.getDereplicationOptions()
                                                                        .getShiftTolerance(),
                                                        requestResult.getDereplicationOptions()
                                                                        .getMaximumAverageDeviation(),
                                                        requestResult.getDereplicationOptions()
                                                                        .isCheckMultiplicity(),
                                                        requestResult.getDereplicationOptions()
                                                                        .isCheckEquivalencesCount(),
                                                        // equivalences are not checked then also allow lower
                                                        // equivalence count
                                                        !requestResult.getDereplicationOptions()
                                                                        .isCheckEquivalencesCount(),
                                                        this.multiplicitySectionsBuilder, false);
                                        // unique the dereplication result
                                        final List<DataSet> uniqueDataSetList = new ArrayList<>();
                                        final Set<String> uniqueDataSetBySourceAndID = new HashSet<>();
                                        String id, source, sourceAndIDKey;
                                        for (final DataSet dataSet : dataSetList) {
                                                source = dataSet.getMeta()
                                                                .get("source");
                                                id = dataSet.getMeta()
                                                                .get("id");
                                                sourceAndIDKey = source
                                                                + "_"
                                                                + id;
                                                if (!uniqueDataSetBySourceAndID.contains(sourceAndIDKey)) {
                                                        uniqueDataSetBySourceAndID.add(sourceAndIDKey);
                                                        uniqueDataSetList.add(dataSet);
                                                }
                                        }
                                        Utilities.addMolFileToDataSets(uniqueDataSetList);

                                        requestResult.getResultRecord().setDataSetList(uniqueDataSetList);
                                        requestResult.getResultRecord().setDataSetListSize(uniqueDataSetList.size());
                                }
                        } catch (final Exception e) {
                                requestResult.setErrorMessage(e.getMessage());
                                return new ResponseEntity<>(requestResult, HttpStatus.NOT_FOUND);
                        }

                        return new ResponseEntity<>(requestResult, HttpStatus.OK);
                }

                requestResult.getResultRecord().setDataSetList(new ArrayList<>());
                requestResult.getResultRecord().setDataSetListSize(0);
                return new ResponseEntity<>(requestResult, HttpStatus.OK);
        }
}
