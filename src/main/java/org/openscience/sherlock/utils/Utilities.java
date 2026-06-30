package org.openscience.sherlock.utils;

import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import casekit.nmr.model.SpectrumCompact;
import casekit.nmr.model.nmrium.Correlations;
import casekit.nmr.utils.Utils;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.io.MDLV3000Writer;
import org.openscience.sherlock.dbservice.dataset.controller.DataSetController;
import org.openscience.sherlock.dbservice.dataset.db.model.DataSetRecord;
import org.openscience.sherlock.dbservice.result.model.ResultRecord;
import org.openscience.sherlock.model.exchange.RequestData;
import org.openscience.sherlock.model.exchange.RequestResult;
import org.springframework.http.ResponseEntity;

import reactor.core.publisher.Flux;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Utilities {

    public static Flux<DataSetRecord> getDataSetRecordFlux(
            final DataSetController dataSetController,
            final Spectrum querySpectrum, final String mf) {

        // @TODO take the nuclei order into account when matching -> now it's just an
        // exact array match
        if (mf != null) {
            return dataSetController.getByDataSetSpectrumNucleiAndDataSetSpectrumSignalCountAndMf(querySpectrum
                    .getNuclei(),
                    querySpectrum.getSignalCount(),
                    Utils.getAlphabeticMF(mf));

        }

        return dataSetController.getByDataSetSpectrumNucleiAndDataSetSpectrumSignalCount(querySpectrum
                .getNuclei(),
                querySpectrum.getSignalCount());
    }

    public static DataSet addMolFileToDataSet(final DataSet dataSet) throws CDKException {
        // store as MOL file
        final MDLV3000Writer mdlv3000Writer = new MDLV3000Writer();
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        mdlv3000Writer.setWriter(byteArrayOutputStream);
        mdlv3000Writer.write(dataSet.getStructure()
                .toAtomContainer());
        dataSet.addMetaInfo("molfile", byteArrayOutputStream.toString());

        return dataSet;
    }

    public static List<DataSet> addMolFileToDataSets(final List<DataSet> dataSetList) throws CDKException {
        for (final DataSet dataSet : dataSetList) {
            addMolFileToDataSet(dataSet);
        }

        return dataSetList;
    }

    public static RequestResult prepareDefaultRequestResult(final RequestData requestData) {
        final RequestResult requestResult = new RequestResult();
        // INPUT DATA CHECK
        final String errorMessage = Utilities.checkAndPrepareRequestData(requestData);
        if (errorMessage != null) {
            requestResult.setErrorMessage(errorMessage);
            return requestResult;
        }

        final Correlations correlations = requestData.getCorrelations();
        final Spectrum querySpectrum = Utils.correlationListToSpectrum1D(correlations.getValues(), "13C");

        requestResult.setQueryType(requestData.getQueryType());
        // requestResult.setRequestId(IdGenerator.generateId());

        requestResult.setDereplicationOptions(requestData.getDereplicationOptions());
        requestResult.setResultRecord(new ResultRecord());
        requestResult.getResultRecord()
                .setQuerySpectrum(new SpectrumCompact(querySpectrum));
        requestResult.getResultRecord().setCorrelations(correlations);
        requestResult.getResultRecord().setDetected(requestData.getDetected());
        requestResult.getResultRecord().setDetections(requestData.getDetections());
        requestResult.getResultRecord().setDetectionOptions(requestData.getDetectionOptions());
        requestResult.getResultRecord().setGrouping(requestData.getGrouping());
        requestResult.getResultRecord().setElucidationOptions(requestData.getElucidationOptions());
        requestResult.getResultRecord().setName(requestData.getName());
        requestResult.getResultRecord().setDataSetList(new ArrayList<>());
        requestResult.getResultRecord().setDataSetListSize(0);
        requestResult.getResultRecord().setPreviewDataSet(null);

        return requestResult;
    }

    public static String getMolecularFormulaFromCorrelations(final Correlations correlations) {
        final String mf = (String) correlations.getOptions()
                .get("mf");
        if (mf == null) {
            return null;
        }

        return Utils.getAlphabeticMF(mf);
    }

    public static String checkAndPrepareRequestData(final RequestData requestData) {
        // check for correlation data
        final Correlations correlations = requestData.getCorrelations();
        if (correlations == null) {
            return "Correlation data is missing!!!";
        }
        // check for query spectrum
        final Spectrum querySpectrum = Utils.correlationListToSpectrum1D(correlations.getValues(), "13C");
        if (querySpectrum == null) {
            return "Query spectrum is missing!!!";
        }
        // check whether each signal has a multiplicity; if not stop here
        if (querySpectrum.getSignals()
                .stream()
                .anyMatch(signal -> signal.getMultiplicity() == null)) {
            return "At least for one carbon the number of attached protons is missing!!!";
        }
        final String mf = Utilities.getMolecularFormulaFromCorrelations(correlations);
        // check for mf
        if (mf == null) {
            return "Molecular formula is missing!!!";
        }
        // // check for error state
        // final Map<String, Map<String, Object>> state = requestTransfer.getData()
        // .getCorrelations()
        // .getState();
        // final Map<String, Map<String, Boolean>> errors = new HashMap<>();
        // for (final Map.Entry<String, Map<String, Object>> atomTypeEntry :
        // state.entrySet()) {
        // if (atomTypeEntry.getValue()
        // .containsKey("error")
        // && !((Map<String, Object>) atomTypeEntry.getValue()
        // .get("error")).isEmpty()) {
        // errors.putIfAbsent(atomTypeEntry.getKey(), new HashMap<>());
        // for (final Map.Entry<String, Boolean> errorEntry : ((Map<String, Boolean>)
        // atomTypeEntry.getValue()
        // .get("error")).entrySet()) {
        // errors.get(atomTypeEntry.getKey())
        // .put(errorEntry.getKey(), errorEntry.getValue());
        // }
        // }
        // }
        // if (!errors.isEmpty()) {
        // System.out.println("ERRORS: "
        // + errors);
        // // responseTransfer.setErrorMessage("There are errors in correlation data:\n"
        // // + errors);
        // // return new ResponseEntity<>(responseTransfer, HttpStatus.BAD_REQUEST);
        // }

        final Map<String, Number> tolerances = (Map<String, Number>) correlations.getOptions().get("tolerance");
        for (final String atomType : tolerances.keySet()) {
            if (tolerances.get(atomType) instanceof Integer) {
                tolerances.put(atomType, tolerances.get(atomType)
                        .doubleValue());
            }
        }
        correlations.getOptions()
                .put("tolerance", tolerances);

        return null; // no error
    }
}
