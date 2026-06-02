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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.UUID;
import org.l2x6.pom.tuner.PomTransformer.Transformation;

public class PomTransformerTestUtils {

    static <T extends Transformation> void assertTransformer(String src, Collection<T> transformations, String expected) {
        PomTransformer.transform(transformations, Paths.get("pom.xml"),
                () -> src, xml -> compareDocuments(xml, expected, ".xml"));
    }

    static void compareDocuments(String actual, String expected, String extension) {
        if (Objects.equals(actual, expected)) {
            return;
        }

        final StringTokenizer stActual = new StringTokenizer(actual, "\n");
        final StringTokenizer stExpected = new StringTokenizer(expected, "\n");
        final List<String> equalLines = new ArrayList<>();
        int line = 1;
        while (true) {
            boolean hasNextActual = stActual.hasMoreTokens();
            boolean hasNextExpected = stExpected.hasMoreTokens();
            if (!hasNextActual && !hasNextExpected) {
                break;
            } else if (hasNextActual && hasNextExpected) {
                String lineActual = stActual.nextToken();
                String lineExpectd = stExpected.nextToken();
                if (lineActual.equals(lineExpectd)) {
                    equalLines.add(lineExpectd);
                } else {
                    int minLength = Math.min(lineActual.length(), lineExpectd.length());
                    int i = 0;
                    while (i < minLength && lineActual.charAt(i) == lineExpectd.charAt(i)) {
                        i++;
                    }
                    StringBuilder msg = new StringBuilder("Unexpected " + extension + " content at line ")
                            .append(line)
                            .append(" column ")
                            .append(i + 1)
                            .append(":\n\n");
                    equalLines.forEach(l -> msg.append(l).append('\n'));

                    msg.append("actual");
                    appendDashes(i - "actual".length(), msg);
                    msg.append("↴\n");
                    msg.append(lineActual).append("↵\n");
                    appendDashes(i, msg);
                    msg.append("↕\n");
                    msg.append(lineExpectd).append("↵\n");
                    msg.append("expected");
                    appendDashes(i - "expected".length(), msg);
                    msg.append("⬏\n");

                    String uuid = UUID.randomUUID().toString();
                    Path aPath = Paths.get("target/" + uuid + "-actual" + extension);
                    Path ePath = Paths.get("target/" + uuid + "-expected" + extension);
                    String diff = System.getenv("DIFF");
                    msg.append("\nCheck the full diff\n\n    ").append(diff == null ? "diff" : diff).append(" ").append(aPath)
                            .append(" ")
                            .append(ePath);
                    try {
                        Files.createDirectories(aPath.getParent());
                        Files.write(aPath, actual.getBytes(StandardCharsets.UTF_8));
                        Files.write(ePath, expected.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    throw new AssertionError(msg.toString());
                }
                line++;
            }
        }

    }

    public static void appendDashes(int i, StringBuilder msg) {
        if (i > 0) {
            for (int j = 0; j < i; j++) {
                msg.append('-');
            }
        }
    }

    public static Path write(String kind, String content, String extension, final String uuid, final Path parent) {
        final Path path = parent.resolve(uuid + "-" + kind + extension);
        try {
            Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write to " + path, e);
        }
        return path;
    }

}
