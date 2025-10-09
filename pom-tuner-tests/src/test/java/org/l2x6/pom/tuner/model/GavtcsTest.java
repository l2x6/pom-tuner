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

public class GavtcsTest {
    static final Gavtcs jffiNative = Gavtcs.of("com.github.jnr:jffi:1.3.10:jar:native");
    static final Gavtcs jffi = Gavtcs.of("com.github.jnr:jffi:1.3.10:jar");

    @Test
    void groupFirstComparator() {
        Assertions.assertThat(Gavtcs.groupFirstComparator().compare(jffi, jffiNative)).isEqualTo(-1);
        Assertions.assertThat(Gavtcs.groupFirstComparator().compare(jffiNative, jffi)).isEqualTo(1);
    }
}
