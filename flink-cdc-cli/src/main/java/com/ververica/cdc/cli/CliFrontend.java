/*
 * Copyright 2023 Ververica Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ververica.cdc.cli;

import com.ververica.cdc.cli.utils.ConfigurationUtils;
import com.ververica.cdc.cli.utils.FlinkEnvironmentUtils;
import com.ververica.cdc.common.annotation.VisibleForTesting;
import com.ververica.cdc.common.configuration.Configuration;
import com.ververica.cdc.composer.PipelineComposer;
import com.ververica.cdc.composer.PipelineExecution;
import com.ververica.cdc.composer.flink.FlinkPipelineComposer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** The frontend entrypoint for the command-line interface of Flink CDC. */
public class CliFrontend {
    private static final Logger LOG = LoggerFactory.getLogger(CliFrontend.class);
    private static final String FLINK_HOME_ENV_VAR = "FLINK_HOME";
    private static final String FLINK_CDC_HOME_ENV_VAR = "FLINK_CDC_HOME";

    public static void main(String[] args) throws Exception {
        Options cliOptions = CliFrontendOptions.initializeOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(cliOptions, args);

        // Help message
        if (args.length == 0 || commandLine.hasOption(CliFrontendOptions.HELP)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.setLeftPadding(4);
            formatter.setWidth(80);
            formatter.printHelp(" ", cliOptions);
            return;
        }

        // Create executor and execute the pipeline
        PipelineExecution.ExecutionInfo result = createExecutor(commandLine).run();

        // Print execution result
        printExecutionInfo(result);
    }

    @VisibleForTesting
    static CliExecutor createExecutor(CommandLine commandLine) throws Exception {
        // The pipeline definition file would remain unparsed
        List<String> unparsedArgs = commandLine.getArgList();
        if (unparsedArgs.isEmpty()) {
            throw new IllegalArgumentException(
                    "Missing pipeline definition file path in arguments. ");
        }

        // Take the first unparsed argument as the pipeline definition file
        Path pipelineDefPath = Paths.get(unparsedArgs.get(0));
        if (!Files.exists(pipelineDefPath)) {
            throw new FileNotFoundException(
                    String.format("Cannot find pipeline definition file \"%s\"", pipelineDefPath));
        }

        // Global pipeline configuration
        Configuration globalPipelineConfig = getGlobalConfig(commandLine);

        // Create pipeline composer
        boolean useMiniCluster = commandLine.hasOption(CliFrontendOptions.USE_MINI_CLUSTER);
        PipelineComposer composer =
                useMiniCluster
                        ? FlinkPipelineComposer.ofMiniCluster()
                        : createRemoteComposer(commandLine);

        // Build executor
        return new CliExecutor(pipelineDefPath, globalPipelineConfig, composer);
    }

    private static PipelineComposer createRemoteComposer(CommandLine commandLine) throws Exception {
        // Load Flink environment
        Path flinkHome = getFlinkHome(commandLine);
        Configuration flinkConfig = FlinkEnvironmentUtils.loadFlinkConfiguration(flinkHome);

        // Additional JARs
        List<Path> additionalJars =
                Arrays.stream(
                                Optional.ofNullable(
                                                commandLine.getOptionValues(CliFrontendOptions.JAR))
                                        .orElse(new String[0]))
                        .map(Paths::get)
                        .collect(Collectors.toList());

        return FlinkPipelineComposer.ofRemoteCluster(
                org.apache.flink.configuration.Configuration.fromMap(flinkConfig.toMap()),
                additionalJars);
    }

    private static Path getFlinkHome(CommandLine commandLine) {
        // Check command line arguments first
        String flinkHomeFromArgs = commandLine.getOptionValue(CliFrontendOptions.FLINK_HOME);
        if (flinkHomeFromArgs != null) {
            LOG.debug("Flink home is loaded by command-line argument: {}", flinkHomeFromArgs);
            return Paths.get(flinkHomeFromArgs);
        }

        // Fallback to environment variable
        String flinkHomeFromEnvVar = System.getenv(FLINK_HOME_ENV_VAR);
        if (flinkHomeFromEnvVar != null) {
            LOG.debug("Flink home is loaded by environment variable: {}", flinkHomeFromEnvVar);
            return Paths.get(flinkHomeFromEnvVar);
        }

        throw new IllegalArgumentException(
                "Cannot find Flink home from either command line arguments \"--flink-home\" "
                        + "or the environment variable \"FLINK_HOME\". "
                        + "Please make sure Flink home is properly set. ");
    }

    private static Configuration getGlobalConfig(CommandLine commandLine) throws Exception {
        // Try to get global config path from command line
        String globalConfig = commandLine.getOptionValue(CliFrontendOptions.GLOBAL_CONFIG);
        if (globalConfig != null) {
            Path globalConfigPath = Paths.get(globalConfig);
            LOG.info("Using global config in command line: {}", globalConfigPath);
            return ConfigurationUtils.loadMapFormattedConfig(globalConfigPath);
        }

        // Fallback to Flink CDC home
        String flinkCdcHome = System.getenv(FLINK_CDC_HOME_ENV_VAR);
        if (flinkCdcHome != null) {
            Path globalConfigPath =
                    Paths.get(flinkCdcHome).resolve("conf").resolve("flink-cdc.yaml");
            LOG.info("Using global config in FLINK_CDC_HOME: {}", globalConfigPath);
            return ConfigurationUtils.loadMapFormattedConfig(globalConfigPath);
        }

        // Fallback to empty configuration
        LOG.warn(
                "Cannot find global configuration in command-line or FLINK_CDC_HOME. Will use empty global configuration.");
        return new Configuration();
    }

    private static void printExecutionInfo(PipelineExecution.ExecutionInfo info) {
        System.out.println("Pipeline has been submitted to cluster.");
        System.out.printf("Job ID: %s\n", info.getId());
        System.out.printf("Job Description: %s\n", info.getDescription());
    }
}
