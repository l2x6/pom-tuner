package org.l2x6.pom.tuner;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.l2x6.pom.tuner.model.Gavtcs;
import org.l2x6.pom.tuner.model.GavtcsSet;
import org.l2x6.pom.tuner.transform.Dependencies;
import org.l2x6.pom.tuner.transform.ProfileId;

public class DependenciesTest {

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
                + "    <dependencies>\n" //
                + "        <!-- foo -->\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep1</artifactId>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
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
                + "    <dependencies>\n" //
                + "        <!-- foo -->\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep1</artifactId>\n" //
                + "        </dependency>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep2</artifactId>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Dependencies.add(new Gavtcs("org.acme", "dep2", null))), expected);
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
                + "    <dependencies>\n" //
                + "        <!-- foo -->\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep1</artifactId>\n" //
                + "        </dependency>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep2</artifactId>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
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
                + "    <dependencies>\n" //
                + "        <!-- foo -->\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep1</artifactId>\n" //
                + "        </dependency>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep2</artifactId>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Dependencies.add(new Gavtcs("org.acme", "dep2", null))), expected);
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
                + "    <dependencies>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep2</artifactId>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Dependencies.add(new Gavtcs("org.acme", "dep2", null))), expected);
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
                + "    <dependencies>\n" //
                + "        <!-- foo -->\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep1</artifactId>\n" //
                + "        </dependency>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep3</artifactId>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
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
                + "    <dependencies>\n" //
                + "        <!-- foo -->\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep1</artifactId>\n" //
                + "        </dependency>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep2</artifactId>\n" //
                + "        </dependency>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep3</artifactId>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Dependencies.add(new Gavtcs("org.acme", "dep2", null))
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
                + "    <dependencies>\n" //
                + "        <!-- foo -->\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep1</artifactId>\n" //
                + "        </dependency>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep3</artifactId>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
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
                + "    <dependencies>\n" //
                + "        <!-- foo -->\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep1</artifactId>\n" //
                + "        </dependency>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep2</artifactId>\n" //
                + "        </dependency>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep3</artifactId>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Dependencies.add(new Gavtcs("org.acme", "dep2", null))
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
                + "    <dependencies>\n" //
                + "        <!-- foo -->\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep1</artifactId>\n" //
                + "        </dependency>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep3</artifactId>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
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
                + "    <dependencies>\n" //
                + "        <!-- foo -->\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep1</artifactId>\n" //
                + "        </dependency>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep2</artifactId>\n" //
                + "        </dependency>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep3</artifactId>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Dependencies.add(new Gavtcs("org.acme", "dep2", null))
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
                + "    <dependencies>\n" //
                + "        <!-- foo -->\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep1</artifactId>\n" //
                + "            <version>1.2.3</version>\n" //
                + "        </dependency>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep2</artifactId>\n" //
                + "            <version>1.2.3</version>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "\n" //
                + "            <dependencies>\n" //
                + "                <!-- foo -->\n" //
                + "                <dependency>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep3</artifactId>\n" //
                + "                    <version>1.2.3</version>\n" //
                + "                </dependency>\n" //
                + "                <dependency>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep4</artifactId>\n" //
                + "                    <version>1.2.3</version>\n" //
                + "                </dependency>\n" //
                + "            </dependencies>\n" //
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
                + "    <dependencies>\n" //
                + "        <!-- foo -->\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep1</artifactId>\n" //
                + "            <version>1.2.3</version>\n" //
                + "        </dependency>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep2</artifactId>\n" //
                + "            <version>1.2.3</version>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "\n" //
                + "            <dependencies>\n" //
                + "                <!-- foo -->\n" //
                + "                <dependency>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep3</artifactId>\n" //
                + "                    <version>1.2.3</version>\n" //
                + "                </dependency>\n" //
                + "                <dependency>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep5</artifactId>\n" //
                + "                    <version>1.2.3</version>\n" //
                + "                </dependency>\n" //
                + "                <dependency>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep4</artifactId>\n" //
                + "                    <version>1.2.3</version>\n" //
                + "                </dependency>\n" //
                + "            </dependencies>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Collections.singletonList(

