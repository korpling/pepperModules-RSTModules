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

import org.eclipse.emf.common.util.URI;

import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperFW.PepperFWFactory;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.FormatDefinition;
import de.hu_berlin.german.korpling.saltnpepper.pepper.testSuite.moduleTests.PepperImporterTest;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.rstModules.RSTImporter;

public class RSTImporterTest extends PepperImporterTest 
{
	
	public void setUp()
	{
		this.setFixture(new RSTImporter());
		FormatDefinition formatDef= PepperFWFactory.eINSTANCE.createFormatDefinition();
		formatDef.setFormatName("rs3");
		formatDef.setFormatVersion("1.0");
		this.supportedFormatsCheck.add(formatDef);
		
		File tmpFolder= new File(System.getProperty("java.io.tmpdir")+"/"+System.nanoTime());
		if (!tmpFolder.exists())
			tmpFolder.mkdirs();		
		this.setTemprorariesURI(URI.createFileURI(tmpFolder.getAbsolutePath()));
		File resFolder= new File("./src/main/resources");
		this.setResourcesURI(URI.createFileURI(resFolder.getAbsolutePath()));
	}
}
