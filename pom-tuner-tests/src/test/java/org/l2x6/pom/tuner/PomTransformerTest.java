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

import java.io.StringReader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.l2x6.pom.tuner.PomTransformer.ContainerElement;
import org.l2x6.pom.tuner.PomTransformer.NodeGavtcs;
import org.l2x6.pom.tuner.PomTransformer.SimpleElementWhitespace;
import org.l2x6.pom.tuner.PomTransformer.Transformation;
import org.l2x6.pom.tuner.PomTransformer.TransformationContext;
import org.l2x6.pom.tuner.model.Ga;
import org.l2x6.pom.tuner.model.Gavtcs;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class PomTransformerTest {

    @Test
    void postProcess() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.emptyList(), expected);
    }

    @Test
    void setParent() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <parent>\n" //
                + "        <groupId>org.acme</groupId>\n" //
                + "        <artifactId>old-parent</artifactId>\n" //
                + "        <version>1.2.3</version>\n" //
                + "        <relativePath>../pom.xml</relativePath>\n" //
                + "    </parent>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <parent>\n" //
                + "        <groupId>org.acme</groupId>\n" //
                + "        <artifactId>new-parent</artifactId>\n" //
                + "        <version>1.2.3</version>\n" //
                + "        <relativePath>../../pom.xml</relativePath>\n" //
                + "    </parent>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(Transformation.setParent("new-parent", "../../pom.xml")),
                expected);
    }

    @Test
    void setParentNoRelPath() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <parent>\n" //
                + "        <groupId>org.acme</groupId>\n" //
                + "        <artifactId>old-parent</artifactId>\n" //
                + "        <version>1.2.3</version>\n" //
                + "    </parent>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <parent>\n" //
                + "        <groupId>org.acme</groupId>\n" //
                + "        <artifactId>new-parent</artifactId>\n" //
                + "        <version>1.2.3</version>\n" //
                + "        <relativePath>../../pom.xml</relativePath>\n" //
                + "    </parent>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(Transformation.setParent("new-parent", "../../pom.xml")),
                expected);
    }

    @Test
    void postProcessLicenseHeader() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<!--\n" //
                + "\n" //
                + "    Licensed to the Apache Software Foundation (ASF) under one or more\n" //
                + "\n" //
                + "-->\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<!--\n" //
                + "\n" //
                + "    Licensed to the Apache Software Foundation (ASF) under one or more\n" //
                + "\n" //
                + "-->\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.emptyList(), expected);
    }

    @Test
    void postProcessEol() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n" //
                + "    <modelVersion>4.0.0</modelVersion>\r\n" //
                + "</project>";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n" //
                + "    <modelVersion>4.0.0</modelVersion>\r\n" //
                + "</project>";
        assertTransformation(source, Collections.emptyList(), expected);
    }

    @Test
    void addModuleNoModules() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>new-module</module>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(Transformation.addModule("new-module")), expected);
    }

    @Test
    void addModuleIfNeeded() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>new-module</module>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(Transformation.addModuleIfNeeded("new-module", String::compareTo)), expected);
    }

    @Test
    void addModuleIfNeededBefore() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>old-module</module>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>new-module</module>\n" //
                + "        <module>old-module</module>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(Transformation.addModuleIfNeeded("new-module", String::compareTo)),
                expected);
    }

    @Test
    void addModuleNoModulesNoIndent() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging></project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>new-module</module>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(Transformation.addModule("new-module")), expected);
    }

    @Test
    void addModuleAfterModule() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>old-module</module>\n" //
                + "    </modules>\n" + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>old-module</module>\n" //
                + "        <module>new-module</module>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(Transformation.addModule("new-module")), expected);
    }

    @Test
    void addModuleAfterModuleNoIndent() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>old-module</module></modules>\n" + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>old-module</module>\n" //
                + "        <module>new-module</module>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(Transformation.addModule("new-module")), expected);
    }

    @Test
    void addModuleBeforeBuild() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <build>\n" //
                + "    </build>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>new-module</module>\n" //
                + "    </modules>\n" //
                + "\n" //
                + "    <build>\n" //
                + "    </build>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(Transformation.addModule("new-module")), expected);
    }

    @Test
    void addModuleOutOfOrder() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo>bar</foo>\n" //
                + "    </properties>\n" //
                + "\n" //
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.foo</groupId>\n" //
                + "                <artifactId>bar</artifactId>\n" //
                + "                <version>${bar.version}</version>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <!-- build those first -->\n" //
                + "        <module>module-1</module>\n" //
                + "    </modules>\n" //
                + "\n" //
                + "    <build>\n" //
                + "    </build>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo>bar</foo>\n" //
                + "    </properties>\n" //
                + "\n" //
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.foo</groupId>\n" //
                + "                <artifactId>bar</artifactId>\n" //
                + "                <version>${bar.version}</version>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <!-- build those first -->\n" //
                + "        <module>module-1</module>\n" //
                + "        <module>module-2</module>\n" //
                + "    </modules>\n" //
                + "\n" //
                + "    <build>\n" //
                + "    </build>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(Transformation.addModule("module-2")), expected);
    }

    @Test
    void setProperty() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo>bar</foo>\n" //
                + "        <baz>boo</baz>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo>bar</foo>\n" //
                + "        <baz>new</baz>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(Transformation.addOrSetProperty("baz", "new")), expected);
    }

    @Test
    void addPropertyOrdered() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p1>bar</p1>\n" //
                + "        <p3>boo</p3>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p1>bar</p1>\n" //
                + "        <p2>new</p2>\n" //
                + "        <p3>boo</p3>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(
                (Document document, TransformationContext context) -> {
                    final ContainerElement props = context.getOrAddContainerElement("properties");
                    props.addChildTextElementIfNeeded("p2", "new", Comparators.entryKeyOnly());
                }), expected);
    }

    @Test
    void addPropertyOrderedSecond() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p0>foo</p0>\n" //
                + "        <p1>bar</p1>\n" //
                + "        <p3>boo</p3>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p0>foo</p0>\n" //
                + "        <p1>bar</p1>\n" //
                + "        <p2>new</p2>\n" //
                + "        <p3>boo</p3>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(
                (Document document, TransformationContext context) -> {
                    final ContainerElement props = context.getOrAddContainerElement("properties");
                    props.addChildTextElementIfNeeded("p2", "new", Comparators.entryKeyOnly());
                }), expected);
    }

    @Test
    void addPropertyOrderedFirst() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p1>bar</p1>\n" //
                + "        <p2>foo</p2>\n" //
                + "        <p3>boo</p3>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p0>new</p0>\n" //
                + "        <p1>bar</p1>\n" //
                + "        <p2>foo</p2>\n" //
                + "        <p3>boo</p3>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(
                (Document document, TransformationContext context) -> {
                    final ContainerElement props = context.getOrAddContainerElement("properties");
                    props.addChildTextElementIfNeeded("p0", "new", Comparators.entryKeyOnly());
                }), expected);
        assertTransformation(source, Collections.singletonList(
                (Document document, TransformationContext context) -> {
                    final ContainerElement props = context.getOrAddContainerElement("properties");
                    props.addChildTextElementIfNeeded("p0", "new", Comparators.beforeFirst());
                }), expected);
    }

    @Test
    void addPropertyOrderedBadOrder() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p3>boo</p3>\n" //
                + "        <p1>bar</p1>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p3>boo</p3>\n" //
                + "        <p1>new</p1>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(
                (Document document, TransformationContext context) -> {
                    final ContainerElement props = context.getOrAddContainerElement("properties");
                    props.addChildTextElementIfNeeded("p1", "new", Comparators.entryKeyOnly());
                }), expected);
    }

    @Test
    void addPropertyOrderedExistingKey() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p3>boo</p3>\n" //
                + "        <p1>bar</p1>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p3>boo</p3>\n" //
                + "        <p1>new</p1>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(
                (Document document, TransformationContext context) -> {
                    final ContainerElement props = context.getOrAddContainerElement("properties");
                    props.addChildTextElementIfNeeded("p1", "new", Comparators.entryKeyOnly());
                }), expected);
    }

    @Test
    void addPropertyOrderedLast() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p3>boo</p3>\n" //
                + "        <p1>bar</p1>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p3>boo</p3>\n" //
                + "        <p1>bar</p1>\n" //
                + "        <p5>new</p5>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(
                (Document document, TransformationContext context) -> {
                    final ContainerElement props = context.getOrAddContainerElement("properties");
                    props.addChildTextElementIfNeeded("p5", "new", Comparators.entryKeyOnly());
                }), expected);
        assertTransformation(source, Collections.singletonList(
                (Document document, TransformationContext context) -> {
                    final ContainerElement props = context.getOrAddContainerElement("properties");
                    props.addChildTextElementIfNeeded("p5", "new", Comparators.afterLast());
                }), expected);
    }

    @Test
    void addPropertyOrderedBeforeKey() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p0>foo</p0>\n" //
                + "        <p1>bar</p1>\n" //
                + "        <p3>boo</p3>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p0>foo</p0>\n" //
                + "        <p2>new</p2>\n" //
                + "        <p1>bar</p1>\n" //
                + "        <p3>boo</p3>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(
                (Document document, TransformationContext context) -> {
                    final ContainerElement props = context.getOrAddContainerElement("properties");
                    props.addChildTextElementIfNeeded("p2", "new",
                            Comparator.comparing(Map.Entry::getKey, Comparators.before("p1")));
                }), expected);
    }

    @Test
    void addPropertyOrderedBeforeFirstKey() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p1>bar</p1>\n" //
                + "        <p3>boo</p3>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p0>new</p0>\n" //
                + "        <p1>bar</p1>\n" //
                + "        <p3>boo</p3>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(
                (Document document, TransformationContext context) -> {
                    final ContainerElement props = context.getOrAddContainerElement("properties");
                    props.addChildTextElementIfNeeded("p0", "new",
                            Comparator.comparing(Map.Entry::getKey, Comparators.before("p1")));
                }), expected);
    }

    @Test
    void addPropertyOrderedBeforeNonExistentKey() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p1>bar</p1>\n" //
                + "        <p3>boo</p3>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p1>bar</p1>\n" //
                + "        <p3>boo</p3>\n" //
                + "        <p0>new</p0>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(
                (Document document, TransformationContext context) -> {
                    final ContainerElement props = context.getOrAddContainerElement("properties");
                    props.addChildTextElementIfNeeded("p0", "new",
                            Comparator.comparing(Map.Entry::getKey, Comparators.before("p2")));
                }), expected);
    }

    @Test
    void addPropertyOrderedAfterKey() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p0>foo</p0>\n" //
                + "        <p1>bar</p1>\n" //
                + "        <p3>boo</p3>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p0>foo</p0>\n" //
                + "        <p1>bar</p1>\n" //
                + "        <p2>new</p2>\n" //
                + "        <p3>boo</p3>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(
                (Document document, TransformationContext context) -> {
                    final ContainerElement props = context.getOrAddContainerElement("properties");
                    props.addChildTextElementIfNeeded("p2", "new",
                            Comparator.comparing(Map.Entry::getKey, Comparators.after("p1")));
                }), expected);
    }

    @Test
    void addPropertyOrderedAfterFirstKey() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p1>bar</p1>\n" //
                + "        <p3>boo</p3>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p1>bar</p1>\n" //
                + "        <p0>new</p0>\n" //
                + "        <p3>boo</p3>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(
                (Document document, TransformationContext context) -> {
                    final ContainerElement props = context.getOrAddContainerElement("properties");
                    props.addChildTextElementIfNeeded("p0", "new",
                            Comparator.comparing(Map.Entry::getKey, Comparators.after("p1")));
                }), expected);
    }

    @Test
    void addPropertyOrderedAfterNonExistentKey() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p1>bar</p1>\n" //
                + "        <p3>boo</p3>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p1>bar</p1>\n" //
                + "        <p3>boo</p3>\n" //
                + "        <p0>new</p0>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(
                (Document document, TransformationContext context) -> {
                    final ContainerElement props = context.getOrAddContainerElement("properties");
                    props.addChildTextElementIfNeeded("p0", "new",
                            Comparator.comparing(Map.Entry::getKey, Comparators.after("p2")));
                }), expected);
    }

    @Test
    void addPropertyOrderedComments() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p1>bar</p1>\n" //
                + "        <!-- comment -->\n" //
                + "        <p3>boo</p3>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p1>bar</p1>\n" //
                + "        <p2>new</p2>\n" //
                + "        <!-- comment -->\n" //
                + "        <p3>boo</p3>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(
                (Document document, TransformationContext context) -> {
                    final ContainerElement props = context.getOrAddContainerElement("properties");
                    props.addChildTextElementIfNeeded("p2", "new", Comparators.entryKeyOnly());
                }), expected);
    }

    @Test
    void addPropertyOrderedBeforeAdjacentComments() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p1>bar</p1><!-- comment -->\n" //
                + "        <p3>boo</p3>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <p1>bar</p1><!-- comment -->\n" //
                + "        <p>new</p>\n" //
                + "        <p3>boo</p3>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(
                (Document document, TransformationContext context) -> {
                    final ContainerElement props = context.getOrAddContainerElement("properties");
                    props.addChildTextElementIfNeeded("p", "new",
                            Comparator.comparing(Map.Entry::getKey, Comparators.before("p3")));
                }), expected);
    }

    @Test
    void addProperty() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo>bar</foo>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo>bar</foo>\n" //
                + "        <baz>new</baz>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(Transformation.addOrSetProperty("baz", "new")), expected);
    }

    @Test
    void addModuleBeforeBuildNoIndent() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging><build>\n" //
                + "    </build>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>new-module</module>\n" //
                + "    </modules>\n" //
                + "\n" //
                + "    <build>\n" //
                + "    </build>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(Transformation.addModule("new-module")), expected);
    }

    @Test
    void removeModule() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>module-1</module>\n" //
                + "        <module>module-2</module>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>module-1</module>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        assertTransformation(source, Arrays.asList(
                Transformation.removeModule(true, true, "module-2"),
                Transformation.removeContainerElementIfEmpty(true, true, true, "modules")), expected);
    }

    @Test
    void commentModule() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>module-1</module>\n" //
                + "        <module>module-2</module>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>module-1</module>\n" //
                + "        <!-- <module>module-2</module> test comment -->\n" //
                + "    </modules>\n" //
                + "</project>\n";

        assertTransformation(source, Arrays.asList(
                Transformation.commentModules(Arrays.asList("module-2"), "test comment")),
                expected);

        assertTransformation(expected, Arrays.asList(
                Transformation.uncommentModules("test comment")),
                source);

        assertTransformation(expected, Arrays.asList(
                Transformation.uncommentModules("test comment", m -> "module-2".equals(m))),
                source);

        assertTransformation(expected, Arrays.asList(
                Transformation.uncommentModules("test comment", m -> "foo".equals(m))),
                expected);

    }

    @Test
    void commentModuleInProfile() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <profiles>\n" //
                + "       <profile>\n"
                + "         <id>profile-1</id>\n"
                + "         <modules>\n"
                + "           <module>module-1</module>\n"
                + "           <module>module-2</module>\n"
                + "           <module>module-3</module>\n"
                + "         </modules>\n"
                + "       </profile>" //
                + "     </profiles>\n"
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <profiles>\n" //
                + "       <profile>\n"
                + "         <id>profile-1</id>\n"
                + "         <modules>\n"
                + "           <!-- <module>module-1</module> test comment -->\n"
                + "           <!-- <module>module-2</module> test comment -->\n"
                + "           <module>module-3</module>\n"
                + "         </modules>\n"
                + "       </profile>" //
                + "     </profiles>\n"
                + "</project>\n";

        assertTransformation(source, Arrays.asList(
                Transformation.commentModulesInProfile("profile-1", Arrays.asList("module-1", "module-2"), "test comment")),
                expected);
    }

    @Test
    void removeLastModule() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <!-- comment -->\n" //
                + "    <modules>\n" //
                + "        <module>module-1</module>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "</project>\n";
        assertTransformation(source, Arrays.asList(
                Transformation.removeModule(true, true, "module-1"),
                Transformation.removeContainerElementIfEmpty(true, true, true, "modules")), expected);
    }

    @Test
    void removeModuleWithComment() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>module-1</module>\n" //
                + "        <!-- comment -->\n" //
                + "        <module>module-2</module>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>module-1</module>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(Transformation.removeModule(true, true, "module-2")), expected);
    }

    @Test
    void removeModuleWithoutComment() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>module-1</module>\n" //
                + "        <!-- comment -->\n" //
                + "        <module>module-2</module>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>module-1</module>\n" //
                + "        <!-- comment -->\n" //
                + "    </modules>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(Transformation.removeModule(false, true, "module-2")), expected);
    }

    @Test
    void removeAllModulesDefaultProfile() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>module-1</module>\n" //
                + "        <!-- comment -->\n" //
                + "        <module>module-2</module>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(Transformation.removeAllModules(null, true, true)), expected);
    }

    @Test
    void removeNodes() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>module-1</module>\n" //
                + "        <!-- comment -->\n" //
                + "        <module>module-2</module>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(
                (Document document, TransformationContext context) -> context.removeNodes(
                        PomTunerUtils.anyNs("project", "modules", "module"),
                        TransformationContext.removePrecedingCommentsAndWhiteSpace(true, true))),
                expected);
    }

    @Test
    void removeNode() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>module-1</module>\n" //
                + "        <!-- comment -->\n" //
                + "        <module>module-2</module>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <!-- comment -->\n" //
                + "        <module>module-2</module>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(
                (Document document, TransformationContext context) -> context.removeNode(
                        PomTunerUtils.anyNs("project", "modules", "module"), true, true, false)),
                expected);
    }

    @Test
    void removeAllModulesInProfile() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>module-1</module>\n" //
                + "        <!-- comment -->\n" //
                + "        <module>module-2</module>\n" //
                + "    </modules>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "            <modules>\n" //
                + "                <module>module-4</module>\n" //
                + "                <!-- comment -->\n" //
                + "                <module>module-5</module>\n" //
                + "                <module>../../module-6</module>\n" //
                + "            </modules>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>module-1</module>\n" //
                + "        <!-- comment -->\n" //
                + "        <module>module-2</module>\n" //
                + "    </modules>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "            <modules>\n" //
                + "            </modules>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(Transformation.removeAllModules("profile1", true, true)),
                expected);
    }

    @Test
    void addModulesToProfile() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>module-1</module>\n" //
                + "        <!-- comment -->\n" //
                + "        <module>module-2</module>\n" //
                + "    </modules>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "            <modules>\n" //
                + "                <module>module-4</module>\n" //
                + "                <!-- comment -->\n" //
                + "                <module>module-5</module>\n" //
                + "            </modules>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>module-1</module>\n" //
                + "        <!-- comment -->\n" //
                + "        <module>module-2</module>\n" //
                + "    </modules>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "            <modules>\n" //
                + "                <module>module-4</module>\n" //
                + "                <!-- comment -->\n" //
                + "                <module>module-5</module>\n" //
                + "                <module>module-6</module>\n" //
                + "            </modules>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(Transformation.addModules("profile1", "module-6")),
                expected);
    }

    @Test
    void addModulesToNonExistentProfile() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>module-1</module>\n" //
                + "        <!-- comment -->\n" //
                + "        <module>module-2</module>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>module-1</module>\n" //
                + "        <!-- comment -->\n" //
                + "        <module>module-2</module>\n" //
                + "    </modules>\n" //
                + "\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "            <modules>\n" //
                + "                <module>module-6</module>\n" //
                + "            </modules>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(Transformation.addModules("profile1", "module-6")),
                expected);
    }

    static void assertTransformation(String src, Collection<Transformation> transformations,
            SimpleElementWhitespace simpleElementWhitespace, String expected) {
        PomTransformer.transform(transformations, simpleElementWhitespace, Paths.get("pom.xml"),
                () -> src, xml -> org.assertj.core.api.Assertions.assertThat(xml).isEqualTo(expected));
    }

    static void assertTransformation(String src, Collection<Transformation> transformations, String expected) {
        PomTransformer.transform(transformations, SimpleElementWhitespace.EMPTY, Paths.get("pom.xml"),
                () -> src, xml -> org.assertj.core.api.Assertions.assertThat(xml).isEqualTo(expected));
    }

    @Test
    void addDependencyManagementDependencies() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "    <dependencyManagement>\n" //
                + "    </dependencyManagement>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(Transformation.addContainerElementsIfNeeded("dependencyManagement", "dependencies")),
                expected);
    }

    @Test
    void addDependencyManagement() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(Transformation.addContainerElementsIfNeeded("dependencyManagement", "dependencies")),
                expected);
    }

    @Test
    void addDependencyManagementBeforeDependencies() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "    <dependencies>\n" //
                + "    </dependencies>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "    <dependencies>\n" //
                + "    </dependencies>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(Transformation.addContainerElementsIfNeeded("dependencyManagement", "dependencies")),
                expected);
    }

    @Test
    void addDependencyManagementBeforeBuild() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "    <build>\n" //
                + "    </build>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "\n" //
                + "    <build>\n" //
                + "    </build>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(Transformation.addContainerElementsIfNeeded("dependencyManagement", "dependencies")),
                expected);
    }

    @Test
    void addManagedDependency() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>my-ext</artifactId>\n" //
                + "                <version>${project.version}</version>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(
                        Transformation.addManagedDependency(new Gavtcs("org.acme", "my-ext", "${project.version}"))),
                expected);
    }

    @Test
    void addPluginOrdered() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "    <build>\n" //
                + "        <plugins>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>p1</artifactId>\n" //
                + "                <version>1.2.3</version>\n" //
                + "            </plugin>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.foo</groupId>\n" //
                + "                <artifactId>p3</artifactId>\n" //
                + "                <version>2.3.4</version>\n" //
                + "            </plugin>\n" //
                + "        </plugins>\n" //
                + "    </build>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "    <build>\n" //
                + "        <plugins>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>p1</artifactId>\n" //
                + "                <version>1.2.3</version>\n" //
                + "            </plugin>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>p2</artifactId>\n" //
                + "                <version>1.0.0</version>\n" //
                + "            </plugin>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.foo</groupId>\n" //
                + "                <artifactId>p3</artifactId>\n" //
                + "                <version>2.3.4</version>\n" //
                + "            </plugin>\n" //
                + "        </plugins>\n" //
                + "    </build>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(
                        (Document document, TransformationContext context) -> {
                            final ContainerElement plugins = context.getOrAddContainerElements("build", "plugins");
                            plugins.addGavtcsIfNeeded(new Gavtcs("org.acme", "p2", "1.0.0"), Gavtcs.groupFirstComparator());
                        }),
                expected);
    }

    @Test
    void addPluginOrderedSecond() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "    <build>\n" //
                + "        <plugins>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>p1</artifactId>\n" //
                + "                <version>1.0.0</version>\n" //
                + "            </plugin>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>p2</artifactId>\n" //
                + "                <version>1.0.0</version>\n" //
                + "            </plugin>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>p4</artifactId>\n" //
                + "                <version>1.0.0</version>\n" //
                + "            </plugin>\n" //
                + "        </plugins>\n" //
                + "    </build>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "    <build>\n" //
                + "        <plugins>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>p1</artifactId>\n" //
                + "                <version>1.0.0</version>\n" //
                + "            </plugin>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>p2</artifactId>\n" //
                + "                <version>1.0.0</version>\n" //
                + "            </plugin>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>p3</artifactId>\n" //
                + "                <version>1.0.0</version>\n" //
                + "            </plugin>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>p4</artifactId>\n" //
                + "                <version>1.0.0</version>\n" //
                + "            </plugin>\n" //
                + "        </plugins>\n" //
                + "    </build>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(
                        (Document document, TransformationContext context) -> {
                            final ContainerElement plugins = context.getOrAddContainerElements("build", "plugins");
                            plugins.addGavtcsIfNeeded(new Gavtcs("org.acme", "p3", "1.0.0"), Gavtcs.groupFirstComparator());
                        }),
                expected);
    }

    @Test
    void addPluginOrderedFirst() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "    <build>\n" //
                + "        <plugins>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>p1</artifactId>\n" //
                + "                <version>1.2.3</version>\n" //
                + "            </plugin>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.foo</groupId>\n" //
                + "                <artifactId>p3</artifactId>\n" //
                + "                <version>2.3.4</version>\n" //
                + "            </plugin>\n" //
                + "        </plugins>\n" //
                + "    </build>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "    <build>\n" //
                + "        <plugins>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>p0</artifactId>\n" //
                + "                <version>1.0.0</version>\n" //
                + "            </plugin>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>p1</artifactId>\n" //
                + "                <version>1.2.3</version>\n" //
                + "            </plugin>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.foo</groupId>\n" //
                + "                <artifactId>p3</artifactId>\n" //
                + "                <version>2.3.4</version>\n" //
                + "            </plugin>\n" //
                + "        </plugins>\n" //
                + "    </build>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(
                        (Document document, TransformationContext context) -> {
                            final ContainerElement plugins = context.getOrAddContainerElements("build", "plugins");
                            plugins.addGavtcsIfNeeded(new Gavtcs("org.acme", "p0", "1.0.0"), Gavtcs.groupFirstComparator());
                        }),
                expected);
    }

    @Test
    void addPluginOrderedComments() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "    <build>\n" //
                + "        <plugins>\n" //
                + "            <!-- comment -->\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>p1</artifactId>\n" //
                + "                <version>1.2.3</version>\n" //
                + "            </plugin>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.foo</groupId>\n" //
                + "                <artifactId>p3</artifactId>\n" //
                + "                <version>2.3.4</version>\n" //
                + "            </plugin>\n" //
                + "        </plugins>\n" //
                + "    </build>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "    <build>\n" //
                + "        <plugins>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>p0</artifactId>\n" //
                + "                <version>1.0.0</version>\n" //
                + "            </plugin>\n" //
                + "            <!-- comment -->\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>p1</artifactId>\n" //
                + "                <version>1.2.3</version>\n" //
                + "            </plugin>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.foo</groupId>\n" //
                + "                <artifactId>p3</artifactId>\n" //
                + "                <version>2.3.4</version>\n" //
                + "            </plugin>\n" //
                + "        </plugins>\n" //
                + "    </build>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(
                        (Document document, TransformationContext context) -> {
                            final ContainerElement plugins = context.getOrAddContainerElements("build", "plugins");
                            plugins.addGavtcsIfNeeded(new Gavtcs("org.acme", "p0", "1.0.0"), Gavtcs.groupFirstComparator());
                        }),
                expected);
    }

    @Test
    void addPluginOrderedBadOrder() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "    <build>\n" //
                + "        <plugins>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>p1</artifactId>\n" //
                + "                <version>1.2.3</version>\n" //
                + "            </plugin>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.foo</groupId>\n" //
                + "                <artifactId>p3</artifactId>\n" //
                + "                <version>2.3.4</version>\n" //
                + "            </plugin>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>p0</artifactId>\n" //
                + "                <version>1.0.0</version>\n" //
                + "            </plugin>\n" //
                + "        </plugins>\n" //
                + "    </build>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "    <build>\n" //
                + "        <plugins>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>p1</artifactId>\n" //
                + "                <version>1.2.3</version>\n" //
                + "            </plugin>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.foo</groupId>\n" //
                + "                <artifactId>p3</artifactId>\n" //
                + "                <version>2.3.4</version>\n" //
                + "            </plugin>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>p0</artifactId>\n" //
                + "                <version>1.0.0</version>\n" //
                + "            </plugin>\n" //
                + "        </plugins>\n" //
                + "    </build>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(
                        (Document document, TransformationContext context) -> {
                            final ContainerElement plugins = context.getOrAddContainerElements("build", "plugins");
                            plugins.addGavtcsIfNeeded(new Gavtcs("org.acme", "p0", "1.0.0"), Gavtcs.groupFirstComparator());
                        }),
                expected);
    }

    @Test
    void addManagedDependencyIfNeeded() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <dependency>\n" //
                + "                <groupId>${quarkus.platform.group-id}</groupId>\n" //
                + "                <artifactId>${quarkus.platform.artifact-id}</artifactId>\n" //
                + "                <version>${quarkus.platform.version}</version>\n" //
                + "                <type>pom</type>\n" //
                + "                <scope>import</scope>\n" //
                + "            </dependency>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>my-ext2</artifactId>\n" //
                + "                <version>${project.version}</version>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <dependency>\n" //
                + "                <groupId>${quarkus.platform.group-id}</groupId>\n" //
                + "                <artifactId>${quarkus.platform.artifact-id}</artifactId>\n" //
                + "                <version>${quarkus.platform.version}</version>\n" //
                + "                <type>pom</type>\n" //
                + "                <scope>import</scope>\n" //
                + "            </dependency>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>my-ext2</artifactId>\n" //
                + "                <version>${project.version}</version>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(
                        Transformation.addManagedDependencyIfNeeded(
                                new Gavtcs("${quarkus.platform.group-id}", "${quarkus.platform.artifact-id}",
                                        "${quarkus.platform.version}", "pom", null, "import"))),
                expected);
    }

    @Test
    void removeManagedDependencies() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep1</artifactId>\n" //
                + "                <version>${project.version}</version>\n" //
                + "            </dependency>\n" //
                + "            <!-- comment -->\n" //
                + "\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep2</artifactId>\n" //
                + "                <version>${project.version}</version>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep1</artifactId>\n" //
                + "                <version>${project.version}</version>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(
                        Transformation.removeManagedDependencies(true, true, gavtcs -> "dep2".equals(gavtcs.getArtifactId()))),
                expected);
    }

    @Test
    void removePlugin() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <build>\n" //
                + "        <plugins>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>p1</artifactId>\n" //
                + "                <version>${project.version}</version>\n" //
                + "            </plugin>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.foo</groupId>\n" //
                + "                <artifactId>p2</artifactId>\n" //
                + "                <version>${project.version}</version>\n" //
                + "            </plugin>\n" //
                + "        </plugins>\n" //
                + "    </build>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <build>\n" //
                + "        <plugins>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>p1</artifactId>\n" //
                + "                <version>${project.version}</version>\n" //
                + "            </plugin>\n" //
                + "        </plugins>\n" //
                + "    </build>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(
                        Transformation.removePlugins(null, true, true, gavtcs -> "org.foo".equals(gavtcs.getGroupId()))),
                expected);
    }

    @Test
    void setManagedDependencyVersion() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep1</artifactId>\n" //
                + "                <version>${dep1.version}</version>\n" //
                + "            </dependency>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep2</artifactId>\n" //
                + "                <version>${dep2.version}</version>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep1</artifactId>\n" //
                + "                <version>${dep1-new.version}</version>\n" //
                + "            </dependency>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep2</artifactId>\n" //
                + "                <version>${dep2.version}</version>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(
                        Transformation.setManagedDependencyVersion("${dep1-new.version}",
                                Arrays.asList(Ga.of("org.acme:dep1")))),
                expected);
    }

    @Test
    void setDependencyVersion() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencies>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep1</artifactId>\n" //
                + "            <version>${dep1.version}</version>\n" //
                + "        </dependency>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep2</artifactId>\n" //
                + "            <version>${dep2.version}</version>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencies>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep1</artifactId>\n" //
                + "            <version>${dep1-new.version}</version>\n" //
                + "        </dependency>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep2</artifactId>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
                + "</project>\n";
        assertTransformation(source,
                Arrays.asList(
                        Transformation.setDependencyVersion("${dep1-new.version}",
                                Arrays.asList(Ga.of("org.acme:dep1"))),
                        Transformation.setDependencyVersion(null,
                                Arrays.asList(Ga.of("org.acme:dep2")))),
                expected);
    }

    @Test
    void setManagedDependencyVersionInProfile() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep1</artifactId>\n" //
                + "                <version>${dep1.version}</version>\n" //
                + "            </dependency>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep2</artifactId>\n" //
                + "                <version>${dep2.version}</version>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "            <dependencyManagement>\n" //
                + "                <dependencies>\n" //
                + "                    <dependency>\n" //
                + "                        <groupId>org.acme</groupId>\n" //
                + "                        <artifactId>dep1</artifactId>\n" //
                + "                        <version>${dep1.version}</version>\n" //
                + "                    </dependency>\n" //
                + "                    <dependency>\n" //
                + "                        <groupId>org.acme</groupId>\n" //
                + "                        <artifactId>dep2</artifactId>\n" //
                + "                        <version>${dep2.version}</version>\n" //
                + "                    </dependency>\n" //
                + "                </dependencies>\n" //
                + "            </dependencyManagement>\n" //
                + "        </profile>\n" //
                + "        <profile>\n" //
                + "            <id>profile2</id>\n" //
                + "            <dependencyManagement>\n" //
                + "                <dependencies>\n" //
                + "                    <dependency>\n" //
                + "                        <groupId>org.acme</groupId>\n" //
                + "                        <artifactId>dep1</artifactId>\n" //
                + "                        <version>${dep1.version}</version>\n" //
                + "                    </dependency>\n" //
                + "                    <dependency>\n" //
                + "                        <groupId>org.acme</groupId>\n" //
                + "                        <artifactId>dep2</artifactId>\n" //
                + "                        <version>${dep2.version}</version>\n" //
                + "                    </dependency>\n" //
                + "                </dependencies>\n" //
                + "            </dependencyManagement>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep1</artifactId>\n" //
                + "                <version>${dep1.version}</version>\n" //
                + "            </dependency>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep2</artifactId>\n" //
                + "                <version>${dep2.version}</version>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "            <dependencyManagement>\n" //
                + "                <dependencies>\n" //
                + "                    <dependency>\n" //
                + "                        <groupId>org.acme</groupId>\n" //
                + "                        <artifactId>dep1</artifactId>\n" //
                + "                        <version>${dep1.version}</version>\n" //
                + "                    </dependency>\n" //
                + "                    <dependency>\n" //
                + "                        <groupId>org.acme</groupId>\n" //
                + "                        <artifactId>dep2</artifactId>\n" //
                + "                        <version>${dep2-new.version}</version>\n" //
                + "                    </dependency>\n" //
                + "                </dependencies>\n" //
                + "            </dependencyManagement>\n" //
                + "        </profile>\n" //
                + "        <profile>\n" //
                + "            <id>profile2</id>\n" //
                + "            <dependencyManagement>\n" //
                + "                <dependencies>\n" //
                + "                    <dependency>\n" //
                + "                        <groupId>org.acme</groupId>\n" //
                + "                        <artifactId>dep1</artifactId>\n" //
                + "                        <version>${dep1.version}</version>\n" //
                + "                    </dependency>\n" //
                + "                    <dependency>\n" //
                + "                        <groupId>org.acme</groupId>\n" //
                + "                        <artifactId>dep2</artifactId>\n" //
                + "                        <version>${dep2.version}</version>\n" //
                + "                    </dependency>\n" //
                + "                </dependencies>\n" //
                + "            </dependencyManagement>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(
                        Transformation.setManagedDependencyVersion("profile1", "${dep2-new.version}",
                                Arrays.asList(Ga.of("org.acme:dep2")))),
                expected);
    }

    @Test
    void addDependenciesIfNeeded() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencies>\n" //
                + "    </dependencies>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(Transformation.addContainerElementsIfNeeded("dependencies")),
                expected);
    }

    @Test
    void addDependenciesIfNeededBeforeBuildWithSpace() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <build/>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencies>\n" //
                + "    </dependencies>\n" //
                + "\n" //
                + "    <build/>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(Transformation.addContainerElementsIfNeeded("dependencies")),
                expected);
    }

    @Test
    void addDependenciesIfNeededBeforeBuild() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "    <build/>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencies>\n" //
                + "    </dependencies>\n" //
                + "\n" //
                + "    <build/>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(Transformation.addContainerElementsIfNeeded("dependencies")),
                expected);
    }

    @Test
    void addDependenciesNotNeeded() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencies/>\n" //
                + "\n" //
                + "    <build/>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencies/>\n" //
                + "\n" //
                + "    <build/>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(Transformation.addContainerElementsIfNeeded("dependencies")),
                expected);
    }

    @Test
    void addDependencyIfNeeded() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencies>\n" //
                + "    </dependencies>\n" //
                + "\n" //
                + "    <build/>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencies>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>a1</artifactId>\n" //
                + "            <version>1.2.3</version>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
                + "\n" //
                + "    <build/>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(Transformation.addDependencyIfNeeded(new Gavtcs("org.acme", "a1", "1.2.3"),
                        Gavtcs.scopeAndTypeFirstComparator())),
                expected);
    }

    @Test
    void reIndent() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencies>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>a1</artifactId>\n" //
                + "            <version>1.2.3</version>\n" //
                + "        </dependency>\n" //
                + "\n" //
                + "        <!-- Comment 1 -->\n" //
                + "        <!-- Comment 2 -->\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>a2</artifactId>\n" //
                + "            <version>1.2.3</version>\n" //
                + "            <exclusions>\n" //
                + "                <exclusion>\n" //
                + "                    <groupId>*</groupId>\n" //
                + "                    <artifactId>*</artifactId>\n" //
                + "                </exclusion>\n" //
                + "            </exclusions>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
                + "\n" //
                + "    <build/>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencies>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>a1</artifactId>\n" //
                + "            <version>1.2.3</version>\n" //
                + "        </dependency>\n" //
                + "\n" //
                + "                <!-- Comment 1 -->\n" //
                + "                <!-- Comment 2 -->\n" //
                + "                <dependency>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>a2</artifactId>\n" //
                + "                    <version>1.2.3</version>\n" //
                + "                    <exclusions>\n" //
                + "                        <exclusion>\n" //
                + "                            <groupId>*</groupId>\n" //
                + "                            <artifactId>*</artifactId>\n" //
                + "                        </exclusion>\n" //
                + "                    </exclusions>\n" //
                + "                </dependency>\n" //
                + "    </dependencies>\n" //
                + "\n" //
                + "    <build/>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(
                        (Document document, TransformationContext context) -> {
                            Set<NodeGavtcs> deps = context.getDependencies();
                            Iterator<NodeGavtcs> it = deps.iterator();
                            it.next();
                            NodeGavtcs dep2 = it.next();
                            context.reIndent(dep2.getNode().getNodes(TransformationContext.ALL_WHITESPACE_AND_COMMENTS),
                                    "                ");
                        }),
                expected);
    }

    @Test
    void updateDependencySubset() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencies>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>a1</artifactId>\n" //
                + "            <version>${project.version}</version>\n" //
                + "        </dependency>\n" //
                + "\n" //
                + "        <!-- initial comment -->\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>a2</artifactId>\n" //
                + "            <version>${project.version}</version>\n" //
                + "            <type>pom</type>\n" //
                + "            <scope>test</scope>\n" //
                + "            <exclusions>\n" //
                + "                <exclusion>\n" //
                + "                    <groupId>*</groupId>\n" //
                + "                    <artifactId>*</artifactId>\n" //
                + "                </exclusion>\n" //
                + "            </exclusions>\n" //
                + "        </dependency>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>a3</artifactId>\n" //
                + "            <version>${project.version}</version>\n" //
                + "            <type>pom</type>\n" //
                + "            <scope>test</scope>\n" //
                + "            <exclusions>\n" //
                + "                <exclusion>\n" //
                + "                    <groupId>*</groupId>\n" //
                + "                    <artifactId>*</artifactId>\n" //
                + "                </exclusion>\n" //
                + "            </exclusions>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
                + "\n" //
                + "    <build/>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencies>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>a1</artifactId>\n" //
                + "            <version>${project.version}</version>\n" //
                + "        </dependency>\n" //
                + "\n" //
                + "        <!-- initial comment -->\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>a2</artifactId>\n" //
                + "            <version>${project.version}</version>\n" //
                + "            <type>pom</type>\n" //
                + "            <scope>test</scope>\n" //
                + "            <exclusions>\n" //
                + "                <exclusion>\n" //
                + "                    <groupId>*</groupId>\n" //
                + "                    <artifactId>*</artifactId>\n" //
                + "                </exclusion>\n" //
                + "            </exclusions>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
                + "\n" //
                + "    <build/>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(
                        Transformation.updateDependencySubset(
                                gavtcs -> gavtcs.isVirtual(),
                                Collections.singleton(new Gavtcs("org.acme", "a2", "${project.version}").toVirtual()),
                                Gavtcs.scopeAndTypeFirstComparator(),
                                " initial comment ")),
                expected);
    }

    @Test
    void addDependencyTestAfterCompile() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencies>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>a1</artifactId>\n" //
                + "            <version>1.2.3</version>\n" //
                + "        </dependency>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>a2</artifactId>\n" //
                + "            <version>1.2.3</version>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
                + "\n" //
                + "    <build/>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencies>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>a1</artifactId>\n" //
                + "            <version>1.2.3</version>\n" //
                + "        </dependency>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>a2</artifactId>\n" //
                + "            <version>1.2.3</version>\n" //
                + "        </dependency>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>a1</artifactId>\n" //
                + "            <version>1.2.3</version>\n" //
                + "            <scope>test</scope>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
                + "\n" //
                + "    <build/>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(Transformation.addDependencyIfNeeded(Gavtcs.testJar("org.acme", "a1", "1.2.3"),
                        Gavtcs.scopeAndTypeFirstComparator())),
                expected);
    }

    @Test
    void addVirtualDependencyIfNeeded() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencies>\n" //
                + "    </dependencies>\n" //
                + "\n" //
                + "    <build/>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencies>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>a1</artifactId>\n" //
                + "            <version>1.2.3</version>\n" //
                + "            <type>pom</type>\n" //
                + "            <scope>test</scope>\n" //
                + "            <exclusions>\n" //
                + "                <exclusion>\n" //
                + "                    <groupId>*</groupId>\n" //
                + "                    <artifactId>*</artifactId>\n" //
                + "                </exclusion>\n" //
                + "            </exclusions>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
                + "\n" //
                + "    <build/>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(Transformation.addDependencyIfNeeded(Gavtcs.virtual("org.acme", "a1", "1.2.3"),
                        Gavtcs.scopeAndTypeFirstComparator())),
                expected);
    }

    @Test
    void importBom() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>bom</artifactId>\n" //
                + "                <version>${bom.version}</version>\n" //
                + "                <type>pom</type>\n" //
                + "                <scope>import</scope>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(
                        Transformation.addManagedDependency(Gavtcs.importBom("org.acme", "bom", "${bom.version}"))),
                expected);
    }

    @Test
    void trailingNewLine() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo />\n" //
                + "    </properties>\n" //
                + "\n" //
                + "</project>\n";

        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo />\n" //
                + "    </properties>\n" //
                + "\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.emptyList(),
                SimpleElementWhitespace.AUTODETECT_PREFER_EMPTY,
                expected);
    }

    @Test
    void trailingTwoNewLines() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo />\n" //
                + "    </properties>\n" //
                + "\n" //
                + "</project>\n\n";

        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo />\n" //
                + "    </properties>\n" //
                + "\n" //
                + "</project>\n\n";
        assertTransformation(source,
                Collections.emptyList(),
                SimpleElementWhitespace.AUTODETECT_PREFER_EMPTY,
                expected);
    }

    @Test
    void noTrailingWhiteSpace() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo />\n" //
                + "    </properties>\n" //
                + "\n" //
                + "</project>";

        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo />\n" //
                + "    </properties>\n" //
                + "\n" //
                + "</project>";
        assertTransformation(source,
                Collections.emptyList(),
                SimpleElementWhitespace.AUTODETECT_PREFER_EMPTY,
                expected);
    }

    @Test
    void trailingSpaces() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo />\n" //
                + "    </properties>\n" //
                + "\n" //
                + "</project>    ";

        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo />\n" //
                + "    </properties>\n" //
                + "\n" //
                + "</project>    ";
        assertTransformation(source,
                Collections.emptyList(),
                SimpleElementWhitespace.AUTODETECT_PREFER_EMPTY,
                expected);
    }

    @Test
    void trailingComment() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo />\n" //
                + "    </properties>\n" //
                + "\n" //
                + "</project><!-- trailing comment -->";

        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo>val</foo>\n" //
                + "    </properties>\n" //
                + "\n" //
                + "</project><!-- trailing comment -->";
        assertTransformation(source,
                Collections.singleton(Transformation.addOrSetProperty("foo", "val")),
                SimpleElementWhitespace.AUTODETECT_PREFER_EMPTY,
                expected);
    }

    @Test
    void trailingCommentOnNewline() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo />\n" //
                + "    </properties>\n" //
                + "\n" //
                + "</project>" //
                + "<!-- trailing comment -->";

        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo>val</foo>\n" //
                + "    </properties>\n" //
                + "\n" //
                + "</project><!-- trailing comment -->";
        assertTransformation(source,
                Collections.singleton(Transformation.addOrSetProperty("foo", "val")),
                SimpleElementWhitespace.AUTODETECT_PREFER_EMPTY,
                expected);
    }

    @Test
    void trailingMultilineComment() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo />\n" //
                + "    </properties>\n" //
                + "\n" //
                + "</project>\n"
                + "<!--\n"
                + "Modified by POM Manipulation Extension for Maven 4.5 ( SHA: 698c5e7b )\n"
                + "-->\n";

        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo>val</foo>\n" //
                + "    </properties>\n" //
                + "\n" //
                + "</project>\n"
                + "<!--\n"
                + "Modified by POM Manipulation Extension for Maven 4.5 ( SHA: 698c5e7b )\n"
                + "-->\n";
        assertTransformation(source,
                Collections.singleton(Transformation.addOrSetProperty("foo", "val")),
                SimpleElementWhitespace.AUTODETECT_PREFER_EMPTY,
                expected);
    }

    @Test
    void trailingMultilineCommentWithOtherComments() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo />\n" //
                + "    </properties>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>module1</module>\n" //
                + "        <!-- <module>module2</module> disabled by cq-prod-maven-plugin:prod-excludes -->\n" //
                + "        <module>module3</module>\n" //
                + "    </modules>\n" //
                + "\n" //
                + "</project>\n"
                + "<!--\n"
                + "Modified by POM Manipulation Extension for Maven 4.5 ( SHA: 698c5e7b )\n"
                + "-->\n";

        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo />\n" //
                + "    </properties>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "        <module>module1</module>\n" //
                + "        <module>module2</module>\n" //
                + "        <module>module3</module>\n" //
                + "    </modules>\n" //
                + "\n" //
                + "</project>\n"
                + "<!--\n"
                + "Modified by POM Manipulation Extension for Maven 4.5 ( SHA: 698c5e7b )\n"
                + "-->\n";
        assertTransformation(source,
                Collections.singleton(Transformation.uncommentModules("disabled by cq-prod-maven-plugin:prod-excludes")),
                SimpleElementWhitespace.AUTODETECT_PREFER_EMPTY,
                expected);
    }

    @Test
    void trailingMultilineComments() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo />\n" //
                + "    </properties>\n" //
                + "\n" //
                + "</project>\n"
                + "<!--\n"
                + "Modified by POM Manipulation Extension for Maven 4.5 ( SHA: 698c5e7b )\n"
                + "-->\n"
                + "<!--\n"
                + "Modified by POM Manipulation Extension for Maven 4.5 ( SHA: 698c5e7b )\n"
                + "-->\n";

        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo>val</foo>\n" //
                + "    </properties>\n" //
                + "\n" //
                + "</project>\n"
                + "<!--\n"
                + "Modified by POM Manipulation Extension for Maven 4.5 ( SHA: 698c5e7b )\n"
                + "-->\n"
                + "<!--\n"
                + "Modified by POM Manipulation Extension for Maven 4.5 ( SHA: 698c5e7b )\n"
                + "-->\n";
        assertTransformation(source,
                Collections.singleton(Transformation.addOrSetProperty("foo", "val")),
                SimpleElementWhitespace.AUTODETECT_PREFER_EMPTY,
                expected);
    }

    @Test
    void elementWhitespace() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo/>\n" //
                + "    </properties>\n" //
                + "\n" //
                + "</project>\n";

        {
            final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                    + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                    + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                    + "    <modelVersion>4.0.0</modelVersion>\n" //
                    + "    <groupId>org.acme</groupId>\n" //
                    + "    <artifactId>bom</artifactId>\n" //
                    + "    <version>0.1-SNAPSHOT</version>\n" //
                    + "    <packaging>pom</packaging>\n" //
                    + "\n" //
                    + "    <properties>\n" //
                    + "        <foo />\n" //
                    + "    </properties>\n" //
                    + "\n" //
                    + "</project>\n";
            assertTransformation(source,
                    Collections.emptyList(),
                    SimpleElementWhitespace.SPACE,
                    expected);
        }
        {
            final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                    + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                    + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                    + "    <modelVersion>4.0.0</modelVersion>\n" //
                    + "    <groupId>org.acme</groupId>\n" //
                    + "    <artifactId>bom</artifactId>\n" //
                    + "    <version>0.1-SNAPSHOT</version>\n" //
                    + "    <packaging>pom</packaging>\n" //
                    + "\n" //
                    + "    <properties>\n" //
                    + "        <foo/>\n" //
                    + "    </properties>\n" //
                    + "\n" //
                    + "</project>\n";
            assertTransformation(source,
                    Collections.emptyList(),
                    SimpleElementWhitespace.EMPTY,
                    expected);
        }
    }

    @Test
    void elementWhitespaceAutodetect() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo />\n" //
                + "    </properties>\n" //
                + "\n" //
                + "</project>\n";

        {
            final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                    + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                    + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                    + "    <modelVersion>4.0.0</modelVersion>\n" //
                    + "    <groupId>org.acme</groupId>\n" //
                    + "    <artifactId>bom</artifactId>\n" //
                    + "    <version>0.1-SNAPSHOT</version>\n" //
                    + "    <packaging>pom</packaging>\n" //
                    + "\n" //
                    + "    <properties>\n" //
                    + "        <foo />\n" //
                    + "        <bar />\n" //
                    + "    </properties>\n" //
                    + "\n" //
                    + "</project>\n";
            assertTransformation(source,
                    Collections.singletonList((Document document, TransformationContext context) -> {
                        final ContainerElement props = context.getOrAddContainerElement("properties");
                        props.addChildElement("bar", props.getOrAddLastIndent());
                    }),
                    SimpleElementWhitespace.AUTODETECT_PREFER_EMPTY,
                    expected);
        }
    }

    @Test
    void format() throws TransformerConfigurationException, TransformerException, TransformerFactoryConfigurationError {
        assertFormat("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <parent>\n" +
                "        <groupId>org.jboss</groupId>\n" +
                "        <artifactId>jboss-parent</artifactId>\n" +
                "        <version>35</version>\n" +
                "    </parent>\n" +
                "\n" +
                "    <groupId>io.quarkus</groupId>\n" +
                "    <artifactId>quarkus-parent</artifactId>\n" +
                "    <name>Quarkus - Parent pom</name>\n" +
                "    <version>999-SNAPSHOT</version>\n" +
                "    <packaging>pom</packaging>\n" +
                "</project>", "    ", "\n");
        assertFormat("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" +
                "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
                "    xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n"
                +
                "  <modelVersion>4.0.0</modelVersion>\r\n" +
                "</project>", "  ", "\r\n");
        assertFormat("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                +
                "    <!-- comment --><modelVersion>4.0.0</modelVersion>\n" +
                "</project>", "    ", "\n");
        assertFormat("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                +
                "\t<!-- comment --><modelVersion>4.0.0</modelVersion>\n" +
                "</project>", "\t", "\n");
    }

    @Test
    void keepFirst() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "    <dependencies>\n" //
                + "\n" //
                + "        <!-- The following dependencies guarantee that this module is built after them. You can update them by running `mvn process-resources -Pformat -N` from the source tree root directory -->\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>a1</artifactId>\n" //
                + "            <version>1.2.3</version>\n" //
                + "        </dependency>\n" //
                + "\n" //
                + "        <!-- The following dependencies guarantee that this module is built after them. You can update them by running `mvn process-resources -Pformat -N` from the source tree root directory -->\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>a2</artifactId>\n" //
                + "            <version>1.2.3</version>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "    <dependencies>\n" //
                + "\n" //
                + "        <!-- The following dependencies guarantee that this module is built after them. You can update them by running `mvn process-resources -Pformat -N` from the source tree root directory -->\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>a1</artifactId>\n" //
                + "            <version>1.2.3</version>\n" //
                + "        </dependency>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>a2</artifactId>\n" //
                + "            <version>1.2.3</version>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(
                        Transformation.keepFirst(
                                "//comment()[contains(.,' The following dependencies guarantee that this module is built after them. You can update them by running `mvn process-resources -Pformat -N` from the source tree root directory ')]",
                                true)),
                expected);
    }

    @Test
    void removeIfEmpty() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencyManagement>\n" //
                + "    </dependencyManagement>\n" //
                + "</project>\n";
        assertTransformation(source,
                Collections.singletonList(
                        Transformation.removeIfEmpty(true, true, "project", "dependencyManagement", "dependencies")),
                expected);
    }

    static void assertFormat(String xml, String expectedIndent, String expectedEol)
            throws TransformerConfigurationException, TransformerException, TransformerFactoryConfigurationError {
        final XPath xPath = XPathFactory.newInstance().newXPath();
        DOMResult result = new DOMResult();
        TransformerFactory.newInstance().newTransformer().transform(new StreamSource(new StringReader(xml)), result);
        final Node document = result.getNode();
        Assertions.assertEquals(expectedIndent, PomTransformer.detectIndentation(document, xPath));
        Assertions.assertEquals(expectedEol, PomTransformer.detectEol(xml));
    }

    @Test
    void endOfRootElement() {
        final String src = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <properties>\n" //
                + "        <foo />\n" //
                + "    </properties>\n" //
                + "\n" //
                + "</project>\n"
                + "<!--\n"
                + "Modified by POM Manipulation Extension for Maven 4.5 ( SHA: 698c5e7b )\n"
                + "-->\n";
        final int actual = PomTransformer.endOfRootElement(src);

        Assertions.assertEquals("\n"
                + "<!--\n"
                + "Modified by POM Manipulation Extension for Maven 4.5 ( SHA: 698c5e7b )\n"
                + "-->\n", src.substring(actual));
    }

    @ParameterizedTest
    @ValueSource(ints = {
            5, // a small number
            44, // max for AXP0801002: the compiler encountered an XPath expression containing '101' operators that
            // exceeds the '100' limit set by 'FEATURE_SECURE_PROCESSING' without any additional fix
            45, // 44+1
            2000 // a large number
    })
    void unlinkModules(int count) {
        final String sourceTemplate = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <modules>\n" //
                + "%s" //
                + "    </modules>\n" //
                + "</project>\n";
        final String sourceReplacement = intStream(count)
                .mapToObj(i -> String.format("        <module>module-%02d</module>\n", i))
                .collect(Collectors.joining(""));
        final String expectedReplacement = intStream(count)
                .mapToObj(i -> String.format("        <!-- <module>module-%02d</module> test remove -->\n", i))
                .collect(Collectors.joining(""));
        final Set<String> commentModules = intStream(count)
                .mapToObj(i -> String.format("module-%02d", i))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        final String source = String.format(sourceTemplate, sourceReplacement);
        final String expected = String.format(sourceTemplate, expectedReplacement);
        assertTransformation(source,
                Collections.singletonList(
                        Transformation.commentModules(commentModules, "test remove")),
                expected);
    }

    @Test
    void addScmUrlIfNeeded() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>bom</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <scm>\n"
                + "        <connection>scm:git:git@github.com:l2x6/pom-tuner.git</connection>\n"
                + "        <developerConnection>scm:git:git@github.com:l2x6/pom-tuner.git</developerConnection>\n"
                + "        <url>https://github.com/l2x6/pom-tuner</url>\n"
                + "        <tag>HEAD</tag>\n"
                + "    </scm>\n"
                + "</project>\n";
        String fullName = "l2x6/pom-tuner";
        assertTransformation(source, Collections.singletonList(
                (Document document, TransformationContext context) -> {
                    final ContainerElement scm = context.getOrAddContainerElement("scm");
                    scm.addOrSetChildTextElement("connection", String.format("scm:git:git@github.com:%s.git", fullName));
                    scm.addOrSetChildTextElement("developerConnection",
                            String.format("scm:git:git@github.com:%s.git", fullName));
                    scm.addOrSetChildTextElement("url", String.format("https://github.com/%s", fullName));
                    scm.addOrSetChildTextElement("tag", "HEAD");
                }),
                expected);

    }

    public IntStream intStream(int count) {
        return IntStream.range(1, count + 1);
    }
}
