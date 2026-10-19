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

import java.util.Optional;

import org.apache.johnzon.core.jsonpath.regex.IRegexp;
import org.apache.johnzon.core.jsonpath.regex.RegexCache;

/**
 * The compiled patterns of one {@code match()} or {@code search()} call of a query, in I-Regexp (RFC 9485),
 * matched in time linear in the length of the input. A literal pattern ({@code match(@.a, 'a.*')}) is
 * compiled once, when the query is parsed. Patterns read from documents ({@code match(@.value, @.pattern)})
 * are compiled on first use and kept in a bounded cache of the call, so patterns from the documents of one
 * query cannot evict those of another.
 */
final class Patterns {

    private final String literal;
    private final Optional<IRegexp> literalRegexp;
    private final RegexCache cache;

    /**
     * Creates the patterns of a call.
     *
     * @param literal the pattern if it is a string literal, otherwise {@code null}
     */
    Patterns(String literal) {
        this.literal = literal;
        this.literalRegexp = literal != null ? IRegexp.compile(literal) : Optional.<IRegexp>empty();
        this.cache = literal != null ? null : new RegexCache();
    }

    /** Returns the compiled pattern, or empty if {@code pattern} is not a valid I-Regexp. */
    Optional<IRegexp> get(String pattern) {
        if (cache == null) {
            // A literal pattern: the argument is always this literal.
            return pattern.equals(literal) ? literalRegexp : IRegexp.compile(pattern);
        }
        return cache.get(pattern);
    }
}
