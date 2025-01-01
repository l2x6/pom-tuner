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
package org.l2x6.pom.tuner.shell;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;
import org.slf4j.Logger;

public interface LineConsumer extends Consumer<String>, Closeable {

    class LoggerLineConsumer implements LineConsumer {

        private final String id;
        private Logger logger;

        LoggerLineConsumer(String id, Logger logger) {
            super();
            this.id = id;
            this.logger = logger;
        }

        @Override
        public void accept(String t) {
            logger.info("srcdeps[{}]: {}", id, t);
        }

        @Override
        public void close() throws IOException {
        }
    }

    class StringLineConsumer implements LineConsumer {

        private final StringBuilder sb = new StringBuilder();

        @Override
        public void accept(String t) {
            sb.append(t).append('\n');
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public String toString() {
            return sb.toString();
        }

    }

    class TeeLineConsumer implements LineConsumer {

        private final LineConsumer delegate1;
        private final LineConsumer delegate2;

        TeeLineConsumer(LineConsumer delegate1, LineConsumer delegate2) {
            super();
            this.delegate1 = delegate1;
            this.delegate2 = delegate2;
        }

        @Override
        public void accept(String t) {
            delegate1.accept(t);
            delegate2.accept(t);
        }

        @Override
        public void close() throws IOException {
            delegate1.close();
            delegate2.close();
        }

    }

    class WriterLineConsumer implements LineConsumer {

        private final Writer delegate;
        private final String id;

        WriterLineConsumer(String id, Writer delegate) {
            super();
            this.id = id;
            this.delegate = delegate;
        }

        @Override
        public void accept(String t) {
            try {
                delegate.write(t);
                delegate.write('\n');
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        @Override
        public String toString() {
            return id;
        }

    }

    LineConsumer DUMMY = new LineConsumer() {

        @Override
        public void accept(String t) {
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public String toString() {
            return "DummyLineConsumer";
        }

    };

    static LineConsumer dummy() {
        return DUMMY;
    }

    static LineConsumer logger(String id, Logger logger) {
        return new LoggerLineConsumer(id, logger);
    }

    static void renameIfNeeded(Path path, int level, int count, String base, String ext) throws IOException {
        final int newLevel = level + 1;
        if (newLevel < count) {
            final Path newPath = path.getParent().resolve(base + "-" + newLevel + ext);
            renameIfNeeded(newPath, newLevel, count, base, ext);
            if (Files.exists(path)) {
                Files.move(path, newPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    static LineConsumer rotate(Path path, int count) {
        final String fileName = path.getFileName().toString();
        final int periodPos = fileName.lastIndexOf('.');
        final String base = periodPos >= 0 ? fileName.substring(0, periodPos) : fileName;
        final String ext = periodPos >= 0 ? fileName.substring(periodPos) : "";
        try {
            renameIfNeeded(path, 0, count, base, ext);
            return new WriterLineConsumer(path.toString(), Files.newBufferedWriter(path, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static LineConsumer string() {
        return new StringLineConsumer();
    }

    static LineConsumer tee(LineConsumer lc1, LineConsumer lc2) {
        return new TeeLineConsumer(lc1, lc2);
    }

}
