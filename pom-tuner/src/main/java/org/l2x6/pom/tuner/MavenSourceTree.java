/*
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

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;
import eu.maveniverse.domtrip.Text;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.l2x6.pom.tuner.ExpressionEvaluator.ConstantOnlyExpressionEvaluator;
import org.l2x6.pom.tuner.PomTransformer.DomTripUtils;
import org.l2x6.pom.tuner.PomTransformer.GavtcsElement;
import org.l2x6.pom.tuner.PomTransformer.SimpleElementWhitespace;
import org.l2x6.pom.tuner.PomTransformer.Transformation;
import org.l2x6.pom.tuner.PomTransformer.Transformer;
import org.l2x6.pom.tuner.model.Dependency;
import org.l2x6.pom.tuner.model.Expression;
import org.l2x6.pom.tuner.model.Expression.NoSuchPropertyException;
import org.l2x6.pom.tuner.model.Ga;
import org.l2x6.pom.tuner.model.GavExpression;
import org.l2x6.pom.tuner.model.GavSet;
import org.l2x6.pom.tuner.model.Gavtcs;
import org.l2x6.pom.tuner.model.Module;
import org.l2x6.pom.tuner.model.Plugin;
import org.l2x6.pom.tuner.model.Profile;
import org.l2x6.pom.tuner.model.ValueDefinition;
import org.l2x6.pom.tuner.transform.dependencies;
import org.l2x6.pom.tuner.transform.dependencyManagement;
import org.l2x6.pom.tuner.transform.extensions;
import org.l2x6.pom.tuner.transform.modules;
import org.l2x6.pom.tuner.transform.parent;
import org.l2x6.pom.tuner.transform.pluginManagement;
import org.l2x6.pom.tuner.transform.plugins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A representation of a Maven module hierarchy.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  1.0.0
 */
public class MavenSourceTree {
    private static final Pattern PLACE_HOLDER_PATTERN = Pattern.compile("\\$\\{([^\\}]+)\\}");
    static final Function<Document, Text> PROJECT_ARTIFACT_ID_XPATH = document -> document.root()
            .child("groupId").orElseThrow(() -> new IllegalStateException("Cannot find /project/artifactId"))
            .textChild().orElseThrow(() -> new IllegalStateException("/project/artifactId has no text content"));
    static final Function<Document, Text> PROJECT_GROUP_ID_XPATH = document -> document.root()
            .child("groupId").orElseThrow(() -> new IllegalStateException("Cannot find /project/groupId"))
            .textChild().orElseThrow(() -> new IllegalStateException("/project/groupId has no text content"));
    public static final Function<Document, Text> PROJECT_VERSION_XPATH = document -> document.root()
            .child("version").orElseThrow(() -> new IllegalStateException("Cannot find /project/version"))
            .textChild().orElseThrow(() -> new IllegalStateException("/project/version has no text content"));

    public static class ActiveProfiles implements Predicate<Profile> {

        static final Predicate<Profile> EMPTY = new ActiveProfiles();

        /**
         * @param  profileIds the active profiles (can be empty)
         * @return            a new {@link Profile} filter which will hold the named {@code profileIds} for active
         */
        public static Predicate<Profile> of(String... profileIds) {
            return profileIds.length == 0 ? EMPTY : new ActiveProfiles(profileIds);
        }

        /**
         * @param  args Maven command line arguments
         * @return      a new {@link Predicate}
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

        /**
         * @return a {@link Predicate} returning always {@code true}
         */
        public static Predicate<Profile> all() {
            return profile -> true;
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
        private Predicate<Dependency> dependencyExcludes = dep -> false;

        private final Predicate<Profile> activeProfiles;

        Builder(Path rootDirectory, Charset encoding) {
            this(rootDirectory, encoding, ActiveProfiles.all());
        }

        Builder(Path rootDirectory, Charset encoding, Predicate<Profile> activeProfiles) {
            super();
            this.rootDirectory = rootDirectory;
            this.encoding = encoding;
            this.activeProfiles = activeProfiles;
        }

        public MavenSourceTree build() {

            final Map<String, Module> byPath = new LinkedHashMap<>(modulesByPath.size());
            final Map<Ga, Module> byGa = new LinkedHashMap<>(modulesByPath.size());

            final ConstantOnlyExpressionEvaluator evaluator = new ConstantOnlyExpressionEvaluator();
            for (Module.Builder e : modulesByPath.values()) {
                final Module module = e.build();
                byGa.put(evaluator.evaluateGa(module.getGav()), module);
                byPath.put(module.getPomPath(), module);
            }
            return new MavenSourceTree(rootDirectory, encoding, Collections.unmodifiableMap(byPath),
                    Collections.unmodifiableMap(byGa), dependencyExcludes);
        }

        Builder dependencyExcludes(Predicate<Dependency> dependencyExcludes) {
            this.dependencyExcludes = dependencyExcludes;
            return this;
        }

        Builder pomXml(final Path pomXml) {
            return pomXml(pomXml, null);
        }

        Builder pomXml(final Path pomXml, ModuleCallback callback) {
            final Module.Builder module = new Module.Builder(rootDirectory, pomXml, encoding, dependencyExcludes);
            final String pomPath = module.getPomPath();
            if (callback != null) {
                callback.enter(pomPath);
            }
            modulesByPath.put(pomPath, module);
            modulesByGa.put(module.getModuleGav().getGa(), module);
            try {
                for (Profile.Builder profile : module.getProfiles()) {
                    if (activeProfiles.test(profile.build()))
                        for (String path : profile.getChildren()) {
                            if (!modulesByPath.containsKey(path)) {
                                pomXml(rootDirectory.resolve(path), callback);
                            }
                        }
                }
            } catch (Exception e) {
                throw new RuntimeException("Exception occurred when reading " + pomPath, e);
            }
            if (callback != null) {
                callback.exit(pomPath);
            }
            return this;
        }
    }

