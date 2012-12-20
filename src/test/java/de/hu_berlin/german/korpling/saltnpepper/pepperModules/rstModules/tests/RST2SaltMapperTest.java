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
package de.hu_berlin.german.korpling.saltnpepper.pepperModules.rstModules.tests;

import junit.framework.TestCase;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;

import de.hu_berlin.german.korpling.rst.Group;
import de.hu_berlin.german.korpling.rst.RSTDocument;
import de.hu_berlin.german.korpling.rst.RSTFactory;
import de.hu_berlin.german.korpling.rst.Segment;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.rstModules.RST2SaltMapper;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.rstModules.RSTImporterProperties;
import de.hu_berlin.german.korpling.saltnpepper.salt.SaltFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.resources.dot.Salt2DOT;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDataSourceSequence;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDominanceRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SStructure;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STYPE_NAME;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;

public class RST2SaltMapperTest extends TestCase{

	protected RST2SaltMapper fixture= null;

	public RST2SaltMapper getFixture() {
		return fixture;
	}

	public void setFixture(RST2SaltMapper fixture) {
		this.fixture = fixture;
	}
	
	public void setUp()
	{
		this.setFixture(new RST2SaltMapper());
		this.getFixture().setCurrentSDocument(SaltFactory.eINSTANCE.createSDocument());
		this.getFixture().setCurrentRSTDocument(RSTFactory.eINSTANCE.createRSTDocument());
		this.getFixture().setProps(new RSTImporterProperties());
	}	
	
	private String[] text= {"Is", "this", "example", "more", "complicated", "than", "it", "is", "supposed", "to", "be", "?"};
	private String text1= "Is this example";
	private String text2= "more complicated than it is supposed to be?";
	
	private RSTDocument addSegments(RSTDocument rstDocument)
	{	
		EList<Segment> segments= new BasicEList<Segment>();
		Segment seg1= RSTFactory.eINSTANCE.createSegment();
		seg1.setText(text1);
		seg1.setId("seg1");
		seg1.setType("multinuc");
		segments.add(seg1);
		this.getFixture().getCurrentRSTDocument().getSegments().add(seg1);
		
		Segment seg2= RSTFactory.eINSTANCE.createSegment();
		seg2.setText(text2);
		seg2.setId("seg2");
		seg2.setType("rst");
		segments.add(seg2);
		this.getFixture().getCurrentRSTDocument().getSegments().add(seg2);
		
		return(rstDocument);
	}
	
	public SStructure getSStructureByName(String sName)
	{
		for (SStructure sStruct: this.getFixture().getCurrentSDocument().getSDocumentGraph().getSStructures())
		{
			if (sStruct.getSName().equals(sName))
				return(sStruct);
		}
		return(null);
	}
	
	/**
	 * Maps a {@link RSTDocument} having only {@link Segment} nodes.
	 */
	public void testMapSegmentsWithTokenize()
	{
		addSegments(this.getFixture().getCurrentRSTDocument());
		
		this.getFixture().mapRSTDocument2SDocument();
		SDocumentGraph sDocGraph=  this.getFixture().getCurrentSDocument().getSDocumentGraph();
		
		//STextualDS
		assertNotNull(sDocGraph.getSTextualDSs());
		assertEquals(1,sDocGraph.getSTextualDSs().size());
		assertNotNull(sDocGraph.getSTextualDSs().get(0));
		assertEquals(text1+text2,sDocGraph.getSTextualDSs().get(0).getSText());
		
		//SToken
		assertNotNull(sDocGraph.getSTokens());
		assertEquals(12,sDocGraph.getSTokens().size());
		int i= 0;
		for (SToken sToken: sDocGraph.getSTokens())
		{
			assertNotNull(sToken);
			EList<STYPE_NAME> relTypes= new BasicEList<STYPE_NAME>();
			relTypes.add(STYPE_NAME.STEXT_OVERLAPPING_RELATION);
			SDataSourceSequence sequence= sDocGraph.getOverlappedDSSequences(sToken, relTypes).get(0);
			assertEquals(text[i], ((STextualDS)sequence.getSSequentialDS()).getSText().substring(sequence.getSStart(), sequence.getSEnd()));
			i++;
		}
		
		//SStructure
		assertNotNull(sDocGraph.getSStructures());
		assertEquals(2,sDocGraph.getSStructures().size());
		
		SStructure struct1= this.getSStructureByName("seg1");
		assertNotNull(struct1);
		assertEquals(2, struct1.getSAnnotations().size());
		assertEquals(RST2SaltMapper.NODE_KIND_SEGMENT, struct1.getSAnnotation(this.getFixture().getProps().getNodeKindName()).getSValue());
		assertEquals("multinuc", struct1.getSAnnotation(this.getFixture().getProps().getNodeTypeName()).getSValue());
		
		SStructure struct2= this.getSStructureByName("seg2");
		assertNotNull(struct2);
		assertEquals(2, struct2.getSAnnotations().size());
		assertEquals(RST2SaltMapper.NODE_KIND_SEGMENT, struct2.getSAnnotation(this.getFixture().getProps().getNodeKindName()).getSValue());
		assertEquals("rst", struct2.getSAnnotation(this.getFixture().getProps().getNodeTypeName()).getSValue());
		
		//SDominanceRelation
		assertNotNull(sDocGraph.getSDominanceRelations());
		assertEquals(12,sDocGraph.getSDominanceRelations().size());
	}
	
