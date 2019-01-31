/**
 * Copyright 2009 Humboldt-Universität zu Berlin, INRIA.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */
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
