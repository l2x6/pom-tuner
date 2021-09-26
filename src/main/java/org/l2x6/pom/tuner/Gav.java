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
package org.l2x6.pom.tuner;

import java.util.StringTokenizer;

/**
 * An immutable {@link #groupId}, {@link #artifactId}, {@link #version} triple with a fast {@link #hashCode()} and
 * {@link #equals(Object)}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class Gav implements Comparable<Gav> {

    /**
     * Returns a new {@link Gav} instance parsed out of the given {@code gavString}.
     *
     * @param  gavString the string to parse, something of the form {@code groupId:artifactId:version}
     * @return           a new {@link Gav} instance parsed out of the given {@code gavString}
     */
    public static Gav of(String gavString) {
        StringTokenizer st = new StringTokenizer(gavString, ":");
        if (!st.hasMoreTokens()) {
            throw new IllegalStateException(String.format("Cannot parse [%s] to a " + Gav.class.getName(), gavString));
        } else {
            final String g = st.nextToken();
            if (!st.hasMoreTokens()) {
                throw new IllegalStateException(
                        String.format("Cannot parse [%s] to a " + Gav.class.getName(), gavString));
            } else {
                final String a = st.nextToken();
                if (!st.hasMoreTokens()) {
                    throw new IllegalStateException(
                            String.format("Cannot parse [%s] to a " + Gav.class.getName(), gavString));
                } else {
                    final String v = st.nextToken();
                    return new Gav(g, a, v);
                }
            }
        }
    }

    private final String artifactId;
    private final String groupId;
    private final int hashCode;
    private final String version;

    public Gav(String groupId, String artifactId, String version) {
        super();
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.hashCode = 31 * (31 * (31 + artifactId.hashCode()) + groupId.hashCode()) + version.hashCode();
    }

    @Override
    public int compareTo(Gav o) {
        int result = this.groupId.compareTo(o.groupId);
        if (result != 0) {
            return result;
        } else {
            result = this.artifactId.compareTo(o.artifactId);
            if (result != 0) {
                return result;
            } else {
                return this.version.compareTo(o.version);
            }
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
        return this.version.equals(other.version) && this.artifactId.equals(other.artifactId)
                && this.groupId.equals(other.groupId);
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }

    public Ga toGa() {
        return new Ga(groupId, artifactId);
    }
}
