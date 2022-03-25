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

public class Dependency extends GavExpression {
    private final String scope;
    private final String type;
    private final Expression classifier;
    static final String JAR = "jar";
    static final String COMPILE = "compile";

    public Dependency(Expression groupId, Expression artifactId, Expression version, String type, Expression classifier,
            String scope) {
        super(groupId, artifactId, version);
        this.classifier = classifier;
        this.type = type == null || type.isEmpty() ? JAR : type;
        this.scope = scope == null || scope.isEmpty() ? COMPILE : scope;
    }

    public String getType() {
        return type;
    }

    public Expression getClassifier() {
        return classifier;
    }

    public String getScope() {
        return scope;
    }

    public boolean isVirtual() {
        return "pom".equals(type) && "test".equals(scope);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
        result = prime * result + ((scope == null) ? 0 : scope.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        Dependency other = (Dependency) obj;
        if (classifier == null) {
            if (other.classifier != null)
                return false;
        } else if (!classifier.equals(other.classifier))
            return false;
        if (scope == null) {
            if (other.scope != null)
                return false;
        } else if (!scope.equals(other.scope))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return getGroupId() + ":" + getArtifactId() + ":" + type + ":" + classifier + ":" + getVersion() + ":" + scope;
    }

}
