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

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

public class Comparators {
    private static final Comparator<String> SAFE_STRING_COMPARATOR = (a, b) -> a == b
            ? 0
            : (a != null ? (b != null ? a.compareTo(b) : 1) : -1);

    private Comparators() {
    }

    public static Comparator<Map.Entry<String, String>> entryKeyOnly() {
        return Comparator
                .comparing(Map.Entry::getKey, SAFE_STRING_COMPARATOR);
    }

    public static Comparator<Map.Entry<String, String>> entryValueOnly() {
        return Comparator
                .comparing(Map.Entry::getValue, SAFE_STRING_COMPARATOR);
    }

    public static Comparator<Map.Entry<String, String>> entryKeyValue() {
        return Comparator
                .comparing((Map.Entry<String, String> en) -> en.getKey(), SAFE_STRING_COMPARATOR)
                .thenComparing(Map.Entry::getValue, SAFE_STRING_COMPARATOR);
    }

    public static <T> Comparator<T> before(T item) {
        return new BeforeItemComparator<T>(item);
    }

    public static <T> Comparator<T> beforeFirst() {
        return (T o1, T o2) -> {
            if (Objects.equals(o1, o2)) {
                return 0;
            }
            return -1;
        };
    }

    public static <T> Comparator<T> after(T item) {
        return new AfterItemComparator<T>(item);
    }

    public static <T> Comparator<T> afterLast() {
        return (T o1, T o2) -> {
            if (Objects.equals(o1, o2)) {
                return 0;
            }
            return 1;
        };
    }

    public static Comparator<String> safeStringComparator() {
        return SAFE_STRING_COMPARATOR;
    }

    static class AfterItemComparator<T> implements Comparator<T> {
        private final T item;
        private boolean found = false;

        public AfterItemComparator(T item) {
            this.item = item;
        }

        @Override
        public int compare(T o1, T o2) {
            boolean oldFound = found;
            if (Objects.equals(o2, item)) {
                found = true;
            }
            if (Objects.equals(o1, o2)) {
                return 0;
            }
            return oldFound ? -1 : 1;
        }
    }

    static class BeforeItemComparator<T> implements Comparator<T> {
        private final T item;
        private boolean found = false;

        public BeforeItemComparator(T item) {
            this.item = item;
        }

        @Override
        public int compare(T o1, T o2) {
            if (Objects.equals(o2, item)) {
                found = true;
            }
            if (Objects.equals(o1, o2)) {
                return 0;
            }
            return found ? -1 : 1;
        }
    }

}
