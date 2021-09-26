/**
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.l2x6.pom.tuner.MavenSourceTree.ActiveProfiles;
import org.l2x6.pom.tuner.MavenSourceTree.Builder;
import org.l2x6.pom.tuner.PomTransformer.SimpleElementWhitespace;
import org.l2x6.pom.tuner.model.Dependency;
import org.l2x6.pom.tuner.model.Expression;
import org.l2x6.pom.tuner.model.Expression.NoSuchPropertyException;
import org.l2x6.pom.tuner.model.Ga;
import org.l2x6.pom.tuner.model.Gav;
import org.l2x6.pom.tuner.model.GavExpression;
import org.l2x6.pom.tuner.model.GavSet;
import org.l2x6.pom.tuner.model.Module;
import org.l2x6.pom.tuner.model.Profile;
import org.l2x6.pom.tuner.model.Profile.PropertyBuilder;
import org.l2x6.pom.tuner.shell.BadExitCodeException;
import org.l2x6.pom.tuner.shell.BuildException;
import org.l2x6.pom.tuner.shell.CommandTimeoutException;
import org.l2x6.pom.tuner.shell.LineConsumer;
import org.l2x6.pom.tuner.shell.Shell;
import org.l2x6.pom.tuner.shell.ShellCommand;
import org.l2x6.pom.tuner.shell.ShellCommand.ShellCommandBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public class MavenSourceTreeTest {
    private static final Path BASEDIR = Paths.get(System.getProperty("project.basedir", "."));
    private static final Path MVN_LOCAL_REPO;
    private static final Path MVNW;

    static {
        MVN_LOCAL_REPO = BASEDIR.resolve("target/mvn-local-repo");
        MVNW = ShellCommand.findMvnw(BASEDIR);
    }

    static void assertProperty(MavenSourceTree t, String propertyName, Ga ga, String expectedValue, String... profiles)
            throws BadExitCodeException, CommandTimeoutException, BuildException {

        final LineConsumer output = LineConsumer.string();
        final ShellCommandBuilder cmd = ShellCommand.builder() //
                .id("assertProperty") //
                .workingDirectory(t.getRootDirectory()) //
                .executable(MVNW.toString()) //
                .arguments( //
                        "org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate", //
                        "-Dexpression=" + propertyName, //
                        "-Dartifact=" + ga.toString(), //
                        "-Dmaven.repo.local=" + MVN_LOCAL_REPO.toString(), //
                        "-q", //
                        "-DforceStdout") //
                .output(() -> output) //
        ;
        if (profiles.length > 0) {
            final StringBuilder profs = new StringBuilder("-P");
            if (profiles.length > 0) {
                for (int i = 0; i < profiles.length; i++) {
                    if (i > 0) {
                        profs.append(',');
                    }
                    profs.append(profiles[i]);
                }
            }
            cmd.arguments(profs.toString());
        }
        Shell.execute(cmd.build()).assertSuccess();
        Assertions.assertEquals(expectedValue, output.toString().trim());
        ExpressionEvaluator evaluator = t.getExpressionEvaluator(ActiveProfiles.of(profiles));
        Assertions.assertEquals(expectedValue, evaluator.evaluate(Expression.of("${" + propertyName + "}", ga)));
    }

    static GavExpression moduleGae(String gavString) {
        final Gav gav = Gav.of(gavString);
        final Ga ga = new Ga(gav.getGroupId(), gav.getArtifactId());
        return new GavExpression(Expression.of(gav.getGroupId(), ga), Expression.of(gav.getArtifactId(), ga),
                Expression.of(gav.getVersion(), ga));
    }

    static Map<String, Expression> props(Ga ga, String... keyVals) {
        final Map<String, Expression> props = new LinkedHashMap<>();
        for (int i = 0; i < keyVals.length;) {
            props.put(keyVals[i++], Expression.of(keyVals[i++], ga));
        }
        return props;
    }

    @Test
    public void filterDependencies() throws IOException {
        final Path root = BASEDIR.resolve("target/test-classes/MavenSourceTree/set-versions");
        final MavenSourceTree t = new Builder(root, StandardCharsets.UTF_8).pomXml(root.resolve("pom.xml")).build();
        final GavSet gavSet = GavSet.builder().include("org.srcdeps.external").build();
        final Set<Ga> actual = t.filterDependencies(gavSet, ActiveProfiles.of());
        final Set<Ga> expected = new TreeSet<Ga>(Arrays.asList( //
                Ga.of("org.srcdeps.external:extenal-parent"), //
                Ga.of("org.srcdeps.external:extenal-bom"), //
                Ga.of("org.srcdeps.external:external-extension"), //
                Ga.of("org.srcdeps.external:external-plugin-1"), //
                Ga.of("org.srcdeps.external:external-plugin-2") //
        ));
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void ofArgs() {
        Assertions.assertEquals(ActiveProfiles.EMPTY, ActiveProfiles.ofArgs(Arrays.asList()));
        Assertions.assertEquals(ActiveProfiles.of("p1"), ActiveProfiles.ofArgs(Arrays.asList("-Pp1")));
        Assertions.assertEquals(ActiveProfiles.of("p1", "p2"), ActiveProfiles.ofArgs(Arrays.asList("-Pp1,p2")));
        Assertions.assertEquals(ActiveProfiles.of("p1"), ActiveProfiles.ofArgs(Arrays.asList("-P", "p1")));
        Assertions.assertEquals(ActiveProfiles.of("p1", "p2"), ActiveProfiles.ofArgs(Arrays.asList("-P", "p1,p2")));
        Assertions.assertEquals(ActiveProfiles.of("p1"), ActiveProfiles.ofArgs(Arrays.asList("--activate-profiles", "p1")));
        Assertions.assertEquals(ActiveProfiles.of("p1", "p2"),
                ActiveProfiles.ofArgs(Arrays.asList("--activate-profiles", "p1,p2")));
    }

    @Test
    public void propertyEval() throws BadExitCodeException, CommandTimeoutException, BuildException {
        final Path root = BASEDIR.resolve("target/test-classes/MavenSourceTree/properties");
        final MavenSourceTree t = new Builder(root, StandardCharsets.UTF_8).pomXml(root.resolve("pom.xml")).build();

        final Module m8 = t.getModulesByGa().get(Ga.of("org.srcdeps.properties:module-1"));
        Assertions.assertEquals(new Expression("val-1/main", new Ga("org.srcdeps.properties", "module-1")),
                m8.findPropertyDefinition("prop1", ActiveProfiles.of()).getValue());
        Assertions.assertEquals(new Expression("val-1/p1", new Ga("org.srcdeps.properties", "module-1")),
                m8.findPropertyDefinition("prop1", ActiveProfiles.of("p1")).getValue());
        Assertions.assertEquals(new Expression("val-1/p2", new Ga("org.srcdeps.properties", "module-1")),
                m8.findPropertyDefinition("prop1", ActiveProfiles.of("p2")).getValue());
        Assertions.assertEquals(new Expression("val-1/p2", new Ga("org.srcdeps.properties", "module-1")),
                m8.findPropertyDefinition("prop1", ActiveProfiles.of("p1", "p2")).getValue());

        final ShellCommand cmd = ShellCommand.builder() //
                .id("propertyEval") //
                .workingDirectory(root) //
                .executable(MVNW.toString()) //
                .arguments("clean", "install", "-Dmaven.repo.local=" + MVN_LOCAL_REPO.toString(), "-B") //
                .output(LineConsumer::dummy) //
                .build();
        Shell.execute(cmd).assertSuccess();

        assertProperty(t, "prop1", Ga.of("org.srcdeps.properties:module-1"), "val-1/main");
        assertProperty(t, "prop1", Ga.of("org.srcdeps.properties:module-1"), "val-1/p1", "p1");
        assertProperty(t, "prop1", Ga.of("org.srcdeps.properties:module-1"), "val-1/p2", "p2");
        assertProperty(t, "prop1", Ga.of("org.srcdeps.properties:module-1"), "val-1/p2", "p1", "p2");
        assertProperty(t, "empty1", Ga.of("org.srcdeps.properties:tree-parent"), "");
        assertProperty(t, "empty2", Ga.of("org.srcdeps.properties:tree-parent"), "");

        Assertions.assertEquals(3, t.getRootModule().getProfiles().get(0).getProperties().size());

        try {
            t.getExpressionEvaluator(ActiveProfiles.of())
                    .evaluate(Expression.of("${non-existent}", Ga.of("org.srcdeps.properties:module-1")));
            Assertions.fail("NoSuchPropertyException expected");
        } catch (NoSuchPropertyException expected) {
        }
    }

    @Test
    public void setVersions() throws IOException {
        final Path root = BASEDIR.resolve("target/test-classes/MavenSourceTree/set-versions");

        final MavenSourceTree t = new Builder(root, StandardCharsets.UTF_8).pomXml(root.resolve("pom.xml")).build();
        t.setVersions("2.2.2", ActiveProfiles.of(), SimpleElementWhitespace.AUTODETECT_PREFER_SPACE);

        final Path expectedRoot = BASEDIR.resolve("target/test-classes/MavenSourceTree/set-versions-expected");

        for (String path : t.getModulesByPath().keySet()) {
            final Path actualPath = root.resolve(path);
            final Path expectedPath = expectedRoot.resolve(path);
            org.assertj.core.api.Assertions
                    .assertThat(actualPath)
                    .hasSameTextualContentAs(expectedPath, StandardCharsets.UTF_8);
        }

    }

    @Test
    public void dependencyFilter() throws IOException {
        final Path root = BASEDIR.resolve("target/test-classes/MavenSourceTree/tree-1");
        final MavenSourceTree t = MavenSourceTree.of(root.resolve("pom.xml"), StandardCharsets.UTF_8,
                dep -> dep.getGroupId().toString().equals("org.srcdeps.tree-1")
                        && dep.getArtifactId().toString().equals("tree-module-5"));
        final Module m4 = t.getModulesByGa().get(Ga.of("org.srcdeps.tree-1:tree-module-4"));
        final Ga m4Ga = new Ga("org.srcdeps.tree-1", "tree-module-4");
        Assertions.assertEquals(
                new LinkedHashSet<>(Arrays.asList(
                        new Dependency(
                                new Expression("org.srcdeps.tree-1", m4Ga),
                                new Expression("tree-module-1", m4Ga),
                                new Expression("0.0.1", m4Ga), "jar", "compile"))),
                m4.getProfiles().get(0).getDependencies());
    }

    @Test
    public void getModuleByPath() throws IOException {
        final Path root = BASEDIR.toAbsolutePath().normalize().resolve("target/test-classes/MavenSourceTree/tree-1");
        final MavenSourceTree t = MavenSourceTree.of(root.resolve("pom.xml"), StandardCharsets.UTF_8);

        final Path relPath = Paths.get("module-4/pom.xml");
        {
            final Module m4 = t.getModuleByPath(relPath);
            org.assertj.core.api.Assertions.assertThat(m4).isNotNull();
            org.assertj.core.api.Assertions.assertThat(
                    t.getExpressionEvaluator(ActiveProfiles.of()).evaluateGa(m4.getGav())
                            .getArtifactId())
                    .isEqualTo("tree-module-4");
        }
        {
            final Module m4 = t.getModuleByPath(t.getRootDirectory().resolve(relPath));
            org.assertj.core.api.Assertions.assertThat(m4).isNotNull();
            org.assertj.core.api.Assertions
                    .assertThat(
                            t.getExpressionEvaluator(ActiveProfiles.of()).evaluateGa(m4.getGav()).getArtifactId())
                    .isEqualTo("tree-module-4");
        }
    }

    @Test
    public void collectDependencies() throws IOException {
        final Path root = BASEDIR.resolve("target/test-classes/MavenSourceTree/tree-1");
        MavenSourceTree t = MavenSourceTree.of(root.resolve("pom.xml"), StandardCharsets.UTF_8);

        final Predicate<Profile> profiles = ActiveProfiles.of();
        final Ga m1 = new Ga("org.srcdeps.tree-1", "tree-module-1");
        final ExpressionEvaluator evaluator = t.getExpressionEvaluator(profiles);
        assertThat(
                t.collectOwnDependencies(m1, profiles).stream()
                        .map(dep -> evaluator.evaluateGa(dep).toString()))
                                .containsExactly("org.srcdeps.external:artifact-3");

        final Ga m8 = new Ga("org.srcdeps.tree-1", "tree-module-8");
        assertThat(
                t.collectOwnDependencies(m8, profiles).stream()
                        .map(dep -> evaluator.evaluateGa(dep).toString()))
                                .containsExactly("org.srcdeps.tree-1:tree-module-1", "org.srcdeps.external:artifact-5");

        assertThat(
                t.collectTransitiveDependencies(m8, profiles).stream()
                        .map(dep -> evaluator.evaluateGa(dep).toString()))
                                .containsExactly("org.srcdeps.tree-1:tree-module-1", "org.srcdeps.external:artifact-3",
                                        "org.srcdeps.external:artifact-5");

    }

    @Test
    public void tree() throws IOException {
        final Path root = BASEDIR.resolve("target/test-classes/MavenSourceTree/tree-1");
        final Builder b = new Builder(root, StandardCharsets.UTF_8).pomXml(root.resolve("pom.xml"));

        Assertions.assertEquals(12, b.modulesByGa.size());
        Assertions.assertEquals(12, b.modulesByPath.size());

        final Module.Builder parent = b.modulesByGa.get(Ga.of("org.srcdeps.tree-1:tree-parent"));
        Assertions.assertTrue(b.modulesByPath.get("pom.xml") == parent);
        Assertions.assertEquals("pom.xml", parent.getPomPath());
        final GavExpression treeParentGav = moduleGae("org.srcdeps.tree-1:tree-parent:0.0.1");
        Assertions.assertEquals(treeParentGav, parent.getModuleGav().build());
        Assertions.assertEquals(moduleGae("org.srcdeps.external:external-parent:1.2.3"), parent.getParentGav().build());

        Assertions.assertEquals(new LinkedHashSet<String>(Arrays.asList("module-1/pom.xml", "module-2/pom.xml",
                "module-3/pom.xml", "module-4/pom.xml", "module-6/pom.xml", "module-7/pom.xml", "plugin/pom.xml",
                "proper-parent/pom.xml", "declared-parent/pom.xml")), parent.getProfiles().get(0).getChildren());
        Assertions.assertEquals(Collections.emptyList(), parent.getProfiles().get(0).getDependencies().stream()
                .map(bu -> bu.build().toString()).collect(Collectors.toList()));
        Assertions.assertEquals(//
                props(new ExpressionEvaluator.ConstantOnlyExpressionEvaluator().evaluateGa(treeParentGav), "prop1",
                        "val-parent").entrySet(), //
                parent.getProfiles().get(0).getProperties().stream().map(PropertyBuilder::build)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));

        {
            final Module.Builder properParent = b.modulesByGa.get(Ga.of("org.srcdeps.tree-1:proper-parent"));
            Assertions.assertTrue(b.modulesByPath.get("proper-parent/pom.xml") == properParent);
            Assertions.assertEquals("proper-parent/pom.xml", properParent.getPomPath());
            GavExpression gav = moduleGae("org.srcdeps.tree-1:proper-parent:0.0.1");
            Assertions.assertEquals(gav, properParent.getModuleGav().build());
            Assertions.assertEquals(treeParentGav, properParent.getParentGav().build());
            Assertions.assertEquals(new LinkedHashSet<String>(Arrays.asList("proper-parent/module-5/pom.xml")),
                    properParent.getProfiles().get(0).getChildren());
            Assertions.assertEquals(Collections.emptyList(), properParent.getProfiles().get(0).getDependencies().stream()
                    .map(bu -> bu.build().toString()).collect(Collectors.toList()));
            Assertions.assertEquals(//
                    props(new ExpressionEvaluator.ConstantOnlyExpressionEvaluator().evaluateGa(gav), "prop1",
                            "val-proper-parent").entrySet(), //
                    properParent.getProfiles().get(0).getProperties().stream().map(PropertyBuilder::build)
                            .collect(Collectors.toCollection(LinkedHashSet::new)));
        }

        {
            final Module.Builder m1 = b.modulesByGa.get(Ga.of("org.srcdeps.tree-1:tree-module-1"));
            Assertions.assertTrue(b.modulesByPath.get("module-1/pom.xml") == m1);
            Assertions.assertEquals("module-1/pom.xml", m1.getPomPath());
            Assertions.assertEquals(moduleGae("org.srcdeps.tree-1:tree-module-1:0.0.1"), m1.getModuleGav().build());
            Assertions.assertEquals(treeParentGav, m1.getParentGav().build());
            Assertions.assertEquals(Arrays.asList("org.srcdeps.external:artifact-3:1.2.3"),
                    m1.getProfiles().get(0).getDependencies()
                            .stream().map(bu -> bu.build().toString()).collect(Collectors.toList()));
            Assertions.assertEquals(Collections.emptySet(), m1.getProfiles().get(0).getChildren());
        }

        {
            final Module.Builder m2 = b.modulesByGa.get(Ga.of("org.srcdeps.tree-1:tree-module-2"));
            Assertions.assertTrue(b.modulesByPath.get("module-2/pom.xml") == m2);
            Assertions.assertEquals("module-2/pom.xml", m2.getPomPath());
            Assertions.assertEquals(moduleGae("org.srcdeps.tree-1:tree-module-2:0.0.1"), m2.getModuleGav().build());
            Assertions.assertEquals(treeParentGav, m2.getParentGav().build());
            Assertions.assertEquals(
                    Arrays.asList("org.srcdeps.tree-1:tree-module-4:0.0.1", "org.srcdeps.tree-1:tree-module-7:0.0.1",
                            "org.srcdeps.tree-1:tree-module-8:0.0.1", "org.srcdeps.external:artifact-4:1.2.3"),
                    m2.getProfiles().get(0).getDependencies().stream().map(bu -> bu.build().toString())
                            .collect(Collectors.toList()));
            Assertions.assertEquals(Collections.emptySet(), m2.getProfiles().get(0).getChildren());
        }

        {
            final Module.Builder m3 = b.modulesByGa.get(Ga.of("org.srcdeps.tree-1:tree-module-3"));
            Assertions.assertTrue(b.modulesByPath.get("module-3/pom.xml") == m3);
            Assertions.assertEquals("module-3/pom.xml", m3.getPomPath());
            Assertions.assertEquals(moduleGae("org.srcdeps.tree-1:tree-module-3:0.0.1"), m3.getModuleGav().build());
            Assertions.assertEquals(treeParentGav, m3.getParentGav().build());
            Assertions.assertEquals(Arrays.asList("org.srcdeps.external:artifact-1:1.2.3"),
                    m3.getProfiles().get(0).getDependencies()
                            .stream().map(bu -> bu.build().toString()).collect(Collectors.toList()));
            Assertions.assertEquals(Collections.emptySet(), m3.getProfiles().get(0).getChildren());
        }

        {
            final Module.Builder m4 = b.modulesByGa.get(Ga.of("org.srcdeps.tree-1:tree-module-4"));
            Assertions.assertTrue(b.modulesByPath.get("module-4/pom.xml") == m4);
            Assertions.assertEquals("module-4/pom.xml", m4.getPomPath());
            Assertions.assertEquals(moduleGae("org.srcdeps.tree-1:tree-module-4:0.0.1"), m4.getModuleGav().build());
            Assertions.assertEquals(treeParentGav, m4.getParentGav().build());
            Assertions.assertEquals(
                    Arrays.asList("org.srcdeps.tree-1:tree-module-1:0.0.1", "org.srcdeps.tree-1:tree-module-5:0.0.1"),
                    m4.getProfiles().get(0).getDependencies().stream().map(bu -> bu.build().toString())
                            .collect(Collectors.toList()));
            Assertions.assertEquals(Collections.emptySet(), m4.getProfiles().get(0).getChildren());
        }

        {
            final Module.Builder m5 = b.modulesByGa.get(Ga.of("org.srcdeps.tree-1:tree-module-5"));
            Assertions.assertTrue(b.modulesByPath.get("proper-parent/module-5/pom.xml") == m5);
            Assertions.assertEquals("proper-parent/module-5/pom.xml", m5.getPomPath());
            GavExpression gav = moduleGae("org.srcdeps.tree-1:tree-module-5:0.0.1");
            Assertions.assertEquals(gav, m5.getModuleGav().build());
            Assertions.assertEquals(treeParentGav, m5.getParentGav().build());
            Assertions.assertEquals(Collections.emptyList(), m5.getProfiles().get(0).getDependencies().stream()
                    .map(bu -> bu.build().toString()).collect(Collectors.toList()));
            Assertions.assertEquals(Collections.emptySet(), m5.getProfiles().get(0).getChildren());
            Assertions.assertEquals(//
                    props(new ExpressionEvaluator.ConstantOnlyExpressionEvaluator().evaluateGa(gav), "prop1", "val-5")
                            .entrySet(), //
                    m5.getProfiles().get(0).getProperties().stream().map(PropertyBuilder::build)
                            .collect(Collectors.toCollection(LinkedHashSet::new)));
        }

        final MavenSourceTree t = b.build();
        Assertions.assertEquals(new Expression("val-parent", new Ga("org.srcdeps.tree-1", "tree-parent")),
                t.getRootModule().findPropertyDefinition("prop1", ActiveProfiles.of()).getValue());
        {
            final Module m8 = t.getModulesByGa().get(Ga.of("org.srcdeps.tree-1:tree-module-8"));
            Assertions.assertEquals(new Expression("val-8/main", new Ga("org.srcdeps.tree-1", "tree-module-8")),
                    m8.findPropertyDefinition("prop2", ActiveProfiles.of()).getValue());
            Assertions.assertEquals(new Expression("val-8/p1", new Ga("org.srcdeps.tree-1", "tree-module-8")),
                    m8.findPropertyDefinition("prop2", ActiveProfiles.of("p1")).getValue());
            Assertions.assertEquals(new Expression("val-8/p2", new Ga("org.srcdeps.tree-1", "tree-module-8")),
                    m8.findPropertyDefinition("prop2", ActiveProfiles.of("p2")).getValue());
            Assertions.assertEquals(new Expression("val-8/p2", new Ga("org.srcdeps.tree-1", "tree-module-8")),
                    m8.findPropertyDefinition("prop2", ActiveProfiles.of("p1", "p2")).getValue());
            Assertions.assertEquals(new Expression("val-8/p2", new Ga("org.srcdeps.tree-1", "tree-module-8")),
                    m8.findPropertyDefinition("prop2", ActiveProfiles.of("p1", "p2")).getValue());
        }

        final Predicate<Profile> profileSelector = p -> true;
        final Set<Ga> expandedIncludes = t
                .findRequiredModules(Arrays.asList(Ga.of("org.srcdeps.tree-1:tree-module-2")), profileSelector);
        Assertions.assertEquals(Arrays
                .asList("org.srcdeps.tree-1:tree-module-2", "org.srcdeps.tree-1:tree-parent",
                        "org.srcdeps.tree-1:tree-module-4", "org.srcdeps.tree-1:tree-module-1",
                        "org.srcdeps.tree-1:tree-module-5", "org.srcdeps.tree-1:proper-parent",
                        "org.srcdeps.tree-1:tree-module-7", "org.srcdeps.tree-1:tree-module-8",
                        "org.srcdeps.tree-1:declared-parent", "org.srcdeps.tree-1:tree-plugin")
                .stream().map(Ga::of).collect(Collectors.toCollection(LinkedHashSet::new)), expandedIncludes);

        ExpressionEvaluator evaluator = t.getExpressionEvaluator(profileSelector);
        final Map<String, Set<Path>> removeChildPaths = t.unlinkModules(expandedIncludes, t.getRootModule(),
                new LinkedHashMap<String, Set<Path>>(), profileSelector, evaluator);
        Assertions.assertEquals(1, removeChildPaths.size());
        Set<Path> rootUnlinks = removeChildPaths.get("pom.xml");
        org.assertj.core.api.Assertions
                .assertThat(rootUnlinks)
                .isEqualTo(
                        Stream.of("module-3/pom.xml", "module-6/pom.xml")
                                .map(root::resolve)
                                .collect(Collectors.toCollection(LinkedHashSet::new)));

        t.unlinkModules(expandedIncludes, profileSelector, StandardCharsets.UTF_8,
                SimpleElementWhitespace.AUTODETECT_PREFER_SPACE, "removed by srcdeps");

        final Path expectedRoot = BASEDIR.resolve("target/test-classes/MavenSourceTree/tree-1-expected");

        for (String path : removeChildPaths.keySet()) {
            final Path actualPath = root.resolve(path);
            final Path expectedPath = expectedRoot.resolve(path);
            org.assertj.core.api.Assertions.assertThat(actualPath).hasSameTextualContentAs(expectedPath,
                    StandardCharsets.UTF_8);
        }
    }

}
