package org.corpus_tools.peppermodules.rstModules.models;

public class Relation {
    private AbstractNode parent;
    public AbstractNode getParent() {
        return parent;
    }
    public void setParent(AbstractNode n) {
        parent = n;
    }

    private AbstractNode child;
    public AbstractNode getChild() {
        return child;
    }
    public void setChild(AbstractNode n) {
        child = n;
    }

    private String type;
    public String getType() {
        return type;
    }
    public void setType(String s) {
        type = s;
    }

    private String name;
    public String getName() {
        return name;
    }
    public void setName(String s) {
        name = s;
    }
}
