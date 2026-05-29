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
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import org.l2x6.pom.tuner.Comparators;
import org.l2x6.pom.tuner.PomTransformer.ContainerElement;
import org.l2x6.pom.tuner.PomTransformer.GavtcsElement;
import org.l2x6.pom.tuner.PomTransformer.ProfileElement;
import org.l2x6.pom.tuner.PomTransformer.TransformationContext;
import org.l2x6.pom.tuner.model.Gavtcs;

/**
 * A generic adder of {@code pom.xml} GAVTCS elements such as {@code <dependency>} or managed dependency entries.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  5.0.0
 */
public class AddGavtcsTransformer<P extends ContainerElement, T extends GavtcsElement, THIS extends AddGavtcsTransformer<P, T, THIS>>
        extends AbstractAddTransformer<P, T, Gavtcs, THIS> {

    AddGavtcsTransformer(
            Function<TransformationContext, ProfileElement> profileSelector,
            Function<ProfileElement, P> profileToParentElement,
            BiFunction<ContainerElement, Comparator<Gavtcs>, T> createChild,
            Comparator<Gavtcs> comparator,
            List<Consumer<T>> postprocessors) {
        super(profileSelector, profileToParentElement, createChild, comparator, postprocessors);
    }

    public AddGavtcsTransformer(
            Function<ProfileElement, P> profileToParentElement,
            BiFunction<ContainerElement, Comparator<Gavtcs>, T> createChild,
            Comparator<Gavtcs> comparator) {
        this(
                AddElementTransformer.selectProject(),
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
    public THIS intoProfile(Function<TransformationContext, ProfileElement> profileSelector) {
        return (THIS) new AddGavtcsTransformer<>(
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
    public THIS intoProfile(String profileId) {
        return (THIS) new AddGavtcsTransformer<P, T, THIS>(
                AddElementTransformer.selectProfile(profileId),
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
    public THIS at(Comparator<Gavtcs> position) {
        return (THIS) new AddGavtcsTransformer<P, T, THIS>(
                profileSelector,
                profileToParentElement,
                createChild,
                position,
                postprocessors);
    }

    /**
     * @param  gavtcs the {@link Gavtcs} after which the new element should be added; if there is no such
     *                element, the new element is added at the last position
     * @return        a copy of this {@link AddGavtcsTransformer} instance with the
     *                {@link #comparator} adjusted
     * @since         5.0.0
     */
    public AddGavtcsTransformer<P, T, THIS> after(Gavtcs gavtcs) {
        return new AddGavtcsTransformer<P, T, THIS>(
                profileSelector,
                profileToParentElement,
                createChild,
                Comparators.after(gavtcs),
                postprocessors);
    }

    /**
     * @param  gavtcs the {@link Gavtcs} before which the new element should be added; if there is no such
     *                element, the new element is added at the last position
     * @return        a copy of this {@link AddGavtcsTransformer} instance with the
     *                {@link #comparator} adjusted
     * @since         5.0.0
     */
    public AddGavtcsTransformer<P, T, THIS> before(Gavtcs gavtcs) {
        return new AddGavtcsTransformer<P, T, THIS>(
                profileSelector,
                profileToParentElement,
                createChild,
                Comparators.before(gavtcs),
                postprocessors);
    }

}
