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

/**
 * Limits for running a query, to bound the work spent on queries and documents from untrusted sources
 * (RFC 9535, section 4.1). A query that exceeds a limit fails with a {@link jakarta.json.JsonException}.
 *
 * <ul>
 *     <li>{@code maxVisitedNodes}: node visits per run; each node a selector produces, each node a descendant
 *     segment walks through and each node a filter tests counts, so the count is roughly proportional to the
 *     time a query takes.</li>
 *     <li>{@code maxResultSize}: nodes in a result.</li>
 *     <li>{@code maxDepth}: how deep a query walks into the query argument and how deep {@code ==} compares
 *     values; the root has depth 0, its children depth 1.</li>
 * </ul>
 */
final class Limits {

    /** Default maximum number of node visits per run, which bounds a query to a few seconds of work. */
    static final long DEFAULT_MAX_VISITED_NODES = 100_000_000L;

    /** Default maximum number of nodes in a result: unlimited. */
    static final int DEFAULT_MAX_RESULT_SIZE = Integer.MAX_VALUE;

    /** Default maximum nesting depth, the default of Johnzon's parser. */
    static final int DEFAULT_MAX_DEPTH = 1000;

    static final Limits DEFAULT = new Limits(DEFAULT_MAX_VISITED_NODES, DEFAULT_MAX_RESULT_SIZE, DEFAULT_MAX_DEPTH);

    final long maxVisitedNodes;
    final int maxResultSize;
    final int maxDepth;

    /**
     * Creates limits.
     *
     * @throws IllegalArgumentException if a limit is negative
     */
    Limits(long maxVisitedNodes, int maxResultSize, int maxDepth) {
        if (maxVisitedNodes < 0 || maxResultSize < 0 || maxDepth < 0) {
            throw new IllegalArgumentException("Limits must not be negative: maxVisitedNodes=" + maxVisitedNodes
                    + ", maxResultSize=" + maxResultSize + ", maxDepth=" + maxDepth);
        }
        this.maxVisitedNodes = maxVisitedNodes;
        this.maxResultSize = maxResultSize;
        this.maxDepth = maxDepth;
    }
}
