/**
 * Copyright 2009 Humboldt University of Berlin, INRIA.
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
package de.hu_berlin.german.korpling.saltnpepper.pepperModules.rstModules;

import java.io.IOException;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

import de.hu_berlin.german.korpling.rst.Group;
import de.hu_berlin.german.korpling.rst.RSTDocument;
import de.hu_berlin.german.korpling.rst.Relation;
import de.hu_berlin.german.korpling.rst.Segment;
import de.hu_berlin.german.korpling.rst.resources.RSTResourceFactory;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.MAPPING_RESULT;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.PepperMapper;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.impl.PepperMapperImpl;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.rstModules.exceptions.RSTImporterException;
import de.hu_berlin.german.korpling.saltnpepper.salt.SaltFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDominanceRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SStructure;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.tokenizer.Tokenizer;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SAnnotation;

/**
 * Maps a Rst-Document (RSTDocument) to a Salt document (SDocument).
 * <ul>
 * <li>
 * all relations, having a different type from span will be mapped to a pointing
 * relation (or a dominance relation with type=secedge, this is done for ANNIS)
 * <li/>
 * <li>
 * for all relations of type span an artificial relation will be created for all
 * transitive children of the relations source to the relations target</li>
 * <li>
 * the text including in a segment will be tokenized and SToken objects will be
 * created</li>
 * 
 * </ul>
 * 
 * @author Florian Zipser
 * 
 */
public class RST2SaltMapper extends PepperMapperImpl implements PepperMapper {
    /**
     * Initializes this object.
     */
    public RST2SaltMapper() {
	this.init();
    }

    /**
     * Initializes this object. Sets the tokenizer.
     */
    private void init() {
	this.tokenizer = new Tokenizer();
	this.rstId2SStructure = new Hashtable<String, SStructure>();
    }

    /** {@link ResourceSet} for loading EMF models **/
    private ResourceSet resourceSet = null;

    /** Sets {@link ResourceSet} for loading EMF models **/
    public void setResourceSet(ResourceSet resourceSet) {
	this.resourceSet = resourceSet;
    }

    /** Returns {@link ResourceSet} for loading EMF models **/
    public ResourceSet getResourceSet() {
	// Register XML resource factory
	if (resourceSet == null) {
	    resourceSet = new ResourceSetImpl();
	    resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
		    .put(RSTImporter.FILE_ENDING_RS3, new RSTResourceFactory());
	}
	return (resourceSet);
    }

    private RSTDocument currentRSTDocument = null;

    /**
     * @param currentSDocument
     *            the currentSDocument to set
     */
    public void setCurrentRSTDocument(RSTDocument currentRSTDocument) {
	this.currentRSTDocument = currentRSTDocument;
    }

    /**
     * @return the currentSDocument
     */
    public RSTDocument getCurrentRSTDocument() {
	return this.currentRSTDocument;
    }

    // ================================================ end: current SDocument

    /**
     * {@inheritDoc PepperMapper#setSDocument(SDocument)}
     * 
     * OVERRIDE THIS METHOD FOR CUSTOMIZED MAPPING.
     */
    @Override
    public MAPPING_RESULT mapSDocument() {

	// load resource
	Resource resource = this.getResourceSet().createResource(
		this.getResourceURI());

	if (resource == null)
	    throw new RSTImporterException("Cannot load the RST file: "
		    + this.getResourceURI() + ", becuase the resource is null.");
	try {
	    resource.load(null);
	} catch (IOException e) {
	    throw new RSTImporterException("Cannot load the RST file: "
		    + this.getResourceURI() + ".", e);
	}
	RSTDocument rstDocument = null;
	rstDocument = (RSTDocument) resource.getContents().get(0);
	
	this.mapSDocument(rstDocument);
	
	return (MAPPING_RESULT.FINISHED);
    }

    /**
     * Maps the given {@link RSTDocument} to th {@link SDocument} given at {@link #getSDocument()}.
     * @param rstDocument
     */
    public void mapSDocument(RSTDocument rstDocument) {
	
	if (this.getSDocument().getSDocumentGraph()== null)
	    this.getSDocument().setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
	this.setCurrentRSTDocument(rstDocument);

	// map segments to STextualDS, Tokens and SStructures
	if (this.getCurrentRSTDocument().getSegments().size() > 0) {
	    if (((RSTImporterProperties) this.getProperties()).isToTokenize())
		this.mapSegmentsWithTokenize(this.getCurrentRSTDocument()
			.getSegments());
	    else
		this.mapSegmentsWithoutTokenize(this.getCurrentRSTDocument()
			.getSegments());
	}
	// map segments to STextualDS, Tokens and SStructures
	// map group to SStructure
	for (Group group : this.getCurrentRSTDocument().getGroups())
	    this.mapGroup2SStructure(group);
	// map group to SStructure
	// maps all relations and creates artificial ones if neccessary
	for (Relation relation : this.getCurrentRSTDocument().getRelations()) {
	    this.mapRelation(relation);
	}
    }

