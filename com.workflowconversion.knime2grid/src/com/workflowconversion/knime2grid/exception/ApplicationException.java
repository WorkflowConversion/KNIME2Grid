package com.workflowconversion.knime2grid.exception;

/**
 * Generic exception.
 * 
 * @author delagarza
 *
 */
public class ApplicationException extends RuntimeException {

	private static final long serialVersionUID = 8742655203645104243L;

	/**
	 * @param message
	 */
	public ApplicationException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public ApplicationException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public ApplicationException(String message, Throwable cause) {
		super(message, cause);
	}
}
