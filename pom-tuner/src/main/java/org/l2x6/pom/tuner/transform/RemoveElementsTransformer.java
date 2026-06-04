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

import eu.maveniverse.domtrip.Element;
import eu.maveniverse.domtrip.Node;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.l2x6.pom.tuner.PomTransformer.ContainerElement;
import org.l2x6.pom.tuner.PomTransformer.GavtcsElement;
import org.l2x6.pom.tuner.PomTransformer.NodeGavtcs;
import org.l2x6.pom.tuner.PomTransformer.ProfileElement;
import org.l2x6.pom.tuner.PomTransformer.RemovableNode;
import org.l2x6.pom.tuner.PomTransformer.TextElement;
import org.l2x6.pom.tuner.PomTransformer.TransformationContext;

/**
 * A generic remover of {@code pom.xml} elements such as {@code <properties>}, their child properties,
 * {@code <modules>}, {@code <module>}, etc.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  5.0.0
 */
public class RemoveElementsTransformer<T extends TextElement, THIS extends RemoveElementsTransformer<T, THIS>>
        extends RemoveTransformer<T, THIS> {

    /**
     * Maps a {@link ProfileElement} to a stream of text elements whose parent is a direct child of the given
     * {@link ProfileElement}.
     * Useful for getting {@code <properties>}, {@code <modules>}, etc.
     *
     * @param  name the name of the text element parent, such as {@code properties} or {@code modules}
     * @return      a {@link Function} mapping a {@link ProfileElement} to a stream of text elements whose parent is a
     *              direct child of the given {@link ProfileElement}
     *
     * @since       5.0.0
     */
    static Function<ProfileElement, Stream<ContainerElement>> containerElementsMapper(String elementName,
            String... otherElementNames) {
        return profile -> profile.getChildContainerElement(elementName, otherElementNames)
                .map(Stream::of)
                .orElse(Stream.empty());
    }

    /**
     * Maps a {@link ProfileElement} to a stream of text elements which are grand children of the given
     * {@link ProfileElement}.
     * Useful for getting children of {@code <properties>}, {@code <modules>}, etc.
     *
     * @param  name the name of the text element parent, such as {@code properties} or {@code modules}
     * @return      a {@link Function} mapping a {@link ProfileElement} to a stream of text elements whose parent is a
     *              direct child of the given {@link ProfileElement}
     *
     * @since       5.0.0
     */
    static Function<ProfileElement, Stream<TextElement>> textGrandChildrenMapper(String name) {
        return profile -> profile.childElementsStream()
                .filter(ch -> name.equals(ch.getElementName()))
                .findFirst()
                .map(parent -> parent.childTextElementsStream())
                .orElse(Stream.empty());
    }

    /**
     * Maps a {@link ProfileElement} to a stream of {@link GavtcsElement} which are children of the given
     * {@link ProfileElement} under the path specified by {@code elementName} and {@code otherElementNames}
     * Useful for getting children of {@code <dependencies>}, {@code <plugins>}, etc.
     *
     * @param  elementName       the first element of the xPath for selecting the target nodes
     * @param  otherElementNames additional elements of the xPath for selecting the target nodes
     * @return                   a {@link Function} mapping a {@link ProfileElement} to a stream of {@link NodeGavtcs}s
     *
     * @since                    5.0.0
     */
    static Function<ProfileElement, Stream<GavtcsElement>> gavtcsElementsMapper(String elementName,
            String... otherElementNames) {
        return profile -> profile.getChildContainerElement(elementName, otherElementNames)
                .map(ContainerElement::childElementsStream)
                .orElse(Stream.empty())
                .map(ContainerElement::asGavtcsElement);
    }

    /**
     * Maps a {@link ProfileElement} to a stream of {@link GavtcsElement} which are children of the given
     * {@link ProfileElement} under the path specified by {@code elementName} and {@code otherElementNames}
     * Useful for getting children of {@code <dependencies>}, {@code <plugins>}, etc.
     *
     * @param  elementName       the first element of the xPath for selecting the target nodes
     * @param  otherElementNames additional elements of the xPath for selecting the target nodes
     * @return                   a {@link Function} mapping a {@link ProfileElement} to a stream of {@link NodeGavtcs}s
     *
     * @since                    5.0.0
     */
    static Function<ProfileElement, Stream<GavtcsElement>> pluginDependenciesMapper(String elementName,
            String... otherElementNames) {
        return profile -> profile.getChildContainerElement(elementName, otherElementNames)
                .map(ContainerElement::childElementsStream)
                .orElse(Stream.empty())
                .flatMap(pluginElement -> pluginElement.getChildContainerElement("dependencies")
                        .map(ContainerElement::childElementsStream)
                        .orElse(Stream.empty()))
                .map(ContainerElement::asGavtcsElement);
    }

    private RemoveElementsTransformer(
            Predicate<String> profileSelector,
            Function<ProfileElement, Stream<T>> profileToTextElements,
            Predicate<T> elementSelector,
            List<Function<Node, List<RemovableNode>>> siblingsSelectors,
            boolean immutableSiblingsSelectors) {
        super(profileSelector, profileToTextElements, elementSelector, siblingsSelectors, immutableSiblingsSelectors);
    }

    RemoveElementsTransformer(
            Function<ProfileElement, Stream<T>> profileToTextElements,
            Predicate<T> elementSelector) {
        this(
                ProfileId.main(),
                profileToTextElements,
                elementSelector,
                Collections.singletonList(Siblings.previousCommentsOrWhitespace()),
                true);
    }

    /**
     * Choose whether the elements should be removed from under the {@code <project>} and/or from under specific profiles;
     * use utility methods in {@link ProfileId} to select profiles by name, including or excluding the {@code <project>}
     * pseudo-profile.
     * <p>
     * Note that this library handles the {@code <project>} element as a profile with a {@code null} {@code id},
     * so the given {@link Predicate}'s {@link Predicate#test(Object)} should handle the {@code null} value
     * as the {@code <project>} element.
     * <p>
     * If none of the {@code from*(*)} methods is called,
     * the default behavior is to remove the matching elements only from under the {@code <project>} element
     * and do nothing with matching elements under {@code <profile>} elements.
     *
     * @param  profileSelector the profile selector to set on the resulting {@link RemoveElementsTransformer}
     * @return                 a copy of this {@link RemoveElementsTransformer} instance with the
     *                         {@link #profileSelector} set to the given {@code profileSelector}
     * @since                  5.0.0
     * @see                    ProfileId
     */
    @SuppressWarnings("unchecked")
    public THIS from(Predicate<String> profileSelector) {
        return (THIS) new RemoveElementsTransformer<>(
                profileSelector,
                profileToRemovedElements,
                elementSelector,
                siblingsSelectors,
                true);
    }

    /**
     * Choose from under which specific profiles should the matching elements be removed; matching elements under the
     * {@code <project>} element will be removed too.
     * Use {@link #fromProfilesOnly(String...)} to avoid removing from under the {@code <project>} element.
     * <p>
     * If none of the {@code from*(*)} methods is called,
     * the default behavior is to remove the matching elements only from under the {@code <project>} element
     * and do nothing with matching elements under {@code <profile>} elements.
     *
     * @param  profileIds the profile {@code id}s to select on the resulting {@link RemoveElementsTransformer} in addition
     *                    to the {@link ProfileId#main()}
     * @return            a copy of this {@link RemoveElementsTransformer} instance with the
     *                    {@link #profileSelector} adjusted
     * @since             5.0.0
     * @see               ProfileId#ids(String...)
     */
    @SuppressWarnings("unchecked")
    public THIS from(String... profileIds) {
        return (THIS) new RemoveElementsTransformer<>(
                ProfileId.ids(profileIds),
                profileToRemovedElements,
                elementSelector,
                siblingsSelectors,
                true);
    }

    /**
     * Choose from under which specific profiles should the matching elements be removed; matching elements under the
     * {@code <project>} element will not be removed.
     * Use {@link #from(String...)} to remove also the matching elements from under the {@code <project>} element.
     * <p>
     * If none of the {@code from*(*)} methods is called,
     * the default behavior is to remove the matching elements only from under the {@code <project>} element
     * and do nothing with matching elements under {@code <profile>} elements.
     *
     * @param  profileIds the profile {@code id}s to select on the resulting {@link RemoveElementsTransformer} (but not the
     *                    {@link ProfileId#main()}
     * @return            a copy of this {@link RemoveElementsTransformer} instance with the
     *                    {@link #profileSelector} adjusted
     * @since             5.0.0
     * @see               ProfileId#idsOnly(String...)
     */
    @SuppressWarnings("unchecked")
    public THIS fromProfilesOnly(String... profileIds) {
        return (THIS) new RemoveElementsTransformer<>(
                ProfileId.idsOnly(profileIds),
                profileToRemovedElements,
                elementSelector,
                siblingsSelectors,
                true);
    }

    /**
     * Select some nodes around the removed node to be removed too.
     * Handy to remove any preceding or subsequent whitespace, comments, and/or empty parent elements.
     * <p>
     * Calling this method replaces the default siblings selector ({@link Siblings#previousCommentsOrWhitespace()}).
     * <p>
     * Call {@code alsoRemove()} without arguments to remove the default siblings selector.
     *
     * @param  siblings one, many or none {@link Siblings} to also remove in addition to primary nodes selected for removal.
     * @return          a copy of this {@link RemoveTransformer} instance with
     *                  {@link #siblingsSelectors} adjusted
     * @since           5.0.0
     * @see             Siblings
     */
    @SuppressWarnings("unchecked")
    public THIS alsoRemove(Siblings... siblings) {
        List<Siblings> sSelectors = new ArrayList<>();
        for (Siblings s : siblings) {
            sSelectors.add(s);
        }
        return (THIS) new RemoveElementsTransformer<>(
                profileSelector,
                profileToRemovedElements,
                elementSelector,
                Collections.unmodifiableList(sSelectors),
                true);
    }

    @Override
    public void perform(TransformationContext context) {

        List<RemovableNode> nodesToRemove = new ArrayList<>();
        context.getProfilesStream()
                .filter(profile -> profileSelector.test(profile.getId()))
                .flatMap(profileToRemovedElements)
                .filter(elementSelector)
                .forEach(textNode -> {
                    final Element removedNode = textNode.getNode();
                    nodesToRemove.add(RemovableNode.of(removedNode));
                    for (Function<Node, List<RemovableNode>> siblingsSelector : siblingsSelectors) {
                        nodesToRemove.addAll(siblingsSelector.apply(removedNode));
                    }
                });
        for (RemovableNode node : nodesToRemove) {
            node.remove();
        }
    }

    static <T> List<T> add(List<T> list, T element) {
        List<T> result = new ArrayList<>(list);
        result.add(element);
        return Collections.unmodifiableList(result);
    }

}
