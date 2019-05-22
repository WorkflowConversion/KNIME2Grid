package com.workflowconversion.knime2grid.exception;

import com.workflowconversion.knime2grid.resource.Application;

/**
 * Exception thrown when an attempt to save an application that doesn't exist is made.
 * 
 * @author delagarza
 *
 */
public class ApplicationNotFoundException extends ApplicationException {

	private static final long serialVersionUID = -7009117876635767771L;

	/**
	 * @param notFoundApplication
	 *            the application whose name, version, path don't exist on a resource.
	 */
	public ApplicationNotFoundException(final Application notFoundApplication) {
		super("The application does not exist in this resource: " + notFoundApplication);
	}
}
