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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.xpath.XPath;

import org.l2x6.pom.tuner.model.Ga;
import org.l2x6.pom.tuner.model.Gavtcs;
import org.l2x6.pom.tuner.transform.dependencies;
import org.l2x6.pom.tuner.transform.dependencyManagement;
import org.l2x6.pom.tuner.transform.modules;
import org.l2x6.pom.tuner.transform.properties;
import org.l2x6.pom.tuner.transform.api.Siblings;
import org.w3c.dom.DocumentFragment;

import eu.maveniverse.domtrip.Comment;
import eu.maveniverse.domtrip.ContainerNode;
import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;
import eu.maveniverse.domtrip.Node;
import eu.maveniverse.domtrip.Node.NodeType;
import eu.maveniverse.domtrip.Text;

/**
 * A utility to programmatically modify a {@code pom.xml} file while keeping the original comments and formatting also
 * on places where common {@code javax.xml.transform.Transformer} or {@code javax.xml.parsers.DocumentBuilder} based
 * solutions tend to fail, such as
 * <ul>
 * <li>Order of XML declaration attributes</li>
 * <li>Whitespace after the XML declaration</li>
 * <li>Line breaks between element attributes</li>
 * <li>File final whitespace</li>
 * </ul>
 * Typical usage:
 *
 * <pre>
 * <code>
 * import org.l2x6.pom.tuner.transform.*;
 *
 * PomTransformer.of(
 *     // You can use ready to use Transformers from org.l2x6.pom.tuner.transform package
 *     properties.set("slf4j.version", "2.17.0"),
 *     dependencies.add(Gavtcs.of("org.foo:bar:1.2.3")),
 *     // Or you can use an ad-hoc Transformer
 *     (TransformationContext context) -> context.getProject().addOrSetChildTextElement("version", "0.2-SNAPSHOT")
 * )
   .transform(Path.of("pom.xml"));
 * </code>
 * </pre>
 * <p>
 * Use {@link #builder()} if you need to adjust the {@link #charset} or {@link #simpleElementWhitespace}.
 */
public class PomTransformer {

    static final Pattern EOL_PATTERN = Pattern.compile("\r?\n");
    static final String WS_REGEX = "[ \t\n\r]+";
    static final Pattern WS_PATTERN = Pattern.compile(WS_REGEX);
    static final Pattern INDENT_PATTERN = Pattern.compile("(\r?\n)([ \t]+)");
    static final String EMPTY_LINE_REGEX = "[ \t]*\r?\n\r?\n[ \t\r\n]*";
    static final Pattern EMPTY_LINE_PATTERN = Pattern.compile(EMPTY_LINE_REGEX);
    static final Pattern SIMPLE_ELEM_WS_PATTERN = Pattern.compile("<([^ \t\n\r]+)([ \t\n\r]*)/>");

    private final Charset charset;
    private final Collection<? extends Transformer> transformers;

    /**
     * @param  transformers
     * @return              a new {@link PomTransformer} with {@link #charset} {@link StandardCharsets#UTF_8}
     *
     * @since               5.0.0
     */
    public static PomTransformer of(Collection<? extends Transformer> transformers) {
        return builder().transformers(transformers).build();
    }

    /**
     * @param  transformers
     * @return              a new {@link PomTransformer} with {@link #charset} {@link StandardCharsets#UTF_8}
     *
     * @since               5.0.0
     */
    @SafeVarargs
    public static <T extends Transformer> PomTransformer of(T... transformers) {
        return builder().transformers(transformers).build();
    }

    /**
     * @return a new {@link Builder}
     *
     * @since  5.0.0
     */
    public static Builder builder() {
        return new Builder();
    }

    private PomTransformer(Charset charset, Collection<? extends Transformer> transformers) {
        super();
        this.charset = charset;
        this.transformers = transformers;
    }

    /**
     * Loads the {@code pom.xml} document from the given {@link #path}, applies the {@link #transformers} and stores the
     * document
     * back to the given {@link #path}.
     *
     * @param transformations the {@link Transformation}s to apply
     * @since                 5.0.0
     */
    public void transform(Path file) {
        LazyWriter lazyWriter = new LazyWriter(file, charset);
        transform(transformers, file, lazyWriter::read, lazyWriter::write);
    }

    static void transform(
            Collection<? extends Transformer> edits,
            Path path,
            Supplier<String> source,
            Consumer<String> outConsumer) {
        String src = source.get();
        //final String eol = detectEol(src);

        final Document document = Document.of(src);
        final TransformationContext context = new TransformationContext(path, document, detectIndentation(document));
        for (Transformer edit : edits) {
            edit.perform(context);
        }
        String result = document.toXml();
        //result = EOL_PATTERN.matcher(result).replaceAll(eol);
        outConsumer.accept(result);
    }

    static String detectIndentation(Document document) {
        return document.root().children().findFirst().map(firstElem -> {
            String ws = firstElem.precedingWhitespace();
            {
                final Matcher matcher = INDENT_PATTERN.matcher(ws);
                if (matcher.find()) {
                    return matcher.group(2);
                }
            }
            Node current = firstElem;
            while ((current = DomTripUtils.previousSibling(current)) != null) {
                if (current.type() == NodeType.TEXT) {
                    final Matcher matcher = INDENT_PATTERN.matcher(((Text) current).content());
                    if (matcher.find()) {
                        return matcher.group(2);
                    }
                }
            }
            return "    ";
        }).orElse("    ");
    }

    static String detectEol(String src) {
        return src.indexOf('\r') >= 0 ? "\r\n" : "\n";
    }

    /**
     * @param  src the input document
     * @return     the index after the &gt; character of the closing root element tag or -1 if there is no closing tag
     */
    static int endOfRootElement(String src) {
        if (src == null || src.isEmpty()) {
            return -1;
        }
        int lastGtPos = src.length() - 1;
        while (lastGtPos >= 0) {
            lastGtPos = src.lastIndexOf('>', lastGtPos);
            if (lastGtPos < 0) {
                return -1;
            } else if (lastGtPos >= 2
                    && src.charAt(lastGtPos - 1) == '-'
                    && src.charAt(lastGtPos - 2) == '-') {
                /* a trailing comment */
                lastGtPos -= 3;
                continue;
            } else {
                return lastGtPos + 1;
            }
        }
        return -1;
    }

    public static class Builder {
        private Collection<Transformer> transformers = new ArrayList<>();
        private Charset charset = StandardCharsets.UTF_8;

        public PomTransformer build() {
            return new PomTransformer(charset, transformers);
        }

        public <T extends Transformer> Builder charset(Charset charset) {
            this.charset = charset;
            return this;
        }

        public <T extends Transformer> Builder transformers(Collection<T> transformers) {
            this.transformers.addAll(transformers);
            return this;
        }

        public <T extends Transformer> Builder transformers(T... transformers) {
            for (T t : transformers) {
                this.transformers.add(t);
            }
            return this;
        }

        public void transform(Path pomXml) {
            new PomTransformer(charset, transformers).transform(pomXml);
        }
    }

    /**
     * A {@link Gavtcs} storing the associated {@link ContainerElement} from the {@code pom.xml} file.
     * This is useful for DOM modifications.
     */
    public static class NodeGavtcs extends Gavtcs {
        private final ContainerElement node;

        NodeGavtcs(String groupId, String artifactId, String version, String type, String classifier, String scope,
                Collection<Ga> exclusions, ContainerElement node) {
            super(groupId, artifactId, version, type, classifier, scope, exclusions);
            this.node = node;
        }

        /**
         * @return the associated {@link ContainerElement}.
         */
        public ContainerElement getNode() {
            return node;
        }
    }

    /**
     * An XML element having a single text child, such as {@code <version>1.2.3</version>}.
     */
    public static class TextElement implements Map.Entry<String, String> {
        protected final TransformationContext context;
        protected final Element node;
        protected final int indentLevel;

        static TextElement dummy(TransformationContext context, String elementName, String textContent) {
            Element n = Element.text(elementName, textContent);
            return new TextElement(context, n, 0);
        }

        public TextElement(TransformationContext context, Element node, int indentLevel) {
            this.context = context;
            this.node = node;
            this.indentLevel = indentLevel;
        }

        /**
         * @return a {@link Comment} preceding this {@link ContainerElement} or {@code null} if no comment precedes this
         *         {@link ContainerElement}
         */
        public Comment previousSiblingCommentNode() {
            final ContainerNode parent = node.parent();
            int i = DomTripUtils.indexOf(node);
            if (i < 0) {
                throw new IllegalStateException("Could not find " + node + " under parent " + parent);
            }
            i--;
            while (i >= 0) {
                Node ch = parent.getNode(i);
                switch (ch.type()) {
                case COMMENT:
                    return (Comment) ch;
                case TEXT:
                    break;
                default:
                    return null;
                }
                i--;
            }
            return null;
        }

