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
package org.l2x6.maven.utils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

    static final Pattern[] POSTPROCESS_PATTERNS = new Pattern[] { Pattern.compile("(<\\?xml[^>]*\\?>)?(\\s*)<"),
            Pattern.compile("(\\s*)<project([^>]*)>"), Pattern.compile("\\s*$") };
    static final Pattern EOL_PATTERN = Pattern.compile("\r?\n");
    static final Pattern WS_PATTERN = Pattern.compile("[ \t\n\r]+");
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
        LazyWriter lazyWriter = new LazyWriter();
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
            final String ws = (String) xPath.evaluate(anyNs("project") + "/*[1]/preceding-sibling::text()[last()]",
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

    /**
     * A generator of XPath 1.0 "any namespace" selector, such as
     * {@code /*:[local-name()='foo']/*:[local-name()='bar']}. In XPath 2.0, this would be just {@code /*:foo/*:bar},
     * but as of Java 13, there is only XPath 1.0 available in the JDK.
     *
     * @param  elements namespace-less element names
     * @return          am XPath 1.0 style selector
     */
    public static String anyNs(String... elements) {
        StringBuilder sb = new StringBuilder();
        for (String e : elements) {
            sb.append("/*[local-name()='").append(e).append("']");
        }
        return sb.toString();
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

    public static class WrappedNode<T extends Node> {
        protected final TransformationContext context;
        protected final T node;
        protected final int indentLevel;

        public WrappedNode(TransformationContext context, T node, int indentLevel) {
            this.context = context;
            this.node = node;
            this.indentLevel = indentLevel;
        }

        public ContainerElement asContainerElement() {
            return new ContainerElement(context, (Element) node, null, indentLevel);
        }

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

        public Node previousSiblingInsertionRefNode() {
            Node currentNode = this.node;
            while (true) {
                Node next = currentNode.getPreviousSibling();
                if (next == null) {
                    return currentNode;
                }
                switch (next.getNodeType()) {
                case Node.COMMENT_NODE:
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

        public Comment prependComment(String comment) {
            final Node refNode = previousSiblingInsertionRefNode();
            Comment result = node.getOwnerDocument().createComment(comment);
            node.getParentNode().insertBefore(context.newLine(), refNode);
            node.getParentNode().insertBefore(context.indent(indentLevel), refNode);
            node.getParentNode().insertBefore(result, refNode);
            return result;
        }

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

        public T getNode() {
            return node;
        }
    }

    public static class ContainerElement extends WrappedNode<Element> {
        private final Predicate<Node> ELEMENT_FILTER = n -> n.getNodeType() == Node.ELEMENT_NODE;
        protected Text lastIndent;

        public ContainerElement(TransformationContext context, Element containerElement, Text lastIndent, int indentLevel) {
            super(context, containerElement, indentLevel);
            this.lastIndent = lastIndent;
        }

        public Iterable<WrappedNode<Element>> childElements() {
            return () -> new NodeIterator<WrappedNode<Element>>(
                    node.getChildNodes(),
                    ELEMENT_FILTER,
                    n -> new WrappedNode<Element>(context, (Element) n, indentLevel + 1));
        }

        public Stream<WrappedNode<Element>> childElementsStream() {
            return StreamSupport.stream(childElements().spliterator(), false);
        }

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

        public ContainerElement addChildContainerElement(String elementName) {
            return addChildContainerElement(elementName, null, false, false);
        }

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
         * Creates the given {@code elementName} under the given {@code parent} unless it exists already.
         *
         * @return an existing or newly created {@link Element} with the given {@code elementName}
         */
        public ContainerElement getOrAddChildContainerElement(String elementName) {
            for (WrappedNode<Element> child : childElements()) {
                if (child.node.getNodeName().equals(elementName)) {
                    /* No need to insert, return existing */
                    return child.asContainerElement();
                }
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

        public void addChildTextElement(String nodeName, final String text) {
            addChildTextElement(nodeName, text, getOrAddLastIndent());
        }

        public void addChildTextElement(String nodeName, final String text, Node refNode) {
            if (text != null) {
                node.insertBefore(context.indent(indentLevel + 1), refNode);
                final Element result1 = context.document.createElement(nodeName);
                result1.appendChild(context.document.createTextNode(text));
                final Node result = result1;
                node.insertBefore(result, refNode);
            }
        }

        public void addChildElement(String nodeName, Node refNode) {
            node.insertBefore(context.indent(indentLevel + 1), refNode);
            final Element result = context.document.createElement(nodeName);
            node.insertBefore(result, refNode);
        }

        public void addOrSetChildTextElement(String name, String value) {
            for (WrappedNode<Element> prop : childElements()) {
                if (prop.node.getNodeName().equals(name)) {
                    prop.node.setTextContent(value);
                    return;
                }
            }
            addChildTextElement(name, value, getOrAddLastIndent());
        }

        public void addFragment(DocumentFragment fragment) {
            addFragment(fragment, getOrAddLastIndent());
        }

        public void addFragment(DocumentFragment fragment, Node refNode) {
            final NodeList children = fragment.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                final Node child = children.item(i);
                final Node imported = node.getOwnerDocument().importNode(child, true);
                node.insertBefore(imported, refNode);
            }
        }

        public void addGavtcs(Gavtcs gavtcs) {
            addGavtcs(gavtcs, getOrAddLastIndent());
        }

        public void addGavtcs(Gavtcs gavtcs, Node refNode) {
            final ContainerElement dep = addChildContainerElement("dependency", refNode, false, false);
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
        }

        public Gavtcs asGavtcs() {
            String groupId = null;
            String artifactId = null;
            String version = null;
            String type = null;
            String classifier = null;
            String scope = null;
            List<Ga> exclusions = null;
            for (WrappedNode<Element> depChild : childElements()) {
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
                    for (WrappedNode<Element> excl : depChild.asContainerElement().childElements()) {
                        String exclGroupId = null;
                        String exclArtifactId = null;
                        for (WrappedNode<Element> exclChild : excl.asContainerElement().childElements()) {
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
            if (exclusions == null) {
                return new Gavtcs(groupId, artifactId, version, type, classifier, scope);
            } else {
                return new Gavtcs(groupId, artifactId, version, type, classifier, scope, exclusions);
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

        }

        public Comment ensureCommentPrepended(String comment) {
            Comment precedingComment = previousSiblingCommentNode();
            if (precedingComment == null || !comment.equals(precedingComment.getTextContent())) {
                return prependComment(comment);
            }
            return precedingComment;
        }
    }

    class LazyWriter {
        private String oldContent;

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
     * A context of a set of {@link Transformation}s.
     */
    public static class TransformationContext {
        private final Path pomXmlPath;
        private final Document document;
        private final ContainerElement project;
        private final XPath xPath;
        private final String indentationString;
        private static volatile Map<String, ElementOrderEntry> elementOrdering;
        private static final Object elementOrderingLock = new Object();

        public TransformationContext(Path pomXmlPath, Document document, String indentationString, XPath xPath) {
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
            for (WrappedNode<Element> projectChild : project.childElements()) {
                final String projectChildName = projectChild.node.getNodeName();
                if (projectChildName.equals(elementName)) {
                    /* No need to insert, return existing */
                    return projectChild.asContainerElement();
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

        public Optional<ContainerElement> getContainerElement(String... path) {
            try {
                final Node node = (Node) xPath.evaluate(anyNs(path), document, XPathConstants.NODE);
                if (node != null) {
                    return Optional.of(new ContainerElement(this, (Element) node, null, path.length - 1));
                }
                return Optional.empty();
            } catch (XPathExpressionException e) {
                throw new RuntimeException(e);
            }
        }

        public Set<Gavtcs> getDependencies() {
            return getContainerElement("project", "dependencies")
                    .map(deps -> deps.childElementsStream()
                            .map(dep -> dep.asContainerElement().asGavtcs())
                            .collect(Collectors.toCollection(() -> (Set<Gavtcs>) new LinkedHashSet<Gavtcs>())))
                    .orElse(Collections.emptySet());
        }

        public Optional<ContainerElement> findDependency(Gavtcs gavtcs) {
            return getContainerElement("project", "dependencies")
                    .map(depsNode -> depsNode.childElementsStream()
                            .map(WrappedNode::asContainerElement)
                            .filter(depNode -> depNode.asGavtcs().equals(gavtcs))
                            .findFirst()
                            .orElse(null));
        }

        public void removeDependency(Gavtcs removedDependency, boolean removePrecedingComments,
                boolean removePrecedingWhitespace) {
            getContainerElement("project", "dependencies")
                    .ifPresent(deps -> deps.childElementsStream()
                            .filter(wrappedDepNode -> wrappedDepNode.asContainerElement().asGavtcs().equals(removedDependency))
                            .findFirst()
                            .ifPresent(wrappedDepNode -> wrappedDepNode.remove(removePrecedingComments,
                                    removePrecedingWhitespace)));
        }

        public void addDependencyIfNeeded(Gavtcs gavtcs, Comparator<Gavtcs> comparator) {
            final ContainerElement deps = getOrAddContainerElement("dependencies");
            Node refNode = null;
            for (WrappedNode<Element> dep : deps.childElements()) {
                final Gavtcs depGavtcs = dep.asContainerElement().asGavtcs();
                int comparison = comparator.compare(gavtcs, depGavtcs);
                if (comparison == 0) {
                    /* the given gavtcs is available, no need to add it */
                    return;
                }
                if (refNode == null && comparison < 0) {
                    refNode = dep.previousSiblingInsertionRefNode();
                }
            }

            if (refNode == null) {
                refNode = deps.getOrAddLastIndent();
            }
            deps.addGavtcs(gavtcs, refNode);
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
    }

    /**
     * A transformation of a DOM
     */
    public interface Transformation {

        public static Transformation addModule(String module) {
            return (Document document, TransformationContext context) -> {
                final ContainerElement modules = context.getOrAddContainerElement("modules");
                modules.addChildTextElement("module", module);
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
                final String xPath = anyNs(path);
                context.removeNode(xPath, removePrecedingComments, removePrecedingWhitespace, onlyIfEmpty);
            };
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

        public static Transformation addDependencyIfNeeded(Gavtcs gavtcs, Comparator<Gavtcs> comparator) {
            return (Document document, TransformationContext context) -> context.addDependencyIfNeeded(gavtcs, comparator);
        }

        public static Transformation updateMappedDependencies(
                Predicate<Gavtcs> isSubsetMember,
                Function<Gavtcs, Optional<Gavtcs>> dependencyMapper,
                Comparator<Gavtcs> comparator,
                String initialComment) {
            return (Document document, TransformationContext context) -> {
                final Set<Gavtcs> deps = context.getDependencies();
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
                            .ifPresent(firstDepNode -> firstDepNode.ensureCommentPrepended(initialComment));
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

                Set<Gavtcs> deps = context.getDependencies();
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
                            .ifPresent(firstDepNode -> firstDepNode.ensureCommentPrepended(initialComment));
                }

            };
        }

        public static Transformation removeModule(boolean removePrecedingComments, boolean removePrecedingWhitespace,
                String module) {
            return (Document document, TransformationContext context) -> {
                final String xPath = anyNs("project", "modules", "module") + "[text() = '" + module + "']";
                context.removeNode(xPath, removePrecedingComments, removePrecedingWhitespace, false);
            };
        }

        public static Transformation removeModules(boolean removePrecedingComments, boolean removePrecedingWhitespace,
                Set<String> modules) {
            return (Document document, TransformationContext context) -> {
                for (String module : modules) {
                    final String xPath = anyNs("project", "modules", "module") + "[text() = '" + module + "']";
                    context.removeNode(xPath, removePrecedingComments, removePrecedingWhitespace, false);
                }
            };
        }

        public static Transformation commentModules(Collection<String> modulesToComment, String commentText) {
            return (Document document, TransformationContext context) -> {
                final String condition = modulesToComment.stream()
                        .map(m -> "text() = '" + m + "'")
                        .collect(Collectors.joining(" or "));
                final String xPathExpr = anyNs("project", "modules", "module") + "[" + condition + "]";
                try {
                    final NodeList moduleNodes = (NodeList) context.getXPath().evaluate(xPathExpr, document,
                            XPathConstants.NODESET);
                    for (int i = 0; i < moduleNodes.getLength(); i++) {
                        TransformationContext.commentTextNode(moduleNodes.item(i), commentText);
                    }
                } catch (XPathExpressionException | DOMException e) {
                    throw new RuntimeException(e);
                }
            };
        }

        public static Transformation uncommentModules(String commentText) {
            return (Document document, TransformationContext context) -> {
                final String xPathExpr = anyNs("project", "modules") + "/comment()[starts-with(., '"
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
                        final Node parent = commentNode.getParentNode();
                        final Element newModuleNode = context.document.createElement("module");
                        newModuleNode.appendChild(context.document.createTextNode(modulePath));
                        parent.replaceChild(newModuleNode, commentNode);
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
                final String xPath = anyNs("project", "properties", propertyName);
                context.removeNode(xPath, removePrecedingComments, removePrecedingWhitespace, false);
            };
        }

        public static Transformation removePlugin(boolean removePrecedingComments, boolean removePrecedingWhitespace,
                String groupId, String artifactId) {
            return (Document document, TransformationContext context) -> {
                final String xPath = anyNs("project", "build", "plugins", "plugin")
                        + "[." + anyNs("groupId") + "/text() = '" + groupId + "' and ." + anyNs("artifactId") + "/text() = '"
                        + artifactId + "']";
                context.removeNode(xPath, removePrecedingComments, removePrecedingWhitespace, false);
            };
        }

        public static Transformation removeDependency(boolean removePrecedingComments, boolean removePrecedingWhitespace,
                Predicate<Gavtcs> predicate) {
            return (Document document, TransformationContext context) -> {
                context.getDependencies().stream()
                        .filter(predicate)
                        .forEach(dep -> context.removeDependency(dep, removePrecedingComments, removePrecedingWhitespace));
            };
        }

        public static Transformation setParent(String artifactId, String relativePath) {
            return (Document document, TransformationContext context) -> {
                try {
                    {
                        final String xPathExpression = anyNs("project", "parent", "artifactId") + "text()";
                        Node artifactIdNode = (Node) context.getXPath().evaluate(
                                xPathExpression, document,
                                XPathConstants.NODE);
                        if (artifactIdNode != null) {
                            artifactIdNode.setNodeValue(artifactId);
                        } else {
                            throw new IllegalStateException("Could not find " + xPathExpression + " in " + context.pomXmlPath);
                        }

                    }
                    if (relativePath == null) {
                        /* remove relativePath */
                        final String xPathExpression = anyNs("project", "parent", "relativePath");
                        Node node = (Node) context.getXPath().evaluate(
                                xPathExpression, document,
                                XPathConstants.NODE);
                        if (node != null) {
                            final Node prevSibling = node.getPreviousSibling();
                            if (prevSibling != null && prevSibling.getNodeType() == Node.TEXT_NODE
                                    && WS_PATTERN.matcher(prevSibling.getTextContent()).matches()) {
                                /* remove any preceding whitespace */
                                prevSibling.getParentNode().removeChild(prevSibling);
                            }
                            node.getParentNode().removeChild(node);
                        }
                    } else {
                        /* Add or set relativePath */
                        final String xPathExpression = anyNs("project", "parent", "relativePath");
                        Node node = (Node) context.getXPath().evaluate(
                                xPathExpression, document,
                                XPathConstants.NODE);
                        if (node != null) {
                            node.getFirstChild().setNodeValue(relativePath);
                        } else {
                            node = document.createElement("relativePath");
                            ((Node) context.getXPath().evaluate(
                                    anyNs("project", "parent"), document,
                                    XPathConstants.NODE)).appendChild(node);
                            final Text text = document.createTextNode(relativePath);
                            node.appendChild(text);
                        }
                    }
                } catch (XPathExpressionException | DOMException e) {
                    throw new RuntimeException(e);
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
