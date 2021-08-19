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
package org.l2x6.maven.utils.shell;

/**
 * Thrown when an execution of a shell command is not finished within some given timeout.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class CommandTimeoutException extends BuildException {

    private static final long serialVersionUID = -8311365402356710884L;

    public CommandTimeoutException(String message) {
        super(message);
    }

    public CommandTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommandTimeoutException(Throwable cause) {
        super(cause);
    }

}
