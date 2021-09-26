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
package org.l2x6.pom.tuner.shell;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility for executing {@link ShellCommand}s.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class Shell {

    /**
     * A simple wrapper over {@link Process} that manages its destroying and offers Java 8-like
     * {@link #waitFor(long, TimeUnit, String[])} with timeout.
     */
    public static class CommandProcess implements Closeable {

        private final Process process;
        private final Thread shutDownHook;
        private final StreamGobbler stdOut;

        public CommandProcess(String requestId, Process process, LineConsumer out) {
            super();
            this.process = process;
            this.stdOut = new StreamGobbler(process.getInputStream(), out);
            stdOut.start();

            this.shutDownHook = new Thread(new Runnable() {
                @Override
                public void run() {
                    stdOut.cancel();
                    // stdErr.cancel();
                    CommandProcess.this.process.destroy();
                }
            });
            Runtime.getRuntime().addShutdownHook(shutDownHook);
        }

        @Override
        public void close() {
            process.destroy();
        }

        public CommandResult waitFor(long timeout, TimeUnit unit, String[] cmdArray)
                throws CommandTimeoutException, InterruptedException, IOException {
            final long startMillisTime = System.currentTimeMillis();
            final long startNanoTime = System.nanoTime();
            long rem = unit.toNanos(timeout);

            do {
                try {
                    int exitCode = process.exitValue();
                    try {
                        Runtime.getRuntime().removeShutdownHook(shutDownHook);
                    } catch (Exception ignored) {
                    }

                    stdOut.join();
                    stdOut.assertSuccess();

                    return new CommandResult(cmdArray, exitCode, System.currentTimeMillis() - startMillisTime);
                } catch (IllegalThreadStateException ex) {
                    if (rem > 0) {
                        Thread.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1, 100));
                    }
                }
                rem = unit.toNanos(timeout) - (System.nanoTime() - startNanoTime);
            } while (rem > 0);
            throw new CommandTimeoutException(
                    String.format("Command has not finished within [%d] ms: %s", timeout, Arrays.toString(cmdArray)));
        }

    }

    /**
     * A result of a {@link ShellCommand}'s execution.
     */
    public static class CommandResult {
        private final String[] cmdArray;
        private final int exitCode;
        private final long runtimeMs;

        public CommandResult(String[] cmdArray, int exitCode, long runtimeMs) {
            super();
            this.cmdArray = cmdArray;
            this.exitCode = exitCode;
            this.runtimeMs = runtimeMs;
        }

        /**
         * @return                      this {@link CommandResult}
         * @throws BadExitCodeException if and only if {@link #exitCode} != 0
         */
        public CommandResult assertSuccess() throws BadExitCodeException {
            if (exitCode != 0) {
                throw new BadExitCodeException(cmdArray, exitCode);
            }
            return this;
        }

        /**
         * @return the exit code returned by the underlying {@link ShellCommand}
         */
        public int getExitCode() {
            return exitCode;
        }

        public long getRuntimeMs() {
            return runtimeMs;
        }
    }

    /**
     * The usual friend of {@link Process#getInputStream()} / {@link Process#getErrorStream()}.
     */
    static class StreamGobbler extends Thread {
        private volatile boolean cancelled;
        private IOException exception;
        private final InputStream in;
        private final LineConsumer out;

        private StreamGobbler(InputStream in, LineConsumer out) {
            this.in = in;
            this.out = out;
        }

        public void assertSuccess() throws IOException {
            if (exception != null) {
                throw exception;
            }
        }

        public void cancel() {
            this.cancelled = true;
        }

        @Override
        public void run() {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while (!cancelled && (line = r.readLine()) != null) {
                    out.accept(line);
                }
            } catch (IOException e) {
                exception = e;
            }
        }
    }

    static final Logger log = LoggerFactory.getLogger(Shell.class);

    /**
     * Executes the given {@link ShellCommand} synchronously.
     *
     * @param  command                 the command to execute
     * @return                         the {@link CommandResult} that can be used to determine if the execution was
     *                                 successful
     * @throws BuildException          on any build related problems
     * @throws CommandTimeoutException if the execution is not finished within the timeout defined in
     *                                 {@link ShellCommand#getTimeoutMs()}
     */
    public static CommandResult execute(ShellCommand command) throws BuildException, CommandTimeoutException {
        final String[] cmdArray = command.asCmdArray();
        final String cmdArrayString = Arrays.stream(cmdArray).collect(Collectors.joining(" "));
        final Map<String, String> env = command.getEnvironment();
        log.info("srcdeps[{}]: Executing command [{}] using env {}", command.getId(), cmdArrayString, env);
        ProcessBuilder builder = new ProcessBuilder(cmdArray) //
                .directory(command.getWorkingDirectory().toFile()) //
                .redirectErrorStream(true);
        if (!env.isEmpty()) {
            builder.environment().putAll(env);
        }
        try (LineConsumer out = command.getOutput().get();
                CommandProcess process = new CommandProcess(command.getId(), builder.start(), out)) {
            return process.waitFor(command.getTimeoutMs(), TimeUnit.MILLISECONDS, cmdArray).assertSuccess();
        } catch (IOException | InterruptedException e) {
            throw new BuildException(String.format("Could not start command [%s]", cmdArrayString), e);
        }
    }

    /**
     * You are looking for {@link #execute(ShellCommand)}.
     */
    private Shell() {
        super();
    }

}
