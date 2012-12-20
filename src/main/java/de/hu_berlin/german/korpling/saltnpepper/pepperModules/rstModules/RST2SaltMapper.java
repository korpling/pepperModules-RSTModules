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

import java.util.Hashtable;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.log.LogService;

import de.hu_berlin.german.korpling.rst.Group;
import de.hu_berlin.german.korpling.rst.RSTDocument;
import de.hu_berlin.german.korpling.rst.Relation;
import de.hu_berlin.german.korpling.rst.Segment;
import de.hu_berlin.german.korpling.saltnpepper.misc.treetagger.tokenizer.TTTokenizer;
import de.hu_berlin.german.korpling.saltnpepper.misc.treetagger.tokenizer.TTTokenizer.TT_LANGUAGES;
import de.hu_berlin.german.korpling.saltnpepper.misc.treetagger.tokenizer.Token;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.rstModules.exceptions.RSTImporterException;
import de.hu_berlin.german.korpling.saltnpepper.salt.SaltFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDominanceRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SStructure;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SAnnotation;

/**
 * Maps a Rst-Document (RSTDocument) to a Salt document (SDocument).
 * <ul>
 * 	<li>
 * 		all relations, having a different type from span will be mapped to a pointing relation (or a dominance 
 * 		relation with type=secedge, this is done for ANNIS)
 * 	<li/>
 * 	<li>
 * 		for all relations of type span an artificial relation will be created for all transitive children of the relations source to the relations target
 * 	</li>
 *  <li>
 *  	the text including in a segment will be tokenized and SToken objects will be created
 *  </li>
 *  
 * </ul>
 *  
 * @author Florian Zipser
 *
 */
public class RST2SaltMapper 
{
	/**
	 * Initializes this object.
	 */
	public RST2SaltMapper()
	{
		this.init();
	}
	
	/**
	 * Initializes this object. Sets the tokenizer.
	 */
	private void init()
	{
		this.tokenizer= new TTTokenizer();
		this.rstId2SStructure= new Hashtable<String, SStructure>();
	}
	
// ================================================ start: LogService	
	private LogService logService;

	public void setLogService(LogService logService) 
	{
		this.logService = logService;
	}
	
	public LogService getLogService() 
	{
		return(this.logService);
	}
// ================================================ end: LogService
// ================================================ start: property setting
	/**
	 * properties for the mapping.
	 */
	private RSTImporterProperties props= null;
	/**
	 * Sets properties for the mapping.
	 * @param props the props to set
	 */
	public void setProps(RSTImporterProperties props) 
	{
		this.props = props;
	}

	/**
	 * Returns the set properties for the mapping.
	 * @return the props
	 */
	public RSTImporterProperties getProps() {
		return props;
	}
	
// ================================================ start: property setting
	
// ================================================ start: physical path of the rst-documents
	/**
	 * Stores the physical path of the rst-documents.
	 */
	private URI currentRSTDocumentURI= null;
	/**
	 * @param currentRSTDocument the currentRSTDocument to set
	 */
	public void setCurrentRSTDocumentURI(URI currentRSTDocumentURI) {
		this.currentRSTDocumentURI = currentRSTDocumentURI;
	}

	/**
	 * @return the currentRSTDocument
	 */
	public URI getCurrentRSTDocumentURI() 
	{
		return currentRSTDocumentURI;
	}
	
// ================================================ end: physical path of the rst-documents
// ================================================ start: current SDocument	
	private SDocument currentSDocument= null;
	/**
	 * @param currentSDocument the currentSDocument to set
	 */
	public void setCurrentSDocument(SDocument currentSDocument) {
		this.currentSDocument = currentSDocument;
	}

