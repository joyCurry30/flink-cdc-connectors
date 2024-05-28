package org.apache.flink.cdc.example;

import org.apache.flink.cdc.cli.CliFrontend;

public class ExampleMain {
    public static void main(String[] args) throws Exception {
        String jobPath = "/Users/lapata/clickzetta_jobs/flink-cdc-3.0/mysql_2_values_1.yaml";
        String[] extArgs = {jobPath, "--use-mini-cluster", "true"};

        CliFrontend.main(extArgs);
    }
}