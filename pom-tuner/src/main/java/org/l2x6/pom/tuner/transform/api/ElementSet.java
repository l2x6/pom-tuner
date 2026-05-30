package org.l2x6.pom.tuner.transform.api;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.l2x6.pom.tuner.PomTransformer.ProfileElement;
import org.l2x6.pom.tuner.PomTransformer.TextElement;
import org.l2x6.pom.tuner.PomTransformer.Transformation;

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
    public static Function<ProfileElement, Stream<TextElement>> textGrandChildrenMapper(String name) {
        return profile -> profile.childElementsStream()
                .filter(ch -> name.equals(ch.getElementName()))
                .findFirst()
                .map(parent -> parent.childTextElementsStream())
                .orElse(Stream.empty());
    }

    protected final Predicate<String> profileSelector;
    protected final Function<ProfileElement, Stream<T>> getNodes;
    protected final Predicate<T> nodeSelector;

    public ElementSet(Function<ProfileElement, Stream<T>> getNodes, Predicate<T> nodeSelector) {
        this.profileSelector = ProfileId.main();
        this.getNodes = getNodes;
        this.nodeSelector = nodeSelector;
    }

    public ElementSet(Predicate<String> profileSelector, Function<ProfileElement, Stream<T>> getNodes,
            Predicate<T> nodeSelector) {
        this.profileSelector = profileSelector;
        this.getNodes = getNodes;
        this.nodeSelector = nodeSelector;
    }

    /**
     * Choose whether the matching elements should be selected from under the {@code <project>} and/or from under specific
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

    public Transformation modify(Consumer<T> element) {
        return context -> {
            context.getProfilesStream()
                    .filter(profile -> profileSelector.test(profile.getId()))
                    .flatMap(getNodes)
                    .filter(nodeSelector)
                    .collect(Collectors.toList()) // create a temporary list to allow deletions, etc.
                    .forEach(element);
        };
    }

    @SuppressWarnings("unchecked")
    protected THIS create(Predicate<String> profileSelector, Function<ProfileElement, Stream<T>> getNodes,
            Predicate<T> nodeSelector) {
        return (THIS) new ElementSet<>(profileSelector, getNodes, nodeSelector);
    }

}
