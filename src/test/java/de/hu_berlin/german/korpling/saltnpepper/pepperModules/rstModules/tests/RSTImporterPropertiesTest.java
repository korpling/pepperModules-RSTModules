package de.hu_berlin.german.korpling.saltnpepper.pepperModules.rstModules.tests;

import java.util.Properties;

import junit.framework.TestCase;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.rstModules.RSTImporterProperties;

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
		this.getFixture().setPropertyValues(props);
		
		assertTrue(this.getFixture().isToTokenize());
		
		this.setFixture(new RSTImporterProperties());
		String toTokenize= "no";
		
		props.setProperty(RSTImporterProperties.PROP_TOKENIZE, toTokenize);
		this.getFixture().setPropertyValues(props);
		
		assertEquals(Boolean.FALSE, this.getFixture().isToTokenize());
	}
	
	public void test_PROP_SEGMENT_SEPARATOR()
	{
		assertEquals(" ", this.getFixture().getSegementSeparator());
		
		String sep="##";
		Properties props= new Properties();
		props.put(RSTImporterProperties.PROP_SEGMENT_SEPARATOR, sep);
		this.getFixture().setPropertyValues(props);
		assertEquals(sep, this.getFixture().getSegementSeparator());
	}
}
