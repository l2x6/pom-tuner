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

import java.nio.file.Path;
import java.util.Objects;
import org.l2x6.pom.tuner.model.Gavtc.Type;

/**
 * A Maven dependency defined by {@code groupId}, {@code artifactId}, {@code version}, {@code type},
 * {@code classifier} and {@code file}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  5.0.0
 */
public class Gavtcf {

    private final Gavtc gavtc;
    private final Path file;
    private final int hashCode;

    public Gavtcf(String groupId, String artifactId, String version, Path file) {
        this(groupId, artifactId, version, Type.empty(), file);
    }

    public Gavtcf(String groupId, String artifactId, String version, OptionalWithDefault type, Path file) {
        this(groupId, artifactId, version, type, null, file);
    }

    public Gavtcf(String groupId, String artifactId, String version, OptionalWithDefault type, String classifier,
            Path file) {
        this(new Gavtc(groupId, artifactId, version, type, classifier), file);
    }

    /**
     * @param gavtc
     * @param file
     *
     * @since       5.0.0
     */
    public Gavtcf(Gavtc gavtc, Path file) {
        this.gavtc = gavtc;
        this.file = Objects.requireNonNull(file, "file");

        this.hashCode = hc();
    }

    /**
     * @return the {@code groupId}, never {@code null}
     *
     * @since  5.0.0
     */
    public String getGroupId() {
        return gavtc.getGroupId();
    }

    /**
     * @return the {@code artifactId}, never {@code null}
     *
     * @since  5.0.0
     */
    public String getArtifactId() {
        return gavtc.getArtifactId();
    }

    /**
     * @return the version of this {@link Gav} or {@code null} if the version is unknown
     *
     * @since  5.0.0
     */
    public String getVersion() {
        return gavtc.getVersion();
    }

    /**
     * @return the artifact type (sometimes called extension), such as `pom`, `jar`, `war`, etc.
     *
     * @since  5.0.0
     */
    public OptionalWithDefault getType() {
        return gavtc.getType();
    }

    /**
     * @return the classifier or {@code null} (rather than empty string) if it was not set
     *
     * @since  5.0.0
     */
    public String getClassifier() {
        return gavtc.getClassifier();
    }

    /**
     * @return a path pointing at this artifact's file, either absolute or relative to the working directory of the current
     *         JVM process
     *
     * @since  5.0.0
     */
    public Path getFile() {
        return file;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    /**
     * Append {@code <groupId>:<artifactId>:<version>[:type[:classifier]]:file} to the given {@link StringBuilder} and
     * return it.
     *
     * @param  stringBuilder the {@link StringBuilder} to append to
     * @return               the passed-in {@link StringBuilder}
     *
     * @since                5.0.0
     */
    public StringBuilder toString(StringBuilder stringBuilder) {
        toGavtc().toGav().toString(stringBuilder);
        final String typeValue = getType().getValue();
        final String classifier = getClassifier();
        if (typeValue != null || classifier != null || file != null) {
            stringBuilder.append(':');
            if (typeValue != null) {
                stringBuilder.append(typeValue);
            }
            if (classifier != null || file != null) {
                stringBuilder.append(':');
                if (classifier != null) {
                    stringBuilder.append(classifier);
                }
                if (file != null) {
                    stringBuilder.append(':').append(file);
                }
            }
        }
        return stringBuilder;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    private int hc() {
        return 31 * gavtc.hashCode() + file.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Gavtcf))
            return false;
        Gavtcf other = (Gavtcf) obj;
        if (!this.gavtc.equals(other.gavtc)) {
            return false;
        }
        if (!this.file.equals(other.file)) {
            return false;
        }
        return true;
    }

    /**
     * @return the embedded {@link Ga} instance, shorthand for {@link #toGavtc()}.{@link Gavtc#toGa()}
     *
     * @since  5.0.0
     */
    public Ga toGa() {
        return gavtc.toGa();
    }

    /**
     * @return the embedded {@link Gavtc} instance
     *
     * @since  5.0.0
     */
    public Gavtc toGavtc() {
        return gavtc;
    }

}
