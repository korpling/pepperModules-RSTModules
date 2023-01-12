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

import java.util.Hashtable;
import java.util.List;

import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.peppermodules.rstModules.models.*;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.*;
import org.corpus_tools.salt.common.tokenizer.SimpleTokenizer;
import org.corpus_tools.salt.common.tokenizer.Tokenizer;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SLayer;

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
 * @author Florian Zipser, Luke Gessler
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

	private RSTDocument currentRSTDocument = null;

	public void setCurrentRSTDocument(RSTDocument currentRSTDocument) {
		this.currentRSTDocument = currentRSTDocument;
	}

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
		RSTDocument rstDocument;
		rstDocument = new RSTDocument(this.getResourceURI());

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
		this.mapSecondaryEdges();
		this.mapSignals();
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
                int seenTokens = 0;
                Character[] seps = null;
                if (((RSTImporterProperties)getProperties()).getSimpleTokenizationSeparators()!= null){
                    seps = ((RSTImporterProperties)getProperties()).getSimpleTokenizationSeparators().toArray(new Character[((RSTImporterProperties)getProperties()).getSimpleTokenizationSeparators().size()]);
                }
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
                                        seenTokens = getDocument().getDocumentGraph().getTokens().size() - 1; // get 0 based index of last seen token; -1 if this is the very first token
					tokenizer.setDocumentGraph(getDocument().getDocumentGraph());
					tokenizer.tokenize(sText, start, end, seps);  // note that the SimpleTokenizer does not return tokens, but alters the document graph with new tokens
                                        // collect the new tokens as a sublist from the document's current graph
                                        tokens = getDocument().getDocumentGraph().getTokens().subList(seenTokens+1, getDocument().getDocumentGraph().getTokens().size());

				}else{
					Tokenizer tokenizer = this.getDocument().getDocumentGraph().createTokenizer();
					tokens = tokenizer.tokenize(sText, null, start, end); // the normal Tokenizer actually returns the tokens
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
			if (relation.getChild() == null)
				throw new PepperModuleException(this, "Cannot map the rst-model of file'" + this.getResourceURI() + "', because the child of a relation is empty.");

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

	private void mapSignals() {
		List<Signal> signals = this.getCurrentRSTDocument().getSignals();
		if (signals != null && signals.size() > 0) {
			SLayer signalsLayer = SaltFactory.createSLayer();
			String layerName = ((RSTImporterProperties) this.getProperties()).getSignalsLayerName();
			signalsLayer.setName(layerName);

			for (Signal signal : this.getCurrentRSTDocument().getSignals()) {
				this.mapSignal(signal, signalsLayer);
			}

			this.getDocument().getDocumentGraph().addLayer(signalsLayer);
		}
	}

	private void mapSignal(Signal signal, SLayer layer) {
		if (signal == null) {
			return;
		}

		if (signal.getSource() == null) {
			throw new PepperModuleException(this, "Cannot map the rst-model of file'" + this.getResourceURI()
					+ "', because the source of a signal is empty.");
		}

		SStructure sSource = this.rstId2SStructure.get(signal.getSource().getId());
		if (sSource == null) {
			throw new PepperModuleException(this, "Cannot map the rst-model of file'" + this.getResourceURI()
					+ "', because the parent of a signal points to a non existing node with id '"
					+ signal.getSource().getId() + "'.");
		}

		List<Integer> tokenIds = signal.getTokenIds();
		List<SToken> tokens = this.getDocument().getDocumentGraph().getTokens();

		// create node representing the signal
		SStructure signalNode = SaltFactory.createSStructure();
		signalNode.createAnnotation(null, "signal_type", signal.getType());
		signalNode.createAnnotation(null, "signal_subtype", signal.getSubtype());

		// add annotations to the signal node: signal_text for space-separated tokens, signal_indexes for their indexes
		if (tokenIds != null) {
			StringBuilder tokenTextSb = new StringBuilder();
			StringBuilder tokenIndexesSb = new StringBuilder();
			for (int i = 0; i < tokenIds.size(); i++) {
				SToken token = tokens.get(tokenIds.get(i) - 1);
				String tokenText = getDocument().getDocumentGraph().getText(token);

				tokenTextSb.append(tokenText);
				tokenIndexesSb.append(tokenIds.get(i).toString());
				if (i < tokenIds.size() - 1) {
					tokenTextSb.append(" ");
					tokenIndexesSb.append(" ");
				}
			}
			signalNode.createAnnotation(null, "signal_text", tokenTextSb.toString());
			signalNode.createAnnotation(null, "signal_indexes", tokenIndexesSb.toString());
		}

		layer.addNode(signalNode);
		this.getDocument().getDocumentGraph().addNode(signalNode);

		// make the signals node dominate the RST node that it is associated with. When RST is represented graphically,
		// this is the node that the arrow points out of, but in Salt, it is the node that is the child of a dominance
		// relation
		SDominanceRelation signal2rstNode = SaltFactory.createSDominanceRelation();
		String associatedSignalNodeId = signal.getSource().getId();
		signal2rstNode.setSource(signalNode);
		signal2rstNode.setTarget(sSource);

		List<Relation> incomingRelations = this.getCurrentRSTDocument().getIncomingRelations(associatedSignalNodeId);
		if (incomingRelations != null && incomingRelations.size() > 0) {
			Relation incomingRelation = incomingRelations.get(0);
			// annotate the edge connecting signal and rst node
			signal2rstNode.createAnnotation(null, "signal", incomingRelation.getName());
			// also annotate the signal node itself
			signalNode.createAnnotation(null, "signaled_relation", incomingRelation.getName());
		}

		layer.addRelation(signal2rstNode);
		this.getDocument().getDocumentGraph().addRelation(signal2rstNode);

		// also make the RST node dominate every token
		if (tokenIds != null) {
			List<SToken> sTokens = this.getDocument().getDocumentGraph().getTokens();
			for (int tokenId : signal.getTokenIds()) {
				SDominanceRelation tokRel = SaltFactory.createSDominanceRelation();
				tokRel.setSource(signalNode);
				// tokens are 1-indexed, list is 0-indexed
				tokRel.setTarget(sTokens.get(tokenId - 1));
				tokRel.setType("signal_token");
				tokRel.setSource(signalNode);
				layer.addRelation(tokRel);
				this.getDocument().getDocumentGraph().addRelation(tokRel);
			}
		}
	}

	private void mapSecondaryEdges() {
		List<SecondaryEdge> secondaryEdges = this.getCurrentRSTDocument().getSecondaryEdges();
		if (secondaryEdges != null && secondaryEdges.size() > 0) {
			SLayer secondaryEdgesLayer = SaltFactory.createSLayer();
			String layerName = ((RSTImporterProperties) this.getProperties()).getSecondaryEdgesLayerName();
			secondaryEdgesLayer.setName(layerName);
			for (SecondaryEdge e : this.getCurrentRSTDocument().getSecondaryEdges()) {
				this.mapSecondaryEdge(e, secondaryEdgesLayer);
			}
			this.getDocument().getDocumentGraph().addLayer(secondaryEdgesLayer);
		}
	}

	private void mapSecondaryEdge(SecondaryEdge e, SLayer layer) {
		if (e == null) {
			return;
		}

		if (e.getSource() == null) {
			throw new PepperModuleException(this, "Cannot map the rst-model of file '" + this.getResourceURI()
					+ "', because the source of a secondary edge is empty.");
		}
		if (e.getTarget() == null) {
			throw new PepperModuleException(this, "Cannot map the rst-model of file '" + this.getResourceURI()
					+ "', because the target of a secondary edge is empty.");
		}

		SStructure sSource = this.rstId2SStructure.get(e.getSource().getId());
		SStructure sTarget = this.rstId2SStructure.get(e.getTarget().getId());
		if (sSource == null) {
			throw new PepperModuleException(this, "Cannot map the rst-model of file'" + this.getResourceURI()
					+ "', because the source of a secondary edge points to a non existing node with id '"
					+ e.getSource().getId() + "'.");
		}
		if (sTarget == null) {
			throw new PepperModuleException(this, "Cannot map the rst-model of file'" + this.getResourceURI()
					+ "', because the target of a secondary edgepoints to a non existing node with id '"
					+ e.getTarget().getId() + "'.");
		}

		// Despite the name, a "secondary edge" is actually like an EDU or a CDU (complex discourse unit).
		// We therefore represent it with an SStructure with two pointing relations between it and its
		// source and target nodes.
		SStructure seNode = SaltFactory.createSStructure();
		seNode.setName(e.getId());

		SPointingRelation inbound = SaltFactory.createSPointingRelation();
		inbound.setType(e.getRelationName() + "-in");
		inbound.setName(e.getId() + "|secondary|in");
		inbound.setSource(sSource);
		inbound.setTarget(seNode);

		SPointingRelation outbound = SaltFactory.createSPointingRelation();
		outbound.setType(e.getRelationName() + "-out");
		outbound.setName(e.getId() + "|secondary|out");
		outbound.setSource(seNode);
		outbound.setTarget(sTarget);

		layer.addNode(seNode);
		layer.addRelation(outbound);
		layer.addRelation(inbound);
		this.getDocument().getDocumentGraph().addNode(seNode);
		this.getDocument().getDocumentGraph().addRelation(outbound);
		this.getDocument().getDocumentGraph().addRelation(inbound);
		this.rstId2SStructure.put(e.getId(), seNode);
	}
}
