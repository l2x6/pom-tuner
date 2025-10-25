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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.function.Predicate;
import org.l2x6.pom.tuner.model.GavtcsSet.IncludeExcludeGavSet.Builder;

/**
 * A set of {@link Gavtcs}s defined by included and excluded {@link GavtcsPattern}s.
 * <p>
 * Historical note: before version 4.5.0, {@link GavtcsSet} used to be a class that is now {@link IncludeExcludeGavSet}.
 *
 * @since  4.5.0
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public interface GavtcsSet extends Predicate<Gavtcs> {

    public static Builder builder() {
        return new Builder();
    }

    public static UnionGavSet.Builder unionBuilder() {
        return new UnionGavSet.Builder();
    }

    public static GavtcsSet includeAll() {
        return IncludeExcludeGavSet.INCLUDE_ALL;
    }

    public static GavtcsSet excludeAll() {
        return IncludeExcludeGavSet.EXCLUDE_ALL;
    }

    /**
     * @param  gavtcsSets the {@link GavtcsSet}s to union
     * @return            a {@link GavtcsSet} that is a union of the given {@code gavtcsSets}
     *
     * @since             4.5.0
     */
    public static GavtcsSet union(Collection<GavtcsSet> gavtcsSets) {
        if (gavtcsSets == null || gavtcsSets.isEmpty()) {
            return IncludeExcludeGavSet.INCLUDE_ALL;
        }
        return new UnionGavSet(new ArrayList<>(gavtcsSets));
    }

    /**
     * @param  gavtcsSets the {@link GavtcsSet}s to union
     * @return            a {@link GavtcsSet} that is a union of the given {@code gavtcsSets}
     *
     * @since             4.5.0
     */
    public static GavtcsSet union(GavtcsSet... gavtcsSets) {
        if (gavtcsSets == null || gavtcsSets.length == 0) {
            return IncludeExcludeGavSet.INCLUDE_ALL;
        }
        return new UnionGavSet(new ArrayList<>(Arrays.asList(gavtcsSets)));
    }

    /**
     *
     * @param  groupId
     * @param  artifactId
     * @return            {@code true} if the given GA identifier is a member of this {@link GavtcsSet} and {@code false}
     *                    otherwise
     */
    boolean contains(String groupId, String artifactId);

    /**
     * Shorthand for {@code contains(ga.getGroupId(), ga.getArtifactId())}.
     *
     * @param  ga the groupId and artiafctId to check for membership in this gavtcsSet
     * @return    {@code true} if the given {@link Ga} is a member of this {@link GavtcsSet} and {@code false}
     *            otherwise
     *
     * @since     4.5.0
     */
    default boolean contains(Ga ga) {
        return contains(ga.getGroupId(), ga.getArtifactId());
    }

    /**
     *
     * @param  groupId
     * @param  artifactId
     * @param  version
     * @return            {@code true} if the given GAV triple is a member of this {@link GavtcsSet} and {@code false}
     *                    otherwise
     */
    boolean contains(String groupId, String artifactId, String version);

    /**
     * Shorthand for {@code contains(gav.getGrooupId(), gav.getArtifactId(), gav.getVersion())}.
     *
     * @param  gav the groupId, artiafctId and version to check for membership in this gavtcsSet
     * @return     {@code true} if the given {@link Gav} is a member of this {@link GavtcsSet} and {@code false}
     *             otherwise
     *
     * @since      4.5.0
     */
    default boolean contains(Gav gav) {
        return contains(gav.getGroupId(), gav.getArtifactId(), gav.getVersion());
    }

    /**
     * An implementation of {@link Predicate#test(Object)} Delegates to {@link #contains(Gav)}.
     *
     * @param  gav the groupId, artiafctId and version to check for membership in this {@link GavSet}
     * @return     {@code true} if the given {@link Gav} is a member of this {@link GavSet} and {@code false}
     *             otherwise
     *
     * @since      4.7.0
     */
    @Override
    default boolean test(Gavtcs gav) {
        return contains(gav);
    }

    /**
     *
     * @param  groupId
     * @param  artifactId
     * @param  version
     * @param  type
     * @param  classifier
     * @param  scope
     * @return            {@code true} if the given given {@code groupId}, {@code artifactId}, {@code version},
     *                    {@code type}, {@code classifier},
     *                    {@code scope} sextuple is a member of this {@link GavtcsSet} and {@code false}
     *                    otherwise
     * @since             4.5.0
     */
    boolean contains(String groupId, String artifactId, String version, String type, String classifier, String scope);

    /**
     * Shorthand for {@code contains(gav.getGrooupId(), gav.getArtifactId(), gav.getVersion())}.
     *
     * @param  gavtcs the groupId, artiafctId and version to check for membership in this gavtcsSet
     * @return        {@code true} if the given {@link Gavtcs} is a member of this {@link GavtcsSet} and {@code false}
     *                otherwise
     *
     * @since         4.5.0
     */
    default boolean contains(Gavtcs gavtcs) {
        return contains(gavtcs.getGroupId(), gavtcs.getArtifactId(), gavtcs.getVersion(), gavtcs.getType(),
                gavtcs.getClassifier(), gavtcs.getScope());
    }

    /**
     * Unions {@code this} {@link GavtcsSet} with the given {@code other} {@link GavtcsSet}
     *
     * @param  other the {@link GavtcsSet} to union with
     * @return       if {@code this} is equal to {@code other} returns {@code this}; otherwise returns a new
     *               {@link GavtcsSet}
     *               that is a union of {@code this} {@link GavtcsSet} and the given {@code other}
     *               {@link GavtcsSet}
     *
     * @since        4.5.0
     */
    default GavtcsSet union(GavtcsSet other) {
        if (this.equals(other)) {
            return this;
        }
        return new UnionGavSet(this, other);
    }

    public static class UnionGavSet implements GavtcsSet, Serializable {
        private static final long serialVersionUID = 6946413843688129003L;

        private final List<GavtcsSet> gavtcsSets;

        UnionGavSet(GavtcsSet... gavtcsSets) {
            this.gavtcsSets = Arrays.asList(gavtcsSets);
        }

        UnionGavSet(List<GavtcsSet> gavtcsSets) {
            this.gavtcsSets = gavtcsSets;
        }

        @Override
        public boolean contains(String groupId, String artifactId) {
            return gavtcsSets.stream().anyMatch(gavtcsSet -> gavtcsSet.contains(groupId, artifactId));
        }

        @Override
        public boolean contains(String groupId, String artifactId, String version) {
            return gavtcsSets.stream().anyMatch(gavtcsSet -> gavtcsSet.contains(groupId, artifactId, version));
        }

        @Override
        public boolean contains(String groupId, String artifactId, String version, String type, String classifier,
                String scope) {
            return gavtcsSets.stream().anyMatch(gavtcsSet -> gavtcsSet.contains(groupId, artifactId, version, type, classifier,
                    scope));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((gavtcsSets == null) ? 0 : gavtcsSets.hashCode());
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
            if (gavtcsSets == null) {
                if (other.gavtcsSets != null)
                    return false;
            } else if (!gavtcsSets.equals(other.gavtcsSets))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "UnionGavSet [" + gavtcsSets + "]";
        }

        public static class Builder {
            private List<GavtcsSet> gavtcsSets = new ArrayList<>();
            private GavtcsSet defaultResult = IncludeExcludeGavSet.INCLUDE_ALL;

            public Builder union(GavtcsSet gavtcsSet) {
                gavtcsSets.add(gavtcsSet);
                return this;
            }

            /**
             * @param  gavtcsSet a {@link GavtcsSet} to return from {@link #build()} method in case no {@link GavtcsSet}s were
             *                   added via
             *                   {@link #union(GavtcsSet)}
             * @return           {@code this}
             */
            public Builder defaultResult(GavtcsSet gavtcsSet) {
                this.defaultResult = gavtcsSet;
                return this;
            }

            public GavtcsSet build() {
                final List<GavtcsSet> gs = gavtcsSets;
                if (gs.isEmpty()) {
                    return defaultResult;
                }
                gavtcsSets = null;
                return new UnionGavSet(Collections.unmodifiableList(gs));
            }
        }

    }

    public static class IncludeExcludeGavSet implements GavtcsSet, Serializable {
        public static class Builder {
            private List<GavtcsPattern> excludes = new ArrayList<>();
            private List<GavtcsPattern> includes = new ArrayList<>();
            private GavtcsSet defaultResult = IncludeExcludeGavSet.INCLUDE_ALL;

            private Builder() {
            }

            public GavtcsSet build() {
                if (includes.isEmpty() && excludes.isEmpty()) {
                    return defaultResult;
                }
                if (includes.isEmpty()) {
                    includes.add(GavtcsPattern.matchAll());
                }

                List<GavtcsPattern> useIncludes = Collections.unmodifiableList(includes);
                List<GavtcsPattern> useExcludes = Collections.unmodifiableList(excludes);

                this.includes = null;
                this.excludes = null;

                return new IncludeExcludeGavSet(useIncludes, useExcludes);
            }

            /**
             * @param  gavtcsSet a {@link GavtcsSet} to return from {@link #build()} method in case no includes or excludes were
             *                   added
             * @return           {@code this}
             */
            public Builder defaultResult(GavtcsSet gavtcsSet) {
                this.defaultResult = gavtcsSet;
                return this;
            }

            /**
             * Exclude a single GAV pattern.
             *
             * @param  rawPattern
             * @return            this {@link Builder}
             */
            public Builder exclude(String rawPattern) {
                this.excludes.add(GavtcsPattern.of(rawPattern));
                return this;
            }

            /**
             * Parses the entries of the given {@link Collection} of {@code rawPatterns} and excludes those.
             *
             * @param  rawPatterns {@link Collection} of GAV patterns to parse via {@link GavtcsPattern#of(String)}
             * @return             this {@link Builder}
             */
            public Builder excludes(Collection<String> rawPatterns) {
                if (rawPatterns != null) {
                    for (String rawPattern : rawPatterns) {
                        this.excludes.add(GavtcsPattern.of(rawPattern));
                    }
                }
                return this;
            }

            /**
             * Parses the entries of the given array of {@code rawPatterns} and excludes those.
             *
             * @param  rawPatterns a list of GAV patterns to parse via {@link GavtcsPattern#of(String)}
             * @return             this {@link Builder}
             */
            public Builder excludes(String... rawPatterns) {
                if (rawPatterns != null) {
                    for (String rawPattern : rawPatterns) {
                        this.excludes.add(GavtcsPattern.of(rawPattern));
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
                        this.excludes.add(GavtcsPattern.of(st.nextToken()));
                    }
                }
                return this;
            }

            /**
             * Adds {@link GavtcsPattern#matchSnapshots()} to {@link #excludes}.
             *
             * @return this {@link Builder}
             */
            public Builder excludeSnapshots() {
                this.excludes.add(GavtcsPattern.matchSnapshots());
                return this;
            }

            /**
             * Include a single GAV pattern.
             *
             * @param  rawPattern
             * @return            this {@link Builder}
             */
            public Builder include(String rawPattern) {
                this.includes.add(GavtcsPattern.of(rawPattern));
                return this;
            }

            /**
             * Parses the entries of the given {@link Collection} of {@code rawPatterns} and includes those.
             *
             * @param  rawPatterns {@link Collection} of GAV patterns to parse via {@link GavtcsPattern#of(String)}
             * @return             this {@link Builder}
             */
            public Builder includes(Collection<String> rawPatterns) {
                if (rawPatterns != null) {
                    for (String rawPattern : rawPatterns) {
                        this.includes.add(GavtcsPattern.of(rawPattern));
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
                        this.includes.add(GavtcsPattern.of(st.nextToken()));
                    }
                }
                return this;
            }

            /**
             * Parses the entries of the given array of {@code rawPatterns} and includes those.
             *
             * @param  rawPatterns a list of GAV patterns to parse via {@link GavtcsPattern#of(String)}
             * @return             this {@link Builder}
             */
            public Builder includes(String... rawPatterns) {
                if (rawPatterns != null) {
                    for (String rawPattern : rawPatterns) {
                        this.includes.add(GavtcsPattern.of(rawPattern));
                    }
                }
                return this;
            }

        }

        private static final List<GavtcsPattern> EMPTY_LIST = Collections.emptyList();

        private static final GavtcsSet INCLUDE_ALL = new IncludeExcludeGavSet(
                Collections.singletonList(GavtcsPattern.matchAll()),
                EMPTY_LIST);
        private static final GavtcsSet EXCLUDE_ALL = new IncludeExcludeGavSet(EMPTY_LIST,
                Collections.singletonList(GavtcsPattern.matchAll()));
        /**  */
        private static final long serialVersionUID = 4495169649760950618L;

        private static void append(List<GavtcsPattern> cludes, Appendable out) throws IOException {
            boolean first = true;
            for (GavtcsPattern GavtcsPattern : cludes) {
                if (first) {
                    first = false;
                } else {
                    out.append(',');
                }
                out.append(GavtcsPattern.toString());
            }
        }

        private static boolean matches(String groupId, String artifactId, List<GavtcsPattern> patterns) {
            for (GavtcsPattern pattern : patterns) {
                if (pattern.matches(groupId, artifactId)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean matches(String groupId, String artifactId, String version, List<GavtcsPattern> patterns) {
            for (GavtcsPattern pattern : patterns) {
                if (pattern.matches(groupId, artifactId, version)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean matches(String groupId, String artifactId, String version, String type, String classifier,
                String scope, List<GavtcsPattern> patterns) {
            for (GavtcsPattern pattern : patterns) {
                if (pattern.matches(groupId, artifactId, version, type, classifier, scope)) {
                    return true;
                }
            }
            return false;
        }

        private final List<GavtcsPattern> excludes;
        private final transient int hashcode;;

        private final List<GavtcsPattern> includes;

        IncludeExcludeGavSet(List<GavtcsPattern> includes, List<GavtcsPattern> excludes) {
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
         * @return            {@code true} if the given GA identifier is a member of this {@link GavtcsSet} and {@code false}
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
         * @return            {@code true} if the given GAV triple is a member of this {@link GavtcsSet} and {@code false}
         *                    otherwise
         */
        public boolean contains(String groupId, String artifactId, String version) {
            return matches(groupId, artifactId, version, includes) && !matches(groupId, artifactId, version, excludes);
        }

        @Override
        public boolean contains(String groupId, String artifactId, String version, String type, String classifier,
                String scope) {
            return matches(groupId, artifactId, version, type, classifier, scope, includes)
                    && !matches(groupId, artifactId, version, type, classifier, scope, excludes);
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
        public List<GavtcsPattern> getExcludes() {
            return excludes;
        }

        /**
         * @return the list of includes
         */
        public List<GavtcsPattern> getIncludes() {
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
