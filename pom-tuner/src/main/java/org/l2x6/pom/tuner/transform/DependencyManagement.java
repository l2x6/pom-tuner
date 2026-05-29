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
import java.util.Set;
import java.util.function.Predicate;
import org.l2x6.pom.tuner.Comparators;
import org.l2x6.pom.tuner.PomTransformer;
import org.l2x6.pom.tuner.PomTransformer.ContainerElement;
import org.l2x6.pom.tuner.PomTransformer.GavtcsElement;
import org.l2x6.pom.tuner.PomTransformer.Transformer;
import org.l2x6.pom.tuner.PomTunerUtils;
import org.l2x6.pom.tuner.model.Gavtcs;
import org.l2x6.pom.tuner.model.GavtcsPattern;
import org.l2x6.pom.tuner.model.GavtcsSet;
import org.l2x6.pom.tuner.transform.api.AddGavtcsTransformer;
import org.l2x6.pom.tuner.transform.api.ElementSet;
import org.l2x6.pom.tuner.transform.api.RemoveElementsTransformer;

/**
 * Operations on {@code pom.xml} {@code dependencyManagement} usable with
 * {@link PomTransformer#transform(Transformer...)}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  5.0.0
 */
public interface DependencyManagement {

    public static final String ELEMENT_NAME = "dependencyManagement";
    public static final String OTHER_ELEMENT_NAMES = "dependencies";

    /**
     * If the given dependency management entry is available already, does nothing; otherwise adds the given dependency as
     * the last element
     * under {@code /project/dependencyManagement/dependencies}.
     * <p>
     * The returned {@link AddGavtcsTransformer} instance can be further customized to target a specific profile using
     * {@link AddGavtcsTransformer#intoProfile(String)}
     * or to insert the dependency at some specific position using {@link AddGavtcsTransformer#before(Gavtcs)},
     * {@link AddGavtcsTransformer#after(Gavtcs)} or {@link AddGavtcsTransformer#at(Comparator)} and
     * compatible {@link Comparators}.
     *
     * @param  dependency the dependency to add
     * @return            a new customizable {@link AddGavtcsTransformer}
     *
     * @since             5.0.0
     */
    public static <THIS extends AddGavtcsTransformer<ContainerElement, GavtcsElement, THIS>> AddGavtcsTransformer<ContainerElement, GavtcsElement, THIS> add(
            Gavtcs dependency) {
        return new AddGavtcsTransformer<>(
                profile -> profile.getOrAddChildContainerElement(ELEMENT_NAME)
                        .getOrAddChildContainerElement(OTHER_ELEMENT_NAMES),
                (parent, comparator) -> parent.addGavtcsIfNeeded(dependency, comparator),
                Comparators.afterLast());
    }

    /**
     * Returns a new {@link RemoveElementsTransformer} removing {@code dependencyManagement} entries fulfilling the
     * specified {@code predicate};
     * the removed {@code dependencyManagement} entries are located under {@code /project/dependencyManagement/dependencies}
     * (but not under any profiles);
     * also removes any previous sibling comments and whitespace.
     * <p>
     * The returned {@link RemoveElementsTransformer} instance can be further customized to select profiles
     * or other kinds of sibling nodes to remove.
     * <p>
     * If no {@code dependencyManagement} entries fulfill the specified {@code predicate} then the returned
     * {@link RemoveElementsTransformer} exits
     * quietly rather than throwing an exception.
     * <p>
     * Tip: {@link GavtcsSet} implements {@code Predicate<Gavtcs>} and can be used as an argument for this method.
     *
     * @param  <THIS>    type of the returned {@link RemoveElementsTransformer}
     * @param  predicate a {@link Predicate} selecting {@code dependencyManagement} entries to remove
     * @return           a new {@link RemoveElementsTransformer} having its node selector set as specified
     * @since            5.0.0
     */
    public static <THIS extends RemoveElementsTransformer<GavtcsElement, THIS>> RemoveElementsTransformer<GavtcsElement, THIS> remove(
            Predicate<Gavtcs> predicate) {
        return new RemoveElementsTransformer<>(
                RemoveElementsTransformer.gavtcsElementsMapper(ELEMENT_NAME, OTHER_ELEMENT_NAMES),
                textElement -> predicate.test(textElement.getGavtcs()));
    }

