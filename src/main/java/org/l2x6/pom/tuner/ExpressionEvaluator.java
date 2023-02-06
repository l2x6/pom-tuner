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

import org.l2x6.pom.tuner.model.Expression;
import org.l2x6.pom.tuner.model.Ga;
import org.l2x6.pom.tuner.model.Gav;
import org.l2x6.pom.tuner.model.GavExpression;

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
