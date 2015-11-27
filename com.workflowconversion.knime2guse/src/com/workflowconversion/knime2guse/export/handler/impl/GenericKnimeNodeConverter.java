package com.workflowconversion.knime2guse.export.handler.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;

import com.genericworkflownodes.knime.commandline.CommandLineElement;
import com.genericworkflownodes.knime.commandline.impl.CommandLineCTDFile;
import com.genericworkflownodes.knime.config.INodeConfiguration;
import com.genericworkflownodes.knime.config.reader.CTDConfigurationReader;
import com.genericworkflownodes.knime.config.reader.InvalidCTDFileException;
import com.genericworkflownodes.knime.config.writer.CTDConfigurationWriter;
import com.genericworkflownodes.knime.generic_node.GenericKnimeNodeModel;
import com.genericworkflownodes.knime.parameter.FileListParameter;
import com.genericworkflownodes.knime.parameter.FileParameter;
import com.genericworkflownodes.knime.parameter.Parameter;
import com.genericworkflownodes.knime.port.Port;
import com.workflowconversion.knime2guse.export.ConverterUtils;
import com.workflowconversion.knime2guse.export.handler.NodeContainerConverter;
import com.workflowconversion.knime2guse.model.Input;
import com.workflowconversion.knime2guse.model.Job;
import com.workflowconversion.knime2guse.model.Output;

/**
 * Converts nodes that were imported into KNIME via GKN.
 * 
 * @author delagarza
 * 
 */
public class GenericKnimeNodeConverter implements NodeContainerConverter {

	@Override
	public boolean canHandle(NativeNodeContainer nativeNodeContainer) {
		return GenericKnimeNodeModel.class.isAssignableFrom(nativeNodeContainer.getNodeModel().getClass());
	}

	@Override
	public Job convert(NativeNodeContainer nativeNodeContainer, final WorkflowManager workflowManager,
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

		// we need to check if this node supports ctds natively and if so, we need to modify the ctd file in order
		// to set the right inputs and outputs
		boolean ctdFound = false;
		for (final CommandLineElement element : commandLineElements) {
			if (element instanceof CommandLineCTDFile) {
				// create an input for the ctd file
				if (ctdFound) {
					throw new IllegalStateException(
							"This job already has a CTD file. Only one CTD file per job is allowed.");
				}
				handleCTDFile((CommandLineCTDFile) element, job, nodeConfiguration);
				ctdFound = true;
			} else {
				associateCommandLineElementToJob(element, job, nodeConfiguration);
			}
		}

		return job;
	}

	// associates the passed element to the given job
	// it also "fixes" the inputs/outputs inside the CTD file
	private void handleCTDFile(final CommandLineCTDFile element, final Job job,
			final INodeConfiguration nodeConfiguration) throws IOException, InvalidCTDFileException {
		final Input ctdInput = new Input();
		ctdInput.setCTD(true);
		ctdInput.setName("ctd-input");
		// write out the ctd into a file and fix the inputs and outputs (i.e., remove the paths from the files)
		// we should not modify the node configuration because this will affect future runs of the workflow!
		final INodeConfiguration clonedNodeConfiguration = cloneNodeConfiguration(nodeConfiguration);
		fixAndassociateConfigurationToJob(clonedNodeConfiguration, job);
		// set the fixed CTD as data for this input
		ctdInput.setData(generateCTD(clonedNodeConfiguration));
		job.addInput(ctdInput);
	}

	private File generateCTD(final INodeConfiguration nodeConfiguration) throws IOException {
		final File ctdFile = File.createTempFile("ctdfile", "ctd");
		final CTDConfigurationWriter writer = new CTDConfigurationWriter(ctdFile);
		writer.write(nodeConfiguration);
		return ctdFile;
	}

	private INodeConfiguration cloneNodeConfiguration(final INodeConfiguration nodeConfiguration)
			throws InvalidCTDFileException, IOException {
		final File ctdFile = generateCTD(nodeConfiguration);
		final CTDConfigurationReader reader = new CTDConfigurationReader();
		return reader.read(new FileInputStream(ctdFile));
	}

