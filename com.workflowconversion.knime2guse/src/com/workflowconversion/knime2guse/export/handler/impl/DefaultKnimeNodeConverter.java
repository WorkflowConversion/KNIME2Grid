package com.workflowconversion.knime2guse.export.handler.impl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;

import org.knime.base.node.io.csvreader.CSVReaderNodeFactory;
import org.knime.base.node.io.csvwriter.CSVWriterNodeFactory;
import org.knime.base.node.io.csvwriter.CSVWriterNodeModel;
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

import com.genericworkflownodes.knime.commandline.CommandLineElement;
import com.genericworkflownodes.knime.commandline.impl.CommandLineFile;
import com.genericworkflownodes.knime.commandline.impl.CommandLineKNIMEWorkflowFile;
import com.genericworkflownodes.knime.commandline.impl.CommandLineOptionIdentifier;
import com.genericworkflownodes.knime.commandline.impl.CommandLineParameter;
import com.genericworkflownodes.knime.nodes.io.importer.MimeFileImporterNodeFactory;
import com.genericworkflownodes.knime.nodes.io.outputfile.OutputFileNodeFactory;
import com.genericworkflownodes.knime.parameter.FileParameter;
import com.genericworkflownodes.knime.parameter.StringParameter;
import com.workflowconversion.knime2guse.export.ConverterUtils;
import com.workflowconversion.knime2guse.export.handler.NodeContainerConverter;
import com.workflowconversion.knime2guse.model.Input;
import com.workflowconversion.knime2guse.model.Job;
import com.workflowconversion.knime2guse.model.Output;

/**
 * Converts <i>native</i> KNIME nodes.
 * 
 * @author delagarza
 * 
 */
public class DefaultKnimeNodeConverter implements NodeContainerConverter {

	// -workflow.variable="<name>","<value>","<type>" (type is one of int, double, String)
	// sets flow variables when executing workflows in batch mode.
	private static final String FLOW_VARIABLE_PREFIX = "-workflow.variable=\"";
	private static final String FLOW_VARIABLE_SUFFIX = "\",\"String\"";

	private static final String CSVREADER_CLASS_NAME = "org.knime.base.node.io.csvreader.CSVReaderNodeModel";
	private static final String CSVWRITER_CLASS_NAME = CSVWriterNodeModel.class.getCanonicalName();

	private final static NodeLogger LOGGER = NodeLogger.getLogger(DefaultKnimeNodeConverter.class);
	private final static WorkflowManager WORKFLOW_MANAGER = WorkflowManager.ROOT.createAndAddProject(
			"KNIME_WF_converter_tmp_wf", new WorkflowCreationHelper());

	static {
		WORKFLOW_MANAGER.addListener(new WorkflowListener() {
			@Override
			public void workflowChanged(final WorkflowEvent event) {
				switch (event.getType()) {
				case NODE_ADDED:
					int s = WORKFLOW_MANAGER.getNodeContainers().size();
					String n = ((WorkflowManager) event.getNewValue()).getName();
					LOGGER.debug("Added project \"" + n + "\" to (virtual) " + "grid root workflow, total count: " + s);
					break;
				case NODE_REMOVED:
					s = WORKFLOW_MANAGER.getNodeContainers().size();
					n = ((WorkflowManager) event.getOldValue()).getName();
					LOGGER.debug("Removed project \"" + n + "\" from " + "(virtual) grid root workflow, total count: "
							+ s);
					break;
				default:
				}
			}
		});
	}

	@Override
	public boolean canHandle(NativeNodeContainer nativeNodeContainer) {
		// as a default handler, this one must handle all possible nodes
		return true;
	}

