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

import java.util.Collection;
import java.util.Comparator;
import java.util.function.Predicate;
import org.l2x6.pom.tuner.Comparators;

/**
 * A Maven artifact defined by {@code groupId}, {@code artifactId}, {@code version}, {@code type} and
 * {@code classifier}.
 *
 * @since 4.8.0
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class Gavtc {
    static final String DEFAULT_TYPE = "jar";
    static final Comparator<String> TYPE_COMPARATOR = (a, b) -> (a == null ? "jar" : a)
            .compareTo(b == null ? DEFAULT_TYPE : b);
    static final Comparator<String> SAFE_STRING_COMPARATOR = Comparators.safeStringComparator();

    static final Comparator<Gavtc> GROUP_FIRST_COMPARATOR = Comparator
            .comparing(Gavtc::getGroupId, SAFE_STRING_COMPARATOR)
            .thenComparing(Gavtc::getArtifactId, SAFE_STRING_COMPARATOR)
            .thenComparing(Gavtc::getVersion, SAFE_STRING_COMPARATOR)
            .thenComparing(Gavtc::getType, TYPE_COMPARATOR)
            .thenComparing(Gavtc::getClassifier, SAFE_STRING_COMPARATOR);

    static final Comparator<Gavtc> TYPE_FIRST_COMPARATOR = Comparator
            .comparing(Gavtc::getType, TYPE_COMPARATOR)
            .thenComparing(Gavtc::getGroupId, SAFE_STRING_COMPARATOR)
            .thenComparing(Gavtc::getArtifactId, SAFE_STRING_COMPARATOR)
            .thenComparing(Gavtc::getVersion, SAFE_STRING_COMPARATOR)
            .thenComparing(Gavtc::getClassifier, SAFE_STRING_COMPARATOR);

    /**
     * Parse the given {@code <groupId>:<artifactId>:<version>[:<type>[:<classifier>]]} {@code rawGavtcs} and return a new {@link Gavtc} instance.
     * @param rawGavtcs the string to parse
     * @return a new {@link Gavtc} instance
     * @throws IllegalStateException on any parse errors
     *
     * @since 4.8.0
     */
    public static Gavtc of(String rawGavtcs) {
        String[] gavtcArr = rawGavtcs.split(":");
        if (gavtcArr.length < 3) {
            throw new IllegalStateException("Cannot parse '" + rawGavtcs + " to a " + Gavtc.class.getName()
                    + "; expected '<groupId>:<artifactId>:<version>[:<type>[:<classifier>]]', found too little segments");
        }
        if (gavtcArr.length > 5) {
            throw new IllegalStateException("Cannot parse '" + rawGavtcs + " to a " + Gavtc.class.getName()
                    + "; expected '<groupId>:<artifactId>:<version>[:<type>[:<classifier>]]', found too many segments");
        }
        int i = 0;
        final String groupId = gavtcArr[i++];
        final String artifactId = gavtcArr[i++];
        final String version = gavtcArr[i++];
        final String type = i < gavtcArr.length ? emptyToNull(gavtcArr[i++]) : null;
        final String classifier = i < gavtcArr.length ? emptyToNull(gavtcArr[i++]) : null;
        return new Gavtc(groupId, artifactId, version, type, classifier);
    }

    /**
     * @return a {@link Comparator} that compares on {@link #getGroupId()}, {@link #getArtifactId()}, {@link #getVersion()},
     * {@link #getType()} and {@link #getClassifier()} respectively.
     *
     * @see #typeFirstComparator()
     */
    public static Comparator<Gavtc> groupFirstComparator() {
        return GROUP_FIRST_COMPARATOR;
    }

    /**
     * @return a {@link Comparator} that compares on {@link #getType()}, {@link #getGroupId()}, {@link #getArtifactId()}, {@link #getVersion()} and {@link #getClassifier()} respectively.
     */
    public static Comparator<Gavtc> typeFirstComparator() {
        return TYPE_FIRST_COMPARATOR;
    }

    static String emptyToNull(String string) {
        return string != null && !string.isEmpty() ? string : null;
    }

    static String nullOrEmptyToDefault(String string) {
        return string == null || string.isEmpty() ? DEFAULT_TYPE : string;
    }

    private final Gav gav;
    private final String type;
    private final String classifier;
    private final int hashCode;

    public Gavtc(String groupId, String artifactId, String version) {
        this(groupId, artifactId, version, null, null);
    }

    public Gavtc(String groupId, String artifactId, String version, String type, String classifier) {
        this(new Gav(groupId, artifactId, version), type, classifier);
    }

    public Gavtc(Ga ga, String version, String type, String classifier) {
        this(ga.toGav(version), type, classifier);
    }

    public Gavtc(Gav gav, String type, String classifier) {
        this.gav = gav;
        this.type = type == null || type.isEmpty() ? null : type;
        this.classifier = classifier == null || classifier.isEmpty() ? null : classifier;

        final String useType = DEFAULT_TYPE.equals(type) ? null : type;
        int h = 31 * gav.hashCode() + ((classifier == null) ? 0 : classifier.hashCode());
        h = 31 * h + ((useType == null) ? 0 : useType.hashCode());
        this.hashCode = h;
    }
    /**
     * @return the {@code groupId}, never {@code null}
     *
     * @since 4.8.0
     */
    public String getGroupId() {
        return gav.getGroupId();
    }

    /**
     * @return the {@code artifactId}, never {@code null}
     *
     * @since 4.8.0
     */
    public String getArtifactId() {
        return gav.getArtifactId();
    }

    /**
     * @return the version of this {@link Gav} or {@code null} if the version is unknown
     *
     * @since 4.8.0
     */
    public String getVersion() {
        return gav.getVersion();
    }

    /**
     * @return the artifact type (sometimes called extension), such as `pom`, `jar`, `war`, etc.
     *
     * @since 4.8.0
     */
    public String getType() {
        return type;
    }

    /**
     * @return the classifier or {@code null} (rather than empty string) if it was not set
     *
     * @since 4.8.0
     */
    public String getClassifier() {
        return classifier;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    /**
     * Append {@code <groupId>:<artifactId>:<version>:<type>:<classifier>} to the given {@link StringBuilder} and return it.
     * The {@link #classifier} is only appended if it is not {@code null}.
     *
     * @param stringBuilder the {@link StringBuilder} to append to
     * @return the passed-in {@link StringBuilder}
     *
     * @since 4.8.0
     */
    public StringBuilder toString(StringBuilder stringBuilder) {
        gav.toString(stringBuilder);
        if (type != null || classifier != null) {
            stringBuilder.append(':');
            if (type != null) {
                stringBuilder.append(type);
            }
            if (classifier != null) {
                stringBuilder.append(':');
                if (classifier != null) {
                    stringBuilder.append(classifier);
                }
            }
        }
        return stringBuilder;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Gavtc))
            return false;
        Gavtc other = (Gavtc) obj;
        if (!gav.equals(other.gav)) {
            return false;
        }
        if (classifier == null) {
            if (other.classifier != null)
                return false;
        } else if (!classifier.equals(other.classifier))
            return false;
        final String useType = DEFAULT_TYPE.equals(type) ? null : type;
        final String useOtherType = DEFAULT_TYPE.equals(other.type) ? null : other.type;
        if (useType == null) {
            if (useOtherType != null)
                return false;
        } else if (!useType.equals(useOtherType))
            return false;
        return true;
    }

    /**
     * @return the embedded {@link Ga} instance, shorthand for {@link #toGav()}.{@link Gav#toGa()}
     *
     * @since 4.8.0
     */
    public Ga toGa() {
        return gav.toGa();
    }

    /**
     * @return the embedded {@link Gav} instance
     *
     * @since 4.8.0
     */
    public Gav toGav() {
        return gav;
    }

    /**
     * @param scope
     * @return new {@link Gavtcs} embedding this {@link Gavtc} and having the given {@code scope}.
     *
     * @since 4.8.0
     */
    public Gavtcs toGavtcs(String scope) {
        return new Gavtcs(this, scope);
    }

    /**
     * @param scope
     * @return new {@link Gavtcs} embedding this {@link Gavtc} and having the given {@code scope} and {@code exclusion}
     *
     * @since 4.8.0
     */
    public Gavtcs toGavtcs(String scope, Ga exclusion) {
        return new Gavtcs(this, scope, exclusion);
    }

    /**
     * @param scope
     * @param exclusions
     * @return new {@link Gavtcs} embedding this {@link Gavtc} and having the given {@code scope} and {@code exclusions}
     *
     * @since 4.8.0
     */
    public Gavtcs toGavtcs(String scope, Collection<Ga> exclusions) {
        return new Gavtcs(this, scope, exclusions);
    }

}
