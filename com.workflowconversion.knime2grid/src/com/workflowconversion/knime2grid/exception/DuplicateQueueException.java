package com.workflowconversion.knime2grid.exception;

import com.workflowconversion.knime2grid.resource.Queue;

/**
 * Queues are unique in the scope of a resource. This kind of exception is thrown whenever a duplicated queue is added to a resource.
 * 
 * @author delagarza
 *
 */
public class DuplicateQueueException extends ApplicationException {

	private static final long serialVersionUID = -9125533818224219300L;

	/**
	 * @param duplicateApplication
	 *            The duplicate application.
	 */
	public DuplicateQueueException(final Queue duplicateQueue) {
		super("A queue with the same name already exists on this resource. Duplicate: " + duplicateQueue);
	}
}
