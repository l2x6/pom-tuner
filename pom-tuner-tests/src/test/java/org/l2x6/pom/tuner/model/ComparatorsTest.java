/*
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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.l2x6.pom.tuner.Comparators;

public class ComparatorsTest {
    @Test
    void t() {
        Assertions.assertThat(Comparators.safeStringComparator().compare(null, null)).isEqualTo(0);
        Assertions.assertThat(Comparators.safeStringComparator().compare(null, "")).isEqualTo(-1);
        Assertions.assertThat(Comparators.safeStringComparator().compare("", null)).isEqualTo(1);
        Assertions.assertThat(Comparators.safeStringComparator().compare("a", "b")).isEqualTo(-1);
        Assertions.assertThat(Comparators.safeStringComparator().compare("a", "a")).isEqualTo(0);
        Assertions.assertThat(Comparators.safeStringComparator().compare("b", "a")).isEqualTo(1);
    }
}
