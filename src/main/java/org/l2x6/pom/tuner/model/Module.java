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
package org.l2x6.pom.tuner.model;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.XMLEvent;
import org.l2x6.pom.tuner.MavenSourceTree;
import org.l2x6.pom.tuner.PomTunerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Maven module.
 */
public class Module {

    /**
     * A {@link Module} builder.
     */
    public static class Builder {
        static final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        static final Logger log = LoggerFactory.getLogger(Builder.class);

        final Profile.Builder implicitProfile;
        final ModuleGavBuilder moduleGav;
        final ParentGavBuilder parentGav;
        /** Relative to source tree root directory */
        final String pomPath;
        String name;
        List<Profile.Builder> profiles;

        public Builder(Path rootDirectory, Path pomXml, Charset encoding, Predicate<Dependency> dependencyExcludes) {
            parentGav = new ParentGavBuilder();
            moduleGav = new ModuleGavBuilder(parentGav);
            implicitProfile = new Profile.Builder(dependencyExcludes);
            profiles = new ArrayList<>();
            profiles.add(implicitProfile);

            final Stack<String> elementStack = new Stack<>();
            final Path dir = pomXml.getParent();
            try (Reader in = Files.newBufferedReader(pomXml, encoding)) {
                final XMLEventReader r = xmlInputFactory.createXMLEventReader(in);
                this.pomPath = PomTunerUtils.toUnixPath(rootDirectory.relativize(pomXml).toString());

                final Stack<GavBuilder> gavBuilderStack = new Stack<>();
                gavBuilderStack.push(moduleGav);
                Profile.Builder profile = implicitProfile;

                int ignoredSubtreesCount = 0;

                while (r.hasNext()) {
                    final XMLEvent e = r.nextEvent();
                    if (e.isStartElement()) {
                        final String elementName = e.asStartElement().getName().getLocalPart();
                        final int elementStackSize = elementStack.size();
                        if (ignoredSubtreesCount > 0) {
                            /* ignore */
                        } else if ("configuration".equals(elementName) || "exclusions".equals(elementName)) {
                            /* ignore elements under <configuration>, etc. */
                            ignoredSubtreesCount++;
                        } else if ("parent".equals(elementName) && r.hasNext()) {
                            gavBuilderStack.push(parentGav);
                        } else if ("name".equals(elementName) && "project".equals(elementStack.peek())) {
                            this.name = r.nextEvent().asCharacters().getData();
                        } else if ("dependency".equals(elementName)) {
                            final String grandParent = elementStack.get(elementStackSize - 2);
                            final DependencyBuilder gav = new DependencyBuilder(moduleGav);
                            if ("dependencyManagement".equals(grandParent)) {
                                profile.dependencyManagement.add(gav);
                            } else if ("project".equals(grandParent) || "profile".equals(grandParent)) {
                                profile.dependencies.add(gav);
                            } else if ("plugin".equals(grandParent)) {
                                final PluginGavBuilder pluginGavBuilder = (PluginGavBuilder) gavBuilderStack.peek();
                                pluginGavBuilder.dependency(gav);
                            } else {
                                log.warn("srcdeps: Unexpected grand parent of <dependency>: <{}> in [{}]",
                                        grandParent, pomXml);
                            }
                            gavBuilderStack.push(gav);
                        } else if ("extension".equals(elementName)) {
                            final PlainGavBuilder gav = new PlainGavBuilder(moduleGav);
                            profile.extensions.add(gav);
                            gavBuilderStack.push(gav);
                        } else if ("plugin".equals(elementName)) {
                            final PluginGavBuilder gav = new PluginGavBuilder(moduleGav);
                            gavBuilderStack.push(gav);
                            final String parentElement = elementStack.peek();
                            if ("plugins".equals(parentElement) && elementStack.size() > 1
                                    && "pluginManagement".equals(elementStack.get(elementStackSize - 2))) {
                                profile.pluginManagement.add(gav);
                            } else if ("plugins".equals(parentElement)) {
                                profile.plugins.add(gav);
                            } else {
                                throw new IllegalStateException(
                                        String.format("Unexpected grand parent of <plugin>: <%s> in [%s]",
                                                parentElement, pomXml));
                            }
                        } else if ("module".equals(elementName)) {
                            final String relPath = r.nextEvent().asCharacters().getData() + "/pom.xml";
                            final Path childPomXml = dir.resolve(relPath).normalize();
                            final String rootRelPath = rootDirectory.relativize(childPomXml).toString();
                            profile.children.add(PomTunerUtils.toUnixPath(rootRelPath));
                        } else if (elementStackSize > 0 && "properties".equals(elementStack.peek())) {
                            final XMLEvent nextEvent = r.peek();
                            if (nextEvent instanceof Characters) {
                                profile.properties.add(new Profile.PropertyBuilder(elementName,
                                        r.nextEvent().asCharacters().getData(), moduleGav));
                            } else if (nextEvent instanceof EndElement) {
                                profile.properties.add(new Profile.PropertyBuilder(elementName, "", moduleGav));
                            } else {
                                throw new IllegalStateException(String.format("Unexpected XML event [%s] in [%s]",
                                        nextEvent.getClass().getName(), pomXml));
                            }
                        } else if ("profile".equals(elementName)) {
                            profile = new Profile.Builder(dependencyExcludes);
                        } else if ("id".equals(elementName)) {
                            if ("profile".equals(elementStack.peek())) {
                                final String id = r.nextEvent().asCharacters().getData();
                                profile.id(id);
                                profiles.add(profile);
                            }
                        } else if ("groupId".equals(elementName)) {
                            gavBuilderStack.peek().groupId(r.nextEvent().asCharacters().getData());
                        } else if ("artifactId".equals(elementName)) {
                            gavBuilderStack.peek().artifactId(r.nextEvent().asCharacters().getData());
                        } else if ("version".equals(elementName)) {
                            gavBuilderStack.peek().version(r.nextEvent().asCharacters().getData());
                        } else if ("type".equals(elementName)
                                && gavBuilderStack.peek() instanceof DependencyBuilder) {
                            ((DependencyBuilder) gavBuilderStack.peek())
                                    .type(r.nextEvent().asCharacters().getData());
                        } else if ("scope".equals(elementName)
                                && gavBuilderStack.peek() instanceof DependencyBuilder) {
                            ((DependencyBuilder) gavBuilderStack.peek())
                                    .scope(r.nextEvent().asCharacters().getData());
                        }
                        elementStack.push(elementName);
                    } else if (e.isEndElement()) {
                        final String elementName = elementStack.pop();
                        if ("configuration".equals(elementName) || "exclusions".equals(elementName)) {
                            /* ignore elements under <configuration>, etc. */
                            ignoredSubtreesCount--;
                        } else if (ignoredSubtreesCount > 0) {
                            /* ignore */
                        } else if ("parent".equals(elementName)) {
                            final GavBuilder gav = gavBuilderStack.pop();
                            assert gav instanceof ParentGavBuilder;
                        } else if ("dependency".equals(elementName)) {
                            final GavBuilder gav = gavBuilderStack.pop();
                            assert gav instanceof PlainGavBuilder;
                        } else if ("extension".equals(elementName)) {
                            final GavBuilder gav = gavBuilderStack.pop();
                            assert gav instanceof PlainGavBuilder;
                        } else if ("plugin".equals(elementName)) {
                            final GavBuilder gav = gavBuilderStack.pop();
                            assert gav instanceof PluginGavBuilder;
                        } else if ("profile".equals(elementName)) {
                            profile = implicitProfile;
                        }
                    }
                }
            } catch (IOException | XMLStreamException e1) {
                throw new RuntimeException("Couldnot parse " + pomXml, e1);
            }
        }