                Dependencies.add(new Gavtcs("org.acme", "dep5", "1.2.3"))
                        .intoProfile("profile1")
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
                + "    <dependencies>\n" //
                + "        <!-- foo -->\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep1</artifactId>\n" //
                + "        </dependency>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep2</artifactId>\n" //
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
                + "            <artifactId>dep2</artifactId>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                Dependencies.remove("org.acme:dep1")),

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
                + "    <dependencies>\n" //
                + "        <!-- foo -->\n" //
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
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "\n" //
                + "            <dependencies>\n" //
                + "                <!-- foo -->\n" //
                + "                <dependency>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep3</artifactId>\n" //
                + "                    <version>${dep3.version}</version>\n" //
                + "                </dependency>\n" //
                + "                <dependency>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep4</artifactId>\n" //
                + "                    <version>${dep4.version}</version>\n" //
                + "                </dependency>\n" //
                + "            </dependencies>\n" //
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
                + "    <dependencies>\n" //
                + "        <!-- foo -->\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep1</artifactId>\n" //
                + "            <version>${dep1.version}</version>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "\n" //
                + "            <dependencies>\n" //
                + "                <!-- foo -->\n" //
                + "                <dependency>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep3</artifactId>\n" //
                + "                    <version>${dep3.version}</version>\n" //
                + "                </dependency>\n" //
                + "            </dependencies>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Collections.singletonList(

                Dependencies.remove("org.acme:dep2", "org.acme:dep4").from("profile1")

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
                + "    <dependencies>\n" //
                + "    </dependencies>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "\n" //
                + "            <dependencies>\n" //
                + "            </dependencies>\n" //
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
                + "    </profiles>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Collections.singletonList(

