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
import org.l2x6.pom.tuner.model.GavPattern.GavSegmentPattern;

/**
 * A general purpose pattern for matching GAs (i.e. pairs consisting of {@code groupId} and {@code artifactId}).
 * <p>
 * To create a new {@link GaPattern}, use either {@link #of(String)} or {@link #builder()}, both of which accept
 * wildcard patterns (rather than regular expression patterns). See the JavaDocs of the two respective methods for more
 * details.
 * <p>
 * {@link GaPattern} overrides {@link #hashCode()} and {@link #equals(Object)} and can thus be used as a key in a
 * {@link Map}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 *
 * @since  5.0.0
 */
public class GaPattern implements Serializable, Comparable<GaPattern>, Predicate<Ga> {

    /**
     * A {@link GaPattern} builder.
     */
    public static class Builder {

        private GavSegmentPattern artifactIdPattern = GavSegmentPattern.MATCH_ALL;
        private GavSegmentPattern groupIdPattern = GavSegmentPattern.MATCH_ALL;

        private Builder() {
        }

        /**
         * Sets the pattern for {@code artifactId}
         *
         * @param  wildcardPattern a pattern that can contain string literals and asterisk {@code *} wildcards
         * @return                 this {@link Builder}
         */
        public Builder artifactIdPattern(String wildcardPattern) {
            if (GavSegmentPattern.MATCH_ALL_PATTERN_SOURCE.equals(wildcardPattern)) {
                this.artifactIdPattern = GavSegmentPattern.MATCH_ALL;
            } else {
                this.artifactIdPattern = new GavSegmentPattern(wildcardPattern);
            }
            return this;
        }

        public GaPattern build() {
            if (groupIdPattern == GavSegmentPattern.MATCH_ALL && artifactIdPattern == GavSegmentPattern.MATCH_ALL) {
                return GaPattern.MATCH_ALL;
            }
            return new GaPattern(groupIdPattern, artifactIdPattern);
        }

        /**
         * Sets the pattern for {@code groupId}
         *
         * @param  wildcardPattern a pattern that can contain string literals and asterisk {@code *} wildcards
         * @return                 this {@link Builder}
         */
        public Builder groupIdPattern(String wildcardPattern) {
            if (GavSegmentPattern.MATCH_ALL_PATTERN_SOURCE.equals(wildcardPattern)) {
                this.groupIdPattern = GavSegmentPattern.MATCH_ALL;
            } else {
                this.groupIdPattern = new GavSegmentPattern(wildcardPattern);
            }
            return this;
        }

    }

    private static final GaPattern MATCH_ALL = new GaPattern(GavSegmentPattern.MATCH_ALL, GavSegmentPattern.MATCH_ALL);

    /**
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return a singleton that matches all possible GAs
     */
    public static GaPattern matchAll() {
        return MATCH_ALL;
    }

    /**
     * Creates a new {@link GaPattern} out of the given {@code wildcardPattern}. A wildcard pattern consists of string
     * literals and asterisk wildcard {@code *}. {@code *} matches zero or many arbitrary characters. Wildcard patterns
     * for groupId and artifactId need to be delimited by colon {@code :}.
     * <p>
     * GA pattern examples:
     * <p>
     * {@code org.my-group} - an equivalent of {@code org.my-group:*}. It will match any artifact
     * having groupId {@code org.my-group}.
     * <p>
     * {@code org.my-group*} - an equivalent of {@code org.my-group*:*}. It will match any artifact
     * whose groupId starts with {@code org.my-group} - i.e. it will match all of {@code org.my-group},
     * {@code org.my-group.api}, {@code org.my-group.impl}, etc.
     * <p>
     * {@code org.my-group:my-artifact} - will match all such artifacts that have groupId {@code org.my-group}
     * and artifactId {@code my-artifact}
     *
     * @param  wildcardPattern a string pattern to parse and create a new {@link GaPattern} from
     * @return                 a new {@link GaPattern}
     */
    public static GaPattern of(String wildcardPattern) {
        final GavSegmentPattern groupIdPattern;
        StringTokenizer st = new StringTokenizer(wildcardPattern, GavPattern.DELIMITER_STRING);
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
        return new GaPattern(groupIdPattern, artifactIdPattern);
    }

    final GavSegmentPattern artifactIdPattern;
    final GavSegmentPattern groupIdPattern;
    private final transient String source;

    GaPattern(GavSegmentPattern groupIdPattern, GavSegmentPattern artifactIdPattern) {
        super();
        this.groupIdPattern = groupIdPattern;
        this.artifactIdPattern = artifactIdPattern;

        StringBuilder source = new StringBuilder(groupIdPattern.getSource().length()
                + artifactIdPattern.getSource().length() + 1);

        source.append(groupIdPattern.getSource());
        if (!artifactIdPattern.matchesAll()) {
            source.append(GavPattern.DELIMITER).append(artifactIdPattern.getSource());
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
        GaPattern other = (GaPattern) obj;
        return this.source.equals(other.source);
    }

    @Override
    public int hashCode() {
        return this.source.hashCode();
    }

    /**
     * Matches the given {@code groupId}, {@code artifactId} pair against this {@link GaPattern}.
     *
     * @param  groupId
     * @param  artifactId
     * @return            {@code true} if this {@link GaPattern} matches the given {@code groupId} and {@code artifactId},
     *                    and {@code false} otherwise
     */
    public boolean matches(String groupId, String artifactId) {
        return groupIdPattern.matches(groupId) && //
                artifactIdPattern.matches(artifactId);
    }

    /**
     * Matches the given {@code groupId}, {@code artifactId} pair against this {@link GaPattern}.
     *
     * @param  ga
     * @return    {@code true} if this {@link GaPattern} matches the given {@code groupId} and {@code artifactId},
     *            and {@code false} otherwise
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
    public int compareTo(GaPattern other) {
        return this.source.compareTo(other.source);
    }

    /**
     * @return the {@link #groupIdPattern} and {@link #artifactIdPattern} as a new {@link Ga}
     */
    public Ga asWildcardGa() {
        return new Ga(groupIdPattern.toString(), artifactIdPattern.toString());
    }

    /**
     * Returns {@code true} if the given {@link Ga} matches this {@link GaPattern} or {@code false} otherwise.
     *
     * @param  ga the {@link Ga} to match against this {@link GaPattern}
     * @return    {@code true} if the given {@link Ga} matches this {@link GaPattern} or {@code false} otherwise
     */
    @Override
    public boolean test(Ga ga) {
        return matches(ga);
    }

    /**
     * @return the wildcard source of the {@code artifactId} pattern
     */
    public String getArtifactIdPattern() {
        return artifactIdPattern.getSource();
    }

    /**
     * @return the wildcard source of the {@code groupId} pattern
     */
    public String getGroupIdPattern() {
        return groupIdPattern.getSource();
    }

    /**
     * Creates a new {@link GaPattern} out of the given {@code groupIdPattern} and {@code artifactIdPattern}.
     *
     * @param  groupIdPattern    a wildcard pattern for {@code groupId}
     * @param  artifactIdPattern a wildcard pattern for {@code artifactId}
     * @return                   a new {@link GaPattern}
     */
    public static GaPattern of(String groupIdPattern, String artifactIdPattern) {
        return builder().groupIdPattern(groupIdPattern).artifactIdPattern(artifactIdPattern).build();
    }
}
