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
/**
  * Copyright 2015-2016 Maven Source Dependencies
 * Plugin contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.l2x6.pom.tuner.shell;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A definition of a shell command that can be executed by {@link Shell#execute(ShellCommand)}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class ShellCommand {
    public static class ShellCommandBuilder {
        private List<String> arguments = new ArrayList<>();
        private Map<String, String> environment = new LinkedHashMap<>();
        private String executable;
        private String id;
        private Supplier<LineConsumer> output;
        private long timeoutMs = Long.MAX_VALUE;
        private Path workingDirectory;

        public ShellCommandBuilder arguments(List<String> args) {
            this.arguments.addAll(args);
            return this;
        }

        public ShellCommandBuilder arguments(String... args) {
            for (int i = 0; i < args.length; i++) {
                this.arguments.add(args[i]);
            }
            return this;
        }

        public ShellCommand build() {
            return new ShellCommand(id, executable, Collections.unmodifiableList(arguments), workingDirectory,
                    Collections.unmodifiableMap(environment), output, timeoutMs);
        }

        public ShellCommandBuilder environment(Map<String, String> buildEnvironment) {
            this.environment.putAll(buildEnvironment);
            return this;
        }

        public ShellCommandBuilder environmentEntry(String key, String value) {
            this.environment.put(key, value);
            return this;
        }

        public ShellCommandBuilder executable(String executable) {
            this.executable = executable;
            return this;
        }

        public ShellCommandBuilder id(String id) {
            this.id = id;
            return this;
        }

        public ShellCommandBuilder output(Supplier<LineConsumer> output) {
            this.output = output;
            return this;
        }

        public ShellCommandBuilder timeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public ShellCommandBuilder workingDirectory(Path workingDirectory) {
            this.workingDirectory = workingDirectory;
            return this;
        }

    }

    public static ShellCommandBuilder builder() {
        return new ShellCommandBuilder();
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

    private static final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

    private final List<String> arguments;
    private final Map<String, String> environment;
    private final String executable;
    private final String id;
    private final Supplier<LineConsumer> output;

    private final long timeoutMs;

    private final Path workingDirectory;

    private ShellCommand(String id, String executable, List<String> arguments, Path workingDirectory,
            Map<String, String> environment, Supplier<LineConsumer> output, long timeoutMs) {
        super();
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(executable, "executable");
        Objects.requireNonNull(arguments, "arguments");
        Objects.requireNonNull(workingDirectory, "workingDirectory");
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(output, "output");

        this.id = id;
        this.executable = executable;
        this.arguments = arguments;
        this.workingDirectory = workingDirectory;
        this.environment = environment;
        this.output = output;
        this.timeoutMs = timeoutMs;
    }

    /**
     * @return an array containing the executable and its arguments that can be passed e.g. to
     *         {@link ProcessBuilder#command(String...)}
     */
    public String[] asCmdArray() {
        String[] result = new String[arguments.size() + 1];
        int i = 0;
        result[i++] = executable;
        for (String arg : arguments) {
            result[i++] = arg;
        }
        return result;
    }

    /**
     * @return the {@link List} arguments for the executable. Cannot be {@code null}.
     */
    public List<String> getArguments() {
        return arguments;
    }

    /**
     * @return a {@link Map} of environment variables that should be used when executing this {@link ShellCommand}.
     *         Cannot be {@code null}. Note that these are just overlay variables - when a new {@link Process} is
     *         spawned, the environment is copied from the present process and only the variables the provided by the
     *         present method are overwritten.
     */
    public Map<String, String> getEnvironment() {
        return environment;
    }

    /**
     * @return the executable file that should be called
     */
    public String getExecutable() {
        return executable;
    }

    public String getId() {
        return id;
    }
    //
    // public Consumer<String> getLogger() {
    // return logger;
    // }

    public Supplier<LineConsumer> getOutput() {
        return output;
    }

    /**
     * @return timeout in milliseconds
     */
    public long getTimeoutMs() {
        return timeoutMs;
    }

    /**
     * @return the directory in which this {@link ShellCommand} should be executed
     */
    public Path getWorkingDirectory() {
        return workingDirectory;
    }

}
