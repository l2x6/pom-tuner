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
import org.l2x6.pom.tuner.model.GavSet.IncludeExcludeGavSet.Builder;

import static org.assertj.core.api.Assertions.assertThat;

public class GavSetTest extends AbstractSetTest<GavSet> {

    @Override
    List<GavSet> set(String[] includes, String[] excludes) {
        List<GavSet> result = new ArrayList<>();
        result.add(GavSet.builder().includes(includes).excludes(excludes).build());
        result.add(GavSet.builder().includes(Arrays.asList(includes)).excludes(Arrays.asList(excludes)).build());

        final Builder b = GavSet.builder();
        Stream.of(includes).forEach(b::include);
        Stream.of(excludes).forEach(b::exclude);
        result.add(b.build());
        return result;
    }

    @Override
    List<GavSet> set(String includes, String excludes) {
        List<GavSet> result = new ArrayList<>();
        result.add(GavSet.builder().includes(includes).excludes(excludes).build());
        return result;
    }

    @Override
    void containsGav(boolean expected, GavSet set, String g, String a, String v) {
        assertThat(set.contains(g, a, v)).isEqualTo(expected);
        Gav gav = new Gav(g, a, v);
        assertThat(set.contains(gav)).isEqualTo(expected);
        assertThat(set.test(gav)).isEqualTo(expected);
    }

    @Override
    List<GavSet> union(String[] includes, String[] excludes, String[] unionIncludes) {
        GavSet r = GavSet.builder() //
                .includes(includes) //
                .excludes(excludes) //
                .build();
        if (unionIncludes.length > 0) {
            return Arrays.asList(r
                    .union(GavSet.builder().includes(unionIncludes).build()));
        } else {
            return Arrays.asList(r);
        }
    }

    @Override
    List<GavSet> unionDefaultResultExcludeAll() {
        return Arrays.asList(GavSet.unionBuilder() //
                .defaultResult(GavSet.excludeAll())
                .build());
    }

    @Override
    List<GavSet> setDefaultResultExcludeAll() {
        return Arrays.asList(GavSet.builder() //
                .defaultResult(GavSet.excludeAll())
                .build());
    }

}
