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

package eu.fasten.analyzer.javacgwala.data.callgraph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import eu.fasten.analyzer.javacgwala.core.FastenJavaURI;
import eu.fasten.analyzer.javacgwala.data.callgraph.analyzer.WalaResultAnalyzer;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Disabled until a way to make Wala platform independent found")
class WalaResultAnalyzerTest {

    private static CallGraph graph;

    @BeforeAll
    static void setUp() throws ClassHierarchyException, CallGraphBuilderCancelException, IOException {
        var path = new File(Thread.currentThread().getContextClassLoader()
                .getResource("SingleSourceToTarget.jar")
                .getFile()).getAbsolutePath();

        graph = CallGraphConstructor.generateCallGraph(path);
    }

    @Test
    void wrap() {
        var wrapped = WalaResultAnalyzer.wrap(graph);

        assertEquals(1, wrapped.getGraph().getInternalCalls().size());
        assertEquals(1, wrapped.getGraph().getExternalCalls().size());

        var source = "/name.space/SingleSourceToTarget.SingleSourceToTarget()%2Fjava.lang%2FVoidType";
        var target = "///java.lang/Object.Object()VoidType";

        var callMetadata = wrapped.getGraph().getExternalCalls().values().iterator().next();
        var callValues = wrapped.getGraph().getExternalCalls().keySet().iterator().next();

        var type = wrapped.getClassHierarchy()
                .get(new FastenJavaURI("/name.space/SingleSourceToTarget"));

        assertEquals(source, type.getMethods().get(callValues.getKey()).toString());
        assertEquals(target, callValues.getValue().toString());
        assertEquals("invokespecial", callMetadata.keySet().iterator().next());
        assertEquals("1", callMetadata.values().iterator().next());

        source = "/name.space/SingleSourceToTarget.sourceMethod()%2Fjava.lang%2FVoidType";
        target = "/name.space/SingleSourceToTarget.targetMethod()%2Fjava.lang%2FVoidType";

        var resolvedCall = wrapped.getGraph().getInternalCalls().get(0);

        assertEquals(source, type.getMethods().get(resolvedCall.get(0)).toString());
        assertEquals(target, type.getMethods().get(resolvedCall.get(1)).toString());
    }
}