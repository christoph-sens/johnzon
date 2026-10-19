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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonPath;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

import org.apache.johnzon.core.JsonProviderImpl;
import org.junit.jupiter.api.Test;

/**
 * Tests of JSONPath through the JSON-P API: {@link Json#createPath(String)}.
 */
public class JsonPathProviderTest {

    private static JsonValue json(String text) {
        try (JsonReader reader = Json.createReader(new StringReader(text.replace('\'', '"')))) {
            return reader.readValue();
        }
    }

    @Test
    void createsPathsThroughTheApi() {
        JsonValue doc = json("{'store': {'book': [{'title': 'A', 'price': 8.95}, {'title': 'B', 'price': 12.99}]}}");

        List<JsonPath.Node> nodes = Json.createPath("$.store.book[?@.price < 10].title").query(doc);

        assertEquals(1, nodes.size());
        assertEquals(Json.createValue("A"), nodes.get(0).getValue());
        assertEquals("$['store']['book'][0]['title']", nodes.get(0).getPath());
        assertEquals("/store/book/0/title", nodes.get(0).getPointer().toString());
        assertEquals(Json.createValue("A"), nodes.get(0).getPointer().getValue(doc.asJsonObject()));
    }

    @Test
    void keepsTheQueryString() {
        assertEquals("$..a", Json.createPath("$..a").toString());
    }

    @Test
    void reportsSyntaxErrorsAsJsonException() {
        JsonException e = assertThrows(JsonException.class, () -> Json.createPath("$.a[?@.b <]"));

        assertTrue(e.getMessage().startsWith("Expected a query, a literal or a function at position 10:"),
                e.getMessage());
    }

    @Test
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> Json.createPath(null));
        assertThrows(NullPointerException.class, () -> Json.createPath("$").query(null));
    }

    @Test
    void readsTheVisitLimitFromASystemProperty() {
        JsonArrayBuilder numbers = Json.createArrayBuilder();
        for (int i = 0; i < 10; i++) {
            numbers.add(i);
        }
        JsonValue doc = numbers.build();
        String property = "org.apache.johnzon.jsonpath.max-visited-nodes";
        System.setProperty(property, "9");
        try {
            JsonPath path = new JsonProviderImpl().createPath("$[*]");

            assertEquals("Query visited more than 9 nodes, the limit",
                    assertThrows(JsonException.class, () -> path.query(doc)).getMessage());
        } finally {
            System.clearProperty(property);
        }
        assertEquals(10, new JsonProviderImpl().createPath("$[*]").query(doc).size());
    }

    @Test
    void readsTheResultLimitFromASystemProperty() {
        String property = "org.apache.johnzon.jsonpath.max-result-size";
        System.setProperty(property, "1");
        try {
            JsonPath path = new JsonProviderImpl().createPath("$[*]");

            assertEquals(1, path.query(json("[1]")).size());
            assertThrows(JsonException.class, () -> path.query(json("[1, 2]")));
        } finally {
            System.clearProperty(property);
        }
    }

    @Test
    void usesTheMaximumDepthOfJohnzon() {
        JsonValue deep = JsonValue.EMPTY_JSON_ARRAY;
        for (int i = 0; i < 20; i++) {
            deep = Json.createArrayBuilder().add(deep).build();
        }
        System.setProperty("org.apache.johnzon.jsonpath.max-depth", "10");
        try {
            JsonPath path = new JsonProviderImpl().createPath("$..*");
            JsonValue doc = deep;

            assertTrue(assertThrows(JsonException.class, () -> path.query(doc)).getMessage().contains("10 levels"));
        } finally {
            System.clearProperty("org.apache.johnzon.jsonpath.max-depth");
        }
        assertEquals(20, new JsonProviderImpl().createPath("$..*").query(deep).size());
    }
}
