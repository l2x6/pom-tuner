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
import org.l2x6.pom.tuner.model.GaSet.IncludeExcludeGaSet.Builder;

/**
 * A set of {@link Ga}s defined by included and excluded {@link GaPattern}s.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 *
 * @since  5.0.0
 */
public interface GaSet extends Predicate<Ga> {

    public static Builder builder() {
        return new Builder();
    }

    public static UnionGaSet.Builder unionBuilder() {
        return new UnionGaSet.Builder();
    }

    public static GaSet includeAll() {
        return IncludeExcludeGaSet.INCLUDE_ALL;
    }

    public static GaSet excludeAll() {
        return IncludeExcludeGaSet.EXCLUDE_ALL;
    }

    /**
     * @param  gaSets the {@link GaSet}s to union
     * @return        a {@link GaSet} that is a union of the given {@code gaSets}
     */
    public static GaSet union(Collection<GaSet> gaSets) {
        if (gaSets == null || gaSets.isEmpty()) {
            return IncludeExcludeGaSet.INCLUDE_ALL;
        }
        return new UnionGaSet(new ArrayList<>(gaSets));
    }

    /**
     * @param  gaSets the {@link GaSet}s to union
     * @return        a {@link GaSet} that is a union of the given {@code gaSets}
     */
    public static GaSet union(GaSet... gaSets) {
        if (gaSets == null || gaSets.length == 0) {
            return IncludeExcludeGaSet.INCLUDE_ALL;
        }
        return new UnionGaSet(new ArrayList<>(Arrays.asList(gaSets)));
    }

    /**
     *
     * @param  groupId
     * @param  artifactId
     * @return            {@code true} if the given GA identifier is a member of this {@link GaSet} and {@code false}
     *                    otherwise
     */
    boolean contains(String groupId, String artifactId);

    /**
     * Shorthand for {@code contains(ga.getGroupId(), ga.getArtifactId())}.
     *
     * @param  ga the groupId and artifactId to check for membership in this gaSet
     * @return    {@code true} if the given {@link Ga} is a member of this {@link GaSet} and {@code false}
     *            otherwise
     */
    default boolean contains(Ga ga) {
        return contains(ga.getGroupId(), ga.getArtifactId());
    }

    /**
     * An implementation of {@link Predicate#test(Object)}. Delegates to {@link #contains(Ga)}.
     *
     * @param  ga the groupId and artifactId to check for membership in this {@link GaSet}
     * @return    {@code true} if the given {@link Ga} is a member of this {@link GaSet} and {@code false}
     *            otherwise
     */
    @Override
    default boolean test(Ga ga) {
        return contains(ga);
    }

    /**
     * Unions {@code this} {@link GaSet} with the given {@code other} {@link GaSet}
     *
     * @param  other the {@link GaSet} to union with
     * @return       if {@code this} is equal to {@code other} returns {@code this}; otherwise returns a new {@link GaSet}
     *               that is a union of {@code this} {@link GaSet} and the given {@code other}
     *               {@link GaSet}
     */
    default GaSet union(GaSet other) {
        if (this.equals(other)) {
            return this;
        }
        return new UnionGaSet(this, other);
    }

    public static class UnionGaSet implements GaSet, Serializable {
        private static final long serialVersionUID = 7052524943789230104L;

        private final List<GaSet> gaSets;

        UnionGaSet(GaSet... gaSets) {
            this.gaSets = Arrays.asList(gaSets);
        }

        UnionGaSet(List<GaSet> gaSets) {
            this.gaSets = gaSets;
        }

        @Override
        public boolean contains(String groupId, String artifactId) {
            return gaSets.stream().anyMatch(gaSet -> gaSet.contains(groupId, artifactId));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((gaSets == null) ? 0 : gaSets.hashCode());
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
            UnionGaSet other = (UnionGaSet) obj;
            if (gaSets == null) {
                if (other.gaSets != null)
                    return false;
            } else if (!gaSets.equals(other.gaSets))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "UnionGaSet [" + gaSets + "]";
        }

        public static class Builder {
            private List<GaSet> gaSets = new ArrayList<>();
            private GaSet defaultResult = IncludeExcludeGaSet.INCLUDE_ALL;

            public Builder union(GaSet gaSet) {
                gaSets.add(gaSet);
                return this;
            }

            /**
             * @param  gaSet a {@link GaSet} to return from {@link #build()} method in case no {@link GaSet}s were added via
             *               {@link #union(GaSet)}
             * @return       {@code this}
             */
            public Builder defaultResult(GaSet gaSet) {
                this.defaultResult = gaSet;
                return this;
            }

