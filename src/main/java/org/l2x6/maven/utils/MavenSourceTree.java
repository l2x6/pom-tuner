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
package org.l2x6.maven.utils;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.XMLEvent;

import org.l2x6.maven.utils.MavenSourceTree.Expression.Constant;
import org.l2x6.maven.utils.MavenSourceTree.Expression.NonConstant;
import org.l2x6.maven.utils.MavenSourceTree.GavExpression.DependencyBuilder;
import org.l2x6.maven.utils.MavenSourceTree.GavExpression.GavBuilder;
import org.l2x6.maven.utils.MavenSourceTree.GavExpression.ModuleGavBuilder;
import org.l2x6.maven.utils.MavenSourceTree.GavExpression.ParentGavBuilder;
import org.l2x6.maven.utils.MavenSourceTree.GavExpression.PlainGavBuilder;
import org.l2x6.maven.utils.MavenSourceTree.GavExpression.PluginGavBuilder;
import org.l2x6.maven.utils.MavenSourceTree.Module.Profile;
import org.l2x6.maven.utils.PomTransformer.SimpleElementWhitespace;
import org.l2x6.maven.utils.PomTransformer.Transformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A representation of a Maven module hierarchy.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 4.1
 */
public class MavenSourceTree {
    public static class ActiveProfiles implements Predicate<Profile> {

        static final Predicate<Profile> EMPTY = new ActiveProfiles();

        /**
         * @param profileIds the active profiles (can be empty)
         * @return a new {@link Profile} filter which will hold the named {@code profileIds} for active
         */
        public static Predicate<Profile> of(String... profileIds) {
            return profileIds.length == 0 ? EMPTY : new ActiveProfiles(profileIds);
        }

        /**
         * @param args Maven command line arguments
         * @return a new {@link Predicate}
         */
        public static Predicate<Profile> ofArgs(List<String> args) {
            for (Iterator<String> it = args.iterator(); it.hasNext();) {
                final String arg = it.next();
                if ("-P".equals(arg) || "--activate-profiles".equals(arg)) {
                    return of(it.next().split(","));
                } else if (arg.startsWith("-P")) {
                    return of(arg.substring(2).split(","));
                }
            }
            return EMPTY;
        }

        final Set<String> profileIds;

        ActiveProfiles(String... profileIds) {
            super();
            Set<String> m = new LinkedHashSet<>(profileIds.length);
            for (String profileId : profileIds) {
                m.add(profileId);
            }
            this.profileIds = m;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ActiveProfiles other = (ActiveProfiles) obj;
            if (profileIds == null) {
                if (other.profileIds != null)
                    return false;
            } else if (!profileIds.equals(other.profileIds))
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((profileIds == null) ? 0 : profileIds.hashCode());
            return result;
        }

        @Override
        public boolean test(Profile t) {
            return t.getId() == null || profileIds.contains(t.getId());
        }

    }

    /**
     * A {@link MavenSourceTree} builder.
     */
    static class Builder {

        private final Charset encoding;

        final Map<Ga, Module.Builder> modulesByGa = new LinkedHashMap<>();

        /** By pom.xml path relative to {@link MavenSourceTree#rootDirectory} */
        final Map<String, Module.Builder> modulesByPath = new LinkedHashMap<>();

        private final Path rootDirectory;

        Builder(Path rootDirectory, Charset encoding) {
            super();
            this.rootDirectory = rootDirectory;
            this.encoding = encoding;
        }

        public MavenSourceTree build() {

            final Map<String, Module> byPath = new LinkedHashMap<>(modulesByPath.size());
            final Map<Ga, Module> byGa = new LinkedHashMap<>(modulesByPath.size());
            for (MavenSourceTree.Module.Builder e : modulesByPath.values()) {
                final Module module = e.build();
                byGa.put(module.getGav().resolveGa(null, null), module);
                byPath.put(module.pomPath, module);
            }
            return new MavenSourceTree(rootDirectory, encoding, Collections.unmodifiableMap(byPath),
                    Collections.unmodifiableMap(byGa));
        }

        Builder pomXml(final Path pomXml) {
            final Module.Builder module = new Module.Builder(rootDirectory, pomXml, encoding);
            modulesByPath.put(module.pomPath, module);
            modulesByGa.put(module.moduleGav.getGa(), module);
            for (Profile.Builder profile : module.profiles) {
                for (String path : profile.children) {
                    if (!modulesByPath.containsKey(path)) {
                        pomXml(rootDirectory.resolve(path));
                    }
                }
            }
            return this;
        }
    }

    public static class Dependency extends GavExpression {
        private final String scope;

        public Dependency(Expression groupId, Expression artifactId, Expression version, String scope) {
            super(groupId, artifactId, version);
            this.scope = scope;
        }

        public String getScope() {
            return scope;
        }
    }

    /**
     * A set of {@link DomEdit}s.
     */
    static class DomEdits {
        final Map<String, Set<Transformation>> domEditsByPath = new LinkedHashMap<>();

        /**
         * @param path a file system path to a {@code pom.xml} file relative to {@link MavenSourceTree#rootDirectory}
         * @param domEdit the operation to add
         */
        public void add(String path, Transformation domEdit) {
            Set<Transformation> edits = domEditsByPath.get(path);
            if (edits == null) {
                domEditsByPath.put(path, edits = new LinkedHashSet<>());
            }
            edits.add(domEdit);
        }

