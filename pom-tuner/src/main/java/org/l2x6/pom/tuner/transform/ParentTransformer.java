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

import java.util.Collections;
import java.util.List;
import org.l2x6.pom.tuner.PomTransformer.Transformation;
import org.l2x6.pom.tuner.PomTransformer.TransformationContext;

public class ParentTransformer implements Transformation {

    private final List<Transformation> transformers;

    private ParentTransformer(List<Transformation> transformers) {
        this.transformers = transformers;
    }

    public ParentTransformer() {
        this.transformers = Collections.emptyList();
    }

    /**
     * Creates the {@code <parent>} element if needed and sets its {@code groupId} to the given value.
     *
     * @param  groupId the {@code groupId} to set, must not be {@code null}
     * @return         a new customizable {@link ParentTransformer}
     *
     * @since          5.0.0
     */
    public ParentTransformer setGroupId(String groupId) {
        return new ParentTransformer(RemoveElementsTransformer.add(transformers,
                context -> context.getOrAddContainerElement("parent").addOrSetChildTextElement("groupId", groupId)));
    }

    /**
     * Creates the {@code <parent>} element if needed and sets its {@code artifactId} to the given value.
     *
     * @param  artifactId the {@code artifactId} to set, must not be {@code null}
     * @return            a new customizable {@link ParentTransformer}
     *
     * @since             5.0.0
     */
    public ParentTransformer setArtifactId(String artifactId) {
        return new ParentTransformer(RemoveElementsTransformer.add(transformers,
                context -> context.getOrAddContainerElement("parent").addOrSetChildTextElement("artifactId", artifactId)));
    }

    /**
     * Creates the {@code <parent>} element if needed and sets its {@code version} to the given value.
     *
     * @param  version the {@code version} to set, must not be {@code null}
     * @return         a new customizable {@link ParentTransformer}
     *
     * @since          5.0.0
     */
    public ParentTransformer setVersion(String version) {
        return new ParentTransformer(RemoveElementsTransformer.add(transformers,
                context -> context.getOrAddContainerElement("parent").addOrSetChildTextElement("version", version)));
    }

    /**
     * Creates the {@code <parent>} element if needed and sets its {@code relativePath} to the given value.
     *
     * @param  relativePath the {@code relativePath} to set, must not be {@code null}
     * @return              a new customizable {@link ParentTransformer}
     *
     * @since               5.0.0
     */
    public ParentTransformer setRelativePath(String relativePath) {
        return new ParentTransformer(RemoveElementsTransformer.add(transformers,
                context -> context.getOrAddContainerElement("parent").addOrSetChildTextElement("relativePath", relativePath)));
    }

    @Override
    public void perform(TransformationContext context) {
        for (Transformation transformer : transformers) {
            transformer.perform(context);
        }
    }

}
