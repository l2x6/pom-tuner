package org.l2x6.pom.tuner.model;

public class ValueDefinition {
    private final Module module;
    private final Expression value;
    private final String xPath;

    public ValueDefinition(Module module, String xPath, Expression value) {
        super();
        this.module = module;
        this.xPath = xPath;
        this.value = value;
    }

    /**
     * @return the {@link Module} in which {@link #value} is defined
     */
    public Module getModule() {
        return module;
    }

    /**
     * @return the value of the {@link Expression}
     */
    public Expression getValue() {
        return value;
    }

    /**
     * @return an XPath expression pointing at the element where the {@link #value} is defined
     */
    public String getXPath() {
        return xPath;
    }
}
