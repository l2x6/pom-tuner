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

import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GavtcsPatternTest {

    @Test
    void comparable() {
        GavPatternTest.comparable(() -> new TreeSet<GavtcsPattern>(), GavtcsPattern::of);
    }

    @Test
    void asWildcardGa() {
        GavPatternTest.asWildcardGa(s -> GavtcsPattern.of(s).asWildcardGa());
    }

    @Test
    void matchesGa() {
        GavPatternTest.assertMatchesGa(
                GavtcsPattern::of,
                (pat, ga) -> pat.matches(ga),
                (pat, g, a) -> pat.matches(g, a));
    }

    @Test
    void matchesGav() {
        GavPatternTest.assertMatchesGav(
                GavtcsPattern::of,
                (pat, ga) -> pat.matches(ga),
                (pat, gav) -> pat.matches(gav[0], gav[1], gav[2]));
    }

    @Test
    void matchesGavtc() {
        assertMatchesGavtc(
                GavtcsPattern::of,
                (pat, ga) -> pat.matches(ga),
                (pat, gav) -> pat.matches(gav[0], gav[1], gav[2], gav[3], gav[4]));
    }

    @Test
    void matchesGavtcs() {
        assertMatchesGavtcs(
                GavtcsPattern::of,
                (pat, ga) -> pat.matches(ga),
                (pat, gav) -> pat.matches(gav[0], gav[1], gav[2], gav[3], gav[4], gav[5]));
    }

    static <T> void assertMatchesGavtc(
            Function<String, T> of,
            BiFunction<T, Gavtc, Boolean> matches1,
            BiFunction<T, String[], Boolean> matches2) {
        assertMatchesGavtc(of, matches1, matches2, "*", "g:a:v:t:c", true);
        assertMatchesGavtc(of, matches1, matches2, "*:*", "g:a:v:t:c", true);
        assertMatchesGavtc(of, matches1, matches2, "*:*:*", "g:a:v:t:c", true);
        assertMatchesGavtc(of, matches1, matches2, "*:*:*:*", "g:a:v:t:c", true);
        assertMatchesGavtc(of, matches1, matches2, "*:*:*:*:*", "g:a:v:t:c", true);
        assertMatchesGavtc(of, matches1, matches2, "*:*:*:*:*:*", "g:a:v:t:c", true);

        assertMatchesGavtc(of, matches1, matches2, "g", "g:a:v:t:c", true);
        assertMatchesGavtc(of, matches1, matches2, "g:a", "g:a:v:t:c", true);
        assertMatchesGavtc(of, matches1, matches2, "g:a:v", "g:a:v:t:c", true);
        assertMatchesGavtc(of, matches1, matches2, "g:a:v:t:c:s", "g:a:v:t:c", true);
        assertMatchesGavtc(of, matches1, matches2, "*:a", "g:a:v:t:c", true);
        assertMatchesGavtc(of, matches1, matches2, "*:*:v", "g:a:v:t:c", true);
        assertMatchesGavtc(of, matches1, matches2, "g:*", "g:a:v:t:c", true);
        assertMatchesGavtc(of, matches1, matches2, "g:a:*", "g:a:v:t:c", true);
        assertMatchesGavtc(of, matches1, matches2, "g:*:v", "g:a:v:t:c", true);

        assertMatchesGavtc(of, matches1, matches2, "b", "g:a:v:t:c", false);
        assertMatchesGavtc(of, matches1, matches2, "g:b", "g:a:v:t:c", false);
        assertMatchesGavtc(of, matches1, matches2, "g:a:d", "g:a:v:t:c", false);
        assertMatchesGavtc(of, matches1, matches2, "*:b", "g:a:v:t:c", false);
        assertMatchesGavtc(of, matches1, matches2, "*:b:*", "g:a:v:t:c", false);
        assertMatchesGavtc(of, matches1, matches2, "g:*:d", "g:a:v:t:c", false);

        assertMatchesGavtc(of, matches1, matches2, "g:a:v:t:c:s", "x:a:v:t:c", false);
        assertMatchesGavtc(of, matches1, matches2, "g:a:v:t:c:s", "g:x:v:t:c", false);
        assertMatchesGavtc(of, matches1, matches2, "g:a:v:t:c:s", "g:a:x:t:c", false);
        assertMatchesGavtc(of, matches1, matches2, "g:a:v:t:c:s", "g:a:v:x:c", false);
        assertMatchesGavtc(of, matches1, matches2, "g:a:v:t:c:s", "g:a:v:t:x", false);

    }

    static <T> void assertMatchesGavtc(
            Function<String, T> of,
            BiFunction<T, Gavtc, Boolean> matches1,
            BiFunction<T, String[], Boolean> matches2,
            String pattern,
            String probe,
            boolean expected) {
        final T pat = of.apply(pattern);
        final Gavtc gav = Gavtc.of(probe);
        assertThat(matches1.apply(pat, gav)).isEqualTo(expected);
        assertThat(matches2.apply(
                pat,
                new String[] {
                        gav.getGroupId(),
                        gav.getArtifactId(),
                        gav.getVersion(),
                        gav.getType(),
                        gav.getClassifier()
                })).isEqualTo(expected);
    }

    static <T> void assertMatchesGavtcs(
            Function<String, T> of,
            BiFunction<T, Gavtcs, Boolean> matches1,
            BiFunction<T, String[], Boolean> matches2) {
        assertMatchesGavtcs(of, matches1, matches2, "*", "g:a:v:t:c:s", true);
        assertMatchesGavtcs(of, matches1, matches2, "*:*", "g:a:v:t:c:s", true);
        assertMatchesGavtcs(of, matches1, matches2, "*:*:*", "g:a:v:t:c:s", true);
        assertMatchesGavtcs(of, matches1, matches2, "*:*:*:*", "g:a:v:t:c:s", true);
        assertMatchesGavtcs(of, matches1, matches2, "*:*:*:*:*", "g:a:v:t:c:s", true);
        assertMatchesGavtcs(of, matches1, matches2, "*:*:*:*:*:*", "g:a:v:t:c:s", true);

        assertMatchesGavtcs(of, matches1, matches2, "g", "g:a:v:t:c:s", true);
        assertMatchesGavtcs(of, matches1, matches2, "g:a", "g:a:v:t:c:s", true);
        assertMatchesGavtcs(of, matches1, matches2, "g:a:v", "g:a:v:t:c:s", true);
        assertMatchesGavtcs(of, matches1, matches2, "g:a:v:t:c:s", "g:a:v:t:c:s", true);
        assertMatchesGavtcs(of, matches1, matches2, "*:a", "g:a:v:t:c:s", true);
        assertMatchesGavtcs(of, matches1, matches2, "*:*:v", "g:a:v:t:c:s", true);
        assertMatchesGavtcs(of, matches1, matches2, "g:*", "g:a:v:t:c:s", true);
        assertMatchesGavtcs(of, matches1, matches2, "g:a:*", "g:a:v:t:c:s", true);
        assertMatchesGavtcs(of, matches1, matches2, "g:*:v", "g:a:v:t:c:s", true);

        assertMatchesGavtcs(of, matches1, matches2, "b", "g:a:v:t:c:s", false);
        assertMatchesGavtcs(of, matches1, matches2, "g:b", "g:a:v:t:c:s", false);
        assertMatchesGavtcs(of, matches1, matches2, "g:a:d", "g:a:v:t:c:s", false);
        assertMatchesGavtcs(of, matches1, matches2, "*:b", "g:a:v:t:c:s", false);
        assertMatchesGavtcs(of, matches1, matches2, "*:b:*", "g:a:v:t:c:s", false);
        assertMatchesGavtcs(of, matches1, matches2, "g:*:d", "g:a:v:t:c:s", false);

        assertMatchesGavtcs(of, matches1, matches2, "g:a:v:t:c:s", "x:a:v:t:c:s", false);
        assertMatchesGavtcs(of, matches1, matches2, "g:a:v:t:c:s", "g:x:v:t:c:s", false);
        assertMatchesGavtcs(of, matches1, matches2, "g:a:v:t:c:s", "g:a:x:t:c:s", false);
        assertMatchesGavtcs(of, matches1, matches2, "g:a:v:t:c:s", "g:a:v:x:c:s", false);
        assertMatchesGavtcs(of, matches1, matches2, "g:a:v:t:c:s", "g:a:v:t:x:s", false);
        assertMatchesGavtcs(of, matches1, matches2, "g:a:v:t:c:s", "g:a:v:t:c:x", false);

    }

    static <T> void assertMatchesGavtcs(
            Function<String, T> of,
            BiFunction<T, Gavtcs, Boolean> matches1,
            BiFunction<T, String[], Boolean> matches2,
            String pattern,
            String probe,
            boolean expected) {
        final T pat = of.apply(pattern);
        final Gavtcs gav = Gavtcs.of(probe);
        assertThat(matches1.apply(pat, gav)).isEqualTo(expected);
        assertThat(matches2.apply(
                pat,
                new String[] {
                        gav.getGroupId(),
                        gav.getArtifactId(),
                        gav.getVersion(),
                        gav.getType(),
                        gav.getClassifier(),
                        gav.getScope()
                })).isEqualTo(expected);
    }

}