    /**
     * A utility for inspecting the process of {@link MavenSourceTree} building.
     */
    static class ModuleCallback {
        private final Deque<String> moduleStack = new ArrayDeque<>();

        public void enter(String path) {
            moduleStack.push(path);
        }

        public void exit(String path) {
            moduleStack.pop();
        }
    }

    /**
     * A set of {@link DomEdit}s.
     */
    static class DomEdits {
        final Map<String, Set<Transformer>> domEditsByPath = new LinkedHashMap<>();

        /**
         * @param path    a file system path to a {@code pom.xml} file relative to {@link MavenSourceTree#rootDirectory}
         * @param domEdit the operation to add
         */
        public void add(String path, Transformer domEdit) {
            domEditsByPath.computeIfAbsent(path, k -> new LinkedHashSet<>()).add(domEdit);
        }

        /**
         * Perform the operations added via {@link #set(String, DomEdit)}.
         *
         * @param rootDirectory
         * @param encoding
         */
        public void perform(Path rootDirectory, Charset encoding, SimpleElementWhitespace simpleElementWhitespace) {
            while (!domEditsByPath.isEmpty()) {
                LinkedHashMap<String, Set<Transformer>> cp = new LinkedHashMap<>(domEditsByPath);
                domEditsByPath.clear();
                for (Entry<String, Set<Transformer>> e : cp.entrySet()) {
                    final Path pomXml = rootDirectory.resolve(e.getKey());
                    final Set<Transformer> tfs = e.getValue();
                    PomTransformer.builder()
                            .charset(encoding)
                            .simpleElementWhitespace(simpleElementWhitespace)
                            .transformers(tfs)
                            .transform(pomXml);
                }
            }
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
            } else if (PROJECT_VERSION_XPATH == valueDefinition.getXPath()) {
                /* ignore */
            } else {
                final Transformer edit = context -> {
                    final Text text = valueDefinition.getXPath().apply(context.getDocument());
                    text.content(newValue);
                };
                final String pomPath = valueDefinition.getModule().getPomPath();
                edits.add(
                        pomPath,
                        edit);
            }
        }

    }

    class SourceTreeExpressionEvaluator implements ExpressionEvaluator {

        private final Predicate<Profile> isProfileActive;
        private final Map<Expression, String> cache = new HashMap<>();

        public SourceTreeExpressionEvaluator(Predicate<Profile> isProfileActive) {
            this.isProfileActive = isProfileActive;
        }

        @Override
        public String evaluate(Expression expression) {
            if (expression.isConstant()) {
                return expression.getRawExpression();
            } else {
                return cache.computeIfAbsent(expression, k -> {
                    final StringBuffer result = new StringBuffer();
                    final Consumer<ValueDefinition> consumer = new Consumer<ValueDefinition>() {
                        @Override
                        public void accept(ValueDefinition propertyDefinition) {
                            final Expression propertyValue = propertyDefinition.getValue();
                            if (propertyValue.isConstant()) {
                                result.append(evaluate(propertyValue));
                            } else {
                                evaluateExpression(propertyValue, this);
                            }
                        }
                    };
                    evaluateExpression(k, consumer);
                    return result.toString();
                });
            }
        }

