package org.apache.flink.cdc.example;

import org.apache.flink.cdc.cli.CliFrontend;

public class ExampleMain {
    public static void main(String[] args) throws Exception {
        String jobPath = "/Users/lapata/clickzetta_jobs/flink-cdc-3.0/values_2_values.yml";
        String[] extArgs = {jobPath, "-t", "yarn-application"};

        CliFrontend.main(extArgs);
    }
}
