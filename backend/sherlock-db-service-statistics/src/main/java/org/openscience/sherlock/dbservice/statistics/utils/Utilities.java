package org.openscience.sherlock.dbservice.statistics.utils;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.io.MDLV3000Writer;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.sherlock.dbservice.statistics.service.model.DataSetRecord;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Utilities {

    public static Flux<DataSetRecord> getAllDataSets(final WebClient.Builder webClientBuilder,
                                                     final ExchangeStrategies exchangeStrategies) {
        final WebClient webClient = webClientBuilder.baseUrl(
                                                            "http://sherlock-gateway:8080/sherlock-db-service-dataset/dataset/getAll")
                                                    .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                   MediaType.APPLICATION_JSON_VALUE)
                                                    .exchangeStrategies(exchangeStrategies)
                                                    .build();

        return webClient.get()
                        .retrieve()
                        .bodyToFlux(DataSetRecord.class);
    }

    public static Flux<DataSetRecord> getByDataSetSpectrumNuclei(final WebClient.Builder webClientBuilder,
                                                                 final ExchangeStrategies exchangeStrategies,
                                                                 final String[] nuclei) {
        final WebClient webClient = webClientBuilder.baseUrl(
                                                            "http://sherlock-gateway:8080/sherlock-db-service-dataset/dataset")
                                                    .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                   MediaType.APPLICATION_JSON_VALUE)
                                                    .exchangeStrategies(exchangeStrategies)
                                                    .build();
        // @TODO take the nuclei order into account when matching -> now it's just an exact array match
        final String nucleiString = Arrays.stream(nuclei)
                                          .reduce("", (concat, current) -> concat
                                                  + current);
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
        uriComponentsBuilder.path("/getByNuclei")
                            .queryParam("nuclei", nucleiString);

        return webClient.get()
                        .uri(uriComponentsBuilder.toUriString())
                        .retrieve()
                        .bodyToFlux(DataSetRecord.class);
    }

    public static String createMolFileContent(final IAtomContainer structure) throws CDKException {
        final MDLV3000Writer mdlv3000Writer = new MDLV3000Writer();
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        mdlv3000Writer.setWriter(byteArrayOutputStream);
        mdlv3000Writer.write(structure);

        return byteArrayOutputStream.toString();
    }

    public static boolean setUpAndDownBondsOnTwoMethylGroups(final IAtomContainer structure,
                                                             final boolean resetBondStereo) {
        final List<IBond> bondsToUnset = new ArrayList<>();
        for (final IBond bond : structure.bonds()) {
            bondsToUnset.add(bond);
        }

        for (final IAtom atom : structure.atoms()) {
            if (AtomContainerManipulator.countHydrogens(structure, atom)
                    != 0
                    || !atom.getSymbol()
                            .equals("C")) {
                continue;
            }
            final List<Integer> methylGroups = new ArrayList<>();
            final List<IAtom> connectedAtomsList = structure.getConnectedAtomsList(atom);
            for (final IAtom connectedAtom : connectedAtomsList) {
                if (AtomContainerManipulator.countHydrogens(structure, connectedAtom)
                        == 3) {
                    methylGroups.add(connectedAtom.getIndex());
                }
            }
            if (methylGroups.size()
                    == 2) {
                final IBond bond1 = structure.getBond(atom, structure.getAtom(methylGroups.get(0)));
                final IBond bond2 = structure.getBond(atom, structure.getAtom(methylGroups.get(1)));
                if (bond1.getStereo()
                         .equals(IBond.Stereo.UP)
                        || bond1.getStereo()
                                .equals(IBond.Stereo.DOWN)) {
                    bondsToUnset.remove(bond1);
                    if (bond1.getStereo()
                             .equals(IBond.Stereo.UP)) {
                        bond2.setStereo(IBond.Stereo.DOWN);
                        bondsToUnset.remove(bond2);
                    } else {
                        bond2.setStereo(IBond.Stereo.UP);
                        bondsToUnset.remove(bond2);
                    }
                } else if (bond2.getStereo()
                                .equals(IBond.Stereo.UP)
                        || bond2.getStereo()
                                .equals(IBond.Stereo.DOWN)) {
                    bondsToUnset.remove(bond2);
                    if (bond2.getStereo()
                             .equals(IBond.Stereo.UP)) {
                        bond1.setStereo(IBond.Stereo.DOWN);
                        bondsToUnset.remove(bond1);
                    } else {
                        bond1.setStereo(IBond.Stereo.UP);
                        bondsToUnset.remove(bond1);
                    }
                }
            }
        }

        if (resetBondStereo) {
            for (final IBond bond : bondsToUnset) {
                bond.setStereo(IBond.Stereo.NONE);
            }
        }

        return structure.getBondCount()
                != bondsToUnset.size();
    }
}
