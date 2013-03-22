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

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.log.LogService;

import de.hu_berlin.german.korpling.rst.RSTDocument;
import de.hu_berlin.german.korpling.rst.resources.RSTResourceFactory;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperExceptions.PepperFWException;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperExceptions.PepperModuleException;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.PepperImporter;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.impl.PepperImporterImpl;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.rstModules.exceptions.RSTImporterException;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpus;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpusGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SElementId;

/**
 * @author Florian Zipser
 * @version 1.0
 *
 */
@Component(name="RSTImporterComponent", factory="PepperImporterComponentFactory")
public class RSTImporter extends PepperImporterImpl implements PepperImporter
{	
	public RSTImporter()
	{
		super();
		//setting name of module
		this.name= "RSTImporter";
		//set list of formats supported by this module
		this.addSupportedFormat("rs3", "1.0", null);
		//set empty property object to be filled via pepper-framework 
		this.setProperties(new RSTImporterProperties());
	}
	
	//===================================== start: performance variables
	/**
	 * Measured time which is needed to import the corpus structure. 
	 */
	private Long timeImportSCorpusStructure= 0l;
	/**
	 * Measured total time which is needed to import the document corpus structure. 
	 */
	private Long totalTimeImportSDocumentStructure= 0l;
	/**
	 * Measured time which is needed to load all documents into exmaralda model.. 
	 */
	private Long totalTimeToLoadDocument= 0l;
	/**
	 * Measured time which is needed to map all documents to salt. 
	 */
	private Long totalTimeToMapDocument= 0l;
//===================================== end: performance variables
	
//===================================== start: thread number
	/**
	 * Defines the number of processes which can maximal work in parallel for importing documents.
	 * Means the number of parallel imported documents. Default value is 5.
	 */
	private Integer numOfParallelDocuments= 5;
	/**
	 * Sets the number of processes which can maximal work in parallel for importing documents.
	 * Means the number of parallel imported documents.
	 * @param numOfParallelDocuments the numOfParallelDocuments to set
	 */
	public void setNumOfParallelDocuments(Integer numOfParallelDocuments) {
		this.numOfParallelDocuments = numOfParallelDocuments;
	}

	/**
	 * Returns the number of processes which can maximal work in parallel for importing documents.
	 * Means the number of parallel imported documents.
	 * @return the numOfParallelDocuments
	 */
	public Integer getNumOfParallelDocuments() {
		return numOfParallelDocuments;
	}	
	
	public static final String PROP_NUM_OF_PARALLEL_DOCUMENTS="rstImporter.numOfParallelDocuments";
//===================================== start: thread number
	
// ========================== start: flagging for parallel running	
	/**
	 * If true, RSTImporter imports documents in parallel.
	 */
	private Boolean RUN_IN_PARALLEL= true;
	/**
	 * @param rUN_IN_PARALLEL the rUN_IN_PARALLEL to set
	 */
	public void setRUN_IN_PARALLEL(Boolean rUN_IN_PARALLEL) {
		RUN_IN_PARALLEL = rUN_IN_PARALLEL;
	}

	/**
	 * @return the rUN_IN_PARALLEL
	 */
	public Boolean getRUN_IN_PARALLEL() {
		return RUN_IN_PARALLEL;
	}
	
	/**
	 * Identifier of properties which contains the maximal number of parallel processed documents. 
	 */
	public static final String PROP_RUN_IN_PARALLEL="rstImporter.runInParallel";
// ========================== end: flagging for parallel running
	
	/**
	 * Stores relation between documents and their resource 
	 */
	private Map<SElementId, URI> sDocumentResourceTable= null;
	
// ========================== start: extract corpus-path	
	/**
	 * Stores the endings which are used for RST-files
	 */
	private EList<String> RST_FILE_ENDINGS= new BasicEList<String>();
	{
		RST_FILE_ENDINGS.add("rs3");
	}
	
