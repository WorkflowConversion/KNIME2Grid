package com.workflowconversion.knime2grid.export.node.impl;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
import org.knime.core.node.NodeLogger;
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
import com.workflowconversion.knime2grid.exception.ApplicationException;
import com.workflowconversion.knime2grid.export.node.NodeContainerConverter;
import com.workflowconversion.knime2grid.export.workflow.ConverterUtils;
import com.workflowconversion.knime2grid.model.ConnectionType;
import com.workflowconversion.knime2grid.model.Input;
import com.workflowconversion.knime2grid.model.Job;
import com.workflowconversion.knime2grid.model.JobType;
import com.workflowconversion.knime2grid.model.Output;

/**
 * Converts nodes that were imported into KNIME via GKN.
 * 
 * @author delagarza
 */
public class GenericKnimeNodeConverter implements NodeContainerConverter {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(GenericKnimeNodeConverter.class);

	@Override
	public boolean canHandle(final NativeNodeContainer nativeNodeContainer) {
		return GenericKnimeNodeModel.class.isAssignableFrom(nativeNodeContainer.getNodeModel().getClass());
	}

	@Override
	public Job convert(final NativeNodeContainer nativeNodeContainer, final WorkflowManager workflowManager, final File workingDirectory) throws Exception {
		final GenericKnimeNodeModel gknModel = (GenericKnimeNodeModel) (nativeNodeContainer).getNodeModel();
		final INodeConfiguration nodeConfiguration = gknModel.getNodeConfiguration();

		final Job job = new Job();
		job.setJobType(JobType.CommandLine);
		ConverterUtils.copyBasicInformation(job, nativeNodeContainer);

		final Collection<CommandLineElement> commandLineElements = gknModel.getCommandLine(workingDirectory);
		job.setCommandLine(commandLineElements);

		// input/output ports are also handled as parameters in GKN, so we need to keep track of
		// the ports that we've processed
		final Set<String> processedPortNames = new TreeSet<String>();

		// all inputs and outputs of GKNs are URIs, so this simplifies the conversion
		// we need to know how to actually execute this job on a command line environment
		// so we need to access the command generator... however, at this point, there
		// is no guarantee that all jobs have been converted so we need to use the WorkflowManager to
		// gather information about other nodes
		createInputsAndOutputsFromKnimeWorkflow(nativeNodeContainer, workflowManager, nodeConfiguration, job, processedPortNames);

		// process all non-input/non-output parameters AND the CTD, if any.
		// this must be AFTER inputs/outputs have been processed, because we are adding
		// a CTD input that might shift the indices of the current inputs.
		boolean ctdFound = false;
		for (final CommandLineElement element : commandLineElements) {
			if (element instanceof CommandLineCTDFile) {
				// create an input for the ctd file
				if (ctdFound) {
					throw new ApplicationException(
							"This job already has a CTD file. Only one CTD file per job is allowed. This is probably a bug and should be reported.");
				}
				addCTDInputPort(workflowManager, (CommandLineCTDFile) element, job, nodeConfiguration, nativeNodeContainer);
				ctdFound = true;
			} else if (element instanceof ParametrizedCommandLineElement && !processedPortNames.contains(element.getKey())) {
				// we need to process only true parameters, not flags or option identifiers
				final Parameter<?> parameter = nodeConfiguration.getParameter(element.getKey());
				job.addParameter(element.getKey(), parameter.getStringRep());
			} else if (element instanceof CommandLineFile) {
				// TODO: this seems to be dead code... only elements in the command line are the CTD and the flag...
				// fix the given paths for export
				final CommandLineFile commandLineFile = (CommandLineFile) element;
				final FileParameter fileParameter = commandLineFile.getValue();
				final String extension = FilenameUtils.getExtension(fileParameter.getValue());
				final String fixedPath;
				if (commandLineFile.isSequenced()) {
					fixedPath = ConverterUtils.generateFileNameForExport(fileParameter.getKey(), extension, commandLineFile.getSequenceNumber());
				} else {
					fixedPath = ConverterUtils.generateFileNameForExport(fileParameter.getKey(), extension);
				}
				fileParameter.setValue(fixedPath);
			}
		}

		return job;
	}

