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

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.l2x6.pom.tuner.PomTransformer.ContainerElement;
import org.l2x6.pom.tuner.PomTransformer.GavtcsElement;
import org.l2x6.pom.tuner.PomTransformer.ProfileElement;
import org.l2x6.pom.tuner.PomTransformer.TextElement;
import org.l2x6.pom.tuner.PomTransformer.Transformation;
import org.l2x6.pom.tuner.PomTransformer.TransformationContext;

/**
 * A set of {@code pom.xml} elements selected for modification.
 *
 * @author        <a href="https://github.com/ppalaga">Peter Palaga</a>
 *
 * @since         5.0.0
 * @param  <T>    the type of elements in this {@link ElementSet}
 * @param  <THIS> the generic type of this {@link ElementSet}
 */
public class ElementSet<T extends TextElement, THIS extends ElementSet<T, THIS>> {

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

    protected final Predicate<String> profileSelector;
    protected final Function<ProfileElement, Stream<T>> getNodes;
    protected final Predicate<T> nodeSelector;

    ElementSet(Function<ProfileElement, Stream<T>> getNodes, Predicate<T> nodeSelector) {
        this.profileSelector = ProfileId.main();
        this.getNodes = getNodes;
        this.nodeSelector = nodeSelector;
    }

    ElementSet(Predicate<String> profileSelector, Function<ProfileElement, Stream<T>> getNodes,
            Predicate<T> nodeSelector) {
        this.profileSelector = profileSelector;
        this.getNodes = getNodes;
        this.nodeSelector = nodeSelector;
    }

    /**
     * Choose whether the matching elements should be selected from under the {@code <project>} and/or from under
     * specific
     * profiles;
     * use utility methods in {@link ProfileId} to select profiles by name, including or excluding the {@code <project>}
     * pseudo-profile.
     * <p>
     * Note that this library handles the {@code <project>} element as a profile with a {@code null} {@code id},
     * so the given {@link Predicate}'s {@link Predicate#test(Object)} should handle the {@code null} value
     * as the {@code <project>} element.
     * <p>
     * If none of the {@code from*(*)} methods is called,
     * the default behavior is to select the matching elements only from under the {@code <project>} element
     * and ignore any matching elements under {@code <profile>} elements.
     *
     * @param  profileSelector the profile selector to set on the resulting {@link ElementSet}
     * @return                 a copy of this {@link ElementSet} instance with the
     *                         {@link #profileSelector} set to the given {@code profileSelector}
     * @since                  5.0.0
     * @see                    ProfileId
     */
    public THIS from(Predicate<String> profileSelector) {
        return create(profileSelector, getNodes, nodeSelector);
    }

    /**
     * Choose from under which specific profiles should the matching elements be selected; matching elements under the
     * {@code <project>} element will be selected too.
     * Use {@link #fromProfilesOnly(String...)} to avoid selecting from under the {@code <project>} element.
     * <p>
     * If none of the {@code from*(*)} methods is called,
     * the default behavior is to select the matching elements only from under the {@code <project>} element
     * and ignore any matching elements under {@code <profile>} elements.
     *
     * @param  profileIds the profile {@code id}s to select on the resulting {@link ElementSet} in addition
     *                    to the {@link ProfileId#main()}
     * @return            a copy of this {@link ElementSet} instance with the
     *                    {@link #profileSelector} adjusted
     * @since             5.0.0
     * @see               ProfileId#ids(String...)
     */
    public THIS from(String... profileIds) {
        return create(ProfileId.ids(profileIds), getNodes, nodeSelector);
    }

    /**
     * Choose from under which specific profiles should the matching elements be selected; matching elements under the
     * {@code <project>} element will not be selected.
     * Use {@link #from(String...)} to select also the matching elements from under the {@code <project>} element.
     * <p>
     * If none of the {@code from*(*)} methods is called,
     * the default behavior is to select the matching elements only from under the {@code <project>} element
     * and ignore any matching elements under {@code <profile>} elements.
     *
     * @param  profileIds the profile {@code id}s to select on the resulting {@link ElementSet} (but not the
     *                    {@link ProfileId#main()}
     * @return            a copy of this {@link ElementSet} instance with the
     *                    {@link #profileSelector} adjusted
     * @since             5.0.0
     * @see               ProfileId#idsOnly(String...)
     */
    public THIS fromProfilesOnly(String... profileIds) {
        return create(ProfileId.idsOnly(profileIds), getNodes, nodeSelector);
    }

