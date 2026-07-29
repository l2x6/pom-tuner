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
package org.l2x6.pom.tuner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.l2x6.pom.tuner.model.Gav;
import org.l2x6.pom.tuner.model.Gavtc.Type;
import org.l2x6.pom.tuner.model.Gavtcf;

/**
 * A Maven repository - local or remote.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  5.0.0
 */
public interface MavenRepository {

    static MavenRepository local(Path rootDirectory) {
        return new LocalMavenRepository(rootDirectory);
    }

    /**
     * @return a {@link Stream} of {@link Gavtcf} having their {@link Gavtcf#getFile()} set to a path relative to the
     *         root directory of this {@link MavenRepository}; the returned Stream wraps a Files.walk() DirectoryStream
     *         and must be closed by the caller
     */
    Stream<Gavtcf> gavtcfStream();

    /**
     * A local Maven repository
     *
     * @since 5.0.0
     */
    static class LocalMavenRepository implements MavenRepository {
        private static final Set<String> NON_ARTIFACT_EXTENSIONS = new HashSet<>(
                Arrays.asList("asc", "md5", "sha1", "sha256", "sha512"));
        private final Path rootDirectory;

        private LocalMavenRepository(Path rootDirectory) {
            this.rootDirectory = rootDirectory;
        }

        @Override
        public Stream<Gavtcf> gavtcfStream() {
            try {
                return Files.walk(rootDirectory, FileVisitOption.FOLLOW_LINKS)
                        .filter(Files::isDirectory)
                        .map(versionDir -> VersionDirectory.of(rootDirectory, versionDir))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .flatMap(VersionDirectory::artifacts);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not walk " + rootDirectory, e);
            }
        }

        static class VersionDirectory {

            static Optional<VersionDirectory> of(Path rootDirectory, Path versionDirectory) {
                final Path relDir = rootDirectory.relativize(versionDirectory);
                String version = relDir.getFileName().toString();
                final Path artifactDir = relDir.getParent();
                if (artifactDir == null) {
                    return Optional.empty();
                }
                String artifactId = artifactDir.getFileName().toString();
                if (Files.isRegularFile(versionDirectory.resolve(artifactId + "-" + version + ".pom"))) {
                    final Path groupDir = artifactDir.getParent();
                    if (groupDir == null) {
                        return Optional.empty();
                    }
                    Iterator<Path> it = groupDir.iterator();
                    final StringBuilder groupId = new StringBuilder(it.next().toString());
                    while (it.hasNext()) {
                        groupId.append('.').append(it.next().toString());
                    }
                    return Optional
                            .of(new VersionDirectory(
                                    versionDirectory,
                                    new Gav(groupId.toString(), artifactId, version)));
                }
                return Optional.empty();
            }

            private VersionDirectory(Path absPath, Gav gav) {
                this.absVersionDir = absPath;
                this.gav = gav;
            }

            private final Path absVersionDir;
            private final Gav gav;

            public Stream<Gavtcf> artifacts() {
                try {
                    String prefix = gav.getArtifactId() + "-" + gav.getVersion();
                    return Files.list(absVersionDir)
                            .map(ArtifactFile::new)
                            .filter(f -> f.type != null
                                    && !NON_ARTIFACT_EXTENSIONS.contains(f.type)
                                    && f.fileName.startsWith(prefix)
                                    && Files.isRegularFile(f.path))
                            .map(f -> {
                                String classifier = (prefix.length() == f.lastPeriodPosition)
                                        ? null
                                        : f.fileName.substring(prefix.length() + 1, f.lastPeriodPosition);
                                return gav.toGavtc(Type.of(f.type), classifier)
                                        .toGavtcf(absVersionDir.resolve(f.path));
                            });
                } catch (IOException e) {
                    throw new UncheckedIOException("Could not list " + absVersionDir, e);
                }
            }
        }

        static class ArtifactFile {
            private final String type;
            private final Path path;
            private final String fileName;
            private final int lastPeriodPosition;

            ArtifactFile(Path path) {
                this.path = path;
                this.fileName = path.getFileName().toString();
                this.lastPeriodPosition = this.fileName.lastIndexOf('.');
                this.type = lastPeriodPosition > 0 && lastPeriodPosition < fileName.length() - 1
                        ? fileName.substring(lastPeriodPosition + 1)
                        : null;
            }
        }

    }
}
