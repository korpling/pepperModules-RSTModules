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
package de.hu_berlin.german.korpling.saltnpepper.pepperModules.rstModules;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.osgi.service.component.annotations.Component;

import de.hu_berlin.german.korpling.rst.resources.RSTResourceFactory;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.PepperImporter;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.PepperMapper;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.impl.PepperImporterImpl;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SElementId;

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
	this.name = "RSTImporter";
	// set list of formats supported by this module
	this.addSupportedFormat("rs3", "1.0", null);
	// set empty property object to be filled via pepper-framework
	this.setProperties(new RSTImporterProperties());
	this.getSDocumentEndings().add(FILE_ENDING_RS3);
    }

    /** resourceSet for loading EMF models **/
    private ResourceSet resourceSet = null;

    /**
     * Creates a mapper of type {@link PAULA2SaltMapper}. {@inheritDoc
     * PepperModule#createPepperMapper(SElementId)}
     */
    @Override
    public PepperMapper createPepperMapper(SElementId sElementId) {
	RST2SaltMapper mapper = new RST2SaltMapper();

	// Register XML resource factory
	if (resourceSet == null) {
	    resourceSet = new ResourceSetImpl();
	    resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
		    .put(FILE_ENDING_RS3, new RSTResourceFactory());
	}
	mapper.setResourceSet(resourceSet);

	return (mapper);
    }
}