	/**
	 * Maps a {@link RSTDocument} having only {@link Segment} nodes.
	 */
	public void testMapSegmentsAndGroups()
	{
		String text1= "Jim went to Harvard,";
		String text2= "and John went to Yale.";
		String text3= "Therefore, both attended good schools.";
		
		Group group1= RSTFactory.eINSTANCE.createGroup();
		group1.setType("span");
		group1.setId("grp1");
		this.getFixture().getCurrentRSTDocument().getGroups().add(group1);
		
		Group group2= RSTFactory.eINSTANCE.createGroup();
		group2.setType("multinuc");
		group2.setId("grp2");
		this.getFixture().getCurrentRSTDocument().getGroups().add(group2);

		this.getFixture().getCurrentRSTDocument().createRelation(group1, group2, "span", null);
		
		EList<Segment> segments= new BasicEList<Segment>();
		Segment seg1= RSTFactory.eINSTANCE.createSegment();
		seg1.setText(text1);
		seg1.setId("seg1");
		segments.add(seg1);
		this.getFixture().getCurrentRSTDocument().getSegments().add(seg1);
		
		this.getFixture().getCurrentRSTDocument().createRelation(group2, seg1, "conjunction", "multinuc");
		
		Segment seg2= RSTFactory.eINSTANCE.createSegment();
		seg2.setText(text2);
		seg2.setId("seg2");
		segments.add(seg2);
		this.getFixture().getCurrentRSTDocument().getSegments().add(seg2);
		
		this.getFixture().getCurrentRSTDocument().createRelation(group2, seg2, "conjunction", "multinuc");
		
		Segment seg3= RSTFactory.eINSTANCE.createSegment();
		seg3.setText(text3);
		seg3.setId("seg3");
		segments.add(seg3);
		this.getFixture().getCurrentRSTDocument().getSegments().add(seg3);
		
		this.getFixture().getCurrentRSTDocument().createRelation(group2, seg3, "nonvolitional-result", "rst");
		
		this.getFixture().mapRSTDocument2SDocument();
		SDocumentGraph sDocGraph=  this.getFixture().getCurrentSDocument().getSDocumentGraph();
		
		Salt2DOT s2d= new Salt2DOT();
		s2d.salt2Dot(sDocGraph, URI.createFileURI("d:/Test/RST/Benjamin/rst/RSTManMini/dot/b1.dot"));
		
		//STextualDS
		assertNotNull(sDocGraph.getSTextualDSs());
		assertEquals(1,sDocGraph.getSTextualDSs().size());
		assertNotNull(sDocGraph.getSTextualDSs().get(0));
		
		//SToken
		assertNotNull(sDocGraph.getSTokens());
		assertEquals(18,sDocGraph.getSTokens().size());
	
		//SStructure
		assertNotNull(sDocGraph.getSStructures());
		assertEquals(5,sDocGraph.getSStructures().size());
		
		SStructure grp1= this.getSStructureByName("grp1");
		assertNotNull(grp1);
		assertEquals(2, grp1.getSAnnotations().size());
		assertEquals(RST2SaltMapper.NODE_KIND_GROUP, grp1.getSAnnotation(this.getFixture().getProps().getNodeKindName()).getSValue());
		assertEquals("span", grp1.getSAnnotation(this.getFixture().getProps().getNodeTypeName()).getSValue());
		
		SStructure grp2= this.getSStructureByName("grp2");
		assertNotNull(grp2);
		assertEquals(2, grp2.getSAnnotations().size());
		assertEquals(RST2SaltMapper.NODE_KIND_GROUP, grp2.getSAnnotation(this.getFixture().getProps().getNodeKindName()).getSValue());
		assertEquals("multinuc", grp2.getSAnnotation(this.getFixture().getProps().getNodeTypeName()).getSValue());

		
		SStructure struct1= this.getSStructureByName("seg1");
		assertNotNull(struct1);
		assertEquals(1, struct1.getSAnnotations().size());
		assertEquals(RST2SaltMapper.NODE_KIND_SEGMENT, struct1.getSAnnotation(this.getFixture().getProps().getNodeKindName()).getSValue());
		
		SStructure struct2= this.getSStructureByName("seg2");
		assertNotNull(struct2);
		assertEquals(1, struct2.getSAnnotations().size());
		assertEquals(RST2SaltMapper.NODE_KIND_SEGMENT, struct2.getSAnnotation(this.getFixture().getProps().getNodeKindName()).getSValue());
		
		SStructure struct3= this.getSStructureByName("seg3");
		assertNotNull(struct3);
		assertEquals(1, struct3.getSAnnotations().size());
		assertEquals(RST2SaltMapper.NODE_KIND_SEGMENT, struct3.getSAnnotation(this.getFixture().getProps().getNodeKindName()).getSValue());
					
		//SDominanceRelation
		assertNotNull(sDocGraph.getSDominanceRelations());
		assertEquals(22,sDocGraph.getSDominanceRelations().size());
		
		assertEquals(1, sDocGraph.getEdges(grp1.getSId(), grp2.getSId()).size());
		assertTrue(sDocGraph.getEdges(grp1.getSId(), grp2.getSId()).get(0) instanceof SDominanceRelation);
		assertEquals("span", ((SDominanceRelation)sDocGraph.getEdges(grp1.getSId(), grp2.getSId()).get(0)).getSAnnotation(this.getFixture().getProps().getRelationName()).getSValue());
		
		
		assertEquals(1, sDocGraph.getEdges(grp2.getSId(), struct1.getSId()).size());
		assertTrue(sDocGraph.getEdges(grp2.getSId(), struct1.getSId()).get(0) instanceof SDominanceRelation);
		assertEquals("multinuc", ((SDominanceRelation)sDocGraph.getEdges(grp2.getSId(), struct1.getSId()).get(0)).getSTypes().get(0));
		assertEquals("conjunction", ((SDominanceRelation)sDocGraph.getEdges(grp2.getSId(), struct1.getSId()).get(0)).getSAnnotation(this.getFixture().getProps().getRelationName()).getSValue());
		
		assertEquals(1, sDocGraph.getEdges(grp2.getSId(), struct2.getSId()).size());
		assertTrue(sDocGraph.getEdges(grp2.getSId(), struct2.getSId()).get(0) instanceof SDominanceRelation);
		assertEquals("multinuc", ((SDominanceRelation)sDocGraph.getEdges(grp2.getSId(), struct2.getSId()).get(0)).getSTypes().get(0));
		assertEquals("conjunction", ((SDominanceRelation)sDocGraph.getEdges(grp2.getSId(), struct2.getSId()).get(0)).getSAnnotation(this.getFixture().getProps().getRelationName()).getSValue());
		
		assertEquals(1, sDocGraph.getEdges(grp2.getSId(), struct3.getSId()).size());
		assertTrue(sDocGraph.getEdges(grp2.getSId(), struct3.getSId()).get(0) instanceof SDominanceRelation);
		assertEquals("rst", ((SDominanceRelation)sDocGraph.getEdges(grp2.getSId(), struct3.getSId()).get(0)).getSTypes().get(0));
		assertEquals("nonvolitional-result", ((SDominanceRelation)sDocGraph.getEdges(grp2.getSId(), struct3.getSId()).get(0)).getSAnnotation(this.getFixture().getProps().getRelationName()).getSValue());
		
	}
}