	private void fixAndassociateConfigurationToJob(final INodeConfiguration nodeConfiguration, final Job job) {
		// go through all inputs/outputs of the configuration and associate them to the passed job
		for (final Port inputPort : nodeConfiguration.getInputPorts()) {
			final Parameter<?> associatedParameter = nodeConfiguration.getParameter(inputPort.getName());
			final String parameterName = inputPort.getName();
			if (associatedParameter instanceof FileParameter && !inputPort.isMultiFile()) {
				// given a value such as /var/tmp/tmpinput0.xml, generate input_0.xml
				final String filename = ((FileParameter) associatedParameter).getValue();
				final String extension = FilenameUtils.getExtension(filename);
				((FileParameter) associatedParameter).setValue(parameterName + '.' + extension);
				final Input input = new Input();
				input.setExtension(extension);
				input.setName(parameterName);
				job.addInput(input);
			} else if (associatedParameter instanceof FileListParameter && inputPort.isMultiFile()) {
				// create as many inputs as needed
				final List<String> fixedFilenames = new LinkedList<String>();
				int fileNumber = 0;
				for (final String filename : ((FileListParameter) associatedParameter).getStrings()) {
					final String extension = FilenameUtils.getExtension(filename);
					fixedFilenames.add(parameterName + '_' + fileNumber + '.' + extension);
					final Input input = new Input();
					input.setName(parameterName + '_' + fileNumber);
					input.setExtension(extension);
					job.addInput(input);
				}
				((FileListParameter) associatedParameter).setValue(fixedFilenames);
			} else {
				throw new RuntimeException(
						"Invalid association between parameters and input files. This is probably a bug, please report it.");
			}
		}
		for (final Port outputPort : nodeConfiguration.getOutputPorts()) {
			final Parameter<?> associatedParameter = nodeConfiguration.getParameter(outputPort.getName());
			final String parameterName = outputPort.getName();
			if (associatedParameter instanceof FileParameter && !outputPort.isMultiFile()) {
				final String filename = ((FileParameter) associatedParameter).getValue();
				final String extension = FilenameUtils.getExtension(filename);
				((FileParameter) associatedParameter).setValue(parameterName + '.' + extension);
				final Output output = new Output();
				output.setName(parameterName);
				output.setExtension(extension);
				job.addOutput(output);
			} else if (associatedParameter instanceof FileListParameter && outputPort.isMultiFile()) {
				final List<String> fixedFilenames = new LinkedList<String>();
				int fileNumber = 0;
				for (final String filename : ((FileListParameter) associatedParameter).getStrings()) {
					final String extension = FilenameUtils.getExtension(filename);
					fixedFilenames.add(parameterName + '_' + fileNumber + '.' + extension);
					final Output output = new Output();
					output.setName(parameterName + '_' + fileNumber);
					output.setExtension(extension);
					job.addOutput(output);
				}
				((FileListParameter) associatedParameter).setValue(fixedFilenames);
			} else {
				throw new RuntimeException(
						"Invalid association between parameters and output files. This is probably a bug, please report it.");
			}
		}
	}

	// associate the passed element to the input/output of the passed job using the element's key
	// if the element cannot be associated, an exception will be thrown
	private void associateCommandLineElementToJob(final CommandLineElement element, final Job job,
			final INodeConfiguration nodeConfiguration) {
		boolean associated = false;
		// try the inputs
		for (final Port port : nodeConfiguration.getInputPorts()) {
			if (element.getKey().equals(port.getName())) {
				final Input input = new Input();
				input.setName(element.getKey());
				job.addInput(input);
				associated = true;
				break;
			}
		}
		// now the outputs
		if (!associated) {
			for (final Port port : nodeConfiguration.getOutputPorts()) {
				if (element.getKey().equals(port.getName())) {
					final Output output = new Output();
					output.setName(element.getKey());
					job.addOutput(output);
					associated = true;
					break;
				}
			}
		}
		if (!associated) {
			throw new IllegalArgumentException(
					"The provided command line element could not be associated to any of the inputs or outputs. This is probably a bug and should be reported.");
		}
	}
}
