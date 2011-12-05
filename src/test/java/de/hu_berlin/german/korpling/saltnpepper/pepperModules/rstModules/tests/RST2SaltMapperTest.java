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

import java.io.File;
import java.util.Properties;

import de.hu_berlin.german.korpling.saltnpepper.misc.treetagger.tokenizer.TTTokenizer.TT_LANGUAGES;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.rstModules.RST2SaltMapper;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.rstModules.RSTImporter;

import junit.framework.TestCase;

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
	}
	
	/**
	 * Tests the special parameters.
	 */
	public void testProperties()
	{
		assertTrue(this.getFixture().isToTokenize());
		assertNull(this.getFixture().getTokenizer().getAbbreviationFolder());
		assertEquals(null, this.getFixture().getTokenizer().getLngLang());
		
		Properties props= new Properties();
		this.getFixture().setProps(props);
		
		File abbrFolder= new File("/home/me/abbreviation");
		TT_LANGUAGES lang= TT_LANGUAGES.DE;
		String toTokenize= "no";
		
		props.setProperty(RSTImporter.PROP_RST_IMPORTER_ABBFOLDER, abbrFolder.getAbsolutePath());
		props.setProperty(RSTImporter.PROP_RST_IMPORTER_LANGUAGE, lang.toString());
		props.setProperty(RSTImporter.PROP_RST_IMPORTER_TOKENIZE, toTokenize);
		this.getFixture().setProps(props);
		
		assertEquals(lang, this.getFixture().getTokenizer().getLngLang());
		assertEquals(abbrFolder.getAbsoluteFile(), this.getFixture().getTokenizer().getAbbreviationFolder());
		assertEquals(Boolean.FALSE, this.getFixture().isToTokenize());
	}
}