        public Module build() {
            final List<Profile> useProfiles = Collections
                    .unmodifiableList(profiles.stream().map(Profile.Builder::build).collect(Collectors.toList()));
            profiles = null;
            return new Module(pomPath, moduleGav.build(), parentGav.build(), name, useProfiles);
        }

        public ModuleGavBuilder getModuleGav() {
            return moduleGav;
        }

        public String getPomPath() {
            return pomPath;
        }

        public List<Profile.Builder> getProfiles() {
            return profiles;
        }

        public ParentGavBuilder getParentGav() {
            return parentGav;
        }

    }

    public interface GavBuilder {
        void artifactId(String artifactId);

        void groupId(String groupId);

        void version(String version);

    }

    public static class DependencyBuilder extends PlainGavBuilder {

        private String scope = "compile";
        private String type = "jar";

        public DependencyBuilder(ModuleGavBuilder module) {
            super(module);
        }

        public Dependency build() {
            final Ga ga = module.getGa();
            return new Dependency(Expression.of(groupId, ga), Expression.of(artifactId, ga),
                    version != null ? Expression.of(version, ga) : null, type, scope);
        }

        public void scope(String scope) {
            this.scope = scope;
        }

        public void type(String type) {
            this.type = type;
        }

    }

    public static class ModuleGavBuilder extends ParentGavBuilder {
        private Ga ga;
        private final ParentGavBuilder parent;

        public ModuleGavBuilder(ParentGavBuilder parent) {
            super();
            this.parent = parent;
        }

        public GavExpression build() {
            final Ga ga = getGa();
            final Expression v = version != null ? Expression.of(version, ga) : parent.build().getVersion();
            return new GavExpression(Expression.of(ga.getGroupId(), ga), Expression.of(ga.getArtifactId(), ga), v);
        }

        public Ga getGa() {
            if (this.ga == null) {
                final String g = groupId != null ? groupId : parent.groupId;
                this.ga = new Ga(g, artifactId);
            }
            return this.ga;
        }
    }

    public static class ParentGavBuilder implements GavBuilder {

        String artifactId;
        String groupId;
        String version;

