package org.corpus_tools.peppermodules.rstModules.models;

public abstract class AbstractNode {
    private String type;
    public String getType() {
        return type;
    }
    public void setType(String s) {
        type = s;
    }

    private String id;
    public String getId() {
        return id;
    }
    public void setId(String s) {
        id = s;
    }
}
