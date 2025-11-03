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
import org.l2x6.pom.tuner.transform.plugins;

public class PluginsTest {
    @Test
    void remove() {
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
                + "            <!-- foo -->\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep1</artifactId>\n" //
                + "                <version>1.2.3</version>\n" //
                + "            </plugin>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep2</artifactId>\n" //
                + "                <version>1.2.3</version>\n" //
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
                + "                <artifactId>dep2</artifactId>\n" //
                + "                <version>1.2.3</version>\n" //
                + "            </plugin>\n" //
                + "        </plugins>\n" //
                + "    </build>\n" //
                + "</project>\n";
        assertTransformation(source, Arrays.asList(
                plugins.remove("org.acme:dep1")),

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
                + "    <build>\n" //
                + "        <plugins>\n" //
                + "            <!-- foo -->\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep1</artifactId>\n" //
                + "                <version>1.2.3</version>\n" //
                + "            </plugin>\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep2</artifactId>\n" //
                + "                <version>1.2.3</version>\n" //
                + "            </plugin>\n" //
                + "        </plugins>\n" //
                + "    </build>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "\n" //
                + "            <build>\n" //
                + "                <plugins>\n" //
                + "                    <!-- foo -->\n" //
                + "                    <plugin>\n" //
                + "                        <groupId>org.acme</groupId>\n" //
                + "                        <artifactId>dep3</artifactId>\n" //
                + "                        <version>1.2.3</version>\n" //
                + "                    </plugin>\n" //
                + "                    <plugin>\n" //
                + "                        <groupId>org.acme</groupId>\n" //
                + "                        <artifactId>dep4</artifactId>\n" //
                + "                        <version>1.2.3</version>\n" //
                + "                    </plugin>\n" //
                + "                </plugins>\n" //
                + "            </build>\n" //
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
                + "    <build>\n" //
                + "        <plugins>\n" //
                + "            <!-- foo -->\n" //
                + "            <plugin>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep1</artifactId>\n" //
                + "                <version>1.2.3</version>\n" //
                + "            </plugin>\n" //
                + "        </plugins>\n" //
                + "    </build>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "\n" //
                + "            <build>\n" //
                + "                <plugins>\n" //
                + "                    <!-- foo -->\n" //
                + "                    <plugin>\n" //
                + "                        <groupId>org.acme</groupId>\n" //
                + "                        <artifactId>dep3</artifactId>\n" //
                + "                        <version>1.2.3</version>\n" //
                + "                    </plugin>\n" //
                + "                </plugins>\n" //
                + "            </build>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(

                plugins.remove("org.acme:dep2", "org.acme:dep4").profiles("profile1")

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
                + "    <build>\n" //
                + "        <plugins>\n" //
                + "        </plugins>\n" //
                + "    </build>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "\n" //
                + "            <build>\n" //
                + "                <plugins>\n" //
                + "                </plugins>\n" //
                + "            </build>\n" //
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
                + "    <build>\n" //
                + "    </build>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "\n" //
                + "            <build>\n" //
                + "            </build>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(

                plugins.removeEmptyParent().profiles(ProfileId.all())

        ),
                expected);
    }

    static void assertTransformation(String src, Collection<Transformer> transformations, String expected) {
        PomTransformer.transform(transformations, SimpleElementWhitespace.EMPTY, Paths.get("pom.xml"),
                () -> src, xml -> Assertions.assertThat(xml).isEqualTo(expected));
    }

}
