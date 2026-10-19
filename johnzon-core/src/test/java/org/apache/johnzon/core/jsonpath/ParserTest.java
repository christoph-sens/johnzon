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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import jakarta.json.JsonException;
import jakarta.json.JsonNumber;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;

import org.apache.johnzon.core.JsonProviderImpl;
import org.apache.johnzon.core.jsonpath.Ast.Comparison;
import org.apache.johnzon.core.jsonpath.Ast.Filter;
import org.apache.johnzon.core.jsonpath.Ast.Literal;
import org.apache.johnzon.core.jsonpath.Ast.Query;
import org.junit.jupiter.api.Test;

/**
 * Tests of the parser beyond the Compliance Test Suite: error messages and positions, limits, literals.
 */
class ParserTest {

    private static final JsonProvider PROVIDER = new JsonProviderImpl();

    private static Query parse(String query) {
        return Parser.parse(query, PROVIDER);
    }

    private static String error(String query) {
        return assertThrows(JsonException.class, () -> parse(query)).getMessage();
    }

    private static int position(String query) {
        String message = error(query);
        String marker = " at position ";
        int start = message.indexOf(marker) + marker.length();
        return Integer.parseInt(message.substring(start, message.indexOf(':', start)));
    }

    private static String repeat(String s, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    private static JsonValue rightLiteral(String query) {
        Filter filter = (Filter) parse(query).segments.get(0).selectors.get(0);
        return ((Literal) ((Comparison) filter.expr).right).value;
    }

    @Test
    void showsAnExcerptWithACaretInSyntaxErrors() {
        assertEquals("Expected a query, a literal or a function at position 18:\n"
                        + "  $.store[?@.price <]\n"
                        + "                    ^",
                error("$.store[?@.price <]"));
    }

    @Test
    void shortensLongQueriesInSyntaxErrors() {
        String query = "$." + repeat("a", 200) + "[?@.x <]" + repeat(".b", 100);

        String[] lines = error(query).split("\n");

        assertEquals(3, lines.length);
        assertEquals("Expected a query, a literal or a function at position 209:", lines[0]);
        assertTrue(lines[1].startsWith("  ...") && lines[1].endsWith("..."), lines[1]);
        assertEquals(2 + 3 + 60 + 3, lines[1].length());
        assertEquals(']', lines[1].charAt(lines[2].indexOf('^')));
    }

    @Test
    void keepsTheCaretAlignedWhenTheQueryContainsLineBreaks() {
        String[] lines = error("$[?@.a ==\n\t]").split("\n");

        assertEquals("  $[?@.a ==  ]", lines[1]);
        assertEquals(2 + 11, lines[2].indexOf('^'));
    }

    @Test
    void pointsTypeErrorsAtTheOperand() {
        assertEquals(3, position("$[?length(@.a) && @.b]"));
        assertEquals(11, position("$[?@.b || !length(@.a)]"));
    }

    @Test
    void rejectsDeeplyNestedExpressions() {
        String nested = repeat("(", 1000) + "@.a" + repeat(")", 1000);

        assertTrue(error("$[?" + nested + "]").contains("nested more than 64 levels"));
    }

    @Test
    void acceptsNestingUpToTheLimit() {
        // The filter itself is the first level.
        String nested = repeat("(", Parser.MAX_NESTING - 1) + "@.a" + repeat(")", Parser.MAX_NESTING - 1);

        parse("$[?" + nested + "]");
    }

    @Test
    void rejectsJaywayOnlySyntax() {
        // Jayway JsonPath accepts these; RFC 9535 does not.
        error("$.book[?(@.price < 10)].length()");
        error("$.book[?@.tags in ['a']]");
        error("$.book[?@.title =~ /a.*/]");
    }

    @Test
    void namesUnknownFunctions() {
        assertTrue(error("$[?foo(@.a)]").startsWith("Unknown function 'foo' at position 3:"));
        assertTrue(error("$[?length(@.a, @.b) == 1]").startsWith("Function 'length' expects 1 argument(s)"));
    }

    @Test
    void limitsIntegersToTheIJsonRange() {
        parse("$[9007199254740991]");
        parse("$[-9007199254740991]");
        parse("$[-9007199254740991:9007199254740991:9007199254740991]");
        assertTrue(error("$[9007199254740992]").startsWith("Integer out of range at position 2:"));
        error("$[-9007199254740992]");
        error("$[99999999999999999999999]");
        error("$[0:1:9007199254740992]");
    }

    @Test
    void parsesNumberLiteralsExactly() {
        JsonValue value = rightLiteral("$[?@ == 0.1]");

        assertEquals(new BigDecimal("0.1"), ((JsonNumber) value).bigDecimalValue());
        assertEquals(new BigDecimal("1E+400"), ((JsonNumber) rightLiteral("$[?@ == 1e400]")).bigDecimalValue());
        assertEquals(new BigDecimal("-12.5E-7"), ((JsonNumber) rightLiteral("$[?@ == -12.5E-7]")).bigDecimalValue());
    }

    @Test
    void rejectsNumbersBeyondTheRangeOfBigDecimal() {
        assertTrue(error("$[?@ == 1e99999999999]").startsWith("Number out of range"));
    }

    @Test
    void parsesOtherLiterals() {
        assertEquals("a'bé😀", ((JsonString) rightLiteral("$[?@ == 'a\\'b\\u00e9\\ud83d\\ude00']")).getString());
        assertSame(JsonValue.TRUE, rightLiteral("$[?@ == true]"));
        assertSame(JsonValue.FALSE, rightLiteral("$[?@ == false]"));
        assertSame(JsonValue.NULL, rightLiteral("$[?@ == null]"));
    }

    @Test
    void knowsSingularQueries() {
        assertTrue(parse("$.a['b'][0]").isSingular());
        assertTrue(parse("$").isSingular());
        assertFalse(parse("$.a[*]").isSingular());
        assertFalse(parse("$..a").isSingular());
        assertFalse(parse("$[0,1]").isSingular());
        assertFalse(parse("$[0:1]").isSingular());
    }

    @Test
    void rejectsQueriesThatDoNotStartWithTheRoot() {
        assertTrue(error("").startsWith("Query must start with '$' at position 0:"));
        error("@.a");
        error(" $.a");
        error("$.a ");
    }
}
