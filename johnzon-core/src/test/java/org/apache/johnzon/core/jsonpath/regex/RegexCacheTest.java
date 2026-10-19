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

import static org.apache.johnzon.core.jsonpath.regex.IRegexpTest.repeat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The per-call cache of patterns that queries read from documents. */
class RegexCacheTest {

    @Test
    void returnsTheSameExpressionForTheSamePattern() {
        RegexCache cache = new RegexCache();

        assertSame(cache.get("a+").get(), cache.get("a+").get());
        assertFalse(cache.get("(").isPresent());
        assertEquals(2, cache.size());
    }

    @Test
    void boundsTheNumberOfExpressions() {
        RegexCache cache = new RegexCache();
        for (int n = 0; n < 1_000; n++) {
            assertTrue(cache.get("a{" + n + "}").get().matches(repeat("a", n)));
            assertTrue(cache.size() <= RegexCache.MAX_ENTRIES);
        }
    }

    @Test
    void boundsTheInstructionsOfTheExpressions() {
        RegexCache cache = new RegexCache();
        for (int n = 0; n < 60; n++) {
            assertTrue(cache.get("a{5000}x{0," + n + "}").isPresent());
            assertTrue(cache.instructions() <= RegexCache.MAX_INSTRUCTIONS);
        }
        assertTrue(cache.get("a{5000}").get().matches(repeat("a", 5000)));
    }

    @Test
    void returnsTheMemoryOfEvictedAutomata() {
        RegexCache cache = new RegexCache();
        IRegexp evicted = cache.get("(a|b)*a(a|b){8}").get();
        evicted.matches(repeat("ab", 50));
        long automaton = evicted.automatonBytes(false);
        int generation = IRegexp.dfaGeneration();
        long before = IRegexp.cachedDfaBytes();

        for (int n = 0; n < RegexCache.MAX_ENTRIES; n++) {
            cache.get("x{" + n + "}");
        }

        assertTrue(automaton > 0);
        assertEquals(0, evicted.automatonBytes(false));
        assertEquals(generation, IRegexp.dfaGeneration());
        assertEquals(before - automaton, IRegexp.cachedDfaBytes());
    }
}
