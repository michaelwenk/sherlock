package org.openscience.webcase.pylsd.utils.detection;

import casekit.nmr.model.nmrium.Correlation;
import org.openscience.webcase.pylsd.model.Detections;
import org.openscience.webcase.pylsd.model.exchange.Transfer;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

public class Detection {

    public static Transfer detect(final WebClient.Builder webClientBuilder, final Transfer requestTransfer) {
        final Transfer responseTransfer = new Transfer();
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

        final Map<Integer, Map<String, Set<Integer>>> detectedConnectivities = ConnectivityDetection.detectConnectivities(
                webClientBuilder, correlationList, shiftTol, requestTransfer.getDetectionOptions()
                                                                            .getElementCountThreshold(),
                requestTransfer.getMf());

        System.out.println("detectedConnectivities: "
                                   + detectedConnectivities);

        final Map<Integer, Map<String, Set<Integer>>> forbiddenNeighbors = ForbiddenNeighborDetection.detectForbiddenNeighbors(
                detectedConnectivities, requestTransfer.getMf());
        System.out.println("-> forbiddenNeighbors: "
                                   + forbiddenNeighbors);

        responseTransfer.setDetections(
                new Detections(detectedHybridizations, detectedConnectivities, forbiddenNeighbors, new HashMap<>()));

        return responseTransfer;
    }
}