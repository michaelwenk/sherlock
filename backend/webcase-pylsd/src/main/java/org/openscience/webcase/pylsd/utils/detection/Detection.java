package org.openscience.webcase.pylsd.utils.detection;

import casekit.nmr.lsd.Utilities;
import casekit.nmr.model.nmrium.Correlation;
import org.openscience.webcase.pylsd.model.exchange.Transfer;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Detection {

    public static Transfer detect(final WebClient.Builder webClientBuilder, final Transfer requestTransfer) {
        final int shiftTol = 0;
        final List<Correlation> correlationList = requestTransfer.getData()
                                                                 .getCorrelations()
                                                                 .getValues();
        final Map<Integer, List<Integer>> detectedHybridizations = HybridizationDetection.detectHybridizations(
                webClientBuilder, correlationList, requestTransfer.getDetectionOptions()
                                                                  .getHybridizationDetectionThreshold(), shiftTol);
        System.out.println("detectedHybridizations: "
                                   + detectedHybridizations);
        // set hybridization of correlations from detection if there was nothing set before
        for (final Map.Entry<Integer, List<Integer>> entry : detectedHybridizations.entrySet()) {
            if (correlationList.get(entry.getKey())
                               .getHybridization()
                    == null
                    || correlationList.get(entry.getKey())
                                      .getHybridization()
                                      .isEmpty()) {
                correlationList.get(entry.getKey())
                               .setHybridization(new ArrayList<>(entry.getValue()
                                                                      .stream()
                                                                      .map(numericHybridization -> "SP"
                                                                              + numericHybridization)
                                                                      .collect(Collectors.toList())));
            }
        }

        final Map<Integer, Map<String, Map<String, Set<Integer>>>> detectedConnectivities = ConnectivityDetection.detectConnectivities(
                webClientBuilder, correlationList, shiftTol, requestTransfer.getDetectionOptions()
                                                                            .getHybridizationCountThreshold(),
                requestTransfer.getDetectionOptions()
                               .getProtonsCountThreshold(), requestTransfer.getMf());

        System.out.println("detectedConnectivities: "
                                   + detectedConnectivities);
        System.out.println("allowedNeighborAtomHybridizations: "
                                   + Utilities.buildAllowedNeighborAtomHybridizations(correlationList,
                                                                                      detectedConnectivities));
        System.out.println("allowedNeighborAtomProtonCounts: "
                                   + Utilities.buildAllowedNeighborAtomProtonCounts(correlationList,
                                                                                    detectedConnectivities));
        final Map<Integer, Map<String, Map<Integer, Set<Integer>>>> forbiddenNeighbors = ForbiddenNeighborDetection.detectForbiddenNeighbors(
                detectedConnectivities, requestTransfer.getMf());
        System.out.println("-> forbiddenNeighbors: "
                                   + forbiddenNeighbors);

        final Transfer responseTransfer = new Transfer();
        responseTransfer.setDetectedHybridizations(detectedHybridizations);
        responseTransfer.setDetectedConnectivities(detectedConnectivities);
        responseTransfer.setForbiddenNeighbors(forbiddenNeighbors);

        return responseTransfer;
    }
}