    /**
     * stores the rstId of an AbstractNode and the corresponding SStructure
     * mapped to the AbstractNode
     */
    private Hashtable<String, SStructure> rstId2SStructure = null;

    /**
     * The TreeTaggerTokenizer to tokenize an untokenized primary text.
     */
    private Tokenizer tokenizer = null;

    /**
     * Returns the TreeTaggerTokenizer to tokenize an untokenized primary text.
     * 
     * @return
     */
    public Tokenizer getTokenizer() {
	return tokenizer;
    }

    /**
     * Name of node kind segment.
     */
    public static final String NODE_KIND_SEGMENT = "segment";
    /**
     * Name of node kind group.
     */
    public static final String NODE_KIND_GROUP = "group";

    /**
     * Maps the given {@link Segment} to the current {@link STextualDS} by
     * adding all textual values of segment after the preceding one. The created
     * {@link STextualDS} will be added to the {@link SDocumentGraph}. Also
     * {@link SToken} objects created by a tokenizer will be added and connected
     * to the {@link STextualDS} object. As last a {@link SStructure} object for
     * the {@link Segment} object will be created and related to the
     * {@link SToken} objects.
     * 
     * @param segments
     *            a list of {@link Segment} objects
     * @return
     */
    public void mapSegmentsWithTokenize(EList<Segment> segments) {
	STextualDS sText = null;
	if ((segments != null) && (segments.size() > 0)) {
	    sText = SaltFactory.eINSTANCE.createSTextualDS();
	    this.getSDocument().getSDocumentGraph().addSNode(sText);
	    // StringBuffer strBuffer= new StringBuffer();
	    for (Segment segment : segments) {// for all segments adding their
					      // text, creating tokens, and
					      // relations
		List<SToken> tokens = null;
		int start = 0;
		if (sText.getSText() != null) {
		    start = sText.getSText().length();
		    sText.setSText(sText.getSText()
			    + ((RSTImporterProperties) this.getProperties())
				    .getSegementSeparator() + segment.getText());
		} else
		    sText.setSText(segment.getText());
		int end = sText.getSText().length();

		Tokenizer tokenizer = this.getSDocument().getSDocumentGraph()
			.createTokenizer();
		tokens = tokenizer.tokenize(sText, null, start, end);

		if ((tokens != null) && (tokens.size() > 0)) {// if tokens exist
		    SStructure sStruct = SaltFactory.eINSTANCE
			    .createSStructure();
		    sStruct.setSName(segment.getId());
		    sStruct.createSAnnotation(null,
			    ((RSTImporterProperties) this.getProperties())
				    .getNodeKindName(), NODE_KIND_SEGMENT);
		    if (segment.getType() != null)
			sStruct.createSAnnotation(null,
				((RSTImporterProperties) this.getProperties())
					.getNodeTypeName(), segment.getType());

		    // puts segment.id and mapped SStructure-object into table
		    this.rstId2SStructure.put(segment.getId(), sStruct);
		    this.getSDocument().getSDocumentGraph().addSNode(sStruct);

		    for (SToken sToken : tokens) {// put each token in
						  // SDocumentGraph
			SDominanceRelation sDomRel = SaltFactory.eINSTANCE
				.createSDominanceRelation();
			sDomRel.setSSource(sStruct);
			sDomRel.setSTarget(sToken);
			this.getSDocument().getSDocumentGraph()
				.addSRelation(sDomRel);
		    }// put each token in SDocumentGraph
		}// if tokens exist
	    }// for all segments
	}
    }