        void evaluateExpression(final Expression expression, final Consumer<ValueDefinition> consumer) {
            final Module context = MavenSourceTree.this.getModulesByGa().get(expression.getGa());
            final String src = expression.getRawExpression();
            final Matcher m = PLACE_HOLDER_PATTERN.matcher(src);
            int offset = 0;
            while (m.find()) {
                if (m.start() > offset) {
                    consumer.accept(new ValueDefinition(context, null,
                            new Expression(src.substring(offset, m.start()), expression.getGa())));
                }
                final String propName = m.group(1);
                evaluateProperty(context, propName, consumer);
                offset = m.end();
            }
            if (offset < src.length()) {
                consumer.accept(new ValueDefinition(context, null,
                        new Expression(src.substring(offset, src.length()), expression.getGa())));
            }
        }

        void evaluateProperty(final Module context,
                final String propertyName,
                final Consumer<ValueDefinition> consumer) {

            if ("project.version".equals(propertyName)) {
                consumer.accept(new ValueDefinition(context, PROJECT_VERSION_XPATH, context.getGav().getVersion()));
            } else if ("project.groupId".equals(propertyName)) {
                consumer.accept(
                        new ValueDefinition(context, PROJECT_GROUP_ID_XPATH, context.getGav().getGroupId()));
            } else if ("project.artifactId".equals(propertyName)) {
                consumer.accept(
                        new ValueDefinition(context, PROJECT_ARTIFACT_ID_XPATH, context.getGav().getArtifactId()));
            } else {
                final ValueDefinition propertyDefinition = context.findPropertyDefinition(propertyName,
                        isProfileActive);
                if (propertyDefinition == null) {
                    /* No such property: climb up */
                    final Module parent = MavenSourceTree.this.getDeclaredParentModule(context);
                    if (parent == null) {
                        /* unable to resolve */
                        throw new NoSuchPropertyException(propertyName);
                    } else {
                        evaluateProperty(parent, propertyName, consumer);
                    }
                } else {
                    consumer.accept(propertyDefinition);
                }

            }
        }

    }

    static final Logger log = LoggerFactory.getLogger(MavenSourceTree.class);

    /**
     * @param  rootPomXml the path to the {@code pom.xml} file of the root Maven module
     * @param  encoding   the encoding to use when reading {@code pom.xml} files in the given file tree
     * @return            a new {@link MavenSourceTree}
     */
    public static MavenSourceTree of(Path rootPomXml, Charset encoding) {
        return new Builder(rootPomXml.getParent(), encoding).pomXml(rootPomXml).build();
    }

    /**
     * @param  rootPomXml         the path to the {@code pom.xml} file of the root Maven module
     * @param  encoding           the encoding to use when reading {@code pom.xml} files in the given file tree
     * @param  dependencyExcludes a {@link Predicate} deciding whether a given {@link Dependency} should be ignored when
     *                            building the resulting {@link MavenSourceTree}
     * @return                    a new {@link MavenSourceTree}
     */
    public static MavenSourceTree of(Path rootPomXml, Charset encoding, Predicate<Dependency> dependencyExcludes) {
        return new Builder(rootPomXml.getParent(), encoding).dependencyExcludes(dependencyExcludes)
                .pomXml(rootPomXml, new ModuleCallback()).build();
    }

    /**
     * @param  rootPomXml         the path to the {@code pom.xml} file of the root Maven module
     * @param  encoding           the encoding to use when reading {@code pom.xml} files in the given file tree
     * @param  dependencyExcludes a {@link Predicate} deciding whether a given {@link Dependency} should be ignored when
     *                            building the resulting {@link MavenSourceTree}
     * @param  profiles           a predicate deciding whether some profile is active for the purposes of building the
     *                            resulting {@link MavenSourceTree}
     * @return                    a new {@link MavenSourceTree}
     * @return
     */
    public static MavenSourceTree of(Path rootPomXml, Charset encoding, Predicate<Dependency> dependencyExcludes,
            Predicate<Profile> profiles) {
        return new Builder(rootPomXml.getParent(), encoding, profiles).dependencyExcludes(dependencyExcludes)
                .pomXml(rootPomXml).build();
    }

    public static Function<Document, Optional<Element>> xPathProfile(String id, String... elements) {
        return document -> DomTripUtils.findProfile(document, id)
                .flatMap(profile -> profile.path(elements));
    }

    private final Charset encoding;

    private final Map<Ga, Module> modulesByGa;

    private final Map<String, Module> modulesByPath;

    final Path rootDirectory;

    private final Predicate<Dependency> dependencyExcludes;

    private final Map<Predicate<Profile>, ExpressionEvaluator> evaluators = new HashMap<>();

    MavenSourceTree(Path rootDirectory, Charset encoding, Map<String, Module> modulesByPath,
            Map<Ga, Module> modulesByGa, Predicate<Dependency> dependencyExcludes) {
        this.rootDirectory = rootDirectory;
        this.modulesByPath = modulesByPath;
        this.modulesByGa = modulesByGa;
        this.encoding = encoding;
        this.dependencyExcludes = dependencyExcludes;
    }

