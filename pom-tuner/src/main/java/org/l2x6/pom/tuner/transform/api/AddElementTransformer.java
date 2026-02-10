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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import org.l2x6.pom.tuner.Comparators;
import org.l2x6.pom.tuner.PomTransformer.ContainerElement;
import org.l2x6.pom.tuner.PomTransformer.ProfileElement;
import org.l2x6.pom.tuner.PomTransformer.TextElement;
import org.l2x6.pom.tuner.PomTransformer.TransformationContext;

/**
 * A generic removed of {@code pom.xml} elements such as {@code <properties>}, their child properties,
 * {@code <modules>}, {@code <module>}, etc.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  5.0.0
 */
public class AddElementTransformer<P extends ContainerElement, T extends TextElement, THIS extends AddElementTransformer<P, T, THIS>>
        extends AbstractAddTransformer<P, T, Map.Entry<String, String>, THIS> {

    AddElementTransformer(
            Function<TransformationContext, ProfileElement> profileSelector,
            Function<ProfileElement, P> profileToParentElement,
            BiFunction<ContainerElement, Comparator<Map.Entry<String, String>>, T> createChild,
            Comparator<Map.Entry<String, String>> comparator,
            List<Consumer<T>> postprocessors) {
        super(profileSelector, profileToParentElement, createChild, comparator, postprocessors);
    }

    public AddElementTransformer(
            Function<ProfileElement, P> profileToParentElement,
            BiFunction<ContainerElement, Comparator<Map.Entry<String, String>>, T> createChild,
            Comparator<Map.Entry<String, String>> comparator) {
        this(
                selectProject(),
                profileToParentElement,
                createChild,
                comparator,
                Collections.<Consumer<T>> emptyList());
    }

    /**
     * @param  profileSelector a {@link Function} retrieving the profile under which the current operation should be applied
     * @return                 a copy of this {@link AddElementTransformer} instance with the
     *                         {@link #profileSelector} set to the given {@code profileSelector}
     * @since                  5.0.0
     * @see                    ProfileId
     */
    @SuppressWarnings("unchecked")
    public THIS profile(Function<TransformationContext, ProfileElement> profileSelector) {
        return (THIS) new AddElementTransformer<P, T, THIS>(
                profileSelector,
                profileToParentElement,
                createChild,
                comparator,
                postprocessors);
    }

    /**
     * @param  profileId the {@code id} of the {@code pom.xml} profile under which the new element should be applied; if the
     *                   profile does not exist an {@link IllegalStateException} is thrown from
     *                   {@link #perform(TransformationContext)}
     * @return           a copy of this {@link AddElementTransformer} instance with the
     *                   {@link #profileSelector} adjusted
     * @since            5.0.0
     */
    @SuppressWarnings("unchecked")
    public THIS profile(String profileId) {
        return (THIS) new AddElementTransformer<P, T, THIS>(
                selectProfile(profileId),
                profileToParentElement,
                createChild,
                comparator,
                postprocessors);
    }

    /**
     * @param  position a {@link Comparator} deciding where among its available siblings the new element should be added
     * @return          a copy of this {@link AddElementTransformer} instance with the
     *                  {@link #position} {@link Comparator} adjusted
     * @since           5.0.0
     */
    @SuppressWarnings("unchecked")
    public THIS at(Comparator<Map.Entry<String, String>> position) {
        return (THIS) new AddElementTransformer<P, T, THIS>(
                profileSelector,
                profileToParentElement,
                createChild,
                position,
                postprocessors);
    }

    /**
     * @param  elementName the name of the XML element after which the new element should be added; if there is no such
     *                     element, the new element is added at the last position
     * @return             a copy of this {@link AddElementTransformer} instance with the
     *                     {@link #position} {@link Comparator} adjusted
     * @since              5.0.0
     */
    public AddElementTransformer<P, T, THIS> afterElement(String elementName) {
        return new AddElementTransformer<>(
                profileSelector,
                profileToParentElement,
                createChild,
                Comparators.elementName(Comparators.after(elementName)),
                postprocessors);
    }

    /**
     * @param  elementName the name of the XML element before which the new element should be added; if there is no such
     *                     element, the new element is added at the last position
     * @return             a copy of this {@link AddElementTransformer} instance with the
     *                     {@link #position} {@link Comparator} adjusted
     * @since              5.0.0
     */
    public AddElementTransformer<P, T, THIS> beforeElement(String elementName) {
        return new AddElementTransformer<>(
                profileSelector,
                profileToParentElement,
                createChild,
                Comparators.elementName(Comparators.before(elementName)),
                postprocessors);
    }

    /**
     * @param  elementName the text content of the XML element after which the new element should be added; if there is no
     *                     such element, the new element is added at the last position
     * @return             a copy of this {@link AddElementTransformer} instance with the
     *                     {@link #position} {@link Comparator} adjusted
     * @since              5.0.0
     */
    public AddElementTransformer<P, T, THIS> afterTextContent(String textContent) {
        return new AddElementTransformer<>(
                profileSelector,
                profileToParentElement,
                createChild,
                Comparators.textContent(Comparators.after(textContent)),
                postprocessors);
    }

    /**
     * @param  elementName the text content of the XML element before which the new element should be added; if there is no
     *                     such element, the new element is added at the last position
     * @return             a copy of this {@link AddElementTransformer} instance with the
     *                     {@link #position} {@link Comparator} adjusted
     * @since              5.0.0
     */
    public AddElementTransformer<P, T, THIS> beforeTextContent(String textContent) {
        return new AddElementTransformer<>(
                profileSelector,
                profileToParentElement,
                createChild,
                Comparators.textContent(Comparators.before(textContent)),
                postprocessors);
    }

}
