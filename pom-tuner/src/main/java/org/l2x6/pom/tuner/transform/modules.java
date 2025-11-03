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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.l2x6.pom.tuner.PomTransformer;
import org.l2x6.pom.tuner.PomTransformer.ContainerElement;
import org.l2x6.pom.tuner.PomTransformer.TextElement;
import org.l2x6.pom.tuner.PomTransformer.TransformationContext;
import org.l2x6.pom.tuner.PomTransformer.Transformer;
import org.l2x6.pom.tuner.transform.api.RemoveElementsTransformer;

/**
 * Operations on {@code pom.xml} modules usable with {@link PomTransformer#transform(Transformer...)}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  5.0.0
 */
public interface modules {
    public static final String ELEMENT_NAME = "modules";

    public static Transformer addIfNeeded(String module) {
        return addIfNeeded(null, null, Collections.singleton(module));
    }

    public static Transformer addIfNeeded(String profileId, String... modulePaths) {
        return addIfNeeded(profileId, null, Arrays.asList(modulePaths));
    }

    public static Transformer addIfNeeded(String module, Comparator<String> comparator) {
        return (TransformationContext context) -> {
            ContainerElement modules = context.getOrAddContainerElement(ELEMENT_NAME);
            context.addTextChildIfNeeded(modules, "module", module, comparator);
        };
    }

    public static Transformer addIfNeeded(String profileId, Collection<String> modulePaths) {
        return addIfNeeded(profileId, null, modulePaths);
    }

    public static Transformer addIfNeeded(String profileId, Comparator<String> comparator,
            Collection<String> modulePaths) {
        return (TransformationContext context) -> {
            final ContainerElement profileParent = context.getOrAddProfileParent(profileId);
            final ContainerElement modules = profileParent.getOrAddChildContainerElement(ELEMENT_NAME);
            for (String m : modulePaths) {
                if (comparator != null) {
                    context.addTextChildIfNeeded(modules, "module", m, comparator);
                } else {
                    modules.addChildTextElement("module", m);
                }
            }
        };
    }

    /**
     * Returns a new {@link RemoveElementsTransformer} removing modules having the specified {@code modulePaths}
     * that are located under {@code /project/modules} (but not under any profiles);
     * also removes any previous sibling comments and whitespace.
     * <p>
     * The returned {@link RemoveElementsTransformer} instance can be further customized to select profiles
     * or other kinds of sibling nodes to remove.
     * <p>
     * If no modules match the given {@code modulePaths} then the returned {@link RemoveElementsTransformer} exits
     * quietly rather than throwing an exception.
     *
     * @param  modulePaths of the modules to remove
     * @return             a new {@link RemoveElementsTransformer} removing modules having the specified {@code modulePaths}
     * @since              5.0.0
     */
    public static RemoveElementsTransformer<TextElement> remove(String... modulePaths) {
        return new RemoveElementsTransformer<>(
                RemoveElementsTransformer.textGrandChildrenMapper(ELEMENT_NAME),
                textElement -> Stream.of(modulePaths).anyMatch(textElement.getText()::equals));
    }

    /**
     * Returns a new {@link RemoveElementsTransformer} removing modules matching the given {@code selector}
     * that are located under {@code /project/modules} (but not under any profiles);
     * also removes any previous sibling comments and whitespace.
     * <p>
     * The returned {@link RemoveElementsTransformer} instance can be further customized to select profiles
     * or other kinds of sibling nodes to remove.
     * <p>
     * If no modules match the given {@code selector} then the returned {@link RemoveElementsTransformer} exits
     * quietly rather than throwing an exception.
     *
     * @param  selector a {@link Predicate} to select module nodes to remove
     * @return          a new {@link RemoveElementsTransformer} removing modules having the specified {@code modulePaths}
     * @since           5.0.0
     */
    public static RemoveElementsTransformer<TextElement> remove(Predicate<TextElement> selector) {
        return new RemoveElementsTransformer<>(
                RemoveElementsTransformer.textGrandChildrenMapper(ELEMENT_NAME),
                selector);
    }

    /**
     * Returns a new {@link RemoveElementsTransformer} removing the {@code <modules>} node (including all its child module
     * nodes)
     * that is located under {@code /project} (but not under any profiles);
     * also removes any previous sibling comments and whitespace.
     * <p>
     * The returned {@link RemoveElementsTransformer} instance can be further customized to select profiles
     * or other kinds of sibling nodes to remove.
     * <p>
     * If there is no {@code modules} node(s) in the specified context(s), then the returned
     * {@link RemoveElementsTransformer} exits quietly rather than throwing an exception.
     *
     * @return a new {@link RemoveElementsTransformer} removing modules having the specified names
     * @since  5.0.0
     */
    public static RemoveElementsTransformer<ContainerElement> removeAll() {
        return new RemoveElementsTransformer<>(
                RemoveElementsTransformer.containerElementsMapper(ELEMENT_NAME),
                containerElement -> true);
    }

    /**
     * Returns a new {@link RemoveElementsTransformer} removing the {@code <modules>} node (including all its child
     * modules)
     * that is located under {@code /project} (but not under any profiles),
     * if the {@code <modules>} node has no element children;
     * also removes any previous sibling comments and whitespace.
     * <p>
     * The returned {@link RemoveElementsTransformer} instance can be further customized to select profiles
     * or other kinds of sibling nodes to remove.
     * <p>
     * If there is no {@code modules} node(s) in the specified context, then the returned
     * {@link RemoveElementsTransformer} exits
     * quietly rather than throwing an exception.
     *
     * @return a new {@link RemoveElementsTransformer} removing modules having the specified names
     * @since  5.0.0
     */
    public static RemoveElementsTransformer<ContainerElement> removeEmptyParent() {
        return new RemoveElementsTransformer<>(
                RemoveElementsTransformer.containerElementsMapper(ELEMENT_NAME),
                ((Predicate<ContainerElement>) ContainerElement::hasChildElements).negate());
    }
}
