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
package org.corpus_tools.peppermodules.rstModules.reader;

import java.io.File;
import java.util.*;

import org.corpus_tools.peppermodules.rstModules.models.*;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

public class RSTReader extends DefaultHandler2 {
    public RSTReader() {
        this.init();
    }

    private void init() {
        this.idAbstractNodeTable = new HashMap<String, AbstractNode>();
        this.idRelationTable = new HashMap<String, Vector<Relation>>();
        this.rstElementStack = new Stack<RSTReader.RSTElements>();
        this.relNameType = new HashMap<String, String>();
        this.signalTypes = new HashMap<String, Set<String>>();
    }

    // ========================= start: RSTFile
    private File rstFile = null;

    /**
     * Sets the file from which the RST Reader actually reads from.
     *
     * @param rstFile
     *            the rstFile to set
     */
    public void setRstFile(File rstFile) {
        this.rstFile = rstFile;
    }

    /**
     * Returns the file from which the RST Reader actually reads from.
     *
     * @return the rstFile
     */
    public File getRstFile() {
        return rstFile;
    }

    // ========================= end: RSTFile
    // ========================= start: RSTDocument
    private RSTDocument rstDocument = null;

    public void setRSTDocument(RSTDocument rstDocument) {
        this.rstDocument = rstDocument;
    }

    public RSTDocument getRSTDocument() {
        return rstDocument;
    }

    // ========================= end: RSTDocument
    /**
     * XML-element types for RST
     */
    public enum RSTElements {
        RST, HEADER, ENCODING, RELATIONS, REL, BODY, SEGMENT, GROUP,
        SIGNAL_TYPES, SIGNAL_TYPE, SIGNALS, SIGNAL, SECONDARY_EDGES, SECONDARY_EDGE
    };

    /**
     * Stores the last read RST-XML-Elements
     */
    private Stack<RSTElements> rstElementStack = null;

    /**
     * stores the read text inside an <segment> element
     */
    private StringBuffer currentText = null;

    /**
     * Stores correspondation between relation name and relation type, given in
     * the header
     */
    private HashMap<String, String> relNameType = null;

    /**
     * Contains pairs of <type, subtypes> as specified in the header
     */
    private HashMap<String, Set<String>> signalTypes = null;

