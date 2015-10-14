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

import java.util.List;

import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.PepperModuleProperty;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.core.SAnnotation;


/**
 * Defines the properties to be used for the {@link RSTImporter}.
 * 
 * @author Florian Zipser
 *
 */
@SuppressWarnings("serial")
public class RSTImporterProperties extends PepperModuleProperties {
	
	public static final String PREFIX = "rstImporter.";

	/**
	 * Special parameter property to determine if the primary data have to be
	 * tokenized.
	 */
	public final static String PROP_TOKENIZE = PREFIX + "tokenize";
	/**
	 * Name of the property to specify the sName of the SAnnotation to which the
	 * name attribute of a relation is mapped to
	 */
	public final static String PROP_SIMPLE_TOKENIZE = "simpleTokenize";
	/**
	 * Name of the property to specify the sName of the {@link SAnnotation} to
	 * which the kind of a node (segment or group) is mapped.
	 */
	public final static String PROP_NODE_KIND_NAME = PREFIX + "nodeKindName";
	/**
	 * Name of the property to specify the sName of the SAnnotation to which the
	 * type attribute of a node is mapped.
	 */
	public final static String PROP_NODE_TYPE_NAME = PREFIX + "nodeTypeName";

	/**
	 * Name of the property to specify the sName of the SAnnotation to which the
	 * name attribute of a relation is mapped to
	 */
	public final static String PROP_RELATION_NAME = PREFIX + "relationTypeName";
	/**
	 * Name of the property to add a a separator like a blank between the text
	 * of segments, when it is concatenated to the primary text in
	 * {@link STextualDS}. For instance the segment text 'Is' of segment1 and
	 * the segment text 'this' of segment2 will be concatenated to an sText
	 * value 'is'SEPARATOR'this'.
	 */
	public final static String PROP_SEGMENT_SEPARATOR = PREFIX + "segmentSeparator";

	public RSTImporterProperties() {
		this.addProperty(new PepperModuleProperty<String>(PROP_TOKENIZE, String.class, "Determines if the rst data have to be tokenized during import. Possible values are 'yes' and 'no'.", "yes", false));
		this.addProperty(new PepperModuleProperty<String>(PROP_SIMPLE_TOKENIZE, String.class, "Switches on a very simple tokenization. With this property you can pass a list of characters, which should be used as separators to find the borders of tokens e.g. \"' ','.'\" to use a blank and a dot. Note that using this property will overwrite the default TreeTagger tokenizer. This property needs "+PROP_SIMPLE_TOKENIZE+" to be set to true. ", false));
		this.addProperty(new PepperModuleProperty<String>(PROP_NODE_KIND_NAME, String.class, "Specifies the sName of the SAnnotation to which the kind of a node (segment or group) is mapped to.", "kind", false));
		this.addProperty(new PepperModuleProperty<String>(PROP_NODE_TYPE_NAME, String.class, "Specifies the sName of the SAnnotation to which the type attribute of a node is mapped to.", "type", false));
		this.addProperty(new PepperModuleProperty<String>(PROP_RELATION_NAME, String.class, "Specifies the sName of the SAnnotation to which the name attribute of a relation is mapped to.", "relname", false));
		this.addProperty(new PepperModuleProperty<String>(PROP_SEGMENT_SEPARATOR, String.class, "A property to add a a separator like a blank between the text of segments, when it is concatenated to the primary text in STextualDS.For instance the segment text 'Is' of segment1 and the segment text 'this' of segment2 will be concatenated to an sText value 'is'SEPARATOR'this'.", " ", false));
	}

	// ================================================ start: tokenizing
	/**
	 * Stores if a tokenization has to be done.
	 */
	private Boolean isToTokenize = null;

	/**
	 * Returns if a tokenization has to be done.
	 * 
	 * @return the isToTokenize
	 */
	public Boolean isToTokenize() {
		if (isToTokenize == null) {
			String tokenize = null;

			tokenize = ((String) this.getProperty(PROP_TOKENIZE).getValue());
			if (tokenize != null) {
				if ("yes".equalsIgnoreCase(tokenize))
					this.isToTokenize = true;
				else if ("no".equalsIgnoreCase(tokenize)) {
					this.isToTokenize = false;
				}
			} else
				this.isToTokenize = true;
		}
		return isToTokenize;
	}

	/** list of separators in case of simple tokenization is used **/
	private List<Character> simpleTokSeparators = null;

	/**
	 * Returns a list of separators in case of simple tokenization is used.
	 * 
	 * @return the isToTokenize
	 */
	public List<Character> getSimpleTokenizationSeparators() {
		if (simpleTokSeparators == null) {
			String seps = ((String) this.getProperty(PROP_SIMPLE_TOKENIZE).getValue());
			if (seps != null) {
				simpleTokSeparators= this.stringToCharList(seps);
			}
		}
		return simpleTokSeparators;
	}

	// ================================================ end: tokenizing

	/**
	 * Returns the Name of the sName od the SAnnotation to which the kind of a
	 * node is mapped to.
	 * 
	 * @return
	 */
	public String getNodeKindName() {
		String kind = ((String) this.getProperty(PROP_NODE_KIND_NAME).getValue());
		return (kind);
	}

	/**
	 * Returns the Name of the sName of the SAnnotation to which the type
	 * attribute of a node is mapped to.
	 * 
	 * @return
	 */
	public String getNodeTypeName() {
		String type = ((String) this.getProperty(PROP_NODE_TYPE_NAME).getValue());
		return (type);
	}

	/**
	 * Returns the Name of the property to specify the sName of the SAnnotation
	 * to which the name attribute of a relation is mapped to
	 * 
	 * @return
	 */
	public String getRelationName() {
		String relname = ((String) this.getProperty(PROP_RELATION_NAME).getValue());
		return (relname);
	}

	/**
	 * Returns the Name of the property to specify the sName of the SAnnotation
	 * to which the name attribute of a relation is mapped to
	 * 
	 * @return
	 */
	public String getSegmentSeparator() {
		String sep = ((String) this.getProperty(PROP_SEGMENT_SEPARATOR).getValue());
		return (sep);
	}
}