        /**
         * Perform the operations added via {@link #add(String, DomEdit)}.
         *
         * @param rootDirectory
         * @param encoding
         */
        public void perform(Path rootDirectory, Charset encoding, SimpleElementWhitespace simpleElementWhitespace) {
            for (Entry<String, Set<Transformation>> e : domEditsByPath.entrySet()) {
                final Path pomXml = rootDirectory.resolve(e.getKey());
                PomTransformer transformer = new PomTransformer(pomXml, encoding, simpleElementWhitespace);
                final Set<Transformation> transformations = e.getValue();
                transformer.transform(transformations);
            }
        }
    }

    /**
     * An expression used in Maven {@code pom.xml} files, such as <code>${my-property}</code> or
     * <code>my-prefix-${my-property}</code>
     */
    public interface Expression {

        /**
         * A constant containing no <code>${...}</code> placeholders.
         */
        class Constant implements Expression {
            final String expression;

            public Constant(String expression) {
                super();
                this.expression = expression;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj)
                    return true;
                if (obj == null)
                    return false;
                if (getClass() != obj.getClass())
                    return false;
                return this.expression.equals(((Constant) obj).expression);
            }

            @Override
            public String evaluate(MavenSourceTree tree, Predicate<Profile> isProfileActive) {
                return expression;
            }

            public String getExpression() {
                return expression;
            }

            @Override
            public String getRawExpression() {
                return expression;
            }

            @Override
            public int hashCode() {
                return expression.hashCode();
            }

            @Override
            public String toString() {
                return expression;
            }

        }

        /**
         * An {@link Expression} containing <code>${...}</code> placeholders.
         */
        class NonConstant extends Constant {

            private static final Pattern PLACE_HOLDER_PATTERN = Pattern.compile("\\$\\{([^\\}]+)\\}");

            static void evaluateExpression(final NonConstant expression, final MavenSourceTree tree,
                    final Predicate<Profile> isProfileActive, final Consumer<ValueDefinition> consumer) {
                final Module context = tree.getModulesByGa().get(expression.ga);
                final String src = expression.expression;
                final Matcher m = PLACE_HOLDER_PATTERN.matcher(src);
                int offset = 0;
                while (m.find()) {
                    if (m.start() > offset) {
                        consumer.accept(new ValueDefinition(context, null,
                                new Expression.Constant(src.substring(offset, m.start()))));
                    }
                    final String propName = m.group(1);
                    evaluateProperty(tree, context, isProfileActive, propName, consumer);
                    offset = m.end();
                }
                if (offset < src.length()) {
                    consumer.accept(new ValueDefinition(context, null,
                            new Expression.Constant(src.substring(offset, src.length()))));
                }
            }

            static void evaluateProperty(final MavenSourceTree tree, final Module context,
                    final Predicate<Profile> isProfileActive, final String propertyName,
                    final Consumer<ValueDefinition> consumer) {

                if ("project.version".equals(propertyName)) {
                    consumer.accept(new ValueDefinition(context, PROJECT_VERSION_XPATH, context.getGav().getVersion()));
                } else if ("project.groupId".equals(propertyName)) {
                    consumer.accept(
                            new ValueDefinition(context, PROJECT_GROUP_ID_XPATH, context.getGav().getGroupId()));
                } else if ("project.artifactId".equals(propertyName)) {
                    consumer.accept(
                            new ValueDefinition(context, PROJECT_ARTIFACT_ID_XPATH, context.getGav().getGroupId()));
                } else {
                    final ValueDefinition propertyDefinition = context.findPropertyDefinition(propertyName,
                            isProfileActive);
                    if (propertyDefinition == null) {
                        /* No such property: climb up */
                        final Module parent = tree.getDeclaredParentModule(context);
                        if (parent == null) {
                            /* unable to resolve */
                            throw new IllegalStateException(String.format(
                                    "Unable to resolve property [%s]: root of the module hierarchy reached",
                                    propertyName));
                        } else {
                            evaluateProperty(tree, parent, isProfileActive, propertyName, consumer);
                        }
                    } else {
                        consumer.accept(propertyDefinition);
                    }

                }
            }

            /**
             * {@link Ga} of the module where the resolution of this {@link Expression} should start. For expressions
             * used in {@code <parent>} block, this would be the parent {@link Ga}. For all other cases, this is the
             * {@link Gav} of the {@code pom.xml} where this {@link Expression} occurs.
             */
            private final Ga ga;

            public NonConstant(String expression, Ga ga) {
                super(expression);
                this.ga = ga;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj)
                    return true;
                if (!super.equals(obj))
                    return false;
                if (getClass() != obj.getClass())
                    return false;
                NonConstant other = (NonConstant) obj;
                if (ga == null) {
                    if (other.ga != null)
                        return false;
                } else if (!ga.equals(other.ga))
                    return false;
                return true;
            }