        @Override
        public void artifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        public GavExpression build() {
            final int sum = (groupId != null ? 1 : 0) + (artifactId != null ? 1 : 0) + (version != null ? 1 : 0);
            switch (sum) {
            case 0:
                /* none of the three set */
                return null;
            case 3:
                final Ga ga = new Ga(groupId, artifactId);
                return new GavExpression(Expression.of(groupId, ga), Expression.of(artifactId, ga),
                        Expression.of(version, ga));
            default:
                throw new IllegalStateException(String.format(
                        "groupId, artifactId and version must be all null or both not null: groupId: [%s], artifactId: [%s], version: [%s]",
                        groupId, artifactId, version));
            }
        }

        @Override
        public void groupId(String groupId) {
            this.groupId = groupId;
        }

        @Override
        public void version(String version) {
            this.version = version;
        }
    }

    public static class PlainGavBuilder extends ParentGavBuilder {

        final ModuleGavBuilder module;

        PlainGavBuilder(ModuleGavBuilder module) {
            super();
            this.module = module;
        }

        public GavExpression build() {
            final Ga ga = module.getGa();
            return new GavExpression(Expression.of(groupId, ga), Expression.of(artifactId, ga),
                    version != null ? Expression.of(version, ga) : null);
        }
    }

    public static class PluginGavBuilder extends PlainGavBuilder {

        private List<PlainGavBuilder> dependencies = new ArrayList<>();

        PluginGavBuilder(ModuleGavBuilder module) {
            super(module);
            this.groupId = "org.apache.maven.plugins";
        }

        public Plugin build() {
            final Set<GavExpression> deps = dependencies.stream()
                    .map(PlainGavBuilder::build).collect(Collectors.toCollection(LinkedHashSet::new));
            final Set<GavExpression> useDependencies = Collections.unmodifiableSet(deps);
            dependencies = null;
            final Ga ga = module.getGa();
            return new Plugin(Expression.of(groupId, ga), Expression.of(artifactId, ga),
                    version != null ? Expression.of(version, ga) : null, useDependencies);
        }

        public void dependency(PlainGavBuilder gav) {
            dependencies.add(gav);
        }

    }

    final GavExpression gav;
    final GavExpression parentGav;
    /** Relative to source tree root directory */
    final String pomPath;
    private final String name;

    final List<Profile> profiles;

    Module(String pomPath, GavExpression gav, GavExpression parentGa, String name, List<Profile> profiles) {
        super();
        this.pomPath = pomPath;
        this.gav = gav;
        this.parentGav = parentGa;
        this.name = name;
        this.profiles = profiles;
    }

    /**
     * Goes through active profiles and find the definition of the property having the given {@code propertyName}.
     *
     * @param  propertyName    the property name to find a definition for
     * @param  isProfileActive tells which profiles are active
     * @return                 the {@link ValueDefinition} of the seeked property or {@code null} if no such property is
     *                         defined in
     *                         this {@link Module}
     */
    public ValueDefinition findPropertyDefinition(String propertyName, Predicate<Profile> isProfileActive) {
        final ListIterator<Profile> it = this.profiles.listIterator(this.profiles.size());
        while (it.hasPrevious()) {
            final Profile p = it.previous();
            if (isProfileActive.test(p)) {
                final Expression result = p.properties.get(propertyName);
                if (result != null) {
                    final String xPath = MavenSourceTree.xPathProfile(p.getId(), "properties", propertyName);
                    return new ValueDefinition(this, xPath, result);
                }
            }
        }
        return null;
    }

    /**
     * @return the {@link GavExpression} of this Maven module
     */
    public GavExpression getGav() {
        return gav;
    }

    /**
     * @return the {@link GavExpression} of the Maven parent module of this module or {@code null} if this module
     *         has no parent
     */
    public GavExpression getParentGav() {
        return parentGav;
    }

    /**
     * @return a path to the this module's {@code pom.xml} relative to {@link MavenSourceTree#getRootModule()}
     */
    public String getPomPath() {
        return pomPath;
    }

    /**
     * @return the content of the {@code <name>} element of this Maven module or {@code null} if there is no
     *         {@code <name>}.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the {@link List} of profiles defined in this {@link Module}. Note that the top level profile-less
     *         dependencies, dependencyManagement, etc. are defined in {@link Module} with {@code id} {@code null}.
     */
    public List<Profile> getProfiles() {
        return profiles;
    }

    /**
     * @param  childPomPath    a path to {@code pom.xml} file relative to {@link MavenSourceTree#rootDirectory}
     * @param  isProfileActive
     * @return                 {@code true} if the {@code pom.xml} represented by this {@link Module} has a {@code <module>}
     *                         with
     *                         the given pom.xml path or {@code false} otherwise
     */
    public boolean hasChild(String childPomPath, Predicate<Profile> isProfileActive) {
        for (Profile p : profiles) {
            if (isProfileActive.test(p)) {
                if (p.children.contains(childPomPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param  isProfileActive an active profile selector
     * @return                 a {@link Stream} of own {@link Dependency Dependencies} collected from all active
     *                         {@link Profile}s
     */
    public Stream<Dependency> streamOwnDependencies(final Predicate<Profile> isProfileActive) {
        return profiles.stream()
                .filter(isProfileActive)
                .flatMap(p -> p.getDependencies().stream());
    }

    public String toString() {
        return gav.toString() + "@" + pomPath;
    }

}
