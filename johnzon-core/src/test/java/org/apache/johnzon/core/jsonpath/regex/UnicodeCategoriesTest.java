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

package org.apache.johnzon.core.jsonpath.regex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;

/**
 * Unicode categories from a fixed Unicode version. CI runs these tests on Java 11 (Unicode 10.0), 17 (13.0),
 * 21 (15.0) and 25 (16.0).
 */
class UnicodeCategoriesTest {

    @Test
    void usesUnicode16() {
        assertEquals("16.0.0", UnicodeCategoryData.VERSION);
    }

    @Test
    void agreesWithJava25ForEveryCodePoint() {
        // Java 25 implements Unicode 16.0, the version of the tables.
        assumeTrue("25".equals(System.getProperty("java.specification.version")));
        for (int cp = 0; cp <= Character.MAX_CODE_POINT; cp++) {
            if (UnicodeCategories.type(cp) != Character.getType(cp)) {
                assertEquals(Character.getType(cp), UnicodeCategories.type(cp), String.format("U+%04X", cp));
            }
        }
    }

    @Test
    void knowsCodePointsAssignedAfterUnicode13OnEveryJdk() {
        // Unicode 14.0, 15.0 and 16.0; unassigned in Java 17's Unicode 13.0.
        assertMatches("\\p{Lo}", 0x0870);
        assertMatches("\\p{L}", 0x1E4D0);
        assertMatches("\\p{Lu}", 0x1C89);
        assertMatches("\\p{Ll}", 0x1C8A);
        assertMatches("\\p{So}", 0x1CC00);
        assertMatches("\\P{Cn}", 0x1C89);
        // Within a range of UnicodeData.txt (CJK Ideograph Extension H, Unicode 15.0).
        assertMatches("\\p{Lo}", 0x31350 + 100);
    }

    @Test
    void classifiesUnassignedSurrogateAndPrivateUseCodePoints() {
        assertEquals(Character.UNASSIGNED, UnicodeCategories.type(0x2FFFF));
        assertEquals(Character.PRIVATE_USE, UnicodeCategories.type(0x10FFFD));
        assertEquals(Character.UNASSIGNED, UnicodeCategories.type(Character.MAX_CODE_POINT));
        assertEquals(Character.SURROGATE, UnicodeCategories.type(0xD800));
        assertEquals(Character.PRIVATE_USE, UnicodeCategories.type(0xE000));
        assertEquals(Character.UPPERCASE_LETTER, UnicodeCategories.type('A'));
        assertEquals(Character.LOWERCASE_LETTER, UnicodeCategories.type(0xFF));
        assertEquals(Character.UPPERCASE_LETTER, UnicodeCategories.type(0x100));
    }

    private static void assertMatches(String regexp, int cp) {
        assertTrue(IRegexp.compile(regexp).get().matches(new String(Character.toChars(cp))),
                String.format("%s on U+%04X", regexp, cp));
    }
}