	private void createInputsAndOutputsFromKnimeWorkflow(final NativeNodeContainer nativeNodeContainer, final WorkflowManager workflowManager,
			final INodeConfiguration nodeConfiguration, final Job job, final Set<String> processedPortNames) {
		for (final ConnectionContainer connectionContainer : workflowManager.getIncomingConnectionsFor(nativeNodeContainer.getID())) {
			final NodeID sourceNodeId = connectionContainer.getSource();
			final Node sourceNode = ((NativeNodeContainer) workflowManager.getNodeContainer(connectionContainer.getSource())).getNode();
			final int destPortNr = connectionContainer.getDestPort();
			final Port destPort = nodeConfiguration.getInputPorts().get(ConverterUtils.convertFromKnimePort(destPortNr));
			final Input input = new Input();
			input.setSourceId(sourceNodeId);
			input.setOriginalPortNr(destPortNr);
			input.setName(destPort.getName() + getExtensionForPort(sourceNode, connectionContainer.getSourcePort()));
			// input.setMultiFile(destPort.isMultiFile());
			job.addInput(input);

			processedPortNames.add(destPort.getName());
		}
		for (final ConnectionContainer connectionContainer : workflowManager.getOutgoingConnectionsFor(nativeNodeContainer.getID())) {
			final int sourcePortNr = connectionContainer.getSourcePort();
			if (job.getOutputByOriginalPortNr(sourcePortNr) == null) {
				// first time we see the output, we need to add it
				final Port sourcePort = nodeConfiguration.getOutputPorts().get(ConverterUtils.convertFromKnimePort(sourcePortNr));
				final Output newOutput = new Output();
				final Node sourceNode = ((NativeNodeContainer) workflowManager.getNodeContainer(connectionContainer.getSource())).getNode();
				// newOutput.setMultiFile(sourcePort.isMultiFile());
				newOutput.setName(sourcePort.getName() + getExtensionForPort(sourceNode, connectionContainer.getSourcePort()));
				newOutput.setOriginalPortNr(sourcePortNr);
				job.addOutput(newOutput);
				processedPortNames.add(sourcePort.getName());
			}
		}
	}

	// when executing GKN in KNIME, each GKN generates an "on the fly" CTD file and uses it to
	// execute the associated binary, but what we need here is to add a new input containing a CTD
	private void addCTDInputPort(final WorkflowManager workflowManager, final CommandLineCTDFile element, final Job job,
			final INodeConfiguration nodeConfiguration, final NativeNodeContainer nativeNodeContainer) throws IOException, InvalidCTDFileException {
		final Input ctdInput = new Input();
		ctdInput.setName(CommandLineCTDFile.CTD_FILE_KEY);
		ctdInput.setConnectionType(ConnectionType.UserProvided);
		// write out the ctd into a file and fix the inputs and outputs
		// we should not modify the node configuration because this will affect future runs of the workflow!
		final INodeConfiguration clonedNodeConfiguration = cloneNodeConfiguration(nodeConfiguration);
		fixFilenamesInConfiguration(workflowManager, clonedNodeConfiguration, nativeNodeContainer);
		// set the fixed CTD as data for this input
		final FileParameter ctdFileParameter = new FileParameter(CommandLineCTDFile.CTD_FILE_KEY,
				dumpConfiguration(clonedNodeConfiguration).getCanonicalPath());
		ctdInput.setAssociatedFileParameter(ctdFileParameter);
		job.addInput(ctdInput);
		// fix the command line element!
		element.setValue(ctdFileParameter);
	}

	private File dumpConfiguration(final INodeConfiguration nodeConfiguration) throws IOException {
		final File ctdFile = File.createTempFile("ctdfile", ".ctd");
		final CTDConfigurationWriter ctdWriter = new CTDConfigurationWriter(ctdFile);
		ctdWriter.setIgnoreUnusedParameters(false);
		ctdWriter.write(nodeConfiguration);
		return ctdFile;
	}

	private INodeConfiguration cloneNodeConfiguration(final INodeConfiguration nodeConfiguration) throws InvalidCTDFileException, IOException {
		final StringWriter stringWriter = new StringWriter();
		final BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);
		final CTDConfigurationWriter ctdWriter = new CTDConfigurationWriter(bufferedWriter);
		ctdWriter.setIgnoreUnusedParameters(false);
		ctdWriter.write(nodeConfiguration);

