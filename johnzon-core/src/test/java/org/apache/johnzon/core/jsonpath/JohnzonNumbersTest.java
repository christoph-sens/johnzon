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

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

import org.junit.jupiter.api.Test;

/**
 * Filters over the number implementations of Johnzon: longs ({@code JsonLongImpl}), doubles
 * ({@code JsonDoubleImpl}, built with {@code add(double)}) and decimals ({@code JsonNumberImpl}). All of them
 * compare by exact value, like the numbers a document parsed from text has.
 */
class JohnzonNumbersTest {

    private static int count(JsonValue document, String query) {
        return Json.createPath(query).query(document).size();
    }

    private static JsonArray parse(String text) {
        try (JsonReader reader = Json.createReader(new StringReader(text))) {
            return reader.readArray();
        }
    }

    @Test
    void equalsAcrossNumberTypes() {
        JsonArray doc = Json.createArrayBuilder()
                .add(1).add(1L).add(1.0d).add(new BigDecimal("1.00")).add(new BigInteger("1"))
                .add(2).build();

        assertEquals(5, count(doc, "$[?@ == 1]"));
        assertEquals(5, count(doc, "$[?@ == 1.0]"));
        assertEquals(5, count(doc, "$[?@ == 1e0]"));
        assertEquals(1, count(doc, "$[?@ == 2]"));
    }

    @Test
    void doublesAreComparedByTheirDecimalValue() {
        // 0.1d is not 1/10 in binary, but it is the number 0.1 as JSON text
        JsonArray doc = Json.createArrayBuilder().add(0.1d).add(0.3d).add(0.1d + 0.2d).build();

        assertEquals(1, count(doc, "$[?@ == 0.1]"));
        assertEquals(1, count(doc, "$[?@ == 0.3]"));
        assertEquals(1, count(doc, "$[?@ > 0.30000000000000001]"));
        assertEquals(parse("[0.1, 0.3, 0.30000000000000004]"), doc);
    }

    @Test
    void longsBeyondTheExactRangeOfDoublesStayExact() {
        long big = 9_007_199_254_740_993L; // 2^53 + 1, not representable as double
        JsonArray doc = Json.createArrayBuilder().add(big).add(big - 1).add(Long.MAX_VALUE).add(Long.MIN_VALUE).build();

        assertEquals(1, count(doc, "$[?@ == 9007199254740993]"));
        assertEquals(1, count(doc, "$[?@ == 9007199254740992]"));
        assertEquals(2, count(doc, "$[?@ >= 9007199254740993]"));
        assertEquals(1, count(doc, "$[?@ == 9223372036854775807]"));
        assertEquals(1, count(doc, "$[?@ < -9223372036854775807]"));
    }

    @Test
    void parsedNumbersOfAllSizes() {
        JsonArray doc = parse("[0, -0, 0.0, 1e2, 100, 1E+2, 12345678901234567890, 1.5e300, -2.5e-300]");

        assertEquals(3, count(doc, "$[?@ == 0]"));
        assertEquals(3, count(doc, "$[?@ == 100]"));
        assertEquals(1, count(doc, "$[?@ == 12345678901234567890]"));
        assertEquals(1, count(doc, "$[?@ > 1e300 && @ < 2e300]"));
        assertEquals(1, count(doc, "$[?@ < 0 && @ > -1e-299]"));
    }

    @Test
    void comparisonsWithOtherTypesAreFalse() {
        JsonArray doc = Json.createArrayBuilder().add(1).add("1").add(true).addNull().build();

        assertEquals(1, count(doc, "$[?@ == 1]"));
        assertEquals(0, count(doc, "$[?@ < 'a' && @ == 1 && @ == '1']"));
        assertEquals(1, count(doc, "$[?@ <= 1 && @ >= 1]"));
    }
}
