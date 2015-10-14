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

import org.corpus_tools.pepper.common.PepperConfiguration;
import org.corpus_tools.pepper.impl.PepperImporterImpl;
import org.corpus_tools.pepper.modules.PepperImporter;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.salt.graph.Identifier;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.osgi.service.component.annotations.Component;

import de.hu_berlin.german.korpling.rst.resources.RSTResourceFactory;

/**
 * @author Florian Zipser
 * @version 1.0
 * 
 */
@Component(name = "RSTImporterComponent", factory = "PepperImporterComponentFactory")
public class RSTImporter extends PepperImporterImpl implements PepperImporter {
	public static final String FILE_ENDING_RS3 = "rs3";

	public RSTImporter() {
		super();
		// setting name of module
		setName("RSTImporter");
		setSupplierContact(URI.createURI(PepperConfiguration.EMAIL));
		setSupplierHomepage(URI.createURI("https://github.com/korpling/pepperModules-RSTModules"));
		setDesc("This importer transforms data in rs3 format produced by the RST Tool (see: http://www.wagsoft.com/RSTTool/) to a Salt model.");
		// set list of formats supported by this module
		addSupportedFormat("rs3", "1.0", null);
		// set empty property object to be filled via pepper-framework
		setProperties(new RSTImporterProperties());
		getDocumentEndings().add(FILE_ENDING_RS3);
	}

	/** resourceSet for loading EMF models **/
	private ResourceSet resourceSet = null;

	/**
	 * Creates a mapper of type {@link PAULA2SaltMapper}. {@inheritDoc
	 * PepperModule#createPepperMapper(Identifier)}
	 */
	@Override
	public PepperMapper createPepperMapper(Identifier sElementId) {
		RST2SaltMapper mapper = new RST2SaltMapper();

		// Register XML resource factory
		if (resourceSet == null) {
			resourceSet = new ResourceSetImpl();
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(FILE_ENDING_RS3, new RSTResourceFactory());
		}
		mapper.setResourceSet(resourceSet);

		return (mapper);
	}
}