		final CTDConfigurationReader reader = new CTDConfigurationReader();
		// no need to close byte array input streams
		final InputStream inputStream = new ByteArrayInputStream(stringWriter.getBuffer().toString().getBytes(StandardCharsets.UTF_8));
		return reader.read(inputStream);
	}

	// the inputs/outputs of the original CTD contain absolute filenames... this method will "fix" those
	// names and transform each filename to a relative path and will also include
	// the name of the input/output port, for instance:
	//
	// original value for input named "ligands" with extension "sdf":
	// /var/etc/tmp9237.sdf
	// changed value for input": ligands.sdf
	private void fixFilenamesInConfiguration(final WorkflowManager workflowManager, final INodeConfiguration nodeConfiguration,
			final NativeNodeContainer nativeNodeContainer) {
		// [in/out]_{portname}, eg: in_sequence, out_result
		// since an incoming and an outgoing port can have the same names, we
		// need to be
		// able to distinguish them
		final Collection<PortWrapper> connectedIncomingPorts = new LinkedList<PortWrapper>();
		final Collection<PortWrapper> connectedOutgoingPorts = new LinkedList<PortWrapper>();
		// we first need to figure out which ports are connected and therefore,
		// in use
		extractConnectedPorts(workflowManager, nativeNodeContainer, nodeConfiguration, connectedIncomingPorts, connectedOutgoingPorts);
		// keep track of the ports that we've processed so we only process each
		// port
		// only once
		final Set<String> processedInPortNames = new TreeSet<String>();
		final Set<String> processedOutPortNames = new TreeSet<String>();
		// first, try the happy path, which is: ports already contain the info
		// we need
		fixFilePathsFromAssociatedParameters(workflowManager, nativeNodeContainer, nodeConfiguration, connectedIncomingPorts, processedInPortNames);
		fixFilePathsFromAssociatedParameters(workflowManager, nativeNodeContainer, nodeConfiguration, connectedOutgoingPorts, processedOutPortNames);
		// now, for the input ports, get the PortObject from the source port
		fixIncomingPortsFromSourcePorts(workflowManager, nativeNodeContainer, nodeConfiguration, connectedIncomingPorts, processedInPortNames);
		// for the output ports, just process the associated PortObject
		fixOutgoingPortsUsingPortObjects(workflowManager, nativeNodeContainer, nodeConfiguration, connectedOutgoingPorts, processedOutPortNames);
	}

	// fills the passed collections with the actually used incoming/outgoing
	// ports
	private void extractConnectedPorts(final WorkflowManager workflowManager, final NativeNodeContainer nativeNodeContainer,
			final INodeConfiguration nodeConfiguration, final Collection<PortWrapper> connectedIncomingPorts,
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

	private void fixFilePathsFromAssociatedParameters(final WorkflowManager workflowManager, final NativeNodeContainer nativeNodeContainer,
			final INodeConfiguration nodeConfiguration, final Collection<PortWrapper> portWrappers, final Set<String> processedPorts) {

		for (final PortWrapper portWrapper : portWrappers) {
			final Port port = portWrapper.port;
			final Parameter<?> associatedParameter = nodeConfiguration.getParameter(port.getName());
			final String parameterName = port.getName();
			if (!processedPorts.contains(parameterName)) {
				final List<String> fileNames = extractFileNames(port, nodeConfiguration);
				if (fileNames.size() == 1) {
					final String fileName = fileNames.get(0);
					if (StringUtils.isNotBlank(fileName)) {
						final String extension = FilenameUtils.getExtension(fileName);
						final String fixedFileName = ConverterUtils.generateFileNameForExport(parameterName, extension);
						// single file from a non multifile port or from a multifile port!
						if (associatedParameter instanceof FileParameter) {
							((FileParameter) associatedParameter).setValue(fixedFileName);
						} else {
							((FileListParameter) associatedParameter).setValue(Arrays.asList(fixedFileName));
						}
						processedPorts.add(parameterName);
					}
				} else if (fileNames.size() > 1) {
					if (associatedParameter.getValue() != null) {
						// multifile
						final List<String> fixedFilenames = new LinkedList<String>();
						int fileNumber = 0;
						for (final String fileName : fileNames) {
							final String extension = FilenameUtils.getExtension(fileName);
							fixedFilenames.add(ConverterUtils.generateFileNameForExport(parameterName, extension, fileNumber));
							fileNumber++;
						}
						((FileListParameter) associatedParameter).setValue(fixedFilenames);
						processedPorts.add(parameterName);
					}
				} else {
					// 0 inputs?
					throw new RuntimeException("Invalid association between parameters and input files. This is probably a bug, please report it.");
				}
			}
		}
	}

	// some multifile ports have only one file associated, in this case, it's not needed to treat ports as multifile
	private List<String> extractFileNames(final Port port, final INodeConfiguration nodeConfiguration) {
		final String parameterName = port.getName();
		final Parameter<?> associatedParameter = nodeConfiguration.getParameter(parameterName);
		final List<String> fileNames = new ArrayList<>();
		if ((associatedParameter instanceof FileParameter && !port.isMultiFile())) {
			// single file
			fileNames.add(((FileParameter) associatedParameter).getValue());
		} else if (associatedParameter instanceof FileListParameter && port.isMultiFile()) {
			// potential multifile
			fileNames.addAll(((FileListParameter) associatedParameter).getStrings());
		} else {
			throw new ApplicationException("Invalid association between parameters and input files. This is probably a bug and should be reported");
		}
		return fileNames;
	}

	private void fixIncomingPortsFromSourcePorts(final WorkflowManager workflowManager, final NativeNodeContainer nativeNodeContainer,
			final INodeConfiguration nodeConfiguration, final Collection<PortWrapper> incomingPorts, final Set<String> processedPortNames) {
		for (final PortWrapper portWrapper : incomingPorts) {
			final String key = portWrapper.port.getName();
			if (!processedPortNames.contains(key)) {
				final int portNr = ConverterUtils.convertToKnimePort(portWrapper.portNr);
				final ConnectionContainer connection = workflowManager.getIncomingConnectionFor(nativeNodeContainer.getID(), portNr);
				final Node sourceNode = ((NativeNodeContainer) workflowManager.getNodeContainer(connection.getSource())).getNode();
				transferToNodeConfiguration(nodeConfiguration, sourceNode, connection.getSourcePort(), portWrapper.port);
				processedPortNames.add(key);
			}
		}
	}

	private void fixOutgoingPortsUsingPortObjects(final WorkflowManager workflowManager, final NativeNodeContainer nativeNodeContainer,
			final INodeConfiguration nodeConfiguration, final Collection<PortWrapper> outgoingPorts, final Set<String> processedPortNames) {
		for (final PortWrapper portWrapper : outgoingPorts) {
			final String key = portWrapper.port.getName();
			if (!processedPortNames.contains(key)) {
				transferToNodeConfiguration(nodeConfiguration, nativeNodeContainer.getNode(), ConverterUtils.convertToKnimePort(portWrapper.portNr),
						portWrapper.port);
				processedPortNames.add(key);
			}
		}
	}

	private void transferToNodeConfiguration(final INodeConfiguration nodeConfiguration, final Node sourceNode, final int sourcePortNr, final Port targetPort) {
		final IURIPortObject portObject = (IURIPortObject) sourceNode.getOutputObject(sourcePortNr);
		if (portObject != null) {
			final List<URIContent> uriContents = portObject.getURIContents();
			if (uriContents != null && !uriContents.isEmpty()) {
				final IFileParameter fileParam = (IFileParameter) nodeConfiguration.getParameter(targetPort.getName());
				if (targetPort.isMultiFile()) {
					int fileNumber = 0;
					final List<String> fixedFilenames = new LinkedList<String>();
					for (final URIContent uriContent : uriContents) {
						fixedFilenames.add(ConverterUtils.generateFileNameForExport(targetPort.getName(), uriContent.getExtension(), fileNumber));
						fileNumber++;
					}
					((FileListParameter) fileParam).setValue(fixedFilenames);
				} else {
					((FileParameter) fileParam).setValue(ConverterUtils.generateFileNameForExport(targetPort.getName(), uriContents.get(0).getExtension()));
				}
			} else {
				throw new RuntimeException("PortObject is empty. This is probably a bug and should be reported!");
			}
		} else {
			final URIPortObjectSpec portObjectSpec = (URIPortObjectSpec) sourceNode.getOutputSpec(sourcePortNr);
			final String extension = portObjectSpec.getFileExtensions().get(0);
			final String filename = ConverterUtils.generateFileNameForExport(targetPort.getName(), extension);
			if (targetPort.isMultiFile()) {
				((FileListParameter) nodeConfiguration.getParameter(targetPort.getName())).setValue(Arrays.asList(filename));
			} else {
				((FileParameter) nodeConfiguration.getParameter(targetPort.getName())).setValue(filename);
			}
		}
	}

	// PortObjects are available only through output ports, so we need the source node/port to retrieve the extension
	// includes the '.' before the extension!
	private String getExtensionForPort(final Node sourceNode, final int sourcePortNr) {
		final IURIPortObject portObject = (IURIPortObject) sourceNode.getOutputObject(sourcePortNr);
		final List<URIContent> uriContents = portObject.getURIContents();
		if (uriContents != null && !uriContents.isEmpty()) {
			return '.' + uriContents.get(0).getExtension();
		}

		LOGGER.error(String.format("Could not find extension for node %s at port number %d", sourceNode.getName(), sourcePortNr));
		return "";
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
