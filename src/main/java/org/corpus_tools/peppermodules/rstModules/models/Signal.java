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