    /**
     * Returns an {@link ElementStream} mapping each element using the given {@code mapper} operation.
     *
     * @param  <R>    the type of the returned {@link ElementStream} elements
     * @param  mapper a {@link Function} transforming this {@link ElementSet} elements to something else.
     * @return        a new {@link ElementStream}
     * @since         5.0.0
     */
    public <R> ElementStream<R> map(Function<T, ? extends R> mapper) {
        return new ElementStream<R>(stream().andThen(stream -> stream.map(mapper)));
    }

    /**
     * Returns an {@link ElementStream} navigating through the given descendant path, returning the first matching
     * {@link ContainerElement} at each level.
     *
     * @param  descendantsPath the names of the descendant elements to navigate through
     * @return                 a new {@link ElementStream}
     * @since                  5.0.0
     */
    public ElementStream<ContainerElement> mapFirst(String... descendantsPath) {

        Function<TransformationContext, Stream<ContainerElement>> stream = stream()
                .andThen(s -> s.map(textElement -> (ContainerElement) textElement));

        for (String descendantName : descendantsPath) {
            stream = stream.andThen(s -> s
                    .map(containerElement -> containerElement.getChildContainerElement(descendantName))
                    .filter(Optional::isPresent)
                    .map(Optional::get));
        }
        return new ElementStream<ContainerElement>(stream);
    }

    /**
     * Returns an {@link ElementStream} flat-mapping each element of this {@link ElementSet} using the given
     * {@code mapper}.
     *
     * @param  <R>    the type of the returned {@link ElementStream} elements
     * @param  mapper a {@link Function} transforming each element to a {@link Stream} of results
     * @return        a new {@link ElementStream}
     * @since         5.0.0
     */
    public <R> ElementStream<R> flatMap(Function<T, Stream<? extends R>> mapper) {
        return new ElementStream<R>(stream().andThen(stream -> stream.flatMap(mapper)));
    }

    /**
     * Returns an {@link ElementStream} flat-mapping each element to its child {@link ContainerElement}s.
     *
     * @return a new {@link ElementStream}
     * @since  5.0.0
     */
    public ElementStream<ContainerElement> flatMapChildren() {
        return new ElementStream<>(stream()
                .andThen(s -> s.map(textElement -> ((ContainerElement) textElement))))
                .flatMapChildren();
    }

    /**
     * Returns an {@link ElementStream} flat-mapping each element to its child {@link ContainerElement}s.
     * and converting the resulting elements to {@link GavtcsElement}s.
     *
     * @return a new {@link ElementStream} of {@link GavtcsElement}s
     * @since  5.0.0
     */
    public ElementStream<GavtcsElement> flatMapGavtcs() {
        return flatMapChildren().map(ContainerElement::asGavtcsElement);
    }

    /**
     * Returns a {@link Transformation} that applies the given {@code action} to each selected element.
     *
     * @param  action the consumer to apply to each selected element
     * @return        a new {@link Transformation}
     * @since         5.0.0
     */
    public Transformation forEach(Consumer<T> action) {
        return context -> {
            stream().apply(context)
                    .collect(Collectors.toList()) // create a temporary list to allow deletions, etc.
                    .forEach(action);
        };
    }

    Function<TransformationContext, Stream<T>> stream() {
        return context -> context.getProfilesStream()
                .filter(profile -> profileSelector.test(profile.getId()))
                .flatMap(getNodes)
                .filter(nodeSelector);
    }

