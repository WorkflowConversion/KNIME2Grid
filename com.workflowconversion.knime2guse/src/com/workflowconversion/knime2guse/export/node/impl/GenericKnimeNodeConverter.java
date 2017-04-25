package com.workflowconversion.knime2guse.export.node.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.knime.core.data.uri.IURIPortObject;
import org.knime.core.data.uri.URIContent;
import org.knime.core.data.uri.URIPortObjectSpec;
import org.knime.core.node.Node;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;

import com.genericworkflownodes.knime.commandline.CommandLineElement;
import com.genericworkflownodes.knime.commandline.ParametrizedCommandLineElement;
import com.genericworkflownodes.knime.commandline.impl.CommandLineCTDFile;
import com.genericworkflownodes.knime.commandline.impl.CommandLineFile;
import com.genericworkflownodes.knime.config.INodeConfiguration;
import com.genericworkflownodes.knime.config.reader.CTDConfigurationReader;
import com.genericworkflownodes.knime.config.reader.InvalidCTDFileException;
import com.genericworkflownodes.knime.config.writer.CTDConfigurationWriter;
import com.genericworkflownodes.knime.generic_node.GenericKnimeNodeModel;
import com.genericworkflownodes.knime.parameter.FileListParameter;
import com.genericworkflownodes.knime.parameter.FileParameter;
import com.genericworkflownodes.knime.parameter.IFileParameter;
import com.genericworkflownodes.knime.parameter.Parameter;
import com.genericworkflownodes.knime.port.Port;
import com.workflowconversion.knime2guse.export.node.NodeContainerConverter;
import com.workflowconversion.knime2guse.export.workflow.ConverterUtils;
import com.workflowconversion.knime2guse.model.ConnectionType;
import com.workflowconversion.knime2guse.model.Input;
import com.workflowconversion.knime2guse.model.Job;
import com.workflowconversion.knime2guse.model.Output;

/**
 * Converts nodes that were imported into KNIME via GKN.
 * 
 * @author delagarza
 */
public class GenericKnimeNodeConverter implements NodeContainerConverter {

	@Override
	public boolean canHandle(final NativeNodeContainer nativeNodeContainer) {
		return GenericKnimeNodeModel.class.isAssignableFrom(nativeNodeContainer.getNodeModel().getClass());
	}