	@Override
	public void importCorpusStructure(SCorpusGraph corpusGraph)
			throws RSTImporterException 
	{
		this.timeImportSCorpusStructure= System.nanoTime();
		this.setSCorpusGraph(corpusGraph);
		if (this.getSCorpusGraph()== null)
			throw new RSTImporterException(this.name+": Cannot start with importing corpus, because salt project isn�t set.");
		
		if (this.getCorpusDefinition()== null)
			throw new RSTImporterException(this.name+": Cannot start with importing corpus, because no corpus definition to import is given.");
		if (this.getCorpusDefinition().getCorpusPath()== null)
			throw new RSTImporterException(this.name+": Cannot start with importing corpus, because the path of given corpus definition is null.");
		
		if (this.getCorpusDefinition().getCorpusPath().isFile())
		{
			this.sDocumentResourceTable= new Hashtable<SElementId, URI>();
			//clean uri in corpus path (if it is a folder and ends with/, / has to be removed)
			if (	(this.getCorpusDefinition().getCorpusPath().toFileString().endsWith("/")) || 
					(this.getCorpusDefinition().getCorpusPath().toFileString().endsWith("\\")))
			{
				this.getCorpusDefinition().setCorpusPath(this.getCorpusDefinition().getCorpusPath().trimSegments(1));
			}
			try {
				this.sDocumentResourceTable= this.createCorpusStructure(this.getCorpusDefinition().getCorpusPath(), null, RST_FILE_ENDINGS);
			} catch (IOException e) {
				throw new RSTImporterException(this.name+": Cannot start with importing corpus, because saome exception occurs: ",e);
			}
			finally
			{
				timeImportSCorpusStructure= System.nanoTime()- timeImportSCorpusStructure;
			}
		}	
	}
	
// ========================== end: extract corpus-path

	/**
	 * ThreadPool
	 */
	private ExecutorService executorService= null;
	
	/**
	 * 
	 */
	@Override
	public void start() throws PepperModuleException
	{
		this.mapperRunners= new BasicEList<MapperRunner>();
		{//initialize ThreadPool
			executorService= Executors.newFixedThreadPool(this.getNumOfParallelDocuments());
		}//initialize ThreadPool
		
		boolean isStart= true;
		SElementId sElementId= null;
		while ((isStart) || (sElementId!= null))
		{	
			isStart= false;
			sElementId= this.getPepperModuleController().get();
			if (sElementId== null)
				break;
			
			//call for using push-method
			this.start(sElementId);
		}	
		
		for (MapperRunner mapperRunner: this.mapperRunners)
		{
			mapperRunner.waitUntilFinish();
		}
		this.end();
	}
	
	/**
	 * List of all used mapper runners.
	 */
	private EList<MapperRunner> mapperRunners= null;
	
	/**
	 * This method is called by method start() of superclass PepperImporter, if the method was not overridden
	 * by the current class. If this is not the case, this method will be called for every document which has
	 * to be processed.
	 * @param sElementId the id value for the current document or corpus to process  
	 */
	@Override
	public void start(SElementId sElementId) throws PepperModuleException 
	{
		if (	(sElementId!= null) &&
				(sElementId.getSIdentifiableElement()!= null) &&
				((sElementId.getSIdentifiableElement() instanceof SDocument) ||
				((sElementId.getSIdentifiableElement() instanceof SCorpus))))
		{//only if given sElementId belongs to an object of type SDocument or SCorpus	
			if (sElementId.getSIdentifiableElement() instanceof SCorpus)
			{//mapping SCorpus	
			}//mapping SCorpus
			if (sElementId.getSIdentifiableElement() instanceof SDocument)
			{//mapping SDocument
				SDocument sDocument= (SDocument) sElementId.getSIdentifiableElement();
				MapperRunner mapperRunner= new MapperRunner();
				{//configure mapper and mapper runner
					RST2SaltMapper mapper= new RST2SaltMapper();
					mapperRunner.mapper= mapper;
					mapperRunner.sDocument= sDocument;
					//set properties
					mapper.setProps((RSTImporterProperties)this.getProperties());

					mapper.setCurrentSDocument(sDocument);
					mapper.setLogService(this.getLogService());
				}//configure mapper and mapper runner
				
				if (this.getRUN_IN_PARALLEL())
				{//run import in parallel	
					this.mapperRunners.add(mapperRunner);
					this.executorService.execute(mapperRunner);
				}//run import in parallel
				else 
				{//do not run import in parallel
					mapperRunner.start();
				}//do not run import in parallel
			}//mapping SDocument
		}//only if given sElementId belongs to an object of type SDocument or SCorpus
	}
	
