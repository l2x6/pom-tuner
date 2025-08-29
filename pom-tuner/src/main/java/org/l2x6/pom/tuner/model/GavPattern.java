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

import java.io.Serializable;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * A general purpose pattern for matching GAVs (i.e. triples consisting of {@code groupId}, {@code artifactId} and
 * {@code version}).
 * <p>
 * To create a new {@link GavPattern}, use either {@link #of(String)} or {@link #builder()}, both of which accept
 * wildcard patterns (rather than regular expression patterns). See the JavaDocs of the two respective methods for more
 * details.
 * <p>
 * {@link GavPattern} overrides {@link #hashCode()} and {@link #equals(Object)} and can thus be used as a key in a
 * {@link Map}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class GavPattern implements Serializable, Comparable<GavPattern>, Predicate<Gav> {

    /**
     * A {@link GavPattern} builder.
     */
    public static class Builder {

        private GavSegmentPattern artifactIdPattern = GavSegmentPattern.MATCH_ALL;
        private GavSegmentPattern groupIdPattern = GavSegmentPattern.MATCH_ALL;
        private GavSegmentPattern versionPattern = GavSegmentPattern.MATCH_ALL;

        private Builder() {
        }

        /**
         * Sets the pattern for {@code artifactId}
         *
         * @param  wildcardPattern a pattern that can contain string literals and asterisk {@code *} wildcards
         * @return                 this {@link Builder}
         */
        public Builder artifactIdPattern(String wildcardPattern) {
            this.artifactIdPattern = new GavSegmentPattern(wildcardPattern);
            return this;
        }

        public GavPattern build() {
            return new GavPattern(groupIdPattern, artifactIdPattern, versionPattern);
        }

        /**
         * Sets the pattern for {@code groupId}
         *
         * @param  wildcardPattern a pattern that can contain string literals and asterisk {@code *} wildcards
         * @return                 this {@link Builder}
         */
        public Builder groupIdPattern(String wildcardPattern) {
            this.groupIdPattern = new GavSegmentPattern(wildcardPattern);
            return this;
        }

        /**
         * Sets the pattern for {@code version}
         *
         * @param  wildcardPattern a pattern that can contain string literals and asterisk {@code *} wildcards
         * @return                 this {@link Builder}
         */
        public Builder versionPattern(String wildcardPattern) {
            this.versionPattern = new GavSegmentPattern(wildcardPattern);
            return this;
        }

    }

    /**
     * A pair of a {@link Pattern} and its wildcard source.
     */
    static class GavSegmentPattern implements Serializable {
        static final GavSegmentPattern MATCH_ALL = new GavSegmentPattern(GavPattern.MULTI_WILDCARD);
        static final String MATCH_ALL_PATTERN_SOURCE = ".*";
        /**  */
        private static final long serialVersionUID = 1063634992004995585L;
        private final transient Pattern pattern;
        private final String source;

        GavSegmentPattern(String wildcardSource) {
            super();
            final StringBuilder sb = new StringBuilder(wildcardSource.length() + 2);
            final StringTokenizer st = new StringTokenizer(wildcardSource, GavPattern.MULTI_WILDCARD, true);
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (GavPattern.MULTI_WILDCARD.equals(token)) {
                    sb.append(MATCH_ALL_PATTERN_SOURCE);
                } else {
                    sb.append(Pattern.quote(token));
                }
            }
            this.pattern = Pattern.compile(sb.toString());
            this.source = wildcardSource;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            GavSegmentPattern other = (GavSegmentPattern) obj;
            return source.equals(other.source);
        }

        /**
         * @return the wildcard source of the {@link #pattern}
         */
        public String getSource() {
            return source;
        }

        @Override
        public int hashCode() {
            return source.hashCode();
        }

        public boolean matches(String input) {
            return matchesAll() || pattern.matcher(input == null ? "" : input).matches();
        }

        /**
         * @return {@code true} if this {@link GavSegmentPattern} is equal to {@link #MATCH_ALL}; {@code false}
         *         otherwise
         */
        public boolean matchesAll() {
            return MATCH_ALL.equals(this);
        }

        @Override
        public String toString() {
            return source;
        }
    }

    static final char DELIMITER = ':';
    static final String DELIMITER_STRING = ":";
    private static final GavPattern MATCH_ALL;
    private static final GavPattern MATCH_SNAPSHOTS;
    static final String MULTI_WILDCARD = "*";
    static final char MULTI_WILDCARD_CHAR = '*';
    private static final long serialVersionUID = 5570763687443531797L;
    static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

    static {
        MATCH_ALL = new GavPattern(GavSegmentPattern.MATCH_ALL, GavSegmentPattern.MATCH_ALL,
                GavSegmentPattern.MATCH_ALL);
        MATCH_SNAPSHOTS = new GavPattern(GavSegmentPattern.MATCH_ALL, GavSegmentPattern.MATCH_ALL,
                new GavSegmentPattern(MULTI_WILDCARD + SNAPSHOT_SUFFIX));
    }

    /**
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return a singleton that matches all possible GAVs
     */
    public static GavPattern matchAll() {
        return MATCH_ALL;
    }

    /**
     * @return a singleton that matches any GAV that has a version ending with {@value #SNAPSHOT_SUFFIX}
     */
    public static GavPattern matchSnapshots() {
        return MATCH_SNAPSHOTS;
    }

    /**
     * Creates a new {@link GavPattern} out of the given {@code wildcardPattern}. A wildcard pattern consists of string
     * literals and asterisk wildcard {@code *}. {@code *} matches zero or many arbitrary characters. Wildcard patterns
     * for groupId, artifactId and version need to be delimited by colon {@value #DELIMITER}.
     * <p>
     * GAV pattern examples:
     * <p>
     * {@code org.my-group} - an equivalent of {@code org.my-group:*:*}. It will match any version of any artifact
     * having groupId {@code org.my-group}.
     * <p>
     * {@code org.my-group*} - an equivalent of {@code org.my-group*:*:*}. It will match any version of any artifact
     * whose groupId starts with {@code org.my-group} - i.e. it will match all of {@code org.my-group},
     * {@code org.my-group.api}, {@code org.my-group.impl}, etc.
     * <p>
     * {@code org.my-group:my-artifact} - an equivalent of {@code org.my-group:my-artifact:*}. It will match any version
     * of all such artifacts that have groupId {@code org.my-group} and artifactId {@code my-artifact}
     * <p>
     * {@code org.my-group:my-artifact:1.2.3} - will match just the version 1.2.3 of artifacts
     * {@code org.my-group:my-artifact}.
     *
     * @param  wildcardPattern a string pattern to parse and create a new {@link GavPattern} from
     * @return                 a new {@link GavPattern}
     */
    public static GavPattern of(String wildcardPattern) {
        final GavSegmentPattern groupIdPattern;
        StringTokenizer st = new StringTokenizer(wildcardPattern, DELIMITER_STRING);
        if (st.hasMoreTokens()) {
            groupIdPattern = new GavSegmentPattern(st.nextToken());
        } else {
            groupIdPattern = GavSegmentPattern.MATCH_ALL;
        }
        final GavSegmentPattern artifactIdPattern;
        if (st.hasMoreTokens()) {
            artifactIdPattern = new GavSegmentPattern(st.nextToken());
        } else {
            artifactIdPattern = GavSegmentPattern.MATCH_ALL;
        }
        final GavSegmentPattern versionPattern;
        if (st.hasMoreTokens()) {
            versionPattern = new GavSegmentPattern(st.nextToken());
        } else {
            versionPattern = GavSegmentPattern.MATCH_ALL;
        }
        return new GavPattern(groupIdPattern, artifactIdPattern, versionPattern);
    }

    final GavSegmentPattern artifactIdPattern;
    final GavSegmentPattern groupIdPattern;
    private final transient String source;
    final GavSegmentPattern versionPattern;

    GavPattern(GavSegmentPattern groupIdPattern, GavSegmentPattern artifactIdPattern,
            GavSegmentPattern versionPattern) {
        super();
        this.groupIdPattern = groupIdPattern;
        this.artifactIdPattern = artifactIdPattern;
        this.versionPattern = versionPattern;

        StringBuilder source = new StringBuilder(groupIdPattern.getSource().length()
                + artifactIdPattern.getSource().length() + versionPattern.getSource().length() + 2);

        source.append(groupIdPattern.getSource());
        final boolean artifactMatchesAll = artifactIdPattern.matchesAll();
        final boolean versionMatchesAll = versionPattern.matchesAll();
        if (!versionMatchesAll) {
            source.append(DELIMITER).append(artifactIdPattern.getSource());
            source.append(DELIMITER).append(versionPattern.getSource());
        } else if (!artifactMatchesAll) {
            source.append(DELIMITER).append(artifactIdPattern.getSource());
        }
        this.source = source.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GavPattern other = (GavPattern) obj;
        return this.source.equals(other.source);
    }

    @Override
    public int hashCode() {
        return this.source.hashCode();
    }

    /**
     * Matches the given {@code groupId}, {@code artifactId}, {@code version} triple against this {@link GavPattern}.
     *
     * @param  groupId
     * @param  artifactId
     * @param  version
     * @return            {@code true} if this {@link GavPattern} matches the given {@code groupId}, {@code artifactId},
     *                    {@code version} triple and {@code false} otherwise
     */
    public boolean matches(String groupId, String artifactId, String version) {
        return groupIdPattern.matches(groupId) && //
                artifactIdPattern.matches(artifactId) && //
                versionPattern.matches(version);
    }

    /**
     * Matches the given {@code groupId}, {@code artifactId}, {@code version} triple against this {@link GavPattern}.
     *
     * @param  gav
     * @return     {@code true} if this {@link GavPattern} matches the given {@code groupId}, {@code artifactId},
     *             {@code version} triple and {@code false} otherwise
     *
     * @since      4.0.0
     */
    public boolean matches(Gav gav) {
        return groupIdPattern.matches(gav.getGroupId()) && //
                artifactIdPattern.matches(gav.getArtifactId()) && //
                versionPattern.matches(gav.getVersion());
    }

    /**
     * Matches the given {@code groupId}, {@code artifactId} pair against this {@link GavPattern} disregarding the
     * version part of the pattern.
     *
     * @param  groupId
     * @param  artifactId
     * @return            {@code true} if this {@link GavPattern} matches the given {@code groupId} and {@code artifactId}
     *                    (disregarding the version part of this {@link GavPattern}), and {@code false} otherwise
     */
    public boolean matches(String groupId, String artifactId) {
        return groupIdPattern.matches(groupId) && //
                artifactIdPattern.matches(artifactId);
    }

    /**
     * Matches the given {@code groupId}, {@code artifactId} pair against this {@link GavPattern} disregarding the
     * version part of the pattern.
     *
     * @param  ga
     * @return    {@code true} if this {@link GavPattern} matches the given {@code groupId} and {@code artifactId}
     *            (disregarding the version part of this {@link GavPattern}), and {@code false} otherwise
     *
     * @since     4.0.0
     */
    public boolean matches(Ga ga) {
        return groupIdPattern.matches(ga.getGroupId()) && //
                artifactIdPattern.matches(ga.getArtifactId());
    }

    @Override
    public String toString() {
        return source;
    }

    @Override
    public int compareTo(GavPattern other) {
        return this.source.compareTo(other.source);
    }

    /**
     * @return the {@link #groupIdPattern} and {@link #artifactIdPattern} as a new {@link Ga}
     */
    public Ga asWildcardGa() {
        return new Ga(groupIdPattern.toString(), artifactIdPattern.toString());
    }

    /**
     * Returns {@code true} if the given {@link Gav} matches this {@link GavPattern} or {@code false} otherwise.
     *
     * @param  gav the {@link Gav} to match against this {@link GavPattern}
     * @return     {@code true} if the given {@link Gav} matches this {@link GavPattern} or {@code false} otherwise
     */
    @Override
    public boolean test(Gav gav) {
        return matches(gav);
    }
}
