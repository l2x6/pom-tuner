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
import org.l2x6.pom.tuner.model.Gav;
import org.l2x6.pom.tuner.model.GavPattern;
import org.l2x6.pom.tuner.model.GavSet;
import org.l2x6.pom.tuner.transform.api.AddGavTransformer;
import org.l2x6.pom.tuner.transform.api.ElementSet;
import org.l2x6.pom.tuner.transform.api.RemoveElementsTransformer;

/**
 * Operations on {@code pom.xml} plugins usable with {@link PomTransformer#transform(Transformer...)}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  5.0.0
 */
public interface Plugins {

    public static final String ELEMENT_NAME = "build";
    public static final String OTHER_ELEMENT_NAMES = "plugins";

    /**
     * If the given plugin is available already, does nothing; otherwise adds the given plugin as the last element
     * under {@code /project/build/plugins}.
     * <p>
     * The returned {@link AddGavTransformer} instance can be further customized to target a specific profile using
     * {@link AddGavTransformer#intoProfile(String)}
     * or to insert the plugin at some specific position using {@link AddGavTransformer#before(Gav)},
     * {@link AddGavTransformer#after(Gav)} or {@link AddGavTransformer#at(Comparator)} and
     * compatible {@link Comparators}.
     *
     * @param  plugin the plugin to add
     * @return        a new customizable {@link AddGavTransformer}
     *
     * @since         5.0.0
     */
    public static <THIS extends AddGavTransformer<ContainerElement, GavtcsElement, THIS>> AddGavTransformer<ContainerElement, GavtcsElement, THIS> add(
            Gav plugin) {
        return new AddGavTransformer<>(
                profile -> profile.getOrAddChildContainerElement(ELEMENT_NAME)
                        .getOrAddChildContainerElement(OTHER_ELEMENT_NAMES),
                (parent, comparator) -> parent.addGavIfNeeded(plugin, comparator),
                Comparators.afterLast());
    }

    /**
     * Returns a new {@link RemoveElementsTransformer} removing plugins fulfilling the specified {@code predicate};
     * the removed plugins are located under {@code /project/build/plugins} (but not under any profiles);
     * also removes any previous sibling comments and whitespace.
     * <p>
     * The returned {@link RemoveElementsTransformer} instance can be further customized to select profiles
     * or other kinds of sibling nodes to remove.
     * <p>
     * If no plugins fulfill the specified {@code predicate} then the returned {@link RemoveElementsTransformer} exits
     * quietly rather than throwing an exception.
     * <p>
     * Tip: {@link GavSet} implements {@code Predicate<Gav>} and can be used as an argument for this method.
     *
     * @param  <THIS>    type of the returned {@link RemoveElementsTransformer}
     * @param  predicate a {@link Predicate} selecting plugins to remove
     * @return           a new {@link RemoveElementsTransformer} having its node selector set as specified
     * @since            5.0.0
     */
    public static <THIS extends RemoveElementsTransformer<GavtcsElement, THIS>> RemoveElementsTransformer<GavtcsElement, THIS> remove(
            Predicate<Gav> predicate) {
        return new RemoveElementsTransformer<>(
                RemoveElementsTransformer.gavtcsElementsMapper(ELEMENT_NAME, OTHER_ELEMENT_NAMES),
                textElement -> predicate.test(textElement.getGavtcs().toGavtc().toGav()));
    }

    /**
     * Returns a new {@link RemoveElementsTransformer} removing plugins having the specified {@code gavs};
     * the removed plugins are located under {@code /project/build/plugins} (but not under any profiles);
     * also removes any previous sibling comments and whitespace.
     * <p>
     * The returned {@link RemoveElementsTransformer} instance can be further customized to select profiles
     * or other kinds of sibling nodes to remove.
     * <p>
     * If no plugins match the given {@code gavs} then the returned {@link RemoveElementsTransformer} exits
     * quietly rather than throwing an exception.
     *
     * @param  gavs of the plugins to remove
     * @return      a new {@link RemoveElementsTransformer} removing plugins matching the specified {@code patterns}
     * @since       5.0.0
     */
    public static <THIS extends RemoveElementsTransformer<GavtcsElement, THIS>> RemoveElementsTransformer<GavtcsElement, THIS> remove(
            Gav... gavs) {
        final Set<Gav> set = PomTunerUtils.toLinkedHashSet(gavs);
        return new RemoveElementsTransformer<>(
                RemoveElementsTransformer.gavtcsElementsMapper(ELEMENT_NAME, OTHER_ELEMENT_NAMES),
                textElement -> set.contains(textElement.getGavtcs().toGavtc().toGav()));
    }

