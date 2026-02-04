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
public class GaTest {
    @Test
    public void of() {
        Assertions.assertThat(new Ga("g", "a")).isEqualTo(Ga.of("g:a"));
    }

    @Test
    public void ofMissingArtifactId() {
        Assertions.assertThatThrownBy(() -> Ga.of("g")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void ofTooManySegments() {
        Assertions.assertThatThrownBy(() -> Ga.of("g:a:v")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void toStringTest() {
        Assertions.assertThat(new Ga("g", "a").toString()).isEqualTo("g:a");
    }

    @Test
    public void nonNull() {
        Assertions.assertThatThrownBy(() -> new Ga(null, "a")).isInstanceOf(NullPointerException.class);
        Assertions.assertThatThrownBy(() -> new Ga("g", null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void comparable() {
        Assertions.assertThat(new Ga("g", "a").compareTo(new Ga("g", "a"))).isEqualTo(0);

        Assertions.assertThat(new Ga("g", "a").compareTo(new Ga("g", "b"))).isEqualTo(-1);
        Assertions.assertThat(new Ga("g", "b").compareTo(new Ga("g", "a"))).isEqualTo(1);

        Assertions.assertThat(new Ga("g", "a").compareTo(new Ga("h", "a"))).isEqualTo(-1);
        Assertions.assertThat(new Ga("h", "a").compareTo(new Ga("g", "a"))).isEqualTo(1);
    }

    @Test
    void getRepositoryPath() {
        Assertions.assertThat(Ga.of("org.foo:bar").getRepositoryPath()).isEqualTo("org/foo/bar");
        Assertions.assertThat(Ga.of("foo:bar").getRepositoryPath()).isEqualTo("foo/bar");
    }

}
