package com.workflowconversion.knime2guse.export;

import org.apache.commons.lang.StringUtils;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.workflowconversion.knime2guse.model.Job;
import com.workflowconversion.knime2guse.model.Job.JobType;

/**
 * Class with utility methods for converting nodes.
 * 
 * @author delagarza
 * 
 */
public class ConverterUtils {

	/**
	 * Copies name, id, description and job type from the source node to the destination job.
	 * 
	 * @param destinationJob
	 * @param sourceNode
	 */
	public static void copyBasicInformation(final Job destinationJob, final NativeNodeContainer sourceNode) {
		destinationJob.setId(sourceNode.getID().toString());
		destinationJob.setName(sourceNode.getName());
		destinationJob.setDescription(getNodeDescription(sourceNode));
		destinationJob.setJobType(getJobType(sourceNode));
	}

	/**
	 * Retrieves node description from the passed node.
	 * 
	 * @param nativeNodeContainer
	 * @return
	 */
	public static String getNodeDescription(final NativeNodeContainer nativeNodeContainer) {
		// we assume that all nodes have some sort of name
		String nodeDescription = nativeNodeContainer.getName();
		if (StringUtils.isBlank(nodeDescription)) {
			// let's try our luck and find for a nicer description
			final Element descriptionElement = nativeNodeContainer.getNode().getFactory().getXMLDescription();
			if (descriptionElement != null) {
				// extract the shortDescription element
				final NodeList nodeList = descriptionElement.getElementsByTagName("shortDescription");
				if (nodeList.getLength() > 0) {
					nodeDescription = nodeList.item(0).getTextContent();
				}
			}
		}
		return nodeDescription;
	}

	/**
	 * Retrieves the job type from the node container. This is a tricky method, since the class of the node is inspected to determine if the job type will be
	 * collector, generator or normal.
	 * 
	 * @param nativeNodeContainer
	 * @return
	 */
	public static JobType getJobType(final NativeNodeContainer nativeNodeContainer) {
		// TODO: check for other types of nodes
		Job.JobType jobType = Job.JobType.Normal;
		if ("com.workflowconversion.knime2guse.nodes.flow.listzip.ListZipLoopEndNodeModel".equals(nativeNodeContainer.getNodeModel().getClass().getName())) {
			jobType = Job.JobType.Collector;
		} else if ("com.workflowconversion.knime2guse.nodes.flow.listzip.ListZipLoopStartNodeModel".equals(nativeNodeContainer.getNodeModel().getClass()
				.getName())) {
			jobType = Job.JobType.Generator;
		}
		return jobType;
	}
}
