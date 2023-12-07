/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.cdc.cli.utils;

import org.apache.flink.cdc.common.configuration.Configuration;
import org.apache.flink.cdc.composer.flink.FlinkPipelineComposer;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Utilities for handling Flink configuration and environment. */
public class FlinkEnvironmentUtils {

    private static final String FLINK_CONF_DIR = "conf";
    private static final String FLINK_CONF_FILENAME = "flink-conf.yaml";

    public static Configuration loadFlinkConfiguration(Path flinkHome) throws Exception {
        Path flinkConfPath = flinkHome.resolve(FLINK_CONF_DIR).resolve(FLINK_CONF_FILENAME);
        return ConfigurationUtils.loadMapFormattedConfig(flinkConfPath);
    }

    public static FlinkPipelineComposer createComposer(
            boolean useMiniCluster,
            Configuration flinkConfig,
            Configuration pipelineConfig,
            List<Path> additionalJars) {

        org.apache.flink.configuration.Configuration envConfig =
                new org.apache.flink.configuration.Configuration();
        Optional.ofNullable(flinkConfig)
                .ifPresent(
                        configuration ->
                                envConfig.addAll(
                                        org.apache.flink.configuration.Configuration.fromMap(
                                                configuration.toMap())));
        Optional.ofNullable(pipelineConfig)
                .ifPresent(
                        configuration ->
                                envConfig.addAll(
                                        org.apache.flink.configuration.Configuration.fromMap(
                                                configuration.toMap())));

        if (useMiniCluster) {
            return FlinkPipelineComposer.ofMiniCluster(envConfig);
        }

        return FlinkPipelineComposer.ofRemoteCluster(envConfig, additionalJars);
    }
}