            /** {@inheritDoc} */
            @Override
            public String evaluate(final MavenSourceTree tree, final Predicate<Profile> isProfileActive) {

                final StringBuffer result = new StringBuffer();
                final Consumer<ValueDefinition> consumer = new Consumer<ValueDefinition>() {
                    @Override
                    public void accept(ValueDefinition propertyDefinition) {
                        final Expression propertyValue = propertyDefinition.getValue();
                        if (propertyValue instanceof Constant) {
                            result.append(propertyValue.evaluate(tree, isProfileActive));
                        } else if (propertyValue instanceof NonConstant) {
                            evaluateExpression((NonConstant) propertyValue, tree, isProfileActive, this);
                        } else {
                            throw new IllegalStateException();
                        }
                    }
                };

                evaluateExpression(this, tree, isProfileActive, consumer);
                return result.toString();
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = super.hashCode();
                result = prime * result + ((ga == null) ? 0 : ga.hashCode());
                return result;
            }

        }

        /**
         * @param expression the expression possibly containing <code>${...}</code> placeholders
         * @param ga the {@link Ga} against which the resulting {@link Expression} should be evaluated
         * @return a {@link NonConstant} or {@link Constant} depending on whether the given {@code expression} contains
         *         <code>${</code>
         */
        static Expression of(String expression, Ga ga) {
            if (expression.indexOf("${") >= 0) {
                return new NonConstant(expression, ga);
            } else {
                return new Constant(expression);
            }
        }

        /**
         * Evaluate this {@link Expression} by recursively expanding all ${...} placeholders.
         *
         * @param tree the {@link MavenSourceTree} against which this {@link Expression} should be evaluated
         * @param isProfileActive the profile selector to use when evaluating properties
         * @return the result of evaluation
         */
        String evaluate(MavenSourceTree tree, Predicate<Profile> isProfileActive);

        /**
         * @return the raw source of this {@link Expression}
         */
        String getRawExpression();
    }

    /**
     * A {@link Ga} combined with a version {@link Expression}.
     */
    public static class GavExpression {

        public static class DependencyBuilder extends PlainGavBuilder {

            private String scope = "compile";

            public DependencyBuilder(ModuleGavBuilder module) {
                super(module);
            }

            public Dependency build() {
                final Ga ga = module.getGa();
                return new Dependency(Expression.of(groupId, ga), Expression.of(artifactId, ga),
                        version != null ? Expression.of(version, ga) : null, scope);
            }

            public void scope(String scope) {
                this.scope = scope;
            }

        }

        interface GavBuilder {
            void artifactId(String artifactId);

            void groupId(String groupId);

            void version(String version);

        }

        static class ModuleGavBuilder extends ParentGavBuilder {
            private Ga ga;
            private final ParentGavBuilder parent;

            ModuleGavBuilder(ParentGavBuilder parent) {
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

        static class ParentGavBuilder implements GavBuilder {

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

        private final Expression artifactId;
        private volatile Ga ga;
        private final Expression groupId;

        private final Expression version;

        GavExpression(Expression groupId, Expression artifactId, Expression version) {
            this.groupId = Objects.requireNonNull(groupId, "groupId");
            this.artifactId = Objects.requireNonNull(artifactId, "artifactId");
            this.version = version;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            GavExpression other = (GavExpression) obj;
            if (artifactId == null) {
                if (other.artifactId != null)
                    return false;
            } else if (!artifactId.equals(other.artifactId))
                return false;
            if (groupId == null) {
                if (other.groupId != null)
                    return false;
            } else if (!groupId.equals(other.groupId))
                return false;
            if (version == null) {
                if (other.version != null)
                    return false;
            } else if (!version.equals(other.version))
                return false;
            return true;
        }

        public Expression getArtifactId() {
            return artifactId;
        }

        public Expression getGroupId() {
            return groupId;
        }

        public Expression getVersion() {
            return version;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
            result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
            result = prime * result + ((version == null) ? 0 : version.hashCode());
            return result;
        }

        public Gav resolve(final MavenSourceTree tree, Predicate<Profile> isProfileActive) {
            return new Gav(groupId.evaluate(tree, isProfileActive), artifactId.evaluate(tree, isProfileActive),
                    version.evaluate(tree, isProfileActive));
        }

        public Ga resolveGa(final MavenSourceTree tree, Predicate<Profile> isProfileActive) {
            if (ga == null) {
                ga = new Ga(groupId.evaluate(tree, isProfileActive), artifactId.evaluate(tree, isProfileActive));
            }
            return ga;
        }

        @Override
        public String toString() {
            return groupId.getRawExpression() + ":" + artifactId.getRawExpression() + ":" + version;
        }

    }

    /**
     * A Maven module.
     */
    public static class Module {

        /**
         * A {@link Module} builder.
         */
        static class Builder {
            Profile.Builder implicitProfile = new Profile.Builder();
            final ModuleGavBuilder moduleGav;
            final ParentGavBuilder parentGav;
            /** Relative to source tree root directory */
            final String pomPath;
            List<Profile.Builder> profiles;

            Builder(Path rootDirectory, Path pomXml, Charset encoding) {
                parentGav = new ParentGavBuilder();
                moduleGav = new ModuleGavBuilder(parentGav);
                profiles = new ArrayList<>();
                profiles.add(implicitProfile);

                final Stack<String> elementStack = new Stack<>();
                final Path dir = pomXml.getParent();
                try (Reader in = Files.newBufferedReader(pomXml, encoding)) {
                    final XMLEventReader r = xmlInputFactory.createXMLEventReader(in);
                    this.pomPath = Utils.toUnixPath(rootDirectory.relativize(pomXml).toString());

                    final Stack<GavExpression.GavBuilder> gavBuilderStack = new Stack<>();
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
                                profile.children.add(Utils.toUnixPath(rootRelPath));
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
                                profile = new Profile.Builder();
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
                    throw new RuntimeException(e1);
                }
            }

            public Module build() {
                final List<Profile> useProfiles = Collections
                        .unmodifiableList(profiles.stream().map(Profile.Builder::build).collect(Collectors.toList()));
                profiles = null;
                return new Module(pomPath, moduleGav.build(), parentGav.build(), useProfiles);
            }

        }

        /**
         * A Maven profile.
         */
        public static class Profile {

            /**
             * A Maven {@link Profile} builder.
             */
            public static class Builder {
                Set<String> children = new LinkedHashSet<>();
                List<DependencyBuilder> dependencies = new ArrayList<>();
                List<DependencyBuilder> dependencyManagement = new ArrayList<>();
                List<PlainGavBuilder> extensions = new ArrayList<>();
                private String id;
                List<PluginGavBuilder> pluginManagement = new ArrayList<>();
                List<PluginGavBuilder> plugins = new ArrayList<>();
                List<PropertyBuilder> properties = new ArrayList<>();

                public Profile build() {
                    final Set<String> useChildren = Collections.unmodifiableSet(children);
                    children = null;
                    final Set<Dependency> useDependencies = Collections
                            .<Dependency>unmodifiableSet((Set<Dependency>)dependencies.stream().map(DependencyBuilder::build)
                                    .collect(Collectors.toCollection(LinkedHashSet::new)));
                    dependencies = null;
                    final Set<Dependency> useManagedDependencies = Collections
                            .<Dependency>unmodifiableSet((Set<Dependency>)dependencyManagement.stream().map(DependencyBuilder::build)
                                    .collect(Collectors.toCollection(LinkedHashSet::new)));
                    dependencyManagement = null;
                    final Set<Plugin> usePlugins = Collections.<Plugin>unmodifiableSet((Set<Plugin>)plugins.stream()
                            .map(PluginGavBuilder::build).collect(Collectors.toCollection(LinkedHashSet::new)));
                    plugins = null;
                    final Set<Plugin> usePluginManagement = Collections
                            .<Plugin>unmodifiableSet((Set<Plugin>)pluginManagement.stream().map(PluginGavBuilder::build)
                                    .collect(Collectors.toCollection(LinkedHashSet::new)));
                    pluginManagement = null;

                    final Set<GavExpression> useExtensions = Collections.<GavExpression>unmodifiableSet((Set<GavExpression>)extensions
                            .stream().map(PlainGavBuilder::build).collect(Collectors.toCollection(LinkedHashSet::new)));
                    extensions = null;

                    final Map<String, Expression> useProps = Collections.unmodifiableMap(properties.stream() //
                            .map(PropertyBuilder::build) //
                            .collect( //
                                    Collectors.toMap( //
                                            e -> e.getKey(), //
                                            e -> e.getValue(), //
                                            (u, v) -> {
                                                throw new IllegalStateException(String.format("Duplicate key %s", u));
                                            }, //
                                            LinkedHashMap::new //
                                    ) //
                            ) //
                    );
                    this.properties = null;
                    return new Profile(id, useChildren, useDependencies, useManagedDependencies, usePlugins,
                            usePluginManagement, useExtensions, useProps);
                }

                public void id(String id) {
                    this.id = id;
                }
            }

            static class PropertyBuilder {
                final ModuleGavBuilder ga;

                final String key;
                final String value;

                PropertyBuilder(String key, String value, ModuleGavBuilder ga) {
                    super();
                    this.key = key;
                    this.value = value;
                    this.ga = ga;
                }

                public Map.Entry<String, Expression> build() {
                    final Expression val = Expression.of(value, ga.getGa());
                    return new AbstractMap.SimpleImmutableEntry<>(key, val);
                }
            }

            /** A path to child project's pom.xml relative to {@link MavenSourceTree#rootDirectory} */
            private final Set<String> children;
            private final Set<Dependency> dependencies;
            private final Set<Dependency> dependencyManagement;
            private final Set<GavExpression> extensions;
            private final String id;
            private final Set<Plugin> pluginManagement;

            private final Set<Plugin> plugins;
            private final Map<String, Expression> properties;

            Profile(String id, Set<String> children, Set<Dependency> dependencies, Set<Dependency> dependencyManagement,
                    Set<Plugin> plugins, Set<Plugin> pluginManagement, Set<GavExpression> extensions,
                    Map<String, Expression> properties) {
                super();
                this.id = id;
                this.children = children;
                this.dependencies = dependencies;
                this.dependencyManagement = dependencyManagement;
                this.plugins = plugins;
                this.pluginManagement = pluginManagement;
                this.extensions = extensions;
                this.properties = properties;
            }

            /**
             * @return a {@link Set} of paths to {@code pom.xml} files of child modules of this Module realtive to
             *         {@link MavenSourceTree#getRootDirectory()}
             */
            public Set<String> getChildren() {
                return children;
            }

            /**
             * @return a {@link Set} of dependencies declared in this {@link Profile}
             */
            public Set<Dependency> getDependencies() {
                return dependencies;
            }

            /**
             * @return a {@link Set} of dependencyManagement entries declared in this {@link Profile}
             */
            public Set<Dependency> getDependencyManagement() {
                return dependencyManagement;
            }

            public Set<GavExpression> getExtensions() {
                return extensions;
            }

            /**
             * @return an ID of this profile or {@code null} if this is the representation of a top level profile-less
             *         dependencies, plugins, etc.
             */
            public String getId() {
                return id;
            }

            /**
             * @return a {@link Set} of pluginManagement entries declared in this {@link Profile}
             */
            public Set<Plugin> getPluginManagement() {
                return pluginManagement;
            }

            /**
             * @return a {@link Set} of plugins declared in this {@link Profile}
             */
            public Set<Plugin> getPlugins() {
                return plugins;
            }

            /**
             * @return the {@link Map} of properties declared in this {@link Profile}
             */
            public Map<String, Expression> getProperties() {
                return properties;
            }

        }

        private final GavExpression gav;
        private final GavExpression parentGav;
        /** Relative to source tree root directory */
        private final String pomPath;

        private final List<Profile> profiles;

        Module(String pomPath, GavExpression gav, GavExpression parentGa, List<Profile> profiles) {
            super();
            this.pomPath = pomPath;
            this.gav = gav;
            this.parentGav = parentGa;
            this.profiles = profiles;
        }

        /**
         * Goes through active profiles and find the definition of the property having the given {@code propertyName}.
         *
         * @param propertyName the property name to find a definition for
         * @param isProfileActive tells which profiles are active
         * @return the {@link ValueDefinition} of the seeked property or {@code null} if no such property is defined in
         *         this {@link Module}
         */
        public ValueDefinition findPropertyDefinition(String propertyName, Predicate<Profile> isProfileActive) {
            final ListIterator<Profile> it = this.profiles.listIterator(this.profiles.size());
            while (it.hasPrevious()) {
                final Profile p = it.previous();
                if (isProfileActive.test(p)) {
                    final Expression result = p.properties.get(propertyName);
                    if (result != null) {
                        final String xPath = xPathProfile(p.getId(), "properties", propertyName);
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
         * @return the {@link List} of profiles defined in this {@link Module}. Note that the top level profile-less
         *         dependencies, dependencyManagement, etc. are defined in {@link Module} with {@code id} {@code null}.
         */
        public List<Profile> getProfiles() {
            return profiles;
        }

        /**
         * @param childPomPath a path to {@code pom.xml} file relative to {@link MavenSourceTree#rootDirectory}
         * @param isProfileActive
         * @return {@code true} if the {@code pom.xml} represented by this {@link Module} has a {@code <module>} with
         *         the given pom.xml path or {@code false} otherwise
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

    }

    public static class Plugin extends GavExpression {
        private final Set<GavExpression> dependencies;

        public Plugin(Expression groupId, Expression artifactId, Expression version, Set<GavExpression> dependencies) {
            super(groupId, artifactId, version);
            this.dependencies = dependencies;
        }

        public Set<GavExpression> getDependencies() {
            return dependencies;
        }
    }

    /**
     * Decides which {@link ValueDefinition}s delivered via {@link SimplePlaceHolderConsumer#accept(ValueDefinition)}
     * are relevant for setting a new version and eventually adds a new {@link DomEdit} operation to
     * {@link SimplePlaceHolderConsumer#edits}.
     */
    static class SimplePlaceHolderConsumer implements Consumer<ValueDefinition> {

        int counter = 0;

        final DomEdits edits;
        final String newValue;

        SimplePlaceHolderConsumer(DomEdits edits, String newValue) {
            super();
            this.edits = edits;
            this.newValue = newValue;
        }

        @Override
        public void accept(ValueDefinition valueDefinition) {
            if (counter++ > 0) {
                throw new IllegalStateException(String.format("Cannot call [%s] more than once",
                        SimplePlaceHolderConsumer.class.getSimpleName()));
            }
            if (valueDefinition.getXPath() == null) {
                throw new IllegalStateException(String.format("[%s] cannot accept a value without an xPath",
                        SimplePlaceHolderConsumer.class.getSimpleName()));
            } else if (PROJECT_VERSION_XPATH.equals(valueDefinition.getXPath())) {
                /* ignore */
            } else {
                edits.add(valueDefinition.getModule().getPomPath(), Transformation.setTextValue(valueDefinition.getXPath(), newValue));
            }
        }

    }

    /**
     * Where some {@link Expression}'s value is defined - a semi-result of an evaluation of some {@link Expression}.
     */
    static class ValueDefinition {
        private final Module module;
        private final Expression value;
        private final String xPath;

        ValueDefinition(Module module, String xPath, Expression value) {
            super();
            this.module = module;
            this.xPath = xPath;
            this.value = value;
        }

        /**
         * @return the {@link Module} in which {@link #value} is defined
         */
        public Module getModule() {
            return module;
        }

        /**
         * @return the value of the {@link Expression}
         */
        public Expression getValue() {
            return value;
        }

        /**
         * @return an XPath expression pointing at the element where the {@link #value} is defined
         */
        public String getXPath() {
            return xPath;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(MavenSourceTree.class);

    private static final String PROJECT_ARTIFACT_ID_XPATH = "/*[local-name()='project']/*[local-name()='artifactId']";
    private static final String PROJECT_GROUP_ID_XPATH = "/*[local-name()='project']/*[local-name()='groupId']";
    private static final String PROJECT_VERSION_XPATH = "/*[local-name()='project']/*[local-name()='version']";

    private static final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

    /**
     * @param rootPomXml the path to the {@code pom.xml} file of the root Maven module
     * @param encoding the encoding to use when reading {@code pom.xml} files in the given file tree
     * @return a new {@link MavenSourceTree}
     */
    public static MavenSourceTree of(Path rootPomXml, Charset encoding) {
        return new Builder(rootPomXml.getParent(), encoding).pomXml(rootPomXml).build();
    }

    static String xPathDependency(String dependencyKind, GavExpression gav) {
        return "/*[local-name()='" + dependencyKind + "' and *[local-name()='groupId' and text()='"
                + gav.getGroupId().getRawExpression() + "'] and *[local-name()='artifactId' and text()='"
                + gav.getArtifactId().getRawExpression() + "']]";
    }

    static String xPathDependencyVersion(String dependencyKind, GavExpression gav) {
        return xPathDependency(dependencyKind, gav) + "/*[local-name()='version']";
    }

    static String xPathProfile(String id, String... elements) {
        return "/*[local-name()='project']" + (id == null ? ""
                : "/*[local-name()='profiles']/*[local-name()='profile' and *[local-name()='id' and text()='" + id
                        + "']]")
                + PomTransformer.anyNs(elements);
    }

    private final Charset encoding;

    private final Map<Ga, Module> modulesByGa;

    private final Map<String, Module> modulesByPath;

    private final Path rootDirectory;

    MavenSourceTree(Path rootDirectory, Charset encoding, Map<String, Module> modulesByPath,
            Map<Ga, Module> modulesByGa) {
        this.rootDirectory = rootDirectory;
        this.modulesByPath = modulesByPath;
        this.modulesByGa = modulesByGa;
        this.encoding = encoding;
    }

    private void addDeclaredParents(final Module module, final Set<Ga> result, Set<Ga> visited,
            Predicate<Profile> isProfileActive) {
        Module parent;
        Module child = module;
        while ((parent = getDeclaredParentModule(child)) != null) {
            addModule(parent.getGav().resolveGa(this, isProfileActive), result, visited, isProfileActive);
            child = parent;
        }
    }

    private void addModule(Ga includeGa, Set<Ga> result, Set<Ga> visited, Predicate<Profile> isProfileActive) {
        final Module module = modulesByGa.get(includeGa);
        if (module != null && !visited.contains(includeGa)) {
            visited.add(includeGa);
            result.add(includeGa);
            addProperParents(module, result, visited, isProfileActive);
            addDeclaredParents(module, result, visited, isProfileActive);
            for (Profile p : module.profiles) {
                if (isProfileActive.test(p)) {
                    for (GavExpression depGa : p.dependencies) {
                        addModule(depGa.resolveGa(this, isProfileActive), result, visited, isProfileActive);
                    }
                    for (GavExpression depGa : p.plugins) {
                        addModule(depGa.resolveGa(this, isProfileActive), result, visited, isProfileActive);
                    }
                }
            }
        }
    }

    private void addProperParents(final Module module, final Set<Ga> result, Set<Ga> visited,
            Predicate<Profile> isProfileActive) {
        Module parent;
        Module child = module;
        while ((parent = getProperParentModule(child, isProfileActive)) != null) {
            addModule(parent.getGav().resolveGa(this, isProfileActive), result, visited, isProfileActive);
            child = parent;
        }
    }

    /**
     * Returns a {@link Set} that contains all given {@code initialModules} and all such modules from the current
     * {@link MavenSourceTree} that are reachable from the {@code initialModules} via <i>depends on</i> and <i>is parent
     * of</i> relationships.
     *
     * @param initialModules
     * @return {@link Set} of {@code groupId:artifactId}
     */
    public Set<Ga> computeModuleClosure(Collection<Ga> initialModules, Predicate<Profile> isProfileActive) {
        final Set<Ga> visited = new HashSet<>();
        final Set<Ga> result = new LinkedHashSet<>();
        for (Ga includeGa : initialModules) {
            addModule(includeGa, result, visited, isProfileActive);
        }
        return result;
    }

    /**
     * Returns the complement of the given {@code inputSet}, the universe being the set of all modules in this
     * {@link MavenSourceTree}.
     *
     * @param inputSet the set whose complement is to be computed
     * @return a new {@link Set} with stable ordering
     */
    public Set<Ga> complement(Set<Ga> inputSet) {
        final Set<Ga> result = new LinkedHashSet<>();
        for (Ga ga : modulesByGa.keySet()) {
            if (!inputSet.contains(ga)) {
                result.add(ga);
            }
        }
        return result;
    }

    void edit(final String newVersion, final Predicate<Profile> isProfileActive, final DomEdits edits, Module module,
            final Expression moduleVersion, String xPath) {
        if (moduleVersion instanceof NonConstant) {
            NonConstant.evaluateExpression((NonConstant) moduleVersion, this, isProfileActive,
                    new SimplePlaceHolderConsumer(edits, newVersion));
        } else if (moduleVersion instanceof Constant) {
            edits.add(module.getPomPath(), Transformation.setTextValue(xPath, newVersion));
        }
    }

    void editDependencies(String newVersion, final Predicate<Profile> isProfileActive, final DomEdits edits,
            Module module, String profileId, Set<Dependency> dependencies, String... path) {
        if (!dependencies.isEmpty()) {
            final String xPathProfile = xPathProfile(profileId, path);
            for (GavExpression gav : dependencies) {
                if (gav.getVersion() != null && modulesByGa.containsKey(gav.resolveGa(this, isProfileActive))) {
                    final String xPath = xPathProfile + xPathDependencyVersion("dependency", gav);
                    edit(newVersion, isProfileActive, edits, module, gav.getVersion(), xPath);
                }
            }
        }
    }

    void editExtensions(String newVersion, final Predicate<Profile> isProfileActive, final DomEdits edits,
            Module module, String profileId, Set<GavExpression> dependencies, String... path) {
        if (!dependencies.isEmpty()) {
            final String xPathProfile = xPathProfile(profileId, path);
            for (GavExpression gav : dependencies) {
                if (gav.getVersion() != null && modulesByGa.containsKey(gav.resolveGa(this, isProfileActive))) {
                    final String xPath = xPathProfile + xPathDependencyVersion("extension", gav);
                    edit(newVersion, isProfileActive, edits, module, gav.getVersion(), xPath);
                }
            }
        }
    }

    void editPlugins(String newVersion, final Predicate<Profile> isProfileActive, final DomEdits edits, Module module,
            String profileId, Set<Plugin> dependencies, String... path) {
        if (!dependencies.isEmpty()) {
            final String xPathProfile = xPathProfile(profileId, path);
            for (Plugin pluginGav : dependencies) {
                final boolean editPlugin = pluginGav.getVersion() != null
                        && modulesByGa.containsKey(pluginGav.resolveGa(this, isProfileActive));
                if (editPlugin || !pluginGav.getDependencies().isEmpty()) {
                    if (editPlugin) {
                        final String xPath = xPathProfile + xPathDependencyVersion("plugin", pluginGav);
                        edit(newVersion, isProfileActive, edits, module, pluginGav.getVersion(), xPath);
                    }
                    if (!pluginGav.getDependencies().isEmpty()) {
                        final String prefix = xPathProfile + xPathDependency("plugin", pluginGav)
                                + PomTransformer.anyNs("dependencies");
                        for (GavExpression dep : pluginGav.getDependencies()) {
                            if (dep.getVersion() != null
                                    && modulesByGa.containsKey(dep.resolveGa(this, isProfileActive))) {
                                final String xPath = prefix + xPathDependencyVersion("dependency", dep);
                                edit(newVersion, isProfileActive, edits, module, dep.getVersion(), xPath);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Evaluate the given {@link Expression} by recursively expanding all its ${...} placeholders.
     *
     * @param expression the {@link Expression} to evaluate
     * @param isProfileActive the profile selector to use when evaluating properties
     * @return the result of evaluation
     */
    public String evaluate(Expression expression, Predicate<Profile> isProfileActive) {
        return expression.evaluate(this, isProfileActive);
    }

    /**
     * Goes through dependencies and plugins this source tree requires and returns the set of those satisfying
     * {@link GavSet#contains(String, String)}.
     *
     * @param gavSet
     * @param isProfileActive
     * @return a
     */
    public Set<Ga> filterDependencies(GavSet gavSet, final Predicate<Profile> isProfileActive) {
        final Set<Ga> result = new TreeSet<>();
        for (Module module : getModulesByGa().values()) {
            final GavExpression parentGav = module.getParentGav();
            if (parentGav != null) {
                final Ga ga = parentGav.resolveGa(this, isProfileActive);
                if (gavSet.contains(ga.getGroupId(), ga.getArtifactId())) {
                    result.add(ga);
                }
            }
            for (Profile p : module.getProfiles()) {
                if (isProfileActive.test(p)) {
                    for (Dependency depGa : p.getDependencies()) {
                        final Ga ga = depGa.resolveGa(this, isProfileActive);
                        if (gavSet.contains(ga.getGroupId(), ga.getArtifactId())) {
                            result.add(ga);
                        }
                    }
                    for (Dependency depGa : p.getDependencyManagement()) {
                        final Ga ga = depGa.resolveGa(this, isProfileActive);
                        if ("import".equals(depGa.getScope()) && gavSet.contains(ga.getGroupId(), ga.getArtifactId())) {
                            result.add(ga);
                        }
                    }
                    for (Plugin plugin : p.getPlugins()) {
                        {
                            final Ga ga = plugin.resolveGa(this, isProfileActive);
                            if (gavSet.contains(ga.getGroupId(), ga.getArtifactId())) {
                                result.add(ga);
                            }
                        }
                        for (GavExpression plugnDep : plugin.getDependencies()) {
                            final Ga plugnDepGa = plugnDep.resolveGa(this, isProfileActive);
                            if (gavSet.contains(plugnDepGa.getGroupId(), plugnDepGa.getArtifactId())) {
                                result.add(plugnDepGa);
                            }
                        }
                    }
                    for (Plugin plugin : p.getPluginManagement()) {
                        {
                            final Ga ga = plugin.resolveGa(this, isProfileActive);
                            if (gavSet.contains(ga.getGroupId(), ga.getArtifactId())) {
                                result.add(ga);
                            }
                        }
                        for (GavExpression pluginDep : plugin.getDependencies()) {
                            final Ga plugnDepGa = pluginDep.resolveGa(this, isProfileActive);
                            if (gavSet.contains(plugnDepGa.getGroupId(), plugnDepGa.getArtifactId())) {
                                result.add(plugnDepGa);
                            }
                        }
                    }
                    for (GavExpression ext : p.getExtensions()) {
                        final Ga ga = ext.resolveGa(this, isProfileActive);
                        if (gavSet.contains(ga.getGroupId(), ga.getArtifactId())) {
                            result.add(ga);
                        }
                    }
                }
            }
        }
        return result;
    }

    Module getDeclaredParentModule(Module child) {
        final GavExpression parentGa = child.parentGav;
        if (parentGa != null) {
            return modulesByGa.get(parentGa.resolveGa(this, null));
        } else {
            return null;
        }
    }

    /**
     * @return a {@link Charset} to use when reading and writing the {@code pom.xml} files in this
     *         {@link MavenSourceTree}
     */
    public Charset getEncoding() {
        return encoding;
    }

    /**
     * @return a {@link Map} of modules in this {@link MavenSourceTree} by their {@code groupId:artifactId}
     */
    public Map<Ga, Module> getModulesByGa() {
        return modulesByGa;
    }

    /**
     * @return a {@link Map} of modules in this {@link MavenSourceTree} by their {@code pom.xml} path realtive to
     *         {@link #getRootDirectory()}
     */
    public Map<String, Module> getModulesByPath() {
        return modulesByPath;
    }

    /**
     * @param child
     * @return the {@link Module} having the given gild in its {@code <modules>}
     */
    Module getProperParentModule(Module child, Predicate<Profile> isProfileActive) {
        final GavExpression parentGa = child.parentGav;
        if (parentGa != null) {
            final Module declaredParent = modulesByGa.get(parentGa.resolveGa(this, isProfileActive));
            if (declaredParent != null && declaredParent.hasChild(child.pomPath, isProfileActive)) {
                return declaredParent;
            }
            return modulesByGa.values().stream().filter(m -> m.hasChild(child.pomPath, isProfileActive)).findFirst()
                    .orElse(null);
        }
        return null;
    }

    /**
     * @return the root directory of this {@link MavenSourceTree}
     */
    public Path getRootDirectory() {
        return rootDirectory;
    }

    /**
     * @return the module in the directory returned by {@link #getRootDirectory()}
     */
    public Module getRootModule() {
        return modulesByPath.get("pom.xml");
    }

    /**
     * A fast alternative to {@code mvn versions:set -DnewVersion=...}
     *
     * @param newVersion the new version to set
     * @param isProfileActive a {@link Profile} filter, see {@link #profiles(String...)}
     * @param simpleElementWhitespace see {@link SimpleElementWhitespace}
     */
    public void setVersions(final String newVersion, final Predicate<Profile> isProfileActive, SimpleElementWhitespace simpleElementWhitespace) {
        final DomEdits edits = new DomEdits();
        for (Module module : modulesByGa.values()) {

            /* self */
            final GavExpression parentGav = module.getParentGav();
            final Expression moduleVersion = module.getGav().getVersion();
            if (parentGav == null || !moduleVersion.equals(module.getParentGav().getVersion())) {
                /* explicitly defined version */
                edit(newVersion, isProfileActive, edits, module, moduleVersion, PomTransformer.anyNs("project", "version"));
            }

            /* parent */
            if (parentGav != null && modulesByGa.containsKey(parentGav.resolveGa(this, isProfileActive))) {
                final Expression parentVersion = parentGav.getVersion();
                edit(newVersion, isProfileActive, edits, module, parentVersion, PomTransformer.anyNs("project", "parent", "version"));
            }

            for (Profile profile : module.getProfiles()) {
                /* dependencyManagement */
                final String profileId = profile.getId();
                editDependencies(newVersion, isProfileActive, edits, module, profileId,
                        profile.getDependencyManagement(), "dependencyManagement", "dependencies");
                editDependencies(newVersion, isProfileActive, edits, module, profileId, profile.getDependencies(),
                        "dependencies");
                editPlugins(newVersion, isProfileActive, edits, module, profileId, profile.getPluginManagement(),
                        "build", "pluginManagement", "plugins");
                editPlugins(newVersion, isProfileActive, edits, module, profileId, profile.getPlugins(), "build",
                        "plugins");

                editExtensions(newVersion, isProfileActive, edits, module, profileId, profile.getExtensions(), "build",
                        "extensions");

            }
        }
        edits.perform(rootDirectory, encoding, simpleElementWhitespace);
    }

    Map<String, Set<Path>> unlinkUneededModules(Set<Ga> includes, Module module,
            Map<String, Set<Path>> removeChildPaths, Predicate<Profile> isProfileActive) {
        for (Profile p : module.profiles) {
            if (isProfileActive.test(p)) {
                for (String childPath : p.children) {
                    final Module childModule = modulesByPath.get(childPath);
                    final GavExpression childGa = childModule.gav;
                    if (!includes.contains(childGa.resolveGa(this, isProfileActive))) {
                        Set<Path> set = removeChildPaths.get(module.pomPath);
                        if (set == null) {
                            set = new LinkedHashSet<Path>();
                            removeChildPaths.put(module.pomPath, set);
                        }
                        set.add(rootDirectory.resolve(childPath).normalize());
                    } else {
                        unlinkUneededModules(includes, childModule, removeChildPaths, isProfileActive);
                    }
                }
            }
        }
        return removeChildPaths;
    }

    /**
     * Edit the {@code pom.xml} files so that just the given @{@code includes} are buildable, removing all unnecessary
     * {@code <module>} elements from {@code pom.xml} files.
     *
     * @param includes a list of {@code groupId:artifactId}s
     * @param isProfileActive a {@link Profile} filter, see {@link #profiles(String...)}
     */
    public void unlinkUneededModules(Set<Ga> includes, Predicate<Profile> isProfileActive, Charset encoding, SimpleElementWhitespace simpleElementWhitespace) {
        final Module rootModule = modulesByPath.get("pom.xml");
        final Map<String, Set<Path>> removeChildPaths = unlinkUneededModules(includes, rootModule,
                new LinkedHashMap<String, Set<Path>>(), isProfileActive);
        for (Entry<String, Set<Path>> e : removeChildPaths.entrySet()) {
            final Set<Path> paths = e.getValue();
            if (!paths.isEmpty()) {
                unlinkUneededModules(rootDirectory.resolve(e.getKey()), paths, encoding, simpleElementWhitespace);
            }
        }
    }

    void unlinkUneededModules(Path pomXml, Set<Path> removeChildPaths, Charset encoding, SimpleElementWhitespace simpleElementWhitespace) {

        final Path parentDir = pomXml.getParent();
        final Set<String> relPathsToRemove = removeChildPaths.stream()
                .map(Path::getParent) // path/to/pom.xml -> path/to
                .map(p -> parentDir.relativize(p))
                .map(Path::toString)
                .map(Utils::toUnixPath)
                .collect(Collectors.toSet());
        final PomTransformer transformer = new PomTransformer(pomXml, encoding, simpleElementWhitespace);
        transformer.transform(Transformation.commentModules(relPathsToRemove));
    }

}
