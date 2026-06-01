package org.l2x6.pom.tuner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.l2x6.pom.tuner.transform.ProfileId;
import org.l2x6.pom.tuner.transform.Properties;
import org.l2x6.pom.tuner.transform.Siblings;

public class PropertiesTest {

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
                + "    <properties>\n" //
                + "        <p1>v1</p1>\n" //
                + "        <!-- foo -->\n" //
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
                + "        <p1>v1</p1>\n" //
                + "        <!-- foo -->\n" //
                + "        <p2>v2</p2>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Properties.set("p2", "v2")), expected);
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
                + "    <properties>\n" //
                + "        <p2>v2</p2>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Properties.set("p2", "v2")), expected);
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
                + "    <properties>\n" //
                + "        <p1>v1</p1>\n" //
                + "        <p2>v2</p2>\n" //
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
                + "        <p1>v1</p1>\n" //
                + "        <p3>v3</p3>\n" //
                + "        <p2>v2</p2>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Properties.set("p3", "v3")
                        .at(Comparator.comparing(Map.Entry::getKey, Comparators.after("p1")))),
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
                + "    <properties>\n" //
                + "        <p1>v1</p1>\n" //
                + "        <p2>v2</p2>\n" //
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
                + "        <p1>v1</p1>\n" //
                + "        <p3>v3</p3>\n" //
                + "        <p2>v2</p2>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Properties.set("p3", "v3")
                        .afterElement("p1")),
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
                + "    <properties>\n" //
                + "        <p1>v1</p1>\n" //
                + "        <p2>v2</p2>\n" //
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
                + "        <p1>v1</p1>\n" //
                + "        <p3>v3</p3>\n" //
                + "        <p2>v2</p2>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Properties.set("p3", "v3")
                        .beforeElement("p2")),
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
                + "    <properties>\n" //
                + "        <p1>v1</p1>\n" //
                + "        <p2>v2</p2>\n" //
                + "    </properties>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "            <properties>\n" //
                + "                <p4>v4</p4>\n" //
                + "                <!-- comment -->\n" //
                + "                <p5>v5</p5>\n" //
                + "                <p6>v6</p6>\n" //
                + "            </properties>\n" //
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
                + "    <properties>\n" //
                + "        <p1>v1</p1>\n" //
                + "        <p2>v2</p2>\n" //
                + "    </properties>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "            <properties>\n" //
                + "                <p4>v4</p4>\n" //
                + "                <p7>v7</p7>\n" //
                + "                <!-- comment -->\n" //
                + "                <p5>v5</p5>\n" //
                + "                <p6>v6</p6>\n" //
                + "            </properties>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Collections.singletonList(

                Properties.set("p7", "v7").intoProfile("profile1").afterElement("p4")

        ),
                expected);
    }

    @Test
    void set() {
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
                + "        <p1>v1</p1>\n" //
                + "        <p2>v2</p2>\n" //
                + "        <p3>v3</p3>\n" //
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
                + "        <p1>v1</p1>\n" //
                + "        <p2>v4</p2>\n" //
                + "        <p3>v3</p3>\n" //
                + "    </properties>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Properties.set("p2", "v4")), expected);
    }

    @Test
    void removeByNames() {
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
                + "        <p1>v1</p1>\n" //
                + "        <!-- foo -->\n" //
                + "        <p2>v2</p2>\n" //
                + "        <!-- bar -->\n" //
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
                + "        <p1>v1</p1>\n" //
                + "        <!-- bar -->\n" //
                + "    </properties>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Properties.remove("p2")), expected);
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
                + "    <properties>\n" //
                + "        <p1>v1</p1>\n" //
                + "        <!-- foo -->\n" //
                + "        <p2>v2</p2>\n" //
                + "        <!-- bar -->\n" //
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
                + "        <p1>v1</p1>\n" //
                + "        <!-- foo -->\n" //
                + "    </properties>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Properties
                        .remove(te -> te.getTextContent().equals("v2"))
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
                + "    <properties>\n" //
                + "        <!-- baz -->\n" //
                + "        <p1>v1</p1>\n" //
                + "        <!-- foo -->\n" //
                + "        <p2>v2</p2>\n" //
                + "        <!-- bar -->\n" //
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
                + "    </properties>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Properties
                        .remove("p1", "p2")
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
                + "    <properties>\n" //
                + "        <p1>v1</p1>\n" //
                + "        <p2>v2</p2>\n" //
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
                + "    <!-- foo -->\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Properties
                        .removeAll()
                        .alsoRemoveNone()
                        .alsoRemovePrevious(Siblings.whitespace())),
                expected);
    }

    @Test
    void removeProfiles() {
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
                + "        <p1>v1</p1>\n" //
                + "        <!-- comment -->\n" //
                + "        <p2>v2</p2>\n" //
                + "    </properties>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "            <properties>\n" //
                + "                <p4>v4</p4>\n" //
                + "                <!-- comment -->\n" //
                + "                <p5>v5</p5>\n" //
                + "                <p6>v6</p6>\n" //
                + "            </properties>\n" //
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
                + "    <properties>\n" //
                + "        <p1>v1</p1>\n" //
                + "    </properties>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "            <properties>\n" //
                + "                <p4>v4</p4>\n" //
                + "                <p6>v6</p6>\n" //
                + "            </properties>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Collections.singletonList(

                Properties.remove("p2", "p5").from("profile1")

        ),
                expected);
    }

    @Test
    void removeProfilesOnly() {
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
                + "        <p1>v1</p1>\n" //
                + "        <!-- comment -->\n" //
                + "        <p2>v2</p2>\n" //
                + "    </properties>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "            <properties>\n" //
                + "                <p4>v4</p4>\n" //
                + "                <!-- comment -->\n" //
                + "                <p5>v5</p5>\n" //
                + "                <p6>v6</p6>\n" //
                + "            </properties>\n" //
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
                + "    <properties>\n" //
                + "        <p1>v1</p1>\n" //
                + "        <!-- comment -->\n" //
                + "        <p2>v2</p2>\n" //
                + "    </properties>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "            <properties>\n" //
                + "                <p4>v4</p4>\n" //
                + "                <p6>v6</p6>\n" //
                + "            </properties>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Collections.singletonList(

                Properties.remove("p2", "p5").fromProfilesOnly("profile1")

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
                + "    <properties>\n" //
                + "    </properties>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "            <properties>\n" //
                + "            </properties>\n" //
                + "        </profile>\n" //
                + "        <profile>\n" //
                + "            <id>profile2</id>\n" //
                + "            <properties>\n" //
                + "                <foo>bar</foo>\n" //
                + "            </properties>\n" //
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
                + "            <properties>\n" //
                + "                <foo>bar</foo>\n" //
                + "            </properties>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Collections.singletonList(

                Properties.removeEmptyParent().from(ProfileId.all())

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
                + "    <properties>\n" //
                + "        <prop1>val-1</prop1>\n" //
                + "        <!-- foo -->\n" //
                + "        <prop3>val-3</prop3>\n" //
                + "    </properties>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "        </profile>\n" //
                + "        <profile>\n" //
                + "            <id>profile2</id>\n" //
                + "            <properties>\n" //
                + "                <prop4>val-4</prop4>\n" //
                + "            </properties>\n" //
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
                    + "    <properties>\n" //
                    + "        <prop1>val-1-mod</prop1>\n" //
                    + "        <!-- foo -->\n" //
                    + "        <prop3>val-3-mod</prop3>\n" //
                    + "    </properties>\n" //
                    + "    <profiles>\n" //
                    + "        <profile>\n" //
                    + "            <id>profile1</id>\n" //
                    + "        </profile>\n" //
                    + "        <profile>\n" //
                    + "            <id>profile2</id>\n" //
                    + "            <properties>\n" //
                    + "                <prop4>val-4</prop4>\n" //
                    + "            </properties>\n" //
                    + "        </profile>\n" //
                    + "    </profiles>\n" //
                    + "</project>\n";
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    Properties.select(p -> true).modify(te -> te.setTextContent(te.getTextContent() + "-mod"))),
                    expected);
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    Properties.select(p -> true).modifyTextContent(old -> old + "-mod")),
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
                    + "    <properties>\n" //
                    + "        <prop1>val-1-mod</prop1>\n" //
                    + "        <!-- foo -->\n" //
                    + "        <prop3>val-3</prop3>\n" //
                    + "    </properties>\n" //
                    + "    <profiles>\n" //
                    + "        <profile>\n" //
                    + "            <id>profile1</id>\n" //
                    + "        </profile>\n" //
                    + "        <profile>\n" //
                    + "            <id>profile2</id>\n" //
                    + "            <properties>\n" //
                    + "                <prop4>val-4</prop4>\n" //
                    + "            </properties>\n" //
                    + "        </profile>\n" //
                    + "    </profiles>\n" //
                    + "</project>\n";
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    Properties.selectByValue(p -> p.equals("val-1"))
                            .modify(te -> te.setTextContent(te.getTextContent() + "-mod"))),
                    expected);
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    Properties.selectByValue("val-1").modify(te -> te.setTextContent(te.getTextContent() + "-mod"))),
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
                    + "    <properties>\n" //
                    + "        <prop1>val-1-mod</prop1>\n" //
                    + "        <!-- foo -->\n" //
                    + "        <prop3>val-3-mod</prop3>\n" //
                    + "    </properties>\n" //
                    + "    <profiles>\n" //
                    + "        <profile>\n" //
                    + "            <id>profile1</id>\n" //
                    + "        </profile>\n" //
                    + "        <profile>\n" //
                    + "            <id>profile2</id>\n" //
                    + "            <properties>\n" //
                    + "                <prop4>val-4-mod</prop4>\n" //
                    + "            </properties>\n" //
                    + "        </profile>\n" //
                    + "    </profiles>\n" //
                    + "</project>\n";
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    Properties.selectAll()
                            .from(ProfileId.all())
                            .modify(te -> te.setTextContent(te.getTextContent() + "-mod"))),
                    expected);
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    Properties.select(p -> true)
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
                    + "    <properties>\n" //
                    + "        <prop1>val-1</prop1>\n" //
                    + "        <!-- foo -->\n" //
                    + "        <prop3>val-3</prop3>\n" //
                    + "    </properties>\n" //
                    + "    <profiles>\n" //
                    + "        <profile>\n" //
                    + "            <id>profile1</id>\n" //
                    + "        </profile>\n" //
                    + "        <profile>\n" //
                    + "            <id>profile2</id>\n" //
                    + "            <properties>\n" //
                    + "                <!-- <prop4>val-4</prop4> comment -->\n" //
                    + "            </properties>\n" //
                    + "        </profile>\n" //
                    + "    </profiles>\n" //
                    + "</project>\n";
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    Properties.select(p -> true)
                            .fromProfilesOnly("profile2").commentOut(t -> "comment")),
                    expected);
        }
    }

}