    private void addDeclaredParents(final Module module, final Set<Ga> result, Set<Ga> visited,
            Predicate<Profile> isProfileActive, ExpressionEvaluator evaluator) {
        Module parent;
        Module child = module;
        while ((parent = getDeclaredParentModule(child)) != null) {
            addModule(evaluator.evaluateGa(parent.getGav()), result, visited, isProfileActive, evaluator);
            child = parent;
        }
    }

    private void addModule(Ga includeGa, Set<Ga> result, Set<Ga> visited, Predicate<Profile> isProfileActive,
            ExpressionEvaluator evaluator) {
        final Module module = modulesByGa.get(includeGa);
        if (module != null && !visited.contains(includeGa)) {
            visited.add(includeGa);
            result.add(includeGa);
            addProperParents(module, result, visited, isProfileActive, evaluator);
            addDeclaredParents(module, result, visited, isProfileActive, evaluator);
            for (Profile p : module.getProfiles()) {
                if (isProfileActive.test(p)) {
                    for (GavExpression dep : p.getDependencies()) {
                        addModule(evaluator.evaluateGa(dep), result, visited, isProfileActive, evaluator);
                    }
                    for (GavExpression dep : p.getPlugins()) {
                        addModule(evaluator.evaluateGa(dep), result, visited, isProfileActive, evaluator);
                    }
                    for (Dependency dep : p.getDependencyManagement()) {
                        if ("import".equals(dep.getScope())) {
                            addModule(evaluator.evaluateGa(dep), result, visited, isProfileActive, evaluator);
                        }
                    }
                }
            }
        }
    }

    private void addProperParents(final Module module, final Set<Ga> result, Set<Ga> visited,
            Predicate<Profile> isProfileActive, ExpressionEvaluator evaluator) {
        Module parent;
        Module child = module;
        while ((parent = getProperParentModule(child, isProfileActive, evaluator)) != null) {
            addModule(evaluator.evaluateGa(parent.getGav()), result, visited, isProfileActive, evaluator);
            child = parent;
        }
    }

    /**
     * Returns a {@link Set} that contains all given {@code initialModules} and all such modules from the current
     * {@link MavenSourceTree} that are required to build all {@code initialModules}. The set is defined as a union of
     * transitive closures of {@code initialModules} on relationships <i>depends on</i>, <i>is parent
     * of</i> and <i>imports BOM</i>.
     *
     * @param  initialModules
     * @return                {@link Set} of {@code groupId:artifactId}
     */
    public Set<Ga> findRequiredModules(Collection<Ga> initialModules, Predicate<Profile> isProfileActive) {
        final Set<Ga> visited = new HashSet<>();
        final Set<Ga> result = new LinkedHashSet<>();
        final ExpressionEvaluator evaluator = getExpressionEvaluator(isProfileActive);
        for (Ga includeGa : initialModules) {
            addModule(includeGa, result, visited, isProfileActive, evaluator);
        }
        return result;
    }