    /**
     * Returns a new {@link RemoveElementsTransformer} removing the specified {@code dependencyManagement} entries;
     * the removed {@code dependencyManagement} entries are located under
     * {@code /project/dependencyManagement/dependencies} (but not under any profiles);
     * also removes any previous sibling comments and whitespace.
     * <p>
     * The returned {@link RemoveElementsTransformer} instance can be further customized to select profiles
     * or other kinds of sibling nodes to remove.
     * <p>
     * If no {@code dependencyManagement} entries match the given {@code dependencyNames} then the returned
     * {@link RemoveElementsTransformer} exits
     * quietly rather than throwing an exception.
     *
     * @param  dependencies the {@code dependencyManagement} entries to remove
     * @return              a new {@link RemoveElementsTransformer} removing the specified {@code dependencies}
     * @since               5.0.0
     */
    public static <THIS extends RemoveElementsTransformer<GavtcsElement, THIS>> RemoveElementsTransformer<GavtcsElement, THIS> remove(
            Gavtcs... dependencies) {
        final Set<Gavtcs> set = PomTunerUtils.toLinkedHashSet(dependencies);
        return new RemoveElementsTransformer<>(
                RemoveElementsTransformer.gavtcsElementsMapper(ELEMENT_NAME, OTHER_ELEMENT_NAMES),
                textElement -> set.contains(textElement.getGavtcs()));
    }

    /**
     * Returns a new {@link RemoveElementsTransformer} removing {@code dependencyManagement} entries matching any of the
     * specified {@code patterns};
     * the removed {@code dependencyManagement} entries are located under
     * {@code /project/dependencyManagement/dependencies} (but not under any profiles);
     * also removes any previous sibling comments and whitespace.
     * <p>
     * The format of {@code patterns} is {@code groupId[:artifactId[:version[:type[:classifier[:scope]]]]]}.
     * In addition to syntax specified in {@link GavtcsPattern#of(String)}, the entries can be prefixed with {@code !} to be
     * interpreted as excludes.
     * This method is a shorthand for {@link #remove(Predicate)
     * remove(GavtcsSet.builder().includes(patterns).build())}.
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
    public static <THIS extends RemoveElementsTransformer<GavtcsElement, THIS>> RemoveElementsTransformer<GavtcsElement, THIS> remove(
            String... patterns) {
        return remove(GavtcsSet.builder().includes(patterns).build());
    }

    /**
     * Returns a new {@link RemoveElementsTransformer} removing the {@code /project/dependencyManagement/dependencies} node
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
     * @return a new {@link RemoveElementsTransformer} removing the {@code /project/dependencyManagement/dependencies} node
     * @since  5.0.0
     */
    public static <THIS extends RemoveElementsTransformer<ContainerElement, THIS>> RemoveElementsTransformer<ContainerElement, THIS> removeAll() {
        return new RemoveElementsTransformer<>(
                RemoveElementsTransformer.containerElementsMapper(ELEMENT_NAME, OTHER_ELEMENT_NAMES),
                containerElement -> true);
    }

    /**
     * Returns a new {@link RemoveElementsTransformer} removing the {@code /project/dependencyManagement/dependencies} node
     * if the {@code <dependencies>} node has no child elements;
     * also removes any previous sibling comments and whitespace.
     * <p>
     * The returned {@link RemoveElementsTransformer} instance can be further customized to select profiles
     * or other kinds of sibling nodes to remove.
     * <p>
     * If there is no {@code dependencies} node(s) in the specified context, then the returned
     * {@link RemoveElementsTransformer} exits
     * quietly rather than throwing an exception.
     *
     * @return a new {@link RemoveElementsTransformer} removing the empty {@code /project/dependencyManagement/dependencies}
     *         parent node
     * @since  5.0.0
     */
    public static <THIS extends RemoveElementsTransformer<ContainerElement, THIS>> RemoveElementsTransformer<ContainerElement, THIS> removeEmptyParent() {
        return new RemoveElementsTransformer<>(
                RemoveElementsTransformer.containerElementsMapper(ELEMENT_NAME, OTHER_ELEMENT_NAMES),
                ((Predicate<ContainerElement>) ContainerElement::hasChildElements).negate());
    }