    /**
     * Maps the given segment to the current STextualDS by adding all textual
     * values of segment behind the preceding. The created STextualDS will be
     * added to the SDocumentGraph. Also a SToken object will be created and
     * related to the STextualDS. As last a SSTructure object for the segment
     * will be created and related to the tokens.
     * 
     * @param segments
     * @return
     */
    private void mapSegmentsWithoutTokenize(EList<Segment> segments) {
	STextualDS sText = null;
	if ((segments != null) && (segments.size() > 0)) {
	    sText = SaltFactory.eINSTANCE.createSTextualDS();
	    this.getSDocument().getSDocumentGraph().addSNode(sText);
	    StringBuffer strBuffer = new StringBuffer();

	    int i = 0;
	    for (Segment segment : segments) {// for all segments adding their
					      // text, creating tokens, and
					      // relations
		i++;
		SStructure sStruct = SaltFactory.eINSTANCE.createSStructure();
		sStruct.createSAnnotation(null, ((RSTImporterProperties) this
			.getProperties()).getNodeKindName(), NODE_KIND_SEGMENT);
		if (segment.getType() != null)
		    sStruct.createSAnnotation(null,
			    ((RSTImporterProperties) this.getProperties())
				    .getNodeTypeName(), segment.getType());
		sStruct.setSName(segment.getId());
		// puts segment.id and mapped SStructure-object into table
		this.rstId2SStructure.put(segment.getId(), sStruct);
		this.getSDocument().getSDocumentGraph().addSNode(sStruct);

		SToken sToken = SaltFactory.eINSTANCE.createSToken();
		this.getSDocument().getSDocumentGraph().addSNode(sToken);

		STextualRelation sTextRel = SaltFactory.eINSTANCE
			.createSTextualRelation();
		sTextRel.setSTextualDS(sText);
		sTextRel.setSToken(sToken);
		sTextRel.setSStart(strBuffer.length());
		sTextRel.setSEnd(strBuffer.length()
			+ segment.getText().length());
		this.getSDocument().getSDocumentGraph().addSRelation(sTextRel);

		SDominanceRelation sDomRel = SaltFactory.eINSTANCE
			.createSDominanceRelation();
		sDomRel.setSSource(sStruct);
		sDomRel.setSTarget(sToken);
		this.getSDocument().getSDocumentGraph().addSRelation(sDomRel);

		if (i != 0)
		    strBuffer.append(((RSTImporterProperties) this
			    .getProperties()).getSegementSeparator());
		strBuffer.append(segment.getText());
	    }// for all segments
	    sText.setSText(strBuffer.toString());
	}
    }

    /**
     * Maps the given group to a SStructure object, The SStructure object will
     * be added to the graph
     * 
     * @param group
     * @return the created SStructure-object
     */
    private SStructure mapGroup2SStructure(Group group) {
	SStructure sStructure = null;
	if (group != null) {
	    sStructure = SaltFactory.eINSTANCE.createSStructure();
	    sStructure.setSName(group.getId());
	    if (group.getType() != null)
		sStructure.createSAnnotation(null,
			((RSTImporterProperties) this.getProperties())
				.getNodeTypeName(), group.getType());

	    // puts segment.id and mapped SSTructure-object into table
	    this.rstId2SStructure.put(group.getId(), sStructure);

	    {// create SAnnotation containing the group as value
		SAnnotation sAnno = SaltFactory.eINSTANCE.createSAnnotation();
		sAnno.setSName(((RSTImporterProperties) this.getProperties())
			.getNodeKindName());
		sAnno.setSValue(NODE_KIND_GROUP);
		sStructure.addSAnnotation(sAnno);
	    }// create SAnnotation containing the group as value
	    this.getSDocument().getSDocumentGraph().addSNode(sStructure);
	}
	return (sStructure);
    }

    /**
     * Mapps the given relation to one in the Salt model. Further artificial
     * ones will be created.
     * 
     * @param relation
     */
    private void mapRelation(Relation relation) {
	if (relation != null) {
	    if (relation.getParent() == null)
		throw new RSTImporterException(
			"Cannot map the rst-model of file'"
				+ this.getResourceURI()
				+ "', because the parent of a relation is empty.");
	    if (relation.getParent() == null)
		throw new RSTImporterException(
			"Cannot map the rst-model of file'"
				+ this.getResourceURI()
				+ "', because the source of a relation is empty.");

	    SStructure sSource = this.rstId2SStructure.get(relation.getParent()
		    .getId());
	    SStructure sTarget = this.rstId2SStructure.get(relation.getChild()
		    .getId());
	    if (sSource == null)
		throw new RSTImporterException(
			"Cannot map the rst-model of file'"
				+ this.getResourceURI()
				+ "', because the parent of a relation points to a non existing node with id '"
				+ relation.getChild().getId() + "'.");
	    if (sTarget == null)
		throw new RSTImporterException(
			"Cannot map the rst-model of file'"
				+ this.getResourceURI()
				+ "', because the parent of a relation belongs to a non existing node with id '"
				+ relation.getParent().getId() + "'.");

	    SDominanceRelation sDomRel = SaltFactory.eINSTANCE
		    .createSDominanceRelation();
	    if (relation.getType() != null)
		sDomRel.addSType(relation.getType());
	    sDomRel.setSSource(sSource);
	    sDomRel.setSTarget(sTarget);
	    this.getSDocument().getSDocumentGraph().addSRelation(sDomRel);

	    if (relation.getName() != null)
		sDomRel.createSAnnotation(null, ((RSTImporterProperties) this
			.getProperties()).getRelationName(), relation.getName());
	}
    }
}
