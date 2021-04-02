package org.openscience.webcase.core.utils;

import org.openscience.webcase.core.model.nmrdisplayer.Correlation;

public class Utils {

    /**
     * Specified for carbons only -> not generic!!!
     */
    public static String getMultiplicityFromProtonsCount(final Correlation correlation) {
        if (correlation.getAtomType()
                       .equals("C")
                && correlation.getProtonsCount()
                              .size()
                == 1) {
            switch (correlation.getProtonsCount()
                               .get(0)) {
                case 0:
                    return "s";
                case 1:
                    return "d";
                case 2:
                    return "t";
                case 3:
                    return "q";
                default:
                    return null;
            }
        }
        return null;
    }
}
