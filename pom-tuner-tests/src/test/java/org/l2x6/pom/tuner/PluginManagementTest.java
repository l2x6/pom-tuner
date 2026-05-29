package org.l2x6.pom.tuner;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.l2x6.pom.tuner.model.Gav;
import org.l2x6.pom.tuner.model.GavSet;
import org.l2x6.pom.tuner.model.Gavtcs;
import org.l2x6.pom.tuner.model.GavtcsSet;
import org.l2x6.pom.tuner.transform.api.ProfileId;
import org.l2x6.pom.tuner.transform.PluginManagement;

public class PluginManagementTest {

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
                + "    <build>\n" //
                + "        <pluginManagement>\n" //
                + "            <plugins>\n" //
                + "                <!-- foo -->\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep1</artifactId>\n" //
                + "                </plugin>\n" //
                + "            </plugins>\n" //
                + "        </pluginManagement>\n" //
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
                + "    <build>\n" //
                + "        <pluginManagement>\n" //
                + "            <plugins>\n" //
                + "                <!-- foo -->\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep1</artifactId>\n" //
                + "                </plugin>\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep2</artifactId>\n" //
                + "                </plugin>\n" //
                + "            </plugins>\n" //
                + "        </pluginManagement>\n" //
                + "    </build>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                PluginManagement.add(new Gav("org.acme", "dep2", null))), expected);
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
                + "    <build>\n" //
                + "        <pluginManagement>\n" //
                + "            <plugins>\n" //
                + "                <!-- foo -->\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep1</artifactId>\n" //
                + "                </plugin>\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep2</artifactId>\n" //
                + "                </plugin>\n" //
                + "            </plugins>\n" //
                + "        </pluginManagement>\n" //
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
                + "    <build>\n" //
                + "        <pluginManagement>\n" //
                + "            <plugins>\n" //
                + "                <!-- foo -->\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep1</artifactId>\n" //
                + "                </plugin>\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep2</artifactId>\n" //
                + "                </plugin>\n" //
                + "            </plugins>\n" //
                + "        </pluginManagement>\n" //
                + "    </build>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                PluginManagement.add(new Gav("org.acme", "dep2", null))), expected);
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
                + "    <build>\n" //
                + "        <pluginManagement>\n" //
                + "            <plugins>\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep2</artifactId>\n" //
                + "                </plugin>\n" //
                + "            </plugins>\n" //
                + "        </pluginManagement>\n" //
                + "    </build>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                PluginManagement.add(new Gav("org.acme", "dep2", null))), expected);
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
                + "    <build>\n" //
                + "        <pluginManagement>\n" //
                + "            <plugins>\n" //
                + "                <!-- foo -->\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep1</artifactId>\n" //
                + "                </plugin>\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep3</artifactId>\n" //
                + "                </plugin>\n" //
                + "            </plugins>\n" //
                + "        </pluginManagement>\n" //
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
                + "    <build>\n" //
                + "        <pluginManagement>\n" //
                + "            <plugins>\n" //
                + "                <!-- foo -->\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep1</artifactId>\n" //
                + "                </plugin>\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep2</artifactId>\n" //
                + "                </plugin>\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep3</artifactId>\n" //
                + "                </plugin>\n" //
                + "            </plugins>\n" //
                + "        </pluginManagement>\n" //
                + "    </build>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                PluginManagement.add(new Gav("org.acme", "dep2", null))
                        .at(Comparators.after(new Gav("org.acme", "dep1", null)))),
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
                + "    <build>\n" //
                + "        <pluginManagement>\n" //
                + "            <plugins>\n" //
                + "                <!-- foo -->\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep1</artifactId>\n" //
                + "                </plugin>\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep3</artifactId>\n" //
                + "                </plugin>\n" //
                + "            </plugins>\n" //
                + "        </pluginManagement>\n" //
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
                + "    <build>\n" //
                + "        <pluginManagement>\n" //
                + "            <plugins>\n" //
                + "                <!-- foo -->\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep1</artifactId>\n" //
                + "                </plugin>\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep2</artifactId>\n" //
                + "                </plugin>\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep3</artifactId>\n" //
                + "                </plugin>\n" //
                + "            </plugins>\n" //
                + "        </pluginManagement>\n" //
                + "    </build>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                PluginManagement.add(new Gav("org.acme", "dep2", null))
                        .before(new Gav("org.acme", "dep3", null))),
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
                + "    <build>\n" //
                + "        <pluginManagement>\n" //
                + "            <plugins>\n" //
                + "                <!-- foo -->\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep1</artifactId>\n" //
                + "                </plugin>\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep3</artifactId>\n" //
                + "                </plugin>\n" //
                + "            </plugins>\n" //
                + "        </pluginManagement>\n" //
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
                + "    <build>\n" //
                + "        <pluginManagement>\n" //
                + "            <plugins>\n" //
                + "                <!-- foo -->\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep1</artifactId>\n" //
                + "                </plugin>\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep2</artifactId>\n" //
                + "                </plugin>\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep3</artifactId>\n" //
                + "                </plugin>\n" //
                + "            </plugins>\n" //
                + "        </pluginManagement>\n" //
                + "    </build>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                PluginManagement.add(new Gav("org.acme", "dep2", null))
                        .after(new Gav("org.acme", "dep1", null))),
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
                + "    <build>\n" //
                + "        <pluginManagement>\n" //
                + "            <plugins>\n" //
                + "                <!-- foo -->\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep1</artifactId>\n" //
                + "                    <version>1.2.3</version>\n" //
                + "                </plugin>\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep2</artifactId>\n" //
                + "                    <version>1.2.3</version>\n" //
                + "                </plugin>\n" //
                + "            </plugins>\n" //
                + "        </pluginManagement>\n" //
                + "    </build>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "\n" //
                + "            <build>\n" //
                + "                <pluginManagement>\n" //
                + "                    <plugins>\n" //
                + "                        <!-- foo -->\n" //
                + "                        <plugin>\n" //
                + "                            <groupId>org.acme</groupId>\n" //
                + "                            <artifactId>dep3</artifactId>\n" //
                + "                            <version>1.2.3</version>\n" //
                + "                        </plugin>\n" //
                + "                        <plugin>\n" //
                + "                            <groupId>org.acme</groupId>\n" //
                + "                            <artifactId>dep4</artifactId>\n" //
                + "                            <version>1.2.3</version>\n" //
                + "                        </plugin>\n" //
                + "                    </plugins>\n" //
                + "                </pluginManagement>\n" //
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
                + "        <pluginManagement>\n" //
                + "            <plugins>\n" //
                + "                <!-- foo -->\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep1</artifactId>\n" //
                + "                    <version>1.2.3</version>\n" //
                + "                </plugin>\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep2</artifactId>\n" //
                + "                    <version>1.2.3</version>\n" //
                + "                </plugin>\n" //
                + "            </plugins>\n" //
                + "        </pluginManagement>\n" //
                + "    </build>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "\n" //
                + "            <build>\n" //
                + "                <pluginManagement>\n" //
                + "                    <plugins>\n" //
                + "                        <!-- foo -->\n" //
                + "                        <plugin>\n" //
                + "                            <groupId>org.acme</groupId>\n" //
                + "                            <artifactId>dep3</artifactId>\n" //
                + "                            <version>1.2.3</version>\n" //
                + "                        </plugin>\n" //
                + "                        <plugin>\n" //
                + "                            <groupId>org.acme</groupId>\n" //
                + "                            <artifactId>dep5</artifactId>\n" //
                + "                            <version>1.2.3</version>\n" //
                + "                        </plugin>\n" //
                + "                        <plugin>\n" //
                + "                            <groupId>org.acme</groupId>\n" //
                + "                            <artifactId>dep4</artifactId>\n" //
                + "                            <version>1.2.3</version>\n" //
                + "                        </plugin>\n" //
                + "                    </plugins>\n" //
                + "                </pluginManagement>\n" //
                + "            </build>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Collections.singletonList(

                PluginManagement.add(new Gav("org.acme", "dep5", "1.2.3"))
                        .intoProfile("profile1")
                        .after(new Gav("org.acme", "dep3", "1.2.3"))

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
                + "    <build>\n" //
                + "        <pluginManagement>\n" //
                + "            <plugins>\n" //
                + "                <!-- foo -->\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep1</artifactId>\n" //
                + "                    <version>1.2.3</version>\n" //
                + "                </plugin>\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep2</artifactId>\n" //
                + "                    <version>1.2.3</version>\n" //
                + "                </plugin>\n" //
                + "            </plugins>\n" //
                + "        </pluginManagement>\n" //
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
                + "        <pluginManagement>\n" //
                + "            <plugins>\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep2</artifactId>\n" //
                + "                    <version>1.2.3</version>\n" //
                + "                </plugin>\n" //
                + "            </plugins>\n" //
                + "        </pluginManagement>\n" //
                + "    </build>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                PluginManagement.remove("org.acme:dep1")),

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
                + "        <pluginManagement>\n" //
                + "            <plugins>\n" //
                + "                <!-- foo -->\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep1</artifactId>\n" //
                + "                    <version>1.2.3</version>\n" //
                + "                </plugin>\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep2</artifactId>\n" //
                + "                    <version>1.2.3</version>\n" //
                + "                </plugin>\n" //
                + "            </plugins>\n" //
                + "        </pluginManagement>\n" //
                + "    </build>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "\n" //
                + "            <build>\n" //
                + "                <pluginManagement>\n" //
                + "                    <plugins>\n" //
                + "                        <!-- foo -->\n" //
                + "                        <plugin>\n" //
                + "                            <groupId>org.acme</groupId>\n" //
                + "                            <artifactId>dep3</artifactId>\n" //
                + "                            <version>1.2.3</version>\n" //
                + "                        </plugin>\n" //
                + "                        <plugin>\n" //
                + "                            <groupId>org.acme</groupId>\n" //
                + "                            <artifactId>dep4</artifactId>\n" //
                + "                            <version>1.2.3</version>\n" //
                + "                        </plugin>\n" //
                + "                    </plugins>\n" //
                + "                </pluginManagement>\n" //
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
                + "        <pluginManagement>\n" //
                + "            <plugins>\n" //
                + "                <!-- foo -->\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep1</artifactId>\n" //
                + "                    <version>1.2.3</version>\n" //
                + "                </plugin>\n" //
                + "            </plugins>\n" //
                + "        </pluginManagement>\n" //
                + "    </build>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "\n" //
                + "            <build>\n" //
                + "                <pluginManagement>\n" //
                + "                    <plugins>\n" //
                + "                        <!-- foo -->\n" //
                + "                        <plugin>\n" //
                + "                            <groupId>org.acme</groupId>\n" //
                + "                            <artifactId>dep3</artifactId>\n" //
                + "                            <version>1.2.3</version>\n" //
                + "                        </plugin>\n" //
                + "                    </plugins>\n" //
                + "                </pluginManagement>\n" //
                + "            </build>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Collections.singletonList(

                PluginManagement.remove("org.acme:dep2", "org.acme:dep4").from("profile1")

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
                + "        <pluginManagement>\n" //
                + "            <plugins>\n" //
                + "            </plugins>\n" //
                + "        </pluginManagement>\n" //
                + "    </build>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "\n" //
                + "            <build>\n" //
                + "                <pluginManagement>\n" //
                + "                    <plugins>\n" //
                + "                    </plugins>\n" //
                + "                </pluginManagement>\n" //
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
                + "        <pluginManagement>\n" //
                + "        </pluginManagement>\n" //
                + "    </build>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "\n" //
                + "            <build>\n" //
                + "                <pluginManagement>\n" //
                + "                </pluginManagement>\n" //
                + "            </build>\n" //
                + "        </profile>\n" //
                + "    </profiles>\n" //
                + "</project>\n";
        PomTransformerTestUtils.assertTransformer(source, Collections.singletonList(

                PluginManagement.removeEmptyParent().from(ProfileId.all())

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
                + "    <build>\n" //
                + "        <pluginManagement>\n" //
                + "            <plugins>\n" //
                + "                <!-- foo -->\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep1</artifactId>\n" //
                + "                    <version>1.2.3</version>\n" //
                + "                </plugin>\n" //
                + "                <plugin>\n" //
                + "                    <groupId>org.acme</groupId>\n" //
                + "                    <artifactId>dep2</artifactId>\n" //
                + "                    <version>1.2.3</version>\n" //
                + "                </plugin>\n" //
                + "            </plugins>\n" //
                + "        </pluginManagement>\n" //
                + "    </build>\n" //
                + "    <profiles>\n" //
                + "        <profile>\n" //
                + "            <id>profile1</id>\n" //
                + "\n" //
                + "            <build>\n" //
                + "                <pluginManagement>\n" //
                + "                    <plugins>\n" //
                + "                        <!-- foo -->\n" //
                + "                        <plugin>\n" //
                + "                            <groupId>org.acme</groupId>\n" //
                + "                            <artifactId>dep3</artifactId>\n" //
                + "                            <version>1.2.3</version>\n" //
                + "                        </plugin>\n" //
                + "                        <plugin>\n" //
                + "                            <groupId>org.acme</groupId>\n" //
                + "                            <artifactId>dep4</artifactId>\n" //
                + "                            <version>1.2.3</version>\n" //
                + "                        </plugin>\n" //
                + "                    </plugins>\n" //
                + "                </pluginManagement>\n" //
                + "            </build>\n" //
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
                    + "    <build>\n" //
                    + "        <pluginManagement>\n" //
                    + "            <plugins>\n" //
                    + "                <!-- foo -->\n" //
                    + "                <plugin>\n" //
                    + "                    <groupId>org.hackme</groupId>\n" //
                    + "                    <artifactId>dep1-mod</artifactId>\n" //
                    + "                    <version>1.2.4</version>\n" //
                    + "                    <classifier>cl</classifier>\n" //
                    + "                </plugin>\n" //
                    + "                <plugin>\n" //
                    + "                    <groupId>org.hackme</groupId>\n" //
                    + "                    <artifactId>dep2-mod</artifactId>\n" //
                    + "                    <version>1.2.4</version>\n" //
                    + "                    <classifier>cl</classifier>\n" //
                    + "                </plugin>\n" //
                    + "            </plugins>\n" //
                    + "        </pluginManagement>\n" //
                    + "    </build>\n" //
                    + "    <profiles>\n" //
                    + "        <profile>\n" //
                    + "            <id>profile1</id>\n" //
                    + "\n" //
                    + "            <build>\n" //
                    + "                <pluginManagement>\n" //
                    + "                    <plugins>\n" //
                    + "                        <!-- foo -->\n" //
                    + "                        <plugin>\n" //
                    + "                            <groupId>org.hackme</groupId>\n" //
                    + "                            <artifactId>dep3-mod</artifactId>\n" //
                    + "                            <version>1.2.4</version>\n" //
                    + "                            <classifier>cl</classifier>\n" //
                    + "                        </plugin>\n" //
                    + "                        <plugin>\n" //
                    + "                            <groupId>org.hackme</groupId>\n" //
                    + "                            <artifactId>dep4-mod</artifactId>\n" //
                    + "                            <version>1.2.4</version>\n" //
                    + "                            <classifier>cl</classifier>\n" //
                    + "                        </plugin>\n" //
                    + "                    </plugins>\n" //
                    + "                </pluginManagement>\n" //
                    + "            </build>\n" //
                    + "        </profile>\n" //
                    + "    </profiles>\n" //
                    + "</project>\n";
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    PluginManagement.select(p -> true)
                            .from(ProfileId.all())
                            .modify(dep -> dep.setGroupId("org.hackme")
                                    .setArtifactId(dep.getGavtcs().getArtifactId() + "-mod")
                                    .setVersion("1.2.4")
                                    .setClassifier("cl"))),
                    expected);
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    PluginManagement.selectAll()
                            .from(ProfileId.all())
                            .modify(dep -> dep.setGroupId("org.hackme")
                                    .setArtifactId(dep.getGavtcs().getArtifactId() + "-mod")
                                    .setVersion("1.2.4")
                                    .setClassifier("cl"))),
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
                    + "    <build>\n" //
                    + "        <pluginManagement>\n" //
                    + "            <plugins>\n" //
                    + "                <!-- foo -->\n" //
                    + "                <plugin>\n" //
                    + "                    <groupId>org.acme</groupId>\n" //
                    + "                    <artifactId>dep1</artifactId>\n" //
                    + "                    <version>1.2.3</version>\n" //
                    + "                </plugin>\n" //
                    + "                <plugin>\n" //
                    + "                    <groupId>org.acme</groupId>\n" //
                    + "                    <artifactId>dep2</artifactId>\n" //
                    + "                    <version>1.2.4</version>\n" //
                    + "                </plugin>\n" //
                    + "            </plugins>\n" //
                    + "        </pluginManagement>\n" //
                    + "    </build>\n" //
                    + "    <profiles>\n" //
                    + "        <profile>\n" //
                    + "            <id>profile1</id>\n" //
                    + "\n" //
                    + "            <build>\n" //
                    + "                <pluginManagement>\n" //
                    + "                    <plugins>\n" //
                    + "                        <!-- foo -->\n" //
                    + "                        <plugin>\n" //
                    + "                            <groupId>org.acme</groupId>\n" //
                    + "                            <artifactId>dep3</artifactId>\n" //
                    + "                            <version>1.2.3</version>\n" //
                    + "                        </plugin>\n" //
                    + "                        <plugin>\n" //
                    + "                            <groupId>org.acme</groupId>\n" //
                    + "                            <artifactId>dep4</artifactId>\n" //
                    + "                            <version>1.2.3</version>\n" //
                    + "                        </plugin>\n" //
                    + "                    </plugins>\n" //
                    + "                </pluginManagement>\n" //
                    + "            </build>\n" //
                    + "        </profile>\n" //
                    + "    </profiles>\n" //
                    + "</project>\n";
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    PluginManagement.select(p -> p.getArtifactId().equals("dep2")).modify(dep -> dep.setVersion("1.2.4"))),
                    expected);
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    PluginManagement.select(Gav.of("org.acme:dep2:1.2.3")).modify(dep -> dep.setVersion("1.2.4"))),
                    expected);
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    PluginManagement.select(GavSet.builder().include("*:dep2").build())
                            .modify(dep -> dep.setVersion("1.2.4"))),
                    expected);
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    PluginManagement.select("*:dep2").modify(dep -> dep.setVersion("1.2.4"))),
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
                    + "    <build>\n" //
                    + "        <pluginManagement>\n" //
                    + "            <plugins>\n" //
                    + "                <!-- foo -->\n" //
                    + "                <plugin>\n" //
                    + "                    <groupId>org.acme</groupId>\n" //
                    + "                    <artifactId>dep1</artifactId>\n" //
                    + "                    <version>1.2.3</version>\n" //
                    + "                </plugin>\n" //
                    + "                <plugin>\n" //
                    + "                    <groupId>org.acme</groupId>\n" //
                    + "                    <artifactId>dep2</artifactId>\n" //
                    + "                    <version>1.2.3</version>\n" //
                    + "                </plugin>\n" //
                    + "            </plugins>\n" //
                    + "        </pluginManagement>\n" //
                    + "    </build>\n" //
                    + "    <profiles>\n" //
                    + "        <profile>\n" //
                    + "            <id>profile1</id>\n" //
                    + "\n" //
                    + "            <build>\n" //
                    + "                <pluginManagement>\n" //
                    + "                    <plugins>\n" //
                    + "                        <!-- foo -->\n" //
                    + "                        <plugin>\n" //
                    + "                            <groupId>org.acme</groupId>\n" //
                    + "                            <artifactId>dep3</artifactId>\n" //
                    + "                            <version>1.2.3</version>\n" //
                    + "                        </plugin>\n" //
                    + "                        <plugin>\n" //
                    + "                            <groupId>org.acme</groupId>\n" //
                    + "                            <artifactId>dep4</artifactId>\n" //
                    + "                            <version>1.2.4</version>\n" //
                    + "                        </plugin>\n" //
                    + "                    </plugins>\n" //
                    + "                </pluginManagement>\n" //
                    + "            </build>\n" //
                    + "        </profile>\n" //
                    + "    </profiles>\n" //
                    + "</project>\n";
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    PluginManagement.select(p -> p.getArtifactId().equals("dep4"))
                            .fromProfilesOnly("profile1")
                            .modify(dep -> dep.setVersion("1.2.4"))),
                    expected);
            PomTransformerTestUtils.assertTransformer(source, Arrays.asList(
                    PluginManagement.select(p -> p.getArtifactId().equals("dep4"))
                            .from(ProfileId.idsOnly("profile1"))
                            .modify(dep -> dep.setVersion("1.2.4"))),
                    expected);
        }
    }

}
