package org.openscience.webcase.pylsd.utils;

import casekit.io.FileSystem;
import casekit.nmr.model.DataSet;
import org.openscience.webcase.pylsd.model.exchange.Transfer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RetrieveAndRank {

    public static List<DataSet> retrieveAndRankResultsFromPyLSDOutputFile(final WebClient.Builder webClientBuilder,
                                                                          final ExchangeStrategies exchangeStrategies,
                                                                          final String pathToResultsFile,
                                                                          final Transfer requestTransfer) {
        final BufferedReader bufferedReader = FileSystem.readFile(pathToResultsFile);
        if (bufferedReader
                == null) {
            System.out.println("retrieveAndRankResultsFromPyLSDOutputFile: could not read file \""
                                       + pathToResultsFile
                                       + "\"");
            return new ArrayList<>();
        }
        final WebClient webClient = webClientBuilder.baseUrl(
                "http://webcase-gateway:8080/webcase-db-service-hosecode/fileParser/parseResultFile")
                                                    .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                   MediaType.APPLICATION_JSON_VALUE)
                                                    .exchangeStrategies(exchangeStrategies)
                                                    .build();
        final String fileContent = bufferedReader.lines()
                                                 .collect(Collectors.joining("\n"));
        final Transfer queryTransfer = new Transfer();
        queryTransfer.setFileContent(fileContent);
        queryTransfer.setData(requestTransfer.getData());
        queryTransfer.setElucidationOptions(requestTransfer.getElucidationOptions());
        return webClient.post()
                        .bodyValue(queryTransfer)
                        .retrieve()
                        .bodyToMono(Transfer.class)
                        .block()
                        .getDataSetList();
    }
}
