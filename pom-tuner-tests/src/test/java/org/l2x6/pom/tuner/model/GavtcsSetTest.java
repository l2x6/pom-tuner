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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.l2x6.pom.tuner.model.GavtcsSet.IncludeExcludeGavSet.Builder;

import static org.assertj.core.api.Assertions.assertThat;

public class GavtcsSetTest extends AbstractSetTest<GavtcsSet> {

    @Override
    List<GavtcsSet> set(String[] includes, String[] excludes) {
        List<GavtcsSet> result = new ArrayList<>();
        result.add(GavtcsSet.builder().includes(includes).excludes(excludes).build());
        result.add(GavtcsSet.builder().includes(Arrays.asList(includes)).excludes(Arrays.asList(excludes)).build());
        final Builder b = GavtcsSet.builder();
        Stream.of(includes).forEach(b::include);
        Stream.of(excludes).forEach(b::exclude);
        result.add(b.build());
        return result;
    }

    @Override
    List<GavtcsSet> set(String includes, String excludes) {
        List<GavtcsSet> result = new ArrayList<>();
        result.add(GavtcsSet.builder().includes(includes).excludes(excludes).build());
        return result;
    }

    @Override
    void containsGav(boolean expected, GavtcsSet set, String g, String a, String v) {
        assertThat(set.contains(g, a, v)).isEqualTo(expected);
    }

    void containsGav(boolean expected, GavtcsSet set, String g, String a, String v, String type, String classifier,
            String scope) {
        assertThat(set.contains(g, a, v, type, classifier, scope)).isEqualTo(expected);
        assertThat(set.contains(new Gavtcs(g, a, v, type, classifier, scope))).isEqualTo(expected);
    }

    @Override
    List<GavtcsSet> union(String[] includes, String[] excludes, String[] unionIncludes) {
        GavtcsSet r = GavtcsSet.builder() //
                .includes(includes) //
                .excludes(excludes) //
                .build();
        if (unionIncludes.length > 0) {
            return Arrays.asList(r
                    .union(GavtcsSet.builder().includes(unionIncludes).build()));
        } else {
            return Arrays.asList(r);
        }
    }

    @Override
    List<GavtcsSet> unionDefaultResultExcludeAll() {
        return Arrays.asList(GavtcsSet.unionBuilder() //
                .defaultResult(GavtcsSet.excludeAll())
                .build());
    }

    @Override
    List<GavtcsSet> setDefaultResultExcludeAll() {
        return Arrays.asList(GavtcsSet.builder() //
                .defaultResult(GavtcsSet.excludeAll())
                .build());
    }

    @Test
    public void defaults() {
        super.defaults();
        List<GavtcsSet> sets = set(arr(), arr());
        for (GavtcsSet set : sets) {
            containsGav(true, set, "org.group1", "artifact1", "1.2.3", "", "", "");
            containsGav(true, set, "org.group1", "artifact1", "1.2.3", "jar", "x86_64", "compile");
        }
    }

    @Override
    public void excludeArtifact() {
        super.excludeArtifact();

        List<GavtcsSet> sets = set(arr(), arr("org.group1:artifact1:*:jar::test"));
        for (GavtcsSet set : sets) {
            containsGav(false, set, "org.group1", "artifact1", "1.2.3", "jar", "", "test");
            containsGav(true, set, "org.group1", "artifact2", "2.3.4", "jar", "", "test");
            containsGav(true, set, "org.group1", "artifact2", "2.3.4", "jar", "x86_64", "test");

            containsGav(true, set, "org.group1", "artifact1", "1.2.3", "jar", "", "compile");
            containsGav(true, set, "org.group1", "artifact1", "1.2.3", "jar", "x86_64", "test");
            containsGav(true, set, "org.group1", "artifact1", "1.2.3", "pom", "", "test");
        }

    }

    @Override
    public void includeArtifact() {
        super.includeArtifact();

        List<GavtcsSet> sets = set(arr("org.group1:artifact1:*:jar::test"), arr());
        for (GavtcsSet set : sets) {
            containsGav(true, set, "org.group1", "artifact1", "1.2.3", "jar", "", "test");
            containsGav(false, set, "org.group1", "artifact2", "2.3.4", "jar", "", "test");
            containsGav(false, set, "org.group1", "artifact2", "2.3.4", "jar", "x86_64", "test");

            containsGav(false, set, "org.group1", "artifact1", "1.2.3", "jar", "", "compile");
            containsGav(false, set, "org.group1", "artifact1", "1.2.3", "jar", "x86_64", "test");
            containsGav(false, set, "org.group1", "artifact1", "1.2.3", "pom", "", "test");
        }

    }

}
