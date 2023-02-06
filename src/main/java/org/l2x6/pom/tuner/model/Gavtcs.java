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
package org.l2x6.pom.tuner.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A Maven dependency defined by {@code groupId}, {@code artifactId}, {@code version}, etc.
 */
public class Gavtcs {
    private static final Comparator<String> SCOPE_COMPARATOR = (a, b) -> scopeOrdinal(a) - scopeOrdinal(b);
    private static final Comparator<String> TYPE_COMPARATOR = (a, b) -> (a == null ? "jar" : a)
            .compareTo(b == null ? "jar" : b);
    private static final Comparator<String> SAFE_STRING_COMPARATOR = (a, b) -> a == b
            ? 0
            : (a != null ? a.compareTo(b) : -1);

    private static final Comparator<Gavtcs> GROUP_FIRST_COMPARATOR = Comparator
            .comparing(Gavtcs::getGroupId, SAFE_STRING_COMPARATOR)
            .thenComparing(Gavtcs::getArtifactId, SAFE_STRING_COMPARATOR)
            .thenComparing(Gavtcs::getVersion, SAFE_STRING_COMPARATOR)
            .thenComparing(Gavtcs::getType, TYPE_COMPARATOR)
            .thenComparing(Gavtcs::getClassifier, SAFE_STRING_COMPARATOR)
            .thenComparing(Gavtcs::getScope, SCOPE_COMPARATOR)
            .thenComparing(Gavtcs::getExclusions, new ListComparator<Ga>());

    private static final Comparator<Gavtcs> SCOPE_AND_TYPE_FIRST_COMPARATOR = Comparator
            .comparing(Gavtcs::getScope, SCOPE_COMPARATOR)
            .thenComparing(Gavtcs::getType, TYPE_COMPARATOR)
            .thenComparing(Gavtcs::getGroupId, SAFE_STRING_COMPARATOR)
            .thenComparing(Gavtcs::getArtifactId, SAFE_STRING_COMPARATOR)
            .thenComparing(Gavtcs::getVersion, SAFE_STRING_COMPARATOR)
            .thenComparing(Gavtcs::getClassifier, SAFE_STRING_COMPARATOR)
            .thenComparing(Gavtcs::getExclusions, new ListComparator<Ga>());
    private static final String DEFAULT_SCOPE = "compile";
    private static final String DEFAULT_TYPE = "jar";

    static class ListComparator<T extends Comparable<? super T>> implements Comparator<SortedSet<T>> {
        @Override
        public int compare(SortedSet<T> set1, SortedSet<T> set2) {
            if (set1 == set2) {
                return 0;
            }
            if (set1.isEmpty()) {
                return set2.isEmpty() ? 0 : -1;
            }
            if (set2.isEmpty()) {
                return 1;
            }
            final Iterator<T> i1 = set1.iterator();
            final Iterator<T> i2 = set2.iterator();
            while (i1.hasNext() && i2.hasNext()) {
                final T e1 = i1.next();
                final T e2 = i2.next();
                final int result = e1.compareTo(e2);
                if (result != 0) {
                    return result;
                }
            }
            return !i1.hasNext() ? (!i2.hasNext() ? 0 : -1) : 1;
        }
    }

    public static Gavtcs importBom(String groupId, String artifactId, String version) {
        return new Gavtcs(groupId, artifactId, version, "pom", null, "import");
    }

    private static int scopeOrdinal(String scope) {
        if (scope == null) {
            // compile
            return 1;
        }
        switch (scope) {
        case "import":
            return 0;
        case "compile":
            return 1;
        case "provided":
            return 2;
        case "runtime":
            return 3;
        case "system":
            return 4;
        case "test":
            return 5;
        default:
            throw new IllegalArgumentException("Unexpected maven dependency scope '" + scope + "'");
        }
    }

    public static Gavtcs virtual(String groupId, String artifactId, String version) {
        return new Gavtcs(groupId, artifactId, version, "pom", null, "test", Ga.excludeAll());
    }

    public static Gavtcs testJar(String groupId, String artifactId, String version) {
        return new Gavtcs(groupId, artifactId, version, null, null, "test");
    }

    public static Gavtcs of(String rawGavtcs) {
        String[] gavtcArr = rawGavtcs.split(":");
        int i = 0;
        final String groupId = gavtcArr[i++];
        final String artifactId = gavtcArr[i++];
        final String version = gavtcArr[i++];
        final String type = i < gavtcArr.length ? emptyToNull(gavtcArr[i++]) : null;
        final String classifier = i < gavtcArr.length ? emptyToNull(gavtcArr[i++]) : null;
        final String scope = i < gavtcArr.length ? emptyToNull(gavtcArr[i++]) : null;
        return new Gavtcs(groupId, artifactId, version, type, classifier, scope);
    }

    public static Comparator<Gavtcs> groupFirstComparator() {
        return GROUP_FIRST_COMPARATOR;
    }

    public static Comparator<Gavtcs> scopeAndTypeFirstComparator() {
        return SCOPE_AND_TYPE_FIRST_COMPARATOR;
    }

