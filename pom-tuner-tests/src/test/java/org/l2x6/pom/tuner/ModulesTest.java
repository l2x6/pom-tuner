package org.l2x6.pom.tuner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.l2x6.pom.tuner.transform.Modules;
import org.l2x6.pom.tuner.transform.ProfileId;
import org.l2x6.pom.tuner.transform.Siblings;

public class ModulesTest {

    @Test
    void add() {
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
                + "        <!-- foo -->\n" //
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
                + "        <!-- foo -->\n" //
                + "        <module>module-2</module>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Modules.add("module-2")), expected);
    }

    @Test
    void addIdempotent() {
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
                + "        <!-- foo -->\n" //
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
                + "        <!-- foo -->\n" //
                + "        <module>module-2</module>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Modules.add("module-2")), expected);
    }

    @Test
    void addNoParent() {
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
                + "        <module>module-2</module>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Modules.add("module-2")), expected);
    }

    @Test
    void addAt() {
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
                + "        <!-- foo -->\n" //
                + "        <module>module-3</module>\n" //
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
                + "        <module>module-2</module>\n" //
                + "        <!-- foo -->\n" //
                + "        <module>module-3</module>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Modules.add("module-2")
                        .at(Comparator.comparing(Map.Entry::getValue, Comparators.after("module-1")))),
                expected);
    }

    @Test
    void addBefore() {
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
                + "        <!-- foo -->\n" //
                + "        <module>module-3</module>\n" //
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
                + "        <module>module-2</module>\n" //
                + "        <!-- foo -->\n" //
                + "        <module>module-3</module>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Modules.add("module-2")
                        .beforeTextContent("module-3")),
                expected);
    }

    @Test
    void addAfter() {
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
                + "        <!-- foo -->\n" //
                + "        <module>module-3</module>\n" //
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
                + "        <module>module-2</module>\n" //
                + "        <!-- foo -->\n" //
                + "        <module>module-3</module>\n" //
                + "    </modules>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Modules.add("module-2")
                        .afterTextContent("module-1")),
                expected);
    }

    @Test
    void addProfile() {
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
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "            <modules>\n" //
                + "                <module>module-4</module>\n" //
                + "                <module>module-5</module>\n" //
                + "                <!-- foo -->\n" //
                + "                <module>module-6</module>\n" //
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
                + "        <module>module-2</module>\n" //
                + "    </modules>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "            <modules>\n" //
                + "                <module>module-4</module>\n" //
                + "                <module>module-7</module>\n" //
                + "                <module>module-5</module>\n" //
                + "                <!-- foo -->\n" //
                + "                <module>module-6</module>\n" //
                + "            </modules>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Collections.singletonList(

                Modules.add("module-7").intoProfile("profile1").afterTextContent("module-4")

        ),
                expected);
    }

    @Test
    void removeByPaths() {
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
                + "        <!-- foo -->\n" //
                + "        <module>module-2</module>\n" //
                + "        <!-- bar -->\n" //
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
                + "        <!-- bar -->\n" //
                + "    </modules>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Modules.remove("module-2")), expected);
    }

    @Test
    void removeBySelectorAlsoRemoveNext() {
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
                + "        <!-- foo -->\n" //
                + "        <module>module-2</module>\n" //
                + "        <!-- bar -->\n" //
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
                + "        <!-- foo -->\n" //
                + "    </modules>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Modules
                        .remove(te -> te.getTextContent().equals("module-2"))
                        .alsoRemoveNone()
                        .alsoRemoveNext(Siblings.commentsOrWhitespace())),
                expected);
    }

    @Test
    void removeAlsoRemoveAll() {
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
                + "        <!-- baz -->\n" //
                + "        <module>module-1</module>\n" //
                + "        <!-- foo -->\n" //
                + "        <module>module-2</module>\n" //
                + "        <!-- bar -->\n" //
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
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Modules
                        .remove("module-1", "module-2")
                        .alsoRemoveNext(Siblings.commentsOrWhitespace())),
                expected);
    }

    @Test
    void removeAllAlsoRemovePreviousWs() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <!-- foo -->\n" //
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
                + "    <!-- foo -->\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Modules
                        .removeAll()
                        .alsoRemoveNone()
                        .alsoRemovePrevious(Siblings.whitespace())),
                expected);
    }

    @Test
    void removeWithComment() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "    xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
                + "    xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + "    <parent>\n"
                + "        <groupId>org.apache.camel.quarkus</groupId>\n"
                + "        <artifactId>camel-quarkus-parent</artifactId>\n"
                + "        <version>0.1-SNAPSHOT</version>\n"
                + "    </parent>\n"
                + "\n"
                + "    <artifactId>camel-quarkus-extensions-jvm</artifactId>\n"
                + "\n"
                + "    <modules>\n"
                + "        <!-- extensions a..z; do not remove this comment, it is important when sorting via  mvn process-resources -Pformat -->\n"
                + "        <module>foo</module>\n"
                + "    </modules>\n"
                + "\n"
                + "</project>";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "    xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
                + "    xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + "    <parent>\n"
                + "        <groupId>org.apache.camel.quarkus</groupId>\n"
                + "        <artifactId>camel-quarkus-parent</artifactId>\n"
                + "        <version>0.1-SNAPSHOT</version>\n"
                + "    </parent>\n"
                + "\n"
                + "    <artifactId>camel-quarkus-extensions-jvm</artifactId>\n"
                + "\n"
                + "    <modules>\n"
                + "    </modules>\n"
                + "\n"
                + "</project>";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Modules.remove("foo")),
                expected);
    }

    @Test
    void profiles() {
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
                + "    </modules>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "            <modules>\n" //
                + "                <module>module-4</module>\n" //
                + "                <module>../../module-6</module>\n" //
                + "            </modules>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Collections.singletonList(

                Modules.remove("module-2", "module-5").from("profile1")

        ),
                expected);
    }

    @Test
    void profilesOnly() {
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
                + "                <module>module-4</module>\n" //
                + "                <module>../../module-6</module>\n" //
                + "            </modules>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Collections.singletonList(

                Modules.remove("module-2", "module-5").fromProfilesOnly("profile1")

        ),
                expected);
    }

    @Test
    void removeEmptyParent() {
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
                + "    </modules>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "            <modules>\n" //
                + "            </modules>\n" //
                + "        </profile>\n" //
                + "        <profile>\n" //
                + "            <id>profile2</id>\n" //
                + "            <modules>\n" //
                + "                <module>bar</module>\n" //
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
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "        </profile>\n" //
                + "        <profile>\n" //
                + "            <id>profile2</id>\n" //
                + "            <modules>\n" //
                + "                <module>bar</module>\n" //
                + "            </modules>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Collections.singletonList(

                Modules.removeEmptyParent().from(ProfileId.all())

        ),
                expected);
    }

    @Test
    void modify() {
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
                + "        <!-- foo -->\n" //
                + "        <module>module-3</module>\n" //
                + "    </modules>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "        </profile>\n" //
                + "        <profile>\n" //
                + "            <id>profile2</id>\n" //
                + "            <modules>\n" //
                + "                <module>bar</module>\n" //
                + "            </modules>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        {
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
                    + "        <module>module-1-mod</module>\n" //
                    + "        <!-- foo -->\n" //
                    + "        <module>module-3-mod</module>\n" //
                    + "    </modules>\n" //
                    + "    <profiles>\n" //
                    + "        <profile>\n" //
                    + "            <id>profile1</id>\n" //
                    + "        </profile>\n" //
                    + "        <profile>\n" //
                    + "            <id>profile2</id>\n" //
                    + "            <modules>\n" //
                    + "                <module>bar</module>\n" //
                    + "            </modules>\n" //
                    + "        </profile>\n" //
                    + "    </profiles>\n" //
                    + "</project>\n";
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    Modules.select(p -> true).modify(te -> te.setTextContent(te.getTextContent() + "-mod"))),
                    expected);
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    Modules.select(p -> true).modifyTextContent(old -> old + "-mod")),
                    expected);
        }
        {
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
                    + "        <module>module-1-mod</module>\n" //
                    + "        <!-- foo -->\n" //
                    + "        <module>module-3</module>\n" //
                    + "    </modules>\n" //
                    + "    <profiles>\n" //
                    + "        <profile>\n" //
                    + "            <id>profile1</id>\n" //
                    + "        </profile>\n" //
                    + "        <profile>\n" //
                    + "            <id>profile2</id>\n" //
                    + "            <modules>\n" //
                    + "                <module>bar</module>\n" //
                    + "            </modules>\n" //
                    + "        </profile>\n" //
                    + "    </profiles>\n" //
                    + "</project>\n";
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    Modules.select(p -> p.equals("module-1")).modify(te -> te.setTextContent(te.getTextContent() + "-mod"))),
                    expected);
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    Modules.select("module-1").modify(te -> te.setTextContent(te.getTextContent() + "-mod"))),
                    expected);
        }
        {
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
                    + "        <module>module-1-mod</module>\n" //
                    + "        <!-- foo -->\n" //
                    + "        <module>module-3-mod</module>\n" //
                    + "    </modules>\n" //
                    + "    <profiles>\n" //
                    + "        <profile>\n" //
                    + "            <id>profile1</id>\n" //
                    + "        </profile>\n" //
                    + "        <profile>\n" //
                    + "            <id>profile2</id>\n" //
                    + "            <modules>\n" //
                    + "                <module>bar-mod</module>\n" //
                    + "            </modules>\n" //
                    + "        </profile>\n" //
                    + "    </profiles>\n" //
                    + "</project>\n";
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    Modules.selectAll()
                            .from(ProfileId.all())
                            .modify(te -> te.setTextContent(te.getTextContent() + "-mod"))),
                    expected);
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    Modules.select(p -> true)
                            .from("profile1", "profile2").modify(te -> te.setTextContent(te.getTextContent() + "-mod"))),
                    expected);
        }

        {
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
                    + "        <!-- foo -->\n" //
                    + "        <module>module-3</module>\n" //
                    + "    </modules>\n" //
                    + "    <profiles>\n" //
                    + "        <profile>\n" //
                    + "            <id>profile1</id>\n" //
                    + "        </profile>\n" //
                    + "        <profile>\n" //
                    + "            <id>profile2</id>\n" //
                    + "            <modules>\n" //
                    + "                <!-- <module>bar</module> comment -->\n" //
                    + "            </modules>\n" //
                    + "        </profile>\n" //
                    + "    </profiles>\n" //
                    + "</project>\n";
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    Modules.select(p -> true)
                            .fromProfilesOnly("profile2").commentOut(t -> "comment")),
                    expected);
        }
    }

    @Test
    void commentOut() {
    }

}
