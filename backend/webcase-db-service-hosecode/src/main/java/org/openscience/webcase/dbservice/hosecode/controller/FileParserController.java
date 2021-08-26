package org.openscience.webcase.dbservice.hosecode.controller;

import casekit.nmr.model.DataSet;
import casekit.nmr.utils.Parser;
import org.openscience.webcase.dbservice.hosecode.model.exchange.Transfer;
import org.openscience.webcase.dbservice.hosecode.utils.ResultsParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/fileParser")
public class FileParserController {

    // set ExchangeSettings
    final int maxInMemorySizeMB = 1000;
    final ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                                                                    .codecs(configurer -> configurer.defaultCodecs()
                                                                                                    .maxInMemorySize(
                                                                                                            this.maxInMemorySizeMB
                                                                                                                    * 1024
                                                                                                                    * 1024))
                                                                    .build();

    private final WebClient.Builder webClientBuilder;
    private final ResultsParser resultsParser;

    @Autowired
    public FileParserController(final WebClient.Builder webClientBuilder, final ResultsParser resultsParser) {
        this.webClientBuilder = webClientBuilder;
        this.resultsParser = resultsParser;
    }

    @PostMapping(value = "/clearHOSECodeDBEntriesMap")
    public void clearHOSECodeDBEntriesMap() {
        this.resultsParser.clearHOSECodeDBEntriesMap();
    }

    @PostMapping(value = "/fillHOSECodeDBEntriesMap")
    public void fillHOSECodeDBEntriesMap() {
        this.resultsParser.fillHOSECodeDBEntriesMap();
    }

    @PostMapping(value = "/parseResultFile")
    public ResponseEntity<Transfer> parseResultFile(@RequestBody final Transfer requestTransfer) {
        final Transfer resultTransfer = new Transfer();
        List<DataSet> dataSetList = new ArrayList<>();
        //        requestTransfer.setDataSetList(Parser.parseSDFileContent(requestTransfer.getFileContent()));
        requestTransfer.setSmilesList(Parser.smilesFileContentToList(requestTransfer.getFileContent()));
        dataSetList = this.resultsParser.parseAndPredict(requestTransfer, this.getMultiplicitySectionsSettings());

        resultTransfer.setDataSetList(dataSetList);
        return new ResponseEntity<>(resultTransfer, HttpStatus.OK);
    }

    public Map<String, int[]> getMultiplicitySectionsSettings() {
        final WebClient webClient = this.webClientBuilder.baseUrl(
                "http://webcase-gateway:8080/webcase-db-service-dataset/getMultiplicitySectionsSettings")
                                                         .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                        MediaType.APPLICATION_JSON_VALUE)
                                                         .exchangeStrategies(this.exchangeStrategies)
                                                         .build();

        return webClient.get()
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, int[]>>() {
                        })
                        .block();
    }
}
