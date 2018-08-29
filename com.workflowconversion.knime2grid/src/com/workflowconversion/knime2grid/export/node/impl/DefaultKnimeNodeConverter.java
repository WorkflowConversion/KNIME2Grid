package com.workflowconversion.knime2grid.export.node.impl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;

import org.knime.base.node.io.csvreader.CSVReaderNodeFactory;
import org.knime.base.node.io.csvwriter.CSVWriterNodeFactory;
import org.knime.base.node.io.portobject.PortObjectReaderNodeFactory;
import org.knime.base.node.io.portobject.PortObjectWriterNodeFactory;
import org.knime.base.node.io.table.read.ReadTableNodeFactory;
import org.knime.base.node.io.table.write.WriteTableNodeFactory;
import org.knime.core.data.DataTable;
import org.knime.core.data.uri.IURIPortObject;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.Config;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeInPort;
import org.knime.core.node.workflow.NodeOutPort;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowCopyContent;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.FileUtil;
import org.knime.core.util.VMFileLocker;

import com.genericworkflownodes.knime.commandline.CommandLineElement;
import com.genericworkflownodes.knime.commandline.impl.CommandLineFile;
import com.genericworkflownodes.knime.commandline.impl.CommandLineFixedString;
import com.genericworkflownodes.knime.commandline.impl.CommandLineKNIMEWorkflowFile;
import com.genericworkflownodes.knime.commandline.impl.CommandLineParameter;
import com.genericworkflownodes.knime.nodes.io.importer.MimeFileImporterNodeFactory;
import com.genericworkflownodes.knime.nodes.io.outputfile.OutputFileNodeFactory;
import com.genericworkflownodes.knime.parameter.FileParameter;
import com.genericworkflownodes.knime.parameter.StringParameter;
import com.workflowconversion.knime2grid.export.node.NodeContainerConverter;
import com.workflowconversion.knime2grid.export.workflow.ConverterUtils;
import com.workflowconversion.knime2grid.model.ConnectionType;
import com.workflowconversion.knime2grid.model.Input;
import com.workflowconversion.knime2grid.model.Job;
import com.workflowconversion.knime2grid.model.JobType;
import com.workflowconversion.knime2grid.model.Output;

/**
 * Converts <i>native</i> KNIME nodes.
 * 
 * @author delagarza
 */
public class DefaultKnimeNodeConverter implements NodeContainerConverter {

	private static final String FLOW_VARIABLE_PREFIX_RIGHT = "\",\"";
	// -workflow.variable="<name>","<value>","<type>" (type is one of int,
	// double, String)
	// sets flow variables when executing workflows in batch mode.
	private static final String FLOW_VARIABLE_PREFIX_LEFT = "-workflow.variable=\"";
	private static final String FLOW_VARIABLE_SUFFIX = "\",\"String\"";

	private final static NodeLogger LOGGER = NodeLogger.getLogger(DefaultKnimeNodeConverter.class);
	private final static WorkflowManager WORKFLOW_MANAGER = WorkflowManager.ROOT.createAndAddProject("KNIME_WF_converter_tmp_wf", new WorkflowCreationHelper());

	static {
		WORKFLOW_MANAGER.addListener(new WorkflowListener() {
			@Override
			public void workflowChanged(final WorkflowEvent event) {
				switch (event.getType()) {
					case NODE_ADDED :
						int s = WORKFLOW_MANAGER.getNodeContainers().size();
						String n = ((WorkflowManager) event.getNewValue()).getName();
						LOGGER.debug("Added project \"" + n + "\" to (virtual) " + "grid root workflow, total count: " + s);
						break;
					case NODE_REMOVED :
						s = WORKFLOW_MANAGER.getNodeContainers().size();
						n = ((WorkflowManager) event.getOldValue()).getName();
						LOGGER.debug("Removed project \"" + n + "\" from " + "(virtual) grid root workflow, total count: " + s);
						break;
					default :
				}
			}
		});
	}

	@Override
	public boolean canHandle(final NativeNodeContainer nativeNodeContainer) {
		// as a default handler, this one must handle all possible nodes
		return true;
	}

