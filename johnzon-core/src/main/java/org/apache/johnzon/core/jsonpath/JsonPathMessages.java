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

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * Messages of JSONPath errors, from the resource bundle {@code org.apache.johnzon.core.jsonpath.messages}.
 */
final class JsonPathMessages {

    private static final ResourceBundle BUNDLE =
            ResourceBundle.getBundle("org.apache.johnzon.core.jsonpath.messages");

    /** Maximum number of query characters shown in a syntax error. */
    private static final int EXCERPT_LENGTH = 60;

    private JsonPathMessages() {
    }

    /** Returns the message with the given key; arguments are passed as strings, so numbers are not localized. */
    static String get(String key, Object... arguments) {
        return MessageFormat.format(BUNDLE.getString(key), arguments);
    }

    /**
     * Returns the message of a syntax error: the reason, the position and an excerpt of at most
     * {@value #EXCERPT_LENGTH} characters of the query around it, with a caret under the position, so that
     * long queries do not end up in logs in full.
     *
     * <pre>
     * Expected a query, a literal or a function at position 18:
     *   $.store[?@.price &lt;]
     *                     ^
     * </pre>
     */
    static String syntaxError(String reason, String query, int position) {
        return get("path.syntax.error", reason, String.valueOf(position), excerpt(query, position));
    }

    /** Two lines: the query around {@code position}, shortened with "...", and a caret under it. */
    private static String excerpt(String query, int position) {
        int start = 0;
        int end = query.length();
        if (end > EXCERPT_LENGTH) {
            start = Math.max(0, Math.min(position - EXCERPT_LENGTH / 2, end - EXCERPT_LENGTH));
            end = start + EXCERPT_LENGTH;
        }
        StringBuilder line = new StringBuilder("  ");
        if (start > 0) {
            line.append("...");
        }
        int caret = line.length() + (position - start);
        for (int i = start; i < end; i++) {
            char c = query.charAt(i);
            // Blanks such as newlines and tabs would break the alignment of the caret.
            line.append(c < 0x20 ? ' ' : c);
        }
        if (end < query.length()) {
            line.append("...");
        }
        char[] indent = new char[caret];
        Arrays.fill(indent, ' ');
        return line.append('\n').append(indent).append('^').toString();
    }
}
