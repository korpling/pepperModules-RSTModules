package de.hu_berlin.german.korpling.saltnpepper.pepperModules.rstModules.exceptions;

import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperExceptions.PepperModuleException;

public class RSTImporterException extends PepperModuleException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6942425892786934369L;
	
	
	private static final String STD_MSG= "This exception was thrown by the rst-import module: "; 
	public RSTImporterException()
	{ super(); }
	
    public RSTImporterException(String s)
    { super(STD_MSG+ s); }
    
	public RSTImporterException(String s, Throwable ex)
	{super(STD_MSG+s, ex); }
}
