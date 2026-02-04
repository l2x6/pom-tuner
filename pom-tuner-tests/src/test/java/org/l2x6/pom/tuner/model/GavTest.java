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

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class GavTest {
    @Test
    public void of() {
        Assertions.assertThat(new Gav("g", "a", "v")).isEqualTo(Gav.of("g:a:v"));
    }

    @Test
    public void ofMissingArtifactId() {
        Assertions.assertThatThrownBy(() -> Gav.of("g")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void ofMissingVersion() {
        Assertions.assertThatThrownBy(() -> Gav.of("g:a")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void ofTooManySegments() {
        Assertions.assertThatThrownBy(() -> Gav.of("g:a:v:t")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void toStringTest() {
        Assertions.assertThat(new Gav("g", "a", "v").toString()).isEqualTo("g:a:v");
        Assertions.assertThat(new Gav("g", "a", null).toString()).isEqualTo("g:a:");
    }

    static final Gavtcs jffiNative = Gavtcs.of("com.github.jnr:jffi:1.3.10:jar:native");
    static final Gavtcs jffi = Gavtcs.of("com.github.jnr:jffi:1.3.10:jar");

    @Test
    void groupFirstComparator() {
        Assertions.assertThat(Gavtcs.groupFirstComparator().compare(jffi, jffiNative)).isEqualTo(-1);
        Assertions.assertThat(Gavtcs.groupFirstComparator().compare(jffiNative, jffi)).isEqualTo(1);
    }

    @Test
    public void comparable() {
        Assertions.assertThat(new Gav("g", "a", "1").compareTo(new Gav("g", "a", "1"))).isEqualTo(0);
        Assertions.assertThat(new Gav("g", "a", "1").compareTo(new Gav("g", "a", "2"))).isEqualTo(-1);
        Assertions.assertThat(new Gav("g", "a", "2").compareTo(new Gav("g", "a", "1"))).isEqualTo(1);

        Assertions.assertThat(new Gav("g", "a", "1").compareTo(new Gav("g", "a", null))).isEqualTo(1);
        Assertions.assertThat(new Gav("g", "a", null).compareTo(new Gav("g", "a", "1"))).isEqualTo(-1);
        Assertions.assertThat(new Gav("g", "a", "").compareTo(new Gav("g", "a", null))).isEqualTo(0);
        Assertions.assertThat(new Gav("g", "a", null).compareTo(new Gav("g", "a", null))).isEqualTo(0);

        Assertions.assertThat(new Gav("g", "a", "1").compareTo(new Gav("g", "b", "1"))).isEqualTo(-1);
        Assertions.assertThat(new Gav("g", "b", "1").compareTo(new Gav("g", "a", "1"))).isEqualTo(1);

        Assertions.assertThat(new Gav("g", "a", "1").compareTo(new Gav("h", "a", "1"))).isEqualTo(-1);
        Assertions.assertThat(new Gav("h", "a", "1").compareTo(new Gav("g", "a", "1"))).isEqualTo(1);

    }

    @Test
    void getRepositoryPath() {
        Assertions.assertThat(Gav.of("org.foo:bar:1.2.3").getRepositoryPath()).isEqualTo("org/foo/bar/1.2.3");
        Assertions.assertThat(Gav.of("foo:bar:1.2.3").getRepositoryPath()).isEqualTo("foo/bar/1.2.3");
    }

}