	@Override
	public Job convert(NativeNodeContainer nativeNodeContainer, final WorkflowManager workflowManager,
			final File workingDirectory) throws Exception {
		final Job job = new Job();
		ConverterUtils.copyBasicInformation(job, nativeNodeContainer);

		// create a temporary folder on which we will create all of the mini sub-wfs
		final Path sandboxDir = workingDirectory.toPath();

		final Path miniWorkflowDir = Files.createTempDirectory(sandboxDir, "miniwf");
		final WorkflowCreationHelper creationHelper = new WorkflowCreationHelper();
		creationHelper.setWorkflowContext(new WorkflowContext.Factory(miniWorkflowDir.toFile()).createContext());
		final WorkflowManager miniWorkflowManager = WORKFLOW_MANAGER.createAndAddProject("Mini Workflow for "
				+ nativeNodeContainer.getNameWithID(), creationHelper);
		// copy and paste this node into the mini workflow
		final WorkflowCopyContent content = new WorkflowCopyContent();
		content.setNodeIDs(nativeNodeContainer.getID());

		final NodeID miniWorkflowNodeId = miniWorkflowManager.copyFromAndPasteHere(workflowManager, content)
				.getNodeIDs()[0];
		int currentInput = 0, currentOutput = 0;

		final Collection<CommandLineElement> commandLineElements = new LinkedList<CommandLineElement>();
		// knime.exe -noexit -consoleLog -application org.knime.product.KNIME_BATCH_APPLICATION -nosplash
		// -workflowFile="<workflow-path>"
		job.setExecutableName("knime");
		commandLineElements.add(new CommandLineOptionIdentifier("-noexit"));
		commandLineElements.add(new CommandLineOptionIdentifier("-consoleLog"));
		commandLineElements.add(new CommandLineOptionIdentifier("-application"));
		commandLineElements.add(new CommandLineParameter(new StringParameter("application",
				"org.knime.product.KNIME_BATCH_APPLICATION")));
		commandLineElements.add(new CommandLineOptionIdentifier("-nosplash"));

		// find in which connections this node is present to create the inputs/outputs
		for (final ConnectionContainer connectionContainer : workflowManager.getConnectionContainers()) {
			final NodeID destNodeId = connectionContainer.getDest();
			final NodeID sourceNodeId = connectionContainer.getSource();
			if (nativeNodeContainer.getID().equals(destNodeId)) {
				// this node is the recipient of another node's output find out which port is part of this connection
				final int destPort = connectionContainer.getDestPort();
				// we now have the destination port and node... this is enough information to
				// create a node that will feed data into this port time to find out what kind of port this is
				final NodeInPort nodeInPort = nativeNodeContainer.getInPort(destPort);
				final PortType portType = nodeInPort.getPortType();
				final Class<? extends PortObject> portObjectClass = portType.getPortObjectClass();
				NodeFactory<? extends NodeModel> nodeFactory = null;
				final NodeSettings nodeSettings = new NodeSettings("tmp_node_settings");
				// copied from SingleNodeContainer.CFG_VARIABLES
				nodeSettings.addConfig("variables");
				nodeSettings.addConfig(Node.CFG_MISC_SETTINGS);
				nodeSettings.addConfig(Node.CFG_MODEL);
				final Collection<VariableSetting> inputSettings = new LinkedList<VariableSetting>();
				final String inputFileKey = "input_" + currentInput;
				if (DataTable.class.isAssignableFrom(portObjectClass)) {
					if (hasCsvReaderSource(miniWorkflowManager, sourceNodeId)) {
						LOGGER.info("Creating CSVReader");
						// we need to copy the settings from the origin CSVReader
						workflowManager.saveNodeSettings(sourceNodeId, nodeSettings);
						nodeFactory = new CSVReaderNodeFactory();
						inputSettings.add(new VariableSetting("url", inputFileKey));
					} else {
						LOGGER.info("Creating TableReader");
						nodeFactory = new ReadTableNodeFactory();
						inputSettings.add(new VariableSetting("filename", inputFileKey));
					}
				} else if (IURIPortObject.class.isAssignableFrom(portObjectClass)) {
					// FIXME: IURIPortObject contains a list of URIs!!!!!
					LOGGER.info("Creating FileInput");
					nodeFactory = new MimeFileImporterNodeFactory();
					inputSettings.add(new VariableSetting("FILENAME", inputFileKey));
					final String extensionKey = "extension_" + currentInput;
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
					LOGGER.info("PortType " + portObjectClass.getName());
					nodeFactory = new PortObjectReaderNodeFactory(portType);
					inputSettings.add(new VariableSetting("filename", inputFileKey));
				}
				// add the command line element for this file
				commandLineElements.add(buildFileAsFlowVariable(inputFileKey));
				final NodeID miniWorkflowDataNodeId = miniWorkflowManager.addNode(nodeFactory);

				addFlowVariables(nodeSettings, inputSettings);

				// save the settings in the data node
				miniWorkflowManager.loadNodeSettings(miniWorkflowDataNodeId, nodeSettings);
				// connect them
				miniWorkflowManager.addConnection(miniWorkflowDataNodeId, 1, miniWorkflowNodeId, destPort);
				// add an input to the job
				final Input input = new Input();
				input.setName(inputFileKey);
				job.addInput(input);
				currentInput++;
			} else if (nativeNodeContainer.getID().equals(sourceNodeId)) {
				final int sourcePort = connectionContainer.getSourcePort();
				final PortType portType = nativeNodeContainer.getOutputType(sourcePort);
				final Class<? extends PortObject> portObjectClass = portType.getPortObjectClass();
				final NodeFactory<? extends NodeModel> nodeFactory;
				final NodeSettings nodeSettings = new NodeSettings("tmp_node_settings");
				// from SingleNodeContainer.CFG_VARIABLES
				nodeSettings.addConfig("variables");
				nodeSettings.addConfig(Node.CFG_MISC_SETTINGS);
				nodeSettings.addConfig(Node.CFG_MODEL);

				final String outputFileKey = "output_" + currentOutput;
				final Collection<VariableSetting> outputSettings = new LinkedList<VariableSetting>();
				if (DataTable.class.isAssignableFrom(portObjectClass)) {
					if (hasCsvWriterOutput(workflowManager, destNodeId)) {
						// we need to copy the settings from the output node
						workflowManager.saveNodeSettings(destNodeId, nodeSettings);
						nodeFactory = new CSVWriterNodeFactory();
					} else {
						nodeFactory = new WriteTableNodeFactory();
					}
					// "filename" applies both for WriteTableNodeModel and CSVWriterNodeModel
					outputSettings.add(new VariableSetting("filename", outputFileKey));
				} else if (IURIPortObject.class.isAssignableFrom(portObjectClass)) {
					nodeFactory = new OutputFileNodeFactory();
					outputSettings.add(new VariableSetting("FILENAME", outputFileKey));
					// TODO: do we need something like
					// outputSettings.add(new VariableSetting("FILE_EXTENSION", "extension_" + currentOutput));
				} else {
					nodeFactory = new PortObjectWriterNodeFactory(portType);
					outputSettings.add(new VariableSetting("filename", outputFileKey));
				}
				commandLineElements.add(buildFileAsFlowVariable(outputFileKey));
				final NodeID miniWorkflowDataNodeId = miniWorkflowManager.addNode(nodeFactory);

				addFlowVariables(nodeSettings, outputSettings);

				// save the settings in the data node
				miniWorkflowManager.loadNodeSettings(miniWorkflowDataNodeId, nodeSettings);
				// connect them
				miniWorkflowManager.addConnection(miniWorkflowNodeId, sourcePort, miniWorkflowDataNodeId, 1);
				// add output to the job
				final Output output = new Output();
				output.setName(outputFileKey);
				job.addOutput(output);
				currentOutput++;
			}
		}
		// we have now went through all of the inputs/outpus and added
		// needed nodes to provide/extract data,
		// we can now save the mini workflow
		miniWorkflowManager.save(miniWorkflowDir.toFile(), new ExecutionMonitor(), true);

		// compress the workflow folder into a zip file
		final Path miniWorkflowArchive = Files.createTempFile(sandboxDir, "knimejob_"
				+ nativeNodeContainer.getID().toString(), "zip");
		FileUtil.zipDir(miniWorkflowArchive.toFile(), miniWorkflowDir.toFile(), 9);
		commandLineElements.add(new CommandLineKNIMEWorkflowFile(miniWorkflowArchive.toFile()));
		// add the zipped workflow as input
		final Input input = new Input();
		input.setName("knime-workflow-zip");
		job.addInput(input);

		job.setCommandLine(commandLineElements);

		return job;
	}

