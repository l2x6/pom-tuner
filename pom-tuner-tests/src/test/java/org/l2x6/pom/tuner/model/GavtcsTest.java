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

import java.util.Comparator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class GavtcsTest {
    @Test
    void groupFirstComparator() {
        assertOrdering(OptionalWithDefault.rawValueComparator());
        assertOrdering(OptionalWithDefault.valueOrDefaultComparator());
    }

    public void assertOrdering(Comparator<OptionalWithDefault> c) {
        final Gavtcs jffiNative = Gavtcs.of("com.github.jnr:jffi:1.3.10:jar:native");
        final Gavtcs jffi = Gavtcs.of("com.github.jnr:jffi:1.3.10:jar");

        Assertions.assertThat(Gavtcs.groupFirstComparator(c).compare(jffi, jffiNative)).isEqualTo(-1);
        Assertions.assertThat(Gavtcs.groupFirstComparator(c).compare(jffiNative, jffi)).isEqualTo(1);
    }
}
