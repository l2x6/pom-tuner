package org.l2x6.pom.tuner.model;

public class Dependency extends GavExpression {
    private final String scope;
    private final String type;

    public Dependency(Expression groupId, Expression artifactId, Expression version, String type, String scope) {
        super(groupId, artifactId, version);
        this.type = type;
        this.scope = scope;
    }

    public String getScope() {
        return scope;
    }

    public String getType() {
        return type;
    }

    public boolean isVirtual() {
        return "pom".equals(type) && "test".equals(scope);
    }
}