    /**
     * Returns the complement of the given {@code inputSet}, the universe being the set of all modules in this
     * {@link MavenSourceTree}.
     *
     * @param  inputSet the set whose complement is to be computed
     * @return          a new {@link Set} with stable ordering
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

    /**
     * Goes through dependencies and plugins this source tree requires and returns the set of those satisfying
     * {@link GavSet#contains(String, String)}.
     *
     * @param  gavSet
     * @param  isProfileActive
     * @return                 a
     */
    public Set<Ga> filterDependencies(GavSet gavSet, final Predicate<Profile> isProfileActive) {
        final Set<Ga> result = new TreeSet<>();
        final ExpressionEvaluator evaluator = getExpressionEvaluator(isProfileActive);
        for (Module module : getModulesByGa().values()) {
            final GavExpression parentGav = module.getParentGav();
            if (parentGav != null) {
                final Ga ga = evaluator.evaluateGa(parentGav);
                if (gavSet.contains(ga.getGroupId(), ga.getArtifactId())) {
                    result.add(ga);
                }
            }
            for (Profile p : module.getProfiles()) {
                if (isProfileActive.test(p)) {
                    for (Dependency depGa : p.getDependencies()) {
                        final Ga ga = evaluator.evaluateGa(depGa);
                        if (gavSet.contains(ga.getGroupId(), ga.getArtifactId())) {
                            result.add(ga);
                        }
                    }
                    for (Dependency depGa : p.getDependencyManagement()) {
                        final Ga ga = evaluator.evaluateGa(depGa);
                        if ("import".equals(depGa.getScope()) && gavSet.contains(ga.getGroupId(), ga.getArtifactId())) {
                            result.add(ga);
                        }
                    }
                    for (Plugin plugin : p.getPlugins()) {
                        {
                            final Ga ga = evaluator.evaluateGa(plugin);
                            if (gavSet.contains(ga.getGroupId(), ga.getArtifactId())) {
                                result.add(ga);
                            }
                        }
                        for (GavExpression plugnDep : plugin.getDependencies()) {
                            final Ga plugnDepGa = evaluator.evaluateGa(plugnDep);
                            if (gavSet.contains(plugnDepGa.getGroupId(), plugnDepGa.getArtifactId())) {
                                result.add(plugnDepGa);
                            }
                        }
                    }
                    for (Plugin plugin : p.getPluginManagement()) {
                        {
                            final Ga ga = evaluator.evaluateGa(plugin);
                            if (gavSet.contains(ga.getGroupId(), ga.getArtifactId())) {
                                result.add(ga);
                            }
                        }
                        for (GavExpression pluginDep : plugin.getDependencies()) {
                            final Ga plugnDepGa = evaluator.evaluateGa(pluginDep);
                            if (gavSet.contains(plugnDepGa.getGroupId(), plugnDepGa.getArtifactId())) {
                                result.add(plugnDepGa);
                            }
                        }
                    }
                    for (GavExpression ext : p.getExtensions()) {
                        final Ga ga = evaluator.evaluateGa(ext);
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
        final GavExpression parentGa = child.getParentGav();
        if (parentGa != null) {
            return modulesByGa.get(new ConstantOnlyExpressionEvaluator().evaluateGa(parentGa));
        } else {
            return null;
        }
    }

    /**
     * Find a {@link Module} for the given {@link Ga} in this tree and collects its dependencies declared through out
     * its
     * ancestor hierarchy. Consider caching the result as this is a potentially expensive operation.
     *
     * @param  module
     * @param  profiles
     * @return          a {@link Set}
     */
    public Set<Dependency> collectOwnDependencies(Ga module, Predicate<Profile> profiles) {
        ExpressionEvaluator evaluator = getExpressionEvaluator(profiles);
        Set<Dependency> result = new LinkedHashSet<>();
        Module m = modulesByGa.get(module);
        if (m == null) {
            throw new IllegalArgumentException(
                    "Can collect own dependencies only for modules available in this tree; " + module + " was not found here");
        }
        do {
            m.streamOwnDependencies(profiles).forEach(result::add);
            final GavExpression parentGav = m.getParentGav();
            if (parentGav == null) {
                break;
            }
            m = modulesByGa.get(evaluator.evaluateGa(parentGav));
        } while (m != null);
        return result;
    }

    /**
     * Find a {@link Module} for the given {@link Ga} in this tree and collects its dependencies declared through out
     * its
     * ancestor hierarchy and any transitive dependencies available in this tree. Consider caching the result as this is
     * a
     * potentially expensive operation.
     *
     * @param  module
     * @param  profiles
     * @return          a {@link Set}
     */
    public Set<Dependency> collectTransitiveDependencies(Ga module, Predicate<Profile> profiles) {
        Module m = modulesByGa.get(module);
        if (m == null) {
            throw new IllegalArgumentException(
                    "Can collect own dependencies only for modules available in this tree; " + module + " was not found here");
        }
        final Set<Dependency> result = new LinkedHashSet<>();
        final ExpressionEvaluator evaluator = getExpressionEvaluator(profiles);
        collectTransitiveDependencies(module, profiles, result, new HashSet<>(), evaluator);
        return result;
    }

    void collectTransitiveDependencies(Ga module, Predicate<Profile> profiles, Set<Dependency> result, Set<Ga> visited,
            ExpressionEvaluator evaluator) {
        if (visited.contains(module)) {
            return;
        }
        visited.add(module);
        Module m = modulesByGa.get(module);
        if (m == null) {
            return;
        }
        do {
            m.streamOwnDependencies(profiles)
                    .forEach(dep -> {
                        result.add(dep);
                        collectTransitiveDependencies(evaluator.evaluateGa(dep), profiles, result, visited, evaluator);
                    });
            final GavExpression parentGav = m.getParentGav();
            if (parentGav == null) {
                break;
            }
            m = modulesByGa.get(evaluator.evaluateGa(parentGav));
        } while (m != null);

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
     * @param  pomXmlPath an absolute path to a {@code pom.xml} file in this tree or a path relative to
     *                    {@link #getRootDirectory()}
     * @return            a {@link Module} or {@code null} if no {@link Module} is associated with the give path
     */
    public Module getModuleByPath(Path pomXmlPath) {
        if (pomXmlPath.isAbsolute()) {
            pomXmlPath = rootDirectory.relativize(pomXmlPath);
        }
        return modulesByPath.get(PomTunerUtils.toUnixPath(pomXmlPath.toString()));
    }

    /**
     * @param  child
     * @return       the {@link Module} having the given gild in its {@code <modules>}
     */
    Module getProperParentModule(Module child, Predicate<Profile> isProfileActive, ExpressionEvaluator evaluator) {
        final GavExpression parentGa = child.getParentGav();
        if (parentGa != null) {
            final Module declaredParent = modulesByGa.get(evaluator.evaluateGa(parentGa));
            if (declaredParent != null && declaredParent.hasChild(child.getPomPath(), isProfileActive)) {
                return declaredParent;
            }
            return modulesByGa.values().stream().filter(m -> m.hasChild(child.getPomPath(), isProfileActive)).findFirst()
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
     * @param newVersion              the new version to set
     * @param isProfileActive         a {@link Profile} filter, see {@link #from(String...)}
     * @param simpleElementWhitespace see {@link SimpleElementWhitespace}
     */
    public void setVersions(final String newVersion, final Predicate<Profile> isProfileActive,
            SimpleElementWhitespace simpleElementWhitespace) {
        final DomEdits edits = new DomEdits();
        final ExpressionEvaluator evaluator = getExpressionEvaluator(isProfileActive);
        for (Module module : modulesByGa.values()) {

            /* self */
            final GavExpression parentGav = module.getParentGav();
            final Expression moduleVersion = module.getGav().getVersion();
            final String pomPath = module.getPomPath();
            if (parentGav == null || !moduleVersion.equals(module.getParentGav().getVersion())) {
                /* explicitly defined version */
                edits.add(pomPath,
                        context -> context.getProject().getChildContainerElement("version").get().setTextContent(newVersion));
            }

            /* parent */
            if (parentGav != null && modulesByGa.containsKey(evaluator.evaluateGa(parentGav))) {
                final Expression parentVersion = parentGav.getVersion();
                edits.add(pomPath, parent.setVersion(newVersion));
            }

            final Set<String> profileIds = new HashSet<>();
            for (Profile profile : module.getProfiles()) {
                if (isProfileActive.test(profile)) {
                    profileIds.add(profile.getId());
                }
            }

            Ga moduleGa = evaluator.evaluateGa(module.getGav());
            edits.add(
                    module.getPomPath(),
                    dependencyManagement
                            .select(gavtcsElement -> isOwnVersionedDepenency(moduleGa, gavtcsElement, evaluator))
                            .from(profileIds::contains)
                            .modify(gavtcsElement -> setVersion(moduleGa, pomPath, gavtcsElement, newVersion, evaluator,
                                    edits)));
            edits.add(
                    module.getPomPath(),
                    dependencies
                            .select(gavtcsElement -> isOwnVersionedDepenency(moduleGa, gavtcsElement, evaluator))
                            .from(profileIds::contains)
                            .modify(gavtcsElement -> setVersion(moduleGa, pomPath, gavtcsElement, newVersion, evaluator,
                                    edits)));

            edits.add(
                    module.getPomPath(),
                    plugins
                            .select(gavtcsElement -> isOwnVersionedDepenency(moduleGa, gavtcsElement, evaluator))
                            .from(profileIds::contains)
                            .modify(gavtcsElement -> setVersion(moduleGa, pomPath, gavtcsElement, newVersion, evaluator,
                                    edits)));
            edits.add(
                    module.getPomPath(),
                    plugins
                            .selectPluginDependencies(
                                    gavtcsElement -> isOwnVersionedDepenency(moduleGa, gavtcsElement, evaluator))
                            .from(profileIds::contains)
                            .modify(gavtcsElement -> setVersion(moduleGa, pomPath, gavtcsElement, newVersion, evaluator,
                                    edits)));

            edits.add(
                    module.getPomPath(),
                    pluginManagement
                            .select(gavtcsElement -> isOwnVersionedDepenency(moduleGa, gavtcsElement, evaluator))
                            .from(profileIds::contains)
                            .modify(gavtcsElement -> setVersion(moduleGa, pomPath, gavtcsElement, newVersion, evaluator,
                                    edits)));
            edits.add(
                    module.getPomPath(),
                    pluginManagement
                            .selectPluginDependencies(
                                    gavtcsElement -> isOwnVersionedDepenency(moduleGa, gavtcsElement, evaluator))
                            .from(profileIds::contains)
                            .modify(gavtcsElement -> setVersion(moduleGa, pomPath, gavtcsElement, newVersion, evaluator,
                                    edits)));

            edits.add(
                    module.getPomPath(),
                    extensions
                            .select(gavtcsElement -> isOwnVersionedDepenency(moduleGa, gavtcsElement, evaluator))
                            .from(profileIds::contains)
                            .modify(gavtcsElement -> setVersion(moduleGa, pomPath, gavtcsElement, newVersion, evaluator,
                                    edits)));
        }
        edits.perform(rootDirectory, encoding, simpleElementWhitespace);
    }

    void setVersion(Ga module, String modulePath, GavtcsElement gavtcsElement, String newVersion, ExpressionEvaluator evaluator,
            DomEdits edits) {
        GavExpression gavExpresion = toGavExpression(gavtcsElement.getGavtcs(), module);
        final Expression version = gavExpresion.getVersion();
        if (!version.isConstant()) {
            ((SourceTreeExpressionEvaluator) evaluator).evaluateExpression(version,
                    new SimplePlaceHolderConsumer(edits, newVersion));
        } else {
            gavtcsElement.setVersion(newVersion);
        }
    }

    boolean isOwnVersionedDepenency(Ga module, Gavtcs gavtcsElement, ExpressionEvaluator evaluator) {
        if (gavtcsElement.getVersion() == null) {
            return false;
        }
        final GavExpression expr = toGavExpression(gavtcsElement, module);
        final Ga ga = evaluator.evaluateGa(expr);
        final boolean result = modulesByGa.containsKey(ga);
        return result;
    }

    private GavExpression toGavExpression(Gavtcs gavtcs, Ga module) {
        return new GavExpression(Expression.of(gavtcs.getGroupId(), module), Expression.of(gavtcs.getArtifactId(), module),
                Expression.of(gavtcs.getVersion(), module));
    }

    Map<String, Set<Path>> unlinkModules(Set<Ga> includes, Module module,
            Map<String, Set<Path>> removeChildPaths, Predicate<Profile> isProfileActive,
            ExpressionEvaluator evaluator) {
        for (Profile p : module.getProfiles()) {
            if (isProfileActive.test(p)) {
                for (String childPath : p.getChildren()) {
                    final Module childModule = modulesByPath.get(childPath);
                    final GavExpression childGa = childModule.getGav();
                    if (!includes.contains(evaluator.evaluateGa(childGa))) {
                        Set<Path> set = removeChildPaths.get(module.getPomPath());
                        if (set == null) {
                            set = new LinkedHashSet<Path>();
                            removeChildPaths.put(module.getPomPath(), set);
                        }
                        set.add(rootDirectory.resolve(childPath).normalize());
                    } else {
                        unlinkModules(includes, childModule, removeChildPaths, isProfileActive, evaluator);
                    }
                }
            }
        }
        return removeChildPaths;
    }

    /**
     * Delegates to {@link #unlinkModules(Set, Predicate, Charset, SimpleElementWhitespace, boolean)} with
     * {@code remove} set to {@code false}.
     *
     * @param requiredModules         a list of {@code groupId:artifactId}s
     * @param isProfileActive         a {@link Profile} filter, see {@link #from(String...)}
     * @param encoding                the encoding for reading and writing pom.xml files
     * @param simpleElementWhitespace the preference for writing start-end XML elements that have no attributes
     * @param commentText             for @{@code commentText} {@code "a comment"} the resulting snippet would look like
     *                                {@code <!-- <module>some-module</module> a comment --> }
     */
    public void unlinkModules(Set<Ga> requiredModules, Predicate<Profile> isProfileActive, Charset encoding,
            SimpleElementWhitespace simpleElementWhitespace, String commentText) {
        unlinkModules(requiredModules, isProfileActive, encoding, simpleElementWhitespace,
                (Set<String> modules) -> Transformation.commentModules(modules, commentText));
    }

    /**
     * Edit the {@code pom.xml} files so that just the given @{@code requiredModules} are buildable, removing all
     * unnecessary
     * {@code <module>} elements from {@code pom.xml} files.
     *
     * @param requiredModules         a list of {@code groupId:artifactId}s that are required to build
     * @param isProfileActive         a {@link Profile} filter, see {@link #from(String...)}
     * @param encoding                the encoding for reading and writing pom.xml files
     * @param simpleElementWhitespace the preference for writing start-end XML elements that have no attributes
     * @param remover                 a {@link Function} that takes a {@link Set} of module names (as in
     *                                {@code <module>my-module</module>} elements) and produces a {@link Transformation}
     *                                removing those elements.
     */
    public void unlinkModules(Set<Ga> requiredModules, Predicate<Profile> isProfileActive, Charset encoding,
            SimpleElementWhitespace simpleElementWhitespace, Function<Set<String>, PomTransformer.Transformation> remover) {
        final Module rootModule = modulesByPath.get("pom.xml");
        final ExpressionEvaluator evaluator = getExpressionEvaluator(isProfileActive);
        final Map<String, Set<Path>> removeChildPaths = unlinkModules(requiredModules, rootModule,
                new LinkedHashMap<String, Set<Path>>(), isProfileActive, evaluator);
        for (Entry<String, Set<Path>> e : removeChildPaths.entrySet()) {
            final Set<Path> paths = e.getValue();
            if (!paths.isEmpty()) {
                unlinkModules(rootDirectory.resolve(e.getKey()), paths, encoding, simpleElementWhitespace, remover);
            }
        }
    }

    static void unlinkModules(
            Path pomXml,
            Set<Path> removeChildPaths,
            Charset encoding,
            SimpleElementWhitespace simpleElementWhitespace,
            Function<Set<String>, PomTransformer.Transformation> remover) {

        final Path parentDir = pomXml.getParent();
        final Set<String> relPathsToRemove = removeChildPaths.stream()
                .map(Path::getParent) // path/to/pom.xml -> path/to
                .map(p -> parentDir.relativize(p))
                .map(Path::toString)
                .map(PomTunerUtils::toUnixPath)
                .collect(Collectors.toSet());

        PomTransformer.builder()
                .charset(encoding)
                .simpleElementWhitespace(simpleElementWhitespace)
                .transformers(remover.apply(relPathsToRemove))
                .transform(pomXml);
    }

    /**
     * Link back any modules anywhere in the source tree previously removed by
     * {@link #unlinkModules(Set, Predicate, Charset, SimpleElementWhitespace, Function)}.
     * This variant handles only module elements that are not under any profile - see
     * {@link #relinkModules(Charset, SimpleElementWhitespace, String, Predicate)} for a profile-aware alternative.
     *
     * @param  encoding                the encoding for reading and writing pom.xml files
     * @param  simpleElementWhitespace the preference for writing start-end XML elements that have no attributes
     * @param  commentText             has to be the same as used in the previous
     *                                 {@link #unlinkModules(Set, Predicate, Charset, SimpleElementWhitespace, Function)}
     *                                 invocation
     * @return                         either this {@link MavenSourceTree} if no relinking edits could be performed or a new
     *                                 {@link MavenSourceTree} with all modules relinked
     */
    public MavenSourceTree relinkModules(Charset encoding, SimpleElementWhitespace simpleElementWhitespace,
            String commentText) {
        return relinkModules(encoding, simpleElementWhitespace, commentText, ActiveProfiles.of());
    }

    /**
     * Link back any modules anywhere in the source tree previously removed by
     * {@link #unlinkModules(Set, Predicate, Charset, SimpleElementWhitespace, Function)}.
     *
     * @param  encoding                the encoding for reading and writing pom.xml files
     * @param  simpleElementWhitespace the preference for writing start-end XML elements that have no attributes
     * @param  commentText             has to be the same as used in the previous
     *                                 {@link #unlinkModules(Set, Predicate, Charset, SimpleElementWhitespace, Function)}
     *                                 invocation
     * @param  profiles                a predicate selecting profiles whose modules should be transformed; the default
     *                                 profile-less scope is always included
     * @return                         either this {@link MavenSourceTree} if no relinking edits could be performed or a new
     *                                 {@link MavenSourceTree} with all modules relinked
     *
     * @since                          4.6.0
     */
    public MavenSourceTree relinkModules(Charset encoding, SimpleElementWhitespace simpleElementWhitespace,
            String commentText, Predicate<Profile> profiles) {
        for (Entry<String, Module> en : modulesByPath.entrySet()) {
            final String relPath = en.getKey();
            final List<Transformation> transformations = new ArrayList<>();
            for (Profile p : en.getValue().getProfiles()) {
                transformations.add(Transformation.uncommentModules(commentText, m -> true, p.getId()));
            }
            final Path pomXml = rootDirectory.resolve(relPath);

            PomTransformer.builder()
            .charset(encoding)
            .simpleElementWhitespace(simpleElementWhitespace)
            .transformers(transformations)
            .transform(pomXml);

        }
        MavenSourceTree newTree = reload();
        if (modulesByPath.keySet().equals(newTree.modulesByPath.keySet())) {
            return this;
        } else {
            return newTree.relinkModules(encoding, simpleElementWhitespace, commentText);
        }
    }

    /**
     * Re-read the module hierarchy from the file system and return new {@link MavenSourceTree}.
     *
     * @return a new {@link MavenSourceTree};
     */
    public MavenSourceTree reload() {
        return new Builder(rootDirectory, encoding).dependencyExcludes(dependencyExcludes)
                .pomXml(rootDirectory.resolve("pom.xml")).build();
    }

    public ExpressionEvaluator getExpressionEvaluator(Predicate<Profile> profiles) {
        return evaluators.computeIfAbsent(profiles, SourceTreeExpressionEvaluator::new);
    }

}
