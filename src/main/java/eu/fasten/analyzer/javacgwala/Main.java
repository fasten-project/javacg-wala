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

package eu.fasten.analyzer.javacgwala;

import eu.fasten.analyzer.javacgwala.core.MavenCoordinate;
import eu.fasten.analyzer.javacgwala.core.RevisionCallGraph;
import eu.fasten.analyzer.javacgwala.data.callgraph.PartialCallGraph;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Makes javacg-wala module runnable from command line.
 */
@CommandLine.Command(name = "JavaCGWala")
public class Main implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @CommandLine.ArgGroup()
    SetRunner setRunner;

    @CommandLine.Option(names = {"-r"},
            paramLabel = "REPOS",
            description = "Maven repositories",
            split = ",")
    List<String> repos;

    @CommandLine.Option(names = {"-t", "--timestamp"},
            paramLabel = "TS",
            description = "Release TS",
            defaultValue = "0")
    String timestamp;

    @CommandLine.Option(names = {"-o", "--output"},
            paramLabel = "OUT",
            description = "Output path")
    String outputPath;

    @CommandLine.Option(names = {"--stdout"},
            paramLabel = "STDOUT",
            description = "Write to stdout")
    boolean writeToStdout;

    static class FullCoordinate {
        @CommandLine.Option(names = {"-c", "--coord"},
                paramLabel = "COORD",
                description = "Maven coordinates string",
                required = true)
        String mavenCoordStr;
    }

    static class PathToFile {
        @CommandLine.Option(names = {"-f", "--path"},
                paramLabel = "PATH",
                description = "Path to file",
                required = true)
        String path;

        @CommandLine.Option(names = {"-p", "--product"},
                paramLabel = "PRODUCT",
                description = "Product",
                defaultValue = "PRODUCT")
        String product;

        @CommandLine.Option(names = {"-v", "--version"},
                paramLabel = "VERSION",
                description = "Callgraph version",
                defaultValue = "0.0.0")
        String version;

        @CommandLine.Option(names = {"-d", "--dependencies"},
                paramLabel = "DEPENDENCIES",
                description = "One or more dependency coordinate to merge with the artifact",
                split = ",")
        String[] dependencies;
    }

    static class SetRunner {
        @CommandLine.ArgGroup()
        FullCoordinate fullCoordinate;

        @CommandLine.ArgGroup(exclusive = false)
        PathToFile pathToFile;

        @CommandLine.Option(names = {"-s", "--set"},
                paramLabel = "Set",
                description = "Set of maven coordinates",
                required = true)
        String set;
    }

    /**
     * Generates RevisionCallGraphs using Opal for the specified artifact in the command line
     * parameters.
     */
    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }

    /**
     * Runs Wala plugin.
     */
    public void run() {
        MavenCoordinate mavenCoordinate;
        if (setRunner != null && setRunner.pathToFile != null) {
            final List<List<RevisionCallGraph.Dependency>> dependencies = new ArrayList<>();

            final List<MavenCoordinate> coordinates = new ArrayList<>();
            if (setRunner.pathToFile.dependencies != null) {
                for (String currentCoordinate : setRunner.pathToFile.dependencies) {
                    var coordinate = MavenCoordinate.fromString(currentCoordinate);
                    if (repos.size() > 0) {
                        coordinate.setMavenRepos(repos);
                    }
                    coordinates.add(coordinate);
                }
            }

            for (var coordinate : coordinates) {
                dependencies.addAll(MavenCoordinate.MavenResolver
                        .resolveDependencies(coordinate));
            }
            var rcg = PartialCallGraph.generateERCG(setRunner.pathToFile.path,
                    setRunner.pathToFile.product, setRunner.pathToFile.version,
                    Long.parseLong(timestamp), dependencies);

            try {
                writeCallgraph(rcg);
            } catch (IOException e) {
                logger.info("Couldn't write to the file");
            }

        } else if (setRunner != null && setRunner.set != null) {
            consumeSet(setRunner.set);
        } else if (setRunner != null && setRunner.fullCoordinate.mavenCoordStr != null) {
            mavenCoordinate = MavenCoordinate
                    .fromString(this.setRunner.fullCoordinate.mavenCoordStr);
            if (repos != null && repos.size() > 0) {
                mavenCoordinate.setMavenRepos(repos);
            }

            try {
                final var revisionCallGraph = PartialCallGraph.createExtendedRevisionCallGraph(
                        mavenCoordinate,
                        Long.parseLong(this.timestamp));

                try {
                    writeCallgraph(revisionCallGraph);
                } catch (IOException e) {
                    logger.info("Couldn't write to the file");
                }

            } catch (Throwable e) {
                logger.error("Failed to generate a call graph for Maven coordinate: {}, Error: {}",
                        mavenCoordinate.getCoordinate(), e);
            }
        }
    }

    /**
     * Consume a set of maven coordinates and generate call graphs for them.
     *
     * @param path Path to the file containing maven coordinates.
     */
    private void consumeSet(String path) {
        List<String> successfulRecords = new ArrayList<>();
        Map<String, String> failedRecords = new HashMap<>();
        Map<String, Integer> errorOccurrences = new HashMap<>();

        for (var coordinate : getCoordinates(path)) {
            final var mavenCoordinate = getMavenCoordinate(coordinate);
            if (mavenCoordinate != null && repos != null && repos.size() > 0) {
                mavenCoordinate.setMavenRepos(repos);
            }

            try {
                var cg = PartialCallGraph.createExtendedRevisionCallGraph(
                        mavenCoordinate,
                        Long.parseLong(coordinate.get("date").toString()));

                int totalCalls = cg.getGraph().getExternalCalls().size()
                        + cg.getGraph().getInternalCalls().size();

                successfulRecords.add("Number of calls: " + totalCalls
                        + " COORDINATE: " + mavenCoordinate.getCoordinate());

                logger.info("Call graph successfully generated for {}!",
                        mavenCoordinate.getCoordinate());

                writeCallgraph(cg);

            } catch (IOException e) {
                logger.info("Couldn't write to the file");
            } catch (Throwable e) {
                JSONObject error = new JSONObject().put("plugin", this.getClass().getSimpleName())
                        .put("msg", e.getMessage())
                        .put("trace", e.getStackTrace())
                        .put("type", e.getClass().getSimpleName());

                String errorType = error.get("type").toString();

                if (mavenCoordinate != null) {
                    failedRecords.put(mavenCoordinate.getCoordinate(), errorType);
                    logger.info("Failed to generate a call graph for {}!",
                            mavenCoordinate.getCoordinate());
                } else {
                    failedRecords.put("UNKNOWN COORDINATE", errorType);
                    logger.info("Failed to generate a call graph for UNKNOWN COORDINATE!");
                }


                if (errorOccurrences.containsKey(errorType)) {
                    errorOccurrences.put(errorType, errorOccurrences.get(errorType) + 1);
                } else {
                    errorOccurrences.put(errorType, 1);
                }
            }
        }

        printStats(successfulRecords, failedRecords, errorOccurrences);
    }

    /**
     * Print statistics of call graph generation.
     *
     * @param successfulRecords Records that were successfully processed
     * @param failedRecords     Failed records
     * @param errorOccurrences  Map of error and number of their occurrences
     */
    private void printStats(List<String> successfulRecords, Map<String, String> failedRecords,
                            Map<String, Integer> errorOccurrences) {
        for (var record : successfulRecords) {
            System.out.println(record);
        }

        for (var record : failedRecords.entrySet()) {
            System.out.println(record.getKey() + " ERROR: " + record.getValue());
        }

        int total = successfulRecords.size() + failedRecords.size();

        System.out.println();
        System.out.println("===================SUMMARY=================");
        System.out.println("Total number of analyzed coordinates: \t" + total);
        System.out.println("Total number of successful: \t\t\t" + successfulRecords.size());
        System.out.println("Total number of failed: \t\t\t\t" + failedRecords.size());
        System.out.println("Most common exceptions: ");

        var sortedErrorMap = errorOccurrences.entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        for (var entry : sortedErrorMap.entrySet()) {
            System.out.println("\t [" + entry.getKey() + " - " + entry.getValue() + "]");
        }

        if (total > 0) {
            System.out.println("Success rate: \t\t\t\t\t\t\t"
                    + 100 * successfulRecords.size() / total + "%");
        }
    }

    /**
     * Parse JSON representation of MavenCoordinate.
     *
     * @param kafkaConsumedJson Json maven coordinate
     * @return MavenCoordinate
     */
    private MavenCoordinate getMavenCoordinate(final JSONObject kafkaConsumedJson) {

        try {
            return new MavenCoordinate(
                    kafkaConsumedJson.get("groupId").toString(),
                    kafkaConsumedJson.get("artifactId").toString(),
                    kafkaConsumedJson.get("version").toString());
        } catch (JSONException e) {
            logger.error("Could not parse input coordinate: {}", kafkaConsumedJson);
        }
        return null;
    }

    /**
     * Process the file containing maven coordinates and convert it to list of JSON maven
     * coordinates.
     *
     * @param path Path to the file
     * @return List of Json objects
     */
    private List<JSONObject> getCoordinates(String path) {
        try {
            File file = new File(path);
            BufferedReader br = new BufferedReader(new FileReader(file));
            return br.lines()
                    .map(JSONObject::new)
                    .collect(Collectors.toList());


        } catch (IOException | StringIndexOutOfBoundsException e) {
            logger.error("Couldn't parse a file with coordinates");
        }

        return new ArrayList<>();
    }

    /**
     * Writes a callgraph to a specified path.
     *
     * @param graph a callgraph to write to file
     * @throws IOException cannot write to a file
     */
    private void writeCallgraph(RevisionCallGraph graph) throws IOException {
        if (this.outputPath != null) {
            final BufferedWriter writer = new BufferedWriter(
                    new FileWriter(outputPath + "/" + graph.product + "-v" + graph.version + ".json"));
            writer.write(graph.toJSON().toString(4));
            writer.close();
            logger.info("Successfully written the call graph into a file");
        }
        if (writeToStdout) {
            System.out.println(graph.toJSON().toString());
        }
    }
}


