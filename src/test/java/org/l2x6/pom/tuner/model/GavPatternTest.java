/**
 * Copyright (c) 2015 Maven Utilities Project
 * project contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.l2x6.pom.tuner.model;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GavPatternTest {

    @Test
    void comparable() {
        Set<GavPattern> gavPatterns = new TreeSet<GavPattern>();
        gavPatterns.add(GavPattern.of("org.group2:artifact1"));
        gavPatterns.add(GavPattern.of("org.group1:artifact1"));
        gavPatterns.add(GavPattern.of("org.group1:artifact2"));
        gavPatterns.add(GavPattern.of("org.group1"));
        Assertions.assertEquals("org.group1,org.group1:artifact1,org.group1:artifact2,org.group2:artifact1",
                gavPatterns.stream().map(GavPattern::toString).collect(Collectors.joining(",")));
    }
}