    /**
     * Returns a new {@link RemoveElementsTransformer} removing plugins matching any of the specified {@code patterns};
     * the removed plugins are located under {@code /project/build/plugins} (but not under any profiles);
     * also removes any previous sibling comments and whitespace.
     * <p>
     * The format of {@code patterns} is {@code groupId[:artifactId[:version]]}.
     * In addition to syntax specified in {@link GavPattern#of(String)}, the entries can be prefixed with {@code !} to be
     * interpreted as excludes.
     * This method is a shorthand for {@link #remove(Predicate)
     * remove(GavSet.builder().includes(patterns).build())}.
     * <p>
     * The returned {@link RemoveElementsTransformer} instance can be further customized to select profiles
     * or other kinds of sibling nodes to remove.
     * <p>
     * If no plugins match the given {@code patterns} then the returned {@link RemoveElementsTransformer} exits
     * quietly rather than throwing an exception.
     *
     * @param  patterns of the plugins to remove
     * @return          a new {@link RemoveElementsTransformer} removing plugins matching the specified {@code patterns}
     * @since           5.0.0
     */
    public static <THIS extends RemoveElementsTransformer<GavtcsElement, THIS>> RemoveElementsTransformer<GavtcsElement, THIS> remove(
            String... patterns) {
        return remove(GavSet.builder().includes(patterns).build());
    }

    /**
     * Returns a new {@link RemoveElementsTransformer} removing the {@code <plugins>} node (including all its child
     * plugins);
     * the removed {@code <plugins>} node is located under {@code /project/build} (but not under any profiles);
     * also removes any previous sibling comments and whitespace.
     * <p>
     * The returned {@link RemoveElementsTransformer} instance can be further customized to select profiles
     * or other kinds of sibling nodes to remove.
     * <p>
     * If there is no {@code plugins} node(s) in the specified context, then the returned
     * {@link RemoveElementsTransformer} exits
     * quietly rather than throwing an exception.
     *
     * @return a new {@link RemoveElementsTransformer} removing the {@code <plugins>} node
     * @since  5.0.0
     */
    public static <THIS extends RemoveElementsTransformer<ContainerElement, THIS>> RemoveElementsTransformer<ContainerElement, THIS> removeAll() {
        return new RemoveElementsTransformer<>(
                RemoveElementsTransformer.containerElementsMapper(ELEMENT_NAME, OTHER_ELEMENT_NAMES),
                containerElement -> true);
    }

    /**
     * Returns a new {@link RemoveElementsTransformer} removing the {@code <plugins>} node (including all its child
     * plugins)
     * that is located under {@code /project} (but not under any profiles),
     * if the {@code <plugins>} node has no child elements;
     * also removes any previous sibling comments and whitespace.
     * <p>
     * The returned {@link RemoveElementsTransformer} instance can be further customized to select profiles
     * or other kinds of sibling nodes to remove.
     * <p>
     * If there is no {@code plugins} node(s) in the specified context, then the returned
     * {@link RemoveElementsTransformer} exits
     * quietly rather than throwing an exception.
     *
     * @return a new {@link RemoveElementsTransformer} removing the empty {@code <plugins>} parent node
     * @since  5.0.0
     */
    public static <THIS extends RemoveElementsTransformer<ContainerElement, THIS>> RemoveElementsTransformer<ContainerElement, THIS> removeEmptyParent() {
        return new RemoveElementsTransformer<>(
                RemoveElementsTransformer.containerElementsMapper(ELEMENT_NAME, OTHER_ELEMENT_NAMES),
                ((Predicate<ContainerElement>) ContainerElement::hasChildElements).negate());
    }

