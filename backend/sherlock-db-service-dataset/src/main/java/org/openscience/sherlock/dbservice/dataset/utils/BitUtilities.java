package org.openscience.sherlock.dbservice.dataset.utils;

import org.openscience.cdk.fingerprint.BitSetFingerprint;

import java.math.BigInteger;

public class BitUtilities {

    public static String[] buildSingleBitBigIntegerStrings(final int bitLength) {
        final String[] strings = new String[bitLength];
        for (int i = 0; i
                < bitLength; i++) {
            strings[i] = "B'"
                    + buildBitsString(buildBits(i, bitLength), bitLength)
                    + "'";
        }

        return strings;
    }

    public static BigInteger buildDefaultBits(final int bitLength, final boolean setBits) {
        return new BigInteger((setBits
                               ? "1"
                               : "0").repeat(Math.max(0, bitLength)), 2);
    }

    public static BigInteger buildBits(final int bit, final int bitLength) {
        BigInteger bigInteger = buildDefaultBits(bitLength, false);
        bigInteger = bigInteger.setBit(bit);

        return bigInteger;
    }

    public static BigInteger buildBits(final BitSetFingerprint bitSetFingerprint, final int bitLength) {
        BigInteger bigInteger = buildDefaultBits(bitLength, false);
        for (final int setBit : bitSetFingerprint.getSetbits()) {
            bigInteger = bigInteger.setBit(setBit);
        }

        return bigInteger;
    }

    public static BigInteger flipBits(final BigInteger bigInteger, final int bitLength) {
        BigInteger flippedBigInteger = new BigInteger(buildBitsString(bigInteger, bitLength), 2);
        for (int i = 0; i
                < bitLength; i++) {
            flippedBigInteger = flippedBigInteger.flipBit(i);
        }

        return flippedBigInteger;
    }


    public static String buildBitsString(final BigInteger bigInteger, final int bitLength) {
        final String output = bigInteger.toString(2);
        final StringBuilder stringBuilder = new StringBuilder(output);
        while (stringBuilder.toString()
                            .length()
                < bitLength) {
            stringBuilder.insert(0, "0");
        }

        return stringBuilder.toString();
    }
}
