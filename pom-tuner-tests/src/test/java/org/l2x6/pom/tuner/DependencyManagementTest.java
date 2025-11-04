package org.l2x6.pom.tuner;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.l2x6.pom.tuner.PomTransformer.SimpleElementWhitespace;
import org.l2x6.pom.tuner.PomTransformer.Transformer;
import org.l2x6.pom.tuner.model.Gavtcs;
import org.l2x6.pom.tuner.transform.api.ProfileId;
import org.l2x6.pom.tuner.transform.dependencies;
import org.l2x6.pom.tuner.transform.dependencyManagement;

public class DependencyManagementTest {

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
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <!-- foo -->\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep1</artifactId>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
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
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <!-- foo -->\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep1</artifactId>\n" //
                + "            </dependency>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep2</artifactId>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "</project>\n";
        assertTransformation(source, Arrays.asList(
                dependencyManagement.add(new Gavtcs("org.acme", "dep2", null))), expected);
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
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <!-- foo -->\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep1</artifactId>\n" //
                + "            </dependency>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep2</artifactId>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
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
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <!-- foo -->\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep1</artifactId>\n" //
                + "            </dependency>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep2</artifactId>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "</project>\n";
        assertTransformation(source, Arrays.asList(
                dependencyManagement.add(new Gavtcs("org.acme", "dep2", null))), expected);
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
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep2</artifactId>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "</project>\n";
        assertTransformation(source, Arrays.asList(
                dependencyManagement.add(new Gavtcs("org.acme", "dep2", null))), expected);
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
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <!-- foo -->\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep1</artifactId>\n" //
                + "            </dependency>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep3</artifactId>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
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
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <!-- foo -->\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep1</artifactId>\n" //
                + "            </dependency>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep2</artifactId>\n" //
                + "            </dependency>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep3</artifactId>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "</project>\n";
        assertTransformation(source, Arrays.asList(
                dependencyManagement.add(new Gavtcs("org.acme", "dep2", null))
                        .at(Comparators.after(new Gavtcs("org.acme", "dep1", null)))),
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
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <!-- foo -->\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep1</artifactId>\n" //
                + "            </dependency>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep3</artifactId>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
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
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <!-- foo -->\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep1</artifactId>\n" //
                + "            </dependency>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep2</artifactId>\n" //
                + "            </dependency>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep3</artifactId>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "</project>\n";
        assertTransformation(source, Arrays.asList(
                dependencyManagement.add(new Gavtcs("org.acme", "dep2", null))
                        .before(new Gavtcs("org.acme", "dep3", null))),
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
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <!-- foo -->\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep1</artifactId>\n" //
                + "            </dependency>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep3</artifactId>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
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
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <!-- foo -->\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep1</artifactId>\n" //
                + "            </dependency>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep2</artifactId>\n" //
                + "            </dependency>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep3</artifactId>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "</project>\n";
        assertTransformation(source, Arrays.asList(
                dependencyManagement.add(new Gavtcs("org.acme", "dep2", null))
                        .after(new Gavtcs("org.acme", "dep1", null))),
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
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <!-- foo -->\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep1</artifactId>\n" //
                + "                <version>1.2.3</version>\n" //
                + "            </dependency>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep2</artifactId>\n" //
                + "                <version>1.2.3</version>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "\n" //
                + "            <dependencyManagement>\n" //
                + "                <dependencies>\n" //
                + "                    <!-- foo -->\n" //
                + "                    <dependency>\n" //
                + "                        <groupId>org.acme</groupId>\n" //
                + "                        <artifactId>dep3</artifactId>\n" //
                + "                        <version>1.2.3</version>\n" //
                + "                    </dependency>\n" //
                + "                    <dependency>\n" //
                + "                        <groupId>org.acme</groupId>\n" //
                + "                        <artifactId>dep4</artifactId>\n" //
                + "                        <version>1.2.3</version>\n" //
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
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <!-- foo -->\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep1</artifactId>\n" //
                + "                <version>1.2.3</version>\n" //
                + "            </dependency>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep2</artifactId>\n" //
                + "                <version>1.2.3</version>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "\n" //
                + "            <dependencyManagement>\n" //
                + "                <dependencies>\n" //
                + "                    <!-- foo -->\n" //
                + "                    <dependency>\n" //
                + "                        <groupId>org.acme</groupId>\n" //
                + "                        <artifactId>dep3</artifactId>\n" //
                + "                        <version>1.2.3</version>\n" //
                + "                    </dependency>\n" //
                + "                    <dependency>\n" //
                + "                        <groupId>org.acme</groupId>\n" //
                + "                        <artifactId>dep5</artifactId>\n" //
                + "                        <version>1.2.3</version>\n" //
                + "                    </dependency>\n" //
                + "                    <dependency>\n" //
                + "                        <groupId>org.acme</groupId>\n" //
                + "                        <artifactId>dep4</artifactId>\n" //
                + "                        <version>1.2.3</version>\n" //
                + "                    </dependency>\n" //
                + "                </dependencies>\n" //
                + "            </dependencyManagement>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(

                dependencyManagement.add(new Gavtcs("org.acme", "dep5", "1.2.3"))
                .profile("profile1")
                .after(new Gavtcs("org.acme", "dep3", "1.2.3"))

        ),
                expected);
    }
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
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <!-- foo -->\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep1</artifactId>\n" //
                + "                <version>1.2.3</version>\n" //
                + "            </dependency>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep2</artifactId>\n" //
                + "                <version>1.2.3</version>\n" //
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
                + "                <artifactId>dep2</artifactId>\n" //
                + "                <version>1.2.3</version>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "</project>\n";
        assertTransformation(source, Arrays.asList(
                dependencyManagement.remove("org.acme:dep1")),

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
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <!-- foo -->\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep1</artifactId>\n" //
                + "                <version>1.2.3</version>\n" //
                + "            </dependency>\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep2</artifactId>\n" //
                + "                <version>1.2.3</version>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "\n" //
                + "            <dependencyManagement>\n" //
                + "                <dependencies>\n" //
                + "                    <!-- foo -->\n" //
                + "                    <dependency>\n" //
                + "                        <groupId>org.acme</groupId>\n" //
                + "                        <artifactId>dep3</artifactId>\n" //
                + "                        <version>1.2.3</version>\n" //
                + "                    </dependency>\n" //
                + "                    <dependency>\n" //
                + "                        <groupId>org.acme</groupId>\n" //
                + "                        <artifactId>dep4</artifactId>\n" //
                + "                        <version>1.2.3</version>\n" //
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
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "            <!-- foo -->\n" //
                + "            <dependency>\n" //
                + "                <groupId>org.acme</groupId>\n" //
                + "                <artifactId>dep1</artifactId>\n" //
                + "                <version>1.2.3</version>\n" //
                + "            </dependency>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "\n" //
                + "            <dependencyManagement>\n" //
                + "                <dependencies>\n" //
                + "                    <!-- foo -->\n" //
                + "                    <dependency>\n" //
                + "                        <groupId>org.acme</groupId>\n" //
                + "                        <artifactId>dep3</artifactId>\n" //
                + "                        <version>1.2.3</version>\n" //
                + "                    </dependency>\n" //
                + "                </dependencies>\n" //
                + "            </dependencyManagement>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(

                dependencyManagement.remove("org.acme:dep2", "org.acme:dep4").profiles("profile1")

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
                + "    <dependencyManagement>\n" //
                + "        <dependencies>\n" //
                + "        </dependencies>\n" //
                + "    </dependencyManagement>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "\n" //
                + "            <dependencyManagement>\n" //
                + "                <dependencies>\n" //
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
                + "    <artifactId>grand-parent</artifactId>\n" //
                + "    <version>0.1-SNAPSHOT</version>\n" //
                + "    <packaging>pom</packaging>\n" //
                + "\n" //
                + "    <dependencyManagement>\n" //
                + "    </dependencyManagement>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "\n" //
                + "            <dependencyManagement>\n" //
                + "            </dependencyManagement>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        assertTransformation(source, Collections.singletonList(

                dependencyManagement.removeEmptyParent().profiles(ProfileId.all())

        ),
                expected);
    }

    static void assertTransformation(String src, Collection<Transformer> transformations, String expected) {
        PomTransformer.transform(transformations, SimpleElementWhitespace.EMPTY, Paths.get("pom.xml"),
                () -> src, xml -> Assertions.assertThat(xml).isEqualTo(expected));
    }

}
