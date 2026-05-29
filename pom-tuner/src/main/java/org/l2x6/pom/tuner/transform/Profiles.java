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
package org.l2x6.pom.tuner.transform;

import org.l2x6.pom.tuner.PomTransformer;
import org.l2x6.pom.tuner.PomTransformer.Transformer;
import org.l2x6.pom.tuner.transform.api.AddGavtcsTransformer;

/**
 * Operations on {@code pom.xml} profiles usable with {@link PomTransformer#transform(Transformer...)}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  5.0.0
 */
public interface Profiles {

    public static final String ELEMENT_NAME = "profiles";

    /**
     * If the given {@code profile} is available already, does nothing; otherwise adds the given profile as the last
     * element
     * under {@code /project/profiles}.
     *
     * @param  profileId the profile id to add
     * @return           a new {@link Transformer}
     *
     * @since             5.0.0
     */
    public static Transformer add(String profileId) {
        return context -> context.getOrAddProfile(profileId);
    }
}
