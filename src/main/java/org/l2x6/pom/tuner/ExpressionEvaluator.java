package org.l2x6.pom.tuner;

import org.l2x6.pom.tuner.MavenSourceTree.GavExpression;
import org.l2x6.pom.tuner.model.Expression;
import org.l2x6.pom.tuner.model.Ga;
import org.l2x6.pom.tuner.model.Gav;

public interface ExpressionEvaluator {
    String evaluate(Expression expression);

    default Gav evaluateGav(GavExpression gav) {
        return new Gav(evaluate(gav.getGroupId()), evaluate(gav.getArtifactId()), evaluate(gav.getVersion()));
    }

    default Ga evaluateGa(GavExpression gav) {
        return new Ga(evaluate(gav.getGroupId()), evaluate(gav.getArtifactId()));
    }

    class ConstantOnlyExpressionEvaluator implements ExpressionEvaluator {

        @Override
        public String evaluate(Expression expression) {
            if (expression.isConstant()) {
                return expression.getRawExpression();
            }
            throw new IllegalArgumentException(ConstantOnlyExpressionEvaluator.class.getSimpleName()
                    + " cannot resolve a non-constant expression " + expression);
        }

    }
}