	@Override
	public Job convert(final NativeNodeContainer nativeNodeContainer, final WorkflowManager workflowManager,
			final File workingDirectory) throws Exception {
		// we first need the CTD
		final GenericKnimeNodeModel gknModel = (GenericKnimeNodeModel) (nativeNodeContainer).getNodeModel();
		final INodeConfiguration nodeConfiguration = gknModel.getNodeConfiguration();

		// now build the job
		final Job job = new Job();
		ConverterUtils.copyBasicInformation(job, nativeNodeContainer);
		job.setExecutableName(nodeConfiguration.getExecutableName());
		job.setExecutablePath(nodeConfiguration.getExecutablePath());

		// now, we need to know how to actually execute this node on a command line environment
		// so we need to access the command generator... however, at this point, there is no guarantee
		// that all jobs have been converted so in case of inputs whose source is another job, it might
		// be too early to convert... so right now, collect as much information as we can about the job
		final Collection<CommandLineElement> commandLineElements = gknModel.getCommandLine(workingDirectory);
		job.setCommandLine(commandLineElements);

		// input/output ports are also handled as parameters in GKN, so we need to keep track of
		// the ports that we've processed
		final Set<String> processedPortNames = new TreeSet<String>();

		// we know that all inputs and outputs of GKNs are URLs, so this simplifies the conversion

		// go through all connections to create inputs/outputs for this job
		for (final ConnectionContainer connectionContainer : workflowManager
				.getIncomingConnectionsFor(nativeNodeContainer.getID())) {
			final NodeID sourceNodeId = connectionContainer.getSource();
			final int destPortNr = connectionContainer.getDestPort();
			final Port destPort = nodeConfiguration.getInputPorts()
					.get(ConverterUtils.convertFromKnimePort(destPortNr));
			final Input input = new Input();
			input.setSourceId(sourceNodeId);
			input.setOriginalPortNr(destPortNr);
			input.setName(destPort.getName());
			input.setMultiFile(destPort.isMultiFile());
			job.addInput(input);

			processedPortNames.add(destPort.getName());
		}

		for (final ConnectionContainer connectionContainer : workflowManager
				.getOutgoingConnectionsFor(nativeNodeContainer.getID())) {
			final int sourcePortNr = connectionContainer.getSourcePort();
			if (job.getOutputByOriginalPortNr(sourcePortNr) == null) {
				// first time we see the output, we need to add it
				final Port sourcePort = nodeConfiguration.getOutputPorts()
						.get(ConverterUtils.convertFromKnimePort(sourcePortNr));
				final Output newOutput = new Output();
				newOutput.setMultiFile(sourcePort.isMultiFile());
				newOutput.setName(sourcePort.getName());
				newOutput.setOriginalPortNr(sourcePortNr);
				job.addOutput(newOutput);
				processedPortNames.add(sourcePort.getName());
			}
		}

		// process all non-input/non-output parameters AND the CTD, if any. 
		// this must be AFTER inputs/outputs have been processed, because adding a CTD input might 
		// modify the indices of the inputs.
		boolean ctdFound = false;
		for (final CommandLineElement element : commandLineElements) {
			if (element instanceof CommandLineCTDFile) {
				// create an input for the ctd file
				if (ctdFound) {
					throw new IllegalStateException(
							"This job already has a CTD file. Only one CTD file per job is allowed.");
				}
				associateAndFixPathsInCTD(workflowManager, (CommandLineCTDFile) element, job, nodeConfiguration,
						nativeNodeContainer);
				ctdFound = true;
			} else if (element instanceof ParametrizedCommandLineElement
					&& !processedPortNames.contains(element.getKey())) {
				// we need to process only true parameters, not flags or option identifiers
				final Parameter<?> parameter = nodeConfiguration.getParameter(element.getKey());
				job.addParameter(element.getKey(), parameter.getStringRep());
			} else if (element instanceof CommandLineFile) {
				// fix the given paths for export
				final CommandLineFile commandLineFile = (CommandLineFile) element;
				final FileParameter fileParameter = commandLineFile.getValue();
				final String extension = FilenameUtils.getExtension(fileParameter.getValue());
				final String fixedPath;
				if (commandLineFile.isSequenced()) {
					fixedPath = ConverterUtils.generateFileNameForExport(fileParameter.getKey(), extension);
				} else {
					fixedPath = ConverterUtils.generateFileNameForExport(fileParameter.getKey(), extension,
							commandLineFile.getSequenceNumber());
				}
				fileParameter.setValue(fixedPath);
			}
		}

		return job;
	}

	// associates the passed element to the given job
	// it also "fixes" the inputs/outputs inside the CTD file
	private void associateAndFixPathsInCTD(final WorkflowManager workflowManager, final CommandLineCTDFile element,
			final Job job, final INodeConfiguration nodeConfiguration, final NativeNodeContainer nativeNodeContainer)
					throws IOException, InvalidCTDFileException {
		final Input ctdInput = new Input();
		ctdInput.setName("ctd-input");
		ctdInput.setConnectionType(ConnectionType.UserProvided);
		// write out the ctd into a file and fix the inputs and outputs (i.e., remove the paths from the files)
		// we should not modify the node configuration because this will affect future runs of the workflow!
		final INodeConfiguration clonedNodeConfiguration = cloneNodeConfiguration(nodeConfiguration);
		fixFilenamesInConfiguration(workflowManager, clonedNodeConfiguration, nativeNodeContainer);
		// set the fixed CTD as data for this input
		final FileParameter ctdFileParameter = new FileParameter("ctdfile",
				dumpConfiguration(clonedNodeConfiguration).getCanonicalPath());
		ctdInput.setData(ctdFileParameter);
		job.addInput(ctdInput);
		// fix the command line element!
		element.setValue(ctdFileParameter);
	}

	private File dumpConfiguration(final INodeConfiguration nodeConfiguration) throws IOException {
		final File ctdFile = File.createTempFile("ctdfile", ".ctd");
		final CTDConfigurationWriter writer = new CTDConfigurationWriter(ctdFile);
		writer.write(nodeConfiguration);
		return ctdFile;
	}

	private INodeConfiguration cloneNodeConfiguration(final INodeConfiguration nodeConfiguration)
			throws InvalidCTDFileException, IOException {
		final File ctdFile = dumpConfiguration(nodeConfiguration);
		final CTDConfigurationReader reader = new CTDConfigurationReader();
		return reader.read(new FileInputStream(ctdFile));
	}

