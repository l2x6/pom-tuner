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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;
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
        private final Path rootDirectory;

        private LocalMavenRepository(Path rootDirectory) {
            this.rootDirectory = rootDirectory;
        }

        @Override
        public Stream<Gavtcf> gavtcfStream() {
            try {
                return Files.walk(rootDirectory).filter(Files::isDirectory)
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
                            .of(new VersionDirectory(versionDirectory, relDir,
                                    new Gav(groupId.toString(), artifactId, version)));
                }
                return Optional.empty();
            }

            private VersionDirectory(Path absPath, Path relPath, Gav gav) {
                this.absVersionDir = absPath;
                this.relVersionDir = relPath;
                this.gav = gav;
            }

            private final Path absVersionDir;
            private final Path relVersionDir;
            private final Gav gav;

            public Stream<Gavtcf> artifacts() {
                try {
                    String prefix = gav.getArtifactId() + "-" + gav.getVersion();
                    return Files.list(absVersionDir).filter(file -> {
                        String fileName = file.getFileName().toString();
                        return fileName.startsWith(prefix)
                                && !fileName.endsWith(".asc")
                                && !fileName.endsWith(".md5")
                                && !fileName.endsWith(".sha1");
                    })
                            .map(file -> {
                                String fileName = file.getFileName().toString();
                                final int lastPeriodPos = fileName.lastIndexOf('.');
                                final String type = fileName.substring(lastPeriodPos + 1);
                                final String classifier = (prefix.length() == lastPeriodPos)
                                        ? null
                                        : fileName.substring(prefix.length() + 1, lastPeriodPos);
                                return gav.toGavtc(Type.of(type), classifier)
                                        .toGavtcf(relVersionDir.resolve(file.getFileName()));
                            });
                } catch (IOException e) {
                    throw new UncheckedIOException("Could not list " + absVersionDir, e);
                }
            }
        }

    }
}
