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
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Objects;
import java.util.StringTokenizer;

/**
 * An immutable {@link #groupId}, {@link #artifactId}, {@link #version} triple with a fast {@link #hashCode()} and
 * {@link #equals(Object)}.
 * <p>
 * It is possible to create a {@link Gav} instances with {@code null} version using
 * {@link Gav#Gav(String, String, String)} or #{@link Gav#Gav(Ga, String)}.
 * This might be useful to represent dependencies in a {@code pom.xml} whose versions are managed.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class Gav implements Comparable<Gav> {

    /**
     * Parse the given {@code <groupId>:<artifactId>:<version>} {@code gavString} and return a new {@link Gavtc} instance.
     * While it is possible to create a {@link Gav} instance with {@code null} version, this method requires a non empty
     * {@code version}.
     *
     * @param  gavString             the string to parse
     * @return                       a new {@link Gav} instance parsed out of the given {@code gavString}
     * @throws IllegalStateException on any parse errors
     */
    public static Gav of(String gavString) {
        StringTokenizer st = new StringTokenizer(gavString, ":");
        if (!st.hasMoreTokens()) {
            throw new IllegalStateException("Cannot parse '" + gavString + "' to a " + Gav.class.getName()
                    + "; expected '<groupId>:<artifactId>:<version>', found too little segments");
        } else {
            final String g = st.nextToken();
            if (!st.hasMoreTokens()) {
                throw new IllegalStateException("Cannot parse '" + gavString + "' to a " + Gav.class.getName()
                        + "; expected '<groupId>:<artifactId>:<version>', found too little segments");
            } else {
                final String a = st.nextToken();
                if (!st.hasMoreTokens()) {
                    throw new IllegalStateException("Cannot parse '" + gavString + "' to a " + Gav.class.getName()
                            + "; expected '<groupId>:<artifactId>:<version>', found too little segments");
                } else {
                    final String v = st.nextToken();
                    if (st.hasMoreTokens()) {
                        throw new IllegalStateException("Cannot parse '" + gavString + "' to a " + Gav.class.getName()
                                + "; expected '<groupId>:<artifactId>:<version>', found too many segments");
                    }
                    return new Gav(g, a, v);
                }
            }
        }
    }

    private final Ga ga;
    private final int hashCode;
    private final String version;

    /**
     * @param ga      the Ga instance to embed
     * @param version the version of this {@link Gav} or {@code null} if the version is unknown; an empty string is
     *                transformed to {@code null}
     *
     * @since         4.8.0
     */
    public Gav(Ga ga, String version) {
        this.ga = ga;
        this.version = Gavtc.emptyToNull(version);
        this.hashCode = 31 * ga.hashCode() + (version == null ? 0 : version.hashCode());
    }

    /**
     * @param groupId    the {@code groupId} (required)
     * @param artifactId the {@code artifactId} (required)
     * @param version    the version of this {@link Gav} or {@code null} if the version is unknown; an empty string is
     *                   transformed to {@code null}
     */
    public Gav(String groupId, String artifactId, String version) {
        this(new Ga(groupId, artifactId), version);
    }

    @Override
    public int compareTo(Gav o) {
        int result = this.ga.compareTo(o.ga);
        if (result != 0) {
            return result;
        } else {
            return Gavtc.SAFE_STRING_COMPARATOR.compare(version, o.version);
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
        Gav other = (Gav) obj;
        return Objects.equals(this.version, other.version) && this.ga.equals(other.ga);
    }

    /**
     * @return the {@code groupId}, never {@code null}
     */
    public String getGroupId() {
        return ga.getGroupId();
    }

    /**
     * @return the {@code artifactId}, never {@code null}
     */
    public String getArtifactId() {
        return ga.getArtifactId();
    }

    /**
     * @return the version of this {@link Gav} or {@code null} if the version is unknown
     */
    public String getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    /**
     * Append {@code <groupId>:<artifactId>:<version>} to the given {@link StringBuilder} and return it.
     *
     * @param  stringBuilder the {@link StringBuilder} to append to
     * @return               the passed-in {@link StringBuilder}
     */
    public StringBuilder toString(StringBuilder stringBuilder) {
        ga.toString(stringBuilder).append(':');
        if (version != null) {
            stringBuilder.append(version);
        }
        return stringBuilder;
    }

    /**
     * Returns the embedded {@link Ga} instance.
     *
     * @return the embedded {@link Ga} instance
     */
    public Ga toGa() {
        return ga;
    }

    /**
     * @param  type
     * @param  classifier
     * @return            new {@link Gavtc} embedding this {@link Gav} and having the given {@code type} and
     *                    {@code classifier}.
     *
     * @since             4.8.0
     */
    public Gavtc toGavtc(String type, String classifier) {
        return new Gavtc(this, type, classifier);
    }

    /**
     * @return a {@code /}-separated path that, when resolved against a local Maven repository root directory
     *         (such as {@code ~/.m2/repository} or base URL (such as {@code https://repo1.maven.org/maven2}) can be used
     *         to access the artifacts of this {@link Gav}.
     * @since  4.10.0
     */
    public String getRepositoryPath() {
        return appendRepositoryPath(new StringBuilder()).toString();
    }

    /**
     * Append a {@code /}-separated path to the given {@link Appendable} that, when resolved against a local Maven
     * repository root directory (such as {@code ~/.m2/repository} or base URL (such as
     * {@code https://repo1.maven.org/maven2}), can be used to access the artifacts of this {@link Gav}.
     *
     * @param  <T>                  a subtype of {@link Appendable}
     * @param  appendable           typically a {@link StringBuilder} or {@link Writer} to append to
     * @return                      the {@code stringBuilder} with the path appended
     * @throws UncheckedIOException in case {@link Appendable#append(CharSequence)} throws an {@link IOException}
     * @since                       4.10.0
     */
    public <T extends Appendable> T appendRepositoryPath(T stringBuilder) {
        ga.appendRepositoryPath(stringBuilder);
        try {
            stringBuilder.append('/').append(version);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return stringBuilder;
    }

}
