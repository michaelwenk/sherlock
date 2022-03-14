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

package org.openscience.sherlock.core.controller;

import casekit.nmr.analysis.MultiplicitySectionsBuilder;
import casekit.nmr.filterandrank.FilterAndRank;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import casekit.nmr.utils.Utils;
import org.openscience.sherlock.core.model.db.DataSetRecord;
import org.openscience.sherlock.core.model.exchange.Transfer;
import org.openscience.sherlock.core.utils.Utilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/dereplication")
public class DereplicationController {

    private final WebClient.Builder webClientBuilder;
    private final ExchangeStrategies exchangeStrategies;
    private final MultiplicitySectionsBuilder multiplicitySectionsBuilder = new MultiplicitySectionsBuilder();

    @Autowired
    public DereplicationController(final WebClient.Builder webClientBuilder,
                                   final ExchangeStrategies exchangeStrategies) {
        this.webClientBuilder = webClientBuilder;
        this.exchangeStrategies = exchangeStrategies;
    }

    @PostMapping(value = "/dereplicate", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Transfer> dereplicate(@RequestBody final Transfer requestTransfer) {
        final Transfer responseTransfer = new Transfer();

        final Spectrum querySpectrum = requestTransfer.getQuerySpectrum();

        // accept a 1D query spectrum only
        if (querySpectrum.getNuclei().length
                == 1) {
            try {
                final List<DataSetRecord> dataSetRecordList = this.getDataSetRecordFlux(querySpectrum,
                                                                                        requestTransfer.getDereplicationOptions()
                                                                                                       .isUseMF()
                                                                                        ? requestTransfer.getMf()
                                                                                        : null)
                                                                  .collectList()
                                                                  .block();
                if (dataSetRecordList
                        != null) {
                    List<DataSet> dataSetList = dataSetRecordList.stream()
                                                                 .map(DataSetRecord::getDataSet)
                                                                 .collect(Collectors.toList());
                    final Mono<Map<String, int[]>> multiplicitySectionsSettingsMono = Utilities.getMultiplicitySectionsSettings(
                            this.webClientBuilder, this.exchangeStrategies);
                    final Map<String, int[]> multiplicitySectionsSettings = multiplicitySectionsSettingsMono.block();
                    this.multiplicitySectionsBuilder.setMinLimit(
                            multiplicitySectionsSettings.get(querySpectrum.getNuclei()[0])[0]);
                    this.multiplicitySectionsBuilder.setMaxLimit(
                            multiplicitySectionsSettings.get(querySpectrum.getNuclei()[0])[1]);
                    this.multiplicitySectionsBuilder.setStepSize(
                            multiplicitySectionsSettings.get(querySpectrum.getNuclei()[0])[2]);

                    dataSetList = FilterAndRank.filterAndRank(dataSetList, querySpectrum,
                                                              requestTransfer.getDereplicationOptions()
                                                                             .getShiftTolerance(),
                                                              requestTransfer.getDereplicationOptions()
                                                                             .getMaximumAverageDeviation(),
                                                              requestTransfer.getDereplicationOptions()
                                                                             .isCheckMultiplicity(),
                                                              requestTransfer.getDereplicationOptions()
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

                    responseTransfer.setDataSetList(uniqueDataSetList);
                }
            } catch (final Exception e) {
                responseTransfer.setErrorMessage(e.getMessage());
                return new ResponseEntity<>(responseTransfer, HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
        }

        responseTransfer.setDataSetList(new ArrayList<>());
        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
    }

    public Flux<DataSetRecord> getDataSetRecordFlux(final Spectrum querySpectrum, final String mf) {
        final WebClient webClient = this.webClientBuilder.baseUrl(
                                                "http://sherlock-gateway:8080/sherlock-db-service-dataset/")
                                                         .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                        MediaType.APPLICATION_JSON_VALUE)
                                                         .exchangeStrategies(this.exchangeStrategies)
                                                         .build();
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();

        // @TODO take the nuclei order into account when matching -> now it's just an exact array match
        final String nucleiString = Arrays.stream(querySpectrum.getNuclei())
                                          .reduce("", (concat, current) -> concat
                                                  + current);
        if (mf
                != null) {
            uriComponentsBuilder.path("/getByNucleiAndSignalCountAndMf")
                                .queryParam("nuclei", nucleiString)
                                .queryParam("signalCount", querySpectrum.getSignalCount())
                                .queryParam("mf", Utils.getAlphabeticMF(mf));
        } else {
            uriComponentsBuilder.path("/getByNucleiAndSignalCount")
                                .queryParam("nuclei", nucleiString)
                                .queryParam("signalCount", querySpectrum.getSignalCount());
        }

        return webClient.get()
                        .uri(uriComponentsBuilder.toUriString())
                        .retrieve()
                        .bodyToFlux(DataSetRecord.class);
    }
}
