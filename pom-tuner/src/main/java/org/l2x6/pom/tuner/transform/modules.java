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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.l2x6.pom.tuner.Comparators;
import org.l2x6.pom.tuner.PomTransformer;
import org.l2x6.pom.tuner.PomTransformer.ContainerElement;
import org.l2x6.pom.tuner.PomTransformer.GavtcsElement;
import org.l2x6.pom.tuner.PomTransformer.ProfileElement;
import org.l2x6.pom.tuner.PomTransformer.TextElement;
import org.l2x6.pom.tuner.PomTransformer.Transformer;
import org.l2x6.pom.tuner.transform.api.AddElementTransformer;
import org.l2x6.pom.tuner.transform.api.AddGavtcsTransformer;
import org.l2x6.pom.tuner.transform.api.ElementSet;
import org.l2x6.pom.tuner.transform.api.RemoveElementsTransformer;
import org.l2x6.pom.tuner.transform.api.TextElementSet;

/**
 * Operations on {@code pom.xml} modules usable with {@link PomTransformer#transform(Transformer...)}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  5.0.0
 */
public interface modules {
    public static final String ELEMENT_NAME = "modules";

    /**
     * If the given {@code path} is available already, does nothing; otherwise adds the given module as the last element
     * under {@code /project/modules}.
     * <p>
     * If the {@code <modules>} element does not exist under {@code <project>} or under the {@code <profile>} specified via
     * {@link AddElementTransformer#profile(String)}, the it is silently created.
     * <p>
     * The returned {@link AddElementTransformer} instance can be further customized to target a specific profile using
     * {@link AddElementTransformer#profile(String)}
     * or to insert the module at some specific position using {@link AddElementTransformer#beforeTextContent(String)},
     * {@link AddElementTransformer#afterTextContent(String)} or {@link AddElementTransformer#at(Comparator)} and
     * compatible {@link Comparators}.
     *
     * @param  path the module path to add
     * @return      a new customizable {@link AddElementTransformer}
     *
     * @since       5.0.0
     */
    public static <THIS extends AddElementTransformer<ContainerElement, TextElement, THIS>> AddElementTransformer<ContainerElement, TextElement, THIS> add(String path) {
        return new AddElementTransformer<>(
                profile -> profile.getOrAddChildContainerElement(ELEMENT_NAME),
                (parent, comparator) -> parent.addChildTextElementIfNeeded("module", path, comparator),
                Comparators.textContent(Comparators.afterLast()));
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
    public static <THIS extends RemoveElementsTransformer<TextElement, THIS>>  RemoveElementsTransformer<TextElement, THIS> remove(String... modulePaths) {
        return new RemoveElementsTransformer<>(
                RemoveElementsTransformer.textGrandChildrenMapper(ELEMENT_NAME),
                textElement -> Stream.of(modulePaths).anyMatch(textElement.getTextContent()::equals));
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
    public static <THIS extends RemoveElementsTransformer<TextElement, THIS>>  RemoveElementsTransformer<TextElement, THIS> remove(Predicate<TextElement> selector) {
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
    public static <THIS extends RemoveElementsTransformer<ContainerElement, THIS>> RemoveElementsTransformer<ContainerElement, THIS> removeAll() {
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
    public static <THIS extends RemoveElementsTransformer<ContainerElement, THIS>> RemoveElementsTransformer<ContainerElement, THIS> removeEmptyParent() {
        return new RemoveElementsTransformer<>(
                RemoveElementsTransformer.containerElementsMapper(ELEMENT_NAME),
                ((Predicate<ContainerElement>) ContainerElement::hasChildElements).negate());
    }

    /**
     * Select some {@code <module>} nodes for modification.
     * <p>
     * The returned {@link TextElementSet} instance can be further customized to select profiles and/or specify the actual modification operation.
     * <p>
     * If none of the {@code from*(*)} methods of the returned {@link TextElementSet} is called,
     * the default behavior is to select the matching elements only from under the {@code <project>} element
     * and ignore any matching elements under {@code <profile>} elements.
     *
     * @param modulePaths a {@link Predicate} selecting modules by their path as present in {@code <module>path</module>}
     * @return a new {@link TextElementSet} having its node selector set as specified
     * @since  5.0.0
     */
    public static TextElementSet select(Predicate<String> modulePaths) {
        return new TextElementSet(ElementSet.textGrandChildrenMapper(ELEMENT_NAME), textElement -> modulePaths.test(textElement.getTextContent()));
    }

    /**
     * Select some {@code <module>} nodes for modification.
     * <p>
     * The returned {@link TextElementSet} instance can be further customized to select profiles and/or specify the actual modification operation.
     * <p>
     * If none of the {@code from*(*)} methods of the returned {@link TextElementSet} is called,
     * the default behavior is to select the matching elements only from under the {@code <project>} element
     * and ignore any matching elements under {@code <profile>} elements.
     *
     * @param modulePaths module paths as present in {@code <module>path</module>} to select for modification
     * @return a new {@link TextElementSet} having its node selector set as specified
     * @since  5.0.0
     */
    public static TextElementSet select(String... modulePaths) {
        final Set<String> set;
        if (modulePaths.length == 0) {
            set = Collections.emptySet();
        } else if (modulePaths.length == 1) {
            set = Collections.singleton(modulePaths[0]);
        } else {
            set = new HashSet<>();
            for (int i = 0; i < modulePaths.length; i++) {
                set.add(modulePaths[i]);
            }
        }
        return new TextElementSet(ElementSet.textGrandChildrenMapper(ELEMENT_NAME), textElement -> set.contains(textElement.getTextContent()));
    }

    /**
     * Select all {@code <module>} nodes for modification.
     * <p>
     * The returned {@link TextElementSet} instance can be further customized to select profiles and/or specify the actual modification operation.
     * <p>
     * If none of the {@code from*(*)} methods of the returned {@link TextElementSet} is called,
     * the default behavior is to select the matching elements only from under the {@code <project>} element
     * and ignore any matching elements under {@code <profile>} elements.
     *
     * @return a new {@link TextElementSet} having its node selector set as specified
     * @since  5.0.0
     */
    public static TextElementSet selectAll() {
        return new TextElementSet(ElementSet.textGrandChildrenMapper(ELEMENT_NAME), textElement -> true);
    }
}