    /**
     * Select some {@code <plugin>} nodes for modification.
     * <p>
     * The returned {@link ElementSet} instance can be further customized to select profiles and/or specify the actual
     * modification operation.
     * <p>
     * Tip: {@link GavSet} implements {@code Predicate<Gav>} and can be used as an argument for this method.
     * <p>
     * If none of the {@code from*(*)} methods of the returned {@link ElementSet} is called,
     * the default behavior is to select the matching elements only from under the {@code <project>} element
     * and ignore any matching elements under {@code <profile>} elements.
     *
     * @param  <THIS>    type of the returned {@link ElementSet}
     * @param  predicate a {@link Predicate} selecting plugin nodes to modify
     * @return           a new {@link ElementSet} having its node selector set as specified
     * @since            5.0.0
     */
    public static <THIS extends ElementSet<GavtcsElement, THIS>> ElementSet<GavtcsElement, THIS> select(
            Predicate<Gav> predicate) {
        return new ElementSet<>(RemoveElementsTransformer.gavtcsElementsMapper(ELEMENT_NAME, OTHER_ELEMENT_NAMES),
                gavtcsElement -> predicate.test(gavtcsElement.getGavtcs().toGavtc().toGav()));
    }

    /**
     * Select some {@code <plugin>} nodes for modification by an array of
     * {@code groupId[:artifactId[:version]]} patterns.
     * In addition to syntax specified in {@link GavPattern#of(String)}, the entries can be prefixed with {@code !} to be
     * interpreted as excludes.
     * This method is a shorthand for {@link #select(Predicate)
     * select(GavSet.builder().includes(patterns).build())}.
     * <p>
     * The returned {@link ElementSet} instance can be further customized to select profiles and/or specify the actual
     * modification operation.
     * <p>
     * If none of the {@code from*(*)} methods of the returned {@link ElementSet} is called,
     * the default behavior is to select the matching elements only from under the {@code <project>} element
     * and ignore any matching elements under {@code <profile>} elements.
     *
     * @param  <THIS>   type of the returned {@link ElementSet}
     * @param  patterns an array of strings parseable by {@link GavPattern#of(String)}
     * @return          a new {@link ElementSet} having its node selector set as specified
     * @since           5.0.0
     */
    public static <THIS extends ElementSet<GavtcsElement, THIS>> ElementSet<GavtcsElement, THIS> select(
            String... patterns) {
        return select(GavSet.builder().includes(patterns).build());
    }

    /**
     * Select some {@code <plugin>} nodes for modification.
     * <p>
     * The returned {@link ElementSet} instance can be further customized to select profiles and/or specify the actual
     * modification operation.
     * <p>
     * If none of the {@code from*(*)} methods of the returned {@link ElementSet} is called,
     * the default behavior is to select the matching elements only from under the {@code <project>} element
     * and ignore any matching elements under {@code <profile>} elements.
     *
     * @param  <THIS>  type of the returned {@link ElementSet}
     * @param  plugins to select for modification
     * @return         a new {@link ElementSet} having its node selector set as specified
     * @since          5.0.0
     */
    public static <THIS extends ElementSet<GavtcsElement, THIS>> ElementSet<GavtcsElement, THIS> select(Gav... plugins) {
        final Set<Gav> set = PomTunerUtils.toLinkedHashSet(plugins);
        return new ElementSet<>(RemoveElementsTransformer.gavtcsElementsMapper(ELEMENT_NAME, OTHER_ELEMENT_NAMES),
                gavtcsElement -> set.contains(gavtcsElement.getGavtcs().toGavtc().toGav()));
    }

