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

import junit.framework.TestCase;
import de.hu_berlin.german.korpling.saltnpepper.misc.treetagger.tokenizer.TTTokenizer.TT_LANGUAGES;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.rstModules.RSTImporterProperties;

public class RST2SaltMapperTest extends TestCase{

	protected RSTImporterProperties fixture= null;

	public RSTImporterProperties getFixture() {
		return fixture;
	}

	public void setFixture(RSTImporterProperties fixture) {
		this.fixture = fixture;
	}
	
	public void setUp()
	{
		this.setFixture(new RSTImporterProperties());
	}
	
	/**
	 * Tests the special parameters.
	 */
	public void testProperties()
	{
		Properties props= new Properties();
		this.getFixture().addProperties(props);
		
		assertTrue(this.getFixture().isToTokenize());
		assertNull(this.getFixture().getAbbreviationFolder());
		assertNull(this.getFixture().getLanguage());
		
		this.setFixture(new RSTImporterProperties());
		File abbrFolder= new File("/home/me/abbreviation");
		String lang= TT_LANGUAGES.DE.toString();
		String toTokenize= "no";
		
		props.setProperty(RSTImporterProperties.PROP_ABBFOLDER, abbrFolder.getAbsolutePath());
		props.setProperty(RSTImporterProperties.PROP_LANGUAGE, lang.toString());
		props.setProperty(RSTImporterProperties.PROP_TOKENIZE, toTokenize);
		this.getFixture().addProperties(props);
		
		assertEquals(lang, this.getFixture().getLanguage());
		assertEquals(abbrFolder.getAbsoluteFile(), this.getFixture().getAbbreviationFolder());
		assertEquals(Boolean.FALSE, this.getFixture().isToTokenize());
	}
}