	/**
	 * This class is a container for running RSTMappings in parallel.
	 * @author Administrator
	 *
	 */
	private class MapperRunner implements java.lang.Runnable
	{
		public SDocument sDocument= null;
		RST2SaltMapper mapper= null;
		
		/**
		 * Lock to lock await and signal methods.
		 */
		protected Lock lock= new ReentrantLock();
		
		/**
		 * Flag wich says, if mapperRunner has started and finished
		 */
		private Boolean isFinished= false;
		
		/**
		 * If condition is achieved a new SDocument can be created.
		 */
		private Condition finishCondition=lock.newCondition();
		
		public void waitUntilFinish()
		{
			lock.lock();
			try {
				if (!isFinished)
					finishCondition.await();
			} catch (InterruptedException e) {
				throw new PepperFWException(e.getMessage());
			}
			lock.unlock();
		}
		
		public void run() 
		{
			start();
		}
		
		/**
		 * starts Mapping of RST data
		 */
		public void start()
		{
			if (mapper== null)
				throw new RSTImporterException("BUG: Cannot start import, because the mapper is null.");
			if (sDocument== null)
				throw new RSTImporterException("BUG: Cannot start import, because no SDocument object is given.");
			RSTDocument rstDocument= null;
			{//getting rst-document-path
				URI rstDoc= sDocumentResourceTable.get(sDocument.getSElementId());
				if (rstDoc== null)
					throw new RSTImporterException("BUG: Cannot start import, no rst-document-path was found for SDocument '"+sDocument.getSElementId()+"'.");
				{//loading rst model
					Long timeToLoadSDocumentStructure= System.nanoTime();
					// create resource set and resource 
					ResourceSet resourceSet = new ResourceSetImpl();
	
					// Register XML resource factory
					resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("rs3",new RSTResourceFactory());
	
					//load resource 
					Resource resource = resourceSet.createResource(rstDoc);
					
					if (resource== null)
						throw new RSTImporterException("Cannot load the exmaralda file: "+ rstDoc+", becuase the resource is null.");
					try {
						resource.load(null);
					} catch (IOException e) 
					{
						throw new RSTImporterException("Cannot load the exmaralda file: "+ rstDoc+".", e);
					}
					
					rstDocument= (RSTDocument) resource.getContents().get(0);	
					totalTimeToLoadDocument= totalTimeToLoadDocument + (System.nanoTime()- timeToLoadSDocumentStructure);
				}//loading rst model
				mapper.setCurrentRSTDocument(rstDocument);
				mapper.setCurrentRSTDocumentURI(rstDoc);
			}//getting rst-document-path
			try 
			{
				if (mapper.getCurrentRSTDocument()!= null)
				{	
					mapper.mapRSTDocument2SDocument();
					getPepperModuleController().put(this.sDocument.getSElementId());
				}
			}catch (Exception e)
			{
				if (getLogService()!= null)
				{
					getLogService().log(LogService.LOG_WARNING, "Cannot import the SDocument '"+sDocument.getSName()+"'. The reason is: "+e);
					e.printStackTrace();
				}
				getPepperModuleController().finish(this.sDocument.getSElementId());
			}
			this.lock.lock();
			this.isFinished= true;
			this.finishCondition.signal();
			this.lock.unlock();
		}
	}
	
	/**
	 * This method is called by method start() of super class PepperModule. If you do not implement
	 * this method, it will call start(sElementId), for all super corpora in current SaltProject. The
	 * sElementId refers to one of the super corpora. 
	 */
	@Override
	public void end() throws PepperModuleException
	{
		super.end();
		if (this.getLogService()!= null)
		{	
			StringBuffer msg= new StringBuffer();
			msg.append("needed time of "+this.getName()+":\n");
			msg.append("\t time to import whole corpus-structure:\t\t\t\t"+ timeImportSCorpusStructure / 1000000+"\n");
			msg.append("\t total time to import whole document-structure:\t\t"+ totalTimeImportSDocumentStructure / 1000000+"\n");
			msg.append("\t total time to load whole document-structure:\t\t\t"+ totalTimeToLoadDocument / 1000000+"\n");
			msg.append("\t total time to map whole document-structure to salt:\t"+ totalTimeToMapDocument / 1000000+"\n");
			this.getLogService().log(LogService.LOG_DEBUG, msg.toString());
		}
	}
}
