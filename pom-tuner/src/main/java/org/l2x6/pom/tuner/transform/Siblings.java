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

public interface Siblings {
    static Function<Node, List<Node>> none() {
        return node -> Collections.emptyList();
    }

    static Function<Node, List<RemovableNode>> previousOrNext(Predicate<Node> nodeSelector) {
        return node -> {
            final List<RemovableNode> prev = previous(nodeSelector).apply(node);
            final List<RemovableNode> result = new ArrayList<>(prev);
            result.addAll(next(nodeSelector).apply(node));
            return Collections.unmodifiableList(result);
        };
    }

    static Function<Node, List<RemovableNode>> previousCommentsOrWhitespace() {
        return previous(commentsOrWhitespace());
    }

    static Function<Node, List<RemovableNode>> previous(Predicate<Node> nodeSelector) {
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

    static Function<Node, List<RemovableNode>> next(Predicate<Node> nodeSelector) {
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

    static Predicate<Node> comments() {
        return n -> n.type() == NodeType.COMMENT;
    }

    static Predicate<Node> commentsOrWhitespace() {
        return comments().or(whitespace());
    }

    static Predicate<Node> whitespace() {
        return TransformationContext::isWhiteSpaceNode;
    }
}
