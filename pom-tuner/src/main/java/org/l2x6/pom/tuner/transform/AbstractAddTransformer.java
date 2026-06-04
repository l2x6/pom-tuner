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
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import org.l2x6.pom.tuner.PomTransformer.ContainerElement;
import org.l2x6.pom.tuner.PomTransformer.ProfileElement;
import org.l2x6.pom.tuner.PomTransformer.TextElement;
import org.l2x6.pom.tuner.PomTransformer.Transformation;
import org.l2x6.pom.tuner.PomTransformer.TransformationContext;

/**
 * A generic adder of {@code pom.xml} elements.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  5.0.0
 */
abstract class AbstractAddTransformer<P extends ContainerElement, T extends TextElement, C, THIS extends AbstractAddTransformer<P, T, C, THIS>>
        implements Transformation {

    final Function<TransformationContext, ProfileElement> profileSelector;
    final Function<ProfileElement, P> profileToParentElement;
    final BiFunction<ContainerElement, Comparator<C>, T> createChild;
    final Comparator<C> comparator;
    final List<Consumer<T>> postprocessors;

    AbstractAddTransformer(
            Function<TransformationContext, ProfileElement> profileSelector,
            Function<ProfileElement, P> profileToParentElement,
            BiFunction<ContainerElement, Comparator<C>, T> createChild,
            Comparator<C> comparator,
            List<Consumer<T>> postprocessors) {
        this.profileSelector = profileSelector;
        this.profileToParentElement = profileToParentElement;
        this.createChild = createChild;
        this.comparator = comparator;
        this.postprocessors = postprocessors;
    }

    AbstractAddTransformer(
            Function<ProfileElement, P> profileToParentElement,
            BiFunction<ContainerElement, Comparator<C>, T> createChild,
            Comparator<C> comparator) {
        this(
                selectProject(),
                profileToParentElement,
                createChild,
                comparator,
                Collections.<Consumer<T>> emptyList());
    }

    /**
     * @param  profileSelector a {@link Function} retrieving the profile under which the current operation should be applied
     * @return                 a copy of this {@link AbstractAddTransformer} instance with the
     *                         {@link #profileSelector} set to the given {@code profileSelector}
     * @since                  5.0.0
     * @see                    ProfileId
     */
    public abstract THIS intoProfile(Function<TransformationContext, ProfileElement> profileSelector);

    /**
     * @param  profileId the {@code id} of the {@code pom.xml} profile under which the new element should be applied; if the
     *                   profile does not exist an {@link IllegalStateException} is thrown from
     *                   {@link #perform(TransformationContext)}
     * @return           a copy of this {@link AbstractAddTransformer} instance with the
     *                   {@link #profileSelector} adjusted
     * @since            5.0.0
     */
    public abstract THIS intoProfile(String profileId);

    /**
     * @param  position a {@link Comparator} deciding where among its available siblings the new element should be added
     * @return          a copy of this {@link AbstractAddTransformer} instance with the
     *                  {@link #position} {@link Comparator} adjusted
     * @since           5.0.0
     */
    public abstract THIS at(Comparator<C> position);

    /** {@inheritDoc} */
    @Override
    public void perform(TransformationContext context) {
        profileSelector
                .andThen(profileToParentElement)
                .andThen(parent -> createChild.apply(parent, comparator))
                .andThen(newNode -> {
                    postprocessors.forEach(postproc -> postproc.accept(newNode));
                    return null;
                })
                .apply(context);
    }

    static Function<TransformationContext, ProfileElement> selectProject() {
        return ctx -> ctx.getProfileParent(null)
                .orElseThrow(() -> new IllegalStateException(
                        "The root element <project> element not found in " + ctx.getPomXmlPath()));
    }

    static Function<TransformationContext, ProfileElement> selectProfile(String profileId) {
        return ctx -> ctx.getProfileParent(profileId)
                .orElseThrow(() -> new IllegalStateException(
                        "A profile with id '" + profileId + "' not found in " + ctx.getPomXmlPath()));
    }
}
