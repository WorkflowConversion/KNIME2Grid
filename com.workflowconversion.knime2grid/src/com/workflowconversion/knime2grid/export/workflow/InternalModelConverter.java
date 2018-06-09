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
package com.workflowconversion.knime2grid.export.workflow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.Validate;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeInPortEditPart;
import org.knime.workbench.editor2.editparts.NodeOutPortEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;

import com.genericworkflownodes.knime.parameter.IFileParameter;
import com.workflowconversion.knime2grid.export.io.SourceConverter;
import com.workflowconversion.knime2grid.export.node.NodeContainerConverter;
import com.workflowconversion.knime2grid.model.ConnectionType;
import com.workflowconversion.knime2grid.model.GraphicElement;
import com.workflowconversion.knime2grid.model.Input;
import com.workflowconversion.knime2grid.model.Job;
import com.workflowconversion.knime2grid.model.Output;
import com.workflowconversion.knime2grid.model.Output.Destination;
import com.workflowconversion.knime2grid.model.Workflow;

/**
 * This class takes a workflow from the KNIME UI and transforms it to the internal format.
 * 
 * @author Luis de la Garza
 */
public class InternalModelConverter {

	private final static NodeLogger LOGGER = NodeLogger.getLogger(InternalModelConverter.class);

	private final WorkflowEditor editor;
	private final Collection<NodeContainerConverter> nodeConverters;
	private final Collection<SourceConverter> sourceConverters;

	public InternalModelConverter(final WorkflowEditor editor, final Collection<NodeContainerConverter> handlers,
			final Collection<SourceConverter> sourceConverters) {
		Validate.notNull(editor, "editor cannot be null");
		this.editor = editor;
		this.nodeConverters = handlers;
		this.sourceConverters = sourceConverters;
	}

	public Workflow convert() throws Exception {
		final Optional<WorkflowManager> workflowManagerWrapper = editor.getWorkflowManager();
		if (!workflowManagerWrapper.isPresent()) {
			throw new NullPointerException(
					"editor.getWorkflowManager() returned an empty Optional<WorkflowManager>. This seems to be a bug and should be reported.");
		}
		final WorkflowManager workflowManager = workflowManagerWrapper.get();
		final Workflow workflow = new Workflow();

		// 1. convert the nodes (inputs/outputs will be created, but their type will be Unassigned)
		convertNodes(workflowManager, workflow);

		// 2. connect inputs/outputs
		convertEdges(workflowManager, workflow);

		// 3. handle all inputs that were not set as channels
		handleUnassignedInputs(workflowManager, workflow);

		// 4. go through all of the jobs to obtain the coordinates of the input/output ports
		setGraphicalElements(workflow, editor);

		return workflow;
	}

	private void convertNodes(final WorkflowManager workflowManager, final Workflow workflow) throws IOException, Exception {
		for (final NodeContainer nc : workflowManager.getNodeContainers()) {
			if (nc instanceof NativeNodeContainer) {
				final NativeNodeContainer nativeNodeContainer = (NativeNodeContainer) nc;
				final File workingDirectory = Files
						.createTempDirectory("knime2guse_" + ConverterUtils.fixNodeIdForFileSystem(nativeNodeContainer.getID().toString())).toFile();
				if (isProcessingNode(nativeNodeContainer)) {
					// go through all registered handlers
					Job convertedJob = null;
					for (final NodeContainerConverter handler : nodeConverters) {
						if (handler.canHandle(nativeNodeContainer)) {
							convertedJob = handler.convert(nativeNodeContainer, workflowManager, workingDirectory);
							break;
						}
					}
					if (convertedJob == null) {
						throw new RuntimeException("Got a null job when converting node: " + nativeNodeContainer);
					}
					// we only have the jobs, without connections, this will be
					// done later on
					workflow.addJob(convertedJob);
				}
			} else if (nc instanceof WorkflowManager) {
				// TODO: expand metanodes
			}
		}
	}

	private void convertEdges(final WorkflowManager workflowManager, final Workflow workflow) {
		for (final ConnectionContainer connectionContainer : workflowManager.getConnectionContainers()) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Processing connection: " + connectionContainer.getSource() + "->" + connectionContainer.getDest());
			}
			final Job sourceJob = workflow.getJob(connectionContainer.getSource());
			final Job targetJob = workflow.getJob(connectionContainer.getDest());

