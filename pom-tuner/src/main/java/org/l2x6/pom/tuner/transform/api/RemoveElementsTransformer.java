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
import org.l2x6.pom.tuner.PomTransformer.TextElement;
import org.l2x6.pom.tuner.PomTransformer.TransformationContext;
import org.l2x6.pom.tuner.PomTransformer.Transformer;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * A generic remover of {@code pom.xml} elements such as {@code <properties>}, their child properties,
 * {@code <modules>}, {@code <module>}, etc.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  5.0.0
 */
public class RemoveElementsTransformer<T extends TextElement, THIS extends RemoveElementsTransformer<T, THIS>> extends RemoveTransformer<T, THIS> {

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

    private RemoveElementsTransformer(
            Predicate<String> profileSelector,
            Function<ProfileElement, Stream<T>> profileToTextElements,
            Predicate<T> elementSelector,
            List<Function<Node, List<Node>>> siblingsSelectors,
            boolean immutableSiblingsSelectors) {
        super(profileSelector, profileToTextElements, elementSelector, siblingsSelectors, immutableSiblingsSelectors);
    }

    public RemoveElementsTransformer(
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
     * use utility methods in {@link ProfileId} to select profiles by name, including or excluding the {@code <project>} pseudo-profile.
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
     * Choose from under which specific profiles should the matching elements be removed; matching elements under the {@code <project>} element will be removed too.
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
     * Choose from under which specific profiles should the matching elements be removed; matching elements under the {@code <project>} element will not be removed.
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
     * This is a cumulative operation: the specified {@code siblingsSelector} is added to sibling selectors currently
     * available in {@link #siblingsSelectors}.
     * Use {@link #alsoRemoveNone()} to remove the default sibling selectors.
     *
     * @param  siblingsSelector a {@link Function} that for given removed node returns a list of nodes that should also be
     *                          removed.
     * @return                  a copy of this {@link RemoveElementsTransformer} instance with
     *                          {@link #siblingsSelectors} adjusted
     * @since                   5.0.0
     * @see                     Siblings
     */
    @SuppressWarnings("unchecked")
    public THIS alsoRemove(Function<Node, List<Node>> siblingsSelector) {
        return (THIS) new RemoveElementsTransformer<>(
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
     * @return              a copy of this {@link RemoveElementsTransformer} instance with
     *                      {@link #siblingsSelectors} adjusted
     * @since               5.0.0
     * @see                 Siblings#previous(Predicate)
     */
    @SuppressWarnings("unchecked")
    public THIS alsoRemovePrevious(Predicate<Node> nodeSelector) {
        return (THIS) new RemoveElementsTransformer<>(
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
     * @return              a copy of this {@link RemoveElementsTransformer} instance with
     *                      {@link #siblingsSelectors} adjusted
     * @since               5.0.0
     * @see                 Siblings#next(Predicate)
     */
    @SuppressWarnings("unchecked")
    public THIS alsoRemoveNext(Predicate<Node> nodeSelector) {
        return (THIS) new RemoveElementsTransformer<>(
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
     * @return a copy of this {@link RemoveElementsTransformer} instance with
     *         {@link #siblingsSelectors} adjusted
     * @since  5.0.0
     * @see    Siblings
     */
    @SuppressWarnings("unchecked")
    public THIS alsoRemoveNone() {
        return (THIS) new RemoveElementsTransformer<>(
                profileSelector,
                profileToRemovedElements,
                elementSelector,
                Collections.emptyList(),
                true);
    }

    @Override
    public void perform(TransformationContext context) {

        List<Node> nodesToRemove = new ArrayList<>();
        context.getProfilesStream()
                .filter(profile -> profileSelector.test(profile.getId()))
                .flatMap(profileToRemovedElements)
                .filter(elementSelector)
                .forEach(textNode -> {
                    final Element removedNode = textNode.getNode();
                    nodesToRemove.add(removedNode);
                    for (Function<Node, List<Node>> siblingsSelector : siblingsSelectors) {
                        nodesToRemove.addAll(siblingsSelector.apply(removedNode));
                    }
                });
        for (Node node : nodesToRemove) {
            final Node parent = node.getParentNode();
            if (parent != null) {
                parent.removeChild(node);
            }
        }
    }

    static <T> List<T> add(List<T> list, T element) {
        List<T> result = new ArrayList<>(list);
        result.add(element);
        return Collections.unmodifiableList(result);
    }

}
