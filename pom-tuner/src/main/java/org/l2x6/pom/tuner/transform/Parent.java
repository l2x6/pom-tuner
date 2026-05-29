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
import org.l2x6.pom.tuner.PomTransformer.ContainerElement;
import org.l2x6.pom.tuner.PomTransformer.Transformer;
import org.l2x6.pom.tuner.transform.api.ParentTransformer;
import org.l2x6.pom.tuner.transform.api.RemoveElementsTransformer;
import org.l2x6.pom.tuner.transform.api.RemoveTransformer;

/**
 * Operations on {@code pom.xml} {@code parent} usable with {@link PomTransformer#transform(Transformer...)}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  5.0.0
 */
public interface Parent {
    public static final String ELEMENT_NAME = "parent";

    /**
     * Creates the {@code <parent>} element if needed and sets its child elements to the given values.
     *
     * @param  groupId      the {@code groupId} to set, must not be {@code null}
     * @param  artifactId   the {@code artifactId} to set, must not be {@code null}
     * @param  version      the {@code version} to set, must not be {@code null}
     * @param  relativePath the {@code relativePath} to set, must not be {@code null}
     * @return              a new customizable {@link Transformer}
     *
     * @since               5.0.0
     */
    public static Transformer set(
            String groupId,
            String artifactId,
            String version,
            String relativePath) {

        return context -> {
            ContainerElement parent = context.getOrAddContainerElement(ELEMENT_NAME);
            parent.addOrSetChildTextElement("groupId", groupId);
            parent.addOrSetChildTextElement("artifactId", artifactId);
            parent.addOrSetChildTextElement("version", version);
            parent.addOrSetChildTextElement("relativePath", relativePath);
        };
    }

    /**
     * Creates the {@code <parent>} element if needed and sets its child elements to the given values.
     * If the {@code <relativePath>} child node exists already, leaves it as is.
     *
     * @param  groupId      the {@code groupId} to set, must not be {@code null}
     * @param  artifactId   the {@code artifactId} to set, must not be {@code null}
     * @param  version      the {@code version} to set, must not be {@code null}
     * @param  relativePath the {@code relativePath} to set, must not be {@code null}
     * @return              a new customizable {@link Transformer}
     *
     * @since               5.0.0
     */
    public static Transformer set(
            String groupId,
            String artifactId,
            String version) {

        return context -> {
            ContainerElement parent = context.getOrAddContainerElement("parent");
            parent.addOrSetChildTextElement("groupId", groupId);
            parent.addOrSetChildTextElement("artifactId", artifactId);
            parent.addOrSetChildTextElement("version", version);
        };
    }

    /**
     * Creates the {@code <parent>} element if needed and sets its {@code groupId} to the given value.
     *
     * @param  groupId the {@code groupId} to set, must not be {@code null}
     * @return         a new customizable {@link ParentTransformer}
     *
     * @since          5.0.0
     */
    public static ParentTransformer setGroupId(String groupId) {
        return new ParentTransformer().setGroupId(groupId);
    }

    /**
     * Creates the {@code <parent>} element if needed and sets its {@code artifactId} to the given value.
     *
     * @param  artifactId the {@code artifactId} to set, must not be {@code null}
     * @return            a new customizable {@link ParentTransformer}
     *
     * @since             5.0.0
     */
    public static ParentTransformer setArtifactId(String artifactId) {
        return new ParentTransformer().setArtifactId(artifactId);
    }

    /**
     * Creates the {@code <parent>} element if needed and sets its {@code version} to the given value.
     *
     * @param  version the {@code version} to set, must not be {@code null}
     * @return         a new customizable {@link ParentTransformer}
     *
     * @since          5.0.0
     */
    public static ParentTransformer setVersion(String version) {
        return new ParentTransformer().setVersion(version);
    }

    /**
     * Creates the {@code <parent>} element if needed and sets its {@code relativePath} to the given value.
     *
     * @param  relativePath the {@code relativePath} to set, must not be {@code null}
     * @return              a new customizable {@link ParentTransformer}
     *
     * @since               5.0.0
     */
    public static ParentTransformer setRelativePath(String relativePath) {
        return new ParentTransformer().setRelativePath(relativePath);
    }

    /**
     * Returns a new {@link RemoveTransformer} removing the {@code <parent>} element and all its subnodes
     * also removes any previous sibling comments and whitespace.
     * <p>
     * The returned {@link RemoveTransformer} instance can be further customized to remove other kinds of sibling nodes.
     * <p>
     * If there is no {@code <parent>} element in the given context then the returned {@link RemoveElementsTransformer}
     * exits
     * quietly rather than throwing an exception.
     *
     * @return a new {@link RemoveTransformer} removing the {@code <parent>} element
     * @since  5.0.0
     */
    public static <THIS extends RemoveTransformer<ContainerElement, THIS>> RemoveTransformer<ContainerElement, THIS> remove() {
        return new RemoveTransformer<>(
                RemoveElementsTransformer.containerElementsMapper(ELEMENT_NAME),
                containerElement -> true);
    }

    /**
     * Returns a new {@link RemoveTransformer} removing the {@code <relativePath>} element under {@code <parent>}
     * also removes any previous sibling comments and whitespace.
     * <p>
     * The returned {@link RemoveTransformer} instance can be further customized to remove other kinds of sibling nodes.
     * <p>
     * If there is no {@code <parent>} element in the given context then the returned {@link RemoveElementsTransformer}
     * exits
     * quietly rather than throwing an exception.
     *
     * @return a new {@link RemoveTransformer} removing the {@code <relativePath>} element under {@code <parent>}
     * @since  5.0.0
     */
    public static <THIS extends RemoveTransformer<ContainerElement, THIS>> RemoveTransformer<ContainerElement, THIS> removeRelativePath() {
        return new RemoveTransformer<>(
                RemoveElementsTransformer.containerElementsMapper(ELEMENT_NAME, "relativePath"),
                containerElement -> true);
    }

}
