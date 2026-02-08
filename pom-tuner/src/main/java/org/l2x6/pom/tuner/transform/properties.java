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
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.l2x6.pom.tuner.Comparators;
import org.l2x6.pom.tuner.PomTransformer;
import org.l2x6.pom.tuner.PomTransformer.ContainerElement;
import org.l2x6.pom.tuner.PomTransformer.GavtcsElement;
import org.l2x6.pom.tuner.PomTransformer.TextElement;
import org.l2x6.pom.tuner.PomTransformer.Transformer;
import org.l2x6.pom.tuner.transform.api.AddElementTransformer;
import org.l2x6.pom.tuner.transform.api.ElementSet;
import org.l2x6.pom.tuner.transform.api.RemoveElementsTransformer;
import org.l2x6.pom.tuner.transform.api.TextElementSet;

/**
 * Operations on {@code pom.xml} properties usable with {@link PomTransformer#transform(Transformer...)}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  5.0.0
 */
public interface properties {
    public static final String ELEMENT_NAME = "properties";

    /**
     * If available already, sets the given property; otherwise adds the given property as the last element under
     * {@code /project/properties}.
     * <p>
     * If the {@code <properties>} element does not exist under {@code <project>} or under the {@code <profile>} specified
     * via
     * {@link AddElementTransformer#profile(String)}, the it is silently created.
     * <p>
     * The returned {@link AddElementTransformer} instance can be further customized to target a specific profile using
     * {@link AddElementTransformer#profile(String)}
     * or to insert the property at some specific position using {@link AddElementTransformer#beforeElement(String)},
     * {@link AddElementTransformer#afterElement(String)} or {@link AddElementTransformer#at(Comparator)} and compatible
     * {@link Comparators}.
     *
     * @param  name  the property name to add
     * @param  value the property value to set
     * @return       a new customizable {@link AddElementTransformer}
     *
     * @since        5.0.0
     */
    public static <THIS extends AddElementTransformer<ContainerElement, TextElement, THIS>> AddElementTransformer<ContainerElement, TextElement, THIS> set(String name,
            String value) {
        return new AddElementTransformer<>(
                profile -> profile.getOrAddChildContainerElement(ELEMENT_NAME),
                (parent, comparator) -> parent.addChildTextElementIfNeeded(name, value, comparator),
                Comparators.elementName(Comparators.afterLast()));
    }

    /**
     * Returns a new {@link RemoveElementsTransformer} removing properties having the specified {@code propertyNames}
     * that are located under {@code /project/properties} (but not under any profiles);
     * also removes any previous sibling comments and whitespace.
     * <p>
     * The returned {@link RemoveElementsTransformer} instance can be further customized to select profiles
     * or other kinds of sibling nodes to remove.
     * <p>
     * If no properties match the given {@code propertyNames} then the returned {@link RemoveElementsTransformer} exits
     * quietly rather than throwing an exception.
     *
     * @param  propertyNames of the properties to remove
     * @return               a new {@link RemoveElementsTransformer} removing properties having the specified names
     * @since                5.0.0
     */
    public static <THIS extends RemoveElementsTransformer<TextElement, THIS>>  RemoveElementsTransformer<TextElement, THIS> remove(String... propertyNames) {
        return new RemoveElementsTransformer<>(
                RemoveElementsTransformer.textGrandChildrenMapper(ELEMENT_NAME),
                textElement -> Stream.of(propertyNames).anyMatch(textElement.getElementName()::equals));
    }

    /**
     * Returns a new {@link RemoveElementsTransformer} removing properties matching the given {@code selector}
     * that are located under {@code /project/properties} (but not under any profiles);
     * also removes any previous sibling comments and whitespace.
     * <p>
     * The returned {@link RemoveElementsTransformer} instance can be further customized to select profiles
     * or other kinds of sibling nodes to remove.
     * <p>
     * If no properties match the given {@code selector} then the returned {@link RemoveElementsTransformer} exits
     * quietly rather than throwing an exception.
     *
     * @param  selector a {@link Predicate} to select property nodes to remove
     * @return          a new {@link RemoveElementsTransformer} removing properties having the specified names
     * @since           5.0.0
     */
    public static <THIS extends RemoveElementsTransformer<TextElement, THIS>>  RemoveElementsTransformer<TextElement, THIS> remove(Predicate<TextElement> selector) {
        return new RemoveElementsTransformer<>(
                RemoveElementsTransformer.textGrandChildrenMapper(ELEMENT_NAME),
                selector);
    }

    /**
     * Returns a new {@link RemoveElementsTransformer} removing the {@code <properties>} node (including all its child
     * properties)
     * that is located under {@code /project} (but not under any profiles);
     * also removes any previous sibling comments and whitespace.
     * <p>
     * The returned {@link RemoveElementsTransformer} instance can be further customized to select profiles
     * or other kinds of sibling nodes to remove.
     * <p>
     * If there is no {@code properties} node(s) in the specified context, then the returned
     * {@link RemoveElementsTransformer} exits
     * quietly rather than throwing an exception.
     *
     * @return a new {@link RemoveElementsTransformer} removing properties having the specified names
     * @since  5.0.0
     */
    public static <THIS extends RemoveElementsTransformer<ContainerElement, THIS>> RemoveElementsTransformer<ContainerElement, THIS> removeAll() {
        return new RemoveElementsTransformer<>(
                RemoveElementsTransformer.containerElementsMapper(ELEMENT_NAME),
                containerElement -> true);
    }

