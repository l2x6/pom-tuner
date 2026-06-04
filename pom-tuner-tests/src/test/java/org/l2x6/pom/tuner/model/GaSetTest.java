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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.l2x6.pom.tuner.model.GaSet.IncludeExcludeGaSet.Builder;

import static org.assertj.core.api.Assertions.assertThat;

public class GaSetTest extends AbstractSetTest<GaSet> {

    @Override
    List<GaSet> set(String[] includes, String[] excludes) {
        List<GaSet> result = new ArrayList<>();
        result.add(GaSet.builder().includes(includes).excludes(excludes).build());
        result.add(GaSet.builder().includes(Arrays.asList(includes)).excludes(Arrays.asList(excludes)).build());
        result.add(GaSet.builder()
                .includePatterns(Stream.of(includes).map(GaPattern::of).collect(Collectors.toList()))
                .excludePatterns(Stream.of(excludes).map(GaPattern::of).collect(Collectors.toList()))
                .build());
        result.add(GaSet.builder()
                .includes(Stream.of(includes).map(GaPattern::of).toArray(GaPattern[]::new))
                .excludes(Stream.of(excludes).map(GaPattern::of).toArray(GaPattern[]::new))
                .build());
        {
            final Builder b = GaSet.builder();
            Stream.of(includes).forEach(b::include);
            Stream.of(excludes).forEach(b::exclude);
            result.add(b.build());
        }
        {
            final Builder b = GaSet.builder();
            Stream.of(includes).map(GaPattern::of).forEach(b::include);
            Stream.of(excludes).map(GaPattern::of).forEach(b::exclude);
            result.add(b.build());
        }

        /* Exclusion mark */
        final List<String> exclIncludes = Stream.concat(Stream.of(includes), Stream.of(excludes).map(e -> "!" + e))
                .collect(Collectors.toList());
        result.add(GaSet.builder().includes(exclIncludes).build());
        result.add(GaSet.builder().includes(exclIncludes.toArray(new String[0])).build());
        result.add(GaSet.builder().includes(exclIncludes.stream().collect(Collectors.joining(","))).build());
        Builder b = GaSet.builder();
        exclIncludes.forEach(b::include);
        result.add(b.build());

        return result;
    }

    @Override
    List<GaSet> set(String includes, String excludes) {
        List<GaSet> result = new ArrayList<>();
        result.add(GaSet.builder().includes(includes).excludes(excludes).build());
        return result;
    }

    @Override
    void containsGav(boolean expected, GaSet set, String g, String a, String v) {
        assertThat(set.contains(g, a)).isEqualTo(expected);
        Ga ga = new Ga(g, a);
        assertThat(set.contains(ga)).isEqualTo(expected);
        assertThat(set.test(ga)).isEqualTo(expected);
    }

    @Override
    List<GaSet> union(String[] includes, String[] excludes, String[] unionIncludes) {
        GaSet r = GaSet.builder() //
                .includes(includes) //
                .excludes(excludes) //
                .build();
        if (unionIncludes.length > 0) {
            return Arrays.asList(r
                    .union(GaSet.builder().includes(unionIncludes).build()));
        } else {
            return Arrays.asList(r);
        }
    }

    @Override
    List<GaSet> unionDefaultResultExcludeAll() {
        return Arrays.asList(GaSet.unionBuilder() //
                .defaultResult(GaSet.excludeAll())
                .build());
    }

    @Override
    List<GaSet> setDefaultResultExcludeAll() {
        return Arrays.asList(GaSet.builder() //
                .defaultResult(GaSet.excludeAll())
                .build());
    }

}
