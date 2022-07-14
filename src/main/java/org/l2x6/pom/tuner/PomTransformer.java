/**
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
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.l2x6.pom.tuner.model.Ga;
import org.l2x6.pom.tuner.model.Gavtcs;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * A utility to transform {@code pom.xml} files on the DOM level while keeping the original comments and formatting also
 * on places where common {@code javax.xml.transform.Transformer} or {@code javax.xml.parsers.DocumentBuilder} based
 * solutions tend to fail, such as
 * <ul>
 * <li>Order of XML declaration attributes</li>
 * <li>Whitespace after the XML declaration</li>
 * <li>Line breaks between element attributes</li>
 * <li>File final whitespace</li>
 * </ul>
 */
public class PomTransformer {

    static final Pattern[] POSTPROCESS_PATTERNS = new Pattern[] {
            Pattern.compile("(<\\?xml[^>]*\\?>)?(\\s*)<"),
            Pattern.compile("(\\s*)<project([^>]*)>")
    };
    static final Pattern EOL_PATTERN = Pattern.compile("\r?\n");
    static final Pattern WS_PATTERN = Pattern.compile("[ \t\n\r]+");
    static final Pattern INDENT_PATTERN = Pattern.compile("(\r?\n)([ \t]+)");
    static final Pattern EMPTY_LINE_PATTERN = Pattern.compile("[ \t]*\r?\n\r?\n[ \t\r\n]*");
    static final Pattern SIMPLE_ELEM_WS_PATTERN = Pattern.compile("<([^ \t\n\r]+)([ \t\n\r]*)/>");
    private static final String MODULE_COMMENT_PREFIX = " <module>";
    private static final String MODULE_COMMENT_INFIX = "</module> ";

    private final Path path;
    private final Charset charset;
    private final SimpleElementWhitespace simpleElementWhitespace;

    public PomTransformer(Path path, Charset charset, SimpleElementWhitespace simpleElementWhitespace) {
        super();
        this.path = path;
        this.charset = charset;
        this.simpleElementWhitespace = simpleElementWhitespace;
    }

    /**
     * Loads the document under {@link #path}, applies the given {@code transformations}, mitigates the formatting
     * issues caused by {@link Transformer} and finally stores the document back to the file under {@link #path}.
     *
     * @param transformations the {@link Transformation}s to apply
     */
    public void transform(Transformation... transformations) {
        transform(Arrays.asList(transformations));
    }

    /**
     * Loads the document under {@link #path}, applies the given {@code transformations}, mitigates the formatting
     * issues caused by {@link Transformer} and finally stores the document back to the file under {@link #path}.
     *
     * @param transformations the {@link Transformation}s to apply
     */
    public void transform(Collection<Transformation> transformations) {
        LazyWriter lazyWriter = new LazyWriter(path, charset);
        transform(transformations, simpleElementWhitespace, path, lazyWriter::read, lazyWriter::write);
    }

    static void transform(
            Collection<Transformation> edits,
            SimpleElementWhitespace simpleElementWhitespace,
            Path path,
            Supplier<String> source,
            Consumer<String> outConsumer) {
        final String src = source.get();

        final Document document;
        try {
            final DOMResult domResult = new DOMResult();
            TransformerFactory.newInstance().newTransformer()
                    .transform(new StreamSource(new StringReader(source.get())), domResult);
            document = (Document) domResult.getNode();
        } catch (TransformerException | TransformerFactoryConfigurationError e) {
            throw new RuntimeException(String.format("Could not read DOM from [%s]", path), e);
        }

        final XPath xPath = XPathFactory.newInstance().newXPath();
        final TransformationContext context = new TransformationContext(path, document,
                detectIndentation(document, xPath), xPath);
        for (Transformation edit : edits) {
            edit.perform(document, context);
        }
        String result;
        try {
            StringWriter out = new StringWriter();
            TransformerFactory.newInstance().newTransformer().transform(new DOMSource(document), new StreamResult(out));
            result = out.toString();

        } catch (TransformerException | TransformerFactoryConfigurationError e) {
            throw new RuntimeException(String.format("Could not write DOM from [%s]", path), e);
        }

        final String eol = detectEol(src);
        result = EOL_PATTERN.matcher(result).replaceAll(eol);
        result = postprocess(src, result, simpleElementWhitespace);
        outConsumer.accept(result);
    }

    static String postprocess(String src, String result, SimpleElementWhitespace simpleElementWhitespace) {
        for (Pattern p : POSTPROCESS_PATTERNS) {
            final Matcher srcMatcher = p.matcher(src);
            if (srcMatcher.find()) {
                final String replacement = Matcher.quoteReplacement(srcMatcher.group());
                result = p.matcher(result).replaceFirst(replacement);
            }
        }
        result = fixTrailingContent(src, result);
        final String ws;
        if (simpleElementWhitespace.autodetect) {
            final Matcher srcMatcher = SIMPLE_ELEM_WS_PATTERN.matcher(src);
            if (srcMatcher.find()) {
                ws = srcMatcher.group(2);
            } else {
                ws = simpleElementWhitespace.value;
            }
        } else {
            ws = simpleElementWhitespace.value;
        }
        if (!ws.isEmpty()) {
            final StringBuffer resultBuffer = new StringBuffer(result.length());
            final Matcher resultMatcher = SIMPLE_ELEM_WS_PATTERN.matcher(result);
            while (resultMatcher.find()) {
                resultMatcher.appendReplacement(resultBuffer, "<$1 />");
            }
            resultMatcher.appendTail(resultBuffer);
            result = resultBuffer.toString();
        }
        return result;
    }

