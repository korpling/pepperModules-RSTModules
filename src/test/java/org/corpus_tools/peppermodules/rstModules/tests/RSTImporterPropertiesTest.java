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
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.corpus_tools.peppermodules.rstModules.RSTImporterProperties;
import org.junit.Before;
import org.junit.Test;

public class RSTImporterPropertiesTest {

	protected RSTImporterProperties fixture = null;

	public RSTImporterProperties getFixture() {
		return fixture;
	}

	public void setFixture(RSTImporterProperties fixture) {
		this.fixture = fixture;
	}

	@Before
	public void setUp() {
		this.setFixture(new RSTImporterProperties());
	}

	/**
	 * Tests the special parameters.
	 */
	@Test
	public void testProperties() {
		Properties props = new Properties();
		getFixture().setPropertyValues(props);

		assertTrue(getFixture().isToTokenize());

		this.setFixture(new RSTImporterProperties());
		String toTokenize = "no";

		props.setProperty(RSTImporterProperties.PROP_TOKENIZE, toTokenize);
		getFixture().setPropertyValues(props);

		assertEquals(Boolean.FALSE, getFixture().isToTokenize());
	}

	@Test
	public void test_PROP_SEGMENT_SEPARATOR() {
		assertEquals(" ", getFixture().getSegmentSeparator());

		String sep = "##";
		Properties props = new Properties();
		props.put(RSTImporterProperties.PROP_SEGMENT_SEPARATOR, sep);
		getFixture().setPropertyValues(props);
		assertEquals(sep, getFixture().getSegmentSeparator());
	}
	
	@Test
	public void test_PROP_SIMPLE_TOKENIZE_1(){
		assertEquals(null, getFixture().getSimpleTokenizationSeparators());
	}
	@Test
	public void test_PROP_SIMPLE_TOKENIZE_2(){
		String sep = "' ', '.'";
		Properties props = new Properties();
		props.put(RSTImporterProperties.PROP_SIMPLE_TOKENIZE, sep);
		getFixture().setPropertyValues(props);
		
		assertEquals(2, getFixture().getSimpleTokenizationSeparators().size());
		assertEquals(new Character(' '), getFixture().getSimpleTokenizationSeparators().get(0));
		assertEquals(new Character('.'), getFixture().getSimpleTokenizationSeparators().get(1));
	}
	@Test
	public void test_PROP_SIMPLE_TOKENIZE_3(){
		String sep = "' ', '\\'', ',', '\\\\'";
		Properties props = new Properties();
		props.put(RSTImporterProperties.PROP_SIMPLE_TOKENIZE, sep);
		getFixture().setPropertyValues(props);
		
		assertEquals(4, getFixture().getSimpleTokenizationSeparators().size());
		assertEquals(new Character(' '), getFixture().getSimpleTokenizationSeparators().get(0));
		assertEquals(new Character('\''), getFixture().getSimpleTokenizationSeparators().get(1));
		assertEquals(new Character(','), getFixture().getSimpleTokenizationSeparators().get(2));
		assertEquals(new Character('\\'), getFixture().getSimpleTokenizationSeparators().get(3));
	}
}
