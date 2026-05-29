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

import java.security.cert.CollectionCertStoreParameters;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class PomTunerUtils {

    private PomTunerUtils() {
    }

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
     * A generator of XPath 1.0 "any namespace" selector, such as
     * {@code /*:[local-name()='foo']/*:[local-name()='bar']}. In XPath 2.0, this would be just {@code /*:foo/*:bar},
     * but as of Java 13, there is only XPath 1.0 available in the JDK.
     *
     * @param  elements namespace-less element names
     * @return          am XPath 1.0 style selector
     */
    public static String anyNs(String... elements) {
        StringBuilder sb = new StringBuilder();
        for (String e : elements) {
            sb.append("/*[local-name()='").append(e).append("']");
        }
        return sb.toString();
    }

    public static <T> Set<T> toLinkedHashSet(T... entries) {
        if (entries.length == 0) {
            return Collections.emptySet();
        } else if (entries.length == 1) {
            return Collections.singleton(entries[0]);
        } else {
            final Set<T> set = new LinkedHashSet<>();
            for (int i = 0; i < entries.length; i++) {
                set.add(entries[i]);
            }
            return Collections.unmodifiableSet(set);
        }
    }

}
