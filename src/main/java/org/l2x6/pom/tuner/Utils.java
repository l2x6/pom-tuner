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

public class Utils {

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

}
