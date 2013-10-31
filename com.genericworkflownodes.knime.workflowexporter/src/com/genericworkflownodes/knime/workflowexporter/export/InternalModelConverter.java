/**
 * Copyright (c) 2013, Luis de la Garza.
 *
 * This file is part of KnimeWorkflowExporter.
 * 
 * GenericKnimeNodes is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.genericworkflownodes.knime.workflowexporter.export;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.Validate;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeInPortEditPart;
import org.knime.workbench.editor2.editparts.NodeOutPortEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;

import com.genericworkflownodes.knime.generic_node.GenericKnimeNodeModel;
import com.genericworkflownodes.knime.nodes.flow.listzip.ListZipLoopEndNodeModel;
import com.genericworkflownodes.knime.nodes.flow.listzip.ListZipLoopStartNodeModel;
import com.genericworkflownodes.knime.nodes.io.importer.MimeFileImporterNodeModel;
import com.genericworkflownodes.knime.workflowexporter.model.Input;
import com.genericworkflownodes.knime.workflowexporter.model.Job;
import com.genericworkflownodes.knime.workflowexporter.model.Output;
import com.genericworkflownodes.knime.workflowexporter.model.Output.Destination;
import com.genericworkflownodes.knime.workflowexporter.model.Workflow;

/**
 * This class takes a workflow from the KNIME UI and transforms it to the
 * internal format.
 * 
 * @author Luis de la Garza
 */
public class InternalModelConverter {

    private final static NodeLogger LOGGER = NodeLogger.getLogger(InternalModelConverter.class);
    private final WorkflowEditor editor;

    public InternalModelConverter(final WorkflowEditor editor) {
	Validate.notNull(editor, "editor cannot be null");
	this.editor = editor;
    }

