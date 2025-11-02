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

import java.util.Objects;
import java.util.StringTokenizer;

/**
 * An immutable {@link #groupId}, {@link #artifactId} pair with a fast {@link #hashCode()} and {@link #equals(Object)}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class Ga implements Comparable<Ga> {

    /**
     * @deprecated use {@link GavPattern}
     */
    @Deprecated
    private static final Ga EXCELUDE_ALL = new Ga("*", "*");
    private final String artifactId;
    private final String groupId;
    private final int hashCode;

    /**
     * Returns a new {@link Ga} instance parsed out of the given {@code gavString}.
     *
     * @param  gavString the string to parse, something of the form {@code groupId:artifactId:version}
     * @return           a new {@link Ga} instance parsed out of the given {@code gavString}
     */
    public static Ga of(String gavString) {
        StringTokenizer st = new StringTokenizer(gavString, ":");
        if (!st.hasMoreTokens()) {
            throw new IllegalStateException("Cannot parse '" + gavString + " to a " + Ga.class.getName()
                    + "; expected '<groupId>:<artifactId>', found too little segments");
        } else {
            final String g = st.nextToken();
            if (!st.hasMoreTokens()) {
                throw new IllegalStateException("Cannot parse '" + gavString + " to a " + Ga.class.getName()
                        + "; expected '<groupId>:<artifactId>', found too little segments");
            } else {
                final String a = st.nextToken();
                if (st.hasMoreTokens()) {
                    throw new IllegalStateException("Cannot parse '" + gavString + " to a " + Ga.class.getName()
                            + "; expected '<groupId>:<artifactId>', found too many segments");
                }
                return new Ga(g, a);
            }
        }
    }

    public static Ga of(String groupId, String artifactId) {
        if ("*".equals(groupId) && "*".equals(artifactId)) {
            return EXCELUDE_ALL;
        }
        return new Ga(groupId, artifactId);
    }

    public Ga(String groupId, String artifactId) {
        super();
        this.groupId = Objects.requireNonNull(groupId, "groupId");
        this.artifactId = Objects.requireNonNull(artifactId, "artifactId");
        this.hashCode = 31 * (31 + artifactId.hashCode()) + groupId.hashCode();
    }

    @Override
    public int compareTo(Ga o) {
        int result = this.groupId.compareTo(o.groupId);
        if (result != 0) {
            return result;
        } else {
            return this.artifactId.compareTo(o.artifactId);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Ga other = (Ga) obj;
        return this.artifactId.equals(other.artifactId) && this.groupId.equals(other.groupId);
    }

    /**
     * @return the {@code groupId}, never {@code null}
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * @return the {@code artifactId}, never {@code null}
     */
    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId;
    }

    /**
     * Append {@code <groupId>:<artifactId>} to the given {@link StringBuilder} and return it.
     *
     * @param stringBuilder the {@link StringBuilder} to append to
     * @return the passed-in {@link StringBuilder}
     *
     * @since 4.8.0
     */
    public StringBuilder toString(StringBuilder stringBuilder) {
        return stringBuilder.append(groupId).append(':').append(artifactId);
    }

    /**
     * @return {@link #EXCELUDE_ALL}
     * @deprecated use {@link GavPattern}
     */
    @Deprecated
    public static Ga excludeAll() {
        return EXCELUDE_ALL;
    }

    /**
     * @param version the version of this {@link Gav} or {@code null} if the version is unknown; an empty string is transformed to {@code null}
     * @return new {@link Gav} embedding this {@link Ga} and having the given {@code version}.
     *
     * @since 4.8.0
     */
    public Gav toGav(String version) {
        return new Gav(this, version);
    }
}
