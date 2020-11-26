package org.openscience.webcase.nmr.model.bean;

import casekit.nmr.Utils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomType;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.silent.Atom;
import org.openscience.cdk.silent.Bond;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ExtendedConnectionMatrixBean {

    private double[][] connectionMatrix;
    private String[] atomTypes;
    private Integer[][] atomPropertiesNumeric;// hydrogenCounts, valencies, formalCharges;
    private IAtomType.Hybridization[] hybridizations;
    private Boolean[][] atomPropertiesBoolean;// isInRingAtoms, isAromaticAtoms;
    private Boolean[][][] bondProperties;
    private int bondCount;

    public IAtomContainer toAtomContainer(){
        final IAtomContainer ac = SilentChemObjectBuilder.getInstance().newAtomContainer();
        IAtom atom;
        for (int i = 0; i < this.connectionMatrix.length; i++) {
            atom = new Atom(this.atomTypes[i]);
            atom.setImplicitHydrogenCount(this.atomPropertiesNumeric[i][0]);
            atom.setValency(this.atomPropertiesNumeric[i][1]);
            atom.setFormalCharge(this.atomPropertiesNumeric[i][2]);
            atom.setHybridization(this.hybridizations[i]);
            atom.setIsInRing(this.atomPropertiesBoolean[i][0]);
            atom.setIsAromatic(this.atomPropertiesBoolean[i][1]);

            ac.addAtom(atom);
        }
        IBond bond;
        for (int i = 0; i < this.bondProperties.length; i++) {
            for (int k = i + 1; k < this.bondProperties.length; k++) {
                if(this.connectionMatrix[i][k] > 0.0){
                    bond = new Bond(ac.getAtom(i), ac.getAtom(k), Utils.getBondOrder((int) this.connectionMatrix[i][k]));
                    bond.setIsInRing(this.bondProperties[i][k][0]);
                    bond.setIsAromatic(this.bondProperties[i][k][1]);
                    ac.addBond(bond);
                }
            }
        }

        return ac;
    }

}
