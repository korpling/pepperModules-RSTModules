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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import org.corpus_tools.salt.core.SRelation;

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
		this.rstId2SStructure = new Hashtable<>();
		this.secondaryEdgeIndex = new Hashtable<>();
		this.primaryEdgeIndex = new Hashtable<>();
		this.primaryRelationsWithSignals = new HashSet<>();
		this.secondaryRelationsWithSignals = new HashSet<>();
		this.signalsForSecondaryEdge = new HashMap<>();
	}

	private RSTDocument currentRSTDocument = null;

	public void setCurrentRSTDocument(RSTDocument currentRSTDocument) {
		this.rstId2SStructure = new Hashtable<>();
		this.secondaryEdgeIndex = new Hashtable<>();
		this.primaryEdgeIndex = new Hashtable<>();
		this.primaryRelationsWithSignals = new HashSet<>();
		this.secondaryRelationsWithSignals = new HashSet<>();
		this.signalsForSecondaryEdge = new HashMap<>();
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
		this.rstId2SStructure = new Hashtable<>();
		this.secondaryEdgeIndex = new Hashtable<>();
		this.primaryEdgeIndex = new Hashtable<>();
		this.primaryRelationsWithSignals = new HashSet<>();
		this.secondaryRelationsWithSignals = new HashSet<>();
		this.signalsForSecondaryEdge = new HashMap<>();
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
		this.connectSecondaryEdgesToSignals();
		if (((RSTImporterProperties) this.getProperties()).getMarkIsSignaled()) {
			this.markEdgesWithSignals();
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
	 * Mapping from secondary edge ID to the SPointingRelation representing it
	 */
	private Hashtable<String, SStructure> secondaryEdgeIndex = null;

	/**
	 * Contains primary edges which have at least one signal associated with them
	 */
	private HashSet<SDominanceRelation> primaryRelationsWithSignals = null;

	/**
	 * Contains secondary edges which have at least one signal associated with them
	 */
	private HashSet<SStructure> secondaryRelationsWithSignals = null;

	/**
	 * Maps from secondary edge IDs to a list of associated signals
	 */
	private Map<SStructure, List<SStructure>> signalsForSecondaryEdge = null;

	/**
	 * Maps from IDs of discourse units to the relation which they are the child of
	 */
	private Hashtable<String, SDominanceRelation> primaryEdgeIndex = null;

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
		List<Character> simpleTokenizationSeparators = ((RSTImporterProperties) getProperties())
				.getSimpleTokenizationSeparators();
		int seenTokens = 0;
		Character[] seps = null;
		if (((RSTImporterProperties)getProperties()).getSimpleTokenizationSeparators()!= null){
			seps = ((RSTImporterProperties)getProperties()).getSimpleTokenizationSeparators()
					.toArray(new Character[simpleTokenizationSeparators.size()]);
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
					String segmentSeparator = ((RSTImporterProperties) this.getProperties()).getSegmentSeparator();
					sText.setText(sText.getText() + segmentSeparator + segment.getText());
				} else
					sText.setText(segment.getText());
				int end = sText.getText().length();

				if (((RSTImporterProperties)getProperties()).getSimpleTokenizationSeparators()!= null){
					SimpleTokenizer tokenizer= new SimpleTokenizer();
					// get 0 based index of last seen token; -1 if this is the very first token
					seenTokens = getDocument().getDocumentGraph().getTokens().size() - 1;
					tokenizer.setDocumentGraph(getDocument().getDocumentGraph());
					// note that the SimpleTokenizer does not return tokens, but alters the document graph with new tokens
					tokenizer.tokenize(sText, start, end, seps);
					// collect the new tokens as a sublist from the document's current graph
					tokens = getDocument().getDocumentGraph().getTokens()
							.subList(seenTokens+1, getDocument().getDocumentGraph().getTokens().size());
				} else {
					Tokenizer tokenizer = this.getDocument().getDocumentGraph().createTokenizer();
					// the normal Tokenizer actually returns the tokens
					tokens = tokenizer.tokenize(sText, null, start, end);
				}
				if ((tokens != null) && (tokens.size() > 0)) {// if tokens exist
					SStructure sStruct = SaltFactory.createSStructure();
					sStruct.setName(segment.getId());
					String nodeKindKey = ((RSTImporterProperties) this.getProperties()).getNodeKindName();
					String nodeTypeKey = ((RSTImporterProperties) this.getProperties()).getNodeTypeName();
					sStruct.createAnnotation(null, nodeKindKey, NODE_KIND_SEGMENT);
					if (segment.getType() != null)
						sStruct.createAnnotation(null, nodeTypeKey, segment.getType());

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
				String nodeKindKey = ((RSTImporterProperties) this.getProperties()).getNodeKindName();
				String nodeTypeKey = ((RSTImporterProperties) this.getProperties()).getNodeTypeName();
				sStruct.createAnnotation(null, nodeKindKey, NODE_KIND_SEGMENT);
				if (segment.getType() != null)
					sStruct.createAnnotation(null, nodeTypeKey, segment.getType());
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
			String nodeNameKey = ((RSTImporterProperties) this.getProperties()).getNodeTypeName();
			if (group.getType() != null)
				sStructure.createAnnotation(null, nodeNameKey, group.getType());

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
				throw new PepperModuleException(this, "Cannot map the rst-model of file'" + this.getResourceURI()
						+ "', because the parent of a relation is empty.");
			if (relation.getChild() == null)
				throw new PepperModuleException(this, "Cannot map the rst-model of file'" + this.getResourceURI()
						+ "', because the child of a relation is empty.");

			SStructure sSource = this.rstId2SStructure.get(relation.getParent().getId());
			SStructure sTarget = this.rstId2SStructure.get(relation.getChild().getId());
			if (sSource == null)
				throw new PepperModuleException(this, "Cannot map the rst-model of file'" + this.getResourceURI()
						+ "', because the parent of a relation points to a non existing node with id '"
						+ relation.getChild().getId() + "'.");
			if (sTarget == null)
				throw new PepperModuleException(this, "Cannot map the rst-model of file'" + this.getResourceURI()
						+ "', because the parent of a relation belongs to a non existing node with id '"
						+ relation.getParent().getId() + "'.");

			SDominanceRelation sDomRel = SaltFactory.createSDominanceRelation();
			if (relation.getType() != null)
				sDomRel.setType(relation.getType());
			sDomRel.setSource(sSource);
			sDomRel.setTarget(sTarget);
			this.getDocument().getDocumentGraph().addRelation(sDomRel);
			this.primaryEdgeIndex.put(relation.getChild().getId(), sDomRel);

			if (relation.getName() != null) {
				String relationNameKey = ((RSTImporterProperties) this.getProperties()).getRelationName();
				sDomRel.createAnnotation(null, relationNameKey, relation.getName());
			}
		}
	}

	private void mapSignals() {
		List<Signal> signals = this.getCurrentRSTDocument().getSignals();
		if (signals != null && signals.size() > 0) {
			for (Signal signal : this.getCurrentRSTDocument().getSignals()) {
				this.mapSignal(signal);
			}
		}
	}

	private void mapSignal(Signal signal) {
		// If the signal is null or its source attribute is null, quit
		if (signal == null) {
			return;
		}
		if (signal.getSource() == null) {
			throw new PepperModuleException(this, "Cannot map the rst-model of file'" + this.getResourceURI()
					+ "', because the source of a signal is empty.");
		}

		//////////////////////////////////////////////////////////////////////////////////
		// Signal node setup
		//////////////////////////////////////////////////////////////////////////////////
		// Determine whether the signal is associated with a normal or a secondary edge
		SStructure sSource = this.rstId2SStructure.get(signal.getSource().getId());
		SStructure secondaryEdge = this.secondaryEdgeIndex.get(signal.getSource().getId());

		// If neither is true, throw
		// Record that we have seen a signal for the given relation
		if (sSource != null) {
			SDominanceRelation r = this.primaryEdgeIndex.get(signal.getSource().getId());
			this.primaryRelationsWithSignals.add(r);
		} else if (secondaryEdge != null) {
			this.secondaryRelationsWithSignals.add(secondaryEdge);
		} else {
			throw new PepperModuleException(this, "Cannot map the rst-model of file'" + this.getResourceURI()
					+ "', because the parent of a signal points to a non existing node with id '"
					+ signal.getSource().getId() + "'.");
		}

		// create node representing the signal
		SStructure signalNode = SaltFactory.createSStructure();
		signalNode.createAnnotation(null, "signal_type", signal.getType());
		signalNode.createAnnotation(null, "signal_subtype", signal.getSubtype());
		this.getDocument().getDocumentGraph().addNode(signalNode);

		//////////////////////////////////////////////////////////////////////////////////
		// Token handling
		//////////////////////////////////////////////////////////////////////////////////
		// add annotations to the signal node: signal_text for space-separated tokens, signal_indexes for their indexes
		List<Integer> tokenIds = signal.getTokenIds();
		List<SToken> tokens = this.getDocument().getDocumentGraph().getTokens();
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

		// also make the signal node dominate every token
		int earliestToken = Integer.MAX_VALUE;
		if (tokenIds != null) {
			List<SToken> sTokens = this.getDocument().getDocumentGraph().getTokens();
			for (int tokenId : signal.getTokenIds()) {
				SDominanceRelation tokRel = SaltFactory.createSDominanceRelation();
				tokRel.setSource(signalNode);
				// tokens are 1-indexed, list is 0-indexed
				tokRel.setTarget(sTokens.get(tokenId - 1));
				tokRel.setType("signal_token");
				tokRel.setSource(signalNode);
				this.getDocument().getDocumentGraph().addRelation(tokRel);
				if (tokenId < earliestToken) {
					earliestToken = tokenId;
				}
			}
		}

		//////////////////////////////////////////////////////////////////////////////////
		// Signal to other node relation creation
		//////////////////////////////////////////////////////////////////////////////////
		if (secondaryEdge == null) {
			// make the signal node dominate the RST node that it is associated with. When
			// RST is represented graphically, this is the node that the arrow points out of,
			// but in Salt, it is the node that is the child of a dominance relation
			SDominanceRelation signal2rstNode = SaltFactory.createSDominanceRelation();
			String associatedSignalNodeId = signal.getSource().getId();
			signal2rstNode.setSource(signalNode);
			signal2rstNode.setTarget(sSource);

			// If the discourse unit associated with this signal has a parent (i.e., it is
			// the satellite of a nucleus), then we want to add some additional annotations.
			// Specifically, we want to add the name of the relation to the signal relation,
			// and we want to annotate teh signal node with the name of the relation.
			List<Relation> incomingRelations = this.getCurrentRSTDocument()
					.getIncomingRelations(associatedSignalNodeId);
			if (incomingRelations != null && incomingRelations.size() > 0) {
				Relation incomingRelation = incomingRelations.get(0);
				// annotate the edge connecting signal and rst node
				signal2rstNode.createAnnotation("prim", "signal", incomingRelation.getName());
				// also annotate the signal node itself
				signalNode.createAnnotation("prim", "signaled_relation", incomingRelation.getName());
			}
			this.getDocument().getDocumentGraph().addRelation(signal2rstNode);
		} else {
			// If we have a secondary edge associated with the signal, then our strategy is going to be
			// different: the signal node will have two dominance relations, one for each of the SE's ends
			SStructure seSource = null;
			SStructure seTarget = null;
			for (SRelation r : secondaryEdge.getOutRelations()) {
				if (r instanceof SDominanceRelation) {
					Object ann = r.getAnnotation(null, "end").getValue();
					if (ann != null && ann.equals("source")) {
						seSource = (SStructure) r.getTarget();
					}
					else if (ann != null && ann.equals("target")) {
						seTarget = (SStructure) r.getTarget();
					}
				}
			}
			SDominanceRelation signal2source = SaltFactory.createSDominanceRelation();
			SDominanceRelation signal2target = SaltFactory.createSDominanceRelation();
			signal2source.setSource(signalNode);
			signal2source.setTarget(seSource);
			signal2target.setSource(signalNode);
			signal2target.setTarget(seTarget);
			String relationNameKey = ((RSTImporterProperties) this.getProperties()).getRelationName();
			Object signaledRelation = secondaryEdge.getAnnotation("sec", relationNameKey).getValue();
			signal2source.createAnnotation("sec", "signal", signaledRelation);
			signalNode.createAnnotation("sec", "signaled_relation", signaledRelation);
			this.getDocument().getDocumentGraph().addRelation(signal2source);
			this.getDocument().getDocumentGraph().addRelation(signal2target);
			if (!this.signalsForSecondaryEdge.containsKey(secondaryEdge)) {
				this.signalsForSecondaryEdge.put(secondaryEdge, new ArrayList<>());
			}
			this.signalsForSecondaryEdge.get(secondaryEdge).add(signalNode);
			signalNode.createProcessingAnnotation(null, "earliest_token", earliestToken);
		}
	}

	private void mapSecondaryEdges() {
		List<SecondaryEdge> secondaryEdges = this.getCurrentRSTDocument().getSecondaryEdges();
		if (secondaryEdges != null && secondaryEdges.size() > 0) {
			for (SecondaryEdge e : this.getCurrentRSTDocument().getSecondaryEdges()) {
				this.mapSecondaryEdge(e);
			}
		}
	}

	private void mapSecondaryEdge(SecondaryEdge e) {
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

		SStructure ses = SaltFactory.createSStructure();
		this.secondaryEdgeIndex.put(e.getId(), ses);
		String relationNameKey = ((RSTImporterProperties) this.getProperties()).getRelationName();
		ses.createAnnotation("sec", relationNameKey, e.getRelationName());

		SDominanceRelation sourceRel = SaltFactory.createSDominanceRelation();
		sourceRel.setSource(ses);
		sourceRel.setTarget(sSource);
		sourceRel.createAnnotation(null, "end", "source");

		SDominanceRelation targetRel = SaltFactory.createSDominanceRelation();
		targetRel.setSource(ses);
		targetRel.setTarget(sTarget);
		targetRel.createAnnotation(null, "end", "target");

		this.getDocument().getDocumentGraph().addNode(ses);
		this.getDocument().getDocumentGraph().addRelation(sourceRel);
		this.getDocument().getDocumentGraph().addRelation(targetRel);
	}

	private void markEdgesWithSignals() {
		for (SDominanceRelation dr : this.getDocument().getDocumentGraph().getDominanceRelations()) {
			if (! (dr.getTarget() instanceof SToken)
					&& (dr.getSource().getAnnotation("sec", "signaled_relation") == null)
					&& (dr.getSource().getAnnotation("prim", "signaled_relation") == null)
					&& (dr.getSource().getAnnotation("sec", "relname") == null)) {
				dr.createAnnotation(null, "is_signaled", this.primaryRelationsWithSignals.contains(dr));
			}
		}
	}

	private void connectSecondaryEdgesToSignals() {
		for (Map.Entry<SStructure, List<SStructure>> kvp : this.signalsForSecondaryEdge.entrySet()) {
			SStructure secEdge = kvp.getKey();
			List<SStructure> signals = kvp.getValue();

			SStructure winningSignal = null;
			int earliestToken = Integer.MAX_VALUE;
			for (SStructure signal : signals) {
				int signalEarliestToken = (Integer) signal.getProcessingAnnotation("earliest_token").getValue();
				if (signalEarliestToken < earliestToken) {
					earliestToken = signalEarliestToken;
					winningSignal = signal;
				}
			}
			// If we don't have any signal, we should really complain, but just be lenient and allow secedges
			// without any signals
			if (winningSignal != null) {
				SDominanceRelation r = SaltFactory.createSDominanceRelation();
				r.setSource(secEdge);
				r.setTarget(winningSignal);
				this.getDocument().getDocumentGraph().addRelation(r);
			}
		}
	}
}
