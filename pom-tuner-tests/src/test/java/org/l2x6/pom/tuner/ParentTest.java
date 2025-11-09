package org.l2x6.pom.tuner;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.l2x6.pom.tuner.PomTransformer.SimpleElementWhitespace;
import org.l2x6.pom.tuner.PomTransformer.TextElement;
import org.l2x6.pom.tuner.PomTransformer.Transformer;
import org.l2x6.pom.tuner.model.Gav;
import org.l2x6.pom.tuner.model.Gavtcs;
import org.l2x6.pom.tuner.transform.api.ProfileId;
import org.l2x6.pom.tuner.transform.dependencies;
import org.l2x6.pom.tuner.transform.parent;
import org.l2x6.pom.tuner.transform.dependencies;
import org.l2x6.pom.tuner.transform.properties;

public class ParentTest {

    @Test
    void setGavp() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "\n" //
                + "    <parent>\n" //
                + "        <groupId>org.acme</groupId>\n" //
                + "        <artifactId>parent</artifactId>\n" //
                + "        <version>0.1-SNAPSHOT</version>\n" //
                + "        <relativePath>../pom.xml</relativePath>\n" //
                + "    </parent>\n" //
                + "\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "\n" //
                + "    <parent>\n" //
                + "        <groupId>org.foo</groupId>\n" //
                + "        <artifactId>foo</artifactId>\n" //
                + "        <version>0.2-SNAPSHOT</version>\n" //
                + "        <relativePath>../../pom.xml</relativePath>\n" //
                + "    </parent>\n" //
                + "\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "</project>\n";
        assertTransformation(source, Arrays.asList(
                parent.set("org.foo", "foo", "0.2-SNAPSHOT", "../../pom.xml")), expected);
    }


    @Test
    void setGav() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "\n" //
                + "    <parent>\n" //
                + "        <groupId>org.acme</groupId>\n" //
                + "        <artifactId>parent</artifactId>\n" //
                + "        <version>0.1-SNAPSHOT</version>\n" //
                + "        <relativePath>../pom.xml</relativePath>\n" //
                + "    </parent>\n" //
                + "\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "\n" //
                + "    <parent>\n" //
                + "        <groupId>org.foo</groupId>\n" //
                + "        <artifactId>foo</artifactId>\n" //
                + "        <version>0.2-SNAPSHOT</version>\n" //
                + "        <relativePath>../pom.xml</relativePath>\n" //
                + "    </parent>\n" //
                + "\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "</project>\n";
        assertTransformation(source, Arrays.asList(
                parent.set("org.foo", "foo", "0.2-SNAPSHOT")), expected);
    }


    @Test
    void setParent() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "\n" //
                + "    <parent>\n" //
                + "        <groupId>org.acme</groupId>\n" //
                + "        <artifactId>parent</artifactId>\n" //
                + "        <version>0.1-SNAPSHOT</version>\n" //
                + "        <relativePath>../pom.xml</relativePath>\n" //
                + "    </parent>\n" //
                + "\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "\n" //
                + "    <parent>\n" //
                + "        <groupId>org.foo</groupId>\n" //
                + "        <artifactId>parent</artifactId>\n" //
                + "        <version>0.1-SNAPSHOT</version>\n" //
                + "        <relativePath>../pom.xml</relativePath>\n" //
                + "    </parent>\n" //
                + "\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "</project>\n";
        assertTransformation(source, Arrays.asList(
                parent.setGroupId("org.foo")), expected);
    }

    @Test
    void setArtifactId() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "\n" //
                + "    <parent>\n" //
                + "        <groupId>org.acme</groupId>\n" //
                + "        <artifactId>parent</artifactId>\n" //
                + "        <version>0.1-SNAPSHOT</version>\n" //
                + "        <relativePath>../pom.xml</relativePath>\n" //
                + "    </parent>\n" //
                + "\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "\n" //
                + "    <parent>\n" //
                + "        <groupId>org.acme</groupId>\n" //
                + "        <artifactId>foo</artifactId>\n" //
                + "        <version>0.1-SNAPSHOT</version>\n" //
                + "        <relativePath>../pom.xml</relativePath>\n" //
                + "    </parent>\n" //
                + "\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "</project>\n";
        assertTransformation(source, Arrays.asList(
                parent.setArtifactId("foo")), expected);
    }


    @Test
    void setVersion() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "\n" //
                + "    <parent>\n" //
                + "        <groupId>org.acme</groupId>\n" //
                + "        <artifactId>parent</artifactId>\n" //
                + "        <version>0.1-SNAPSHOT</version>\n" //
                + "        <relativePath>../pom.xml</relativePath>\n" //
                + "    </parent>\n" //
                + "\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "\n" //
                + "    <parent>\n" //
                + "        <groupId>org.acme</groupId>\n" //
                + "        <artifactId>parent</artifactId>\n" //
                + "        <version>0.2-SNAPSHOT</version>\n" //
                + "        <relativePath>../pom.xml</relativePath>\n" //
                + "    </parent>\n" //
                + "\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "</project>\n";
        assertTransformation(source, Arrays.asList(
                parent.setVersion("0.2-SNAPSHOT")), expected);
    }

    @Test
    void setRelativePath() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "\n" //
                + "    <parent>\n" //
                + "        <groupId>org.acme</groupId>\n" //
                + "        <artifactId>parent</artifactId>\n" //
                + "        <version>0.1-SNAPSHOT</version>\n" //
                + "        <relativePath>../pom.xml</relativePath>\n" //
                + "    </parent>\n" //
                + "\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "\n" //
                + "    <parent>\n" //
                + "        <groupId>org.acme</groupId>\n" //
                + "        <artifactId>parent</artifactId>\n" //
                + "        <version>0.1-SNAPSHOT</version>\n" //
                + "        <relativePath>../../pom.xml</relativePath>\n" //
                + "    </parent>\n" //
                + "\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "</project>\n";
        assertTransformation(source, Arrays.asList(
                parent.setRelativePath("../../pom.xml")), expected);
    }

    @Test
    void removeRelativePath() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "\n" //
                + "    <parent>\n" //
                + "        <groupId>org.acme</groupId>\n" //
                + "        <artifactId>parent</artifactId>\n" //
                + "        <version>0.1-SNAPSHOT</version>\n" //
                + "        <relativePath>../pom.xml</relativePath>\n" //
                + "    </parent>\n" //
                + "\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "\n" //
                + "    <parent>\n" //
                + "        <groupId>org.acme</groupId>\n" //
                + "        <artifactId>parent</artifactId>\n" //
                + "        <version>0.1-SNAPSHOT</version>\n" //
                + "    </parent>\n" //
                + "\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "</project>\n";
        assertTransformation(source, Arrays.asList(
                parent.removeRelativePath()), expected);
    }

    @Test
    void remove() {
        final String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "\n" //
                + "    <parent>\n" //
                + "        <groupId>org.acme</groupId>\n" //
                + "        <artifactId>parent</artifactId>\n" //
                + "        <version>0.1-SNAPSHOT</version>\n" //
                + "        <relativePath>../pom.xml</relativePath>\n" //
                + "    </parent>\n" //
                + "\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "</project>\n";
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "    <modelVersion>4.0.0</modelVersion>\n" //
                + "\n" //
                + "    <groupId>org.acme</groupId>\n" //
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "</project>\n";
        assertTransformation(source, Arrays.asList(
                parent.remove()), expected);
    }
    static void assertTransformation(String src, Collection<Transformer> transformations, String expected) {
        PomTransformer.transform(transformations, SimpleElementWhitespace.EMPTY, Paths.get("pom.xml"),
                () -> src, xml -> Assertions.assertThat(xml).isEqualTo(expected));
    }

}
