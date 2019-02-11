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
package org.corpus_tools.peppermodules.rstModules.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.corpus_tools.peppermodules.rstModules.RST2SaltMapper;
import org.corpus_tools.peppermodules.rstModules.RSTImporterProperties;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SDominanceRelation;
import org.corpus_tools.salt.common.SStructure;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SRelation;
import org.corpus_tools.salt.util.DataSourceSequence;
import org.junit.Before;
import org.junit.Test;


import org.corpus_tools.peppermodules.rstModules.models.Group;
import org.corpus_tools.peppermodules.rstModules.models.RSTDocument;
import org.corpus_tools.peppermodules.rstModules.models.Segment;

public class RST2SaltMapperTest {

	protected RST2SaltMapper fixture = null;

	public RST2SaltMapper getFixture() {
		return fixture;
	}

	public void setFixture(RST2SaltMapper fixture) {
		this.fixture = fixture;
	}

	@Before
	public void setUp() {
		this.setFixture(new RST2SaltMapper());
		getFixture().setDocument(SaltFactory.createSDocument());
		getFixture().setCurrentRSTDocument(new RSTDocument());
		getFixture().setProperties(new RSTImporterProperties());
	}

	private String[] text = { "Is", "this", "example", "more", "complicated", "than", "it", "is", "supposed", "to", "be", "?" };
	private String text1 = "Is this example";
	private String text2 = "more complicated than it is supposed to be?";

	private RSTDocument addSegments(RSTDocument rstDocument) {
		List<Segment> segments = new ArrayList<Segment>();
		Segment seg1 = new Segment();
		seg1.setText(text1);
		seg1.setId("seg1");
		seg1.setType("multinuc");
		segments.add(seg1);
		getFixture().getCurrentRSTDocument().getSegments().add(seg1);

		Segment seg2 = new Segment();
		seg2.setText(text2);
		seg2.setId("seg2");
		seg2.setType("rst");
		segments.add(seg2);
		getFixture().getCurrentRSTDocument().getSegments().add(seg2);

		return (rstDocument);
	}

	public SStructure getSStructureByName(String sName) {
		for (SStructure sStruct : getFixture().getDocument().getDocumentGraph().getStructures()) {
			if (sStruct.getName().equals(sName))
				return (sStruct);
		}
		return (null);
	}

	/**
	 * Maps a {@link RSTDocument} having only {@link Segment} nodes.
	 */
	@Test
	public void testMapSegmentsWithTokenize() {
		addSegments(getFixture().getCurrentRSTDocument());

		getFixture().mapSDocument(getFixture().getCurrentRSTDocument());
		SDocumentGraph sDocGraph = getFixture().getDocument().getDocumentGraph();

		// STextualDS
		assertNotNull(sDocGraph.getTextualDSs());
		assertEquals(1, sDocGraph.getTextualDSs().size());
		assertNotNull(sDocGraph.getTextualDSs().get(0));
		assertEquals(text1 + " " + text2, sDocGraph.getTextualDSs().get(0).getText());

		// SToken
		assertNotNull(sDocGraph.getTokens());
		assertEquals(12, sDocGraph.getTokens().size());
		int i = 0;
		for (SToken sToken : sDocGraph.getTokens()) {
			assertNotNull(sToken);
			DataSourceSequence sequence = sDocGraph.getOverlappedDataSourceSequence(sToken, SALT_TYPE.STEXT_OVERLAPPING_RELATION).get(0);
			assertEquals(text[i], ((STextualDS) sequence.getDataSource()).getText().substring((Integer)sequence.getStart(), (Integer)sequence.getEnd()));
			i++;
		}

		// SStructure
		assertNotNull(sDocGraph.getStructures());
		assertEquals(2, sDocGraph.getStructures().size());

		SStructure struct1 = this.getSStructureByName("seg1");
		assertNotNull(struct1);
		assertEquals(2, struct1.getAnnotations().size());
		assertEquals(RST2SaltMapper.NODE_KIND_SEGMENT, struct1.getAnnotation(((RSTImporterProperties) getFixture().getProperties()).getNodeKindName()).getValue());
		assertEquals("multinuc", struct1.getAnnotation(((RSTImporterProperties) getFixture().getProperties()).getNodeTypeName()).getValue());

		SStructure struct2 = this.getSStructureByName("seg2");
		assertNotNull(struct2);
		assertEquals(2, struct2.getAnnotations().size());
		assertEquals(RST2SaltMapper.NODE_KIND_SEGMENT, struct2.getAnnotation(((RSTImporterProperties) getFixture().getProperties()).getNodeKindName()).getValue());
		assertEquals("rst", struct2.getAnnotation(((RSTImporterProperties) getFixture().getProperties()).getNodeTypeName()).getValue());

		// SDominanceRelation
		assertNotNull(sDocGraph.getDominanceRelations());
		assertEquals(12, sDocGraph.getDominanceRelations().size());
	}