    /**
     * Returns a new {@link RemoveElementsTransformer} removing the {@code <properties>} node (including all its child
     * properties)
     * that is located under {@code /project} (but not under any profiles),
     * if the {@code <properties>} node has no element children;
     * also removes any previous sibling comments and whitespace.
     * <p>
     * The returned {@link RemoveElementsTransformer} instance can be further customized to select profiles
     * or other kinds of sibling nodes to remove.
     * <p>
     * If there is no {@code properties} node(s) in the specified context, then the returned
     * {@link RemoveElementsTransformer} exits
     * quietly rather than throwing an exception.
     *
     * @return a new {@link RemoveElementsTransformer} removing properties having the specified names
     * @since  5.0.0
     */
    public static <THIS extends RemoveElementsTransformer<ContainerElement, THIS>> RemoveElementsTransformer<ContainerElement, THIS> removeEmptyParent() {
        return new RemoveElementsTransformer<>(
                RemoveElementsTransformer.containerElementsMapper(ELEMENT_NAME),
                ((Predicate<ContainerElement>) ContainerElement::hasChildElements).negate());
    }

    /**
     * Select some property elements by name for modification.
     * <p>
     * The returned {@link TextElementSet} instance can be further customized to select profiles and/or specify the actual modification operation.
     * <p>
     * If none of the {@code from*(*)} methods of the returned {@link TextElementSet} is called,
     * the default behavior is to select the matching elements only from under the {@code <project>} element
     * and ignore any matching elements under {@code <profile>} elements.
     *
     * @param propertyNames a {@link Predicate} selecting property nodes by their element names
     * @return a new {@link TextElementSet} having its node selector set as specified
     * @since  5.0.0
     */
    public static TextElementSet selectByName(Predicate<String> propertyNames) {
        return new TextElementSet(ElementSet.textGrandChildrenMapper(ELEMENT_NAME), textElement -> propertyNames.test(textElement.getElementName()));
    }

    /**
     * Select some property elements by name for modification.
     * <p>
     * The returned {@link TextElementSet} instance can be further customized to select profiles and/or specify the actual modification operation.
     * <p>
     * If none of the {@code from*(*)} methods of the returned {@link TextElementSet} is called,
     * the default behavior is to select the matching elements only from under the {@code <project>} element
     * and ignore any matching elements under {@code <profile>} elements.
     *
     * @param propertyNames property names
     * @return a new {@link TextElementSet} having its node selector set as specified
     * @since  5.0.0
     */
    public static TextElementSet selectByName(String... propertyNames) {
        final Set<String> set;
        if (propertyNames.length == 0) {
            set = Collections.emptySet();
        } else if (propertyNames.length == 1) {
            set = Collections.singleton(propertyNames[0]);
        } else {
            set = new HashSet<>();
            for (int i = 0; i < propertyNames.length; i++) {
                set.add(propertyNames[i]);
            }
        }
        return new TextElementSet(ElementSet.textGrandChildrenMapper(ELEMENT_NAME), textElement -> set.contains(textElement.getElementName()));
    }

    /**
     * Select some property elements by value for modification.
     * <p>
     * The returned {@link TextElementSet} instance can be further customized to select profiles and/or specify the actual modification operation.
     * <p>
     * If none of the {@code from*(*)} methods of the returned {@link TextElementSet} is called,
     * the default behavior is to select the matching elements only from under the {@code <project>} element
     * and ignore any matching elements under {@code <profile>} elements.
     *
     * @param values a {@link Predicate} selecting property nodes by value
     * @return a new {@link TextElementSet} having its node selector set as specified
     * @since  5.0.0
     */
    public static TextElementSet selectByValue(Predicate<String> values) {
        return new TextElementSet(ElementSet.textGrandChildrenMapper(ELEMENT_NAME), textElement -> values.test(textElement.getTextContent()));
    }


    /**
     * Select some property elements by value for modification.
     * <p>
     * The returned {@link TextElementSet} instance can be further customized to select profiles and/or specify the actual modification operation.
     * <p>
     * If none of the {@code from*(*)} methods of the returned {@link TextElementSet} is called,
     * the default behavior is to select the matching elements only from under the {@code <project>} element
     * and ignore any matching elements under {@code <profile>} elements.
     *
     * @param values property values
     * @return a new {@link TextElementSet} having its node selector set as specified
     * @since  5.0.0
     */
    public static TextElementSet selectByValue(String... values) {
        final Set<String> set;
        if (values.length == 0) {
            set = Collections.emptySet();
        } else if (values.length == 1) {
            set = Collections.singleton(values[0]);
        } else {
            set = new HashSet<>();
            for (int i = 0; i < values.length; i++) {
                set.add(values[i]);
            }
        }
        return new TextElementSet(ElementSet.textGrandChildrenMapper(ELEMENT_NAME), textElement -> set.contains(textElement.getTextContent()));
    }

    /**
     * Select some property elements modification.
     * <p>
     * The returned {@link TextElementSet} instance can be further customized to select profiles and/or specify the actual modification operation.
     * <p>
     * If none of the {@code from*(*)} methods of the returned {@link TextElementSet} is called,
     * the default behavior is to select the matching elements only from under the {@code <project>} element
     * and ignore any matching elements under {@code <profile>} elements.
     *
     * @param propertySelector a {@link Predicate} selecting property nodes
     * @return a new {@link TextElementSet} having its node selector set as specified
     * @since  5.0.0
     */
    public static TextElementSet select(Predicate<TextElement> propertySelector) {
        return new TextElementSet(ElementSet.textGrandChildrenMapper(ELEMENT_NAME), propertySelector);
    }

    /**
     * Select all property elements for modification.
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
