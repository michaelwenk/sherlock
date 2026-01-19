package org.openscience.sherlock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SherlockCoreApplication {

    // final Gson gson = new GsonBuilder().setLenient()
    // .create();

    public static void main(final String[] args) {
        SpringApplication.run(SherlockCoreApplication.class, args);
    }

    // @Bean
    // public Map<String, Map<String, Double[]>> buildHoseCodeDBEntriesMap() {
    // final Map<String, Map<String, Double[]>> hoseCodeDBEntriesMap = new
    // HashMap<>();
    // this.fillHOSECodeDBEntriesMap(hoseCodeDBEntriesMap);
    //
    // return hoseCodeDBEntriesMap;
    // }
    //
    // private String loopMethod(final String pathToHOSECodesFile, final int
    // waitingDuration,
    // final int totalWaitingDuration, int currentWaitingDuration) {
    // final String fileContent = FileSystem.getFileContent(pathToHOSECodesFile);
    // if (fileContent
    // == null) {
    // try {
    // System.out.println(" -> could not read HOSE codes from file: \""
    // + pathToHOSECodesFile
    // + "\" -> trying again in "
    // + waitingDuration
    // + " ms");
    // Thread.sleep(waitingDuration);
    // currentWaitingDuration += waitingDuration;
    // } catch (final InterruptedException e) {
    // e.printStackTrace();
    // }
    // if (currentWaitingDuration
    // < totalWaitingDuration) {
    // return this.loopMethod(pathToHOSECodesFile, waitingDuration,
    // totalWaitingDuration,
    // currentWaitingDuration);
    // }
    // }
    //
    // return fileContent;
    // }
    //
    // private void fillHOSECodeDBEntriesMap(final Map<String, Map<String,
    // Double[]>> hoseCodeDBEntriesMap) {
    // System.out.println("\nloading DB entries map...");
    // final String pathToHOSECodesFile = "/data/hosecode/hosecodes.json";
    // final int waitingDuration = 30; // seconds
    // final int totalWaitingDuration = 300; // seconds
    // final String fileContent = this.loopMethod(pathToHOSECodesFile,
    // waitingDuration
    // * 1000, totalWaitingDuration
    // * 1000, 0);
    // if (fileContent
    // != null) {
    // final List<HOSECode> hoseCodeObjectList = this.gson.fromJson(fileContent, new
    // TypeToken<List<HOSECode>>() {
    // }.getType());
    // for (final HOSECode hoseCodeObject : hoseCodeObjectList) {
    // hoseCodeDBEntriesMap.put(hoseCodeObject.getHOSECode(),
    // hoseCodeObject.getValues());
    // }
    // System.out.println(" -> done: "
    // + hoseCodeDBEntriesMap.size());
    // } else {
    // System.out.println(" -> could not read HOSE codes from file: \""
    // + pathToHOSECodesFile
    // + "\" !!!");
    // }
    // }
}
