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

import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.l2x6.pom.tuner.Comparators;
import org.l2x6.pom.tuner.PomTransformer;
import org.l2x6.pom.tuner.PomTransformer.ContainerElement;
import org.l2x6.pom.tuner.PomTransformer.GavtcsElement;
import org.l2x6.pom.tuner.PomTransformer.Transformer;
import org.l2x6.pom.tuner.model.Gavtcs;
import org.l2x6.pom.tuner.model.GavtcsPattern;
import org.l2x6.pom.tuner.transform.api.AddGavtcsTransformer;
import org.l2x6.pom.tuner.transform.api.RemoveElementsTransformer;

/**
 * Operations on {@code pom.xml} dependencyManagement usable with {@link PomTransformer#transform(Transformer...)}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  5.0.0
 */
public interface dependencyManagement {

    public static final String ELEMENT_NAME = "dependencyManagement";
    public static final String OTHER_ELEMENT_NAMES = "dependencies";


    /**
     * If the given dependency management entry is available already, does nothing; otherwise adds the given dependency as the last element
     * under {@code /project/dependencyManagement/dependencies}.
     * <p>
     * The returned {@link AddGavtcsTransformer} instance can be further customized to target a specific profile using
     * {@link AddGavtcsTransformer#profile(String)}
     * or to insert the dependency at some specific position using {@link AddGavtcsTransformer#before(Gavtcs)},
     * {@link AddGavtcsTransformer#after(Gavtcs)} or {@link AddGavtcsTransformer#at(Comparator)} and
     * compatible {@link Comparators}.
     *
     * @param  dependency the dependency to add
     * @return      a new customizable {@link AddGavtcsTransformer}
     *
     * @since       5.0.0
     */
    public static <THIS extends AddGavtcsTransformer<ContainerElement, GavtcsElement, THIS>> AddGavtcsTransformer<ContainerElement, GavtcsElement, THIS> add(Gavtcs dependency) {
        return new AddGavtcsTransformer<>(
                profile -> profile.getOrAddChildContainerElement(ELEMENT_NAME).getOrAddChildContainerElement(OTHER_ELEMENT_NAMES),
                (parent, comparator) -> parent.addGavtcsIfNeeded(dependency, comparator),
                Comparators.afterLast());
    }

    /**
     * Returns a new {@link RemoveElementsTransformer} removing {@code dependencyManagement} entries matching any of the
     * specified {@code patterns};
     * the removed {@code dependencyManagement} entries are located under {@code /project/dependencies} (but not under any
     * profiles);
     * also removes any previous sibling comments and whitespace.
     * <p>
     * The returned {@link RemoveElementsTransformer} instance can be further customized to select profiles
     * or other kinds of sibling nodes to remove.
     * <p>
     * If no {@code dependencyManagement} entries match the given {@code dependencyNames} then the returned
     * {@link RemoveElementsTransformer} exits
     * quietly rather than throwing an exception.
     *
     * @param  patterns of the {@code dependencyManagement} entries to remove
     * @return          a new {@link RemoveElementsTransformer} removing {@code dependencyManagement} entries matching the
     *                  specified {@code patterns}
     * @since           5.0.0
     */
    public static <THIS extends RemoveElementsTransformer<GavtcsElement, THIS>>  RemoveElementsTransformer<GavtcsElement, THIS> remove(GavtcsPattern... patterns) {
        return new RemoveElementsTransformer<>(
                RemoveElementsTransformer.gavtcsElementsMapper(ELEMENT_NAME, OTHER_ELEMENT_NAMES),
                textElement -> Stream.of(patterns).anyMatch(pattern -> pattern.matches(textElement.getGavtcs())));
    }

    /**
     * Returns a new {@link RemoveElementsTransformer} removing {@code dependencyManagement} entries matching any of the
     * specified {@code patterns};
     * the removed {@code dependencyManagement} entries are located under {@code /project/dependencies} (but not under any
     * profiles);
     * also removes any previous sibling comments and whitespace.
     * <p>
     * The returned {@link RemoveElementsTransformer} instance can be further customized to select profiles
     * or other kinds of sibling nodes to remove.
     * <p>
     * If no {@code dependencyManagement} entries match the given {@code dependencyNames} then the returned
     * {@link RemoveElementsTransformer} exits
     * quietly rather than throwing an exception.
     *
     * @param  patterns of the {@code dependencyManagement} entries to remove
     * @return          a new {@link RemoveElementsTransformer} removing {@code dependencyManagement} entries matching the
     *                  specified {@code patterns}
     * @since           5.0.0
     */
    public static <THIS extends RemoveElementsTransformer<GavtcsElement, THIS>>  RemoveElementsTransformer<GavtcsElement, THIS> remove(String... patterns) {
        return remove(Stream.of(patterns).map(GavtcsPattern::of).toArray(GavtcsPattern[]::new));
    }

    /**
     * Returns a new {@link RemoveElementsTransformer} removing the {@code /dependencyManagement/dependencies>} node
     * (including all its child
     * dependencies);
     * the removed {@code <dependencies>} node is located under {@code /project} (but not under any profiles);
     * also removes any previous sibling comments and whitespace.
     * <p>
     * The returned {@link RemoveElementsTransformer} instance can be further customized to select profiles
     * or other kinds of sibling nodes to remove.
     * <p>
     * If there is no {@code dependencies} node(s) in the specified context, then the returned
     * {@link RemoveElementsTransformer} exits
     * quietly rather than throwing an exception.
     *
     * @return a new {@link RemoveElementsTransformer} removing dependencies having the specified names
     * @since  5.0.0
     */
    public static <THIS extends RemoveElementsTransformer<ContainerElement, THIS>> RemoveElementsTransformer<ContainerElement, THIS> removeAll() {
        return new RemoveElementsTransformer<>(
                RemoveElementsTransformer.containerElementsMapper(ELEMENT_NAME, OTHER_ELEMENT_NAMES),
                containerElement -> true);
    }

    /**
     * Returns a new {@link RemoveElementsTransformer} removing the {@code /dependencyManagement/dependencies>} node
     * (including all its child
     * dependencies)
     * that is located under {@code /project} (but not under any profiles),
     * if the {@code <dependencies>} node has no element children;
     * also removes any previous sibling comments and whitespace.
     * <p>
     * The returned {@link RemoveElementsTransformer} instance can be further customized to select profiles
     * or other kinds of sibling nodes to remove.
     * <p>
     * If there is no {@code dependencies} node(s) in the specified context, then the returned
     * {@link RemoveElementsTransformer} exits
     * quietly rather than throwing an exception.
     *
     * @return a new {@link RemoveElementsTransformer} removing dependencies having the specified names
     * @since  5.0.0
     */
    public static <THIS extends RemoveElementsTransformer<ContainerElement, THIS>> RemoveElementsTransformer<ContainerElement, THIS> removeEmptyParent() {
        return new RemoveElementsTransformer<>(
                RemoveElementsTransformer.containerElementsMapper(ELEMENT_NAME, OTHER_ELEMENT_NAMES),
                ((Predicate<ContainerElement>) ContainerElement::hasChildElements).negate());
    }
}
