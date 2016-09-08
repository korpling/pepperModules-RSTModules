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
package org.corpus_tools.peppermodules.rstModules;

import java.io.IOException;
import java.util.Hashtable;
import java.util.List;

import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SDominanceRelation;
import org.corpus_tools.salt.common.SStructure;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STextualRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.common.tokenizer.SimpleTokenizer;
import org.corpus_tools.salt.common.tokenizer.Tokenizer;
import org.corpus_tools.salt.core.SAnnotation;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

import de.hu_berlin.german.korpling.rst.Group;
import de.hu_berlin.german.korpling.rst.RSTDocument;
import de.hu_berlin.german.korpling.rst.Relation;
import de.hu_berlin.german.korpling.rst.Segment;
import de.hu_berlin.german.korpling.rst.resources.RSTResourceFactory;

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
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(RSTImporter.FILE_ENDING_RS3, new RSTResourceFactory());
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
	 * {@inheritDoc PepperMapper#setDocument(SDocument)}
	 * 
	 * OVERRIDE THIS METHOD FOR CUSTOMIZED MAPPING.
	 */
	@Override
	public DOCUMENT_STATUS mapSDocument() {
		// load resource
		Resource resource = this.getResourceSet().createResource(this.getResourceURI());

		if (resource == null)
			throw new PepperModuleException(this, "Cannot load the RST file: " + this.getResourceURI() + ", becuase the resource is null.");
		try {
			resource.load(null);
		} catch (IOException e) {
			throw new PepperModuleException(this, "Cannot load the RST file: " + this.getResourceURI() + ".", e);
		}
		RSTDocument rstDocument = null;
		rstDocument = (RSTDocument) resource.getContents().get(0);

		this.mapSDocument(rstDocument);

		return (DOCUMENT_STATUS.COMPLETED);
	}

	/**
	 * Maps the given {@link RSTDocument} to th {@link SDocument} given at
	 * {@link #getDocument()}.
	 * 
	 * @param rstDocument
	 */
	public void mapSDocument(RSTDocument rstDocument) {

		if (this.getDocument().getDocumentGraph() == null)
			this.getDocument().setDocumentGraph(SaltFactory.createSDocumentGraph());
		this.setCurrentRSTDocument(rstDocument);

		// map segments to STextualDS, Tokens and SStructures
		if (this.getCurrentRSTDocument().getSegments().size() > 0) {
			if (((RSTImporterProperties) this.getProperties()).isToTokenize())
				this.mapSegmentsWithTokenize(this.getCurrentRSTDocument().getSegments());
			else
				this.mapSegmentsWithoutTokenize(this.getCurrentRSTDocument().getSegments());
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
	public void mapSegmentsWithTokenize(List<Segment> segments) {
		STextualDS sText = null;
		if ((segments != null) && (segments.size() > 0)) {
			sText = SaltFactory.createSTextualDS();
			this.getDocument().getDocumentGraph().addNode(sText);
			// StringBuffer strBuffer= new StringBuffer();
			for (Segment segment : segments) {// for all segments adding their
				// text, creating tokens, and
				// relations
				List<SToken> tokens = null;
				int start = 0;
				if (sText.getText() != null) {
					start = sText.getText().length();
					sText.setText(sText.getText() + ((RSTImporterProperties) this.getProperties()).getSegmentSeparator() + segment.getText());
				} else
					sText.setText(segment.getText());
				int end = sText.getText().length();
				
				if (((RSTImporterProperties)getProperties()).getSimpleTokenizationSeparators()!= null){
					SimpleTokenizer tokenizer= new SimpleTokenizer();
					tokenizer.setDocumentGraph(getDocument().getDocumentGraph());
					Character[] seps= ((RSTImporterProperties)getProperties()).getSimpleTokenizationSeparators().toArray(new Character[((RSTImporterProperties)getProperties()).getSimpleTokenizationSeparators().size()]);
					tokenizer.tokenize(sText, start, end, seps);
                                        tokens = getDocument().getDocumentGraph().getTokens();
				}else{
					Tokenizer tokenizer = this.getDocument().getDocumentGraph().createTokenizer();
					tokens = tokenizer.tokenize(sText, null, start, end);
				}
				if ((tokens != null) && (tokens.size() > 0)) {// if tokens exist
					SStructure sStruct = SaltFactory.createSStructure();
					sStruct.setName(segment.getId());
					sStruct.createAnnotation(null, ((RSTImporterProperties) this.getProperties()).getNodeKindName(), NODE_KIND_SEGMENT);
					if (segment.getType() != null)
						sStruct.createAnnotation(null, ((RSTImporterProperties) this.getProperties()).getNodeTypeName(), segment.getType());

					// puts segment.id and mapped SStructure-object into table
					this.rstId2SStructure.put(segment.getId(), sStruct);
					this.getDocument().getDocumentGraph().addNode(sStruct);

					for (SToken sToken : tokens) {// put each token in
						// SDocumentGraph
						SDominanceRelation sDomRel = SaltFactory.createSDominanceRelation();
						sDomRel.setSource(sStruct);
						sDomRel.setTarget(sToken);
						this.getDocument().getDocumentGraph().addRelation(sDomRel);
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
	private void mapSegmentsWithoutTokenize(List<Segment> segments) {
		STextualDS sText = null;
		if ((segments != null) && (segments.size() > 0)) {
			sText = SaltFactory.createSTextualDS();
			this.getDocument().getDocumentGraph().addNode(sText);
			StringBuffer strBuffer = new StringBuffer();

			int i = 0;
			for (Segment segment : segments) {// for all segments adding their
				// text, creating tokens, and
				// relations
				i++;
				SStructure sStruct = SaltFactory.createSStructure();
				sStruct.createAnnotation(null, ((RSTImporterProperties) this.getProperties()).getNodeKindName(), NODE_KIND_SEGMENT);
				if (segment.getType() != null)
					sStruct.createAnnotation(null, ((RSTImporterProperties) this.getProperties()).getNodeTypeName(), segment.getType());
				sStruct.setName(segment.getId());
				// puts segment.id and mapped SStructure-object into table
				this.rstId2SStructure.put(segment.getId(), sStruct);
				this.getDocument().getDocumentGraph().addNode(sStruct);

				SToken sToken = SaltFactory.createSToken();
				this.getDocument().getDocumentGraph().addNode(sToken);

				STextualRelation sTextRel = SaltFactory.createSTextualRelation();
				sTextRel.setTarget(sText);
				sTextRel.setSource(sToken);
				sTextRel.setStart(strBuffer.length());
				sTextRel.setEnd(strBuffer.length() + segment.getText().length());
				this.getDocument().getDocumentGraph().addRelation(sTextRel);

				SDominanceRelation sDomRel = SaltFactory.createSDominanceRelation();
				sDomRel.setSource(sStruct);
				sDomRel.setTarget(sToken);
				this.getDocument().getDocumentGraph().addRelation(sDomRel);

				if (i != 0)
					strBuffer.append(((RSTImporterProperties) this.getProperties()).getSegmentSeparator());
				strBuffer.append(segment.getText());
			}// for all segments
			sText.setText(strBuffer.toString());
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
			sStructure = SaltFactory.createSStructure();
			sStructure.setName(group.getId());
			if (group.getType() != null)
				sStructure.createAnnotation(null, ((RSTImporterProperties) this.getProperties()).getNodeTypeName(), group.getType());

			// puts segment.id and mapped SSTructure-object into table
			this.rstId2SStructure.put(group.getId(), sStructure);

			{// create SAnnotation containing the group as value
				SAnnotation sAnno = SaltFactory.createSAnnotation();
				sAnno.setName(((RSTImporterProperties) this.getProperties()).getNodeKindName());
				sAnno.setValue(NODE_KIND_GROUP);
				sStructure.addAnnotation(sAnno);
			}// create SAnnotation containing the group as value
			this.getDocument().getDocumentGraph().addNode(sStructure);
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
				throw new PepperModuleException(this, "Cannot map the rst-model of file'" + this.getResourceURI() + "', because the parent of a relation is empty.");
			if (relation.getParent() == null)
				throw new PepperModuleException(this, "Cannot map the rst-model of file'" + this.getResourceURI() + "', because the source of a relation is empty.");

			SStructure sSource = this.rstId2SStructure.get(relation.getParent().getId());
			SStructure sTarget = this.rstId2SStructure.get(relation.getChild().getId());
			if (sSource == null)
				throw new PepperModuleException(this, "Cannot map the rst-model of file'" + this.getResourceURI() + "', because the parent of a relation points to a non existing node with id '" + relation.getChild().getId() + "'.");
			if (sTarget == null)
				throw new PepperModuleException(this, "Cannot map the rst-model of file'" + this.getResourceURI() + "', because the parent of a relation belongs to a non existing node with id '" + relation.getParent().getId() + "'.");

			SDominanceRelation sDomRel = SaltFactory.createSDominanceRelation();
			if (relation.getType() != null)
				sDomRel.setType(relation.getType());
			sDomRel.setSource(sSource);
			sDomRel.setTarget(sTarget);
			this.getDocument().getDocumentGraph().addRelation(sDomRel);

			if (relation.getName() != null)
				sDomRel.createAnnotation(null, ((RSTImporterProperties) this.getProperties()).getRelationName(), relation.getName());
		}
	}
}