                Dependencies.removeEmptyParent().from(ProfileId.all())

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
                + "    <dependencies>\n" //
                + "        <!-- foo -->\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep1</artifactId>\n" //
                + "            <version>1.2.3</version>\n" //
                + "        </dependency>\n" //
                + "        <dependency>\n" //
                + "            <groupId>org.acme</groupId>\n" //
                + "            <artifactId>dep2</artifactId>\n" //
                + "            <version>1.2.3</version>\n" //
                + "        </dependency>\n" //
                + "    </dependencies>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "\n" //
                + "            <dependencies>\n" //
                + "                <!-- foo -->\n" //
                + "                <dependency>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep3</artifactId>\n" //
                + "                    <version>1.2.3</version>\n" //
                + "                </dependency>\n" //
                + "                <dependency>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep4</artifactId>\n" //
                + "                    <version>1.2.3</version>\n" //
                + "                </dependency>\n" //
                + "            </dependencies>\n" //
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
                    + "    <dependencies>\n" //
                    + "        <!-- foo -->\n" //
                    + "        <dependency>\n" //
                    + "            <groupId>org.hackme</groupId>\n" //
                    + "            <artifactId>dep1-mod</artifactId>\n" //
                    + "            <version>1.2.4</version>\n" //
                    + "            <classifier>cl</classifier>\n" //
                    + "            <scope>test</scope>\n" //
                    + "        </dependency>\n" //
                    + "        <dependency>\n" //
                    + "            <groupId>org.hackme</groupId>\n" //
                    + "            <artifactId>dep2-mod</artifactId>\n" //
                    + "            <version>1.2.4</version>\n" //
                    + "            <classifier>cl</classifier>\n" //
                    + "            <scope>test</scope>\n" //
                    + "        </dependency>\n" //
                    + "    </dependencies>\n" //
                    + "    <profiles>\n" //
                    + "        <profile>\n" //
                    + "            <id>profile1</id>\n" //
                    + "\n" //
                    + "            <dependencies>\n" //
                    + "                <!-- foo -->\n" //
                    + "                <dependency>\n" //
                    + "                    <groupId>org.hackme</groupId>\n" //
                    + "                    <artifactId>dep3-mod</artifactId>\n" //
                    + "                    <version>1.2.4</version>\n" //
                    + "                    <classifier>cl</classifier>\n" //
                    + "                    <scope>test</scope>\n" //
                    + "                </dependency>\n" //
                    + "                <dependency>\n" //
                    + "                    <groupId>org.hackme</groupId>\n" //
                    + "                    <artifactId>dep4-mod</artifactId>\n" //
                    + "                    <version>1.2.4</version>\n" //
                    + "                    <classifier>cl</classifier>\n" //
                    + "                    <scope>test</scope>\n" //
                    + "                </dependency>\n" //
                    + "            </dependencies>\n" //
                    + "        </profile>\n" //
                    + "    </profiles>\n" //
                    + "</project>\n";
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    Dependencies.select(p -> true)
                            .from(ProfileId.all())
                            .forEach(dep -> dep.setGroupId("org.hackme")
                                    .setArtifactId(dep.getGavtcs().getArtifactId() + "-mod")
                                    .setVersion("1.2.4")
                                    .setClassifier("cl")
                                    .setScope("test"))),
                    expected);
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    Dependencies.selectAll()
                            .from(ProfileId.all())
                            .forEach(dep -> dep.setGroupId("org.hackme")
                                    .setArtifactId(dep.getGavtcs().getArtifactId() + "-mod")
                                    .setVersion("1.2.4")
                                    .setClassifier("cl")
                                    .setScope("test"))),
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
                    + "    <dependencies>\n" //
                    + "        <!-- foo -->\n" //
                    + "        <dependency>\n" //
                    + "            <groupId>org.acme</groupId>\n" //
                    + "            <artifactId>dep1</artifactId>\n" //
                    + "            <version>1.2.3</version>\n" //
                    + "        </dependency>\n" //
                    + "        <dependency>\n" //
                    + "            <groupId>org.acme</groupId>\n" //
                    + "            <artifactId>dep2</artifactId>\n" //
                    + "            <version>1.2.4</version>\n" //
                    + "        </dependency>\n" //
                    + "    </dependencies>\n" //
                    + "    <profiles>\n" //
                    + "        <profile>\n" //
                    + "            <id>profile1</id>\n" //
                    + "\n" //
                    + "            <dependencies>\n" //
                    + "                <!-- foo -->\n" //
                    + "                <dependency>\n" //
                    + "                    <groupId>org.acme</groupId>\n" //
                    + "                    <artifactId>dep3</artifactId>\n" //
                    + "                    <version>1.2.3</version>\n" //
                    + "                </dependency>\n" //
                    + "                <dependency>\n" //
                    + "                    <groupId>org.acme</groupId>\n" //
                    + "                    <artifactId>dep4</artifactId>\n" //
                    + "                    <version>1.2.3</version>\n" //
                    + "                </dependency>\n" //
                    + "            </dependencies>\n" //
                    + "        </profile>\n" //
                    + "    </profiles>\n" //
                    + "</project>\n";
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    Dependencies.select(p -> p.getArtifactId().equals("dep2")).forEach(dep -> dep.setVersion("1.2.4"))),
                    expected);
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    Dependencies.select(Gavtcs.of("org.acme:dep2:1.2.3")).forEach(dep -> dep.setVersion("1.2.4"))),
                    expected);
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    Dependencies.select(GavtcsSet.builder().include("*:dep2").build()).forEach(dep -> dep.setVersion("1.2.4"))),
                    expected);
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    Dependencies.select("*:dep2").forEach(dep -> dep.setVersion("1.2.4"))),
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
                    + "    <dependencies>\n" //
                    + "        <!-- foo -->\n" //
                    + "        <dependency>\n" //
                    + "            <groupId>org.acme</groupId>\n" //
                    + "            <artifactId>dep1</artifactId>\n" //
                    + "            <version>1.2.3</version>\n" //
                    + "        </dependency>\n" //
                    + "        <dependency>\n" //
                    + "            <groupId>org.acme</groupId>\n" //
                    + "            <artifactId>dep2</artifactId>\n" //
                    + "            <version>1.2.3</version>\n" //
                    + "        </dependency>\n" //
                    + "    </dependencies>\n" //
                    + "    <profiles>\n" //
                    + "        <profile>\n" //
                    + "            <id>profile1</id>\n" //
                    + "\n" //
                    + "            <dependencies>\n" //
                    + "                <!-- foo -->\n" //
                    + "                <dependency>\n" //
                    + "                    <groupId>org.acme</groupId>\n" //
                    + "                    <artifactId>dep3</artifactId>\n" //
                    + "                    <version>1.2.3</version>\n" //
                    + "                </dependency>\n" //
                    + "                <dependency>\n" //
                    + "                    <groupId>org.acme</groupId>\n" //
                    + "                    <artifactId>dep4</artifactId>\n" //
                    + "                    <version>1.2.4</version>\n" //
                    + "                </dependency>\n" //
                    + "            </dependencies>\n" //
                    + "        </profile>\n" //
                    + "    </profiles>\n" //
                    + "</project>\n";
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    Dependencies.select(p -> p.getArtifactId().equals("dep4"))
                            .fromProfilesOnly("profile1")
                            .forEach(dep -> dep.setVersion("1.2.4"))),
                    expected);
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    Dependencies.select(p -> p.getArtifactId().equals("dep4"))
                            .from(ProfileId.idsOnly("profile1"))
                            .forEach(dep -> dep.setVersion("1.2.4"))),
                    expected);
        }
    }

}