	@Override
	public Job convert(final NativeNodeContainer nativeNodeContainer, final WorkflowManager workflowManager, final File workingDirectory) throws Exception {
		final Job job = new Job();
		job.setJobType(JobType.KnimeInternal);
		ConverterUtils.copyBasicInformation(job, nativeNodeContainer);

		// create a temporary folder on which we will create all of the mini
		// sub-wfs
		final Path sandboxDir = workingDirectory.toPath();

		final File miniWorkflowDir = Files.createTempDirectory(sandboxDir, "miniwf").toFile();
		final WorkflowCreationHelper creationHelper = new WorkflowCreationHelper();
		creationHelper.setWorkflowContext(new WorkflowContext.Factory(miniWorkflowDir).createContext());
		final WorkflowManager miniWorkflowManager = WORKFLOW_MANAGER.createAndAddProject("Mini Workflow for " + nativeNodeContainer.getNameWithID(),
				creationHelper);
		// copy and paste this node into the mini workflow
		final WorkflowCopyContent.Builder contentBuilder = WorkflowCopyContent.builder();
		contentBuilder.setNodeIDs(nativeNodeContainer.getID());

		final NodeID miniWorkflowNodeId = miniWorkflowManager.copyFromAndPasteHere(workflowManager, contentBuilder.build()).getNodeIDs()[0];
		int currentInput = 0, currentOutput = 0;

		final Collection<CommandLineElement> commandLineElements = new LinkedList<CommandLineElement>();
		// $ knime -noexit -consoleLog -application
		// org.knime.product.KNIME_BATCH_APPLICATION -nosplash
		// -workflowFile="<workflow-path>"
		commandLineElements.add(new CommandLineFixedString("-noexit"));
		commandLineElements.add(new CommandLineFixedString("-consoleLog"));
		commandLineElements.add(new CommandLineFixedString("-application"));
		commandLineElements.add(new CommandLineParameter(new StringParameter("application", "org.knime.product.KNIME_BATCH_APPLICATION")));
		commandLineElements.add(new CommandLineFixedString("-nosplash"));

		for (final ConnectionContainer connectionContainer : workflowManager.getIncomingConnectionsFor(nativeNodeContainer.getID())) {
			final NodeID sourceNodeId = connectionContainer.getSource();
			// this node is the recipient of another node's output find out
			// which port is part of this connection
			final int destPort = connectionContainer.getDestPort();
			// we now have the destination port and node... this is enough
			// information to
			// create a node that will feed data into this port time to find out
			// what kind of port this is
			final NodeInPort nodeInPort = nativeNodeContainer.getInPort(destPort);
			final PortType portType = nodeInPort.getPortType();
			final Class<? extends PortObject> inPortObjectClass = portType.getPortObjectClass();
			NodeFactory<? extends NodeModel> nodeFactory = null;
			final NodeSettings nodeSettings = ConverterUtils.createEmptyNodeSettings();
			final Collection<VariableSetting> inputSettings = new LinkedList<VariableSetting>();
			final String inputFileKey = "input" + currentInput;
			final Input input = new Input();
			input.setName(inputFileKey);
			input.setSourceId(sourceNodeId);
			input.setOriginalPortNr(destPort);
			job.addInput(input);
			if (DataTable.class.isAssignableFrom(inPortObjectClass)) {
				if (hasCsvReaderSource(workflowManager, sourceNodeId)) {
					// since we know that the source of this input is a
					// CSVReader, we can directly
					// create a CSVReader node in the mini workflow
					LOGGER.info("Creating CSVReader");
					// copy the settings from the origin CSVReader
					workflowManager.saveNodeSettings(sourceNodeId, nodeSettings);
					nodeFactory = new CSVReaderNodeFactory();
					inputSettings.add(new VariableSetting("url", inputFileKey));
				} else {
					LOGGER.info("Creating TableReader");
					nodeFactory = new ReadTableNodeFactory();
					inputSettings.add(new VariableSetting("filename", inputFileKey));
				}
			} else if (IURIPortObject.class.isAssignableFrom(inPortObjectClass)) {
				// the number of elements in IURIPortObjects is dynamic, so we
				// should
				// flag this as multifile just to be sure
				input.setMultiFile(true);
				LOGGER.info("Creating FileInput");
				nodeFactory = new MimeFileImporterNodeFactory();
				inputSettings.add(new VariableSetting("FILENAME", inputFileKey, "tmpfile.txt"));
				final String extensionKey = "extension" + currentInput;
				inputSettings.add(new VariableSetting("FILE_EXTENSION", extensionKey));
				final NodeContainer sourceNode = workflowManager.getNodeContainer(sourceNodeId);
				final NodeOutPort sourcePort = sourceNode.getOutPort(connectionContainer.getSourcePort());
				// make sure that the origin is indeed a IURIPortObject!
				if (IURIPortObject.class.isAssignableFrom(sourcePort.getPortType().getPortObjectClass())) {
					final IURIPortObject sourcePortObject = (IURIPortObject) sourcePort.getPortObject();
					final String sourceExtension = sourcePortObject.getURIContents().get(0).getExtension();
					commandLineElements.add(buildStringParameterAsFlowVariable(extensionKey, sourceExtension));
				} else {
					throw new RuntimeException("The port types of the source and destination port do not match");
				}
			} else {
				// not sure what the hell should we do here...
				// TODO: is it ok to assume that model writer is fine?
				LOGGER.info("PortType " + inPortObjectClass.getName());
				nodeFactory = new PortObjectReaderNodeFactory(portType);
				inputSettings.add(new VariableSetting("filename", inputFileKey));
			}
			// add the command line element for this file
			commandLineElements.add(buildFilePathAsFlowVariable(inputFileKey));
			final NodeID miniWorkflowDataNodeId = miniWorkflowManager.addNode(nodeFactory);

			addFlowVariables(nodeSettings, inputSettings);

			// save the settings in the data node
			miniWorkflowManager.loadNodeSettings(miniWorkflowDataNodeId, nodeSettings);
			// connect them
			miniWorkflowManager.addConnection(miniWorkflowDataNodeId, 1, miniWorkflowNodeId, destPort);

			currentInput++;
		}

		for (final ConnectionContainer connectionContainer : workflowManager.getOutgoingConnectionsFor(nativeNodeContainer.getID())) {
			// outputs need to be added only once per job!
			if (job.getOutputByOriginalPortNr(connectionContainer.getSourcePort()) == null) {
				final NodeID destNodeId = connectionContainer.getDest();
				final int sourcePort = connectionContainer.getSourcePort();
				final PortType portType = nativeNodeContainer.getOutputType(sourcePort);
				final Class<? extends PortObject> outPortObjectClass = portType.getPortObjectClass();
				final NodeFactory<? extends NodeModel> nodeFactory;
				final NodeSettings nodeSettings = ConverterUtils.createEmptyNodeSettings();
				final Output output = new Output();

				final String outputFileKey = "output" + currentOutput;
				final Collection<VariableSetting> outputSettings = new LinkedList<VariableSetting>();
				if (DataTable.class.isAssignableFrom(outPortObjectClass)) {
					if (hasCsvWriterOutput(workflowManager, destNodeId)) {
						// copy the settings from the output node
						workflowManager.saveNodeSettings(destNodeId, nodeSettings);
						nodeFactory = new CSVWriterNodeFactory();
					} else {
						nodeFactory = new WriteTableNodeFactory();
					}
					// "filename" applies both for WriteTableNodeModel and
					// CSVWriterNodeModel
					outputSettings.add(new VariableSetting("filename", outputFileKey));
				} else if (IURIPortObject.class.isAssignableFrom(outPortObjectClass)) {
					nodeFactory = new OutputFileNodeFactory();
					outputSettings.add(new VariableSetting("FILENAME", outputFileKey, "tmpfile.txt"));
					// the number of elements in IURIPortObjects is dynamic, so we should
					// flag this as multifile just to be sure
					output.setMultiFile(true);
					// TODO: do we need something like the following? outputSettings.add(new
					// VariableSetting("FILE_EXTENSION", "extension_" + currentOutput));
				} else {
					nodeFactory = new PortObjectWriterNodeFactory(portType);
					outputSettings.add(new VariableSetting("filename", outputFileKey));
				}
				commandLineElements.add(buildFilePathAsFlowVariable(outputFileKey));
				final NodeID miniWorkflowDataNodeId = miniWorkflowManager.addNode(nodeFactory);

				addFlowVariables(nodeSettings, outputSettings);

				// save the settings in the data node
				miniWorkflowManager.loadNodeSettings(miniWorkflowDataNodeId, nodeSettings);
				// connect them
				miniWorkflowManager.addConnection(miniWorkflowNodeId, sourcePort, miniWorkflowDataNodeId, 1);
				// add output to the job
				output.setName(outputFileKey);
				output.setOriginalPortNr(sourcePort);
				job.addOutput(output);
				currentOutput++;
			}
		}

		// we went through all of the inputs/outpus and added needed nodes to
		// provide/extract data, we can now save the mini workflow
		while (VMFileLocker.isLockedForVM(miniWorkflowDir)) {
			VMFileLocker.unlockForVM(miniWorkflowDir);
		}
		miniWorkflowManager.save(miniWorkflowDir, new ExecutionMonitor(), true);
		// make sure there is no file lock for this folder
		while (VMFileLocker.isLockedForVM(miniWorkflowDir)) {
			VMFileLocker.unlockForVM(miniWorkflowDir);
		}

		// compress the workflow folder into a zip file
		final File miniWorkflowArchive = Files
				.createTempFile(sandboxDir, "knimejob_" + ConverterUtils.fixNodeIdForFileSystem(nativeNodeContainer.getID().toString()), ".zip").toFile();
		FileUtil.zipDir(miniWorkflowArchive, miniWorkflowDir, 9);
		commandLineElements.add(new CommandLineKNIMEWorkflowFile(miniWorkflowArchive));
		// add the zipped workflow as input
		final Input input = new Input();
		input.setName(CommandLineKNIMEWorkflowFile.KNIME_MINI_WORKFLOW_KEY);
		input.setConnectionType(ConnectionType.UserProvided);
		input.setAssociatedFileParameter(new FileParameter("knimewf", miniWorkflowArchive.getCanonicalPath()));
		job.addInput(input);
		job.setCommandLine(commandLineElements);

		return job;
	}

