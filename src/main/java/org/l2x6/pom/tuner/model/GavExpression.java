package org.l2x6.pom.tuner.model;

import java.util.Objects;

/**
 * A {@link Ga} combined with a version {@link Expression}.
 */
public class GavExpression {

    private final Expression artifactId;
    private final Expression groupId;

    private final Expression version;

    public GavExpression(Expression groupId, Expression artifactId, Expression version) {
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

    @Override
    public String toString() {
        return groupId.getRawExpression() + ":" + artifactId.getRawExpression() + ":" + version;
    }

}
