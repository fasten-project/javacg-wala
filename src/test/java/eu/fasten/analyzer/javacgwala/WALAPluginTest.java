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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import eu.fasten.analyzer.javacgwala.core.MavenCoordinate;
import eu.fasten.analyzer.javacgwala.core.RevisionCallGraph;
import eu.fasten.analyzer.javacgwala.data.callgraph.PartialCallGraph;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@Disabled("Disabled until a way to make Wala platform independent found")
class WALAPluginTest {

    static WALAPlugin.WALA walaPlugin;

    @BeforeAll
    public static void setUp() {
        walaPlugin = new WALAPlugin.WALA();
    }

    @Test
    public void testConsumerTopic() {
        assertTrue(walaPlugin.consumeTopic().isPresent());
        assertEquals("fasten.maven.pkg", walaPlugin.consumeTopic().get().get(0));
    }

    @Test
    public void testSetTopic() {
        String topicName = "fasten.mvn.pkg";
        walaPlugin.setTopic(topicName);
        assertTrue(walaPlugin.consumeTopic().isPresent());
        assertEquals(topicName, walaPlugin.consumeTopic().get().get(0));
    }

    @Test
    public void testConsume() throws JSONException, IOException, ClassHierarchyException, CallGraphBuilderCancelException {

        JSONObject coordinateJSON = new JSONObject("{\n" +
                "    \"groupId\": \"org.slf4j\",\n" +
                "    \"artifactId\": \"slf4j-api\",\n" +
                "    \"version\": \"1.7.29\",\n" +
                "    \"date\":\"1574072773\"\n" +
                "}");

        walaPlugin.consume(coordinateJSON.toString());

        var coordinate = new MavenCoordinate("org.slf4j", "slf4j-api", "1.7.29");
        var revisionCallGraph = PartialCallGraph.createExtendedRevisionCallGraph(coordinate,
                1574072773);

        assertTrue(walaPlugin.produce().isPresent());
        assertEquals(revisionCallGraph.toJSON().toString(), walaPlugin.produce().get());
    }

    @Test
    public void testEmptyCallGraph() {
        JSONObject emptyCGCoordinate = new JSONObject("{\n" +
                "    \"groupId\": \"activemq\",\n" +
                "    \"artifactId\": \"activemq\",\n" +
                "    \"version\": \"release-1.5\",\n" +
                "    \"date\":\"1574072773\"\n" +
                "}");

        walaPlugin.consume(emptyCGCoordinate.toString());

        assertTrue(walaPlugin.produce().isEmpty());
    }

    @Test
    public void testFileNotFoundException() {
        JSONObject noJARFile = new JSONObject("{\n" +
                "    \"groupId\": \"com.visionarts\",\n" +
                "    \"artifactId\": \"power-jambda-pom\",\n" +
                "    \"version\": \"0.9.10\",\n" +
                "    \"date\":\"1521511260\"\n" +
                "}");

        walaPlugin.consume(noJARFile.toString());
        var error = walaPlugin.getPluginError();

        assertTrue(walaPlugin.produce().isPresent());
        assertEquals(FileNotFoundException.class.getSimpleName(), error.getClass().getSimpleName());
    }

    @Test
    public void testShouldNotFaceClassReadingError() throws JSONException, IOException, ClassHierarchyException, CallGraphBuilderCancelException {

        JSONObject coordinateJSON1 = new JSONObject("{\n" +
                "    \"groupId\": \"com.zarbosoft\",\n" +
                "    \"artifactId\": \"coroutines-core\",\n" +
                "    \"version\": \"0.0.3\",\n" +
                "    \"date\":\"1574072773\"\n" +
                "}");

        walaPlugin.consume(coordinateJSON1.toString());

        var coordinate = new MavenCoordinate("com.zarbosoft", "coroutines-core", "0.0.3");
        RevisionCallGraph revisionCallGraph =
                PartialCallGraph.createExtendedRevisionCallGraph(coordinate, 1574072773);

        assertTrue(walaPlugin.produce().isPresent());
        assertEquals(revisionCallGraph.toJSON().toString(), walaPlugin.produce().get());
    }

    @Test
    public void testName() {
        assertEquals("eu.fasten.analyzer.javacgwala.WALAPlugin.WALA", walaPlugin.name());
    }

    @Test
    public void sendToKafkaTest() throws IOException, ClassHierarchyException, CallGraphBuilderCancelException {
        KafkaProducer<Object, String> producer = Mockito.mock(KafkaProducer.class);

        Mockito.when(producer.send(Mockito.any())).thenReturn(null);

        JSONObject coordinateJSON = new JSONObject("{\n" +
                "    \"groupId\": \"org.slf4j\",\n" +
                "    \"artifactId\": \"slf4j-api\",\n" +
                "    \"version\": \"1.7.29\",\n" +
                "    \"date\":\"1574072773\"\n" +
                "}");

        walaPlugin.consume(coordinateJSON.toString());

        var coordinate = new MavenCoordinate("org.slf4j", "slf4j-api", "1.7.29");
        var revisionCallGraph = PartialCallGraph.createExtendedRevisionCallGraph(coordinate,
                1574072773);

        assertTrue(walaPlugin.produce().isPresent());
        assertEquals(revisionCallGraph.toJSON().toString(), walaPlugin.produce().get());
    }

}