package de.hu_berlin.german.korpling.saltnpepper.pepperModules.rstModules;

import java.io.File;

import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.PepperModuleProperties;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.PepperModuleProperty;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SAnnotation;

/**
 * Defines the properties to be used for the {@link RSTImporter}. 
 * @author Florian Zipser
 *
 */
public class RSTImporterProperties extends PepperModuleProperties 
{
	public static final String PREFIX="rstImporter.";
	
	/**
	 * Special parameter property to determine if the primary data have to be tokenized. 
	 */
	public final static String PROP_TOKENIZE=PREFIX+"tokenize";
	/**
	 * Special parameter property to set a folder containing abbreviations of several languages. Used for tokenization.
	 */
	public final static String PROP_ABBFOLDER=PREFIX+"abbreviationFolder";
	/**
	 * Special parameter property to determine the language of primary text. Used for tokenization.
	 */
	public final static String PROP_LANGUAGE=PREFIX+"language";
	/**
	 * Name of the property to specify the sName of the {@link SAnnotation} to which the kind of a node (segment or group) is mapped.
	 */
	public final static String PROP_NODE_KIND_NAME=PREFIX+"nodeKindName";
	/**
	 * Name of the property to specify the sName of the SAnnotation to which the type attribute of a node is mapped.
	 */
	public final static String PROP_NODE_TYPE_NAME=PREFIX+"nodeTypeName";
	
	/**
	 * Name of the property to specify the sName of the SAnnotation to which the name attribute of a relation is mapped to
	 */
	public final static String PROP_RELATION_NAME=PREFIX+"relationTypeName";
	
	public RSTImporterProperties()
	{
		this.addProperty(new PepperModuleProperty<String>(PROP_TOKENIZE, String.class, "Determines if the rst data have to be tokenized during import. Possible values are 'yes' and 'no'.", "yes", false));
		this.addProperty(new PepperModuleProperty<String>(PROP_ABBFOLDER, String.class, "In case of data have to be tokenized, one can use abbreviation to customize the quality of tokenization. This property determines a folder containing abbreviation files.", false));
		this.addProperty(new PepperModuleProperty<String>(PROP_LANGUAGE, String.class, "Determines the language of the data. If no language was set by the user, it will be computed automatically.", false));
		this.addProperty(new PepperModuleProperty<String>(PROP_NODE_KIND_NAME, String.class, "Specifies the sName of the SAnnotation to which the kind of a node (segment or group) is mapped to.", "kind", false));
		this.addProperty(new PepperModuleProperty<String>(PROP_NODE_TYPE_NAME, String.class, "Specifies the sName of the SAnnotation to which the type attribute of a node is mapped to.", "type", false));
		this.addProperty(new PepperModuleProperty<String>(PROP_RELATION_NAME, String.class, "Specifies the sName of the SAnnotation to which the name attribute of a relation is mapped to.", "relname", false));
	}
	
	// ================================================ start: tokenizing
	/**
	 * Stores if a tokenization has to be done.
	 */
	private Boolean isToTokenize= null;
	
	/**
	 * Returns if a tokenization has to be done.
	 * @return the isToTokenize
	 */
	public Boolean isToTokenize() {
		if (isToTokenize== null)
		{
			String tokenize= null;
			
			tokenize= ((String)this.getProperty(PROP_TOKENIZE).getValue()); 
			if (tokenize!= null)
			{
				if ("yes".equalsIgnoreCase(tokenize))
					this.isToTokenize=true;
				else if ("no".equalsIgnoreCase(tokenize))
				{
					this.isToTokenize=false;
				}
			}
			else
				this.isToTokenize=true;
		}
		return isToTokenize;
	}
	
// ================================================ end: tokenizing
	
	private File abbreviationFolder=null; 
	/**
	 * Returns the abbreviation folder if one is set.
	 * @return
	 */
	public File getAbbreviationFolder()
	{
		if (abbreviationFolder== null)
		{
			String abbFolder= null;
			abbFolder= ((String)this.getProperty(PROP_ABBFOLDER).getValue());
			if (abbFolder!= null)
				abbreviationFolder= new File(abbFolder);
		}
		return(abbreviationFolder);
	}
	
	private String language= null;
	
	/**
	 * Returns setted language, if one is set.
	 * @return language code
	 */
	public String getLanguage()
	{
		if (language== null)
		{
			String lang= null;
			lang= ((String)this.getProperty(PROP_LANGUAGE).getValue());
			language= lang; 
		}
		return(language);
	}
	/**
	 * Returns the Name of the sName od the SAnnotation to which the kind of a node is mapped to.
	 * @return
	 */
	public String getNodeKindName()
	{
		String kind= ((String)this.getProperty(PROP_NODE_KIND_NAME).getValue());
		return(kind);
	}
	/**
	 * Returns the Name of the sName of the SAnnotation to which the type attribute of a node is mapped to.
	 * @return
	 */
	public String getNodeTypeName()
	{
		String type= ((String)this.getProperty(PROP_NODE_TYPE_NAME).getValue());
		return(type);
	}
	/**
	 * Returns the Name of the property to specify the sName of the SAnnotation to which the name attribute of a relation is mapped to
	 * @return
	 */
	public String getRelationName()
	{
		String relname= ((String)this.getProperty(PROP_RELATION_NAME).getValue());
		return(relname);
	}
}