            public GaSet build() {
                final List<GaSet> gs = gaSets;
                if (gs.isEmpty()) {
                    return defaultResult;
                }
                gaSets = null;
                return new UnionGaSet(Collections.unmodifiableList(gs));
            }
        }

    }

    public static class IncludeExcludeGaSet implements GaSet, Serializable {
        public static class Builder {
            private List<GaPattern> excludes = new ArrayList<>();
            private List<GaPattern> includes = new ArrayList<>();
            private GaSet defaultResult = IncludeExcludeGaSet.INCLUDE_ALL;

            private Builder() {
            }

            public GaSet build() {
                if (includes.isEmpty() && excludes.isEmpty()) {
                    return defaultResult;
                }
                if (includes.isEmpty()) {
                    includes.add(GaPattern.matchAll());
                }

                List<GaPattern> useIncludes = Collections.unmodifiableList(includes);
                List<GaPattern> useExcludes = Collections.unmodifiableList(excludes);

                this.includes = null;
                this.excludes = null;

                return new IncludeExcludeGaSet(useIncludes, useExcludes);
            }

            /**
             * @param  gaSet a {@link GaSet} to return from {@link #build()} method in case no includes or excludes were added
             * @return       {@code this}
             */
            public Builder defaultResult(GaSet gaSet) {
                this.defaultResult = gaSet;
                return this;
            }

            /**
             * Exclude a single GA pattern.
             *
             * @param  rawPattern
             * @return            this {@link Builder}
             */
            public Builder exclude(String rawPattern) {
                this.excludes.add(GaPattern.of(rawPattern));
                return this;
            }

            /**
             * Exclude a single GA pattern.
             *
             * @param  pattern
             * @return         this {@link Builder}
             */
            public Builder exclude(GaPattern pattern) {
                this.excludes.add(pattern);
                return this;
            }

            /**
             * Parses the entries of the given {@link Collection} of {@code rawPatterns} and excludes those.
             *
             * @param  rawPatterns {@link Collection} of GA patterns to parse via {@link GaPattern#of(String)}
             * @return             this {@link Builder}
             */
            public Builder excludes(Collection<String> rawPatterns) {
                if (rawPatterns != null) {
                    for (String rawPattern : rawPatterns) {
                        this.excludes.add(GaPattern.of(rawPattern));
                    }
                }
                return this;
            }

            /**
             * Excludes the given {@code patterns}
             *
             * @param  patterns {@link Collection} of {@link GaPattern}s to exclude
             * @return          this {@link Builder}
             */
            public Builder excludePatterns(Collection<GaPattern> patterns) {
                if (patterns != null) {
                    for (GaPattern rawPattern : patterns) {
                        this.excludes.add(rawPattern);
                    }
                }
                return this;
            }

            /**
             * Excludes the given {@code patterns}
             *
             * @param  patterns an array of {@link GaPattern}s to exclude
             * @return          this {@link Builder}
             */
            public Builder excludes(GaPattern... patterns) {
                if (patterns != null) {
                    for (GaPattern rawPattern : patterns) {
                        this.excludes.add(rawPattern);
                    }
                }
                return this;
            }

            /**
             * Parses the entries of the given array of {@code rawPatterns} and excludes those.
             *
             * @param  rawPatterns a list of GA patterns to parse via {@link GaPattern#of(String)}
             * @return             this {@link Builder}
             */
            public Builder excludes(String... rawPatterns) {
                if (rawPatterns != null) {
                    for (String rawPattern : rawPatterns) {
                        this.excludes.add(GaPattern.of(rawPattern));
                    }
                }
                return this;
            }

            /**
             * Parses the given comma or whitespace separated list of {@code rawPatterns} and excludes those.
             *
             * @param  rawPatterns a comma separated list of GA patterns
             * @return             this {@link Builder}
             */
            public Builder excludes(String rawPatterns) {
                if (rawPatterns != null) {
                    StringTokenizer st = new StringTokenizer(rawPatterns, ", \t\n\r");
                    while (st.hasMoreTokens()) {
                        this.excludes.add(GaPattern.of(st.nextToken()));
                    }
                }
                return this;
            }

            /**
             * Exclude the specified GA pattern if it starts with {@code !}; include it otherwise.
             *
             * @param  rawPattern a GA pattern possibly starting with {@code !} exclusion mark
             * @return            this {@link Builder}
             */
            public Builder include(String rawPattern) {
                if (rawPattern.startsWith("!")) {
                    this.excludes.add(GaPattern.of(rawPattern.substring(1)));
                } else {
                    this.includes.add(GaPattern.of(rawPattern));
                }
                return this;
            }

