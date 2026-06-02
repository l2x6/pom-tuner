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

import eu.maveniverse.domtrip.Node;
import eu.maveniverse.domtrip.Node.NodeType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.l2x6.pom.tuner.PomTransformer.RemovableNode;
import org.l2x6.pom.tuner.PomTransformer.TransformationContext;

/**
 * A sector of sibling XML nodes of some specific XML node.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 5.0.0
 */
public interface Siblings extends Function<Node, List<RemovableNode>> {

    /**
     * Returns a sibling selector that selects both previous and next siblings matching the given {@code nodeSelector}.
     *
     * @param  nodeSelector a {@link Predicate} to match sibling nodes
     * @return              a {@link Function} selecting matching previous and next siblings
     * @since               5.0.0
     */
    static Siblings previousOrNext(Predicate<Node> nodeSelector) {
        return node -> {
            final List<RemovableNode> prev = previous(nodeSelector).apply(node);
            final List<RemovableNode> result = new ArrayList<>(prev);
            result.addAll(next(nodeSelector).apply(node));
            return Collections.unmodifiableList(result);
        };
    }

    /**
     * Returns a sibling selector that selects previous siblings that are comments or whitespace.
     *
     * @return a {@link Function} selecting previous comment and whitespace siblings
     * @since  5.0.0
     */
    static Siblings previousCommentsOrWhitespace() {
        return previous(commentsOrWhitespace());
    }

    /**
     * Returns a sibling selector that selects previous siblings matching the given {@code nodeSelector}.
     * The selection continues as long as consecutive previous siblings match.
     *
     * @param  nodeSelector a {@link Predicate} to match sibling nodes
     * @return              a {@link Function} selecting matching previous siblings
     * @since               5.0.0
     */
    static Siblings previous(Predicate<Node> nodeSelector) {
        return node -> {
            final List<RemovableNode> result = new ArrayList<>();
            RemovableNode prevSibling = RemovableNode.of(node);
            while ((prevSibling = prevSibling.previousSibling()) != null
                    && nodeSelector.test(prevSibling.node())) {
                result.add(prevSibling);
            }
            return Collections.unmodifiableList(result);
        };
    }

    /**
     * Returns a sibling selector that selects next siblings that are comments or whitespace.
     *
     * @return a {@link Function} selecting next comment and whitespace siblings
     * @since  5.0.0
     */
    static Siblings nextCommentsOrWhitespace() {
        return next(commentsOrWhitespace());
    }

    /**
     * Returns a sibling selector that selects next siblings matching the given {@code nodeSelector}.
     * The selection continues as long as consecutive next siblings match.
     *
     * @param  nodeSelector a {@link Predicate} to match sibling nodes
     * @return              a {@link Function} selecting matching next siblings
     * @since               5.0.0
     */
    static Siblings next(Predicate<Node> nodeSelector) {
        return node -> {
            final List<RemovableNode> result = new ArrayList<>();
            RemovableNode nextSibling = RemovableNode.of(node);
            while ((nextSibling = nextSibling.nextSibling()) != null
                    && nodeSelector.test(nextSibling.node())) {
                result.add(nextSibling);
            }
            return Collections.unmodifiableList(result);
        };
    }

    /**
     * Returns a {@link Predicate} matching comment nodes.
     *
     * @return a {@link Predicate} matching {@link NodeType#COMMENT} nodes
     * @since  5.0.0
     */
    static Predicate<Node> comments() {
        return n -> n.type() == NodeType.COMMENT;
    }

    /**
     * Returns a {@link Predicate} matching comment or whitespace nodes.
     *
     * @return a {@link Predicate} matching comment or whitespace nodes
     * @since  5.0.0
     */
    static Predicate<Node> commentsOrWhitespace() {
        return comments().or(whitespace());
    }

    /**
     * Returns a {@link Predicate} matching whitespace-only text nodes.
     *
     * @return a {@link Predicate} matching whitespace nodes
     * @since  5.0.0
     */
    static Predicate<Node> whitespace() {
        return TransformationContext::isWhiteSpaceNode;
    }
}
