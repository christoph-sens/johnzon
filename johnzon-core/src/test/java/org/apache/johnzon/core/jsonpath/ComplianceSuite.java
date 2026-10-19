/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.johnzon.core.jsonpath;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import org.apache.johnzon.core.JsonProviderImpl;

/**
 * The cases of the JSONPath Compliance Test Suite
 * (<a href="https://github.com/jsonpath-standard/jsonpath-compliance-test-suite">jsonpath-compliance-test-suite</a>).
 * The build downloads {@code cts.json} at a fixed commit and passes its location in the system property
 * {@code jsonpath.cts}; see {@code impl/pom.xml}.
 */
final class ComplianceSuite {

    /** One case of the suite. */
    static final class Case {

        final String name;
        final String selector;
        final boolean invalidSelector;
        /** The document, or {@code null} for an invalid selector. */
        final JsonValue document;
        /** The values the selector selects: one list, or several alternatives where member order is not defined. */
        final List<JsonArray> results;
        /** The normalized paths of the selected nodes, one list per alternative; empty if the case gives none. */
        final List<List<String>> resultPaths;

        Case(JsonObject test) {
            this.name = test.getString("name");
            this.selector = test.getString("selector");
            this.invalidSelector = test.getBoolean("invalid_selector", false);
            this.document = test.get("document");
            List<JsonArray> results = new ArrayList<>();
            List<List<String>> paths = new ArrayList<>();
            if (test.containsKey("result")) {
                results.add(test.getJsonArray("result"));
                if (test.containsKey("result_paths")) {
                    paths.add(strings(test.getJsonArray("result_paths")));
                }
            } else if (test.containsKey("results")) {
                results.addAll(test.getJsonArray("results").getValuesAs(JsonArray.class));
                if (test.containsKey("results_paths")) {
                    for (JsonArray alternative : test.getJsonArray("results_paths").getValuesAs(JsonArray.class)) {
                        paths.add(strings(alternative));
                    }
                }
            }
            this.results = Collections.unmodifiableList(results);
            this.resultPaths = Collections.unmodifiableList(paths);
        }

        @Override
        public String toString() {
            return name + " | " + selector;
        }
    }

    private static List<Case> cases;

    private ComplianceSuite() {
    }

    /** Returns the cases of the suite, in the order of the suite. */
    static synchronized List<Case> cases() {
        if (cases == null) {
            List<Case> list = new ArrayList<>();
            for (JsonObject test : read().getJsonArray("tests").getValuesAs(JsonObject.class)) {
                list.add(new Case(test));
            }
            cases = Collections.unmodifiableList(list);
        }
        return cases;
    }

    private static JsonObject read() {
        String location = System.getProperty("jsonpath.cts", "target/jsonpath-cts/cts.json");
        Path file = Paths.get(location);
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("JSONPath Compliance Test Suite not found at " + file.toAbsolutePath()
                    + "; the Maven build downloads it in the phase generate-test-resources");
        }
        try (InputStream in = Files.newInputStream(file);
             JsonReader reader = new JsonProviderImpl().createReader(in)) {
            return reader.readObject();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + file, e);
        }
    }

    private static List<String> strings(JsonArray array) {
        List<String> strings = new ArrayList<>();
        for (JsonString s : array.getValuesAs(JsonString.class)) {
            strings.add(s.getString());
        }
        return strings;
    }
}
