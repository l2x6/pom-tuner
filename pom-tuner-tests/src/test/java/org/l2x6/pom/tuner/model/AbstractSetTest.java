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

import java.util.List;
import org.junit.jupiter.api.Test;

public abstract class AbstractSetTest<T> {

    abstract List<T> set(String[] includes, String[] excludes);

    abstract List<T> set(String includes, String excludes);

    abstract List<T> setDefaultResultExcludeAll();

    abstract List<T> union(String[] includes, String[] excludes, String[] unionIncludes);

    abstract List<T> unionDefaultResultExcludeAll();

    abstract void containsGav(boolean expected, T set, String g, String a, String v);

    static String[] arr(String... elems) {
        return elems;
    }

    @Test
    public void defaults() {
        List<T> sets = set(arr(), arr());
        for (T set : sets) {
            containsGav(true, set, "org.group1", "artifact1", "1.2.3");
        }
    }

    @Test
    public void excludeArtifact() {
        List<T> sets = set(arr(), arr("org.group1:artifact1"));
        for (T set : sets) {
            containsGav(false, set, "org.group1", "artifact1", "1.2.3");
            containsGav(true, set, "org.group1", "artifact2", "2.3.4");

            containsGav(true, set, "org.group2", "artifact2", "5.6.7");
            containsGav(true, set, "org.group2", "artifact3", "6.7.8");

            containsGav(true, set, "com.group3", "artifact4", "5.6.7");
        }
    }

    @Test
    public void excludeGroups() {
        List<T> sets = set(arr(), arr("org.group1", "org.group2"));
        for (T set : sets) {
            containsGav(false, set, "org.group1", "artifact1", "1.2.3");
            containsGav(false, set, "org.group1", "artifact2", "2.3.4");

            containsGav(false, set, "org.group2", "artifact2", "5.6.7");
            containsGav(false, set, "org.group2", "artifact3", "6.7.8");

            containsGav(true, set, "com.group3", "artifact4", "5.6.7");
        }
    }

    @Test
    public void includeArtifact() {
        List<T> sets = set(arr("org.group1:artifact1"), arr());
        for (T set : sets) {

            containsGav(true, set, "org.group1", "artifact1", "1.2.3");
            containsGav(false, set, "org.group1", "artifact2", "2.3.4");

            containsGav(false, set, "org.group2", "artifact2", "5.6.7");
            containsGav(false, set, "org.group2", "artifact3", "6.7.8");

            containsGav(false, set, "com.group3", "artifact4", "5.6.7");
        }

    }

    @Test
    public void includeExcludeGroups() {
        List<T> sets = set(arr("org.group1"), arr("org.group2"));
        for (T set : sets) {
            containsGav(true, set, "org.group1", "artifact1", "1.2.3");
            containsGav(true, set, "org.group1", "artifact2", "2.3.4");

            containsGav(false, set, "org.group2", "artifact2", "5.6.7");
            containsGav(false, set, "org.group2", "artifact3", "6.7.8");

            containsGav(false, set, "com.group3", "artifact4", "5.6.7");
        }

    }

    @Test
    public void includeGroup() {
        List<T> sets = set(arr("org.group1"), arr());
        for (T set : sets) {
            containsGav(true, set, "org.group1", "artifact1", "1.2.3");
            containsGav(true, set, "org.group1", "artifact2", "2.3.4");

            containsGav(false, set, "org.group2", "artifact2", "5.6.7");
        }

    }

    @Test
    public void includeGroups() {
        List<T> sets = set(arr("org.group1", "org.group2"), arr());
        for (T set : sets) {
            containsGav(true, set, "org.group1", "artifact1", "1.2.3");
            containsGav(true, set, "org.group1", "artifact2", "2.3.4");

            containsGav(true, set, "org.group2", "artifact2", "5.6.7");
            containsGav(true, set, "org.group2", "artifact3", "6.7.8");

            containsGav(false, set, "com.group3", "artifact4", "5.6.7");
        }

    }

    @Test
    public void includeGroupsExcludeArtifact() {
        List<T> sets = set(arr("org.group1", "org.group2", "com.group3"),
                arr("org.group1:artifact2", "org.group1:artifact3", "org.group2:artifact2", "org.group2:artifact3"));
        for (T set : sets) {
            containsGav(true, set, "org.group1", "artifact1", "1.2.3");
            containsGav(false, set, "org.group1", "artifact2", "2.3.4");
            containsGav(false, set, "org.group1", "artifact3", "2.3.4");

            containsGav(true, set, "org.group2", "artifact1", "1.2.3");
            containsGav(false, set, "org.group2", "artifact2", "2.3.4");
            containsGav(false, set, "org.group2", "artifact3", "2.3.4");

            containsGav(true, set, "com.group3", "artifact1", "5.6.7");
            containsGav(true, set, "com.group3", "artifact2", "5.6.7");
            containsGav(true, set, "com.group3", "artifact3", "5.6.7");
            containsGav(true, set, "com.group3", "artifact4", "5.6.7");
        }

    }

    @Test
    public void whitespaceSeparated() {
        List<T> sets = set("org.group1,org.group2\n     com.group3",
                "org.group1:artifact2\t\torg.group1:artifact3 org.group2:artifact2\torg.group2:artifact3");
        for (T set : sets) {
            containsGav(true, set, "org.group1", "artifact1", "1.2.3");
            containsGav(false, set, "org.group1", "artifact2", "2.3.4");
            containsGav(false, set, "org.group1", "artifact3", "2.3.4");

            containsGav(true, set, "org.group2", "artifact1", "1.2.3");
            containsGav(false, set, "org.group2", "artifact2", "2.3.4");
            containsGav(false, set, "org.group2", "artifact3", "2.3.4");

            containsGav(true, set, "com.group3", "artifact1", "5.6.7");
            containsGav(true, set, "com.group3", "artifact2", "5.6.7");
            containsGav(true, set, "com.group3", "artifact3", "5.6.7");
            containsGav(true, set, "com.group3", "artifact4", "5.6.7");
        }

    }

    @Test
    public void union() {
        List<T> sets = union(arr("org.group1"), arr("org.group1:artifact2"), arr("org.group1:artifact2"));
        for (T set : sets) {
            containsGav(true, set, "org.group1", "artifact1", "1.2.3");
            containsGav(true, set, "org.group1", "artifact2", "2.3.4");
        }

    }

    @Test
    public void unionExplicitDefault() {
        List<T> sets = unionDefaultResultExcludeAll();
        for (T set : sets) {
            containsGav(false, set, "org.group1", "artifact1", "1.2.3");
            containsGav(false, set, "org.group1", "artifact2", "2.3.4");
        }

    }

    @Test
    public void unionImplicitDefault() {
        List<T> sets = union(arr(), arr(), arr());
        for (T set : sets) {
            containsGav(true, set, "org.group1", "artifact1", "1.2.3");
            containsGav(true, set, "org.group1", "artifact2", "2.3.4");
        }

    }

    @Test
    public void includeExcludeExplicitDefault() {
        List<T> sets = setDefaultResultExcludeAll();
        for (T set : sets) {
            containsGav(false, set, "org.group1", "artifact1", "1.2.3");
            containsGav(false, set, "org.group1", "artifact2", "2.3.4");
        }

    }

    @Test
    public void includeExcludeImplicitDefault() {
        List<T> sets = set(arr(), arr());
        for (T set : sets) {
            containsGav(true, set, "org.group1", "artifact1", "1.2.3");
            containsGav(true, set, "org.group1", "artifact2", "2.3.4");
        }
    }
}
