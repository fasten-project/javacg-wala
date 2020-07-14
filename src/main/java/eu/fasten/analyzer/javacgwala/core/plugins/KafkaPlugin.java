/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.fasten.analyzer.javacgwala.core.plugins;

import java.util.List;
import java.util.Optional;

public interface KafkaPlugin extends FastenPlugin {
    /**
     * Returns an optional singleton list with a Kafka topic from which messages
     * need to be consumed. If Optional.empty() is returned, the plugin
     * will work in producer mode only.
     *
     * @return a list with a Kafka topic
     */
    Optional<List<String>> consumeTopic();

    /**
     * Overrides a consume topic of a plug-in.
     *
     * @param topicName new consume topic
     */
    void setTopic(String topicName);

    /**
     * Process an incoming record. This method return only when a record has been
     * processed.
     *
     * @param record a record to process
     */
    void consume(String record);

    /**
     * Return an optional results of the computation. The result is appended to
     * the payload of standard output message. If Optional.empty() is returned,
     * a standard output message with an empty payload is written.
     *
     * @return optional result of the computation
     */
    Optional<String> produce();

    /**
     * Returns a relative path to a file, the result of processing
     * a record should be written to. THe path has the following hierarchy:
     * /forge/first-letter-of-artifactId/artifactId/artifactId_groupId_Version.json
     *
     * @return relative path to the output file
     */
    String getOutputPath();
}