    /**
     * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
     */
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (this.rstElementStack.peek().equals(RSTElements.SEGMENT)) {// current
            // element
            // is
            // <segment/>
            if (this.currentSegment == null)
                throw new RSTException("Cannot add the found text node in file '" + this.getRstFile().getAbsolutePath() + "', because it is not contained in a <segment>-element.");
            if (this.currentText == null)
                this.currentText = new StringBuffer();
            for (int i = start; i < start + length; i++) {// creating the text
                if (ch[i] != '\n')
                    currentText.append(ch[i]);
            } // creating the text
        } // current element is <segment/>
    }

    /**
     * Stores the AbstractNode object to its corresponding id.
     */
    private HashMap<String, AbstractNode> idAbstractNodeTable = null;

    /**
     * Stores the ids to the AbtractNodes, which were already linked by a
     * relation, but not seen
     */
    private HashMap<String, Vector<Relation>> idRelationTable = null;

    /**
     * puts an id and a relation to the table
     *
     * @param id
     * @param relation
     */
    private void addRelation2Table(String id, Relation relation) {
        Vector<Relation> slot = this.idRelationTable.get(id);
        if (slot == null) {
            slot = new Vector<Relation>();
            this.idRelationTable.put(id, slot);
        }
        slot.add(relation);
    }

    /**
     * Storws the last read Sgement-object.
     */
    private Segment currentSegment = null;

    /**
     *
     * @param uri
     * @param localName
     * @param qName
     * @param attributes
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equals(RSTVocabulary.TAG_RST)) {
            this.rstElementStack.push(RSTElements.RST);
        }

        else if (qName.equals(RSTVocabulary.TAG_HEADER)) {
            this.rstElementStack.push(RSTElements.HEADER);
        }

        else if (qName.equals(RSTVocabulary.TAG_ENCODING)) {
            this.rstElementStack.push(RSTElements.ENCODING);
        }

        else if (qName.equals(RSTVocabulary.TAG_RELATIONS)) {
            this.rstElementStack.push(RSTElements.RELATIONS);
        }

        else if (qName.equals(RSTVocabulary.TAG_SIGNAL_TYPES)) {
            this.rstElementStack.push(RSTElements.SIGNAL_TYPES);
        }

        else if (qName.equals(RSTVocabulary.TAG_SIGNAL_TYPE)) {
            this.rstElementStack.push(RSTElements.SIGNAL_TYPE);
            String type = attributes.getValue(RSTVocabulary.ATT_TYPE);
            String[] subtypes = attributes.getValue(RSTVocabulary.ATT_SUBTYPES).split(";");
            this.signalTypes.put(type, new HashSet<>(Arrays.asList(subtypes)));
        }

        else if (qName.equals(RSTVocabulary.TAG_REL)) {
            this.rstElementStack.push(RSTElements.REL);
            String relName = attributes.getValue(RSTVocabulary.ATT_NAME);
            String relType = attributes.getValue(RSTVocabulary.ATT_TYPE);
            relNameType.put(relName, relType);
        }

        else if (qName.equals(RSTVocabulary.TAG_BODY)) {
            this.rstElementStack.push(RSTElements.BODY);
        }

        else if (qName.equals(RSTVocabulary.TAG_SEGMENT)) {
            this.rstElementStack.push(RSTElements.SEGMENT);
            Segment segment = new Segment();
            segment.setId(attributes.getValue(RSTVocabulary.ATT_ID));
            this.getRSTDocument().getSegments().add(segment);

            if (attributes.getValue(RSTVocabulary.ATT_TYPE) != null)
                segment.setType(attributes.getValue(RSTVocabulary.ATT_TYPE));

            this.idAbstractNodeTable.put(attributes.getValue(RSTVocabulary.ATT_ID), segment);
            this.currentSegment = segment;
            {// check if there are relations waiting for this segment
                Vector<Relation> slot = this.idRelationTable.get(attributes.getValue(RSTVocabulary.ATT_ID));
                if (slot != null) {// there are relations waiting for this
                    // segment
                    for (Relation relation : slot) {
                        relation.setParent(segment);
                    }
                } // there are relations waiting for this segment
            } // check if there are relations waiting for this segment
            {// creating relation
                if (attributes.getValue(RSTVocabulary.ATT_PARENT) != null) {
                    Relation relation = new Relation();
                    relation.setChild(segment);
                    this.getRSTDocument().getRelations().add(relation);
                    if (attributes.getValue(RSTVocabulary.ATT_RELNAME) != null) {
                        String relname = attributes.getValue(RSTVocabulary.ATT_RELNAME);
                        relation.setName(relname);
                        if (this.relNameType.containsKey(relname)) {
                            relation.setType(relNameType.get(relname));
                        }
                    }
                    AbstractNode parent = this.idAbstractNodeTable.get(attributes.getValue(RSTVocabulary.ATT_PARENT));
                    if (parent == null) {// parent does not exist so far
                        this.addRelation2Table(attributes.getValue(RSTVocabulary.ATT_PARENT), relation);
                    } // parent does not exist so far
                    else {// parent already exists
                        relation.setParent(parent);
                    } // parent already exists
                }
            } // creating relation
        }

        else if (qName.equals(RSTVocabulary.TAG_GROUP)) {// element <group/>
            // found
            this.rstElementStack.push(RSTElements.GROUP);
            Group group = new Group();
            group.setId(attributes.getValue(RSTVocabulary.ATT_ID));
            this.getRSTDocument().getGroups().add(group);
            if (attributes.getValue(RSTVocabulary.ATT_TYPE) != null)
                group.setType(attributes.getValue(RSTVocabulary.ATT_TYPE));

            this.idAbstractNodeTable.put(attributes.getValue(RSTVocabulary.ATT_ID), group);
            {// check if there are relations waiting for this segment
                Vector<Relation> slot = this.idRelationTable.get(attributes.getValue(RSTVocabulary.ATT_ID));
                if (slot != null) {// there are relations waiting for this
                    // segment
                    for (Relation relation : slot) {
                        relation.setParent(group);
                    }
                } // there are relations waiting for this segment
            } // check if there are relations waiting for this segment
            {// creating relation
                if (attributes.getValue(RSTVocabulary.ATT_PARENT) != null) {
                    Relation relation = new Relation();
                    this.getRSTDocument().getRelations().add(relation);
                    relation.setChild(group);
                    if (attributes.getValue(RSTVocabulary.ATT_RELNAME) != null) {
                        String relname = attributes.getValue(RSTVocabulary.ATT_RELNAME);
                        relation.setName(relname);
                        if (this.relNameType.containsKey(relname)) {
                            relation.setType(relNameType.get(relname));
                        }
                    }

                    AbstractNode parent = this.idAbstractNodeTable.get(attributes.getValue(RSTVocabulary.ATT_PARENT));
                    if (parent == null) {// parent does not exist so far
                        this.addRelation2Table(attributes.getValue(RSTVocabulary.ATT_PARENT), relation);
                    } // parent does not exist so far
                    else {// parent already exists
                        relation.setParent(parent);
                    } // parent already exists
                }
            } // creating relation
        }

        else if (qName.equals(RSTVocabulary.TAG_SIGNALS)) {
            this.rstElementStack.push(RSTElements.SIGNALS);
        }

        else if (qName.equals(RSTVocabulary.TAG_SIGNAL)) {
            this.rstElementStack.push(RSTElements.SIGNAL);
            Signal signal = new Signal();
            signal.setType(attributes.getValue(RSTVocabulary.ATT_TYPE));
            signal.setSubtype(attributes.getValue(RSTVocabulary.ATT_SUBTYPE));

            // tokens are integers separated by commas
            List<Integer> tokenIds = new ArrayList<Integer>();

            String ids = attributes.getValue(RSTVocabulary.ATT_TOKENS);
            if (ids.length() > 0) {
                for (String tokenId: ids.split(",")) {
                    tokenIds.add(Integer.parseInt(tokenId));
                }
                signal.setTokenIds(tokenIds);
            }

            AbstractNode sourceNode = this.idAbstractNodeTable.get(attributes.getValue(RSTVocabulary.ATT_SOURCE));
            signal.setSource(sourceNode);
            this.getRSTDocument().getSignals().add(signal);
        }

        else if (qName.equals(RSTVocabulary.TAG_SECONDARY_EDGES)) {
            this.rstElementStack.push(RSTElements.SECONDARY_EDGES);
        }

        else if (qName.equals(RSTVocabulary.TAG_SECONDARY_EDGE)) {
            this.rstElementStack.push(RSTElements.SECONDARY_EDGE);
            SecondaryEdge e = new SecondaryEdge();
            e.setId(attributes.getValue(RSTVocabulary.ATT_ID));
            e.setRelationName(attributes.getValue(RSTVocabulary.ATT_RELNAME));

            AbstractNode source = this.idAbstractNodeTable.get(attributes.getValue(RSTVocabulary.ATT_SOURCE));
            if (source == null) {
                throw new RSTException("Secondary edge references a source node that doesn't exist!");
            } else {
                e.setSource(source);
            }
            AbstractNode target = this.idAbstractNodeTable.get(attributes.getValue(RSTVocabulary.ATT_TARGET));
            if (target == null) {
                throw new RSTException("Secondary edge references a target node that doesn't exist!");
            } else {
                e.setTarget(target);
            }
            this.idAbstractNodeTable.put(e.getId(), e);
            this.getRSTDocument().getSecondaryEdges().add(e);
        }
    }

    /**
     *
     * @param namespaceURI
     * @param localName
     * @param qName
     * @throws SAXException
     */
    @Override
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        this.rstElementStack.pop();
        if (qName.equals(RSTVocabulary.TAG_BODY)) {// element <body/> found

        } // element <body/> found
        else if (qName.equals(RSTVocabulary.TAG_SEGMENT)) {// element <segment/>
            // found
            if (this.currentText != null) {
                this.currentSegment.setText(this.currentText.toString());
            }
            this.currentText = null;
            this.currentSegment = null;
        } // element <segment/> found
        else if (qName.equals(RSTVocabulary.TAG_GROUP)) {// element <group/>
            // found

        } // element <group/> found
    }
}

