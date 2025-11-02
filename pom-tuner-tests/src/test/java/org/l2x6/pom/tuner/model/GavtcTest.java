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

public class GavtcTest {
    static final Gavtc jffiNative = Gavtc.of("com.github.jnr:jffi:1.3.10:jar:native");
    static final Gavtc jffi = Gavtc.of("com.github.jnr:jffi:1.3.10:jar");

    @Test
    public void of() {
        Assertions.assertThat(Gavtc.of("g:a:v")).isEqualTo(new Gavtc("g", "a", "v"));
        Assertions.assertThat(Gavtc.of("g:a:v:t")).isEqualTo(new Gavtc("g", "a", "v", "t", null));
        Assertions.assertThat(Gavtc.of("g:a:v:t:c")).isEqualTo(new Gavtc("g", "a", "v", "t", "c"));
    }

    @Test
    public void ofTooLittleSegments() {
        Assertions.assertThatThrownBy(() -> Gav.of("g:a")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void ofTooManySegments() {
        Assertions.assertThatThrownBy(() -> Gav.of("g:a:v:t:c:s")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void groupFirstComparator() {
        Assertions.assertThat(Gavtc.groupFirstComparator().compare(jffi, jffiNative)).isEqualTo(-1);
        Assertions.assertThat(Gavtc.groupFirstComparator().compare(jffiNative, jffi)).isEqualTo(1);
    }

    @Test
    public void toStringTest() {
        Assertions.assertThat(new Gavtc("g", "a", "v").toString()).isEqualTo("g:a:v");
        Assertions.assertThat(new Gavtc("g", "a", null).toString()).isEqualTo("g:a:");
        Assertions.assertThat(new Gavtc("g", "a", "v", "t", "c").toString()).isEqualTo("g:a:v:t:c");
    }

}