            /**
             * Include a single GA pattern.
             *
             * @param  pattern
             * @return         this {@link Builder}
             */
            public Builder include(GaPattern pattern) {
                this.includes.add(pattern);
                return this;
            }

            /**
             * For each of the entries of the given GA patterns: if the entry starts with
             * {@code !}, remove the first character and exclude the pattern; otherwise include the pattern.
             *
             * @param  rawPatterns {@link Collection} of GA patterns some of which may start with the {@code !} exclusion mark
             * @return             this {@link Builder}
             */
            public Builder includes(Collection<String> rawPatterns) {
                if (rawPatterns != null) {
                    for (String rawPattern : rawPatterns) {
                        include(rawPattern);
                    }
                }
                return this;
            }

            /**
             * Includes the given {@code patterns}.
             *
             * @param  patterns {@link Collection} of GA patterns to parse via {@link GaPattern#of(String)}
             * @return          this {@link Builder}
             */
            public Builder includePatterns(Collection<GaPattern> patterns) {
                if (patterns != null) {
                    for (GaPattern rawPattern : patterns) {
                        this.includes.add(rawPattern);
                    }
                }
                return this;
            }

            /**
             * Includes the given {@code patterns}.
             *
             * @param  patterns {@link Collection} of GA patterns to include
             * @return          this {@link Builder}
             */
            public Builder includes(GaPattern... patterns) {
                if (patterns != null) {
                    for (GaPattern rawPattern : patterns) {
                        this.includes.add(rawPattern);
                    }
                }
                return this;
            }

            /**
             * For each of the entries of the given GA patterns array: if the entry starts with
             * {@code !}, remove the first character and exclude the pattern; otherwise include the pattern.
             * Parses the entries of the given array of {@code rawPatterns} and includes those.
             *
             * @param  rawPatterns a list of GA patterns some of which may start with the {@code !} exclusion mark
             * @return             this {@link Builder}
             */
            public Builder includes(String... rawPatterns) {
                if (rawPatterns != null) {
                    for (String rawPattern : rawPatterns) {
                        include(rawPattern);
                    }
                }
                return this;
            }

            /**
             * Split the given comma or whitespace separated list of GA patterns into pattern tokens and
             * pass each of those to {@link #include(String)}.
             *
             * @param  rawPatterns a comma separated list of GA patterns some of which may start with the {@code !} exclusion
             *                     mark
             * @return             this {@link Builder}
             */
            public Builder includes(String rawPatterns) {
                if (rawPatterns != null) {
                    StringTokenizer st = new StringTokenizer(rawPatterns, ", \t\n\r");
                    while (st.hasMoreTokens()) {
                        include(st.nextToken());
                    }
                }
                return this;
            }
        }

        private static final List<GaPattern> EMPTY_LIST = Collections.emptyList();

        private static final GaSet INCLUDE_ALL = new IncludeExcludeGaSet(Collections.singletonList(GaPattern.matchAll()),
                EMPTY_LIST);
        private static final GaSet EXCLUDE_ALL = new IncludeExcludeGaSet(EMPTY_LIST,
                Collections.singletonList(GaPattern.matchAll()));
        /**  */
        private static final long serialVersionUID = 8291750947043522878L;

        private static void append(List<GaPattern> cludes, Appendable out) throws IOException {
            boolean first = true;
            for (GaPattern gaPattern : cludes) {
                if (first) {
                    first = false;
                } else {
                    out.append(',');
                }
                out.append(gaPattern.toString());
            }
        }

        private static boolean matches(String groupId, String artifactId, List<GaPattern> patterns) {
            for (GaPattern pattern : patterns) {
                if (pattern.matches(groupId, artifactId)) {
                    return true;
                }
            }
            return false;
        }

        private final List<GaPattern> excludes;
        private final transient int hashcode;;

        private final List<GaPattern> includes;

        IncludeExcludeGaSet(List<GaPattern> includes, List<GaPattern> excludes) {
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
         * @return            {@code true} if the given GA identifier is a member of this {@link GaSet} and {@code false}
         *                    otherwise
         */
        public boolean contains(String groupId, String artifactId) {
            return matches(groupId, artifactId, includes) && !matches(groupId, artifactId, excludes);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            IncludeExcludeGaSet other = (IncludeExcludeGaSet) obj;
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
        public List<GaPattern> getExcludes() {
            return excludes;
        }

        /**
         * @return the list of includes
         */
        public List<GaPattern> getIncludes() {
            return includes;
        }

        @Override
        public int hashCode() {
            return hashcode;
        }

        @Override
        public String toString() {
            return "GaSet [excludes=" + excludes + ", includes=" + includes + "]";
        }

    }

}