	// the inputs/outputs of the original CTD contain absolute filenames... this method will "fix" those
	// names and transform each filename to a relative path and will also include the name of the
	// input/output port, for instance:
	// original value for input named "ligands": /var/etc/tmp9237.sdf
	// changed value for input named "ligands": ligands.sdf
	private void fixFilenamesInConfiguration(final WorkflowManager workflowManager,
			final INodeConfiguration nodeConfiguration, final NativeNodeContainer nativeNodeContainer) {
		// [in/out]_{portname}, eg:
		// in_sequence, out_result
		// since an incoming and an outgoing port can have the same names, we need to be able to distinguish them
		final Collection<PortWrapper> connectedIncomingPorts = new LinkedList<PortWrapper>();
		final Collection<PortWrapper> connectedOutgoingPorts = new LinkedList<PortWrapper>();
		// we first need to figure out which ports are connected and therefore, in use
		extractConnectedPorts(workflowManager, nativeNodeContainer, nodeConfiguration, connectedIncomingPorts,
				connectedOutgoingPorts);
		// keep track of the ports that we've processed so we only process each port only once
		final Set<String> processedInPortNames = new TreeSet<String>();
		final Set<String> processedOutPortNames = new TreeSet<String>();
		// first, try the happy path, which is: ports already contain the info we need
		fixPathsFromAssociatedParameters(workflowManager, nativeNodeContainer, nodeConfiguration,
				connectedIncomingPorts, processedInPortNames);
		fixPathsFromAssociatedParameters(workflowManager, nativeNodeContainer, nodeConfiguration,
				connectedOutgoingPorts, processedOutPortNames);
		// now, for the input ports, get the PortObject from the source port
		fixIncomingPortsFromSourcePorts(workflowManager, nativeNodeContainer, nodeConfiguration, connectedIncomingPorts,
				processedInPortNames);
		// for the output ports, just process the associated PortObject
		fixOutgoingPortsUsingPortObjects(workflowManager, nativeNodeContainer, nodeConfiguration,
				connectedOutgoingPorts, processedOutPortNames);
	}

	// fills the passed collections with the actually used incoming/outgoing ports
	private void extractConnectedPorts(final WorkflowManager workflowManager,
			final NativeNodeContainer nativeNodeContainer, final INodeConfiguration nodeConfiguration,
			final Collection<PortWrapper> connectedIncomingPorts,
			final Collection<PortWrapper> connectedOutgoingPorts) {
		final NodeID nodeId = nativeNodeContainer.getID();
		for (final ConnectionContainer connection : workflowManager.getIncomingConnectionsFor(nodeId)) {
			// adjust for flow variable port
			final int portNr = ConverterUtils.convertFromKnimePort(connection.getDestPort());
			connectedIncomingPorts.add(new PortWrapper(nodeConfiguration.getInputPorts().get(portNr), portNr));
		}
		for (final ConnectionContainer connection : workflowManager.getOutgoingConnectionsFor(nodeId)) {
			// adjust for flow variable port
			final int portNr = ConverterUtils.convertFromKnimePort(connection.getSourcePort());
			connectedOutgoingPorts.add(new PortWrapper(nodeConfiguration.getOutputPorts().get(portNr), portNr));
		}
	}

	private void fixPathsFromAssociatedParameters(final WorkflowManager workflowManager,
			final NativeNodeContainer nativeNodeContainer, final INodeConfiguration nodeConfiguration,
			final Collection<PortWrapper> portWrappers, final Set<String> processedPorts) {

		for (final PortWrapper portWrapper : portWrappers) {
			final Port port = portWrapper.port;
			final Parameter<?> associatedParameter = nodeConfiguration.getParameter(port.getName());
			final String parameterName = port.getName();
			if (!processedPorts.contains(parameterName)) {
				if (associatedParameter instanceof FileParameter && !port.isMultiFile()) {
					final String filename = ((FileParameter) associatedParameter).getValue();
					if (StringUtils.isNotBlank(filename)) {
						final String extension = FilenameUtils.getExtension(filename);
						((FileParameter) associatedParameter)
								.setValue(ConverterUtils.generateFileNameForExport(parameterName, extension));
						processedPorts.add(parameterName);
					}
				} else if (associatedParameter instanceof FileListParameter && port.isMultiFile()) {
					if (associatedParameter.getValue() != null) {
						final List<String> fixedFilenames = new LinkedList<String>();
						if (!fixedFilenames.isEmpty()) {
							int fileNumber = 0;
							for (final String filename : ((FileListParameter) associatedParameter).getStrings()) {
								final String extension = FilenameUtils.getExtension(filename);
								fixedFilenames.add(
										ConverterUtils.generateFileNameForExport(parameterName, extension, fileNumber));
								fileNumber++;
							}
						}
						((FileListParameter) associatedParameter).setValue(fixedFilenames);
						processedPorts.add(parameterName);
					}
				} else {
					throw new RuntimeException(
							"Invalid association between parameters and input files. This is probably a bug, please report it.");
				}
			}
		}
	}