        /**
         * @return a DOM {@link Node} to use as a second argument for {@link Node#insertBefore(Node, Node)} when
         *         inserting before {@link #node}; this can be either {@link #node} itself or a comment or an
         *         indentation whitespace preceding {@link #node}
         */
        public Node previousSiblingInsertionRefNode() {
            Node currentNode = this.node;
            while (true) {
                Node next = DomTripUtils.previousSibling(currentNode);
                if (next == null) {
                    return currentNode;
                }
                switch (next.type()) {
                case COMMENT:
                    final Node previousNode = DomTripUtils.previousSibling(next);
                    if (previousNode != null && previousNode.type() == NodeType.ELEMENT) {
                        /*
                         * A comment following an element with no whitespace in between: such comment belongs to the
                         * previous element
                         */
                        return currentNode;
                    }
                    break;
                case TEXT:
                    if (EMPTY_LINE_PATTERN.matcher(((Text) next).content()).matches()) {
                        return next;
                    } else {
                        break;
                    }
                default:
                    return currentNode;
                }
                currentNode = next;
            }
        }

        /**
         * Remove this {@link TextElement} together with its siblings selected by the given {@code siblingsSelector}
         *
         * @param siblingsSelector selects siblings to remove together with this {@link TextElement}, such as indentation whitespace and/or adjacent comments. See {@link Siblings}
         */
        public void remove(Function<Node, List<RemovableNode>> siblingsSelector) {
            ContainerNode parent = node.parent();
            if (parent != null) {
                final List<RemovableNode> siblings = siblingsSelector.apply(node);
                if (siblings != null && !siblings.isEmpty()) {
                    siblings.forEach(RemovableNode::remove);
                }
                parent.removeNode(node);
            }
        }

        /**
         * Add a properly indented comment before {@link #node}.
         * Note that this method does not add any whitespace around the specified comment text.
         * So of you want to add {@code <!-- my comment -->}, you need to call
         * {@code prependComment(" my comment ")}.
         *
         * @param  comment the text of the comment
         * @return         the newly created {@link Comment} node
         */
        public Comment prependComment(String comment) {
            final Node refNode = previousSiblingInsertionRefNode();
            Comment result = Comment.of(comment);
            int i = DomTripUtils.indexOf(refNode);
            node.parent().insertNode(i, context.indentNode(indentLevel));
            node.parent().insertNode(i, result);
            return result;
        }

        /**
         * Add a properly indented comment before {@link #node} and return it if a comment with the given
         * {@code comment} text does not precede {@link #node}; otherwise return the preceding {@link Comment} node.
         *
         * Note that this method does not add any whitespace around the specified comment text.
         * So of you want to add {@code <!-- my comment -->}, you need to call this as
         * {@code prependComment(" my comment ")}.
         *
         * @param  comment the text of the comment
         * @return         an existing or the newly created {@link Comment} node
         */
        public Comment prependCommentIfNeeded(String comment) {
            Comment precedingComment = previousSiblingCommentNode();
            if (precedingComment == null || !comment.equals(precedingComment.content())) {
                return prependComment(comment);
            }
            return precedingComment;
        }

        /**
         * @return a comment node following after {@link #node} (ignoring any whitespace in between) or {@code null} no
         *         comment follows after {@link #node}
         */
        public Comment nextSiblingCommentNode() {
            Node currentNode = this.node;
            while (true) {
                Node next = DomTripUtils.nextSibling(currentNode);
                if (next == null) {
                    return null;
                }
                switch (next.type()) {
                case COMMENT:
                    return (Comment) next;
                case TEXT:
                    break;
                default:
                    return null;
                }
                currentNode = next;
            }
        }

        /**
         * @return the associated DOM {@link Node}
         */
        public Element getNode() {
            return node;
        }

        /**
         * @return the associated DOM {@link Node} together with the preceding whitespace and comments selected by the
         *         given
         *         {@link Predicate}
         */
        public List<Node> getNodes(Predicate<Node> precedingInclude) {
            final List<Node> result = new ArrayList<>();
            Node prevSibling = node;
            while ((prevSibling = DomTripUtils.previousSibling(prevSibling)) != null
                    && precedingInclude.test(prevSibling)) {
                result.add(prevSibling);
            }
            result.add(node);
            return result;
        }

        /**
         * @return the name of the current XML node without the namespace or prefix
         *
         * @since  5.0.0
         */
        public String getElementName() {
            return node.name();
        }

        /**
         * @return the text content of the current XML text element
         * @since  5.0.0
         */
        public String getTextContent() {
            return node.textContent();
        }

        /**
         * Sets the text content of the current XML node.
         *
         * @param text the text content to set
         * @since      5.0.0
         */
        public void setTextContent(String text) {
            node.textContent(text);
        }

        @Override
        public String getKey() {
            return getElementName();
        }

        @Override
        public String getValue() {
            return getTextContent();
        }

        @Override
        public String setValue(String value) {
            String oldValue = getTextContent();
            setTextContent(value);
            return oldValue;
        }

    }

    /**
     * An XML element in a {@code pom.xml} file that possibly has child {@link ContainerElement}s.
     */
    public static class ContainerElement extends TextElement {

        public ContainerElement(TransformationContext context, Element node, int indentLevel) {
            super(context, node, indentLevel);
        }

        /**
         * @return an {@link Iterable} containing child elements of this {@link ContainerElement}
         */
        public List<ContainerElement> childElements() {
            return childElementsStream().collect(Collectors.toList());
        }

        /**
         * @return a {@link Stream} containing child elements of this {@link ContainerElement}
         */
        public Stream<ContainerElement> childElementsStream() {
            return node.children()
                    .map(n -> new ContainerElement(context, n, indentLevel + 1));
        }

        /**
         * @return an {@link Iterable} containing text child elements of this {@link ContainerElement}
         */
        public List<TextElement> childTextElements() {
            return childTextElementsStream().collect(Collectors.toList());
        }

        /**
         * @return a {@link Stream} containing child text elements of this {@link ContainerElement}
         */
        public Stream<TextElement> childTextElementsStream() {
            return node.children()
                    .map(n -> new TextElement(context, n, indentLevel + 1));
        }

