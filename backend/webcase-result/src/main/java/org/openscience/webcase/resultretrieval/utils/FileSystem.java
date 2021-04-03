package org.openscience.webcase.resultretrieval.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileSystem {

    public static BufferedReader readFile(final String pathToFile) {
        try {
            return new BufferedReader(new FileReader(pathToFile));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static List<String> getSmilesListFromFile(final String pathToSmilesFile){
        final List<String> smilesList = new ArrayList<>();
        try {
            final BufferedReader bufferedReader = FileSystem.readFile(pathToSmilesFile);
            if (bufferedReader != null) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    smilesList.add(line);
                }
                bufferedReader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return smilesList;
    }
}