    static String detectIndentation(Node document, XPath xPath) {
        try {
            final String ws = (String) xPath.evaluate(
                    PomTunerUtils.anyNs("project") + "/*[1]/preceding-sibling::text()[last()]",
                    document, XPathConstants.STRING);
            if (ws != null && !ws.isEmpty()) {
                int i = ws.length() - 1;
                LOOP: while (i >= 0) {
                    switch (ws.charAt(i)) {
                    case ' ':
                    case '\t':
                        i--;
                        break;
                    default:
                        break LOOP;
                    }
                }
                return ws.substring(i + 1);
            }
            return "    ";
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    static String detectEol(String src) {
        return src.indexOf('\r') >= 0 ? "\r\n" : "\n";
    }

    static String fixTrailingContent(String src, String result) {
        final int traillingStart = endOfRootElement(src);
        if (traillingStart >= 0) {
            final String trailingContent = src.substring(traillingStart);
            final int traillingStartResult = endOfRootElement(result);
            if (traillingStartResult >= 0) {
                return result.substring(0, traillingStartResult) + trailingContent;
            }
        }
        return result;
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

    /**
     * A preference whether simple elements should be formated with ({@code <foo />} or without ({@code <foo/>}
     * whitespace.
     */
    public enum SimpleElementWhitespace {
        AUTODETECT_PREFER_SPACE(true, " "),
        AUTODETECT_PREFER_EMPTY(true, ""),
        SPACE(false, " "),
        EMPTY(false, "");

        private final boolean autodetect;
        private final String value;

        private SimpleElementWhitespace(boolean autodetect, String value) {
            this.autodetect = autodetect;
            this.value = value;
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

    public static class TextElement {
        protected final TransformationContext context;
        protected final Element node;
        protected final int indentLevel;

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
            Node currentNode = this.node;
            while (true) {
                Node next = currentNode.getPreviousSibling();
                if (next == null) {
                    return null;
                }
                switch (next.getNodeType()) {
                case Node.COMMENT_NODE:
                    return (Comment) next;
                case Node.TEXT_NODE:
                    break;
                default:
                    return null;
                }
                currentNode = next;
            }
        }

        /**
         * @return a DOM {@link Node} to use as a second argument for {@link Node#insertBefore(Node, Node)} when
         *         inserting before {@link #node}; this can be either {@link #node} itself or a comment or an
         *         indentation whitespace preceding {@link #node}
         */
        public Node previousSiblingInsertionRefNode() {
            Node currentNode = this.node;
            while (true) {
                Node next = currentNode.getPreviousSibling();
                if (next == null) {
                    return currentNode;
                }
                switch (next.getNodeType()) {
                case Node.COMMENT_NODE:
                    final Node previousNode = next.getPreviousSibling();
                    if (previousNode != null && previousNode.getNodeType() == Node.ELEMENT_NODE) {
                        /* A comment following an element with no whitespace in between: such comment belongs to the previous element */
                        return currentNode;
                    }
                    break;
                case Node.TEXT_NODE:
                    if (EMPTY_LINE_PATTERN.matcher(next.getTextContent()).matches()) {
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
         * Remove {@link #node} from the underlying DOM, optionally with preceding comments and preceding whitespace
         *
         * @param removePrecedingComments
         * @param removePrecedingWhitespace
         */
        public void remove(boolean removePrecedingComments, boolean removePrecedingWhitespace) {
            if (removePrecedingComments || removePrecedingWhitespace) {
                Node prevSibling = null;
                while ((prevSibling = node.getPreviousSibling()) != null
                        && ((removePrecedingWhitespace && TransformationContext.isWhiteSpaceNode(prevSibling))
                                || (removePrecedingComments && prevSibling.getNodeType() == Node.COMMENT_NODE))) {
                    /* remove any preceding whitespace or comments */
                    prevSibling.getParentNode().removeChild(prevSibling);
                }
            }
            Node parent = node.getParentNode();
            if (parent != null) {
                parent.removeChild(node);
            }
        }

        /**
         * Add a properly indented comment before {@link #node}.
         * Note that this method does not add any whitespace around the specified comment text.
         * So of you want to add {@code <!-- my comment -->}, you need to call this as
         * {@code prependComment(" my comment ")}.
         *
         * @param  comment the text of the comment
         * @return         the newly created {@link Comment} node
         */
        public Comment prependComment(String comment) {
            final Node refNode = previousSiblingInsertionRefNode();
            Comment result = node.getOwnerDocument().createComment(comment);
            node.getParentNode().insertBefore(context.indent(indentLevel), refNode);
            node.getParentNode().insertBefore(result, refNode);
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
            if (precedingComment == null || !comment.equals(precedingComment.getTextContent())) {
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
                Node next = currentNode.getNextSibling();
                if (next == null) {
                    return null;
                }
                switch (next.getNodeType()) {
                case Node.COMMENT_NODE:
                    return (Comment) next;
                case Node.TEXT_NODE:
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
            while ((prevSibling = prevSibling.getPreviousSibling()) != null
                    && precedingInclude.test(prevSibling)) {
                result.add(prevSibling);
            }
            result.add(node);
            return result;
        }

        /**
         * @return the associated DOM {@link Node} together with the preceding whitespace and comments selected by the
         *         given
         *         {@link Predicate}
         */
        public DocumentFragment getFragment(Predicate<Node> precedingInclude) {
            final DocumentFragment result = node.getOwnerDocument().createDocumentFragment();
            for (Node node : getNodes(precedingInclude)) {
                result.appendChild(node);
            }
            return result;
        }

        /**
         * @return the associated DOM {@link Node} together with all preceding whitespace and comments
         */
        public DocumentFragment getFragment() {
            return getFragment(TransformationContext.ALL_WHITESPACE_AND_COMMENTS);
        }

    }

    /**
     * An XML element in a {@code pom.xml} file that possibly has child {@link ContainerElement}s.
     */
    public static class ContainerElement extends TextElement {
        private final Predicate<Node> ELEMENT_FILTER = n -> n.getNodeType() == Node.ELEMENT_NODE;
        protected Text lastIndent;

        public ContainerElement(TransformationContext context, Element node, int indentLevel) {
            super(context, node, indentLevel);
        }

        public ContainerElement(TransformationContext context, Element containerElement, Text lastIndent, int indentLevel) {
            this(context, containerElement, indentLevel);
            this.lastIndent = lastIndent;
        }

        /**
         * @return an {@link Iterable} containing child elements of this {@link ContainerElement}
         */
        public Iterable<ContainerElement> childElements() {
            return () -> new NodeIterator<ContainerElement>(
                    node.getChildNodes(),
                    ELEMENT_FILTER,
                    n -> new ContainerElement(context, (Element) n, indentLevel + 1));
        }

        /**
         * @return a {@link Stream} containing child elements of this {@link ContainerElement}
         */
        public Stream<ContainerElement> childElementsStream() {
            return StreamSupport.stream(childElements().spliterator(), false);
        }

        /**
         * @return an {@link Iterable} containing text child elements of this {@link ContainerElement}
         */
        public Iterable<TextElement> childTextElements() {
            return () -> new NodeIterator<TextElement>(
                    node.getChildNodes(),
                    ELEMENT_FILTER,
                    n -> new TextElement(context, (Element) n, indentLevel + 1));
        }

        /**
         * @return a {@link Stream} containing child text elements of this {@link ContainerElement}
         */
        public Stream<TextElement> childTextElementsStream() {
            return StreamSupport.stream(childTextElements().spliterator(), false);
        }

        /**
         * @return an existing whitespace node preceding the closing tag of {@link #node} or a newly added whitespace
         *         node preceding the closing tag of {@link #node}
         */
        public Node getOrAddLastIndent() {
            if (lastIndent == null) {
                Node ws = node.getLastChild();
                if (ws == null || ws.getNodeType() != Node.TEXT_NODE) {
                    node.appendChild(lastIndent = context.indent(indentLevel));
                } else {
                    lastIndent = (Text) ws;
                }
            }
            return lastIndent;
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
                refNode = getOrAddLastIndent();
            }
            if (emptyLineBefore) {
                /*
                 * Add an empty line between the new node and previousProjectChildEntry
                 */
                node.insertBefore(context.newLine(), refNode);
            }
            final Element result = node.getOwnerDocument().createElement(elementName);
            final Text newLastIndent = context.indent(indentLevel + 1);
            result.appendChild(newLastIndent);
            node.insertBefore(context.indent(indentLevel + 1), refNode);
            node.insertBefore(result, refNode);

            if (emptyLineAfter) {
                /*
                 * Add an empty line between the new node and projectChild
                 * unless there is one already
                 */
                if (refNode.getNodeType() == Node.ELEMENT_NODE) {
                    /* no indentation and no empty line there */
                    node.insertBefore(context.newLine(), refNode);
                    node.insertBefore(context.indent(indentLevel + 1), refNode);
                } else if (!TransformationContext.isEmptyLineNode(refNode)) {
                    node.insertBefore(context.newLine(), refNode);
                }
            }
            return new ContainerElement(context, result, newLastIndent, indentLevel + 1);
        }

        /**
         * @param  elementName the name of an element to search for
         * @return             an Optional containing the first child with the given {@code elementName} or an empty
         *                     {@link Optional} if no such child exists
         */
        public Optional<ContainerElement> getChildContainerElement(String elementName) {
            for (ContainerElement child : childElements()) {
                if (child.node.getNodeName().equals(elementName)) {
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
                if (child.node.getNodeName().equals(elementName)) {
                    /* No need to insert, return existing */
                    return child;
                }
            }

            if (indentLevel == 0) {
                return context.getOrAddContainerElement(elementName);
            }

            final Element result = node.getOwnerDocument().createElement(elementName);
            final Text newLastIndent = context.indent(indentLevel + 1);
            result.appendChild(newLastIndent);
            final Node refNode = getOrAddLastIndent();
            node.insertBefore(context.indent(indentLevel + 1), refNode);
            node.insertBefore(result, refNode);

            return new ContainerElement(
                    context,
                    result,
                    newLastIndent,
                    indentLevel + 1);
        }

        /**
         * An equivalent of {@code addChildTextElement(elementName, text, getOrAddLastIndent())}
         *
         * @param elementName the name of the {@link Element} to add
         * @param text        the text content of the newly added {@link Element}
         */
        public TextElement addChildTextElement(String elementName, final String text) {
            return addChildTextElement(elementName, text, getOrAddLastIndent());
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
                node.insertBefore(context.indent(indentLevel + 1), refNode);
                final Element result1 = context.document.createElement(elementName);
                result1.appendChild(context.document.createTextNode(text));
                final Node result = result1;
                node.insertBefore(result, refNode);
                return new TextElement(context, node, indentLevel + 1);
            }
            return null;
        }

        public TextElement addChildTextElementIfNeeded(String nodeName, String nodeValue,
                Comparator<Entry<String, String>> comparator) {
            Entry<String, String> newEntry = new AbstractMap.SimpleImmutableEntry<>(nodeName, nodeValue);
            Node refNode = null;
            for (TextElement child : childTextElements()) {
                final Element node = child.getNode();
                int comparison = comparator.compare(newEntry, new AbstractMap.SimpleImmutableEntry<>(
                        node.getNodeName(), node.getTextContent()));
                if (comparison == 0) {
                    /* the given child is available, no need to add it */
                    if (!Objects.equals(node.getTextContent(), nodeValue)) {
                        node.setTextContent(nodeValue);
                    }
                    return child;
                }
                if (refNode == null && comparison < 0) {
                    refNode = child.previousSiblingInsertionRefNode();
                }
            }

            if (refNode == null) {
                refNode = getOrAddLastIndent();
            }
            return addChildTextElement(nodeName, nodeValue, refNode);
        }

        /**
         * Add a new {@link Element} under {@link #node} and before {@code refNode} with the given {@code elementName}.
         *
         * @param elementName the name of the {@link Element} to add
         * @param refNode     a {@link Node} before which the new {@link Element} should be added
         */
        public void addChildElement(String elementName, Node refNode) {
            node.insertBefore(context.indent(indentLevel + 1), refNode);
            final Element result = context.document.createElement(elementName);
            node.insertBefore(result, refNode);
        }

        /**
         * Find an {@link Element} under {@link #node} having the given {@code elementName} or create a new one and set
         * the given {@code value} as its text content.
         *
         * @param elementName the name of the {@link Element} to add
         * @param value       the text content of the newly added {@link Element}
         */
        public void addOrSetChildTextElement(String name, String value) {
            Optional<ContainerElement> existingChild = getChildContainerElement(name);
            if (existingChild.isPresent()) {
                existingChild.get().node.setTextContent(value);
            } else {
                addChildTextElement(name, value, getOrAddLastIndent());
            }
        }

        /**
         * An equivalent of {@code addFragment(fragment, getOrAddLastIndent())}.
         *
         * @param fragment the {@link DocumentFragment} to add
         */
        public void addFragment(DocumentFragment fragment) {
            addFragment(fragment, getOrAddLastIndent());
        }

        /**
         * @param fragment the {@link DocumentFragment} to add
         * @param refNode  a {@link Node} before which the {@link DocumentFragment} should be added
         */
        public void addFragment(DocumentFragment fragment, Node refNode) {
            final NodeList children = fragment.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                final Node child = children.item(i);
                final Node imported = node.getOwnerDocument().importNode(child, true);
                node.insertBefore(imported, refNode);
            }
        }

        /**
         * A equivalent of {@code addGavtcs(gavtcs, getOrAddLastIndent())}
         *
         * @param  gavtcs the {@link Gavtcs} to add
         * @return        the newly created child node
         */
        public ContainerElement addGavtcs(Gavtcs gavtcs) {
            return addGavtcs(gavtcs, getOrAddLastIndent());
        }

        /**
         * Add a new {@code <dependency>} node under {@link #node} with {@code <groupId>}, {@code <artifactId>}, etc.
         * set to value taken from the specified {@link Gavtcs}
         *
         * @param  gavtcs  the GAV coordinates to use when creating the new {@code <dependency>}
         * @param  refNode a {@link Node} before which the new {@link Element} should be added
         * @return         the newly created child node
         */
        public ContainerElement addGavtcs(Gavtcs gavtcs, Node refNode) {

            final String parentName = getNode().getNodeName();
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
            return dep;
        }

        /**
         * If not available already, add a new {@code <dependency>} node under {@link #node} with {@code <groupId>},
         * {@code <artifactId>}, etc. set to value taken from the specified {@link Gavtcs}.
         * The availability an dinsertion point is determined using the given {@link Comparator}.
         *
         * @param  gavtcs     the GAV coordinates to use when creating the new {@code <dependency>}
         * @param  comparator for figuring out whether the given {@code gavtcs} is already available under this
         *                    {@link ContainerElement} or for determining the insert position for a newly added child node
         * @return            the newly created child node
         */
        public ContainerElement addGavtcsIfNeeded(Gavtcs gavtcs, Comparator<Gavtcs> comparator) {
            /* Find the insertion position if the gavtcs is not available yet and possibly add it */
            Node refNode = null;
            for (ContainerElement dep : childElements()) {
                final Gavtcs depGavtcs = dep.asGavtcs();
                int comparison = comparator.compare(gavtcs, depGavtcs);
                if (comparison == 0) {
                    /* We have found the item, no need to add it */
                    return dep;
                }
                if (refNode == null && comparison < 0) {
                    refNode = dep.previousSiblingInsertionRefNode();
                }
            }
            if (refNode == null) {
                refNode = getOrAddLastIndent();
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
                switch (depChild.node.getNodeName()) {
                case "groupId":
                    groupId = depChild.node.getTextContent();
                    break;
                case "artifactId":
                    artifactId = depChild.node.getTextContent();
                    break;
                case "version":
                    version = depChild.node.getTextContent();
                    break;
                case "type":
                    type = depChild.node.getTextContent();
                    break;
                case "classifier":
                    classifier = depChild.node.getTextContent();
                    break;
                case "scope":
                    scope = depChild.node.getTextContent();
                    break;
                case "exclusions":
                    exclusions = new ArrayList<>();
                    for (ContainerElement excl : depChild.childElements()) {
                        String exclGroupId = null;
                        String exclArtifactId = null;
                        for (ContainerElement exclChild : excl.childElements()) {
                            switch (exclChild.node.getNodeName()) {
                            case "groupId":
                                exclGroupId = exclChild.node.getTextContent();
                                break;
                            case "artifactId":
                                exclArtifactId = exclChild.node.getTextContent();
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

        /**
         * Assuming the current {@link ContainerElement} is a {@code <dependency>}, {@code <plugin>} or similar, set its
         * {@code <version>} child to the given {@code newVersion} value, adding the {@code <version>} node if necessary
         * or removing it of {@code newVersion} is {@code null}.
         *
         * @param newVersion the version to set or {@code null} if the {@code <version>} node should be removed
         */
        public void setVersion(String newVersion) {
            Optional<ContainerElement> versionNode = childElementsStream()
                    .filter(ch -> "version".equals(ch.getNode().getLocalName()))
                    .findFirst();
            if (!versionNode.isPresent() && newVersion == null) {
                /* nothing to do */
            } else if (!versionNode.isPresent()) {
                addChildTextElement("version", newVersion);
            } else if (newVersion == null) {
                versionNode.get().remove(true, true);
            } else {
                versionNode.get().getNode().setTextContent(newVersion);
            }
        }

        static class NodeIterator<T> implements Iterator<T> {

            private final NodeList nodes;
            private final Predicate<Node> filter;
            private final Function<Node, T> mapper;

            private T current;
            private int currentIndex = -1;
            private boolean moveCurrent = true;

            public NodeIterator(NodeList nodes, Predicate<Node> filter, Function<Node, T> mapper) {
                this.nodes = nodes;
                this.filter = filter;
                this.mapper = mapper;
            }

            @Override
            public boolean hasNext() {
                if (moveCurrent) {
                    current = moveCurrent();
                    moveCurrent = false;
                }
                return current != null;
            }

            @Override
            public T next() {
                if (moveCurrent) {
                    current = moveCurrent();
                }
                if (current == null) {
                    throw new ArrayIndexOutOfBoundsException();
                }
                moveCurrent = true;
                return current;
            }

            private T moveCurrent() {
                while (true) {
                    currentIndex++;
                    if (currentIndex >= nodes.getLength()) {
                        return null;
                    }
                    final Node node = nodes.item(currentIndex);
                    if (filter.test(node)) {
                        return mapper.apply(node);
                    }
                }
            }

            @Override
            public void remove() {
                if (current == null) {
                    throw new IllegalStateException("current must be set first");
                }
                final Node node = nodes.item(currentIndex);
                node.getParentNode().removeChild(node);
                currentIndex--;
            }

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
        public static final Predicate<Node> ALL_WHITESPACE_AND_COMMENTS = prevSibling -> TransformationContext
                .isWhiteSpaceNode(prevSibling)
                || prevSibling.getNodeType() == Node.COMMENT_NODE;

        private final Path pomXmlPath;
        private final Document document;
        private final ContainerElement project;
        private final XPath xPath;
        private final String indentationString;
        private static volatile Map<String, ElementOrderEntry> elementOrdering;
        private static final Object elementOrderingLock = new Object();

        TransformationContext(Path pomXmlPath, Document document, String indentationString, XPath xPath) {
            super();
            this.pomXmlPath = pomXmlPath;
            this.document = document;
            this.indentationString = indentationString;
            this.xPath = xPath;
            this.project = new ContainerElement(this, document.getDocumentElement(), null, 0);
        }

        /**
         * @return the path to the {@code pom.xml} file that is being transformed
         */
        public Path getPomXmlPath() {
            return pomXmlPath;
        }

        /**
         * @return an {@link XPath} instance that can be used for querying the DOM of the transformed {@code pom.xml}
         *         file
         */
        public XPath getXPath() {
            return xPath;
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
        public Text indent(int indentCount) {
            final StringBuilder sb = new StringBuilder(1 + indentCount * indentationString.length());
            sb.append('\n');
            for (int i = 0; i < indentCount; i++) {
                sb.append(indentationString);
            }
            return document.createTextNode(sb.toString());
        }

        /**
         * @return a newly created text node having single newline {@code \n} as its content.
         */
        public Text newLine() {
            return document.createTextNode("\n");
        }

        public ContainerElement getOrAddContainerElements(String elementName, String... furtherNames) {
            ContainerElement parent = getOrAddContainerElement(elementName);
            for (int i = 0; i < furtherNames.length; i++) {
                parent = parent.getOrAddChildContainerElement(furtherNames[i]);
            }
            return parent;
        }

        public ContainerElement getOrAddContainerElement(String elementName) {
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
            for (ContainerElement projectChild : project.childElements()) {
                final String projectChildName = projectChild.node.getNodeName();
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
            return project.addChildContainerElement(
                    elementName,
                    refNode,
                    emptyLineBefore,
                    emptyLineAfter);
        }

        public static boolean isEmptyLineNode(Node node) {
            return node.getNodeType() == Node.TEXT_NODE
                    && node.getTextContent() != null
                    && EMPTY_LINE_PATTERN.matcher(node.getTextContent()).matches();
        }

        public static boolean isWhiteSpaceNode(Node node) {
            return node.getNodeType() == Node.TEXT_NODE
                    && node.getTextContent() != null
                    && WS_PATTERN.matcher(node.getTextContent()).matches();
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
                        Map<String, ElementOrderEntry> m = new TreeMap<String, PomTransformer.TransformationContext.ElementOrderEntry>();
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

        public Optional<ContainerElement> getProfile(String profileId) {
            try {
                final Node node = (Node) xPath.evaluate(
                        PomTunerUtils.anyNs("project", "profiles", "profile") + "[." + PomTunerUtils.anyNs("id") + "/text() = '"
                                + profileId + "']",
                        document, XPathConstants.NODE);
                if (node != null) {
                    return Optional.of(new ContainerElement(this, (Element) node, null, 2));
                }
                return Optional.empty();
            } catch (XPathExpressionException e) {
                throw new RuntimeException(e);
            }
        }

        public ContainerElement getOrAddProfile(String profileId) {
            try {
                final Node node = (Node) xPath.evaluate(
                        PomTunerUtils.anyNs("project", "profiles", "profile") + "[." + PomTunerUtils.anyNs("id") + "/text() = '"
                                + profileId + "']",
                        document, XPathConstants.NODE);
                if (node != null) {
                    return new ContainerElement(this, (Element) node, null, 2);
                } else {
                    ContainerElement profile = getOrAddContainerElement("profiles").addChildContainerElement("profile");
                    profile.addChildTextElement("id", profileId);
                    return profile;
                }
            } catch (XPathExpressionException e) {
                throw new RuntimeException(e);
            }
        }

        public Optional<ContainerElement> getProfileParent(String profileId) {
            if (profileId == null) {
                final Node node = document.getDocumentElement();
                if (node != null) {
                    return Optional.of(new ContainerElement(this, (Element) node, null, 0));
                }
                return Optional.empty();
            } else {
                return getProfile(profileId);
            }
        }

        public ContainerElement getOrAddProfileParent(String profileId) {
            if (profileId == null) {
                final Node node = document.getDocumentElement();
                if (node == null) {
                    throw new IllegalStateException("No document element in " + pomXmlPath);
                }
                return new ContainerElement(this, (Element) node, null, 0);
            } else {
                return getOrAddProfile(profileId);
            }
        }

        public Optional<ContainerElement> getContainerElement(String... path) {
            try {
                final Node node = (Node) xPath.evaluate(PomTunerUtils.anyNs(path), document, XPathConstants.NODE);
                if (node != null) {
                    return Optional.of(new ContainerElement(this, (Element) node, null, path.length - 1));
                }
                return Optional.empty();
            } catch (XPathExpressionException e) {
                throw new RuntimeException(e);
            }
        }

        public Set<NodeGavtcs> getDependencies() {
            return getContainerElement("project", "dependencies")
                    .map(deps -> deps.childElementsStream()
                            .map(dep -> dep.asGavtcs())
                            .collect(Collectors.toCollection(() -> (Set<NodeGavtcs>) new LinkedHashSet<NodeGavtcs>())))
                    .orElse(Collections.emptySet());
        }

        public Set<NodeGavtcs> getManagedDependencies() {
            return getContainerElement("project", "dependencyManagement", "dependencies")
                    .map(deps -> deps.childElementsStream()
                            .map(dep -> dep.asGavtcs())
                            .collect(Collectors.toCollection(() -> (Set<NodeGavtcs>) new LinkedHashSet<NodeGavtcs>())))
                    .orElse(Collections.emptySet());
        }

        public Optional<ContainerElement> findDependency(Gavtcs gavtcs) {
            return getContainerElement("project", "dependencies")
                    .map(depsNode -> depsNode.childElementsStream()
                            .filter(depNode -> depNode.asGavtcs().equals(gavtcs))
                            .findFirst()
                            .orElse(null));
        }

        public void removeDependency(Gavtcs removedDependency, boolean removePrecedingComments,
                boolean removePrecedingWhitespace) {
            getContainerElement("project", "dependencies")
                    .ifPresent(deps -> deps.childElementsStream()
                            .filter(wrappedDepNode -> wrappedDepNode.asGavtcs().equals(removedDependency))
                            .findFirst()
                            .ifPresent(wrappedDepNode -> wrappedDepNode.remove(removePrecedingComments,
                                    removePrecedingWhitespace)));
        }

        public void removeManagedDependency(Gavtcs removedDependency, boolean removePrecedingComments,
                boolean removePrecedingWhitespace) {
            getContainerElement("project", "dependencyManagement", "dependencies")
                    .ifPresent(deps -> deps.childElementsStream()
                            .filter(wrappedDepNode -> wrappedDepNode.asGavtcs().equals(removedDependency))
                            .findFirst()
                            .ifPresent(wrappedDepNode -> wrappedDepNode.remove(removePrecedingComments,
                                    removePrecedingWhitespace)));
        }

        public void addDependencyIfNeeded(Gavtcs gavtcs, Comparator<Gavtcs> comparator) {
            getOrAddContainerElement("dependencies").addGavtcsIfNeeded(gavtcs, comparator);
        }

        public void addTextChildIfNeeded(ContainerElement parent, String nodeName, String nodeValue,
                Comparator<String> comparator) {
            parent.addChildTextElementIfNeeded(nodeName, nodeValue,
                    (en1, en2) -> comparator.compare(en1.getValue(), en2.getValue()));
        }

        public void removeNode(String xPathExpression, boolean removePrecedingComments, boolean removePrecedingWhitespace,
                boolean onlyIfEmpty) {
            BiConsumer<Node, Node> precedingNodesConsumer = null;
            if (removePrecedingComments || removePrecedingWhitespace) {
                precedingNodesConsumer = (Node deletedNode, Node whitespaceOrComment) -> {
                    if ((removePrecedingWhitespace && TransformationContext.isWhiteSpaceNode(whitespaceOrComment))
                            || (removePrecedingComments && whitespaceOrComment.getNodeType() == Node.COMMENT_NODE)) {
                        /* remove any preceding whitespace or comments */
                        whitespaceOrComment.getParentNode().removeChild(whitespaceOrComment);
                    }
                };
            }
            removeNode(xPathExpression, precedingNodesConsumer, onlyIfEmpty);
        }

        public void removeNode(String xPathExpression, BiConsumer<Node, Node> precedingNodesConsumer,
                boolean onlyIfEmpty) {
            try {
                Node deletedNode = (Node) getXPath().evaluate(xPathExpression, document, XPathConstants.NODE);
                if (deletedNode != null) {
                    if (onlyIfEmpty && hasElementChildren(deletedNode)) {
                        return;
                    }
                    if (precedingNodesConsumer != null) {
                        Node prevSibling = deletedNode.getPreviousSibling();
                        while (prevSibling != null
                                && (TransformationContext.isWhiteSpaceNode(prevSibling)
                                        || prevSibling.getNodeType() == Node.COMMENT_NODE)) {
                            /* remove any preceding whitespace or comments */
                            precedingNodesConsumer.accept(deletedNode, prevSibling);
                            final Node newPrevSibling = deletedNode.getPreviousSibling();
                            if (prevSibling == newPrevSibling) {
                                break;
                            }
                            prevSibling = newPrevSibling;
                        }
                    }
                    deletedNode.getParentNode().removeChild(deletedNode);
                }
            } catch (XPathExpressionException | DOMException e) {
                throw new RuntimeException(e);
            }
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
            switch (node.getNodeType()) {
            case Node.TEXT_NODE:
                final String oldValue = node.getNodeValue();
                final String newValue = INDENT_PATTERN.matcher(oldValue).replaceAll("$1" + newIndent);
                if (!oldValue.equals(newValue)) {
                    node.setNodeValue(newValue);
                }
                break;
            case Node.ELEMENT_NODE:
                NodeList children = node.getChildNodes();
                String passIndent = newIndent + indentationString;
                final int nodeCount = children.getLength();
                for (int i = 0; i < nodeCount; i++) {
                    final Node child = children.item(i);
                    if (i + 1 == nodeCount && child.getNodeType() == Node.TEXT_NODE) {
                        /* the last indent before the closing element */
                        reIndent(child, newIndent);
                    } else {
                        reIndent(child, passIndent);
                    }
                }
                break;
            default:
                break;
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
         * Calls {@link #reIndent(Node, String)} for each child of the given {@code fragment}.
         *
         * @param fragment  the nodes whose indentation should be reset
         * @param newIndent the new indentation string
         */
        public void reIndent(DocumentFragment fragment, String newIndent) {
            final NodeList children = fragment.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                reIndent(children.item(i), newIndent);
            }
        }

        public static boolean hasElementChildren(Node node) {
            final NodeList children = node.getChildNodes();
            if (children.getLength() == 0) {
                return false;
            }
            for (int i = 0; i < children.getLength(); i++) {
                final Node child = children.item(i);
                if (child.getNodeType() != Node.COMMENT_NODE && !isWhiteSpaceNode(child)) {
                    return true;
                }
            }
            return false;
        }

        public static Comment commentTextNode(Node node, String commentText) {
            final String moduleText = node.getTextContent();
            final Node parent = node.getParentNode();
            final Comment moduleComment = node.getOwnerDocument()
                    .createComment(MODULE_COMMENT_PREFIX + moduleText + MODULE_COMMENT_INFIX + commentText + " ");
            parent.replaceChild(moduleComment, node);
            return moduleComment;
        }

        /**
         * Remove {@link Gavtcs}-like nodes (such as {@code <dependency>}, {@code <plugin>}, etc.) matching the given
         * {@code predicate} from the given profile.
         *
         * @param profileId                 the {@code id} of the profile under which the changes should happen or {@code null}
         *                                  if the
         *                                  changes should happen in the default profile-less area
         * @param removePrecedingComments   if {@code true} the comments preceding the removed nodes will be also removed;
         *                                  otherwise the preceding comments won't be removed
         * @param removePrecedingWhitespace if {@code true} the whitespace nodes preceding the removed nodes will be
         *                                  also be removed; otherwise the preceding whitespace nodes won't be removed
         * @param predicate                 the predicate to select the nodes to remove, such as
         *                                  {@link Gavtcs#equalGroupIdAndArtifactId(String, String)}
         * @param nodeName                  the first node name of the path to remove
         * @param otherNodeNames            other optional node names to remove
         */
        void removeGavtcs(String profileId, boolean removePrecedingComments, boolean removePrecedingWhitespace,
                Predicate<Gavtcs> predicate, String nodeName, String... otherNodeNames) {
            getProfileParent(profileId).ifPresent(profileParent -> {
                profileParent
                        .getChildContainerElement(nodeName, otherNodeNames)
                        .ifPresent(gavtcsNode -> {
                            List<NodeGavtcs> deletionList = gavtcsNode.childElementsStream()
                                    .map(dep -> dep.asGavtcs())
                                    .filter(predicate)
                                    .collect(Collectors.toList());

                            deletionList
                                    .forEach(dep -> dep.getNode().remove(removePrecedingComments, removePrecedingWhitespace));
                        });
            });

        }
    }

    /**
     * A transformation of a DOM
     */
    public interface Transformation {

        public static Transformation addModule(String module) {
            return addModules(null, Collections.singleton(module));
        }

        public static Transformation addModules(String profileId, String... modulePaths) {
            return addModules(profileId, Arrays.asList(modulePaths));
        }

        public static Transformation addModuleIfNeeded(String module, Comparator<String> comparator) {
            return (Document document, TransformationContext context) -> {
                ContainerElement modules = context.getOrAddContainerElement("modules");
                context.addTextChildIfNeeded(modules, "module", module, comparator);
            };
        }

        public static Transformation addModules(String profileId, Collection<String> modulePaths) {
            return addModulesIfNeeded(profileId, null, modulePaths);
        }

        public static Transformation addModulesIfNeeded(String profileId, Comparator<String> comparator,
                Collection<String> modulePaths) {
            return (Document document, TransformationContext context) -> {
                final ContainerElement profileParent = context.getOrAddProfileParent(profileId);
                final ContainerElement modules = profileParent.getOrAddChildContainerElement("modules");
                for (String m : modulePaths) {
                    if (comparator != null) {
                        context.addTextChildIfNeeded(modules, "module", m, comparator);
                    } else {
                        modules.addChildTextElement("module", m);
                    }
                }
            };
        }

        public static Transformation addProperty(String name, String value) {
            return (Document document, TransformationContext context) -> {
                final ContainerElement props = context.getOrAddContainerElement("properties");
                props.addChildTextElement(name, value);
            };
        }

        public static Transformation addOrSetProperty(String name, String value) {
            return (Document document, TransformationContext context) -> {
                final ContainerElement props = context.getOrAddContainerElement("properties");
                props.addOrSetChildTextElement(name, value);
            };
        }

        public static Transformation addContainerElementsIfNeeded(String elementName, String... furtherNames) {
            return (Document document, TransformationContext context) -> context.getOrAddContainerElements(elementName,
                    furtherNames);
        }

        public static Transformation addManagedDependency(String groupId, String artifactId, String version) {
            return addManagedDependency(new Gavtcs(groupId, artifactId, version, null, null, null));
        }

        public static Transformation addManagedDependency(Gavtcs gavtcs) {
            return (Document document, TransformationContext context) -> {
                final ContainerElement dependencyManagementDeps = context.getOrAddContainerElements("dependencyManagement",
                        "dependencies");
                dependencyManagementDeps.addGavtcs(gavtcs);
            };
        }

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

                for (String m : modulesToComment) {
                    final String xPathExpr = PomTunerUtils.anyNs("project", "modules", "module") + "[text() = '" + m + "'"
                            + "]";
                    try {
                        final NodeList moduleNodes = (NodeList) context.getXPath().evaluate(xPathExpr, document,
                                XPathConstants.NODESET);
                        for (int i = 0; i < moduleNodes.getLength(); i++) {
                            TransformationContext.commentTextNode(moduleNodes.item(i), commentText);
                        }
                    } catch (XPathExpressionException | DOMException e) {
                        throw new RuntimeException(e);
                    }
                }
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

                for (ContainerElement dep : dependencyManagementDeps.childElements()) {
                    final Ga ga = dep.asGavtcs().toGa();
                    if (gas.contains(ga)) {
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

                for (ContainerElement dep : deps.childElements()) {
                    final Ga ga = dep.asGavtcs().toGa();
                    if (gas.contains(ga)) {
                        dep.setVersion(newVersion);
                    }
                }
            };
        }

        public static Transformation addDependencyIfNeeded(Gavtcs gavtcs, Comparator<Gavtcs> comparator) {
            return (Document document, TransformationContext context) -> context.addDependencyIfNeeded(gavtcs, comparator);
        }

        public static Transformation updateMappedDependencies(
                Predicate<Gavtcs> isSubsetMember,
                Function<Gavtcs, Optional<Gavtcs>> dependencyMapper,
                Comparator<Gavtcs> comparator,
                String initialComment) {
            return (Document document, TransformationContext context) -> {
                final Set<? extends Gavtcs> deps = context.getDependencies();
                final Set<Gavtcs> newMappedDeps = new TreeSet<>(comparator);
                for (Gavtcs dep : deps) {
                    dependencyMapper
                            .apply(dep)
                            .ifPresent(mappedDep -> {
                                newMappedDeps.add(mappedDep);
                                if (!deps.contains(mappedDep)) {
                                    context.addDependencyIfNeeded(mappedDep, comparator);
                                }
                            });
                }

                /* Remove stale mapped deps */
                deps.stream()
                        .filter(isSubsetMember)
                        .filter(dep -> !newMappedDeps.contains(dep))
                        .forEach(dep -> context.removeDependency(dep, true, true));

                if (initialComment != null && !newMappedDeps.isEmpty()) {
                    final Gavtcs firstMappedNode = newMappedDeps.iterator().next();
                    context.findDependency(firstMappedNode)
                            .ifPresent(firstDepNode -> firstDepNode.prependCommentIfNeeded(initialComment));
                }

            };
        }

        public static Transformation updateDependencySubset(
                Predicate<Gavtcs> isSubsetMember,
                Collection<Gavtcs> newSubset,
                Comparator<Gavtcs> comparator,
                String initialComment) {
            return (Document document, TransformationContext context) -> {
                Set<Gavtcs> depsToAdd = new TreeSet<>(comparator);
                depsToAdd.addAll(newSubset);

                final Gavtcs firstSubsetNode = depsToAdd.isEmpty() ? null : depsToAdd.iterator().next();

                Set<? extends Gavtcs> deps = context.getDependencies();
                for (Gavtcs dep : deps) {
                    if (isSubsetMember.test(dep)) {
                        if (!newSubset.contains(dep)) {
                            context.removeDependency(dep, true, true);
                        } else {
                            depsToAdd.remove(dep);
                        }
                    }
                }
                for (Gavtcs dep : depsToAdd) {
                    context.addDependencyIfNeeded(dep, comparator);
                }

                if (initialComment != null && firstSubsetNode != null) {
                    context.findDependency(firstSubsetNode)
                            .ifPresent(firstDepNode -> firstDepNode.prependCommentIfNeeded(initialComment));
                }

            };
        }

        public static Transformation removeContainerElementIfEmpty(boolean removePrecedingComments,
                boolean removePrecedingWhitespace, boolean onlyIfEmpty, String elementName, String... furtherNames) {
            return (Document document, TransformationContext context) -> {
                final String[] path = new String[furtherNames.length + 2];
                int i = 0;
                path[i++] = "project";
                path[i++] = elementName;
                for (String n : furtherNames) {
                    path[i++] = n;
                }
                final String xPath = PomTunerUtils.anyNs(path);
                context.removeNode(xPath, removePrecedingComments, removePrecedingWhitespace, onlyIfEmpty);
            };
        }

        public static Transformation removeModule(boolean removePrecedingComments, boolean removePrecedingWhitespace,
                String module) {
            return (Document document, TransformationContext context) -> {
                final String xPath = PomTunerUtils.anyNs("project", "modules", "module") + "[text() = '" + module + "']";
                context.removeNode(xPath, removePrecedingComments, removePrecedingWhitespace, false);
            };
        }

        public static Transformation removeModules(boolean removePrecedingComments, boolean removePrecedingWhitespace,
                Set<String> modules) {
            return (Document document, TransformationContext context) -> {
                for (String module : modules) {
                    final String xPath = PomTunerUtils.anyNs("project", "modules", "module") + "[text() = '" + module + "']";
                    context.removeNode(xPath, removePrecedingComments, removePrecedingWhitespace, false);
                }
            };
        }

        public static Transformation removeAllModules(String profileId, boolean removePrecedingComments,
                boolean removePrecedingWhitespace) {
            return (Document document, TransformationContext context) -> {
                context.getProfileParent(profileId)
                        .flatMap(profileParent -> profileParent.getChildContainerElement("modules"))
                        .ifPresent(modules -> {
                            Iterator<ContainerElement> children = modules.childElements().iterator();
                            while (children.hasNext()) {
                                children.next().remove(removePrecedingComments, removePrecedingWhitespace);
                                children = modules.childElements().iterator();
                            }
                        });
                ;
            };
        }

        public static Transformation uncommentModules(String commentText) {
            return uncommentModules(commentText, m -> true);
        }

        public static Transformation uncommentModules(String commentText, Predicate<String> modulePathFilter) {
            return (Document document, TransformationContext context) -> {
                final String xPathExpr = PomTunerUtils.anyNs("project", "modules") + "/comment()[starts-with(., '"
                        + MODULE_COMMENT_PREFIX
                        + "') and substring(., string-length(.) - "
                        + (MODULE_COMMENT_INFIX.length() + commentText.length()) + ")  = '" + MODULE_COMMENT_INFIX
                        + commentText + " ']";
                try {
                    final NodeList commentNodes = (NodeList) context.getXPath().evaluate(xPathExpr, document,
                            XPathConstants.NODESET);
                    for (int i = 0; i < commentNodes.getLength(); i++) {
                        final Node commentNode = commentNodes.item(i);
                        final String wholeText = commentNode.getTextContent();
                        final String modulePath = wholeText.substring(MODULE_COMMENT_PREFIX.length(),
                                wholeText.length() - MODULE_COMMENT_INFIX.length() - commentText.length() - 1);
                        if (modulePathFilter.test(modulePath)) {
                            final Node parent = commentNode.getParentNode();
                            final Element newModuleNode = context.document.createElement("module");
                            newModuleNode.appendChild(context.document.createTextNode(modulePath));
                            parent.replaceChild(newModuleNode, commentNode);
                        }
                    }
                } catch (XPathExpressionException e) {
                    throw new RuntimeException("Could not evaluate '" + xPathExpr + "'", e);
                } catch (DOMException e) {
                    throw new RuntimeException(e);
                }
            };
        }

        public static Transformation removeProperty(boolean removePrecedingComments, boolean removePrecedingWhitespace,
                String propertyName) {
            return (Document document, TransformationContext context) -> {
                final String xPath = PomTunerUtils.anyNs("project", "properties", propertyName);
                context.removeNode(xPath, removePrecedingComments, removePrecedingWhitespace, false);
            };
        }

        /**
         * Remove plugins having the given {@code groupId} and {@code artifactId} from the default profile-less area of
         * the {@code pom.xml} file.
         *
         * @param      removePrecedingComments   if {@code true} the comments preceding the removed nodes will be also removed;
         *                                       otherwise the preceding comments won't be removed
         * @param      removePrecedingWhitespace if {@code true} the whitespace nodes preceding the removed nodes will be
         *                                       also be removed; otherwise the preceding whitespace nodes won't be removed
         * @param      predicate                 the predicate to select the nodes to remove, such as
         *                                       {@link Gavtcs#equalGroupIdAndArtifactId(String, String)}
         * @param      groupId                   the {@code groupId} of the plugins to remove
         * @param      artifactId                the {@code artifactId} of the plugins to remove
         * @return                               a new {@link Transformation}
         *
         * @deprecated                           use
         *                                       {@code removePlugins(null, removePrecedingComments, removePrecedingWhitespace, Gavtcs.equalGroupIdAndArtifactId(groupId, artifactId))}
         */
        public static Transformation removePlugin(boolean removePrecedingComments, boolean removePrecedingWhitespace,
                String groupId, String artifactId) {
            return removePlugins(null, removePrecedingComments, removePrecedingWhitespace,
                    Gavtcs.equalGroupIdAndArtifactId(groupId, artifactId));
        }

        /**
         * Remove plugins matching the given {@code predicate} from the given profile.
         *
         * @param  profileId                 the {@code id} of the profile under which the changes should happen or {@code null}
         *                                   if the
         *                                   changes should happen in the default profile-less area
         * @param  removePrecedingComments   if {@code true} the comments preceding the removed nodes will be also removed;
         *                                   otherwise the preceding comments won't be removed
         * @param  removePrecedingWhitespace if {@code true} the whitespace nodes preceding the removed nodes will be
         *                                   also be removed; otherwise the preceding whitespace nodes won't be removed
         * @param  predicate                 the predicate to select the nodes to remove, such as
         *                                   {@link Gavtcs#equalGroupIdAndArtifactId(String, String)}
         * @return                           a new {@link Transformation}
         */
        public static Transformation removePlugins(String profileId, boolean removePrecedingComments,
                boolean removePrecedingWhitespace,
                Predicate<Gavtcs> predicate) {
            return (Document document, TransformationContext context) -> {
                context.removeGavtcs(profileId, removePrecedingComments, removePrecedingWhitespace, predicate, "build",
                        "plugins");
            };
        }

        /**
         * Remove dependencies matching the given {@code predicate} from the default profile-less area of the
         * {@code pom.xml} file.
         *
         * @param      removePrecedingComments   if {@code true} the comments preceding the removed nodes will be also removed;
         *                                       otherwise the preceding comments won't be removed
         * @param      removePrecedingWhitespace if {@code true} the whitespace nodes preceding the removed nodes will be
         *                                       also be removed; otherwise the preceding whitespace nodes won't be removed
         * @param      predicate                 the predicate to select the nodes to remove, such as
         *                                       {@link Gavtcs#equalGroupIdAndArtifactId(String, String)}
         * @return                               a new {@link Transformation}
         *
         * @deprecated                           use {@link #removeDependencies(String, boolean, boolean, Predicate)}
         */
        public static Transformation removeDependency(boolean removePrecedingComments, boolean removePrecedingWhitespace,
                Predicate<Gavtcs> predicate) {
            return removeDependencies(null, removePrecedingComments, removePrecedingWhitespace, predicate);
        }

        /**
         * Remove dependencies matching the given {@code predicate} from the given profile.
         *
         * @param  profileId                 the {@code id} of the profile under which the changes should happen or {@code null}
         *                                   if the
         *                                   changes should happen in the default profile-less area
         * @param  removePrecedingComments   if {@code true} the comments preceding the removed nodes will be also removed;
         *                                   otherwise the preceding comments won't be removed
         * @param  removePrecedingWhitespace if {@code true} the whitespace nodes preceding the removed nodes will be
         *                                   also be removed; otherwise the preceding whitespace nodes won't be removed
         * @param  predicate                 the predicate to select the nodes to remove, such as
         *                                   {@link Gavtcs#equalGroupIdAndArtifactId(String, String)}
         * @return                           a new {@link Transformation}
         */
        public static Transformation removeDependencies(String profileId, boolean removePrecedingComments,
                boolean removePrecedingWhitespace,
                Predicate<Gavtcs> predicate) {
            return (Document document, TransformationContext context) -> {
                context.removeGavtcs(profileId, removePrecedingComments, removePrecedingWhitespace, predicate, "dependencies");
            };
        }

        /**
         * Remove managed dependencies matching the given {@code predicate} from the default profile-less area of the
         * {@code pom.xml} file.
         *
         * @param      removePrecedingComments   if {@code true} the comments preceding the removed nodes will be also removed;
         *                                       otherwise the preceding comments won't be removed
         * @param      removePrecedingWhitespace if {@code true} the whitespace nodes preceding the removed nodes will be
         *                                       also be removed; otherwise the preceding whitespace nodes won't be removed
         * @param      predicate                 the predicate to select the nodes to remove, such as
         *                                       {@link Gavtcs#equalGroupIdAndArtifactId(String, String)}
         * @return                               a new {@link Transformation}
         *
         * @deprecated                           use {@link #removeManagedDependencies(String, boolean, boolean, Predicate)}
         */
        public static Transformation removeManagedDependencies(boolean removePrecedingComments,
                boolean removePrecedingWhitespace,
                Predicate<Gavtcs> predicate) {
            return removeManagedDependencies(null, removePrecedingComments, removePrecedingWhitespace, predicate);
        }

        /**
         * Remove managed dependencies matching the given {@code predicate} from the given profile.
         *
         * @param  profileId                 the {@code id} of the profile under which the changes should happen or {@code null}
         *                                   if the
         *                                   changes should happen in the default profile-less area
         * @param  removePrecedingComments   if {@code true} the comments preceding the removed nodes will be also removed;
         *                                   otherwise the preceding comments won't be removed
         * @param  removePrecedingWhitespace if {@code true} the whitespace nodes preceding the removed nodes will be
         *                                   also be removed; otherwise the preceding whitespace nodes won't be removed
         * @param  predicate                 the predicate to select the nodes to remove, such as
         *                                   {@link Gavtcs#equalGroupIdAndArtifactId(String, String)}
         * @return                           a new {@link Transformation}
         */
        public static Transformation removeManagedDependencies(String profileId, boolean removePrecedingComments,
                boolean removePrecedingWhitespace,
                Predicate<Gavtcs> predicate) {
            return (Document document, TransformationContext context) -> {
                context.removeGavtcs(profileId, removePrecedingComments, removePrecedingWhitespace, predicate,
                        "dependencyManagement", "dependencies");
            };
        }

        /**
         * Set the {@code artifactId} and {@code relativePath} in the {@code parent} element. {@code artifactId} cannot
         * be {@code null}. If {@code relativePath} is {@code null}, the {@code relativePath} element will be removed.
         *
         * @param  artifactId   the {@code artifactId} for the {@code parent} element
         * @param  relativePath a value for the {@code relativePath} element or {@code null} if it should be removed
         * @return              a new {@link Transformation}
         */
        public static Transformation setParent(String artifactId, String relativePath) {
            return (Document document, TransformationContext context) -> {
                ContainerElement parent = context.getContainerElement("project", "parent")
                        .orElseThrow(() -> new IllegalStateException("No parent element in " + context.getPomXmlPath()));
                parent.addOrSetChildTextElement("artifactId", artifactId);
                if (relativePath == null) {
                    parent.getChildContainerElement("relativePath").ifPresent(relPath -> relPath.remove(true, true));
                } else {
                    parent.addOrSetChildTextElement("relativePath", relativePath);
                }
            };
        }

        public static Transformation setTextValue(String selector, String newValue) {
            return new SetTextValueTransformation(selector, newValue);
        }

        public static Transformation addManagedPlugin(String groupId, String artifactId, String version) {
            return (Document document, TransformationContext context) -> {
                final ContainerElement managedPlugins = context.getOrAddContainerElements("build", "pluginManagement",
                        "plugins");
                final ContainerElement pluginElement = managedPlugins.addChildContainerElement("plugin");
                pluginElement.addChildTextElement("groupId", groupId);
                pluginElement.addChildTextElement("artifactId", artifactId);
                pluginElement.addChildTextElement("version", version);
            };
        }

        public static Transformation addFragment(DocumentFragment fragment, String parentPathFirst, String... parentPathOther) {
            return (Document document, TransformationContext context) -> {
                final ContainerElement parent = context.getOrAddContainerElements(parentPathFirst, parentPathOther);
                parent.addFragment(fragment);
            };
        }

        public static Transformation keepFirst(String xPath, boolean removePrecedingWhitespace) {
            return (Document document, TransformationContext context) -> {
                try {
                    NodeList nodes = (NodeList) context.getXPath().evaluate(xPath, document, XPathConstants.NODESET);
                    if (nodes.getLength() > 1) {
                        for (int i = 1; i < nodes.getLength(); i++) {
                            Node deletedNode = nodes.item(i);
                            if (removePrecedingWhitespace) {
                                Node prevSibling = null;
                                while ((prevSibling = deletedNode.getPreviousSibling()) != null
                                        && TransformationContext.isWhiteSpaceNode(prevSibling)) {
                                    /* remove any preceding whitespace or comments */
                                    prevSibling.getParentNode().removeChild(prevSibling);
                                }
                            }
                            deletedNode.getParentNode().removeChild(deletedNode);
                        }
                    }
                } catch (XPathExpressionException | DOMException e) {
                    throw new RuntimeException("Could not evaluate " + xPath, e);
                }
            };
        }

        /**
         * Remove the element specified by the given {@code path} if it has no child elements (child whitespace and
         * comments do
         * not matter).
         * The {@code path} is vararg of element names, e.g. {@code removeIfEmpty(true, true, "project", "properties")}
         * would remove the {@code <properties>} element if there are no properties defined under it.
         *
         * @param  removePrecedingComments
         * @param  removePrecedingWhitespace
         * @param  path                      a vararg of element names
         * @return                           a new {@link Transformation}
         */
        public static Transformation removeIfEmpty(boolean removePrecedingComments, boolean removePrecedingWhitespace,
                String... path) {
            return (Document document, TransformationContext context) -> {
                context.getContainerElement(path).ifPresent(element -> {
                    if (!element.childElements().iterator().hasNext()) {
                        element.remove(removePrecedingComments, removePrecedingWhitespace);
                    }
                });
            };
        }

        /**
         * Perform this {@link Transformation} on the given {@code document}
         *
         * @param document the {@link Document} to transform
         * @param context  the current {@link TransformationContext}
         */
        void perform(Document document, TransformationContext context);

        public static class SetTextValueTransformation implements Transformation {

            private final String selector;
            private final String newValue;

            public SetTextValueTransformation(String selector, String newValue) {
                this.selector = selector;
                this.newValue = newValue;
            }

            @Override
            public void perform(Document document, TransformationContext context) {
                try {
                    final NodeList nodes = (NodeList) context.getXPath().evaluate(selector, document, XPathConstants.NODESET);
                    if (nodes == null || nodes.getLength() == 0) {
                        throw new IllegalStateException(
                                String.format("Xpath expression [%s] did not select any nodes in [%s]", selector,
                                        context.getPomXmlPath()));
                    }
                    for (int i = 0; i < nodes.getLength(); i++) {
                        nodes.item(i).setTextContent(newValue);
                    }
                } catch (XPathExpressionException | DOMException e) {
                    throw new IllegalStateException(
                            String.format("Could not evaluate [%s] on [%s]", selector, context.getPomXmlPath()));
                }
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj)
                    return true;
                if (obj == null)
                    return false;
                if (getClass() != obj.getClass())
                    return false;
                SetTextValueTransformation other = (SetTextValueTransformation) obj;
                if (newValue == null) {
                    if (other.newValue != null)
                        return false;
                } else if (!newValue.equals(other.newValue))
                    return false;
                if (selector == null) {
                    if (other.selector != null)
                        return false;
                } else if (!selector.equals(other.selector))
                    return false;
                return true;
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + ((newValue == null) ? 0 : newValue.hashCode());
                result = prime * result + ((selector == null) ? 0 : selector.hashCode());
                return result;
            }
        }

    }

}
