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

/**
 * An expression used in Maven {@code pom.xml} files, such as <code>${my-property}</code> or
 * <code>my-prefix-${my-property}</code>
 */
public class Expression {

    final String expression;
    private final Ga ga;
    private final boolean constant;

    public Expression(String expression, Ga ga) {
        super();
        this.expression = expression;
        this.constant = expression.indexOf("${") < 0;
        this.ga = ga;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((expression == null) ? 0 : expression.hashCode());
        result = prime * result + ((ga == null || constant) ? 0 : ga.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Expression other = (Expression) obj;
        if (expression == null) {
            if (other.expression != null)
                return false;
        } else if (!expression.equals(other.expression))
            return false;
        if (!constant) {
            if (ga == null) {
                if (other.ga != null)
                    return false;
            } else if (!ga.equals(other.ga))
                return false;
        }
        return true;
    }

    public String getExpression() {
        return expression;
    }

    /**
     * @return the raw source of this {@link Expression}
     */
    public String getRawExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return expression;
    }

    /**
     * @return the constant value of this {@link Expression} if this is a {@link Constant}; otherwise an
     *         {@link IllegalStateException} is thrown
     */
    public String asConstant() {
        if (constant) {
            return expression;
        }
        throw new IllegalStateException(expression + "is not a constant");
    }

    public static class NoSuchPropertyException extends RuntimeException {
        /**  */
        private static final long serialVersionUID = 8378620214313767928L;
        private final String propertyName;

        public NoSuchPropertyException(String propertyName) {
            super(String.format(
                    "Unable to resolve property [%s]: root of the module hierarchy reached",
                    propertyName));
            this.propertyName = propertyName;
        }

        public String getPropertyName() {
            return propertyName;
        }

    }

    /**
     * @param  expression the expression possibly containing <code>${...}</code> placeholders
     * @param  ga         the {@link Ga} against which the resulting {@link Expression} should be evaluated
     * @return            a {@link NonConstant} or {@link Constant} depending on whether the given {@code expression}
     *                    contains
     *                    <code>${</code>
     */
    public static Expression of(String expression, Ga ga) {
        return new Expression(expression, ga);
    }

    public boolean isConstant() {
        return constant;
    }

    /**
     * @return the {@link Ga} of the module where the resolution of this {@link Expression} should start. For expressions
     *         used in {@code <parent>} block, this would be the parent {@link Ga}. For all other cases, this is the
     *         {@link Gav} of the {@code pom.xml} where this {@link Expression} occurs.
     */
    public Ga getGa() {
        return ga;
    }
}
