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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Utils {

    private static final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

    /**
     * @param  anyPath a path with either slashes or backslashes
     * @return         a file path with slashes
     */
    public static String toUnixPath(String anyPath) {
        if (anyPath == null || anyPath.isEmpty()) {
            return anyPath;
        }
        return anyPath.replace('\\', '/');
    }

    /**
     * Climbs up the file system hierarchy starting in {@code start} and searching for a {@code mvnw} or
     * {@code mvnw.cmd} file suitable for the current platform.
     *
     * @param  start                 a preferably absolute path to a file or directory where the search should start
     * @return                       a path to {@code mvnw} or {@code mvnw.cmd} (suitable for the current platform)
     * @throws IllegalStateException if no {@code mvnw} or {@code mvnw.cmd} could be found
     */
    public static Path findMvnw(final Path start) {
        Path dir = Files.isDirectory(start) ? start : start.getParent();
        final Path mvnwRelPath = Paths.get("mvnw" + (isWindows ? ".cmd" : ""));
        while (dir != null) {
            final Path result = dir.resolve(mvnwRelPath);
            if (Files.exists(result)) {
                return result;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException(String.format("Could not find mvnw starting in [%s]", start));
    }

}