abstract class RSTVocabulary {
    // tags and attributes for files of type TEXT(text.dtd)
    public static final String TAG_RST = "rst";
    public static final String TAG_HEADER = "header";
    public static final String TAG_ENCODING = "encoding";
    public static final String TAG_RELATIONS = "relations";
    public static final String TAG_REL = "rel";
    public static final String TAG_BODY = "body";
    public static final String TAG_SEGMENT = "segment";
    public static final String TAG_GROUP = "group";
    public static final String TAG_SIGNAL_TYPES = "sigtypes";
    public static final String TAG_SIGNAL_TYPE = "sig";
    public static final String TAG_SIGNALS = "signals";
    public static final String TAG_SIGNAL = "signal";
    public static final String TAG_SECONDARY_EDGES = "secedges";
    public static final String TAG_SECONDARY_EDGE = "secedge";

    public static final String ATT_NAME = "name";
    public static final String ATT_PARENT = "parent";
    public static final String ATT_TYPE = "type";
    public static final String ATT_SUBTYPE = "subtype";
    public static final String ATT_SUBTYPES = "subtypes";
    public static final String ATT_ID = "id";
    public static final String ATT_RELNAME = "relname";
    public static final String ATT_SOURCE = "source";
    public static final String ATT_TARGET = "target";
    public static final String ATT_TOKENS = "tokens";
}