    @SuppressWarnings("unchecked")
    protected THIS create(Predicate<String> profileSelector, Function<ProfileElement, Stream<T>> getNodes,
            Predicate<T> nodeSelector) {
        return (THIS) new ElementSet<>(profileSelector, getNodes, nodeSelector);
    }

    /**
     * A stream of items stemming from a {@code pom.xml file}.
     *
     * @param <T> the type of the stream items
     */
    public static class ElementStream<T> {
        private final Function<TransformationContext, Stream<T>> streamSource;

        ElementStream(Function<TransformationContext, Stream<T>> streamSource) {
            this.streamSource = streamSource;
        }

        /**
         * Returns an {@link ElementStream} mapping each element of this {@link ElementStream} using the given
         * {@code mapper}.
         *
         * @param  <R>    the type of the returned {@link ElementStream} elements
         * @param  mapper a {@link Function} transforming each element
         * @return        a new {@link ElementStream}
         * @since         5.0.0
         */
        public <R> ElementStream<R> map(Function<? super T, ? extends R> mapper) {
            return new ElementStream<>(streamSource.andThen(stream -> stream.map(mapper)));
        }

        /**
         * Returns an {@link ElementStream} navigating through the given descendant path, returning the first matching
         * {@link ContainerElement} at each level.
         *
         * @param  descendantsPath the names of the descendant elements to navigate through
         * @return                 a new {@link ElementStream}
         * @since                  5.0.0
         */
        public ElementStream<ContainerElement> mapFirst(String... descendantsPath) {

            Function<TransformationContext, Stream<ContainerElement>> stream = streamSource
                    .andThen(s -> s.map(textElement -> (ContainerElement) textElement));

            for (String descendantName : descendantsPath) {
                stream = stream.andThen(s -> s
                        .map(containerElement -> containerElement.getChildContainerElement(descendantName))
                        .filter(Optional::isPresent)
                        .map(Optional::get));
            }
            return new ElementStream<ContainerElement>(stream);
        }

        /**
         * Returns an {@link ElementStream} flat-mapping each element using the given {@code mapper}.
         *
         * @param  <R>    the type of the returned {@link ElementStream} elements
         * @param  mapper a {@link Function} transforming each element to a {@link Stream} of results
         * @return        a new {@link ElementStream}
         * @since         5.0.0
         */
        public <R> ElementStream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
            return new ElementStream<>(streamSource.andThen(stream -> stream.flatMap(mapper)));
        }

        /**
         * Returns an {@link ElementStream} flat-mapping each element to its child {@link ContainerElement}s.
         *
         * @return a new {@link ElementStream}
         * @since  5.0.0
         */
        public ElementStream<ContainerElement> flatMapChildren() {
            return new ElementStream<>(streamSource
                    .andThen(s -> s
                            .map(textElement -> ((ContainerElement) textElement))
                            .flatMap(ContainerElement::childElementsStream)));
        }

        /**
         * Returns an {@link ElementStream} flat-mapping each element to its child {@link ContainerElement}s.
         * and converting the resulting elements to {@link GavtcsElement}s.
         *
         * @return a new {@link ElementStream} of {@link GavtcsElement}s
         * @since  5.0.0
         */
        public ElementStream<GavtcsElement> flatMapGavtcs() {
            return flatMapChildren().map(ContainerElement::asGavtcsElement);
        }

        /**
         * Returns an {@link ElementStream} containing only the elements matching the given {@code filter}.
         *
         * @param  filter the predicate to filter by
         * @return        a new {@link ElementStream}
         * @since         5.0.0
         */
        public ElementStream<T> filter(Predicate<T> filter) {
            return new ElementStream<>(streamSource.andThen(stream -> stream.filter(filter)));
        }

        /**
         * Returns a {@link Transformation} that applies the given {@code action} to each element.
         *
         * @param  action the consumer to apply to each element
         * @return        a new {@link Transformation}
         * @since         5.0.0
         */
        public Transformation forEach(Consumer<T> action) {
            return context -> {
                streamSource.apply(context)
                        .collect(Collectors.toList()) // create a temporary list to allow deletions, etc.
                        .forEach(action);
            };
        }

    }

}
