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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.l2x6.pom.tuner.MavenSourceTree;
import org.l2x6.pom.tuner.model.Module.DependencyBuilder;
import org.l2x6.pom.tuner.model.Module.ModuleGavBuilder;
import org.l2x6.pom.tuner.model.Module.PlainGavBuilder;
import org.l2x6.pom.tuner.model.Module.PluginGavBuilder;

/**
 * A Maven profile.
 */
public class Profile {

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
        List<Profile.PropertyBuilder> properties = new ArrayList<>();
        final Predicate<Dependency> dependencyExcludes;

        Builder(Predicate<Dependency> dependencyExcludes) {
            this.dependencyExcludes = dependencyExcludes;
        }

        public Profile build() {
            final Set<String> useChildren = Collections.unmodifiableSet(children);
            children = null;
            final Set<Dependency> useDependencies = Collections
                    .<Dependency> unmodifiableSet((Set<Dependency>) dependencies.stream()
                            .map(DependencyBuilder::build)
                            .filter(dep -> !dependencyExcludes.test(dep))
                            .collect(Collectors.toCollection(LinkedHashSet::new)));
            dependencies = null;
            final Set<Dependency> useManagedDependencies = Collections
                    .<Dependency> unmodifiableSet(
                            (Set<Dependency>) dependencyManagement.stream().map(DependencyBuilder::build)
                                    .collect(Collectors.toCollection(LinkedHashSet::new)));
            dependencyManagement = null;
            final Set<Plugin> usePlugins = Collections.<Plugin> unmodifiableSet((Set<Plugin>) plugins.stream()
                    .map(PluginGavBuilder::build).collect(Collectors.toCollection(LinkedHashSet::new)));
            plugins = null;
            final Set<Plugin> usePluginManagement = Collections
                    .<Plugin> unmodifiableSet((Set<Plugin>) pluginManagement.stream().map(PluginGavBuilder::build)
                            .collect(Collectors.toCollection(LinkedHashSet::new)));
            pluginManagement = null;

            final Set<GavExpression> useExtensions = Collections
                    .<GavExpression> unmodifiableSet((Set<GavExpression>) extensions
                            .stream().map(PlainGavBuilder::build).collect(Collectors.toCollection(LinkedHashSet::new)));
            extensions = null;

            final Map<String, Expression> useProps = Collections.unmodifiableMap(properties.stream() //
                    .map(PropertyBuilder::build) //
                    .collect(( //
                    Collectors.toMap( //
                            e -> e.getKey(), //
                            e -> e.getValue(), //
                            (u, v) -> {
                                throw new IllegalStateException(
                                        String.format("Duplicate key %s in profile %s", u, id));
                            }, //

                            LinkedHashMap::new //
                    ) //
                    )) //
            );
            this.properties = null;
            return new Profile(id, useChildren, useDependencies, useManagedDependencies, usePlugins,
                    usePluginManagement, useExtensions, useProps);
        }

        public void id(String id) {
            this.id = id;
        }

        public Set<String> getChildren() {
            return children;
        }

        public List<DependencyBuilder> getDependencies() {
            return dependencies;
        }

        public List<Profile.PropertyBuilder> getProperties() {
            return properties;
        }
    }

    public static class PropertyBuilder {
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
    final Set<String> children;
    final Set<Dependency> dependencies;
    final Set<Dependency> dependencyManagement;
    private final Set<GavExpression> extensions;
    private final String id;
    private final Set<Plugin> pluginManagement;

    final Set<Plugin> plugins;
    final Map<String, Expression> properties;

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
