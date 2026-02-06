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
package org.l2x6.pom.tuner.transform.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Utility methods for constructing predicates for selecting {@code pom.xml} profiles by {@code id}.
 * Note that this library handles the {@code <project>} element as a profile with a {@code null} {@code id}.
 * Use {@link #main()} to create a {@link Predicate} secting only the {@code <project>} element and nothing else.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  5.0.0
 */
public class ProfileId {

    private ProfileId() {
    }

    /**
     * @return a new {@link Predicate} selecting the {@code <project>} pseudo-profile having {@code id == null}
     * @since  5.0.0
     */
    public static Predicate<String> main() {
        return id -> id == null;
    }

    /**
     * @return a new {@link Predicate} selecting the {@code <project>} element and all profiles
     * @since  5.0.0
     */
    public static Predicate<String> all() {
        return id -> true;
    }

    /**
     * @param  ids the profile {@code id}s to select in addition to the {@code <project>} pseudo-profile that is added implicitly
     * @return     a new {@link Predicate} selecting the {@code <project>} element and the specified profiles
     * @since      5.0.0
     */
    public static Predicate<String> ids(String... ids) {
        return ids(Arrays.asList(ids));
    }

    /**
     * @param  ids the profile {@code id}s to select; unlike with {@link #ids(String...)} the {@code <project>} pseudo-profile is not added implicitly
     * @return     a new {@link Predicate} selecting only the specified profiles
     * @since      5.0.0
     */
    public static Predicate<String> idsOnly(String... ids) {
        return idsOnly(Arrays.asList(ids));
    }

    /**
     * @param  ids the profile {@code id}s to select in addition to the {@code <project>} pseudo-profile that is added implicitly
     * @return     a new {@link Predicate} selecting the {@code <project>} element and the specified profiles
     * @since      5.0.0
     */
    public static Predicate<String> ids(Collection<String> ids) {
        Set<String> set = new HashSet<>(ids);
        set.add(null);
        return id -> set.contains(id);
    }

    /**
     * @param  ids the profile {@code id}s to select; unlike with {@link #ids(Collection)} the {@code <project>} pseudo-profile is not added implicitly
     * @return     a new {@link Predicate} selecting only the specified profiles
     * @since      5.0.0
     */
    public static Predicate<String> idsOnly(Collection<String> ids) {
        Set<String> set = new HashSet<>(ids);
        return id -> set.contains(id);
    }
}
