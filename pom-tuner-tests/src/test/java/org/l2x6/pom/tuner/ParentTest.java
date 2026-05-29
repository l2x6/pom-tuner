package org.l2x6.pom.tuner;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.l2x6.pom.tuner.PomTransformer.Transformer;
import org.l2x6.pom.tuner.transform.Parent;

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
                Parent.set("org.foo", "foo", "0.2-SNAPSHOT", "../../pom.xml")), expected);
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
                Parent.set("org.foo", "foo", "0.2-SNAPSHOT")), expected);
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
                Parent.setGroupId("org.foo")), expected);
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
                Parent.setArtifactId("foo")), expected);
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
                Parent.setVersion("0.2-SNAPSHOT")), expected);
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
                Parent.setRelativePath("../../pom.xml")), expected);
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
                Parent.removeRelativePath()), expected);
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
                Parent.remove()), expected);
    }

    static void assertTransformation(String src, Collection<Transformer> transformations, String expected) {
        PomTransformer.transform(transformations, Paths.get("pom.xml"),
                () -> src, xml -> Assertions.assertThat(xml).isEqualTo(expected));
    }

}