	/**
	 * @return the currentSDocument
	 */
	public SDocument getCurrentSDocument() {
		return this.currentSDocument;
	}
// ================================================ end: current SDocument
// ================================================ start: current RSTDocument	
	private RSTDocument currentRSTDocument= null;
	/**
	 * @param currentSDocument the currentSDocument to set
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
	
	private void setParams()
	{
		if (this.getProps()== null)
		{
			throw new RSTImporterException("No Property object is given, this seems to be an internal bug.");
		}
		else
		{
			if (this.getProps().getAbbreviationFolder()!= null)
			{//abbreviation folder is set
				this.tokenizer.setAbbreviationFolder(this.getProps().getAbbreviationFolder());
			}//abbreviation folder is set
			if (this.getProps().getLanguage()!= null)
			{//abbreviation folder is set
				if (TT_LANGUAGES.valueOf(this.getProps().getLanguage())!= null)
					this.tokenizer.setLngLang(TT_LANGUAGES.valueOf(this.getProps().getLanguage()));
			}//abbreviation folder is set
		}
	}
	
	/**
	 * stores the rstId of an AbstractNode and the corresponding SStructure mapped to the AbstractNode
	 */
	private Hashtable<String, SStructure> rstId2SStructure= null;
	/**
	 * Maps the set current rstDocument given by an URI, to the set current SDocument. 
	 */
	public void mapRSTDocument2SDocument()
	{
		setParams();
		this.getCurrentSDocument().setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
			
		//map segments to STextualDS, Tokens and SStructures
			if (this.getCurrentRSTDocument().getSegments().size()> 0)
			{	
				if (this.getProps().isToTokenize())
					this.mapSegmentsWithTokenize(this.getCurrentRSTDocument().getSegments());
				else
					this.mapSegmentsWithoutTokenize(this.getCurrentRSTDocument().getSegments());
			}
		//map segments to STextualDS, Tokens and SStructures
		//map group to SStructure
			for (Group group: this.getCurrentRSTDocument().getGroups())
				this.mapGroup2SStructure(group);
		//map group to SStructure
		//maps all relations and creates artificial ones if neccessary
			for (Relation relation: this.getCurrentRSTDocument().getRelations())
			{
				this.mapRelation(relation);
			}
		//maps all relations and creates artificial ones if neccessary
	}
	
	/**
	 * The TreeTaggerTokenizer to tokenize an untokenized primary text.
	 */
	private TTTokenizer tokenizer= null;
	
	/**
	 * Returns the TreeTaggerTokenizer to tokenize an untokenized primary text.
	 * @return
	 */
	public TTTokenizer getTokenizer() {
		return tokenizer;
	}
	/**
	 * Name of node kind segment.
	 */
	public static final String NODE_KIND_SEGMENT="segment";
	/**
	 * Name of node kind group. 
	 */
	public static final String NODE_KIND_GROUP="group";
	
	/**
	 * Maps the given {@link Segment} to the current {@link STextualDS} by adding all textual values of segment after
	 * the preceding one. The created {@link STextualDS} will be added to the {@link SDocumentGraph}. Also {@link SToken} objects 
	 * created by a tokenizer will be added and connected to the {@link STextualDS} object. As last a {@link SStructure} object 
	 * for the {@link Segment} object will be created and related to the {@link SToken} objects.
	 * @param segments a list of {@link Segment} objects
	 * @return
	 */
	public void mapSegmentsWithTokenize(EList<Segment> segments)
	{
		STextualDS sText= null;
		if (	(segments!= null)&&
				(segments.size()> 0))
		{
			sText= SaltFactory.eINSTANCE.createSTextualDS();
			this.getCurrentSDocument().getSDocumentGraph().addSNode(sText);
			StringBuffer strBuffer= new StringBuffer();
			for (Segment segment: segments)
			{//for all segments adding their text, creating tokens, and relations	
				List<Token> tokens= null;
				try
				{
					tokens= tokenizer.tokenizeToToken(segment.getText());
				}
				catch (Exception e) 
				{
					throw new RSTImporterException("Cannot tokenize the following sentence: "+segment.getText(), e);
				}				
				if (	(tokens!= null)&&
						(tokens.size()> 0))
				{//if tokens exist	
					SStructure sStruct= SaltFactory.eINSTANCE.createSStructure();
					sStruct.setSName(segment.getId());
					sStruct.createSAnnotation(null, this.getProps().getNodeKindName(), NODE_KIND_SEGMENT);
					if (segment.getType()!= null)
						sStruct.createSAnnotation(null, this.getProps().getNodeTypeName(), segment.getType());
					
					//puts segment.id and mapped SStructure-object into table
					this.rstId2SStructure.put(segment.getId(), sStruct);
					this.getCurrentSDocument().getSDocumentGraph().addSNode(sStruct);
					
					for (Token token: tokens)
					{//put each token in SDocumentGraph
						SToken sToken= SaltFactory.eINSTANCE.createSToken();
						this.getCurrentSDocument().getSDocumentGraph().addSNode(sToken);
						
						STextualRelation sTextRel= SaltFactory.eINSTANCE.createSTextualRelation();
						sTextRel.setSTextualDS(sText);
						sTextRel.setSToken(sToken);
						sTextRel.setSStart(strBuffer.length()+ token.start);
						sTextRel.setSEnd(strBuffer.length()+ token.end);
						this.getCurrentSDocument().getSDocumentGraph().addSRelation(sTextRel);
						
						SDominanceRelation sDomRel= SaltFactory.eINSTANCE.createSDominanceRelation();
						sDomRel.setSSource(sStruct);
						sDomRel.setSTarget(sToken);
						this.getCurrentSDocument().getSDocumentGraph().addSRelation(sDomRel);
					}//put each token in SDocumentGraph
					strBuffer.append(segment.getText());
				}//if tokens exist
			}//for all segments
			sText.setSText(strBuffer.toString());
		}
	}
	
