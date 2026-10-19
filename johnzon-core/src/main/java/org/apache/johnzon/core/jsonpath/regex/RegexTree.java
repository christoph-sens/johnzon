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

import java.util.List;

/**
 * Syntax tree of an I-Regexp (RFC 9485), as {@link RegexParser} builds it. The node types are the nested
 * classes; the private constructor keeps the hierarchy closed.
 */
abstract class RegexTree {

    private RegexTree() {
    }

    static final class Alternation extends RegexTree {
        final List<RegexTree> branches;

        Alternation(List<RegexTree> branches) {
            this.branches = branches;
        }
    }

    static final class Sequence extends RegexTree {
        final List<RegexTree> pieces;

        Sequence(List<RegexTree> pieces) {
            this.pieces = pieces;
        }
    }

    static final class Repeat extends RegexTree {
        final RegexTree atom;
        final int min;
        /** -1 for "unbounded". */
        final int max;

        Repeat(RegexTree atom, int min, int max) {
            this.atom = atom;
            this.min = min;
            this.max = max;
        }
    }

    static final class Chars extends RegexTree {
        final CharSet set;

        Chars(CharSet set) {
            this.set = set;
        }
    }

    static final class Begin extends RegexTree {
    }

    static final class End extends RegexTree {
    }
}
