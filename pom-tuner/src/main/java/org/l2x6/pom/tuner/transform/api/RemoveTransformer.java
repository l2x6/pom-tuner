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
package org.l2x6.pom.tuner.transform.api;

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
import org.l2x6.pom.tuner.PomTransformer.Transformer;

/**
 * A generic removed of {@code pom.xml} elements such as {@code <properties>}, their child properties,
 * {@code <modules>}, {@code <module>}, etc.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  5.0.0
 */
public class RemoveTransformer<T extends TextElement, THIS extends RemoveTransformer<T, THIS>> implements Transformer {

    final Predicate<String> profileSelector;
    final Function<ProfileElement, Stream<T>> profileToRemovedElements;
    final Predicate<T> elementSelector;
    final List<Function<Node, List<RemovableNode>>> siblingsSelectors;

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
    public static Function<ProfileElement, Stream<ContainerElement>> containerElementsMapper(String elementName,
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
    public static Function<ProfileElement, Stream<TextElement>> textGrandChildrenMapper(String name) {
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
    public static Function<ProfileElement, Stream<GavtcsElement>> gavtcsElementsMapper(String elementName,
            String... otherElementNames) {
        return profile -> profile.getChildContainerElement(elementName, otherElementNames)
                .map(ContainerElement::childElementsStream)
                .orElse(Stream.empty())
                .map(ContainerElement::asGavtcsElement);
    }

    RemoveTransformer(
            Predicate<String> profileSelector,
            Function<ProfileElement, Stream<T>> profileToTextElements,
            Predicate<T> elementSelector,
            List<Function<Node, List<RemovableNode>>> siblingsSelectors,
            boolean immutableSiblingsSelectors) {
        this.profileSelector = profileSelector;
        this.profileToRemovedElements = profileToTextElements;
        this.elementSelector = elementSelector;
        this.siblingsSelectors = immutableSiblingsSelectors
                ? siblingsSelectors
                : Collections.unmodifiableList(new ArrayList<>(siblingsSelectors));
    }

    public RemoveTransformer(
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
     * Select some nodes around the removed node to be removed too.
     * Handy to remove any preceding or subsequent whitespace, comments, and/or empty parent elements.
     * <p>
     * This is a cumulative operation: the specified {@code siblingsSelector} is added to sibling selectors currently
     * available in {@link #siblingsSelectors}.
     * Use {@link #alsoRemoveNone()} to remove the default sibling selectors.
     *
     * @param  siblingsSelector a {@link Function} that for given removed node returns a list of nodes that should also be
     *                          removed.
     * @return                  a copy of this {@link RemoveTransformer} instance with
     *                          {@link #siblingsSelectors} adjusted
     * @since                   5.0.0
     * @see                     Siblings
     */
    @SuppressWarnings("unchecked")
    public THIS alsoRemove(Function<Node, List<RemovableNode>> siblingsSelector) {
        return (THIS) new RemoveTransformer<>(
                profileSelector,
                profileToRemovedElements,
                elementSelector,
                add(siblingsSelectors, siblingsSelector),
                true);
    }

    /**
     * Select some nodes preceding the removed node to be removed too.
     * Handy to remove any preceding sibling whitespace and/or comments.
     * <p>
     * This is a cumulative operation: {@code Siblings.previous(nodeSelector)} is added to sibling selectors currently
     * available in {@link #siblingsSelectors}.
     * Use {@link #alsoRemoveNone()} to remove the default sibling selectors.
     *
     * @param  nodeSelector a {@link Predicate} deciding which of the preceding siblings should be removed; the siblings are
     *                      iterated while {@code nodeSelector} returns {@code true}
     * @return              a copy of this {@link RemoveTransformer} instance with
     *                      {@link #siblingsSelectors} adjusted
     * @since               5.0.0
     * @see                 Siblings#previous(Predicate)
     */
    @SuppressWarnings("unchecked")
    public THIS alsoRemovePrevious(Predicate<Node> nodeSelector) {
        return (THIS) new RemoveTransformer<>(
                profileSelector,
                profileToRemovedElements,
                elementSelector,
                add(siblingsSelectors, Siblings.previous(nodeSelector)),
                true);
    }

    /**
     * Select some nodes following the removed node to be removed too.
     * Handy to remove any subsequent sibling whitespace and/or comments.
     * <p>
     * This is a cumulative operation: {@code Siblings.next(nodeSelector)} is added to sibling selectors currently available
     * in {@link #siblingsSelectors}.
     * Use {@link #alsoRemoveNone()} to remove the default sibling selectors.
     *
     * @param  nodeSelector a {@link Predicate} deciding which of the following siblings should be removed; the siblings are
     *                      iterated while {@code nodeSelector} returns {@code true}
     * @return              a copy of this {@link RemoveTransformer} instance with
     *                      {@link #siblingsSelectors} adjusted
     * @since               5.0.0
     * @see                 Siblings#next(Predicate)
     */
    @SuppressWarnings("unchecked")
    public THIS alsoRemoveNext(Predicate<Node> nodeSelector) {
        return (THIS) new RemoveTransformer<>(
                profileSelector,
                profileToRemovedElements,
                elementSelector,
                add(siblingsSelectors, Siblings.next(nodeSelector)),
                true);
    }

    /**
     * Do not select any nodes following or preceding the removed node (such as whitespace or comments) for removal.
     * <p>
     * Use {@link #alsoRemove(Function)} {@link #alsoRemoveNext(Predicate)} or {@link #alsoRemovePrevious(Predicate)}
     * to select additional neighbor nodes for removal.
     *
     * @return a copy of this {@link RemoveTransformer} instance with
     *         {@link #siblingsSelectors} adjusted
     * @since  5.0.0
     * @see    Siblings
     */
    @SuppressWarnings("unchecked")
    public THIS alsoRemoveNone() {
        return (THIS) new RemoveTransformer<>(
                profileSelector,
                profileToRemovedElements,
                elementSelector,
                Collections.emptyList(),
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
