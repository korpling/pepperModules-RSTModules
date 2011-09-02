package de.hu_berlin.german.korpling.saltnpepper.pepperModules.rstModules.exceptions;

import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperExceptions.PepperModuleException;


public class RSTExporterException extends PepperModuleException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6942425892766934369L;
	
	
	private static final String STD_MSG= "This exception was thrown by the rst-import module: "; 
	public RSTExporterException()
	{ super(); }
	
    public RSTExporterException(String s)
    { super(STD_MSG+ s); }
    
	public RSTExporterException(String s, Throwable ex)
	{super(STD_MSG+s, ex); }
}
