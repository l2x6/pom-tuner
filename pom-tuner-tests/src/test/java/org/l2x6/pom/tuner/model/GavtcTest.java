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

import java.nio.file.Paths;
import java.util.Comparator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.l2x6.pom.tuner.model.Gavtc.Type;

public class GavtcTest {

    @Test
    void groupFirstComparator() {
        assertOrdering(OptionalWithDefault.rawValueComparator());
        assertOrdering(OptionalWithDefault.valueOrDefaultComparator());
    }

    public void assertOrdering(Comparator<OptionalWithDefault> c) {
        final Gavtc jffiNative = Gavtc.of("com.github.jnr:jffi:1.3.10:jar:native");
        final Gavtc jffi = Gavtc.of("com.github.jnr:jffi:1.3.10:jar");

        Assertions.assertThat(Gavtc.groupFirstComparator(c).compare(jffi, jffiNative)).isEqualTo(-1);
        Assertions.assertThat(Gavtc.groupFirstComparator(c).compare(jffiNative, jffi)).isEqualTo(1);
    }

    @Test
    public void of() {
        Assertions.assertThat(Gavtc.of("g:a:v")).isEqualTo(new Gavtc("g", "a", "v", Type.empty()));
        final OptionalWithDefault t = Type.of("t");
        Assertions.assertThat(Gavtc.of("g:a:v:t")).isEqualTo(new Gavtc("g", "a", "v", t, null));
        Assertions.assertThat(Gavtc.of("g:a:v:t:c")).isEqualTo(new Gavtc("g", "a", "v", t, "c"));
    }

    @Test
    public void ofPath() {
        Assertions.assertThat(
                Gavtc.of(
                        Paths.get("org/l2x6/pom-tuner/pom-tuner/5.0.0/pom-tuner-5.0.0.jar")))
                .isEqualTo(new Gavtc("org.l2x6.pom-tuner", "pom-tuner", "5.0.0", Type.of("jar"), null));
        Assertions.assertThat(
                Gavtc.of(
                        Paths.get("org/l2x6/pom-tuner/pom-tuner/5.0.0/pom-tuner-5.0.0-sources.jar")))
                .isEqualTo(new Gavtc("org.l2x6.pom-tuner", "pom-tuner", "5.0.0", Type.of("jar"), "sources"));
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
    public void toStringTest() {
        Assertions.assertThat(new Gavtc("g", "a", "v", Type.empty()).toString()).isEqualTo("g:a:v");
        Assertions.assertThat(new Gavtc("g", "a", null, Type.empty()).toString()).isEqualTo("g:a:");
        final OptionalWithDefault t = Type.of("t");
        Assertions.assertThat(new Gavtc("g", "a", "v", t, "c").toString()).isEqualTo("g:a:v:t:c");
    }

    @Test
    void getRepositoryPath() {
        Assertions.assertThat(Gavtc.of("org.foo:bar:1.2.3").getRepositoryPath()).isEqualTo("org/foo/bar/1.2.3/bar-1.2.3.jar");
        Assertions.assertThat(Gavtc.of("org.foo:bar:1.2.3:pom").getRepositoryPath())
                .isEqualTo("org/foo/bar/1.2.3/bar-1.2.3.pom");
        Assertions.assertThat(Gavtc.of("org.foo:bar:1.2.3:jar:javadoc").getRepositoryPath())
                .isEqualTo("org/foo/bar/1.2.3/bar-1.2.3-javadoc.jar");
    }
}
