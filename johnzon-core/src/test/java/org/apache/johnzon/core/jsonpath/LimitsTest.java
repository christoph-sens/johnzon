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

import static org.apache.johnzon.core.jsonpath.EvaluatorTest.json;
import static org.apache.johnzon.core.jsonpath.EvaluatorTest.nestedArrays;
import static org.apache.johnzon.core.jsonpath.EvaluatorTest.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonPath;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;

import org.apache.johnzon.core.JsonProviderImpl;
import org.junit.jupiter.api.Test;

/**
 * Tests of the evaluation limits: visited nodes, result size, depth, interruption.
 */
class LimitsTest {

    private static final JsonProvider PROVIDER = new JsonProviderImpl();

    private static Limits visits(long maxVisitedNodes) {
        return new Limits(maxVisitedNodes, Integer.MAX_VALUE, Limits.DEFAULT_MAX_DEPTH);
    }

    private static Limits depth(int maxDepth) {
        return new Limits(Limits.DEFAULT_MAX_VISITED_NODES, Integer.MAX_VALUE, maxDepth);
    }

    private static JsonArray numbers(int n) {
        JsonArrayBuilder numbers = PROVIDER.createArrayBuilder();
        for (int i = 0; i < n; i++) {
            numbers.add(i);
        }
        return numbers.build();
    }

    private static String error(String query, JsonValue document, Limits limits) {
        return assertThrows(JsonException.class, () -> query(query, document, limits)).getMessage();
    }

    @Test
    void failsWhenTooManyNodesAreVisited() {
        assertEquals(1_000, query("$[*]", numbers(1_000), visits(1_000)).size());
        assertEquals("Query visited more than 999 nodes, the limit", error("$[*]", numbers(1_000), visits(999)));
    }

    @Test
    void failsWhenTheResultIsTooLarge() {
        assertEquals(1_000, query("$[*]", numbers(1_000), new Limits(Long.MAX_VALUE, 1_000, 10)).size());
        assertEquals("Query selected more than 999 nodes, the limit",
                error("$[*]", numbers(1_000), new Limits(Long.MAX_VALUE, 999, 10)));
    }

    @Test
    void evaluatesAbsoluteQueriesInFiltersOncePerRun() {
        int n = 2_000;
        // Evaluating $[*] again for every element would visit n * n = 4,000,000 nodes.
        assertEquals(n, query("$[?count($[*]) > 0]", numbers(n), visits(3L * n)).size());
    }

    @Test
    void keepsNestedAbsoluteDescendantQueriesLinear() {
        int n = 500;
        JsonArrayBuilder doc = PROVIDER.createArrayBuilder();
        for (int i = 0; i < n; i++) {
            doc.add(PROVIDER.createObjectBuilder().add("v", PROVIDER.createArrayBuilder().add(i).add(i)));
        }
        // 4n nodes below the root; evaluating $..* again for each of them would visit about 32n^2 nodes.
        assertEquals(4 * n, query("$..[?count($..*) > 0]", doc.build(), visits(20L * n)).size());
    }

    @Test
    void walksDocumentsUpToTheDepthLimit() {
        assertEquals(1_000, query("$..*", nestedArrays(1_000)).size());
        assertEquals("Query argument is nested more than 1000 levels deep, the limit",
                error("$..*", nestedArrays(1_001), Limits.DEFAULT));
    }

    @Test
    void appliesTheDepthLimitToChildSegments() {
        JsonValue arrays = nestedArrays(4);
        JsonValue objects = json("{'a': {'b': {'c': 1}}}");

        // The children of nodes at depth 2 are beyond the limit, for arrays and objects alike.
        assertEquals(1, query("$[*][*]", arrays, depth(2)).size());
        error("$[*][*][*]", arrays, depth(2));
        assertEquals(1, query("$.*.*", objects, depth(2)).size());
        error("$.*.*[?@]", objects, depth(2));
    }

    @Test
    void comparesValuesUpToTheDepthLimit() {
        JsonValue doc = PROVIDER.createArrayBuilder()
                .add(PROVIDER.createObjectBuilder().add("a", nestedArrays(10)).add("b", nestedArrays(10)))
                .build();

        // The compared values are 10 arrays deep; the depth counts from them.
        assertEquals(1, query("$[?@.a == @.b]", doc, depth(10)).size());
        assertTrue(error("$[?@.a == @.b]", doc, depth(9)).contains("9 levels"));
    }

    @Test
    void stopsWhenTheThreadIsInterrupted() {
        JsonArray doc = numbers(10_000);
        Thread.currentThread().interrupt();
        try {
            assertEquals("Query evaluation was interrupted", error("$..*", doc, Limits.DEFAULT));
            assertTrue(Thread.currentThread().isInterrupted(), "interrupt status stays set");
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void reusesAbsoluteResultsOnlyWithinOneRun() {
        JsonPath path = JsonPathImpl.create("$.items[?@ == $.target]", PROVIDER, Limits.DEFAULT);

        assertEquals(PROVIDER.createValue(1), path.query(json("{'items': [1, 2, 3], 'target': 1}")).get(0).getValue());
        assertEquals(PROVIDER.createValue(2), path.query(json("{'items': [1, 2, 3], 'target': 2}")).get(0).getValue());
    }

    @Test
    void rejectsNegativeLimits() {
        assertThrows(IllegalArgumentException.class, () -> new Limits(-1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Limits(1, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Limits(1, 1, -1));
    }
}
