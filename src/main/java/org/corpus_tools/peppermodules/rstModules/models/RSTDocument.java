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

import org.corpus_tools.peppermodules.rstModules.reader.RSTReader;
import org.eclipse.emf.common.util.URI;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RSTDocument {
    private List<Segment> segments;
    private List<Group> groups;
    private List<Relation> relations;
    private List<Signal> signals;
    private List<SecondaryEdge> secondaryEdges;

    public List<Segment> getSegments() {
        return segments;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public List<Relation> getRelations() {
        return relations;
    }

    public List<Signal> getSignals() {
        return signals;
    }

    public List<SecondaryEdge> getSecondaryEdges() {
        return secondaryEdges;
    }

    public List<Relation> getIncomingRelations(String id) {
        List<Relation> retVal = null;
        if (this.getRelations() != null) {
            for (Relation relation : this.getRelations()) {
                if (relation.getChild().getId().equals(id)) {
                    if (retVal == null)
                        retVal = new ArrayList<Relation>();
                    retVal.add(relation);
                }
            }
        }
        return (retVal);
    }

    public Relation getOutgoingRelation(String id) {
        Relation retVal = null;
        if (this.getRelations() != null) {
            for (Relation relation : this.getRelations()) {
                if (relation.getParent().getId().equals(id)) {
                    retVal = relation;
                    break;
                }
            }
        }
        return (retVal);
    }

    public List<Relation> getOutgoingRelations(String id) {
        List<Relation> retVal = null;
        if (this.getRelations() != null) {
            for (Relation relation : this.getRelations()) {
                if (relation.getParent().getId().equals(id)) {
                    if (retVal == null)
                        retVal = new ArrayList<Relation>();
                    retVal.add(relation);
                }
            }
        }
        return (retVal);
    }

    public Relation createRelation(AbstractNode parent, AbstractNode child, String name, String type) {
        Relation rel = new Relation();
        rel.setParent(parent);
        rel.setChild(child);
        rel.setName(name);
        rel.setType(type);
        this.getRelations().add(rel);
        return (rel);
    }

    private void init() {
        segments = new ArrayList<Segment>();
        groups = new ArrayList<Group>();
        relations = new ArrayList<Relation>();
        signals = new ArrayList<Signal>();
        secondaryEdges = new ArrayList<SecondaryEdge>();
    }

    public RSTDocument() {
        init();
    }

    public RSTDocument(URI uri) {
        init();
        loadFile(uri);
    }

    private void loadFile(URI uri) {
        if (uri == null) {
            throw new RSTException("Cannot load any resource, because no uri is given.");
        }

        File rstFile = new File(uri.toFileString());
        if (!rstFile.exists()) {
            throw new RSTException("Cannot load resource, because the file does not exist: " + rstFile);
        }

        if (!rstFile.canRead()) {
            throw new RSTException("Cannot load resource, because the file can not be read: " + rstFile);
        }

        SAXParser parser;
        XMLReader xmlReader;
        RSTReader rstReader = new RSTReader();
        rstReader.setRstFile(rstFile);
        rstReader.setRSTDocument(this);

        SAXParserFactory factory = SAXParserFactory.newInstance();

        try {
            parser = factory.newSAXParser();
            xmlReader = parser.getXMLReader();
            // setting LexicalHandler to read DTD
            xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", rstReader);
            xmlReader.setContentHandler(rstReader);
        } catch (ParserConfigurationException e) {
            throw new RSTException("Cannot load RST from resource '" + rstFile.getAbsolutePath() + "'.", e);
        } catch (Exception e) {
            throw new RSTException("Cannot load RST from resource '" + rstFile.getAbsolutePath() + "'.", e);
        }
        try {
            InputStream inputStream = new FileInputStream(rstFile);
            Reader reader = new InputStreamReader(inputStream, "UTF-8");

            InputSource is = new InputSource(reader);
            is.setEncoding("UTF-8");

            xmlReader.parse(is);

        } catch (FileNotFoundException e) {
            throw new RSTException("File not found: " + rstFile.getAbsolutePath() + ".", e);
        } catch (UnsupportedEncodingException e) {
            throw new RSTException("File not in supported encoding: " + rstFile.getAbsolutePath() + ".", e);
        } catch(IOException e) {
            throw new RSTException("Error reading " + rstFile.getAbsolutePath() + ".", e);
        } catch (SAXException e) {

            try {
                parser = factory.newSAXParser();
                xmlReader = parser.getXMLReader();
                xmlReader.parse(rstFile.getAbsolutePath());
                // setting LexicalHandler to read DTD
                xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", rstReader);
                xmlReader.setContentHandler(rstReader);
            } catch (Exception e1) {
                throw new RSTException("Cannot load RST from resource '" + rstFile.getAbsolutePath() + "'.", e1);
            }
        }
    }
}

