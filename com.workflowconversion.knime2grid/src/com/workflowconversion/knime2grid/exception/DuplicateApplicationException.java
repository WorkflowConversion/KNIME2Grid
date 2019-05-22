package com.workflowconversion.knime2grid.exception;

import com.workflowconversion.knime2grid.resource.Application;
import com.workflowconversion.knime2grid.resource.ResourceProvider;

/**
 * Thrown by instances of {@link ResourceProvider} when an insertion would result in overwriting an already existing
 * application.
 * 
 * @author delagarza
 *
 */
public class DuplicateApplicationException extends ApplicationException {

	private static final long serialVersionUID = -7388349157090017138L;

	/**
	 * @param duplicateApplication
	 *            The duplicate application.
	 */
	public DuplicateApplicationException(final Application duplicateApplication) {
		super("An application with the same name, version, path already exists on this resource. Duplicate: "
				+ duplicateApplication);
	}

}