    /**
     * Select all {@code <plugin>} nodes for modification.
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

    /**
     * Select {@code <dependency>} nodes of the specified plugins for modification.
     * <p>
     * The returned {@link ElementSet} instance can be further customized to select profiles and/or specify the actual
     * modification operation.
     * <p>
     * Tip: {@link GavSet} implements {@code Predicate<Gav>} and can be used as an argument for this method.
     * <p>
     * If none of the {@code from*(*)} methods of the returned {@link ElementSet} is called,
     * the default behavior is to select the matching elements only from under the {@code <project>} element
     * and ignore any matching elements under {@code <profile>} elements.
     *
     * @param  <THIS>    type of the returned {@link ElementSet}
     * @param  predicate a {@link Predicate} selecting plugin nodes whose descendant dependency nodes will be selected for
     *                   modification
     * @return           a new {@link ElementSet} having its node selector set as specified
     * @since            5.0.0
     */
    public static <THIS extends ElementSet<GavtcsElement, THIS>> ElementSet<GavtcsElement, THIS> selectPluginDependencies(
            Predicate<Gav> predicate) {
        return new ElementSet<>(RemoveElementsTransformer.pluginDependenciesMapper(ELEMENT_NAME, OTHER_ELEMENT_NAMES),
                gavtcsElement -> predicate.test(gavtcsElement.getGavtcs().toGavtc().toGav()));
    }

    /**
     * Select {@code <dependency>} nodes of the specified plugins for modification.
     * The plugins are selected by an array of {@code groupId[:artifactId[:version]]} patterns.
     * In addition to syntax specified in {@link GavPattern#of(String)}, the entries can be prefixed with {@code !} to be
     * interpreted as excludes.
     * This method is a shorthand for {@link #selectPluginDependencies(Predicate)
     * selectPluginDependencies(GavSet.builder().includes(patterns).build())}.
     * <p>
     * The returned {@link ElementSet} instance can be further customized to select profiles and/or specify the actual
     * modification operation.
     * <p>
     * If none of the {@code from*(*)} methods of the returned {@link ElementSet} is called,
     * the default behavior is to select the matching elements only from under the {@code <project>} element
     * and ignore any matching elements under {@code <profile>} elements.
     *
     * @param  <THIS>   type of the returned {@link ElementSet}
     * @param  patterns an array of strings parseable by {@link GavPattern#of(String)}
     * @return          a new {@link ElementSet} having its node selector set as specified
     * @since           5.0.0
     */
    public static <THIS extends ElementSet<GavtcsElement, THIS>> ElementSet<GavtcsElement, THIS> selectPluginDependencies(
            String... patterns) {
        return selectPluginDependencies(GavSet.builder().includes(patterns).build());
    }

    /**
     * Select {@code <dependency>} nodes of the specified plugins for modification.
     * <p>
     * The returned {@link ElementSet} instance can be further customized to select profiles and/or specify the actual
     * modification operation.
     * <p>
     * If none of the {@code from*(*)} methods of the returned {@link ElementSet} is called,
     * the default behavior is to select the matching elements only from under the {@code <project>} element
     * and ignore any matching elements under {@code <profile>} elements.
     *
     * @param  <THIS>  type of the returned {@link ElementSet}
     * @param  plugins to select for modification
     * @return         a new {@link ElementSet} having its node selector set as specified
     * @since          5.0.0
     */
    public static <THIS extends ElementSet<GavtcsElement, THIS>> ElementSet<GavtcsElement, THIS> selectPluginDependencies(
            Gav... plugins) {
        final Set<Gav> set = PomTunerUtils.toLinkedHashSet(plugins);
        return new ElementSet<>(RemoveElementsTransformer.pluginDependenciesMapper(ELEMENT_NAME, OTHER_ELEMENT_NAMES),
                gavtcsElement -> set.contains(gavtcsElement.getGavtcs().toGavtc().toGav()));
    }

    /**
     * Select {@code <dependency>} nodes of all plugins for modification.
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
    public static <THIS extends ElementSet<GavtcsElement, THIS>> ElementSet<GavtcsElement, THIS> selectAllPluginDependencies() {
        return new ElementSet<>(RemoveElementsTransformer.pluginDependenciesMapper(ELEMENT_NAME, OTHER_ELEMENT_NAMES),
                textElement -> true);
    }
}
