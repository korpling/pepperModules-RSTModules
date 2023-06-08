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

import java.util.*;

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
import org.corpus_tools.salt.core.SNode;

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
		this.rstId2UUID = new Hashtable<>();
		this.primaryEdgeIndex = new Hashtable<>();
	}

	private RSTDocument currentRSTDocument = null;

	public void setCurrentRSTDocument(RSTDocument currentRSTDocument) {
		this.rstId2SStructure = new Hashtable<>();
		this.rstId2UUID = new Hashtable<>();
		this.primaryEdgeIndex = new Hashtable<>();
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
		this.rstId2UUID = new Hashtable<>();
		this.primaryEdgeIndex = new Hashtable<>();
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
		if (!this.getCurrentRSTDocument().getSegments().isEmpty()) {
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

		this.markTokens();
		this.markSecondaryEdges();
		this.markSignals();
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
	 * Maps from secondary edge IDs to a list of associated signals
	 */
	private Map<String, UUID> rstId2UUID = null;

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
		if ((segments != null) && (!segments.isEmpty())) {
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
				if ((tokens != null) && (!tokens.isEmpty())) {// if tokens exist
					SStructure sStruct = SaltFactory.createSStructure();
					sStruct.setName(segment.getId());
					String nodeKindKey = ((RSTImporterProperties) this.getProperties()).getNodeKindName();
					String nodeTypeKey = ((RSTImporterProperties) this.getProperties()).getNodeTypeName();
					sStruct.createAnnotation(null, nodeKindKey, NODE_KIND_SEGMENT);
					if (segment.getType() != null)
						sStruct.createAnnotation(null, nodeTypeKey, segment.getType());

					// puts segment.id and mapped SStructure-object into table
					this.rstId2SStructure.put(segment.getId(), sStruct);
					UUID uuid = UUID.randomUUID();
					sStruct.createAnnotation("TEMP", "uuid", uuid);
					this.rstId2UUID.put(segment.getId(), uuid);
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
		if ((segments != null) && (!segments.isEmpty())) {
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
				UUID uuid = UUID.randomUUID();
				sStruct.createAnnotation("TEMP", "uuid", uuid);
				this.rstId2UUID.put(segment.getId(), uuid);
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
			UUID uuid = UUID.randomUUID();
			sStructure.createAnnotation("TEMP", "uuid", uuid);
			this.rstId2UUID.put(group.getId(), uuid);

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

	private void markSignals() {
		List<Signal> signals = this.getCurrentRSTDocument().getSignals();
		if (signals != null && !signals.isEmpty()) {
			for (Signal signal : this.getCurrentRSTDocument().getSignals()) {
				this.markSignal(signal);
			}
		}
	}

	private void markSignal(Signal signal) {
		// If the signal is null or its source attribute is null, quit
		if (signal == null) {
			return;
		}
		if (signal.getSource() == null) {
			throw new PepperModuleException(this, "Cannot map the rst-model of file'" + this.getResourceURI()
					+ "', because the source of a signal is empty.");
		}

		String sourceId = signal.getSource().getId();
		sourceId = sourceId.contains("-") ? sourceId.split("-")[0] : sourceId;
		SNode n = this.rstId2SStructure.get(sourceId);
		if (n.getAnnotation("TEMP", "signals") == null) {
			n.createAnnotation("TEMP", "signals", new ArrayList<Map<Object, Object>>());
		}
		List<Map<Object, Object>> signalList = (List<Map<Object, Object>>) n.getAnnotation("TEMP", "signals").getValue();
		Map<Object, Object> signalMap = new HashMap<>();
		List<UUID> tokenIds = new ArrayList<>();
		if (signal.getTokenIds() != null) {
			for (Integer tid : signal.getTokenIds()) {
				tokenIds.add(this.rstId2UUID.get("token" + tid));
			}
		}
		signalMap.put("signal:type", signal.getType());
		signalMap.put("signal:subtype", signal.getSubtype());
		signalMap.put("signal:tokens", tokenIds);

		List<UUID> source = new ArrayList<>();
		if (signal.getSource().getId().contains("-")) {
			String part1 = signal.getSource().getId().split("-")[0];
			String part2 = signal.getSource().getId().split("-")[1];
			source.add(this.rstId2UUID.get(part1));
			source.add(this.rstId2UUID.get(part2));
		} else {
			source.add(this.rstId2UUID.get(signal.getSource().getId()));
		}
		signalMap.put("signal:source", source);
		signalList.add(signalMap);
	}

	private void markSecondaryEdges() {
	List<SecondaryEdge> secondaryEdges = this.getCurrentRSTDocument().getSecondaryEdges();
		if (secondaryEdges != null && !secondaryEdges.isEmpty()) {
			for (SecondaryEdge e : this.getCurrentRSTDocument().getSecondaryEdges()) {
				this.markSecondaryEdge(e);
			}
		}
	}

	private void markSecondaryEdge(SecondaryEdge e) {
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

		if (sSource.getAnnotation("TEMP", "secedges") == null) {
			sSource.createAnnotation("TEMP", "secedges", new ArrayList<Map<String, String>>());
		}
		Map<String, Object> sMap = new HashMap<>();
		sMap.put("edgeSource", this.rstId2UUID.get(e.getId().split("-")[0]));
		sMap.put("edgeTarget", this.rstId2UUID.get(e.getId().split("-")[1]));
		sMap.put("relationName", e.getRelationName());
		((List<Map<String, Object>>) sSource.getAnnotation("TEMP", "secedges").getValue()).add(sMap);
	}

	private void markTokens() {
		int i = 1;
		for (SToken t : getDocument().getDocumentGraph().getTokens()) {
			UUID uuid = UUID.randomUUID();
			t.createAnnotation("TEMP", "uuid", uuid);
			this.rstId2UUID.put("token" + i++, uuid);
		}

		SNode n = getDocument().getDocumentGraph().getNodes().get(0);
		if (n != null) {
			n.createAnnotation("TEMP", "rstid2uuid", this.rstId2UUID);
		}
	}
}