	private void addFlowVariables(final NodeSettings nodeSettings, final Collection<VariableSetting> inputSettings)
			throws InvalidSettingsException {
		for (final VariableSetting inputSetting : inputSettings) {
			nodeSettings.getNodeSettings(Node.CFG_MODEL).addString(inputSetting.getSettingName(), "tmpvalue");
			final NodeSettings variableNodeSettings = nodeSettings.getNodeSettings("variables");
			final Config flowVariableConfig = variableNodeSettings.addConfig(inputSetting.getSettingName());
			flowVariableConfig.addString("used_variable", inputSetting.getFlowVariableName());
			// TODO: what is this for?
			flowVariableConfig.addString("exposed_variable", null);
		}
	}

	private CommandLineElement buildFileAsFlowVariable(final String name) {
		final FileParameter fileParameter = new FileParameter(name, "dummy");
		return new CommandLineFile(fileParameter, buildFlowVariablePrefix(name), FLOW_VARIABLE_SUFFIX);
	}

	private CommandLineElement buildStringParameterAsFlowVariable(final String name, final String value) {
		final StringParameter stringParameter = new StringParameter(name, value);
		return new CommandLineParameter(stringParameter, buildFlowVariablePrefix(name), FLOW_VARIABLE_SUFFIX);
	}

	private String buildFlowVariablePrefix(final String name) {
		return FLOW_VARIABLE_PREFIX + name + "\",";
	}

	private boolean hasCsvReaderSource(final WorkflowManager workflowManager, final NodeID sourceNodeId) {
		return nodeModelMatchesClass(workflowManager, sourceNodeId, CSVREADER_CLASS_NAME);
	}

	private boolean hasCsvWriterOutput(final WorkflowManager workflowManager, final NodeID destNodeId) {
		return nodeModelMatchesClass(workflowManager, destNodeId, CSVWRITER_CLASS_NAME);
	}

	private boolean nodeModelMatchesClass(final WorkflowManager workflowManager, final NodeID nodeId,
			final String className) {
		final NodeContainer node = workflowManager.getNodeContainer(nodeId);
		if (node instanceof NativeNodeContainer) {
			final NativeNodeContainer nativeNode = (NativeNodeContainer) node;
			if (className.equals(nativeNode.getNodeModel().getClass().getCanonicalName())) {
				return true;
			}
		}
		return false;
	}

}
