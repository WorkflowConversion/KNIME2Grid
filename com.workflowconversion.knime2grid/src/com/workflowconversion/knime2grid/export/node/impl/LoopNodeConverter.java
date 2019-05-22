package com.workflowconversion.knime2grid.export.node.impl;

import java.io.File;

import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;

import com.workflowconversion.knime2grid.export.node.NodeContainerConverter;
import com.workflowconversion.knime2grid.export.workflow.ConverterUtils;
import com.workflowconversion.knime2grid.model.ConnectionType;
import com.workflowconversion.knime2grid.model.Input;
import com.workflowconversion.knime2grid.model.Job;
import com.workflowconversion.knime2grid.model.JobType;
import com.workflowconversion.knime2grid.model.Output;

/**
 * Converts nodes such as <i>ZipLoopStart</i> and <i>ZipLoopEnd</i>.
 * 
 * @author delagarza
 *
 */
public class LoopNodeConverter implements NodeContainerConverter {

	private final static String ZIPLOOPSTART_NODEMODEL_CLASS = "com.genericworkflownodes.knime.nodes.flow.listzip.ListZipLoopStartNodeModel";
	private final static String ZIPLOOPEND_NODEMODEL_CLASS = "com.genericworkflownodes.knime.nodes.flow.listzip.ListZipLoopEndNodeModel";

	@Override
	public boolean canHandle(final NativeNodeContainer nativeNodeContainer) {
		return ConverterUtils.nodeModelMatchesClass(nativeNodeContainer, ZIPLOOPSTART_NODEMODEL_CLASS)
				|| ConverterUtils.nodeModelMatchesClass(nativeNodeContainer, ZIPLOOPEND_NODEMODEL_CLASS);
	}

	@Override
	public Job convert(final NativeNodeContainer nativeNodeContainer, final WorkflowManager workflowManager, final File workingDirectory) throws Exception {
		final Job job = new Job();
		ConverterUtils.copyBasicInformation(job, nativeNodeContainer);
		boolean generator = false, collector = false;
		if (ConverterUtils.nodeModelMatchesClass(nativeNodeContainer, ZIPLOOPSTART_NODEMODEL_CLASS)) {
			job.setName("Generator");
			job.setJobType(JobType.Generator);
			generator = true;
		} else if (ConverterUtils.nodeModelMatchesClass(nativeNodeContainer, ZIPLOOPEND_NODEMODEL_CLASS)) {
			job.setName("Collector");
			job.setJobType(JobType.Collector);
			collector = true;
		}
		// go through the connections and create input/outputs
		for (final ConnectionContainer connectionContainer : workflowManager.getIncomingConnectionsFor(nativeNodeContainer.getID())) {
			final NodeID sourceID = connectionContainer.getSource();
			final Input input = new Input();
			input.setName("input");
			input.setSourceId(sourceID);
			input.setOriginalPortNr(connectionContainer.getDestPort());
			if (collector) {
				input.setConnectionType(ConnectionType.Collector);
			}
			job.addInput(input);
		}
		for (final ConnectionContainer connectionContainer : workflowManager.getOutgoingConnectionsFor(nativeNodeContainer.getID())) {
			final Output output = new Output();
			output.setName("output");
			output.setOriginalPortNr(connectionContainer.getSourcePort());
			if (generator) {
				output.setConnectionType(ConnectionType.Generator);
			}
			job.addOutput(output);
		}

		return job;
	}

}
