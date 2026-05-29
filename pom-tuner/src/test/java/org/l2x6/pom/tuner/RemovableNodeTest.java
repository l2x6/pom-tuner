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
package org.l2x6.pom.tuner;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.l2x6.pom.tuner.PomTransformer.RemovableNode;
import org.l2x6.pom.tuner.transform.api.Siblings;

public class RemovableNodeTest {

    @Test
    void removableWs() {
        String xml = "<root>\n"
                + "    <child1/>\n"
                + "    <child2/>\n"
                + "</root>\n";

        String expected = "<root>\n"
                + "    <child1/><child2/>\n"
                + "</root>\n";
        {
            Document doc = Document.of(xml);
            Element ch2 = doc.root().query().withName("child2").all().findFirst().orElse(null);
            List<RemovableNode> nodes = Siblings.previous(Siblings.commentsOrWhitespace()).apply(ch2);
            Assertions.assertThat(nodes).hasSize(1);
            nodes.forEach(RemovableNode::remove);

            Assertions.assertThat(doc.toXml()).isEqualTo(expected);
        }
        {
            Document doc = Document.of(xml);
            Element ch1 = doc.root().query().withName("child1").all().findFirst().orElse(null);
            List<RemovableNode> nodes = Siblings.next(Siblings.commentsOrWhitespace()).apply(ch1);
            Assertions.assertThat(nodes).hasSize(1);
            nodes.forEach(RemovableNode::remove);

            Assertions.assertThat(doc.toXml()).isEqualTo(expected);
        }

        {
            Document doc = Document.of("<root>\n"
                    + "    <child0/>\n"
                    + "    <child1/>\n"
                    + "    <child2/>\n"
                    + "</root>\n");
            Element ch2 = doc.root().query().withName("child2").all().findFirst().orElse(null);
            List<RemovableNode> nodes = Siblings.previous(n -> true).apply(ch2);
            Assertions.assertThat(nodes).hasSize(5);
            nodes.forEach(RemovableNode::remove);

            Assertions.assertThat(doc.toXml()).isEqualTo("<root><child2/>\n"
                    + "</root>\n");
        }
    }

    @Test
    void removableCommentAndWsPrevious() {

        String expected = "<root>\n"
                + "    <child1/><child2/>\n"
                + "</root>\n";
        {
            String xml = "<root>\n"
                    + "    <child1/><!-- comment -->\n"
                    + "    <child2/>\n"
                    + "</root>\n";
            Document doc = Document.of(xml);
            Element ch2 = doc.root().query().withName("child2").all().findFirst().orElse(null);
            List<RemovableNode> nodes = Siblings.previous(Siblings.commentsOrWhitespace()).apply(ch2);
            Assertions.assertThat(nodes).hasSize(2);
            nodes.forEach(RemovableNode::remove);

            Assertions.assertThat(doc.toXml()).isEqualTo(expected);
        }
        {
            String xml = "<root>\n"
                    + "    <child1/>\n"
                    + "    <!-- comment --><child2/>\n"
                    + "</root>\n";
            Document doc = Document.of(xml);
            Element ch2 = doc.root().query().withName("child2").all().findFirst().orElse(null);
            List<RemovableNode> nodes = Siblings.previous(Siblings.commentsOrWhitespace()).apply(ch2);
            Assertions.assertThat(nodes).hasSize(2);
            nodes.forEach(RemovableNode::remove);

            Assertions.assertThat(doc.toXml()).isEqualTo(expected);
        }
        {
            String xml = "<root>\n"
                    + "    <child1/>\n"
                    + "<!-- comment -->    <child2/>\n"
                    + "</root>\n";
            Document doc = Document.of(xml);
            Element ch2 = doc.root().query().withName("child2").all().findFirst().orElse(null);
            List<RemovableNode> nodes = Siblings.previous(Siblings.commentsOrWhitespace()).apply(ch2);
            Assertions.assertThat(nodes).hasSize(3);
            nodes.forEach(RemovableNode::remove);

            Assertions.assertThat(doc.toXml()).isEqualTo(expected);
        }
        {
            String xml = "<root>\n"
                    + "    <child1/>\n"
                    + "    <!-- comment -->\n"
                    + "    <child2/>\n"
                    + "</root>\n";
            Document doc = Document.of(xml);
            Element ch2 = doc.root().query().withName("child2").all().findFirst().orElse(null);
            List<RemovableNode> nodes = Siblings.previous(Siblings.commentsOrWhitespace()).apply(ch2);
            Assertions.assertThat(nodes).hasSize(3);
            nodes.forEach(RemovableNode::remove);

            Assertions.assertThat(doc.toXml()).isEqualTo(expected);
        }

    }

    @Test
    void removableCommentAndWsNext() {

        String expected = "<root>\n"
                + "    <child1/><child2/>\n"
                + "</root>\n";
        {
            String xml = "<root>\n"
                    + "    <child1/><!-- comment -->\n"
                    + "    <child2/>\n"
                    + "</root>\n";
            Document doc = Document.of(xml);
            Element ch1 = doc.root().query().withName("child1").all().findFirst().orElse(null);
            List<RemovableNode> nodes = Siblings.next(Siblings.commentsOrWhitespace()).apply(ch1);
            Assertions.assertThat(nodes).hasSize(2);
            nodes.forEach(RemovableNode::remove);

            Assertions.assertThat(doc.toXml()).isEqualTo(expected);
        }
        {
            String xml = "<root>\n"
                    + "    <child1/>\n"
                    + "    <!-- comment --><child2/>\n"
                    + "</root>\n";
            Document doc = Document.of(xml);
            Element ch1 = doc.root().query().withName("child1").all().findFirst().orElse(null);
            List<RemovableNode> nodes = Siblings.next(Siblings.commentsOrWhitespace()).apply(ch1);
            Assertions.assertThat(nodes).hasSize(2);
            nodes.forEach(RemovableNode::remove);

            Assertions.assertThat(doc.toXml()).isEqualTo(expected);
        }
        {
            String xml = "<root>\n"
                    + "    <child1/>\n"
                    + "<!-- comment -->    <child2/>\n"
                    + "</root>\n";
            Document doc = Document.of(xml);
            Element ch1 = doc.root().query().withName("child1").all().findFirst().orElse(null);
            List<RemovableNode> nodes = Siblings.next(Siblings.commentsOrWhitespace()).apply(ch1);
            Assertions.assertThat(nodes).hasSize(3);
            nodes.forEach(RemovableNode::remove);

            Assertions.assertThat(doc.toXml()).isEqualTo(expected);
        }

    }
}
