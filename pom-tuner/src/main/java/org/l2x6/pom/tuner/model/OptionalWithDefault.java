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

import java.util.Comparator;
import java.util.Objects;

/**
 * An optional value with a default.
 * <p>
 * Note that {@link #equals(Object)}, {@link #hashCode()} and {@link #compareTo(OptionalWithDefault)} operate always and
 * only on the value returned by {@link #getValueOrDefault()}. In case you need a comparison taking the raw
 * {@link #value} (that can be {@code null} into account, use {@link #equalsWithDefault(OptionalWithDefault)} or
 * {@link #valueOrDefaultComparator()}.
 *
 * @since 5.0.0
 */
public final class OptionalWithDefault implements Comparable<OptionalWithDefault> {
    static final Comparator<OptionalWithDefault> RAW_VALUE_COMPARATOR = Comparator.comparing(OptionalWithDefault::getValue);
    static final Comparator<OptionalWithDefault> VALUE_OR_DEFAULT_COMPARATOR = Comparator
            .comparing(OptionalWithDefault::getValueOrDefault);

    protected final String value;
    protected final String defaultValue;

    /**
     * @return a {@link Comparator} comparing only by {@link #value}
     *
     * @since  5.0.0
     */
    public static Comparator<OptionalWithDefault> rawValueComparator() {
        return RAW_VALUE_COMPARATOR;
    }

    /**
     * @return a {@link Comparator} comparing {@link #value} if it is set and otherwise by {@link #defaultValue}
     *
     * @since  5.0.0
     */
    public static Comparator<OptionalWithDefault> valueOrDefaultComparator() {
        return VALUE_OR_DEFAULT_COMPARATOR;
    }

    OptionalWithDefault(String value, String defaultValue) {
        this.value = value == null || value.isEmpty() ? null : value;
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
    }

    /**
     * @return the value or <code>null</code> if the value is unknown
     * @see    #getValueOrDefault()
     *
     * @since  5.0.0
     */
    public String getValue() {
        return value;
    }

    /**
     * @return the value if it is set or default value otherwise; never {@code null}
     *
     * @since  5.0.0
     */
    public String getValueOrDefault() {
        return value == null ? defaultValue : value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int hashCode() {
        return getValueOrDefault().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OptionalWithDefault other = (OptionalWithDefault) obj;
        return getValueOrDefault().equals(other.getValueOrDefault());
    }

    /**
     * @param  other the {@link OptionalWithDefault} to compare with
     * @return       {@code true} if {@link #value} and {@code other.value} are the same or equal
     *
     * @since        5.0.0
     */
    public boolean equalsRaw(OptionalWithDefault other) {
        return this.value == other.value || (this.value != null && this.value.equals(other.value));
    }

    /**
     * @return {@code true} if {@link #value} is {@code null} or equal to default and {@code false} otherwise
     *
     * @since  5.0.0
     */
    public boolean isDefault() {
        return value == null || value.equals(defaultValue);
    }

    /**
     * @return {@code true} if {@link #value} is non-{@code null}
     *
     * @since  5.0.0
     */
    public boolean isSet() {
        return value != null;
    }

    /**
     * @return {@code new OptionalWithDefault(null, defaultValue)} if {@code value != null && value.equals(defaultValue)};
     *         otherwise this {@link OptionalWithDefault}.
     *
     * @since  5.0.0
     */
    public OptionalWithDefault preferDefault() {
        if (value != null && value.equals(defaultValue)) {
            return new OptionalWithDefault(null, defaultValue);
        }
        return this;
    }

    /**
     * Order given by {@link #valueOrDefaultComparator()}
     *
     * @since 5.0.0
     */
    @Override
    public int compareTo(OptionalWithDefault other) {
        return VALUE_OR_DEFAULT_COMPARATOR.compare(this, other);
    }

}
