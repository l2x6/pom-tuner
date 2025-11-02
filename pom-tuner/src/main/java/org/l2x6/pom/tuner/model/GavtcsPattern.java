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
 * A general purpose pattern for matching sextuples consisting of {@code groupId}, {@code artifactId},
 * {@code version}, {@code type}, {@code classifier} and {@code scope}).
 * <p>
 * To create a new {@link GavtcsPattern}, use either {@link #of(String)} or {@link #builder()}, both of which accept
 * wildcard patterns (rather than regular expression patterns). See the JavaDocs of the two respective methods for more
 * details.
 * <p>
 * {@link GavtcsPattern} overrides {@link #hashCode()} and {@link #equals(Object)} and can thus be used as a key in a
 * {@link Map}.
 *
 * @since  4.5.0
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class GavtcsPattern implements Serializable, Comparable<GavtcsPattern>, Predicate<Gavtcs> {

    /**
     * A {@link GavtcsPattern} builder.
     */
    public static class Builder {

        private GavSegmentPattern artifactIdPattern = GavSegmentPattern.MATCH_ALL;
        private GavSegmentPattern groupIdPattern = GavSegmentPattern.MATCH_ALL;
        private GavSegmentPattern versionPattern = GavSegmentPattern.MATCH_ALL;
        private GavSegmentPattern typePattern = GavSegmentPattern.MATCH_ALL;
        private GavSegmentPattern classifierPattern = GavSegmentPattern.MATCH_ALL;
        private GavSegmentPattern scopePattern = GavSegmentPattern.MATCH_ALL;

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

        public GavtcsPattern build() {
            return new GavtcsPattern(groupIdPattern, artifactIdPattern, versionPattern, typePattern, classifierPattern,
                    scopePattern);
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

        /**
         * Sets the pattern for {@code type}
         *
         * @param  wildcardPattern a pattern that can contain string literals and asterisk {@code *} wildcards
         * @return                 this {@link Builder}
         */
        public Builder typePattern(String wildcardPattern) {
            this.typePattern = new GavSegmentPattern(wildcardPattern);
            return this;
        }

        /**
         * Sets the pattern for {@code classifier}
         *
         * @param  wildcardPattern a pattern that can contain string literals and asterisk {@code *} wildcards
         * @return                 this {@link Builder}
         */
        public Builder classifierPattern(String wildcardPattern) {
            this.classifierPattern = new GavSegmentPattern(wildcardPattern);
            return this;
        }

        /**
         * Sets the pattern for {@code scope}
         *
         * @param  wildcardPattern a pattern that can contain string literals and asterisk {@code *} wildcards
         * @return                 this {@link Builder}
         */
        public Builder scopePattern(String wildcardPattern) {
            this.scopePattern = new GavSegmentPattern(wildcardPattern);
            return this;
        }

    }

    private static final GavtcsPattern MATCH_ALL;
    private static final GavtcsPattern MATCH_SNAPSHOTS;
    private static final long serialVersionUID = 5570763687443531797L;

    static {
        MATCH_ALL = new GavtcsPattern(GavSegmentPattern.MATCH_ALL, GavSegmentPattern.MATCH_ALL,
                GavSegmentPattern.MATCH_ALL, GavSegmentPattern.MATCH_ALL, GavSegmentPattern.MATCH_ALL,
                GavSegmentPattern.MATCH_ALL);
        MATCH_SNAPSHOTS = new GavtcsPattern(GavSegmentPattern.MATCH_ALL, GavSegmentPattern.MATCH_ALL,
                new GavSegmentPattern(GavPattern.MULTI_WILDCARD + GavPattern.SNAPSHOT_SUFFIX), GavSegmentPattern.MATCH_ALL,
                GavSegmentPattern.MATCH_ALL, GavSegmentPattern.MATCH_ALL);
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
    public static GavtcsPattern matchAll() {
        return MATCH_ALL;
    }

    /**
     * @return a singleton that matches any GAV that has a version ending with {@value #SNAPSHOT_SUFFIX}
     */
    public static GavtcsPattern matchSnapshots() {
        return MATCH_SNAPSHOTS;
    }

    /**
     * Creates a new {@link GavtcsPattern} out of the given {@code wildcardPattern}. A wildcard pattern consists of string
     * literals and asterisk wildcard {@code *}. {@code *} matches zero or many arbitrary characters. Wildcard patterns
     * for {@code groupId}, {@code artifactId}, {@code version}, {@code type}, {@code classifier},
     * and {@code scope} need to be delimited by colon {@value GavPattern#DELIMITER}.
     * <p>
     * GAV pattern examples:
     * <p>
     * {@code org.my-group} - an equivalent of {@code org.my-group:*:*:*:*:*}. It will match any scope, any classifier,
     * any type, any version of any artifact having groupId {@code org.my-group}.
     * <p>
     * {@code org.my-group*} - an equivalent of {@code org.my-group*:*:*:*:*:*}. It will match any version of any scope,
     * any classifier, any type, any artifact whose groupId starts with {@code org.my-group} - i.e. it will match all of
     * {@code org.my-group}, {@code org.my-group.api}, {@code org.my-group.impl}, etc.
     * <p>
     * {@code org.my-group:my-artifact} - an equivalent of {@code org.my-group:my-artifact:*:*:*:*}. It will match any
     * scope, any classifier, any type, any version of all such artifacts that have groupId {@code org.my-group} and
     * artifactId {@code my-artifact}
     * <p>
     * {@code org.my-group:my-artifact:1.2.3} - will match any scope, any classifier, any type of the version 1.2.3 of
     * artifacts {@code org.my-group:my-artifact}.
     * <p>
     * {@code org.my-group:my-artifact:1.2.3} - will match any scope, any classifier, any type of the version 1.2.3 of
     * artifacts {@code org.my-group:my-artifact}.
     * <p>
     * {@code org.my-group:my-artifact:1.2.3:jar:x86_64:compile} - will match scope {@code compile}, classifier
     * {@code x86_64}, type {@code jar}, version 1.2.3 of artifacts {@code org.my-group:my-artifact}.
     *
     * @param  wildcardPattern a string pattern to parse and create a new {@link GavtcsPattern} from
     * @return                 a new {@link GavtcsPattern}
     */
    public static GavtcsPattern of(String wildcardPattern) {
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
        final GavSegmentPattern versionPattern;
        if (st.hasMoreTokens()) {
            versionPattern = new GavSegmentPattern(st.nextToken());
        } else {
            versionPattern = GavSegmentPattern.MATCH_ALL;
        }
        final GavSegmentPattern typePattern;
        if (st.hasMoreTokens()) {
            typePattern = new GavSegmentPattern(st.nextToken());
        } else {
            typePattern = GavSegmentPattern.MATCH_ALL;
        }
        final GavSegmentPattern classifierPattern;
        if (st.hasMoreTokens()) {
            classifierPattern = new GavSegmentPattern(st.nextToken());
        } else {
            classifierPattern = GavSegmentPattern.MATCH_ALL;
        }
        final GavSegmentPattern scopePattern;
        if (st.hasMoreTokens()) {
            scopePattern = new GavSegmentPattern(st.nextToken());
        } else {
            scopePattern = GavSegmentPattern.MATCH_ALL;
        }

        return new GavtcsPattern(groupIdPattern, artifactIdPattern, versionPattern, typePattern, classifierPattern,
                scopePattern);
    }

    final GavSegmentPattern artifactIdPattern;
    final GavSegmentPattern groupIdPattern;
    private final transient String source;
    final GavSegmentPattern versionPattern;
    final GavSegmentPattern typePattern;
    final GavSegmentPattern classifierPattern;
    final GavSegmentPattern scopePattern;

    GavtcsPattern(
            GavSegmentPattern groupIdPattern,
            GavSegmentPattern artifactIdPattern,
            GavSegmentPattern versionPattern,
            GavSegmentPattern typePattern,
            GavSegmentPattern classifierPattern,
            GavSegmentPattern scopePattern) {
        super();
        this.groupIdPattern = groupIdPattern;
        this.artifactIdPattern = artifactIdPattern;
        this.versionPattern = versionPattern;
        this.typePattern = typePattern;
        this.classifierPattern = classifierPattern;
        this.scopePattern = scopePattern;

        StringBuilder source = new StringBuilder(
                groupIdPattern.getSource().length()
                        + artifactIdPattern.getSource().length()
                        + versionPattern.getSource().length()
                        + typePattern.getSource().length()
                        + classifierPattern.getSource().length()
                        + scopePattern.getSource().length()
                        + 5);

        source.append(groupIdPattern.getSource());
        final boolean artifactMatchesAll = artifactIdPattern.matchesAll();
        final boolean versionMatchesAll = versionPattern.matchesAll();
        final boolean typeMatchesAll = typePattern.matchesAll();
        final boolean classifierMatchesAll = classifierPattern.matchesAll();
        final boolean scopeMatchesAll = scopePattern.matchesAll();
        if (!scopeMatchesAll) {
            source.append(GavPattern.DELIMITER).append(artifactIdPattern.getSource());
            source.append(GavPattern.DELIMITER).append(versionPattern.getSource());
            source.append(GavPattern.DELIMITER).append(typePattern.getSource());
            source.append(GavPattern.DELIMITER).append(classifierPattern.getSource());
            source.append(GavPattern.DELIMITER).append(scopePattern.getSource());
        } else if (!classifierMatchesAll) {
            source.append(GavPattern.DELIMITER).append(artifactIdPattern.getSource());
            source.append(GavPattern.DELIMITER).append(versionPattern.getSource());
            source.append(GavPattern.DELIMITER).append(typePattern.getSource());
            source.append(GavPattern.DELIMITER).append(classifierPattern.getSource());
        } else if (!typeMatchesAll) {
            source.append(GavPattern.DELIMITER).append(artifactIdPattern.getSource());
            source.append(GavPattern.DELIMITER).append(versionPattern.getSource());
            source.append(GavPattern.DELIMITER).append(typePattern.getSource());
        } else if (!versionMatchesAll) {
            source.append(GavPattern.DELIMITER).append(artifactIdPattern.getSource());
            source.append(GavPattern.DELIMITER).append(versionPattern.getSource());
        } else if (!artifactMatchesAll) {
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
        GavtcsPattern other = (GavtcsPattern) obj;
        return this.source.equals(other.source);
    }

    @Override
    public int hashCode() {
        return this.source.hashCode();
    }

    /**
     * Matches the given {@code groupId}, {@code artifactId}, {@code version}, {@code type}, {@code classifier},
     * {@code scope} sextuple against this {@link GavtcsPattern}.
     *
     * @param  groupId
     * @param  artifactId
     * @param  version
     * @param  type
     * @param  classifier
     * @param  scope
     * @return            {@code true} if this {@link GavtcsPattern} matches the given parameters and {@code false}
     *                    otherwise
     *
     * @since             4.5.0
     */
    public boolean matches(
            String groupId,
            String artifactId,
            String version,
            String type,
            String classifier,
            String scope) {
        return groupIdPattern.matches(groupId) && //
                artifactIdPattern.matches(artifactId) && //
                versionPattern.matches(version) && //
                typePattern.matches(Gavtcs.toEffectiveType(type)) && //
                classifierPattern.matches(classifier == null ? "" : classifier) && //
                scopePattern.matches(Gavtcs.toEffectiveScope(scope));
    }

    /**
     * Matches the given {@code groupId}, {@code artifactId}, {@code version}, {@code type}, {@code classifier},
     * {@code scope} sextuple against this {@link GavtcsPattern}.
     *
     * @param  gavtcs
     * @return        {@code true} if this {@link GavtcsPattern} matches the given parameters and {@code false} otherwise
     *
     * @since         4.5.0
     */
    public boolean matches(Gavtcs gavtcs) {
        return groupIdPattern.matches(gavtcs.getGroupId()) && //
                artifactIdPattern.matches(gavtcs.getArtifactId()) && //
                versionPattern.matches(gavtcs.getVersion()) && //
                typePattern.matches(gavtcs.getType()) && //
                classifierPattern.matches(gavtcs.getClassifier()) && //
                scopePattern.matches(gavtcs.getScope());
    }

    /**
     * Matches the given {@code groupId}, {@code artifactId}, {@code version}, {@code type}, {@code classifier} pentuple against this {@link GavtcsPattern}.
     *
     * @param  groupId
     * @param  artifactId
     * @param  version
     * @param  type
     * @param  classifier
     * @return            {@code true} if this {@link GavtcsPattern} matches the given parameters and {@code false}
     *                    otherwise
     *
     * @since             4.8.0
     */
    public boolean matches(
            String groupId,
            String artifactId,
            String version,
            String type,
            String classifier) {
        return groupIdPattern.matches(groupId) && //
                artifactIdPattern.matches(artifactId) && //
                versionPattern.matches(version) && //
                typePattern.matches(Gavtcs.toEffectiveType(type)) && //
                classifierPattern.matches(classifier == null ? "" : classifier);
    }

    /**
     * Matches the given {@code groupId}, {@code artifactId}, {@code version}, {@code type}, {@code classifier}
     * pentuple against this {@link GavtcsPattern}.
     *
     * @param  gavtc
     * @return       {@code true} if this {@link GavtcsPattern} matches the given parameters and {@code false} otherwise
     *
     * @since        4.8.0
     */
    public boolean matches(Gavtc gavtc) {
        return groupIdPattern.matches(gavtc.getGroupId()) && //
                artifactIdPattern.matches(gavtc.getArtifactId()) && //
                versionPattern.matches(gavtc.getVersion()) && //
                typePattern.matches(gavtc.getType()) && //
                classifierPattern.matches(gavtc.getClassifier());
    }

    /**
     * Matches the given {@code groupId}, {@code artifactId}, {@code version} triple against this {@link GavPattern}
     * disregarding the type, classifier and scope parts of the pattern.
     *
     * @param  groupId
     * @param  artifactId
     * @param  version
     * @return            {@code true} if this {@link GavPattern} matches the given {@code groupId}, {@code artifactId},
     *                    {@code version} triple and {@code false} otherwise
     * @since             4.5.0
     */
    public boolean matches(String groupId, String artifactId, String version) {
        return groupIdPattern.matches(groupId) && //
                artifactIdPattern.matches(artifactId) && //
                versionPattern.matches(version);
    }

    /**
     * Matches the given {@code groupId}, {@code artifactId}, {@code version} triple against this {@link GavPattern}
     * disregarding the type, classifier and scope parts of the pattern.
     *
     * @param  gav
     * @return     {@code true} if this {@link GavPattern} matches the given {@code groupId}, {@code artifactId},
     *             {@code version} triple and {@code false} otherwise
     *
     * @since      4.5.0
     */
    public boolean matches(Gav gav) {
        return groupIdPattern.matches(gav.getGroupId()) && //
                artifactIdPattern.matches(gav.getArtifactId()) && //
                versionPattern.matches(gav.getVersion());
    }

    /**
     * Matches the given {@code groupId}, {@code artifactId} pair against this {@link GavtcsPattern} disregarding the
     * version, type, classifier and scope parts of the pattern.
     *
     * @param  groupId
     * @param  artifactId
     * @return            {@code true} if this {@link GavtcsPattern} matches the given {@code groupId} and
     *                    {@code artifactId}
     *                    (disregarding the version part of this {@link GavtcsPattern}), and {@code false} otherwise
     * @since             4.5.0
     */
    public boolean matches(String groupId, String artifactId) {
        return groupIdPattern.matches(groupId) && //
                artifactIdPattern.matches(artifactId);
    }

    /**
     * Matches the given {@code groupId}, {@code artifactId} pair against this {@link GavtcsPattern} disregarding the
     * version, type, classifier and scope parts of the pattern.
     *
     * @param  ga
     * @return    {@code true} if this {@link GavtcsPattern} matches the given {@code groupId} and {@code artifactId}
     *            (disregarding the version part of this {@link GavtcsPattern}), and {@code false} otherwise
     *
     * @since     4.5.0
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
    public int compareTo(GavtcsPattern other) {
        return this.source.compareTo(other.source);
    }

    /**
     * @return the {@link #groupIdPattern} and {@link #artifactIdPattern} as a new {@link Ga}
     */
    public Ga asWildcardGa() {
        return new Ga(groupIdPattern.toString(), artifactIdPattern.toString());
    }

    /**
     * @return the {@link #groupIdPattern}, {@link #artifactIdPattern} and {@link #versionPattern} as a new {@link Ga}
     */
    public Gav asWildcardGav() {
        return new Gav(groupIdPattern.toString(), artifactIdPattern.toString(), versionPattern.toString());
    }

    /**
     * @return the {@link #groupIdPattern}, {@link #artifactIdPattern} and {@link #versionPattern} as a new {@link Ga}
     */
    public GavPattern toGavPattern() {
        return new GavPattern(groupIdPattern, artifactIdPattern, versionPattern);
    }

    /**
     * Returns {@code true} if the given {@link Gavtcs} matches this {@link GavtcsPattern} or {@code false} otherwise.
     *
     * @param  gavtcs the {@link Gavtcs} to match against this {@link GavtcsPattern}
     * @return        {@code true} if the given {@link Gavtcs} matches this {@link GavtcsPattern} or {@code false} otherwise
     */
    @Override
    public boolean test(Gavtcs gavtcs) {
        return matches(gavtcs);
    }

}