    /**
     * Select some {@code <dependency>} nodes for modification.
     * <p>
     * The returned {@link ElementSet} instance can be further customized to select profiles and/or specify the actual
     * modification operation.
     * <p>
     * Tip: {@link GavtcsSet} implements {@code Predicate<Gavtcs>} and can be used as an argument for this method.
     * <p>
     * If none of the {@code from*(*)} methods of the returned {@link ElementSet} is called,
     * the default behavior is to select the matching elements only from under the {@code <project>} element
     * and ignore any matching elements under {@code <profile>} elements.
     *
     * @param  <THIS>    type of the returned {@link ElementSet}
     * @param  predicate a {@link Predicate} selecting dependencies to modify
     * @return           a new {@link ElementSet} having its node selector set as specified
     * @since            5.0.0
     */
    public static <THIS extends ElementSet<GavtcsElement, THIS>> ElementSet<GavtcsElement, THIS> select(
            Predicate<Gavtcs> predicate) {
        return new ElementSet<>(RemoveElementsTransformer.gavtcsElementsMapper(ELEMENT_NAME, OTHER_ELEMENT_NAMES),
                gavtcsElement -> predicate.test(gavtcsElement.getGavtcs()));
    }

    /**
     * Select some {@code <dependency>} nodes for modification by an array of
     * {@code groupId[:artifactId[:version[:type[:classifier[:scope]]]]]} patterns.
     * In addition to syntax specified in {@link GavtcsPattern#of(String)}, the entries can be prefixed with {@code !} to be
     * interpreted as excludes.
     * This method is a shorthand for {@link #select(Predicate)
     * select(GavtcsSet.builder().includes(gavtcsPatterns).build())}.
     * <p>
     * The returned {@link ElementSet} instance can be further customized to select profiles and/or specify the actual
     * modification operation.
     * <p>
     * If none of the {@code from*(*)} methods of the returned {@link ElementSet} is called,
     * the default behavior is to select the matching elements only from under the {@code <project>} element
     * and ignore any matching elements under {@code <profile>} elements.
     *
     * @param  <THIS>         type of the returned {@link ElementSet}
     * @param  gavtcsPatterns an array of strings parseable by GavTcs
     * @return                a new {@link ElementSet} having its node selector set as specified
     * @since                 5.0.0
     */
    public static <THIS extends ElementSet<GavtcsElement, THIS>> ElementSet<GavtcsElement, THIS> select(
            String... gavtcsPatterns) {
        return select(GavtcsSet.builder().includes(gavtcsPatterns).build());
    }

    /**
     * Select some {@code <dependency>} nodes for modification.
     * <p>
     * The returned {@link ElementSet} instance can be further customized to select profiles and/or specify the actual
     * modification operation.
     * <p>
     * If none of the {@code from*(*)} methods of the returned {@link ElementSet} is called,
     * the default behavior is to select the matching elements only from under the {@code <project>} element
     * and ignore any matching elements under {@code <profile>} elements.
     *
     * @param  <THIS>       type of the returned {@link ElementSet}
     * @param  dependencies to select for modification
     * @return              a new {@link ElementSet} having its node selector set as specified
     * @since               5.0.0
     */
    public static <THIS extends ElementSet<GavtcsElement, THIS>> ElementSet<GavtcsElement, THIS> select(
            Gavtcs... dependencies) {
        final Set<Gavtcs> set = PomTunerUtils.toLinkedHashSet(dependencies);
        return new ElementSet<>(RemoveElementsTransformer.gavtcsElementsMapper(ELEMENT_NAME, OTHER_ELEMENT_NAMES),
                gavtcsElement -> set.contains(gavtcsElement.getGavtcs()));
    }

    /**
     * Select all {@code <dependency>} nodes for modification.
     * <p>
     * The returned {@link ElementSet} instance can be further customized to select profiles and/or specify the actual
     * modification operation.
     * <p>
     * If none of the {@code from*(*)} methods of the returned {@link ElementSet} is called,
     * the default behavior is to select the matching elements only from under the {@code <project>} element
     * and ignore any matching elements under {@code <profile>} elements.
     *
     * @param  <THIS> type of the returned {@link ElementSet}
     * @return        a new {@link ElementSet} having its node selector set as specified
     * @since         5.0.0
     */
    public static <THIS extends ElementSet<GavtcsElement, THIS>> ElementSet<GavtcsElement, THIS> selectAll() {
        return new ElementSet<>(RemoveElementsTransformer.gavtcsElementsMapper(ELEMENT_NAME, OTHER_ELEMENT_NAMES),
                textElement -> true);
    }
}
