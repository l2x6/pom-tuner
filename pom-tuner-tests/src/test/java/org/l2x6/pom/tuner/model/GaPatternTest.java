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

import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.assertj.core.util.TriFunction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GaPatternTest {

    @Test
    void comparable() {
        comparable(() -> new TreeSet<GaPattern>(), GaPattern::of);
    }

    static <T> void comparable(Supplier<Set<T>> setSupplier, Function<String, T> ofFunction) {
        Set<T> gaPatterns = setSupplier.get();
        gaPatterns.add(ofFunction.apply("org.group2:artifact1"));
        gaPatterns.add(ofFunction.apply("org.group1:artifact1"));
        gaPatterns.add(ofFunction.apply("org.group1:artifact2"));
        gaPatterns.add(ofFunction.apply("org.group1"));
        gaPatterns.add(ofFunction.apply("*"));
        assertThat("*,org.group1,org.group1:artifact1,org.group1:artifact2,org.group2:artifact1").isEqualTo(
                gaPatterns.stream().map(Object::toString).collect(Collectors.joining(",")));
    }

    @Test
    void asWildcardGa() {
        asWildcardGa(s -> GaPattern.of(s).asWildcardGa());
    }

    static <T> void asWildcardGa(Function<String, Ga> as) {
        assertThat(Ga.of("org.group2:artifact1")).isEqualTo(as.apply("org.group2:artifact1"));
        assertThat(Ga.of("org.group2:*")).isEqualTo(as.apply("org.group2"));
        assertThat(Ga.of("*:*")).isEqualTo(as.apply("*"));
    }

    @Test
    void matchesGa() {
        assertMatchesGa(
                GaPattern::of,
                (pat, ga) -> pat.matches(ga),
                (pat, g, a) -> pat.matches(g, a));
    }

    static <T> void assertMatchesGa(
            Function<String, T> of,
            BiFunction<T, Ga, Boolean> matches1,
            TriFunction<T, String, String, Boolean> matches2) {
        assertMatchesGa(of, matches1, matches2, "*", "a:b", true);
        assertMatchesGa(of, matches1, matches2, "*:*", "a:b", true);
        assertMatchesGa(of, matches1, matches2, "a", "a:b", true);
        assertMatchesGa(of, matches1, matches2, "a:b", "a:b", true);
        assertMatchesGa(of, matches1, matches2, "*:b", "a:b", true);
        assertMatchesGa(of, matches1, matches2, "a:*", "a:b", true);

        assertMatchesGa(of, matches1, matches2, "b", "a:b", false);
        assertMatchesGa(of, matches1, matches2, "b:b", "a:b", false);
        assertMatchesGa(of, matches1, matches2, "*:a", "a:b", false);
        assertMatchesGa(of, matches1, matches2, "b:*", "a:b", false);
    }

    static <T> void assertMatchesGa(
            Function<String, T> of,
            BiFunction<T, Ga, Boolean> matches1,
            TriFunction<T, String, String, Boolean> matches2,
            String pattern,
            String probe,
            boolean expected) {
        final T pat = of.apply(pattern);
        final Ga ga = Ga.of(probe);
        assertThat(matches1.apply(pat, ga)).isEqualTo(expected);
        assertThat(matches2.apply(pat, ga.getGroupId(), ga.getArtifactId())).isEqualTo(expected);
    }
}
