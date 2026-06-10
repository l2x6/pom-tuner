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
package org.l2x6.pom.tuner;

import java.util.Arrays;
import java.util.function.Predicate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.l2x6.pom.tuner.MavenSourceTree.ActiveProfiles;
import org.l2x6.pom.tuner.model.Ga;
import org.l2x6.pom.tuner.model.ModelTestUtil;
import org.l2x6.pom.tuner.model.Profile;

public class ActiveProfilesTest {

    static final Ga GA_A = new Ga("org.example", "module-a");
    static final Ga GA_B = new Ga("org.example", "module-b");
    static final Ga GA_C = new Ga("org.other", "module-c");

    static final Profile PROFILE_NULL = ModelTestUtil.newProfile(null);
    static final Profile PROFILE_P1 = ModelTestUtil.newProfile("p1");
    static final Profile PROFILE_P2 = ModelTestUtil.newProfile("p2");
    static final Profile PROFILE_P3 = ModelTestUtil.newProfile("p3");

    @Test
    public void ofArgs() {
        assertProfiles(ActiveProfiles.EMPTY, ActiveProfiles.ofArgs(Arrays.asList()));
        assertProfiles(ActiveProfiles.of("p1"), ActiveProfiles.ofArgs(Arrays.asList("-Pp1")));
        assertProfiles(ActiveProfiles.of("p1", "p2"), ActiveProfiles.ofArgs(Arrays.asList("-Pp1,p2")));
        assertProfiles(ActiveProfiles.of("p1"), ActiveProfiles.ofArgs(Arrays.asList("-P", "p1")));
        assertProfiles(ActiveProfiles.of("p1", "p2"), ActiveProfiles.ofArgs(Arrays.asList("-P", "p1,p2")));
        assertProfiles(ActiveProfiles.of("p1"), ActiveProfiles.ofArgs(Arrays.asList("--activate-profiles", "p1")));
        assertProfiles(ActiveProfiles.of("p1", "p2"), ActiveProfiles.ofArgs(Arrays.asList("--activate-profiles", "p1,p2")));
    }

    @Test
    public void ofEmpty() {
        ActiveProfiles empty = ActiveProfiles.of();
        Assertions.assertTrue(empty.test(GA_A, PROFILE_NULL));
        Assertions.assertFalse(empty.test(GA_A, PROFILE_P1));
        Assertions.assertFalse(empty.test(GA_A, PROFILE_P2));
    }

    @Test
    public void ofWithProfileIds() {
        ActiveProfiles ap = ActiveProfiles.of("p1", "p2");
        Assertions.assertTrue(ap.test(GA_A, PROFILE_NULL));
        Assertions.assertTrue(ap.test(GA_A, PROFILE_P1));
        Assertions.assertTrue(ap.test(GA_A, PROFILE_P2));
        Assertions.assertFalse(ap.test(GA_A, PROFILE_P3));

        Assertions.assertTrue(ap.test(GA_B, PROFILE_P1));
    }

    @Test
    public void all() {
        ActiveProfiles all = ActiveProfiles.all();
        Assertions.assertTrue(all.test(GA_A, PROFILE_NULL));
        Assertions.assertTrue(all.test(GA_A, PROFILE_P1));
        Assertions.assertTrue(all.test(GA_A, PROFILE_P3));
        Assertions.assertTrue(all.test(GA_B, PROFILE_P2));
    }

    @Test
    public void forModule() {
        ActiveProfiles ap = ActiveProfiles.of("p1");
        Predicate<Profile> predicate = ap.forModule(GA_A);
        Assertions.assertTrue(predicate.test(PROFILE_NULL));
        Assertions.assertTrue(predicate.test(PROFILE_P1));
        Assertions.assertFalse(predicate.test(PROFILE_P2));
    }

    @Test
    public void forModuleEmpty() {
        Predicate<Profile> predicate = ActiveProfiles.of().forModule(GA_A);
        Assertions.assertTrue(predicate.test(PROFILE_NULL));
        Assertions.assertFalse(predicate.test(PROFILE_P1));
    }

    @Test
    public void builderAdd() {

        assertPatterns(ActiveProfiles.builder()
                .add("org.example:module-a", "p1", "p2")
                .otherwiseNone());

        assertPatterns(ActiveProfiles.builder()
                .add("org.example:module-a", Arrays.asList("p1", "p2"))
                .otherwiseNone());

        assertPatterns(ActiveProfiles.builder()
                .add(GA_A::equals, "p1", "p2")
                .otherwiseNone());

        assertPatterns(ActiveProfiles.builder()
                .add(GA_A::equals, Arrays.asList("p1", "p2"))
                .otherwiseNone());

        assertPatterns(ActiveProfiles.builder()
                .add(GA_A::equals, p -> p.getId().equals("p1") || p.getId().equals("p2"))
                .otherwiseNone());

    }

    static void assertPatterns(ActiveProfiles ap) {
        Assertions.assertTrue(ap.test(GA_A, PROFILE_NULL));
        Assertions.assertTrue(ap.test(GA_A, PROFILE_P1));
        Assertions.assertTrue(ap.test(GA_A, PROFILE_P2));
        Assertions.assertFalse(ap.test(GA_A, PROFILE_P3));

        Assertions.assertTrue(ap.test(GA_B, PROFILE_NULL));
        Assertions.assertFalse(ap.test(GA_B, PROFILE_P1));
        Assertions.assertFalse(ap.test(GA_B, PROFILE_P2));
        Assertions.assertFalse(ap.test(GA_B, PROFILE_P3));
    }