			if (sourceJob != null && targetJob != null) {
				final Input input = targetJob.getInputByOriginalPortNr(connectionContainer.getDestPort());
				final Output output = sourceJob.getOutputByOriginalPortNr(connectionContainer.getSourcePort());
				input.setConnectionType(ConnectionType.Channel);
				input.setSourcePortNr(output.getPortNr());
				input.setSourceId(connectionContainer.getSource());
				output.addDestination(new Destination(targetJob, input.getPortNr()));
			}
		}
	}

	private void handleUnassignedInputs(final WorkflowManager workflowManager, final Workflow workflow) throws Exception {
		for (final Job job : workflow.getJobs()) {
			for (final Input input : job.getInputs()) {
				if (input.getConnectionType() == ConnectionType.NotAssigned) {
					final NodeID originalSourceID = input.getSourceId();
					final NodeContainer originalSource = workflowManager.getNodeContainer(originalSourceID);
					IFileParameter inputData = null;
					for (final SourceConverter sourceConverter : sourceConverters) {
						if (sourceConverter.canHandle(originalSource)) {
							inputData = sourceConverter.convert(originalSource, workflowManager);
							break;
						}
					}
					if (inputData == null) {
						throw new RuntimeException("The input could not be converted. This is probably a bug and should be reported!");
					}
					input.setConnectionType(ConnectionType.UserProvided);
					input.setData(inputData);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void setGraphicalElements(final Workflow workflow, final WorkflowEditor editor) {
		final WorkflowRootEditPart workflowRootEditPart = (WorkflowRootEditPart) editor.getViewer().getRootEditPart().getChildren().get(0);
		final IFigure figure = workflowRootEditPart.getFigure();
		final Rectangle bounds = figure.getBounds();
		workflow.setHeight(bounds.height);
		workflow.setWidth(bounds.width);

		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;

		for (final EditPart ep : (List<EditPart>) workflowRootEditPart.getChildren()) {
			if (ep instanceof NodeContainerEditPart) {
				final NodeContainerUI nc = ((NodeContainerEditPart) ep).getNodeContainer();

				final Job job = workflow.getJob(nc.getID());
				if (job != null) {
					Rectangle rectangle = ((AbstractGraphicalEditPart) ep).getFigure().getBounds();
					if (rectangle.x < minX) {
						minX = rectangle.x;
					}
					if (rectangle.y < minY) {
						minY = rectangle.y;
					}
					setCoordinates(job, rectangle);
					for (final Object o : ep.getChildren()) {
						if (o instanceof NodeInPortEditPart) {
							final NodeInPortEditPart inPort = (NodeInPortEditPart) o;
							final Input input = job.getInputByOriginalPortNr(inPort.getIndex());
							if (input != null) {
								rectangle = inPort.getFigure().getBounds();
								setCoordinates(input, rectangle);
								if (rectangle.x() < minX) {
									minX = rectangle.x();
								}
								if (rectangle.y() < minY) {
									minY = rectangle.y();
								}
							}
						} else if (o instanceof NodeOutPortEditPart) {
							final NodeOutPortEditPart outPort = (NodeOutPortEditPart) o;
							final Output output = job.getOutputByOriginalPortNr(outPort.getIndex());
							if (output != null) {
								rectangle = outPort.getFigure().getBounds();
								setCoordinates(output, rectangle);
								if (rectangle.x() < minX) {
									minX = rectangle.x();
								}
								if (rectangle.y() < minY) {
									minY = rectangle.y();
								}
							}
						}
					}
				}
			}
		}
		// use minX and minY as offset
		final Collection<GraphicElement> graphicElements = new LinkedList<GraphicElement>();
		for (final Job job : workflow.getJobs()) {
			graphicElements.add(job);
			graphicElements.addAll(job.getInputs());
			graphicElements.addAll(job.getOutputs());
		}
		for (final GraphicElement e : graphicElements) {
			e.setX(e.getX() - minX);
			e.setY(e.getY() - minY);
		}
	}

	// returns true for nodes that receive at least one input and produce at
	// least one output
	private boolean isProcessingNode(final NodeContainer nc) {
		// getNrInPorts/getNrOutPorts returns the number of ports + 1 in order
		// to account for flow variables
		return (nc.getNrInPorts() > 1 && nc.getNrOutPorts() > 1);
		// fun fact: an alternative is to inspect the node model / factory
		// final Set<String> nonProcessingFactories = new TreeSet<String>();
		// nonProcessingFactories.add(CSVWriterNodeFactory.class.getName());
		// nonProcessingFactories.add(CSVReaderNodeFactory.class.getName());
		// nonProcessingFactories.add("com.genericworkflownodes.knime.nodes.io.importer.MimeFileImporterNodeFactory");
		// nonProcessingFactories.add("com.genericworkflownodes.knime.nodes.io.outputfile.OutputFileNodeFactory");
		// ...
		//
		// return
		// !nonProcessingFactories.contains(nc.getNode().getFactory().getClass().getName());
	}

	private void setCoordinates(final GraphicElement graphicElement, final Rectangle rectangle) {
		graphicElement.setX(rectangle.x);
		graphicElement.setY(rectangle.y);

	}
}
