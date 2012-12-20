package de.hu_berlin.german.korpling.saltnpepper.pepperModules.rstModules.tests;

import java.io.File;
import java.util.Properties;

import de.hu_berlin.german.korpling.saltnpepper.misc.treetagger.tokenizer.TTTokenizer.TT_LANGUAGES;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.rstModules.RSTImporterProperties;
import junit.framework.TestCase;

public class RSTImporterPropertiesTest extends TestCase {

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