	/**
	 * Maps a {@link RSTDocument} having only {@link Segment} nodes.
	 */
	@Test
	public void testMapSegmentsAndGroups() {
		String text1 = "Jim went to Harvard,";
		String text2 = "and John went to Yale.";
		String text3 = "Therefore, both attended good schools.";

		Group group1 = new Group();
		group1.setType("span");
		group1.setId("grp1");
		getFixture().getCurrentRSTDocument().getGroups().add(group1);

		Group group2 = new Group();
		group2.setType("multinuc");
		group2.setId("grp2");
		getFixture().getCurrentRSTDocument().getGroups().add(group2);

		getFixture().getCurrentRSTDocument().createRelation(group1, group2, "span", null);

		List<Segment> segments = new ArrayList<Segment>();
		Segment seg1 = new Segment();
		seg1.setText(text1);
		seg1.setId("seg1");
		segments.add(seg1);
		getFixture().getCurrentRSTDocument().getSegments().add(seg1);

		getFixture().getCurrentRSTDocument().createRelation(group2, seg1, "conjunction", "multinuc");

		Segment seg2 = new Segment();
		seg2.setText(text2);
		seg2.setId("seg2");
		segments.add(seg2);
		getFixture().getCurrentRSTDocument().getSegments().add(seg2);

		getFixture().getCurrentRSTDocument().createRelation(group2, seg2, "conjunction", "multinuc");

		Segment seg3 = new Segment();
		seg3.setText(text3);
		seg3.setId("seg3");
		segments.add(seg3);
		getFixture().getCurrentRSTDocument().getSegments().add(seg3);

		getFixture().getCurrentRSTDocument().createRelation(group2, seg3, "nonvolitional-result", "rst");

		getFixture().mapSDocument(getFixture().getCurrentRSTDocument());
		SDocumentGraph sDocGraph = getFixture().getDocument().getDocumentGraph();

		// STextualDS
		assertNotNull(sDocGraph.getTextualDSs());
		assertEquals(1, sDocGraph.getTextualDSs().size());
		assertNotNull(sDocGraph.getTextualDSs().get(0));

		// SToken
		assertNotNull(sDocGraph.getTokens());
		assertEquals(18, sDocGraph.getTokens().size());

		// SStructure
		assertNotNull(sDocGraph.getStructures());
		assertEquals(5, sDocGraph.getStructures().size());

		SStructure grp1 = this.getSStructureByName("grp1");
		assertNotNull(grp1);
		assertEquals(2, grp1.getAnnotations().size());
		assertEquals(RST2SaltMapper.NODE_KIND_GROUP, grp1.getAnnotation(((RSTImporterProperties) getFixture().getProperties()).getNodeKindName()).getValue());
		assertEquals("span", grp1.getAnnotation(((RSTImporterProperties) getFixture().getProperties()).getNodeTypeName()).getValue());

		SStructure grp2 = this.getSStructureByName("grp2");
		assertNotNull(grp2);
		assertEquals(2, grp2.getAnnotations().size());
		assertEquals(RST2SaltMapper.NODE_KIND_GROUP, grp2.getAnnotation(((RSTImporterProperties) getFixture().getProperties()).getNodeKindName()).getValue());
		assertEquals("multinuc", grp2.getAnnotation(((RSTImporterProperties) getFixture().getProperties()).getNodeTypeName()).getValue());

		SStructure struct1 = this.getSStructureByName("seg1");
		assertNotNull(struct1);
		assertEquals(1, struct1.getAnnotations().size());
		assertEquals(RST2SaltMapper.NODE_KIND_SEGMENT, struct1.getAnnotation(((RSTImporterProperties) getFixture().getProperties()).getNodeKindName()).getValue());

		SStructure struct2 = this.getSStructureByName("seg2");
		assertNotNull(struct2);
		assertEquals(1, struct2.getAnnotations().size());
		assertEquals(RST2SaltMapper.NODE_KIND_SEGMENT, struct2.getAnnotation(((RSTImporterProperties) getFixture().getProperties()).getNodeKindName()).getValue());

		SStructure struct3 = this.getSStructureByName("seg3");
		assertNotNull(struct3);
		assertEquals(1, struct3.getAnnotations().size());
		assertEquals(RST2SaltMapper.NODE_KIND_SEGMENT, struct3.getAnnotation(((RSTImporterProperties) getFixture().getProperties()).getNodeKindName()).getValue());

		// SDominanceRelation
		assertNotNull(sDocGraph.getDominanceRelations());
		assertEquals(22, sDocGraph.getDominanceRelations().size());

		assertEquals(1, sDocGraph.getRelations(grp1.getId(), grp2.getId()).size());
		assertTrue((SRelation) sDocGraph.getRelations(grp1.getId(), grp2.getId()).get(0) instanceof SDominanceRelation);
		assertEquals("span", ((SDominanceRelation) (SRelation) sDocGraph.getRelations(grp1.getId(), grp2.getId()).get(0)).getAnnotation(((RSTImporterProperties) getFixture().getProperties()).getRelationName()).getValue());

		assertEquals(1, sDocGraph.getRelations(grp2.getId(), struct1.getId()).size());
		assertTrue((SRelation) sDocGraph.getRelations(grp2.getId(), struct1.getId()).get(0) instanceof SDominanceRelation);
		assertEquals("multinuc", ((SDominanceRelation) (SRelation) sDocGraph.getRelations(grp2.getId(), struct1.getId()).get(0)).getType());
		assertEquals("conjunction", ((SDominanceRelation) (SRelation) sDocGraph.getRelations(grp2.getId(), struct1.getId()).get(0)).getAnnotation(((RSTImporterProperties) getFixture().getProperties()).getRelationName()).getValue());

		assertEquals(1, sDocGraph.getRelations(grp2.getId(), struct2.getId()).size());
		assertTrue((SRelation) sDocGraph.getRelations(grp2.getId(), struct2.getId()).get(0) instanceof SDominanceRelation);
		assertEquals("multinuc", ((SDominanceRelation) (SRelation) sDocGraph.getRelations(grp2.getId(), struct2.getId()).get(0)).getType());
		assertEquals("conjunction", ((SDominanceRelation) (SRelation) sDocGraph.getRelations(grp2.getId(), struct2.getId()).get(0)).getAnnotation(((RSTImporterProperties) getFixture().getProperties()).getRelationName()).getValue());

		assertEquals(1, sDocGraph.getRelations(grp2.getId(), struct3.getId()).size());
		assertTrue((SRelation) sDocGraph.getRelations(grp2.getId(), struct3.getId()).get(0) instanceof SDominanceRelation);
		assertEquals("rst", ((SDominanceRelation) (SRelation) sDocGraph.getRelations(grp2.getId(), struct3.getId()).get(0)).getType());
		assertEquals("nonvolitional-result", ((SDominanceRelation) (SRelation) sDocGraph.getRelations(grp2.getId(), struct3.getId()).get(0)).getAnnotation(((RSTImporterProperties) getFixture().getProperties()).getRelationName()).getValue());

	}
}
