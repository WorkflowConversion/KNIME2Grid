package com.workflowconversion.knime2guse.export.io;

import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowManager;

import com.genericworkflownodes.knime.parameter.IFileParameter;

public interface SourceConverter {

	/**
	 * Determines if an implementation of this interface can handle the given node container.
	 * 
	 * @param sourceNodeContainer
	 *            The node container to analyze.
	 * @return whether an implementation can handle the given node container.
	 */
	boolean canHandle(final NodeContainer sourceNodeContainer);

	/**
	 * Converts the settings of the passed {@code sourceNodeContainer} into a {@link IFileParameter}.
	 * 
	 * @param sourceNodeContainer
	 *            The node that contains a reference to a file.
	 * @param workflowManager
	 *            The manager in which the node is contained. .
	 * @return The {@link IFileParameter} matching the settings of the passed node.
	 */
	IFileParameter convert(final NodeContainer sourceNodeContainer, final WorkflowManager workflowManager)
			throws Exception;
}