	private void addFlowVariables(final NodeSettings nodeSettings, final Collection<VariableSetting> inputSettings) throws InvalidSettingsException {
		for (final VariableSetting inputSetting : inputSettings) {
			nodeSettings.getNodeSettings(Node.CFG_MODEL).addString(inputSetting.getSettingName(), inputSetting.getTempVariableValue());
			final NodeSettings variableNodeSettings = nodeSettings.getNodeSettings("variables");
			final Config flowVariableConfig = variableNodeSettings.addConfig(inputSetting.getSettingName());
			flowVariableConfig.addString("used_variable", inputSetting.getFlowVariableName());
			// TODO: what is this for?
			flowVariableConfig.addString("exposed_variable", null);
		}
	}

	private CommandLineElement buildFilePathAsFlowVariable(final String name) {
		// the value will be set later
		final FileParameter fileParameter = new FileParameter(name, "if you see this, it means that the code is broken! Report this bug!");
		return new CommandLineFile(fileParameter, buildFlowVariablePrefix(name), FLOW_VARIABLE_SUFFIX);
	}

	private CommandLineElement buildStringParameterAsFlowVariable(final String name, final String value) {
		final StringParameter stringParameter = new StringParameter(name, value);
		return new CommandLineParameter(stringParameter, buildFlowVariablePrefix(name), FLOW_VARIABLE_SUFFIX);
	}

	private String buildFlowVariablePrefix(final String name) {
		return FLOW_VARIABLE_PREFIX_LEFT + name + FLOW_VARIABLE_PREFIX_RIGHT;
	}

	private boolean hasCsvReaderSource(final WorkflowManager workflowManager, final NodeID sourceNodeId) {
		return ConverterUtils.nodeModelMatchesClass(workflowManager, sourceNodeId, ConverterUtils.CSVREADER_CLASS_NAME);
	}

	private boolean hasCsvWriterOutput(final WorkflowManager workflowManager, final NodeID destNodeId) {
		return ConverterUtils.nodeModelMatchesClass(workflowManager, destNodeId, ConverterUtils.CSVWRITER_CLASS_NAME);
	}

}
