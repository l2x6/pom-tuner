/**
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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import org.l2x6.pom.tuner.model.GavSet.IncludeExcludeGavSet.Builder;

/**
 * A set of {@link Gav}s defined by included and excluded {@link GavPattern}s.
 * <p>
 * Historical note: before version 4.0.0, {@link GavSet} used to be a class that is now {@link IncludeExcludeGavSet}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public interface GavSet {

    public static Builder builder() {
        return new Builder();
    }

    public static UnionGavSet.Builder unionBuilder() {
        return new UnionGavSet.Builder();
    }

    public static GavSet includeAll() {
        return IncludeExcludeGavSet.INCLUDE_ALL;
    }

    /**
     * @param  gavSets the {@link GavSet}s to union
     * @return         a {@link GavSet} that is a union of the given {@code gavSets}
     *
     * @since          4.0.0
     */
    public static GavSet union(Collection<GavSet> gavSets) {
        if (gavSets == null || gavSets.isEmpty()) {
            return IncludeExcludeGavSet.INCLUDE_ALL;
        }
        return new UnionGavSet(new ArrayList<>(gavSets));
    }

    /**
     * @param  gavSets the {@link GavSet}s to union
     * @return         a {@link GavSet} that is a union of the given {@code gavSets}
     *
     * @since          4.0.0
     */
    public static GavSet union(GavSet... gavSets) {
        if (gavSets == null || gavSets.length == 0) {
            return IncludeExcludeGavSet.INCLUDE_ALL;
        }
        return new UnionGavSet(new ArrayList<>(Arrays.asList(gavSets)));
    }

    /**
     *
     * @param  groupId
     * @param  artifactId
     * @return            {@code true} if the given GA identifier is a member of this {@link GavSet} and {@code false}
     *                    otherwise
     */
    boolean contains(String groupId, String artifactId);

    /**
     * Shorthand for {@code contains(ga.getGrooupId(), ga.getArtifactId())}.
     *
     * @param  ga the groupId and artiafctId to check for membership in this gavSet
     * @return    {@code true} if the given {@link Ga} is a member of this {@link GavSet} and {@code false}
     *            otherwise
     *
     * @since     4.0.0
     */
    default boolean contains(Ga ga) {
        return contains(ga.getGroupId(), ga.getArtifactId());
    }

    /**
     *
     * @param  groupId
     * @param  artifactId
     * @param  version
     * @return            {@code true} if the given GAV triple is a member of this {@link GavSet} and {@code false}
     *                    otherwise
     */
    boolean contains(String groupId, String artifactId, String version);

    /**
     * Shorthand for {@code contains(gav.getGrooupId(), gav.getArtifactId(), gav.getVersion())}.
     *
     * @param  gav the groupId, artiafctId and version to check for membership in this gavSet
     * @return     {@code true} if the given {@link Gav} is a member of this {@link GavSet} and {@code false}
     *             otherwise
     *
     * @since      4.0.0
     */
    default boolean contains(Gav gav) {
        return contains(gav.getGroupId(), gav.getArtifactId(), gav.getVersion());
    }

    /**
     * Unions {@code this} {@link GavSet} with the given {@code other} {@link GavSet}
     *
     * @param  other the {@link GavSet} to union with
     * @return       if {@code this} is equal to {@code other} returns {@code this}; otherwise returns a new {@link GavSet}
     *               that is a union of {@code this} {@link GavSet} and the given {@code other}
     *               {@link GavSet}
     *
     * @since        4.0.0
     */
    default GavSet union(GavSet other) {
        if (this.equals(other)) {
            return this;
        }
        return new UnionGavSet(this, other);
    }

    public static class UnionGavSet implements GavSet, Serializable {
        private static final long serialVersionUID = 6946413843688129003L;

        private final List<GavSet> gavSets;

        UnionGavSet(GavSet... gavSets) {
            this.gavSets = Arrays.asList(gavSets);
        }

        UnionGavSet(List<GavSet> gavSets) {
            this.gavSets = gavSets;
        }

        @Override
        public boolean contains(String groupId, String artifactId) {
            return gavSets.stream().anyMatch(gavSet -> gavSet.contains(groupId, artifactId));
        }

        @Override
        public boolean contains(String groupId, String artifactId, String version) {
            return gavSets.stream().anyMatch(gavSet -> gavSet.contains(groupId, artifactId, version));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((gavSets == null) ? 0 : gavSets.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            UnionGavSet other = (UnionGavSet) obj;
            if (gavSets == null) {
                if (other.gavSets != null)
                    return false;
            } else if (!gavSets.equals(other.gavSets))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "UnionGavSet [" + gavSets + "]";
        }

        public static class Builder {
            List<GavSet> gavSets = new ArrayList<>();

            public Builder union(GavSet gavSet) {
                gavSets.add(gavSet);
                return this;
            }

            public GavSet build() {
                final List<GavSet> gs = gavSets;
                if (gs.isEmpty()) {
                    return IncludeExcludeGavSet.INCLUDE_ALL;
                }
                gavSets = null;
                return new UnionGavSet(Collections.unmodifiableList(gs));
            }
        }

    }

    public static class IncludeExcludeGavSet implements GavSet, Serializable {
        public static class Builder {
            private List<GavPattern> excludes = new ArrayList<>();
            private List<GavPattern> includes = new ArrayList<>();

            private Builder() {
            }

            public GavSet build() {
                if (includes.isEmpty()) {
                    includes.add(GavPattern.matchAll());
                }

                List<GavPattern> useIncludes = Collections.unmodifiableList(includes);
                List<GavPattern> useExcludes = Collections.unmodifiableList(excludes);

                this.includes = null;
                this.excludes = null;

                return new IncludeExcludeGavSet(useIncludes, useExcludes);
            }

            /**
             * Exclude a single GAV pattern.
             *
             * @param  rawPattern
             * @return            this {@link Builder}
             */
            public Builder exclude(String rawPattern) {
                this.excludes.add(GavPattern.of(rawPattern));
                return this;
            }

            /**
             * Parses the entries of the given {@link Collection} of {@code rawPatterns} and excludes those.
             *
             * @param  rawPatterns {@link Collection} of GAV patterns to parse via {@link GavPattern#of(String)}
             * @return             this {@link Builder}
             */
            public Builder excludes(Collection<String> rawPatterns) {
                if (rawPatterns != null) {
                    for (String rawPattern : rawPatterns) {
                        this.excludes.add(GavPattern.of(rawPattern));
                    }
                }
                return this;
            }

            /**
             * Parses the entries of the given array of {@code rawPatterns} and excludes those.
             *
             * @param  rawPatterns a list of GAV patterns to parse via {@link GavPattern#of(String)}
             * @return             this {@link Builder}
             */
            public Builder excludes(String... rawPatterns) {
                if (rawPatterns != null) {
                    for (String rawPattern : rawPatterns) {
                        this.excludes.add(GavPattern.of(rawPattern));
                    }
                }
                return this;
            }

            /**
             * Parses the given comma or whitespace separated list of {@code rawPatterns} and excludes those.
             *
             * @param  rawPatterns a comma separated list of GAV patterns
             * @return             this {@link Builder}
             */
            public Builder excludes(String rawPatterns) {
                if (rawPatterns != null) {
                    StringTokenizer st = new StringTokenizer(rawPatterns, ", \t\n\r");
                    while (st.hasMoreTokens()) {
                        this.excludes.add(GavPattern.of(st.nextToken()));
                    }
                }
                return this;
            }

            /**
             * Adds {@link GavPattern#matchSnapshots()} to {@link #excludes}.
             *
             * @return this {@link Builder}
             */
            public Builder excludeSnapshots() {
                this.excludes.add(GavPattern.matchSnapshots());
                return this;
            }

            /**
             * Include a single GAV pattern.
             *
             * @param  rawPattern
             * @return            this {@link Builder}
             */
            public Builder include(String rawPattern) {
                this.includes.add(GavPattern.of(rawPattern));
                return this;
            }

            /**
             * Parses the entries of the given {@link Collection} of {@code rawPatterns} and includes those.
             *
             * @param  rawPatterns {@link Collection} of GAV patterns to parse via {@link GavPattern#of(String)}
             * @return             this {@link Builder}
             */
            public Builder includes(Collection<String> rawPatterns) {
                if (rawPatterns != null) {
                    for (String rawPattern : rawPatterns) {
                        this.includes.add(GavPattern.of(rawPattern));
                    }
                }
                return this;
            }

            /**
             * Parses the given comma or whitespace separated list of {@code rawPatterns} and includes those.
             *
             * @param  rawPatterns a comma separated list of GAV patterns
             * @return             this {@link Builder}
             */
            public Builder includes(String rawPatterns) {
                if (rawPatterns != null) {
                    StringTokenizer st = new StringTokenizer(rawPatterns, ", \t\n\r");
                    while (st.hasMoreTokens()) {
                        this.includes.add(GavPattern.of(st.nextToken()));
                    }
                }
                return this;
            }

            /**
             * Parses the entries of the given array of {@code rawPatterns} and includes those.
             *
             * @param  rawPatterns a list of GAV patterns to parse via {@link GavPattern#of(String)}
             * @return             this {@link Builder}
             */
            public Builder includes(String... rawPatterns) {
                if (rawPatterns != null) {
                    for (String rawPattern : rawPatterns) {
                        this.includes.add(GavPattern.of(rawPattern));
                    }
                }
                return this;
            }

        }

        private static final List<GavPattern> EMPTY_LIST = Collections.emptyList();

        private static final GavSet INCLUDE_ALL = new IncludeExcludeGavSet(Collections.singletonList(GavPattern.matchAll()),
                EMPTY_LIST);
        /**  */
        private static final long serialVersionUID = 4495169649760950618L;

        private static void append(List<GavPattern> cludes, Appendable out) throws IOException {
            boolean first = true;
            for (GavPattern gavPattern : cludes) {
                if (first) {
                    first = false;
                } else {
                    out.append(',');
                }
                out.append(gavPattern.toString());
            }
        }

        private static boolean matches(String groupId, String artifactId, List<GavPattern> patterns) {
            for (GavPattern pattern : patterns) {
                if (pattern.matches(groupId, artifactId)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean matches(String groupId, String artifactId, String version, List<GavPattern> patterns) {
            for (GavPattern pattern : patterns) {
                if (pattern.matches(groupId, artifactId, version)) {
                    return true;
                }
            }
            return false;
        }

        private final List<GavPattern> excludes;
        private final transient int hashcode;;

        private final List<GavPattern> includes;

        IncludeExcludeGavSet(List<GavPattern> includes, List<GavPattern> excludes) {
            super();
            this.includes = includes;
            this.excludes = excludes;
            this.hashcode = 31 * (31 * 1 + excludes.hashCode()) + includes.hashCode();
        }

        /**
         * Appends {@link #excludes} to the given {@code out} separating them by comma.
         *
         * @param  out         an {@link Appendable} to append to
         * @throws IOException
         */
        public void appendExcludes(Appendable out) throws IOException {
            append(excludes, out);
        }

        /**
         * Appends {@link #includes} to the given {@code out} separating them by comma.
         *
         * @param  out         an {@link Appendable} to append to
         * @throws IOException
         */
        public void appendIncludes(Appendable out) throws IOException {
            append(includes, out);
        }

        /**
         *
         * @param  groupId
         * @param  artifactId
         * @return            {@code true} if the given GA identifier is a member of this {@link GavSet} and {@code false}
         *                    otherwise
         */
        public boolean contains(String groupId, String artifactId) {
            return matches(groupId, artifactId, includes) && !matches(groupId, artifactId, excludes);
        }

        /**
         *
         * @param  groupId
         * @param  artifactId
         * @param  version
         * @return            {@code true} if the given GAV triple is a member of this {@link GavSet} and {@code false}
         *                    otherwise
         */
        public boolean contains(String groupId, String artifactId, String version) {
            return matches(groupId, artifactId, version, includes) && !matches(groupId, artifactId, version, excludes);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            IncludeExcludeGavSet other = (IncludeExcludeGavSet) obj;
            if (excludes == null) {
                if (other.excludes != null)
                    return false;
            } else if (!excludes.equals(other.excludes))
                return false;
            if (includes == null) {
                if (other.includes != null)
                    return false;
            } else if (!includes.equals(other.includes))
                return false;
            return true;
        }

        /**
         * @return the list of excludes
         */
        public List<GavPattern> getExcludes() {
            return excludes;
        }

        /**
         * @return the list of includes
         */
        public List<GavPattern> getIncludes() {
            return includes;
        }

        @Override
        public int hashCode() {
            return hashcode;
        }

        @Override
        public String toString() {
            return "GavSet [excludes=" + excludes + ", includes=" + includes + "]";
        }

    }

}
