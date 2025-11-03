package org.l2x6.pom.tuner;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.l2x6.pom.tuner.PomTransformer.SimpleElementWhitespace;
import org.l2x6.pom.tuner.PomTransformer.Transformer;
import org.l2x6.pom.tuner.transform.api.ProfileId;
import org.l2x6.pom.tuner.transform.api.Siblings;
import org.l2x6.pom.tuner.transform.properties;

public class PropertiesTest {
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
        assertTransformation(source, Arrays.asList(
                properties.remove("p2")), expected);
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
                + "        </properties>\n" //
                + "</project>\n";
        assertTransformation(source, Arrays.asList(
                properties
                        .remove(te -> te.getText().equals("v2"))
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
                + "    <properties/>\n" //
                + "</project>\n";
        assertTransformation(source, Arrays.asList(
                properties
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
        assertTransformation(source, Arrays.asList(
                properties
                        .removeAll()
                        .alsoRemoveNone()
                        .alsoRemovePrevious(Siblings.whitespace())),
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
        assertTransformation(source, Collections.singletonList(

                properties.remove("p2", "p5").profiles("profile1")

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
        assertTransformation(source, Collections.singletonList(

                properties.remove("p2", "p5").profilesOnly("profile1")

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
        assertTransformation(source, Collections.singletonList(

                properties.removeEmptyParent().profiles(ProfileId.all())

        ),
                expected);
    }

    static void assertTransformation(String src, Collection<Transformer> transformations, String expected) {
        PomTransformer.transform(transformations, SimpleElementWhitespace.EMPTY, Paths.get("pom.xml"),
                () -> src, xml -> Assertions.assertThat(xml).isEqualTo(expected));
    }

}
