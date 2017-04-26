package com.workflowconversion.knime2grid.export.workflow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.knime.base.node.io.csvwriter.CSVWriterNodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.workflowconversion.knime2grid.model.Job;

/**
 * Class with utility methods for converting nodes.
 * 
 * @author delagarza
 */
public class ConverterUtils {

	public static final String CSVREADER_CLASS_NAME = "org.knime.base.node.io.csvreader.CSVReaderNodeModel";
	public static final String CSVWRITER_CLASS_NAME = CSVWriterNodeModel.class.getCanonicalName();

	/**
	 * Copies name, id, description and job type from the source node to the destination job.
	 * 
	 * @param destinationJob
	 * @param sourceNode
	 */
	public static void copyBasicInformation(final Job destinationJob, final NativeNodeContainer sourceNode) {
		destinationJob.setId(sourceNode.getID());
		destinationJob.setName(sourceNode.getName());
		destinationJob.setDescription(getNodeDescription(sourceNode));
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
	 * Determines if the node model of the node with the given ID is an instance of the given class.
	 * 
	 * @param workflowManager
	 *            The workflow manager in which the node with the given ID exists.
	 * @param nodeId
	 *            The ID of the node of interest.
	 * @param className
	 *            The class.
	 * @return {@code true} if the node model of the node with the given ID is an instance of the given class.
	 */
	public static boolean nodeModelMatchesClass(final WorkflowManager workflowManager, final NodeID nodeId,
			final String className) {
		final NodeContainer node = workflowManager.getNodeContainer(nodeId);
		return nodeModelMatchesClass(node, className);
	}

	/**
	 * Determines if the node model of the given node container is an instance of the given class.
	 * 
	 * @param nodeContainer
	 *            The node container.
	 * @param className
	 *            The class.
	 * @return {@code true} if the model of the given node is an instance of the given class.
	 */
	public static boolean nodeModelMatchesClass(final NodeContainer nodeContainer, final String className) {
		if (nodeContainer instanceof NativeNodeContainer) {
			final NativeNodeContainer nativeNode = (NativeNodeContainer) nodeContainer;
			if (className.equals(nativeNode.getNodeModel().getClass().getCanonicalName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Creates node settings with the basic variables.
	 * 
	 * @return an empty instance of {@link NodeSettings}.
	 */
	public static NodeSettings createEmptyNodeSettings() {
		final NodeSettings nodeSettings = new NodeSettings("tmp_node_settings");
		// copied from SingleNodeContainer.CFG_VARIABLES
		nodeSettings.addConfig("variables");
		nodeSettings.addConfig(Node.CFG_MISC_SETTINGS);
		nodeSettings.addConfig(Node.CFG_MODEL);
		return nodeSettings;
	}

	/**
	 * Gets the node settings from the passed node container.
	 * 
	 * @param nodeContainer
	 * @param workflowManager
	 * @return
	 * @throws InvalidSettingsException
	 */
	public static NodeSettings getModelSettings(final NodeContainer nodeContainer,
			final WorkflowManager workflowManager) throws InvalidSettingsException {
		final NodeSettings nodeSettings = createEmptyNodeSettings();
		workflowManager.saveNodeSettings(nodeContainer.getID(), nodeSettings);
		return nodeSettings.getNodeSettings(Node.CFG_MODEL);
	}

	/**
	 * Given a string, which could be either an URL or a filepath, this method copies the content into a new file.
	 * 
	 * @param location
	 *            The location (path or URL) of the content.
	 * @return
	 * @throws IOException
	 */
	public static File copyContent(final String location) throws IOException {
		URL url;
		try {
			url = new URL(location);
		} catch (final MalformedURLException e) {
			// it's very probable that this is not a proper URL, we can just ignore this exception for now
			// there's no "pure java" way to check for URL validity :(
			// apache offers one, but we can live without it for now
			url = null;
		}
		if (url != null) {
			// obtain and return the content
			final ReadableByteChannel rbc = Channels.newChannel(url.openStream());
			final File tmpFile = File.createTempFile("knimeconverter", ".download");
			final FileOutputStream fos = new FileOutputStream(tmpFile);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			fos.close();
			return tmpFile;
		} else {
			// it must be a file then, copy its content
			final File tmpFile = File.createTempFile("knimeconverter", ".copied");
			FileUtils.copyFile(new File(location), tmpFile);
			return tmpFile;
		}

	}

	/**
	 * KNIME nodes contain an extra port for flow variables. This method converts a knime port number to the internal
	 * format port number.
	 * 
	 * @param knimePortNr
	 *            The knime port number.
	 * @return The converted port number.
	 */
	public static int convertFromKnimePort(final int knimePortNr) {
		return knimePortNr - 1;
	}

	/**
	 * KNIME nodes contain an extra port for flow variables. This method converts port numbers into knime port numbers.
	 * 
	 * @param portNr
	 *            The port number.
	 * @return The knime port number.
	 */
	public static int convertToKnimePort(final int portNr) {
		return portNr + 1;
	}

	/**
	 * Given a port name and an extension, generates the following relative location
	 * {@code [portName]/[portName].[extension]}.
	 * This simple method helps avoiding clashes in file names that are used as inputs/outputs for jobs.
	 * 
	 * @param portName
	 *            The name of the port needing the file.
	 * @param extension
	 *            The extension of the file.
	 * @return The {@code [portName]/[portName].[extension]} relative location.
	 */
	public static String generateFileNameForExport(final String portName, final String extension) {
		return portName + '/' + portName + '.' + extension;
	}

	/**
	 * Given a port name, an index and an extension, generates the following relative location:
	 * {@code [portName]/[portName]_[index].[extension]}
	 * This simple method helps avoiding clashes in lists of files that are used as inputs/outputs for jobs.
	 * 
	 * @param portName
	 *            The name of the port needing the file.
	 * @param extension
	 *            The extension of the file.
	 * @param index
	 *            The index of the file.
	 * @return The {@code [portName]/[portName]_[index].[extension]} relative location.
	 */
	public static String generateFileNameForExport(final String portName, final String extension, final int index) {
		return portName + '/' + portName + '_' + index + '.' + extension;
	}

}
