package org.l2x6.pom.tuner.model;

import java.util.Set;

public class Plugin extends GavExpression {
    private final Set<GavExpression> dependencies;

    public Plugin(Expression groupId, Expression artifactId, Expression version, Set<GavExpression> dependencies) {
        super(groupId, artifactId, version);
        this.dependencies = dependencies;
    }

    public Set<GavExpression> getDependencies() {
        return dependencies;
    }
}
