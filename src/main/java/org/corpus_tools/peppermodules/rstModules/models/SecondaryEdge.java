package org.corpus_tools.peppermodules.rstModules.models;

public class SecondaryEdge extends AbstractNode {
    private String id;
    private AbstractNode source;
    private AbstractNode target;
    private String relationName;

    public String getId() {
        return id;
    }
    public void setId(String s) {
        this.id = s;
    }

    public AbstractNode getSource() {
        return source;
    }
    public void setSource(AbstractNode n) {
        this.source = n;
    }

    public AbstractNode getTarget() {
        return target;
    }
    public void setTarget(AbstractNode n) {
        this.target = n;
    }

    public String getRelationName() {
        return relationName;
    }
    public void setRelationName(String s) {
        this.relationName = s;
    }
}