	private void fixIncomingPortsFromSourcePorts(final WorkflowManager workflowManager,
			final NativeNodeContainer nativeNodeContainer, final INodeConfiguration nodeConfiguration,
			final Collection<PortWrapper> incomingPorts, final Set<String> processedPortNames) {
		for (final PortWrapper portWrapper : incomingPorts) {
			final String key = portWrapper.port.getName();
			if (!processedPortNames.contains(key)) {
				final int portNr = ConverterUtils.convertToKnimePort(portWrapper.portNr);
				final ConnectionContainer connection = workflowManager
						.getIncomingConnectionFor(nativeNodeContainer.getID(), portNr);
				final Node sourceNode = ((NativeNodeContainer) workflowManager.getNodeContainer(connection.getSource()))
						.getNode();
				transferToNodeConfiguration(nodeConfiguration, sourceNode, connection.getSourcePort(),
						portWrapper.port);
				processedPortNames.add(key);
			}
		}
	}

	private void fixOutgoingPortsUsingPortObjects(final WorkflowManager workflowManager,
			final NativeNodeContainer nativeNodeContainer, final INodeConfiguration nodeConfiguration,
			final Collection<PortWrapper> outgoingPorts, final Set<String> processedPortNames) {
		for (final PortWrapper portWrapper : outgoingPorts) {
			final String key = portWrapper.port.getName();
			if (!processedPortNames.contains(key)) {
				transferToNodeConfiguration(nodeConfiguration, nativeNodeContainer.getNode(),
						ConverterUtils.convertToKnimePort(portWrapper.portNr), portWrapper.port);
				processedPortNames.add(key);
			}
		}
	}

	private void transferToNodeConfiguration(final INodeConfiguration nodeConfiguration, final Node sourceNode,
			final int sourcePortNr, final Port targetPort) {
		// try to use the PortObject
		final IURIPortObject portObject = (IURIPortObject) sourceNode.getOutputObject(sourcePortNr);
		if (portObject != null) {
			final List<URIContent> uriContents = portObject.getURIContents();
			if (uriContents != null && !uriContents.isEmpty()) {
				final IFileParameter fileParam = (IFileParameter) nodeConfiguration.getParameter(targetPort.getName());
				if (targetPort.isMultiFile()) {
					int fileNumber = 0;
					final List<String> fixedFilenames = new LinkedList<String>();
					for (final URIContent uriContent : uriContents) {
						fixedFilenames.add(ConverterUtils.generateFileNameForExport(targetPort.getName(),
								uriContent.getExtension(), fileNumber));
						fileNumber++;
					}
					((FileListParameter) fileParam).setValue(fixedFilenames);
				} else {
					((FileParameter) fileParam).setValue(ConverterUtils.generateFileNameForExport(targetPort.getName(),
							uriContents.get(0).getExtension()));
				}
			} else {
				throw new RuntimeException("PortObject is empty. This is probably a bug and should be reported!");
			}
		} else {
			final URIPortObjectSpec portObjectSpec = (URIPortObjectSpec) sourceNode.getOutputSpec(sourcePortNr);
			final String extension = portObjectSpec.getFileExtensions().get(0);
			final String filename = ConverterUtils.generateFileNameForExport(targetPort.getName(), extension);
			if (targetPort.isMultiFile()) {
				((FileListParameter) nodeConfiguration.getParameter(targetPort.getName()))
						.setValue(Arrays.asList(filename));
			} else {
				((FileParameter) nodeConfiguration.getParameter(targetPort.getName())).setValue(filename);
			}
		}
	}

	private class PortWrapper {
		final Port port;
		final int portNr;

		PortWrapper(final Port port, final int portNr) {
			this.port = port;
			this.portNr = portNr;
		}
	}

}