    public Workflow convert() throws Exception {
	final WorkflowRootEditPart workflowRootEditPart = (WorkflowRootEditPart) editor.getViewer().getRootEditPart().getChildren().get(0);

	final IFigure figure = workflowRootEditPart.getFigure();
	final Rectangle bounds = figure.getBounds();

	final Workflow workflow = new Workflow();
	workflow.setHeight(bounds.height);
	workflow.setWidth(bounds.width);

	// we will use this map to locate the jobs when we are going through the
	// connections
	final Map<NodeID, Job> jobMap = new TreeMap<NodeID, Job>();

	// go through the nodes first
	// we need to go through the edit parts because the coordinates
	// are needed
	@SuppressWarnings("unchecked")
	final List<EditPart> children = workflowRootEditPart.getChildren();
	for (final EditPart ep : children) {
	    if (ep instanceof NodeContainerEditPart) {
		if (LOGGER.isDebugEnabled()) {
		    LOGGER.debug("Converting node: " + ep.toString());
		}
		final NodeContainer nc = ((NodeContainerEditPart) ep).getNodeContainer();
		// TODO: extract the properties from the nodes in a more elegant
		// way!
		// TODO: use CTDs? generate command line?
		if (nc instanceof SingleNodeContainer) {
		    final String name = nc.getName();
		    final NodeID id = nc.getID();
		    final Rectangle nodeBounds = ((NodeContainerEditPart) ep).getFigure().getBounds();
		    String description = "Node " + name;
		    Job.JobType jobType = Job.JobType.Normal;
		    if (((SingleNodeContainer) nc).getNodeModel() instanceof GenericKnimeNodeModel) {
			final GenericKnimeNodeModel nodeModel = (GenericKnimeNodeModel) ((SingleNodeContainer) nc).getNodeModel();
			description = nodeModel.getNodeConfiguration().getDescription();
		    }
		    if (((SingleNodeContainer) nc).getNodeModel() instanceof ListZipLoopEndNodeModel) {
			jobType = Job.JobType.Collector;
		    }
		    if (((SingleNodeContainer) nc).getNodeModel() instanceof ListZipLoopStartNodeModel) {
			jobType = Job.JobType.Generator;
		    }
		    // the "real" ports do not include that weird flow variable
		    // or whatever that is
		    // anyways, node containers have always an extra in-/output
		    // port, this is why nc.getNrInPorts() and
		    // nc.getNrOutPorts() are deducted one
		    final Job job = new Job();
		    job.setX(nodeBounds.x);
		    job.setY(nodeBounds.y);
		    job.setId(id.toString());
		    job.setName(name);
		    job.setDescription(description);
		    job.setJobType(jobType);
		    // right now we can handle the parameters, but not the
		    // in-/output ports
		    // TODO: how to do this for non GKN???
		    final SingleNodeContainer snc = (SingleNodeContainer) nc;
		    final NodeModel nm = snc.getNodeModel();
		    if (nm instanceof GenericKnimeNodeModel) {
			final GenericKnimeNodeModel gknModel = (GenericKnimeNodeModel) nm;
			for (final String key : gknModel.getNodeConfiguration().getParameterKeys()) {
			    final Object paramValue = gknModel.getNodeConfiguration().getParameter(key).getValue();
			    final String strValue = paramValue == null ? "" : paramValue.toString();
			    job.setParam(key, strValue);
			}
		    }
		    workflow.addJob(job);
		    jobMap.put(id, job);
		}

	    }
	}

	// go through the connections
	// in this case, coordinates are not needed and we can use the more
	// suitable workflowManager
	final WorkflowManager workflowManager = workflowRootEditPart.getWorkflowManager();
	// we need to handle output ports that are connected to several
	// input ports
	for (final ConnectionContainer connectionContainer : workflowManager.getConnectionContainers()) {
	    if (LOGGER.isDebugEnabled()) {
		LOGGER.debug("Processing connection: " + connectionContainer.getSource() + "->" + connectionContainer.getDest());
	    }
	    final Job sourceJob = jobMap.get(connectionContainer.getSource());
	    final Job targetJob = jobMap.get(connectionContainer.getDest());
	    Validate.notNull(sourceJob, "source node was not found");
	    Validate.notNull(targetJob, "destination node was not found");

	    final int sourcePortNr = connectionContainer.getSourcePort() - 1;
	    final int targetPortNr = connectionContainer.getDestPort() - 1;

	    final Input input = targetJob.getInput(targetPortNr);
	    input.setSource(sourceJob);
	    input.setSourcePortNr(sourcePortNr);
	    final NodeContainer sourceContainer = workflowManager.getNodeContainer(connectionContainer.getSource());
	    final NodeContainer targetContainer = workflowManager.getNodeContainer(connectionContainer.getDest());
	    String inputName = "in." + targetPortNr;
	    String outputName = "out." + sourcePortNr;
	    Object inputData = null;
	    Object outputData = null;
	    if (sourceContainer instanceof SingleNodeContainer) {
		final SingleNodeContainer snc = (SingleNodeContainer) sourceContainer;
		if (snc.getNodeModel() instanceof GenericKnimeNodeModel) {
		    // channel
		    final GenericKnimeNodeModel sourceModel = (GenericKnimeNodeModel) snc.getNodeModel();
		    // get the ports into arrays
		    // the data is the one coming from the source port
		    outputData = inputData = "output_" + sourceModel.getNodeConfiguration().getOutputPorts().get(sourcePortNr).getName()
			    + '.' + sourceModel.getOutputType(sourcePortNr);
		    outputName = sourceModel.getNodeConfiguration().getOutputPorts().get(sourcePortNr).getName();

		} else if (snc.getNodeModel() instanceof MimeFileImporterNodeModel) {
		    // input file
		    final MimeFileImporterNodeModel nodeModel = (MimeFileImporterNodeModel) snc.getNodeModel();
		    outputData = inputData = nodeModel.getFilename();
		}
	    }
	    // the input name is obtained from the target port
	    if (targetContainer instanceof SingleNodeContainer) {
		final SingleNodeContainer snc = (SingleNodeContainer) targetContainer;
		if (snc.getNodeModel() instanceof GenericKnimeNodeModel) {
		    final GenericKnimeNodeModel targetModel = (GenericKnimeNodeModel) snc.getNodeModel();
		    inputName = targetModel.getNodeConfiguration().getInputPorts().get(targetPortNr).getName();
		}
	    }
	    input.setData(inputData);
	    input.setName(inputName);
	    final Output output = sourceJob.getOutput(sourcePortNr);
	    final Destination dest = new Destination(targetJob, targetPortNr);
	    output.addDestination(dest);
	    output.setData(outputData);
	    output.setName(outputName);

	    if (LOGGER.isDebugEnabled()) {
		LOGGER.debug("Added connection: " + sourceJob + "->" + targetJob);
	    }
	}

	// parse once again to obtain the coordinates of the input/output ports
	for (final EditPart ep : children) {
	    if (ep instanceof NodeContainerEditPart) {
		final NodeContainer nc = ((NodeContainerEditPart) ep).getNodeContainer();
		final Job job = jobMap.get(nc.getID());
		for (final Object o : ep.getChildren()) {
		    if (o instanceof NodeInPortEditPart) {
			final NodeInPortEditPart inPort = (NodeInPortEditPart) o;
			if (inPort.getIndex() == 0) {
			    // flow variable... ignore
			    continue;
			}
			final Input input = job.getInput(inPort.getIndex() - 1);
			final Rectangle rect = inPort.getFigure().getBounds();
			input.setX(rect.x);
			input.setY(rect.y);
		    } else if (o instanceof NodeOutPortEditPart) {
			final NodeOutPortEditPart outPort = (NodeOutPortEditPart) o;
			if (outPort.getIndex() == 0) {
			    // flow variable;
			    continue;
			}
			final Output output = job.getOutput(outPort.getIndex() - 1);
			final Rectangle rect = outPort.getFigure().getBounds();
			output.setX(rect.x);
			output.setY(rect.y);
		    }
		}
	    }
	}

	// validateWorkflow(workflow);

	return workflow;
    }

    /**
     * @param workflow
     */
    private void validateWorkflow(final Workflow workflow) {
	// check that there are not "loose" connections
	for (final Job job : workflow.getJobs()) {
	    for (int i = 0, n = job.getNrInputs(); i < n; i++) {
		final Input input = job.getInput(i);
		Validate.notNull(input.getSource(), "Invalid input port");
		Validate.isTrue(input.getSourcePortNr() > -1, "Invalid source port number", input.getSourcePortNr());
	    }
	    for (int i = 0, n = job.getNrOutputs(); i < n; i++) {
		final Output output = job.getOutput(i);
		for (final Destination destination : output.getDestinations()) {
		    Validate.notNull(destination.getTarget(), "Invalid output");
		    Validate.isTrue(destination.getTargetPortNr() > -1, "Invalid target port number", destination.getTargetPortNr());
		}
	    }
	}
    }

}