        /**
         * @return {@code true} if this {@link ContainerElement} has child nodes of type {@link Node#ELEMENT_NODE}; otherwise
         *         {@code false}
         */
        public boolean hasChildElements() {
            if (node.nodeCount() == 0) {
                return false;
            }
            for (int i = 0; i < node.nodeCount(); i++) {
                final Node child = node.getNode(i);
                if (child.type() == NodeType.ELEMENT) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Ensure that the closing tag of this element is properly indented
         */
        public void ensureClosingTagIndented() {
            if (node.innerPrecedingWhitespace().isEmpty()) {
                node.innerPrecedingWhitespace(context.indentNode(indentLevel).content());
            }
        }

        /**
         * An equivalent of {@code addChildContainerElement(elementName, null, false, false)}
         *
         * @param  elementName the name of the DOM {@link Element} to add
         * @return             a new {@link ContainerElement}
         */
        public ContainerElement addChildContainerElement(String elementName) {
            return addChildContainerElement(elementName, null, false, false);
        }

        /**
         * Add a child {@link ContainerElement} and return it.
         *
         * @param  elementName     the name of the {@link Element} to add
         * @param  refNode         a node to use as the second argument of {@link Node#insertBefore(Node, Node)}
         * @param  emptyLineBefore if {@code true} and empty line will be added before the newly created {@link Element}
         * @param  emptyLineAfter  if {@code true} and empty line will be added after the newly created {@link Element}
         * @return                 the newly created {@link ContainerElement}
         */
        public ContainerElement addChildContainerElement(
                String elementName,
                Node refNode,
                boolean emptyLineBefore,
                boolean emptyLineAfter) {

            if (refNode == null) {
                ensureClosingTagIndented();
            }

            if (emptyLineBefore) {
                /*
                 * Add an empty line between the new node and previousProjectChildEntry
                 */
                DomTripUtils.insertBefore(node, context.newLine(), refNode);
            }

            String indent = context.indentNode(indentLevel + 1).content();
            final Element result = (Element) Element.of(elementName)
                    .innerPrecedingWhitespace(indent)
                    .precedingWhitespace(indent);
            DomTripUtils.insertBefore(node, result, refNode);

            if (emptyLineAfter) {
                /*
                 * Add an empty line between the new node and projectChild
                 * unless there is one already
                 */
                if (refNode != null && refNode.type() == NodeType.ELEMENT) {
                    if (!TransformationContext.hasEmptyLineBefore(refNode)) {
                        refNode.precedingWhitespace("\n" + context.indentNode(indentLevel + 1).content());
                    }
                } else if (refNode == null || !TransformationContext.isEmptyLineNode(refNode)) {
                    DomTripUtils.insertBefore(node, context.newLine(), refNode);
                }
            }
            return new ContainerElement(context, result, indentLevel + 1);
        }

        /**
         * @param  elementName the name of an element to search for
         * @return             an Optional containing the first child with the given {@code elementName} or an empty
         *                     {@link Optional} if no such child exists
         */
        public Optional<ContainerElement> getChildContainerElement(String elementName) {
            for (ContainerElement child : childElements()) {
                if (child.node.name().equals(elementName)) {
                    /* No need to insert, return existing */
                    return Optional.of(child);
                }
            }
            return Optional.empty();
        }

        /**
         * Find a child container element under the given path.
         * E.g. {@code project.getChildContainerElement("dependencyManagement", "dependencies")} would return the
         * dependencies node under dependencyManagement
         *
         * @param  elementName       the name of an element to search for
         * @param  otherElementNames optional array of further element names to search for
         * @return                   an Optional containing the first child with the given {@code elementName} or an empty
         *                           {@link Optional} if no such child exists
         */
        public Optional<ContainerElement> getChildContainerElement(String elementName, String... otherElementNames) {
            Optional<ContainerElement> result = getChildContainerElement(elementName);
            if (!result.isPresent()) {
                return Optional.empty();
            }
            for (String otherName : otherElementNames) {
                result = result.get().getChildContainerElement(otherName);
                if (!result.isPresent()) {
                    return Optional.empty();
                }
            }
            return result;
        }

        /**
         * Creates the given {@code elementName} under the given {@code parent} unless it exists already.
         *
         * @return an existing or newly created {@link Element} with the given {@code elementName}
         */
        public ContainerElement getOrAddChildContainerElement(String elementName) {
            for (ContainerElement child : childElements()) {
                if (child.node.name().equals(elementName)) {
                    /* No need to insert, return existing */
                    return child;
                }
            }

            if (indentLevel == 0) {
                return context.getOrAddContainerElement(elementName);
            }

            final String indent = context.indentNode(indentLevel + 1).content();
            final Element result = (Element) Element.of(elementName)
                    .innerPrecedingWhitespace(indent)
                    .precedingWhitespace(indent);
            node.addNode(result);

            return new ContainerElement(
                    context,
                    result,
                    indentLevel + 1);
        }

        /**
         * An equivalent of {@code addChildTextElement(elementName, text, getOrAddLastIndent())}
         *
         * @param elementName the name of the {@link Element} to add
         * @param text        the text content of the newly added {@link Element}
         */
        public TextElement addChildTextElement(String elementName, final String text) {
            ensureClosingTagIndented();
            return addChildTextElement(elementName, text, null);
        }

        /**
         * Add a new {@link Element} under {@link #node} and before {@code refNode} with the given {@code elementName}
         * and {@code text} content.
         *
         * @param elementName the name of the {@link Element} to add
         * @param text        the text content of the newly added {@link Element}
         * @param refNode     a {@link Node} before which the new {@link Element} should be added
         */
        public TextElement addChildTextElement(String elementName, final String text, Node refNode) {
            if (text != null) {
                DomTripUtils.insertBefore(node, context.indentNode(indentLevel + 1), refNode);
                final Element result = Element.of(elementName);
                result.textContent(text);
                DomTripUtils.insertBefore(node, result, refNode);
                return new TextElement(context, result, indentLevel + 1);
            }
            return null;
        }

        public TextElement addChildTextElementIfNeeded(String nodeName, String nodeValue,
                Comparator<Map.Entry<String, String>> comparator) {
            Node refNode = null;
            if (comparator == null) {
                ensureClosingTagIndented();
            } else {
                TextElement newEntry = TextElement.dummy(context, nodeName, nodeValue);
                for (TextElement child : childTextElements()) {
                    final Element node = child.getNode();
                    int comparison = comparator.compare(newEntry, child);
                    if (comparison == 0) {
                        /* the given child is available, no need to add it */
                        if (!Objects.equals(node.textContent(), nodeValue)) {
                            node.textContent(nodeValue);
                        }
                        return child;
                    }
                    if (refNode == null && comparison < 0) {
                        refNode = child.previousSiblingInsertionRefNode();
                    }
                }
                if (refNode == null) {
                    ensureClosingTagIndented();
                }
            }
            return addChildTextElement(nodeName, nodeValue, refNode);
        }

        /**
         * If {@code textContent} is not {@code null}, find an {@link Element} under {@link #node} having the given
         * {@code elementName} or create a new one and set the given {@code value} as its text content;
         * otherwise remove the element if it exists.
         *
         * @param elementName the name of the {@link Element} to add or set, must not be {@code null}
         * @param textContent the text content to set or add {@link Element}, can be {@code null}
         */
        public void addOrSetChildTextElement(String elementName, String textContent) {
            Objects.requireNonNull(elementName, elementName + " must not be null");

            Optional<ContainerElement> existingChild = getChildContainerElement(elementName);
            if (!existingChild.isPresent() && textContent == null) {
                /* nothing to do */
            } else if (!existingChild.isPresent()) {
                ensureClosingTagIndented();
                addChildTextElement(elementName, textContent, null);
            } else if (textContent == null) {
                existingChild.get().remove(Siblings.previous(Siblings.commentsOrWhitespace()));
            } else {
                existingChild.get().getNode().textContent(textContent);
            }
        }

        /**
         * An equivalent of {@code addFragment(fragment, getOrAddLastIndent())}.
         *
         * @param fragment the {@link DocumentFragment} to add
         */
        public void addFragment(List<Node> fragment) {
            ensureClosingTagIndented();
            addFragment(fragment, null);
        }

        /**
         * @param fragment the {@link DocumentFragment} to add
         * @param refNode  a {@link Node} before which the {@link DocumentFragment} should be added
         */
        public void addFragment(List<Node> fragment, Node refNode) {
            for (int i = 0; i < fragment.size(); i++) {
                final Node child = fragment.get(i);
                DomTripUtils.insertBefore(node, child, refNode);
            }
        }

        /**
         * A equivalent of {@code addGavtcs(gavtcs, getOrAddLastIndent())}
         *
         * @param  gavtcs the {@link Gavtcs} to add
         * @return        the newly created child node
         */
        public ContainerElement addGavtcs(Gavtcs gavtcs) {
            ensureClosingTagIndented();
            return addGavtcs(gavtcs, null);
        }

        /**
         * Add a new {@code <dependency>} node under {@link #node} with {@code <groupId>}, {@code <artifactId>}, etc.
         * set to value taken from the specified {@link Gavtcs}
         *
         * @param  gavtcs  the GAV coordinates to use when creating the new {@code <dependency>}
         * @param  refNode a {@link Node} before which the new {@link Element} should be added
         * @return         the newly created child node
         */
        public GavtcsElement addGavtcs(Gavtcs gavtcs, Node refNode) {

            final String parentName = getNode().name();
            final String childName = parentName.equals("dependencies") ? "dependency"
                    : parentName.substring(0, parentName.length() - 1);

            final ContainerElement dep = addChildContainerElement(childName, refNode, false, false);
            dep.addChildTextElement("groupId", gavtcs.getGroupId());
            dep.addChildTextElement("artifactId", gavtcs.getArtifactId());
            dep.addChildTextElement("version", gavtcs.getVersion());
            if (!"jar".equals(gavtcs.getType())) {
                dep.addChildTextElement("type", gavtcs.getType());
            }
            dep.addChildTextElement("classifier", gavtcs.getClassifier());
            dep.addChildTextElement("scope", gavtcs.getScope());
            final SortedSet<Ga> exclusions = gavtcs.getExclusions();
            if (!exclusions.isEmpty()) {
                final ContainerElement exclusionsNode = dep.addChildContainerElement("exclusions");
                for (Ga ga : exclusions) {
                    final ContainerElement exclusionNode = exclusionsNode.addChildContainerElement("exclusion");
                    exclusionNode.addChildTextElement("groupId", ga.getGroupId());
                    exclusionNode.addChildTextElement("artifactId", ga.getArtifactId());
                }
            }
            return dep.asGavtcsElement();
        }

        /**
         * If not available already, add a new {@code <dependency>} node under {@link #node} with {@code <groupId>},
         * {@code <artifactId>}, etc. set to value taken from the specified {@link Gavtcs}.
         * The availability and insertion point is determined using the given {@link Comparator}.
         *
         * @param  gavtcs     the GAV coordinates to use when creating the new {@code <dependency>}
         * @param  comparator for figuring out whether the given {@code gavtcs} is already available under this
         *                    {@link ContainerElement} or for determining the insert position for a newly added child node
         * @return            the newly created child node
         */
        public GavtcsElement addGavtcsIfNeeded(Gavtcs gavtcs, Comparator<Gavtcs> comparator) {
            /* Find the insertion position if the gavtcs is not available yet and possibly add it */
            Node refNode = null;
            for (ContainerElement dep : childElements()) {
                final Gavtcs depGavtcs = dep.asGavtcs();
                int comparison = comparator.compare(gavtcs, depGavtcs);
                if (comparison == 0) {
                    /* We have found the item, no need to add it */
                    return dep.asGavtcsElement();
                }
                if (refNode == null && comparison < 0) {
                    refNode = dep.previousSiblingInsertionRefNode();
                }
            }
            if (refNode == null) {
                ensureClosingTagIndented();
            }
            return addGavtcs(gavtcs, refNode);
        }

        /**
         * Transform this {@link ContainerElement} to a {@link NodeGavtcs} assuming that it has {@code <groupId>},
         * {@code <artifactId>}, etc. children
         *
         * @return a new {@link NodeGavtcs}
         */
        public NodeGavtcs asGavtcs() {
            String groupId = null;
            String artifactId = null;
            String version = null;
            String type = null;
            String classifier = null;
            String scope = null;
            List<Ga> exclusions = null;
            for (ContainerElement depChild : childElements()) {
                switch (depChild.node.name()) {
                case "groupId":
                    groupId = depChild.node.textContent();
                    break;
                case "artifactId":
                    artifactId = depChild.node.textContent();
                    break;
                case "version":
                    version = depChild.node.textContent();
                    break;
                case "type":
                    type = depChild.node.textContent();
                    break;
                case "classifier":
                    classifier = depChild.node.textContent();
                    break;
                case "scope":
                    scope = depChild.node.textContent();
                    break;
                case "exclusions":
                    exclusions = new ArrayList<>();
                    for (ContainerElement excl : depChild.childElements()) {
                        String exclGroupId = null;
                        String exclArtifactId = null;
                        for (ContainerElement exclChild : excl.childElements()) {
                            switch (exclChild.node.name()) {
                            case "groupId":
                                exclGroupId = exclChild.node.textContent();
                                break;
                            case "artifactId":
                                exclArtifactId = exclChild.node.textContent();
                                break;
                            }
                        }
                        exclusions.add(Ga.of(exclGroupId, exclArtifactId));
                    }
                    break;
                default:
                    break;
                }
            }
            return new NodeGavtcs(groupId, artifactId, version, type, classifier, scope, exclusions, this);
        }

        public ProfileElement asProfileElement() {
            return new ProfileElement(context, node, indentLevel);
        }

        public GavtcsElement asGavtcsElement() {
            String groupId = null;
            String artifactId = null;
            String version = null;
            String type = null;
            String classifier = null;
            String scope = null;
            List<Ga> exclusions = null;
            for (ContainerElement depChild : childElements()) {
                switch (depChild.node.name()) {
                case "groupId":
                    groupId = depChild.node.textContent();
                    break;
                case "artifactId":
                    artifactId = depChild.node.textContent();
                    break;
                case "version":
                    version = depChild.node.textContent();
                    break;
                case "type":
                    type = depChild.node.textContent();
                    break;
                case "classifier":
                    classifier = depChild.node.textContent();
                    break;
                case "scope":
                    scope = depChild.node.textContent();
                    break;
                case "exclusions":
                    exclusions = new ArrayList<>();
                    for (ContainerElement excl : depChild.childElements()) {
                        String exclGroupId = null;
                        String exclArtifactId = null;
                        for (ContainerElement exclChild : excl.childElements()) {
                            switch (exclChild.node.name()) {
                            case "groupId":
                                exclGroupId = exclChild.node.textContent();
                                break;
                            case "artifactId":
                                exclArtifactId = exclChild.node.textContent();
                                break;
                            }
                        }
                        exclusions.add(Ga.of(exclGroupId, exclArtifactId));
                    }
                    break;
                default:
                    break;
                }
            }
            return new GavtcsElement(context, node, indentLevel,
                    groupId, artifactId, version, type, classifier, scope, exclusions);
        }

    }

    public static class ProjectElement extends ContainerElement {
        private static volatile Map<String, ElementOrderEntry> elementOrdering;
        private static final Object elementOrderingLock = new Object();

        private ProjectElement(TransformationContext context, Element containerElement, int indentLevel) {
            super(context, containerElement, indentLevel);
        }

        /**
         * First attempts to find an element with the given {@code elementName}.
         * If it exists, it is returned as a {@link ContainerElement}. Otherwise
         * a new element with the given {@code elementName} is added under {@code <project>} node of the current
         * {@code pom.xml} file. The insert position is given by the
         * <a href="http://maven.apache.org/developers/conventions/code.html#POM_Code_Convention">POM Code Convention</a>.
         *
         * @param  elementName the name of the searched or newly added element
         * @return             a {@link ContainerElement} representing the existing or newly added node; never {@code null}
         */
        public ContainerElement getOrAddChildContainerElement(String elementName) {
            final Map<String, ElementOrderEntry> elementOrdering = getElementOrdering();
            final ElementOrderEntry newEntry = elementOrdering.get(elementName);
            if (newEntry == null) {
                throw new IllegalArgumentException("Unexpected child of <project>: " + elementName + "; expected any of " +
                        elementOrdering.keySet().stream().collect(Collectors.joining(", ")));
            }
            ElementOrderEntry previousProjectChildEntry = null;
            Node refNode = null;
            boolean emptyLineBefore = false;
            boolean emptyLineAfter = false;
            for (ContainerElement projectChild : childElements()) {
                final String projectChildName = projectChild.node.name();
                if (projectChildName.equals(elementName)) {
                    /* No need to insert, return existing */
                    return projectChild;
                }
                if (refNode == null) {
                    final ElementOrderEntry projectChildEntry = elementOrdering.get(projectChildName);
                    if (projectChildEntry != null) {
                        /* Process only known elements */
                        if (projectChildEntry.ordinal > newEntry.ordinal) {
                            refNode = projectChild.previousSiblingInsertionRefNode();
                            emptyLineBefore = previousProjectChildEntry != null
                                    && previousProjectChildEntry.groupId != newEntry.groupId;
                            emptyLineAfter = projectChildEntry != null && projectChildEntry.groupId != newEntry.groupId;
                        }
                        previousProjectChildEntry = projectChildEntry;
                    }
                }
            }
            if (refNode == null) {
                emptyLineBefore = previousProjectChildEntry != null && previousProjectChildEntry.groupId != newEntry.groupId;
            }
            return addChildContainerElement(
                    elementName,
                    refNode,
                    emptyLineBefore,
                    emptyLineAfter);
        }

        /**
         * Returns a {@link LinkedHashSet} of dependencies under {@code project/dependencies} node.
         * The elements are backed by the nodes of the {@link Document} of this {@link TransformationContext},
         * so any edits on those will get effective upon storing the {@link Document} back to {@link #pomXmlPath}.
         *
         * @return a {@link LinkedHashSet} of dependencies under the {@code <project>}
         */
        public Set<NodeGavtcs> getDependencies() {
            return getChildContainerElement("dependencies")
                    .map(deps -> deps.childElementsStream()
                            .map(dep -> dep.asGavtcs())
                            .collect(Collectors.toCollection(() -> (Set<NodeGavtcs>) new LinkedHashSet<NodeGavtcs>())))
                    .orElse(Collections.emptySet());
        }

        /**
         * Returns a {@link LinkedHashSet} of dependencies under {@code project/dependencyManagement/dependencies} node.
         * The elements a backed by the nodes of the {@link Document} of this {@link TransformationContext},
         * so any edits on those will get effective upon storing the {@link Document} back to {@link #pomXmlPath}.
         *
         * @return a {@link LinkedHashSet} of dependencies under the {@code <project>}
         */
        public Set<NodeGavtcs> getManagedDependencies() {
            return getChildContainerElement("dependencyManagement", "dependencies")
                    .map(deps -> deps.childElementsStream()
                            .map(dep -> dep.asGavtcs())
                            .collect(Collectors.toCollection(() -> (Set<NodeGavtcs>) new LinkedHashSet<NodeGavtcs>())))
                    .orElse(Collections.emptySet());
        }

        /**
         * @param  gavtcs the {@link Gavtcs} to find
         * @return        an optional containing the dependency node matching the given {@code gavtcs} or an empty
         *                {@link Optional} if no such dependency exists
         */
        public Optional<GavtcsElement> findDependency(Gavtcs gavtcs) {
            return getChildContainerElement("dependencies")
                    .map(depsNode -> depsNode.childElementsStream()
                            .map(ContainerElement::asGavtcsElement)
                            .filter(depNode -> depNode.getGavtcs().equals(gavtcs))
                            .findFirst()
                            .orElse(null));
        }

        /**
         * @return POM elements ordered according to
         *         <a href="http://maven.apache.org/developers/conventions/code.html#POM_Code_Convention">POM Code
         *         Convention</a>
         */
        static Map<String, ElementOrderEntry> getElementOrdering() {
            if (elementOrdering == null) {
                synchronized (elementOrderingLock) {
                    if (elementOrdering == null) {
                        Map<String, ElementOrderEntry> m = new TreeMap<String, ElementOrderEntry>();
                        int i = 0;
                        int g = 0;
                        m.put("modelVersion", new ElementOrderEntry(i++, g));
                        g++;
                        m.put("parent", new ElementOrderEntry(i++, g));
                        g++;
                        m.put("groupId", new ElementOrderEntry(i++, g));
                        m.put("artifactId", new ElementOrderEntry(i++, g));
                        m.put("version", new ElementOrderEntry(i++, g));
                        m.put("packaging", new ElementOrderEntry(i++, g));
                        g++;
                        m.put("name", new ElementOrderEntry(i++, g));
                        m.put("description", new ElementOrderEntry(i++, g));
                        m.put("url", new ElementOrderEntry(i++, g));
                        m.put("inceptionYear", new ElementOrderEntry(i++, g));
                        m.put("organization", new ElementOrderEntry(i++, g));
                        m.put("licenses", new ElementOrderEntry(i++, g));
                        g++;
                        m.put("developers", new ElementOrderEntry(i++, g));
                        m.put("contributors", new ElementOrderEntry(i++, g));
                        g++;
                        m.put("mailingLists", new ElementOrderEntry(i++, g));
                        g++;
                        m.put("prerequisites", new ElementOrderEntry(i++, g));
                        g++;
                        m.put("modules", new ElementOrderEntry(i++, g));
                        g++;
                        m.put("scm", new ElementOrderEntry(i++, g));
                        m.put("issueManagement", new ElementOrderEntry(i++, g));
                        m.put("ciManagement", new ElementOrderEntry(i++, g));
                        m.put("distributionManagement", new ElementOrderEntry(i++, g));
                        g++;
                        m.put("properties", new ElementOrderEntry(i++, g));
                        g++;
                        m.put("dependencyManagement", new ElementOrderEntry(i++, g));
                        m.put("dependencies", new ElementOrderEntry(i++, g));
                        g++;
                        m.put("repositories", new ElementOrderEntry(i++, g));
                        m.put("pluginRepositories", new ElementOrderEntry(i++, g));
                        g++;
                        m.put("build", new ElementOrderEntry(i++, g));
                        g++;
                        m.put("reporting", new ElementOrderEntry(i++, g));
                        g++;
                        m.put("profiles", new ElementOrderEntry(i++, g));
                        elementOrdering = Collections.unmodifiableMap(m);
                    }
                }
            }
            return elementOrdering;
        }

        static class ElementOrderEntry {
            private final int ordinal;
            private final int groupId;

            public ElementOrderEntry(int ordinal, int groupId) {
                this.ordinal = ordinal;
                this.groupId = groupId;
            }
        }
    }

    public static class ProfileElement extends ProjectElement {

        private ProfileElement(TransformationContext context, Element containerElement, int indentLevel) {
            super(context, containerElement, indentLevel);
        }

        /**
         * @return {@code null} if this {@link ContainerElement} is a {@code project} node or otherwise the text content of the
         *         {@code id} child of this {@link ContainerElement}
         */
        public String getId() {
            if ("project".equals(getElementName())) {
                return null;
            }
            return childTextElementsStream()
                    .filter(textElement -> "id".equals(textElement.getElementName()))
                    .map(TextElement::getTextContent)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Could not find the id value of profile " + this + " in " + context.getPomXmlPath()));
        }

    }

    /**
     * A {@link Gavtcs} storing the associated {@link ContainerElement} from the {@code pom.xml} file.
     * This is useful for DOM modifications.
     */
    public static class GavtcsElement extends ContainerElement {
        private final Gavtcs gavtcs;

        GavtcsElement(
                TransformationContext context, Element containerElement, int indentLevel, String groupId,
                String artifactId, String version, String type, String classifier, String scope, Collection<Ga> exclusions) {
            super(context, containerElement, indentLevel);
            this.gavtcs = new Gavtcs(groupId, artifactId, version, type, classifier, scope, exclusions);
        }

        /**
         * @return the associated {@link Gavtcs}.
         */
        public Gavtcs getGavtcs() {
            return gavtcs;
        }

        /**
         * Set this element's {@code <groupId>} child to the given {@code groupId} value, adding the {@code <groupId>} node if
         * necessary
         * or removing it if {@code groupId} is {@code null}.
         *
         * @param classifier the version to set or {@code null} if the {@code <groupId>} node should be removed
         * @since            5.0.0
         */
        public GavtcsElement setGroupId(String groupId) {
            addOrSetChildTextElement("groupId", groupId);
            return this;
        }

        /**
         * Set this element's {@code <artifactId>} child to the given {@code artifactId} value, adding the {@code <artifactId>}
         * node if necessary
         * or removing it if {@code artifactId} is {@code null}.
         *
         * @param classifier the version to set or {@code null} if the {@code <artifactId>} node should be removed
         * @since            5.0.0
         */
        public GavtcsElement setArtifactId(String artifactId) {
            addOrSetChildTextElement("artifactId", artifactId);
            return this;
        }

        /**
         * Set this element's {@code <version>} child to the given {@code version} value, adding the {@code <version>} node if
         * necessary
         * or removing it if {@code version} is {@code null}.
         *
         * @param version the version to set or {@code null} if the {@code <version>} node should be removed
         * @since         5.0.0
         */
        public GavtcsElement setVersion(String version) {
            addOrSetChildTextElement("version", version);
            return this;
        }

        /**
         * Set this element's {@code <classifier>} child to the given {@code classifier} value, adding the {@code <classifier>}
         * node if necessary
         * or removing it if {@code classifier} is {@code null}.
         *
         * @param classifier the version to set or {@code null} if the {@code <classifier>} node should be removed
         * @since            5.0.0
         */
        public GavtcsElement setClassifier(String classifier) {
            addOrSetChildTextElement("classifier", classifier);
            return this;
        }

        /**
         * Set this element's {@code <scope>} child to the given {@code scope} value, adding the {@code <scope>} node if
         * necessary
         * or removing it if {@code scope} is {@code null}.
         *
         * @param classifier the version to set or {@code null} if the {@code <scope>} node should be removed
         * @since            5.0.0
         */
        public GavtcsElement setScope(String scope) {
            addOrSetChildTextElement("scope", scope);
            return this;
        }
    }

    /**
     * Writes to {@link #path} only if the content is different from the content read by {@link #read()}
     */
    static class LazyWriter {
        private String oldContent;
        private final Path path;
        private final Charset charset;

        LazyWriter(Path path, Charset charset) {
            this.path = path;
            this.charset = charset;
        }

        String read() {
            try {
                oldContent = new String(Files.readAllBytes(path), charset);
                return oldContent;
            } catch (IOException e) {
                throw new RuntimeException(String.format("Could not read DOM from [%s]", path), e);
            }
        }

        void write(String newContent) {
            if (!oldContent.equals(newContent)) {
                try {
                    Files.write(path, newContent.getBytes(charset));
                } catch (IOException e) {
                    throw new RuntimeException(String.format("Could not write DOM from [%s]", path), e);
                }
            }
        }
    }

    /**
     * A context of a set of {@link Transformation} operations.
     */
    public static class TransformationContext {

        private final Path pomXmlPath;
        private final Document document;
        private final ProjectElement project;
        private final String indentationString;

        TransformationContext(Path pomXmlPath, Document document, String indentationString) {
            super();
            this.pomXmlPath = pomXmlPath;
            this.document = document;
            this.indentationString = indentationString;
            this.project = new ProjectElement(this, document.root(), 0);
        }

        /**
         * @return the path to the {@code pom.xml} file that is being transformed
         */
        public Path getPomXmlPath() {
            return pomXmlPath;
        }

        /**
         * @return the {@link Document} being transformed
         */
        public Document getDocument() {
            return document;
        }

        /**
         * @return an indentation string (without newline characters) as it was autodetected using
         *         {@link PomTransformer#detectIndentation(Node, XPath)}
         */
        public String getIndentationString() {
            return indentationString;
        }

        /**
         * @param  indentCount how many times to concatenate the {@link #indentationString}
         * @return             a new indentation node containing a newline and {@code indentCount} times concatenated
         *                     {@link #indentationString}
         */
        public Text indentNode(int indentCount) {
            return Text.of(indent(indentCount));
        }
        /**
         * @param  indentCount how many times to concatenate the {@link #indentationString}
         * @return             a new indentation {@link String} containing a newline and {@code indentCount} times concatenated
         *                     {@link #indentationString}
         */
        public String indent(int indentCount) {
            final StringBuilder sb = new StringBuilder(1 + indentCount * indentationString.length());
            sb.append('\n');
            for (int i = 0; i < indentCount; i++) {
                sb.append(indentationString);
            }
            return sb.toString();
        }

        /**
         * @return a newly created text node having single newline {@code \n} as its content.
         */
        public Text newLine() {
            return Text.of("\n");
        }

        /**
         * First attempts to find an element at the given path under the {@code <project>} element
         * of the current {@code pom.xml} file. If it exists, it is returned as a {@link ContainerElement}. Otherwise
         * a new element at the given path is added under {@code <project>} node of the current
         * {@code pom.xml} file. The insert position of {@code elementName} is given by the
         * <a href="http://maven.apache.org/developers/conventions/code.html#POM_Code_Convention">POM Code Convention</a>.
         * <p>
         * Note that unlike this method, {@link #getContainerElement(String...)} operates on the document level. Hence
         * while you'd normally call {@code getOrAddContainerElements("dependencyManagement", "dependencies")}
         * for {@code getContainerElement()} the arguments would have to start with {@code "project"}:
         * {@code getContainerElements("project", "dependencyManagement", "dependencies")}.
         *
         * @param  elementName  the name of the searched or newly added element
         * @param  furtherNames
         * @return              a {@link ContainerElement} representing the existing or newly added node; never {@code null}
         */
        public ContainerElement getOrAddContainerElements(String elementName, String... furtherNames) {
            ContainerElement parent = getOrAddContainerElement(elementName);
            for (int i = 0; i < furtherNames.length; i++) {
                parent = parent.getOrAddChildContainerElement(furtherNames[i]);
            }
            return parent;
        }

        /**
         * First attempts to find an element with the given {@code elementName} under the {@code <project>} element
         * of the current {@code pom.xml} file. If it exists, it is returned as a {@link ContainerElement}. Otherwise
         * a new element with the given {@code elementName} is added under {@code <project>} node of the current
         * {@code pom.xml} file. The insert position is given by the
         * <a href="http://maven.apache.org/developers/conventions/code.html#POM_Code_Convention">POM Code Convention</a>.
         *
         * @param  elementName the name of the searched or newly added element
         * @return             a {@link ContainerElement} representing the existing or newly added node; never {@code null}
         */
        public ContainerElement getOrAddContainerElement(String elementName) {
            return project.getOrAddChildContainerElement(elementName);
        }

        /**
         * @return a {@link ContainerElement} pointing at the {@code <project>} element of the current {@code pom.xml} file
         */
        public ProjectElement getProject() {
            return project;
        }

        /**
         * @param  profileId the {@code id} of the profile to look up
         * @return           an {@link Optional} containing a {@link ContainerElement} pointing at the {@code <profile>} element
         *                   of the given profile or an empty {@link Optional} if no such profile exists
         */
        public Optional<ProfileElement> getProfile(String profileId) {
            Objects.requireNonNull(profileId, "profileId");
            return getContainerElement("project", "profiles").flatMap(profiles -> profiles.childElementsStream()
                    .map(ContainerElement::asProfileElement)
                    .filter(profile -> profileId.equals(profile.getId()))
                    .findFirst());

        }

        public List<ProfileElement> getProfiles() {
            final List<ProfileElement> result = new ArrayList<>();
            getContainerElement("project").map(ContainerElement::asProfileElement).ifPresent(result::add);
            getContainerElement("project", "profiles").ifPresent(profiles -> {
                profiles.childElementsStream()
                        .map(ContainerElement::asProfileElement)
                        .forEach(result::add);
            });
            return Collections.unmodifiableList(result);
        }

        public Stream<ProfileElement> getProfilesStream() {
            return getProfiles().stream();
        }

        /**
         * @param  profileId the {@code id} of the profile to look up or create
         * @return           a {@link ProfileElement} pointing at a new or existing {@code <profile>} element having
         *                   {@code <id>} equal to the given code {@code id}
         */
        public ProfileElement getOrAddProfile(String profileId) {
            Objects.requireNonNull(profileId, "profileId");
            Optional<ProfileElement> maybeProfile = getProfile(profileId);
            if (maybeProfile.isPresent()) {
                return maybeProfile.get();
            } else {
                ContainerElement profile = getOrAddContainerElement("profiles").addChildContainerElement("profile");
                profile.addChildTextElement("id", profileId);
                return profile.asProfileElement();
            }
        }

        /**
         * @param  profileId the {@code id} of the profile to look up; pass {@code null} to return the {@code <project>} element
         * @return           an Optional containing the {@code <project>} element if the {@code profileId} is {@code null} or
         *                   otherwise delegate to {@link #getProfile(String)}
         */
        public Optional<ProfileElement> getProfileParent(String profileId) {
            if (profileId == null) {
                return Optional.of(project.asProfileElement());
            } else {
                return getProfile(profileId);
            }
        }

        /**
         * @param  profileId the {@code id} of the profile to look up; pass {@code null} to return the {@code <project>} element
         * @return           a {@link ContainerElement} pointing at the {@code <project>} element if the {@code profileId} is
         *                   {@code null} or otherwise delegates to {@link #getOrAddProfile(String)}
         */
        public ProfileElement getOrAddProfileParent(String profileId) {
            if (profileId == null) {
                return project.asProfileElement();
            } else {
                return getOrAddProfile(profileId);
            }
        }

        /**
         * Attempts to find an element at the given path in the current {@code pom.xml} file. If it exists, it is
         * returned as an {@link ContainerElement} {@link Optional}. Otherwise an empty {@link Optional} is returned.
         * <p>
         * Note that unlike this method, {@link #getOrAddContainerElements(String...)} operates on the {@code <project>}
         * node. Hence while you'd normally call {@code getOrAddContainerElements("dependencyManagement", "dependencies")}
         * for {@code getContainerElement()} the arguments would have to start with {@code "project"}:
         * {@code getContainerElements("project", "dependencyManagement", "dependencies")}.
         *
         * @param  path a document level path starting with {@code "project"}
         * @return      An optional possibly refering to a node under the given {@code path}.
         */
        public Optional<ContainerElement> getContainerElement(String... path) {
            if (path.length == 0) {
                throw new IllegalArgumentException();
            }
            if (!"project".equals(path[0])) {
                throw new IllegalArgumentException();
            }
            if (path.length == 1) {
                return Optional.of(this.project);
            }
            final String[] rest = new String[path.length - 2];
            System.arraycopy(path, 2, rest, 0, rest.length);
            return this.project.getChildContainerElement(path[1], rest);
        }

        /**
         * Resets the indentation of the given {@link Node} to the given {@code newIndent}.
         * If the given {@code node} has children, then the indentation of the children gets reset as well to an
         * appropriately expanded indentation string.
         *
         * @param node      the node whose indentation should be reset
         * @param newIndent the new indentation string
         */
        public void reIndent(Node node, String newIndent) {
            switch (node.type()) {
            case TEXT: {
                Text text = ((Text) node);
                fixIndent(text::content, text::content, newIndent);
                break;
            }
            case ELEMENT: {
                Element elem = ((Element) node);

                fixIndent(elem::precedingWhitespace, elem::precedingWhitespace, newIndent);
                fixIndent(elem::innerPrecedingWhitespace, elem::innerPrecedingWhitespace, newIndent);

                String passIndent = newIndent + indentationString;
                final int nodeCount = elem.nodeCount();
                for (int i = 0; i < nodeCount; i++) {
                    final Node child = elem.getNode(i);
                    if (i + 1 == nodeCount && child.type() == NodeType.TEXT) {
                        /* the last indent before the closing element */
                        reIndent(child, newIndent);
                    } else {
                        reIndent(child, passIndent);
                    }
                }
                break;
            }
            default:
                break;
            }
        }

        static void fixIndent(Supplier<String> get, Consumer<String> set,String newIndent) {
            final String oldValue = get.get();
            final String newValue = INDENT_PATTERN.matcher(oldValue).replaceAll("$1" + newIndent);
            if (!oldValue.equals(newValue)) {
                set.accept(newValue);
            }
        }

        /**
         * Calls {@link #reIndent(Node, String)} for each of the given {@code nodes}.
         *
         * @param nodes     the nodes whose indentation should be reset
         * @param newIndent the new indentation string
         */
        public void reIndent(List<Node> nodes, String newIndent) {
            for (Node node : nodes) {
                reIndent(node, newIndent);
            }
        }

        /**
         * @param  refNode the {@link Node} to decide about
         * @return         {@code true} if the given {@code node} is a text node and its text matches
         *                 {@value PomTransformer#EMPTY_LINE_REGEX} or {@code false} otherwise
         */
        public static boolean isEmptyLineNode(Node refNode) {
            return refNode instanceof Text && ((Text) refNode).content() != null
                    && EMPTY_LINE_PATTERN.matcher(((Text) refNode).content()).matches();
        }

        /**
         * @param  node the {@link Node} to decide about
         * @return      {@code true} if the given {@code node} is a text node and its text matches
         *              {@value PomTransformer#EMPTY_LINE_REGEX} or {@code false} otherwise
         */
        public static boolean hasEmptyLineBefore(Node node) {
            return EMPTY_LINE_PATTERN.matcher(node.precedingWhitespace()).matches();
        }

        /**
         * @param  node the {@link Node} to decide about
         * @return      {@code true} if the given {@code node} is a text node and its text matches
         *              {@value PomTransformer#WS_REGEX} or {@code false} otherwise
         */
        public static boolean isWhiteSpaceNode(Node node) {
            return node.type() == NodeType.TEXT
                    && ((Text) node).content() != null
                    && WS_PATTERN.matcher(((Text) node).content()).matches();
        }

        public static boolean hasElementChildren(Node node) {
            if (node instanceof ContainerNode) {
                ContainerNode cn = (ContainerNode) node;
                int cnt = cn.nodeCount();
                if (cnt == 0) {
                    return false;
                }
                for (int i = 0; i < cnt; i++) {
                    final Node child = cn.getNode(i);
                    if (child.type() != NodeType.COMMENT && !isWhiteSpaceNode(child)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Encloses the given text node in XML comment marks {@code <!--} and {@code -->} optionally adding a comment before the
         * closing mark.
         *
         * @param  node        the node to comment
         * @param  commentText an optional comment text to add before the closing mark {@code -->}
         * @return             the new {@link Comment} node
         */
        public static Comment commentTextNode(Element node, String commentText) {
            final String moduleText = node.textContent();
            final String nodeName = node.name();
            final Comment moduleComment = Comment.of(" <" + nodeName + ">" + moduleText + "</" + nodeName + ">"
                    + (commentText != null ? (" " + commentText + " ") : " "));
            DomTripUtils.replace(moduleComment, node);
            return moduleComment;
        }

        /**
         * @param  precedingNodesConsumer a {@link Consumer} to which the preceding comment and whitespace nodes are passed; can
         *                                be {@code null}
         * @return                        a {@link Node} {@link Consumer} that deletes the given node, optionally passing the
         *                                preceding comment and whitespace nodes to the given {@code precedingNodesConsumer}
         *
         * @since                         4.1.0
         */
        public static Consumer<Node> removeNode(BiConsumer<Node, Node> precedingNodesConsumer) {
            return (Node deletedNode) -> {
                if (precedingNodesConsumer != null) {
                    Node prevSibling = DomTripUtils.previousSibling(deletedNode);
                    while (prevSibling != null
                            && (TransformationContext.isWhiteSpaceNode(prevSibling)
                                    || prevSibling.type() == NodeType.COMMENT)) {
                        /* remove any preceding whitespace or comments */
                        precedingNodesConsumer.accept(deletedNode, prevSibling);
                        final Node newPrevSibling = DomTripUtils.previousSibling(deletedNode);
                        if (prevSibling == newPrevSibling) {
                            break;
                        }
                        prevSibling = newPrevSibling;
                    }
                }
                deletedNode.parent().removeNode(deletedNode);
            };
        }

        /**
         * @param  removePrecedingComments   if {@code true} the comments preceding the removed nodes will be removed;
         *                                   otherwise the preceding comments won't be removed
         * @param  removePrecedingWhitespace if {@code true} the whitespace nodes preceding the removed nodes will be
         *                                   be removed; otherwise the preceding whitespace nodes won't be removed
         * @return                           a new {@link BiConsumer} that removes the preceding comments and/or white space
         *                                   depending on the
         *                                   given {@code removePrecedingComments} and {@code removePrecedingWhitespace} values
         *
         * @since                            4.1.0
         */
        public static BiConsumer<Node, Node> removePrecedingCommentsAndWhiteSpace(
                boolean removePrecedingComments,
                boolean removePrecedingWhitespace) {
            return (Node deletedNode, Node whitespaceOrComment) -> {
                if ((removePrecedingWhitespace && TransformationContext.isWhiteSpaceNode(whitespaceOrComment))
                        || (removePrecedingComments && whitespaceOrComment.type() == NodeType.COMMENT)) {
                    /* remove any preceding whitespace or comments */
                    whitespaceOrComment.parent().removeNode(whitespaceOrComment);
                }
            };
        }

    }

    public static class RemovableNode {
        private final Node node;
        private final Consumer<Node> remove;
        private final Function<Node, Node> previous;
        private final Function<Node, Node> next;

        public static RemovableNode of(Node node) {
            //            Node nxt = DomTripUtils.nextSibling(node);
            //            if (nxt != null) {
            //                final String precedingWhitespace = nxt.precedingWhitespace();
            //                if (!precedingWhitespace.isEmpty()) {
            //                    return new RemovableNode(new Text(precedingWhitespace), n -> nxt.precedingWhitespace(""),
            //                            DomTripUtils::previousSibling, n -> nxt);
            //                }
            //            }
            return new RemovableNode(node, DomTripUtils::remove, DomTripUtils::previousSibling,
                    DomTripUtils::nextSibling);

        }

        RemovableNode(Node node, Consumer<Node> remove, Function<Node, Node> previous, Function<Node, Node> next) {
            this.node = node;
            this.remove = remove;
            this.previous = previous;
            this.next = next;
        }

        public RemovableNode previousSibling() {
            final String precedingWhitespace = node.precedingWhitespace();
            if (!precedingWhitespace.isEmpty()) {
                return new RemovableNode(new Text(precedingWhitespace), n -> node.precedingWhitespace(""),
                        n -> DomTripUtils.previousSibling(node), DomTripUtils::nextSibling);
            }
            final Node prev = previous.apply(node);
            return prev != null ? new RemovableNode(
                    prev,
                    DomTripUtils::remove,
                    DomTripUtils::previousSibling,
                    DomTripUtils::nextSibling) : null;
        }

        public void remove() {
            remove.accept(node);
        }

        public Node node() {
            return node;
        }

        public RemovableNode nextSibling() {
            Node nxt = next.apply(node);
            if (nxt == null) {
                return null;
            }
            if (nxt instanceof IgnorePrecedingWsNode) {
                nxt = ((IgnorePrecedingWsNode) nxt).delegate;
            } else {
                final Node localNxt = nxt;
                final String precedingWhitespace = localNxt.precedingWhitespace();
                if (!precedingWhitespace.isEmpty()) {
                    return new RemovableNode(new Text(precedingWhitespace), n -> localNxt.precedingWhitespace(""),
                            DomTripUtils::previousSibling, n -> new IgnorePrecedingWsNode(localNxt));
                }
            }
            return new RemovableNode(nxt, DomTripUtils::remove, DomTripUtils::previousSibling,
                    DomTripUtils::nextSibling);
        }

        @Override
        public String toString() {
            return node.toString();
        }

        static class IgnorePrecedingWsNode extends Node {
            private final Node delegate;

            private IgnorePrecedingWsNode(Node delegate) {
                this.delegate = delegate;
            }

            @Override
            public NodeType type() {
                return delegate.type();
            }

            @Override
            public String toXml() {
                return delegate.toXml();
            }

            @Override
            public void toXml(StringBuilder sb) {
                delegate.toXml(sb);
            }

        }
    }

    public static class DomTripUtils {
        static int indexOf(Node child) {
            if (child == null) {
                return -1;
            }
            final ContainerNode parent = child.parent();
            if (parent == null) {
                return -1;
            }
            final int childCount = parent.nodeCount();
            for (int i = 0; i < childCount; i++) {
                if (child == parent.getNode(i)) {
                    return i;
                }
            }
            return -1;
        }

        public static void insertBefore(ContainerNode parent, Node newNode, Node refNode) {
            if (refNode == null) {
                parent.addNode(newNode);
            } else {
                int i = DomTripUtils.indexOf(refNode);
                parent.insertNode(i, newNode);
            }
        }

        public static void remove(Node node) {
            final ContainerNode parent = node.parent();
            if (parent != null) {
                parent.removeNode(node);
            }
        }
        public static Node previousSibling(Node node) {
            int i = indexOf(node);
            if (i <= 0) {
                return null;
            }
            return node.parent().getNode(i - 1);
        }

        public static Node nextSibling(Node node) {
            int i = indexOf(node);
            if (i < 0) {
                return null;
            }
            i++;
            final ContainerNode parent = node.parent();
            if (i >= parent.nodeCount()) {
                return null;
            }
            return parent.getNode(i);
        }

        public static Node lastChild(ContainerNode parent) {
            final int cnt = parent.nodeCount();
            if (cnt > 0) {
                return parent.getNode(cnt - 1);
            }
            return null;
        }

        public static void replace(Node newNode, Node oldNode) {
            int i = DomTripUtils.indexOf(oldNode);
            final ContainerNode parent = oldNode.parent();
            parent.removeNode(oldNode);
            newNode.precedingWhitespace(oldNode.precedingWhitespace());
            parent.insertNode(i, newNode);
        }

        public static Optional<Element> findProfile(Document document, String profileId) {
            if (profileId == null) {
                return Optional.of(document.root());
            } else {
                return document.root()
                        .child("profiles")
                        .flatMap(profiles -> profiles.children()
                                .filter(ch -> "profile".equals(ch.name()))
                                .filter(profile -> profile.child("id")
                                        .filter(pid -> profileId.equals(pid.textContent()))
                                        .isPresent())
                                .findFirst());
            }
        }
    }

    /**
     * A transformation of a DOM
     */
    public interface Transformer {
        /**
         * Perform this {@link Transformation} on the given {@code document}
         *
         * @param context the current {@link TransformationContext}
         */
        void perform(TransformationContext context);

    }

    public interface Transformation extends Transformer {

        /**
         * @param      groupId
         * @param      artifactId
         * @param      version
         * @return
         *
         * @deprecated            use {@link dependencyManagement#add(Gavtcs)} instead
         */
        @Deprecated
        public static Transformation addManagedDependency(String groupId, String artifactId, String version) {
            return addManagedDependency(new Gavtcs(groupId, artifactId, version, null, null, null));
        }

        /**
         * @param      gavtcs
         * @return
         *
         * @deprecated        use {@link dependencyManagement#add(Gavtcs)} instead
         */
        @Deprecated
        public static Transformation addManagedDependency(Gavtcs gavtcs) {
            return (Document document, TransformationContext context) -> {
                final ContainerElement dependencyManagementDeps = context.getOrAddContainerElements("dependencyManagement",
                        "dependencies");
                dependencyManagementDeps.addGavtcs(gavtcs);
            };
        }

        /**
         * @param      gavtcs
         * @return
         * @deprecated        use {@link dependencyManagement#add(Gavtcs)} instead
         */
        @Deprecated
        public static Transformation addManagedDependencyIfNeeded(Gavtcs gavtcs) {
            return (Document document, TransformationContext context) -> {
                final ContainerElement dependencyManagementDeps = context.getOrAddContainerElements("dependencyManagement",
                        "dependencies");

                if (!dependencyManagementDeps.childElementsStream()
                        .map(ContainerElement::asGavtcs)
                        .anyMatch(dep -> dep.equals(gavtcs))) {
                    dependencyManagementDeps.addGavtcs(gavtcs);
                }

            };
        }

        public static Transformation commentModules(Collection<String> modulesToComment, String commentText) {
            return (Document document, TransformationContext context) -> {
                modules.select(modulesToComment::contains).commentOut(textElem -> commentText).perform(context);
            };
        }

        /**
         * Comments modules which are present in a profile definition (with the specified profile id).
         *
         * @param  profile          Name of the profile, whose modules are commented
         * @param  modulesToComment Collection of modules to be commented.
         * @param  commentText      Explanation for the comment
         * @return                  Transformation.
         */
        public static Transformation commentModulesInProfile(String profile, Collection<String> modulesToComment,
                String commentText) {
            return (Document document, TransformationContext context) -> {
                modules.select(modulesToComment::contains).fromProfilesOnly(profile).commentOut(textElem -> commentText)
                        .perform(context);
            };
        }

        /**
         * @param  newVersion the new version to set on the given {@code gas}
         * @param  gas        the list of {@link Ga} on which the {@code newVersion} should be set
         * @return            a new {@link Transformation}
         */
        public static Transformation setManagedDependencyVersion(String newVersion, Collection<Ga> gas) {
            return setManagedDependencyVersion(null, newVersion, gas);
        }

        public static Transformation setManagedDependencyVersion(String profileId, String newVersion, Collection<Ga> gas) {
            return (Document document, TransformationContext context) -> {
                final ContainerElement profileParent = context.getProfileParent(profileId)
                        .orElseThrow(() -> new IllegalStateException(
                                "No such profile '" + profileId + "' found in " + context.getPomXmlPath()));
                final ContainerElement dependencyManagementDeps = profileParent
                        .getChildContainerElement("dependencyManagement").orElseThrow(
                                () -> new IllegalStateException("dependencyManagement not found under profile '" + profileId
                                        + "' in " + context.getPomXmlPath()))
                        .getChildContainerElement("dependencies").orElseThrow(
                                () -> new IllegalStateException(
                                        "dependencyManagement/dependencies not found under profile '" + profileId + "' in "
                                                + context.getPomXmlPath()));

                for (ContainerElement containerElememt : dependencyManagementDeps.childElements()) {
                    final GavtcsElement dep = containerElememt.asGavtcsElement();
                    if (gas.contains(dep.getGavtcs().toGa())) {
                        dep.setVersion(newVersion);
                    }
                }
            };
        }

        public static Transformation setDependencyVersion(String newVersion, Collection<Ga> gas) {
            return setDependencyVersion(null, newVersion, gas);
        }

        public static Transformation setDependencyVersion(String profileId, String newVersion, Collection<Ga> gas) {
            return (Document document, TransformationContext context) -> {
                final ContainerElement profileParent = context.getProfileParent(profileId)
                        .orElseThrow(() -> new IllegalStateException(
                                "No such profile '" + profileId + "' found in " + context.getPomXmlPath()));
                final ContainerElement deps = profileParent
                        .getChildContainerElement("dependencies").orElseThrow(
                                () -> new IllegalStateException(
                                        "dependencies not found under profile '" + profileId + "' in "
                                                + context.getPomXmlPath()));

                for (ContainerElement containerElememt : deps.childElements()) {
                    final GavtcsElement dep = containerElememt.asGavtcsElement();
                    if (gas.contains(dep.getGavtcs().toGa())) {
                        dep.setVersion(newVersion);
                    }
                }
            };
        }

        /**
         * @param      gavtcs
         * @param      comparator
         * @return
         * @deprecated            use {@link dependencies#add(Gavtcs)} instead
         */
        @Deprecated
        public static Transformation addDependencyIfNeeded(Gavtcs gavtcs, Comparator<Gavtcs> comparator) {
            Transformer t = dependencies.add(gavtcs).at(comparator);
            return (Document document, TransformationContext context) -> t.perform(context);
        }

        /**
         * @param      removePrecedingComments
         * @param      removePrecedingWhitespace
         * @param      module
         * @return
         *
         * @deprecated                           use {@link modules#remove(String...)} instead
         */
        @Deprecated
        public static Transformation removeModule(boolean removePrecedingComments, boolean removePrecedingWhitespace,
                String module) {
            return (Document document, TransformationContext context) -> {
                modules.remove(module).alsoRemoveNone().alsoRemovePrevious(selector(removePrecedingComments, removePrecedingWhitespace))
                        .perform(context);
            };
        }

        static Predicate<Node> selector(boolean removePrecedingComments, boolean removePrecedingWhitespace) {
            Predicate<Node> result = n -> false;
            if (removePrecedingComments) {
                result = result.or(Siblings.comments());
            }
            if (removePrecedingWhitespace) {
                result = result.or(Siblings.whitespace());
            }
            return result;
        }

        /**
         * @param      removePrecedingComments
         * @param      removePrecedingWhitespace
         * @param      modules
         * @return
         *
         * @deprecated                           use {@link modules#remove(String...)} instead
         */
        @Deprecated
        public static Transformation removeModules(boolean removePrecedingComments, boolean removePrecedingWhitespace,
                Set<String> modulesToRemove) {
            return (Document document, TransformationContext context) -> {
                modules.remove(te -> modulesToRemove.contains(te.getTextContent()))
                        .alsoRemoveNone()
                        .alsoRemovePrevious(selector(removePrecedingComments, removePrecedingWhitespace)).perform(context);
            };
        }

        public static Transformation uncommentModules(String commentText) {
            return uncommentModules(commentText, m -> true);
        }

        public static Transformation uncommentModules(String commentText, Predicate<String> modulePathFilter) {
            return uncommentModules(commentText, modulePathFilter, null);
        }

        public static Transformation uncommentModules(String commentText, Predicate<String> modulePathFilter,
                String profileId) {
            final String expandedCommentText = " " + commentText + " ";
            return (Document document, TransformationContext context) -> {

                modules.selectComments(comment -> {
                    final Document doc = comment.getParsedContent();

                    if (doc.nodeCount() != 2) {
                        return false;
                    }
                    Node text = doc.getNode(1);
                    if (text.type() != NodeType.TEXT || !expandedCommentText.equals(((Text) text).content())) {
                        return false;
                    }
                    Node node = doc.getNode(0);
                    if (node.type() != NodeType.ELEMENT) {
                        return false;
                    }
                    Element element = (Element) node;
                    return modulePathFilter.test(element.textContent());
                })
                        .fromProfilesOnly(profileId)
                        .modify(comment -> DomTripUtils.replace(comment.getParsedContent().getNode(0), comment.getSource()))
                        .perform(context);
            };
        }

        /**
         * @param      removePrecedingComments
         * @param      removePrecedingWhitespace
         * @param      propertyName
         * @return
         *
         * @deprecated                           use {@link properties#remove(String...)} instead
         */
        @Deprecated
        public static Transformation removeProperty(boolean removePrecedingComments, boolean removePrecedingWhitespace,
                String propertyName) {
            return (Document document, TransformationContext context) -> {
                properties.remove(propertyName)
                        .alsoRemoveNone()
                        .alsoRemovePrevious(Transformation.selector(removePrecedingComments, removePrecedingWhitespace))
                        .perform(context);
            };
        }

        /**
         * Perform this {@link Transformation} on the given {@code document}
         *
         * @param document the {@link Document} to transform
         * @param context  the current {@link TransformationContext}
         */
        void perform(Document document, TransformationContext context);

        default void perform(TransformationContext context) {
            perform(context.document, context);
        }

    }

}