    public static Predicate<Gavtcs> equalGroupIdAndArtifactId(String groupId, String artifactId) {
        return gavtcs -> groupId.equals(gavtcs.getGroupId()) && artifactId.equals(gavtcs.getArtifactId());
    }

    private static String emptyToNull(String string) {
        return string != null && !string.isEmpty() ? string : null;
    }

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String type;
    private final String classifier;
    private final String scope;
    private final SortedSet<Ga> exclusions;

    public Gavtcs(String groupId, String artifactId, String version) {
        this(groupId, artifactId, version, null, null, null);
    }

    public Gavtcs(String groupId, String artifactId, String version, String type, String classifier, String scope) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type == null || type.isEmpty() ? null : type;
        this.classifier = classifier == null || classifier.isEmpty() ? null : classifier;
        this.scope = scope == null || scope.isEmpty() ? null : scope;
        this.exclusions = Collections.emptySortedSet();
    }

    public Gavtcs(String groupId, String artifactId, String version, String type, String classifier, String scope,
            Ga exclusion) {
        super();
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type == null || type.isEmpty() ? null : type;
        this.classifier = classifier == null || classifier.isEmpty() ? null : classifier;
        this.scope = scope == null || scope.isEmpty() ? null : scope;
        final TreeSet<Ga> set = new TreeSet<>();
        set.add(exclusion);
        this.exclusions = Collections.unmodifiableSortedSet(set);
    }

    public Gavtcs(String groupId, String artifactId, String version, String type, String classifier, String scope,
            Collection<Ga> exclusions) {
        super();
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type == null || type.isEmpty() ? null : type;
        this.classifier = classifier == null || classifier.isEmpty() ? null : classifier;
        this.scope = scope == null || scope.isEmpty() ? null : scope;
        this.exclusions = exclusions == null ? Collections.emptySortedSet()
                : Collections.unmodifiableSortedSet(new TreeSet<>(exclusions));
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getType() {
        return type;
    }

    public String getClassifier() {
        return classifier;
    }

    public String getScope() {
        return scope;
    }

    public SortedSet<Ga> getExclusions() {
        return exclusions;
    }

    public static Function<Gavtcs, Optional<Gavtcs>> deploymentVirtualMapper(Predicate<Gavtcs> isExtension) {
        return gavtcs -> isExtension.test(gavtcs)
                ? Optional.of(gavtcs.toVirtualDeployment())
                : Optional.empty();
    }

    public Gavtcs toVirtualDeployment() {
        return virtual(groupId, artifactId + "-deployment", version == null ? "${project.version}" : version);
    }

    public Gavtcs toVirtual() {
        return virtual(groupId, artifactId, version == null ? "${project.version}" : version);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder().append(groupId).append(':').append(artifactId).append(':')
                .append(version);
        if (type != null || classifier != null || scope != null) {
            sb.append(':');
            if (type != null) {
                sb.append(type);
            }
            if (classifier != null || scope != null) {
                sb.append(':');
                if (classifier != null) {
                    sb.append(classifier);
                }
                if (scope != null) {
                    sb.append(':').append(scope);
                }
            }
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        final String useScope = DEFAULT_SCOPE.equals(scope) ? null : scope;
        final String useType = DEFAULT_TYPE.equals(type) ? null : type;
        result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
        result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
        result = prime * result + ((exclusions == null) ? 0 : exclusions.hashCode());
        result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
        result = prime * result + ((useScope == null) ? 0 : useScope.hashCode());
        result = prime * result + ((useType == null) ? 0 : useType.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Gavtcs))
            return false;
        Gavtcs other = (Gavtcs) obj;
        if (artifactId == null) {
            if (other.artifactId != null)
                return false;
        } else if (!artifactId.equals(other.artifactId))
            return false;
        if (classifier == null) {
            if (other.classifier != null)
                return false;
        } else if (!classifier.equals(other.classifier))
            return false;
        if (exclusions == null) {
            if (other.exclusions != null)
                return false;
        } else if (!exclusions.equals(other.exclusions))
            return false;
        if (groupId == null) {
            if (other.groupId != null)
                return false;
        } else if (!groupId.equals(other.groupId))
            return false;
        final String useScope = DEFAULT_SCOPE.equals(scope) ? null : scope;
        final String useOtherScope = DEFAULT_SCOPE.equals(other.scope) ? null : other.scope;
        if (useScope == null) {
            if (useOtherScope != null)
                return false;
        } else if (!useScope.equals(useOtherScope))
            return false;
        final String useType = DEFAULT_TYPE.equals(type) ? null : type;
        final String useOtherType = DEFAULT_TYPE.equals(other.type) ? null : other.type;
        if (useType == null) {
            if (useOtherType != null)
                return false;
        } else if (!useType.equals(useOtherType))
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }

    public boolean isVirtual() {
        return "pom".equals(type) && "test".equals(scope);
    }

    public boolean isVirtualDeployment() {
        return "pom".equals(type) && "test".equals(scope) && artifactId.endsWith("-deployment");
    }

    /**
     * @return a new {@link Ga} created out of this {@link Gavtcs}'s {@link #groupId} and {@link #artifactId}
     */
    public Ga toGa() {
        return new Ga(groupId, artifactId);
    }

}
