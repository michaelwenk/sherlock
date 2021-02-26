package org.openscience.webcase.resultretrieval.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FileSystem {

    public static BufferedReader readFile(final String pathToFile) {
        try {
            return new BufferedReader(new FileReader(pathToFile));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