	/**
	 * Maps the given segment to the current STextualDS by adding all textual values of segment behind
	 * the preceding. The created STextualDS will be added to the SDocumentGraph. Also a SToken object will be 
	 * created and related to the STextualDS. As last a SSTructure object for the segment will be created and 
	 * related to the tokens.
	 * @param segments 
	 * @return
	 */
	private void mapSegmentsWithoutTokenize(EList<Segment> segments)
	{
		STextualDS sText= null;
		if (	(segments!= null)&&
				(segments.size()> 0))
		{
			sText= SaltFactory.eINSTANCE.createSTextualDS();
			this.getCurrentSDocument().getSDocumentGraph().addSNode(sText);
			StringBuffer strBuffer= new StringBuffer();
			
			for (Segment segment: segments)
			{//for all segments adding their text, creating tokens, and relations	
				SStructure sStruct= SaltFactory.eINSTANCE.createSStructure();
				sStruct.createSAnnotation(null, this.getProps().getNodeKindName(), NODE_KIND_SEGMENT);
				if (segment.getType()!= null)
					sStruct.createSAnnotation(null, this.getProps().getNodeTypeName(), segment.getType());
				sStruct.setSName(segment.getId());
				//puts segment.id and mapped SStructure-object into table
				this.rstId2SStructure.put(segment.getId(), sStruct);
				this.getCurrentSDocument().getSDocumentGraph().addSNode(sStruct);
				
				SToken sToken= SaltFactory.eINSTANCE.createSToken();
				this.getCurrentSDocument().getSDocumentGraph().addSNode(sToken);
				
				STextualRelation sTextRel= SaltFactory.eINSTANCE.createSTextualRelation();
				sTextRel.setSTextualDS(sText);
				sTextRel.setSToken(sToken);
				sTextRel.setSStart(strBuffer.length());
				sTextRel.setSEnd(strBuffer.length()+ segment.getText().length());
				this.getCurrentSDocument().getSDocumentGraph().addSRelation(sTextRel);
				
				SDominanceRelation sDomRel= SaltFactory.eINSTANCE.createSDominanceRelation();
				sDomRel.setSSource(sStruct);
				sDomRel.setSTarget(sToken);
				this.getCurrentSDocument().getSDocumentGraph().addSRelation(sDomRel);
				strBuffer.append(segment.getText());
			}//for all segments
			sText.setSText(strBuffer.toString());
		}
	}
	
	/**
	 * Maps the given group to a SStructure object, The SStructure object will be added to the graph 
	 * @param group
	 * @return the created SStructure-object
	 */
	private SStructure mapGroup2SStructure(Group group)
	{
		SStructure sStructure= null;
		if (group!= null)
		{
			sStructure= SaltFactory.eINSTANCE.createSStructure();
			sStructure.setSName(group.getId());
			if (group.getType()!= null)
				sStructure.createSAnnotation(null, this.getProps().getNodeTypeName(), group.getType());
			
			//puts segment.id and mapped SSTructure-object into table
			this.rstId2SStructure.put(group.getId(), sStructure);
			
			{//create SAnnotation containing the group as value
				SAnnotation sAnno= SaltFactory.eINSTANCE.createSAnnotation();
				sAnno.setSName(this.getProps().getNodeKindName());
				sAnno.setSValue(NODE_KIND_GROUP);
				sStructure.addSAnnotation(sAnno);
			}//create SAnnotation containing the group as value
			this.getCurrentSDocument().getSDocumentGraph().addSNode(sStructure);
		}
		return(sStructure);
	}