    @Test
    public void builderOtherwiseAll() {
        ActiveProfiles ap = ActiveProfiles.builder()
                .add("org.example:module-a", "p1")
                .otherwiseAll();

        Assertions.assertTrue(ap.test(GA_A, PROFILE_NULL));
        Assertions.assertTrue(ap.test(GA_A, PROFILE_P1));
        Assertions.assertFalse(ap.test(GA_A, PROFILE_P2));

        Assertions.assertTrue(ap.test(GA_B, PROFILE_NULL));
        Assertions.assertTrue(ap.test(GA_B, PROFILE_P1));
        Assertions.assertTrue(ap.test(GA_B, PROFILE_P2));
        Assertions.assertTrue(ap.test(GA_B, PROFILE_P3));
    }

    @Test
    public void builderOtherwiseNone() {
        ActiveProfiles ap = ActiveProfiles.builder()
                .add("org.example:module-a", "p1")
                .otherwiseNone();

        Assertions.assertTrue(ap.test(GA_A, PROFILE_NULL));
        Assertions.assertTrue(ap.test(GA_A, PROFILE_P1));
        Assertions.assertFalse(ap.test(GA_A, PROFILE_P2));

        Assertions.assertTrue(ap.test(GA_B, PROFILE_NULL));
        Assertions.assertFalse(ap.test(GA_B, PROFILE_P1));
        Assertions.assertFalse(ap.test(GA_B, PROFILE_P2));
    }

    @Test
    public void builderPerModuleProfiles() {
        ActiveProfiles ap = ActiveProfiles.builder()
                .add("org.example:module-a", "p1")
                .add("org.example:module-b", "p2", "p3")
                .otherwiseNone();

        Assertions.assertTrue(ap.test(GA_A, PROFILE_P1));
        Assertions.assertFalse(ap.test(GA_A, PROFILE_P2));
        Assertions.assertFalse(ap.test(GA_A, PROFILE_P3));

        Assertions.assertFalse(ap.test(GA_B, PROFILE_P1));
        Assertions.assertTrue(ap.test(GA_B, PROFILE_P2));
        Assertions.assertTrue(ap.test(GA_B, PROFILE_P3));

        Assertions.assertFalse(ap.test(GA_C, PROFILE_P1));
        Assertions.assertFalse(ap.test(GA_C, PROFILE_P2));
    }

    @Test
    public void builderFirstMatchWins() {
        ActiveProfiles ap = ActiveProfiles.builder()
                .add("org.example:module-a", "p1")
                .add("org.example:*", "p2")
                .otherwiseNone();

        Assertions.assertTrue(ap.test(GA_A, PROFILE_P1));
        Assertions.assertFalse(ap.test(GA_A, PROFILE_P2));

        Assertions.assertFalse(ap.test(GA_B, PROFILE_P1));
        Assertions.assertTrue(ap.test(GA_B, PROFILE_P2));
    }

    @Test
    public void builderWildcardPattern() {
        ActiveProfiles ap = ActiveProfiles.builder()
                .add("org.example:*", "p1")
                .otherwiseNone();

        Assertions.assertTrue(ap.test(GA_A, PROFILE_P1));
        Assertions.assertTrue(ap.test(GA_B, PROFILE_P1));
        Assertions.assertFalse(ap.test(GA_C, PROFILE_P1));
    }

    @Test
    public void forModuleWithBuilder() {
        ActiveProfiles ap = ActiveProfiles.builder()
                .add("org.example:module-a", "p1")
                .add("org.example:module-b", "p2")
                .otherwiseNone();

        Predicate<Profile> forA = ap.forModule(GA_A);
        Assertions.assertTrue(forA.test(PROFILE_NULL));
        Assertions.assertTrue(forA.test(PROFILE_P1));
        Assertions.assertFalse(forA.test(PROFILE_P2));

        Predicate<Profile> forB = ap.forModule(GA_B);
        Assertions.assertTrue(forB.test(PROFILE_NULL));
        Assertions.assertFalse(forB.test(PROFILE_P1));
        Assertions.assertTrue(forB.test(PROFILE_P2));

        Predicate<Profile> forC = ap.forModule(GA_C);
        Assertions.assertTrue(forC.test(PROFILE_NULL));
        Assertions.assertFalse(forC.test(PROFILE_P1));
        Assertions.assertFalse(forC.test(PROFILE_P2));
    }

    @Test
    public void noMatchingRuleRejectAll() {
        ActiveProfiles ap = ActiveProfiles.builder()
                .add("org.example:module-a", "p1")
                .build();

        Assertions.assertTrue(ap.test(GA_B, PROFILE_NULL));
        Assertions.assertFalse(ap.test(GA_B, PROFILE_P1));
    }

    static void assertProfiles(ActiveProfiles expected, ActiveProfiles actual) {
        Ga ga = new Ga("foo", "bar");
        for (Profile p : Arrays.asList(PROFILE_NULL, PROFILE_P1, PROFILE_P2, PROFILE_P3)) {
            boolean expectedResult = expected.test(ga, p);
            final boolean actualResult = actual.test(ga, p);
            Assertions.assertEquals(expectedResult, actualResult, "Expected profile " + p + (expectedResult ? "" : " not")
                    + " to be included but it was " + (actualResult ? "" : " not") + " included");
        }
    }
}
