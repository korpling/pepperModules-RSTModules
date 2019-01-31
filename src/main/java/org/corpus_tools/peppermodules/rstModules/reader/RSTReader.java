package org.corpus_tools.peppermodules.rstModules.reader;

import java.io.File;
import java.util.HashMap;
import java.util.Stack;
import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

import org.corpus_tools.peppermodules.rstModules.models.RSTException;
import org.corpus_tools.peppermodules.rstModules.models.AbstractNode;
import org.corpus_tools.peppermodules.rstModules.models.Group;
import org.corpus_tools.peppermodules.rstModules.models.RSTDocument;
import org.corpus_tools.peppermodules.rstModules.models.Relation;
import org.corpus_tools.peppermodules.rstModules.models.Segment;

public class RSTReader extends DefaultHandler2 {
    public RSTReader() {
        this.init();
    }

    private void init() {
        this.idAbstractNodeTable = new HashMap<String, AbstractNode>();
        this.idRelationTable = new HashMap<String, Vector<Relation>>();
        this.rstElementStack = new Stack<RSTReader.RSTElements>();
        this.relNameType = new HashMap<String, String>();
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
        RST, HEADER, ENCODING, RELATIONS, REL, BODY, SEGMENT, GROUP
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

    public static final String ATT_NAME = "name";
    public static final String ATT_PARENT = "parent";
    public static final String ATT_TYPE = "type";
    public static final String ATT_ID = "id";
    public static final String ATT_RELNAME = "relname";
}