package com.workflowconversion.knime2grid.export.node;

import java.io.File;

import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;

import com.workflowconversion.knime2grid.model.Job;

/**
 * Simple interface that declares the methods to convert from a single {@link NativeNodeContainer} to a {@link Job}.
 * 
 * @author delagarza
 * 
 */
public interface NodeContainerConverter {

	/**
	 * Determines if the given node container can be handled.
	 * 
	 * @param nativeNodeContainer
	 *            The node container.
	 * @return {@code true} if this instance can handle the given node.
	 */
	public boolean canHandle(final NativeNodeContainer nativeNodeContainer);

	/**
	 * Converts the given node into a {@link Job}.
	 * 
	 * @param nativeNodeContainer
	 *            The node to convert.
	 * @param workflowManager
	 *            KNIME's Workflow Manager contains the workflow and some converters might need to access other nodes.
	 * @param workingDirectory
	 *            A folder in which logs, debugging information and the like could be placed.
	 * @return The converted {@link Job}.
	 */
	public Job convert(final NativeNodeContainer nativeNodeContainer, final WorkflowManager workflowManager,
			final File workingDirectory) throws Exception;

}