	/**
	 * Mapps the given relation to one in the Salt model. Further artificial ones will be created.
	 * @param relation
	 */
	private void mapRelation(Relation relation)
	{
		if (relation!= null)
		{	
			if (relation.getParent()== null)
				throw new RSTImporterException("Cannot map the rst-model of file'"+this.getCurrentRSTDocumentURI()+"', because the parent of a relation is empty.");
			if (relation.getParent()== null)
				throw new RSTImporterException("Cannot map the rst-model of file'"+this.getCurrentRSTDocumentURI()+"', because the source of a relation is empty.");
			
			SStructure sSource= this.rstId2SStructure.get(relation.getParent().getId());
			SStructure sTarget= this.rstId2SStructure.get(relation.getChild().getId());
			if (sSource== null)
				throw new RSTImporterException("Cannot map the rst-model of file'"+this.getCurrentRSTDocumentURI()+"', because the parent of a relation points to a non existing node with id '"+relation.getChild().getId()+"'.");
			if (sTarget== null)
				throw new RSTImporterException("Cannot map the rst-model of file'"+this.getCurrentRSTDocumentURI()+"', because the parent of a relation belongs to a non existing node with id '"+relation.getParent().getId()+"'.");
			
			SDominanceRelation sDomRel= SaltFactory.eINSTANCE.createSDominanceRelation();
			if (relation.getType()!= null)
				sDomRel.addSType(relation.getType());
			sDomRel.setSSource(sSource);
			sDomRel.setSTarget(sTarget);
			this.getCurrentSDocument().getSDocumentGraph().addSRelation(sDomRel);
			
			if (relation.getName()!= null)
				sDomRel.createSAnnotation(null, this.getProps().getRelationName(), relation.getName());
			
			
//			if (	(	(relation.getType()!= null)&&
//						("span".equalsIgnoreCase(relation.getType())))||
//					(	(relation.getName()!= null)&&
//						("span".equalsIgnoreCase(relation.getName())))	)
//			{//either name or type is "span"
//				this.createTransitiveRelations(relation, sSource);
//			}//either name or type is "span"
		}
	}
	
//	/**
//	 * Traverses the rstGraph and creates artificial relations for all non-span relations found in
//	 * subgraph with root parentNode.
//	 * @param relation
//	 * @param parentNode
//	 * @param childNode
//	 */
//	private void createTransitiveRelations(Relation relation, SStructure parentNode)
//	{
//		EList<Relation> incomingRelations= this.getCurrentRSTDocument().getIncomingRelations(relation.getChild().getId());
//		if (incomingRelations!= null)
//		{
//			for (Relation incomingRelation: incomingRelations)
//			{
//				if (	(	(incomingRelation.getType()== null)||
//							(!incomingRelation.getType().equalsIgnoreCase("span")))&&
//						(	(incomingRelation.getName()== null)||
//							(!incomingRelation.getName().equalsIgnoreCase("span")))	)
//				{//neither name nor type is "span"
//					if (relation.getParent()== null)
//						throw new RSTImporterException("Cannot map the rst-model of file'"+this.getCurrentRSTDocumentURI()+"', because the parent of a relation is empty.");
//					if (relation.getParent()== null)
//						throw new RSTImporterException("Cannot map the rst-model of file'"+this.getCurrentRSTDocumentURI()+"', because the source of a relation is empty.");
//					
//					SStructure sTarget= this.rstId2SStructure.get(incomingRelation.getChild().getId());
//					if (sTarget== null)
//						throw new RSTImporterException("Cannot map the rst-model of file'"+this.getCurrentRSTDocumentURI()+"', because the parent of a relation belongs to a non existing node with id '"+relation.getChild().getId()+"'.");
//
//					SDominanceRelation sDomRel= SaltFactory.eINSTANCE.createSDominanceRelation();
//					//TODO delete the comment and delete the creation of dominance relation
//					sDomRel.addSType(relation.getType());
//					sDomRel.setSSource(parentNode);
//					sDomRel.setSTarget(sTarget);
//					this.getCurrentSDocument().getSDocumentGraph().addSRelation(sDomRel);
//					
//					if (incomingRelation.getName()!= null)
//						sDomRel.createSAnnotation(null, this.getProps().getRelationName(), relation.getName());
//					
//					this.createTransitiveRelations(incomingRelation, parentNode);
//				}//neither name nor type is "span"
//			}
//		}
//	}
}
