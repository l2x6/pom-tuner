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

import eu.maveniverse.domtrip.Comment;
import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;
import eu.maveniverse.domtrip.Node.NodeType;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.l2x6.pom.tuner.PomTransformer.ProfileElement;
import org.l2x6.pom.tuner.PomTransformer.Transformation;

/**
 * A set of {@code pom.xml} comments selected for modification.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 *
 */
public class CommentSet {

    /**
     * Maps a {@link ProfileElement} to a stream of text elements which are grand children of the given
     * {@link ProfileElement}.
     * Useful for getting children of {@code <properties>}, {@code <modules>}, etc.
     *
     * @param  parentName the name of the text element parent, such as {@code properties} or {@code modules}
     * @return            a {@link Function} mapping a {@link ProfileElement} to a stream of text elements whose parent is a
     *                    direct child of the given {@link ProfileElement}
     *
     * @since             5.0.0
     */
    public static Function<ProfileElement, Stream<Comment>> commentGrandChildrenMapper(String parentName) {
        return profile -> profile.childElementsStream()
                .filter(ch -> parentName.equals(ch.getElementName()))
                .findFirst()
                .map(parent -> parent.getNode().children()
                        .filter(node -> node.type() == NodeType.COMMENT)
                        .map(node -> (Comment) node))
                .orElse(Stream.empty());
    }

    private final Predicate<String> profileSelector;
    private final Function<ProfileElement, Stream<Comment>> getComments;
    private final Predicate<ParsedComment> nodeSelector;

    CommentSet(Predicate<String> profileSelector, Function<ProfileElement, Stream<Comment>> getComments,
            Predicate<ParsedComment> nodeSelector) {
        this.profileSelector = profileSelector;
        this.getComments = getComments;
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
     * @param  profileSelector the profile selector to set on the resulting {@link CommentSet}
     * @return                 a copy of this {@link CommentSet} instance with the
     *                         {@link #profileSelector} set to the given {@code profileSelector}
     * @since                  5.0.0
     * @see                    ProfileId
     */
    public CommentSet from(Predicate<String> profileSelector) {
        return new CommentSet(profileSelector, getComments, nodeSelector);
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
     * @param  profileIds the profile {@code id}s to select on the resulting {@link CommentSet} in addition
     *                    to the {@link ProfileId#main()}
     * @return            a copy of this {@link CommentSet} instance with the
     *                    {@link #profileSelector} adjusted
     * @since             5.0.0
     * @see               ProfileId#ids(String...)
     */
    public CommentSet from(String... profileIds) {
        return new CommentSet(ProfileId.ids(profileIds), getComments, nodeSelector);
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
     * @param  profileIds the profile {@code id}s to select on the resulting {@link CommentSet} (but not the
     *                    {@link ProfileId#main()}
     * @return            a copy of this {@link CommentSet} instance with the
     *                    {@link #profileSelector} adjusted
     * @since             5.0.0
     * @see               ProfileId#idsOnly(String...)
     */
    public CommentSet fromProfilesOnly(String... profileIds) {
        return new CommentSet(ProfileId.idsOnly(profileIds), getComments, nodeSelector);
    }

    public Transformation uncomment() {
        return forEach(parsedComment -> {
            final Element replacement = parsedComment.getParsedContent().root();
            replacement.precedingWhitespace(parsedComment.getSource().precedingWhitespace());
            parsedComment.getSource().parent().replaceChild(parsedComment.getSource(), replacement);
        });
    }

    /**
     * Returns a {@link Transformation} that applies the given {@code action} to each element of this CommentSet.
     *
     * @param  action the consumer to apply to each element
     * @return         a new {@link Transformation}
     * @since          5.0.0
     */
    public Transformation forEach(Consumer<ParsedComment> action) {
        return context -> {
            context.getProfilesStream()
                    .filter(profile -> profileSelector.test(profile.getId()))
                    .flatMap(getComments)
                    .map(ParsedComment::of)
                    .filter(nodeSelector)
                    .collect(Collectors.toList()) // create a temporary list to allow deletions, etc.
                    .stream()
                    .forEach(action);
        };
    }

    /**
     * A comment containing commented out XML that can be accessed as parsed DOM.
     *
     * @since           5.0.0
     */
    public static class ParsedComment {
        private final Comment source;
        private final Document parsedContent;

        /**
         * @param comment the Comment to create this {@link ParsedComment} from
         * @return a new {@link ParsedComment}
         */
        static ParsedComment of(Comment comment) {
            return new ParsedComment(comment, Document.of(comment.content()));
        }

        private ParsedComment(Comment source, Document parsedContent) {
            this.source = source;
            this.parsedContent = parsedContent;
        }

        /**
         * @return the source {@link Comment} node
         */
        public Comment getSource() {
            return source;
        }

        /**
         * @return the XML that was enclosed in the {@link #getSource() source} comment.
         */
        public Document getParsedContent() {
            return parsedContent;
        }

    }

}
