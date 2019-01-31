package org.corpus_tools.peppermodules.rstModules.models;

public class Signal {
    private String type;
    private String subtype;
    private AbstractNode source;
    private String tokens;

    public String getType() {
        return type;
    }
    public void setType(String s) {
        type = s;
    }

    public String getSubtype() {
        return subtype;
    }
    public void setSubtype(String s) {
        subtype = s;
    }

    public AbstractNode getSource() {
        return source;
    }
    public void setSource(AbstractNode n) {
        source = n;
    }

    public String getTokens() {
        return tokens;
    }
    public void setTokens(String s) {
        tokens = s;
    }
}

