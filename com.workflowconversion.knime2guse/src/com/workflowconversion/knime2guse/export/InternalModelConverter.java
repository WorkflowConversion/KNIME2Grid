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
package com.workflowconversion.knime2guse.export;

import java.io.File;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.Validate;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeInPortEditPart;
import org.knime.workbench.editor2.editparts.NodeOutPortEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;

import com.workflowconversion.knime2guse.export.handler.NodeContainerConverter;
import com.workflowconversion.knime2guse.model.Input;
import com.workflowconversion.knime2guse.model.Job;
import com.workflowconversion.knime2guse.model.Output;
import com.workflowconversion.knime2guse.model.Workflow;

/**
 * This class takes a workflow from the KNIME UI and transforms it to the internal format.
 * 
 * @author Luis de la Garza
 */
public class InternalModelConverter {

	private final static NodeLogger LOGGER = NodeLogger.getLogger(InternalModelConverter.class);

	private final WorkflowEditor editor;
	private final Collection<NodeContainerConverter> handlers;

	public InternalModelConverter(final WorkflowEditor editor, final Collection<NodeContainerConverter> handlers) {
		Validate.notNull(editor, "editor cannot be null");
		this.editor = editor;
		this.handlers = handlers;
	}

	public Workflow convert() throws Exception {
		final WorkflowRootEditPart workflowRootEditPart = (WorkflowRootEditPart) editor.getViewer().getRootEditPart()
				.getChildren().get(0);
		final WorkflowManager workflowManager = editor.getWorkflowManager();

		final IFigure figure = workflowRootEditPart.getFigure();
		final Rectangle bounds = figure.getBounds();

		final Workflow workflow = new Workflow();
		workflow.setHeight(bounds.height);
		workflow.setWidth(bounds.width);

		// we will use this map to locate the jobs when we are going through the connections
		final Map<NodeID, Job> jobMap = new TreeMap<NodeID, Job>();

		// go through the nodes first we need to go through the edit parts because the coordinates are needed
		@SuppressWarnings("unchecked")
		final List<EditPart> children = workflowRootEditPart.getChildren();
		for (final EditPart ep : children) {
			if (ep instanceof NodeContainerEditPart) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Converting node: " + ep.toString());
				}
				final NodeContainer nc = ((NodeContainerEditPart) ep).getNodeContainer();
				// we use this in order to check for null after going through
				// all handlers... it is possible that a handler
				// will return null and we will never know!
				final Job dummyJob = new Job();

				if (nc instanceof NativeNodeContainer) {
					final NativeNodeContainer nativeNodeContainer = (NativeNodeContainer) nc;
					final File workingDirectory = Files.createTempDirectory(
							"knime2guse_" + nativeNodeContainer.getID().toString()).toFile();
					if (isProcessingNode(nativeNodeContainer)) {
						// go through all registered handlers
						Job convertedJob = dummyJob;
						for (final NodeContainerConverter handler : handlers) {
							if (handler.canHandle(nativeNodeContainer)) {
								convertedJob = handler.convert(nativeNodeContainer, workflowManager, workingDirectory);
							}
						}
						// if the job has not been converted, then it must be assigned to dummyJob
						if (convertedJob == dummyJob) {
							// something went wrong... the job was not handled properly
							throw new RuntimeException("Got a null job when converting node: " + nativeNodeContainer);
						}
						setGraphicElements(convertedJob, (NodeContainerEditPart) ep);
						// we only have the jobs, without connections, this will be done later on
						jobMap.put(nc.getID(), convertedJob);
						workflow.addJob(convertedJob);
					} else {
						// TODO: nodes that have only inputs/outputs are quite
						// likely data nodes...
					}
				} else if (nc instanceof SubNodeContainer) {
					// TODO: yeah... now what???? can one "expand" subnodes?
				}
			}
		}

		// go through the connections and connect the inputs/outputs of converted jobs
		for (final ConnectionContainer connectionContainer : workflowManager.getConnectionContainers()) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Processing connection: " + connectionContainer.getSource() + "->"
						+ connectionContainer.getDest());
			}
			final Job sourceJob = jobMap.get(connectionContainer.getSource());
			final Job targetJob = jobMap.get(connectionContainer.getDest());

			// we need to substract one due to flowports
			final int sourcePortNr = connectionContainer.getSourcePort() - 1;
			final int targetPortNr = connectionContainer.getDestPort() - 1;

			final Input input = targetJob.getInput(targetPortNr);
			input.setSource(sourceJob, sourcePortNr);
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

		return workflow;
	}

	// returns true for nodes that are NOT i/o, that is, nodes that do
	// processing and that need to be fancily packed into a knime-wf
	// TODO: maybe state that a processing node is such a node that has at least
	// one input AND at least one output?
	private boolean isProcessingNode(NodeContainer nc) {
		// getNrInPorts/getNrOutPorts returns the number of ports + 1 in order to account for flow variables
		return (nc.getNrInPorts() > 1 && nc.getNrOutPorts() > 1);
		// TODO: an alternative is to inspect the node model / factory
		// final Set<String> nonProcessingFactories = new TreeSet<String>();
		// nonProcessingFactories.add(CSVWriterNodeFactory.class.getName());
		// nonProcessingFactories.add(CSVReaderNodeFactory.class.getName());
		// nonProcessingFactories.add("com.genericworkflownodes.knime.nodes.io.importer.MimeFileImporterNodeFactory");
		// nonProcessingFactories.add("com.genericworkflownodes.knime.nodes.io.outputfile.OutputFileNodeFactory");
		//
		// return
		// !nonProcessingFactories.contains(nc.getNode().getFactory().getClass().getName());
	}

	private void setGraphicElements(final Job jobWrapper, final NodeContainerEditPart editPart) {
		final Rectangle nodeBounds = editPart.getFigure().getBounds();
		jobWrapper.setX(nodeBounds.x);
		jobWrapper.setY(nodeBounds.y);

	}
}
