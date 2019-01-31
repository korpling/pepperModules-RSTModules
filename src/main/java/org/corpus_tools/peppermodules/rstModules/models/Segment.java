package org.corpus_tools.peppermodules.rstModules.models;

public class Segment extends AbstractNode {
    private String text;
    public String getText() {
        return text;
    }
    public void setText(String s) {
        text = s;
    }
}
