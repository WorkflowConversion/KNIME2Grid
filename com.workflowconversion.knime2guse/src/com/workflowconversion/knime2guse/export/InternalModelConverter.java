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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.Validate;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.knime.base.node.io.csvreader.CSVReaderNodeFactory;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowCopyContent;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeInPortEditPart;
import org.knime.workbench.editor2.editparts.NodeOutPortEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import sun.swing.AccumulativeRunnable;

//import com.workflowconversion.knime2guse.generic_node.GenericKnimeNodeModel;
//import com.workflowconversion.knime2guse.nodes.flow.listzip.ListZipLoopEndNodeModel;
//import com.workflowconversion.knime2guse.nodes.flow.listzip.ListZipLoopStartNodeModel;
//import com.workflowconversion.knime2guse.nodes.io.importer.MimeFileImporterNodeModel;
import com.workflowconversion.knime2guse.exception.NodeFactoryClassNotFoundException;
import com.workflowconversion.knime2guse.model.Input;
import com.workflowconversion.knime2guse.model.Job;
import com.workflowconversion.knime2guse.model.Output;
import com.workflowconversion.knime2guse.model.Workflow;
import com.workflowconversion.knime2guse.model.Job.JobType;
import com.workflowconversion.knime2guse.model.Output.Destination;

/**
 * This class takes a workflow from the KNIME UI and transforms it to the
 * internal format.
 * 
 * @author Luis de la Garza
 */
public class InternalModelConverter {

    private final static NodeLogger LOGGER = NodeLogger.getLogger(InternalModelConverter.class);
    private final static WorkflowManager WORKFLOW_MANAGER = 
            WorkflowManager.ROOT.createAndAddProject("KNIME_WF_converter_tmp_wf", new WorkflowCreationHelper());
    
    static {
        WORKFLOW_MANAGER.addListener(new WorkflowListener() {            
            @Override
            public void workflowChanged(final WorkflowEvent event) {
                switch (event.getType()) {
                case NODE_ADDED:
                    int s = WORKFLOW_MANAGER.getNodeContainers().size();
                    String n =
                            ((WorkflowManager)event.getNewValue())
                                    .getName();
                    LOGGER.debug("Added project \"" + n
                            + "\" to (virtual) "
                            + "grid root workflow, total count: " + s);
                    break;
                case NODE_REMOVED:
                    s = WORKFLOW_MANAGER.getNodeContainers().size();
                    n = ((WorkflowManager)event.getOldValue()).getName();
                    LOGGER.debug("Removed project \"" + n + "\" from "
                            + "(virtual) grid root workflow, total count: "
                            + s);
                    break;
                default:
                }
            }
        });
    }
    
    private final WorkflowEditor editor;

    public InternalModelConverter(final WorkflowEditor editor) {
        Validate.notNull(editor, "editor cannot be null");
        this.editor = editor;
    }
    
    private <T extends NodeModel> T createNode(final Class<? extends NodeFactory<T>> factoryClass) throws CoreException {
        //final WorkflowManager manager = WorkflowManager.ROOT.createAndAddSubWorkflow(new PortType[0], new PortType[0], "test");
        // right now, create a single workflow composed of a CSVReader --> Filter --> CSVWriter
        final String pointID = "org.knime.workbench.repository.nodes";
        final String csvReaderFactoryClassName = "org.knime.base.node.io.csvreader.CSVReaderNodeFactory";
        final String csvWriterFactoryClassName = "org.knime.base.node.io.csvwriter.CSVWriterNodeFactory";
        final IExtensionRegistry registry = Platform.getExtensionRegistry();
        final IExtensionPoint point = registry.getExtensionPoint(pointID);
        for (IExtension ext : point.getExtensions()) {
            IConfigurationElement[] elements = ext.getConfigurationElements();
            for (IConfigurationElement e : elements) {
                if (factoryClass.getName().equals(e.getAttribute("factory-class"))) {
                    NodeFactory<T> f = (NodeFactory<T>)e.createExecutableExtension("factory-class");
                    return f.createNodeModel();
                }
            }
        }
        // if here, it means that we could not create the node... throw an exception, because
        // there is not much more to do anyway
        throw new NodeFactoryClassNotFoundException("Class [" + factoryClass.getName() + "] could not be found.");
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
                if (nc instanceof NativeNodeContainer) {
                    final Job jobWrapper = createJobWrapper((NativeNodeContainer)nc);
                    setGraphicElements(jobWrapper, (NodeContainerEditPart)ep);
                    
                    // create the knime mini-wf that will be executed via command line
                    final WorkflowCreationHelper creationHelper = new WorkflowCreationHelper();
                    final Path jobWrapperDir = Files.createTempDirectory(".gkn");
                    creationHelper.setWorkflowContext(new WorkflowContext.Factory(jobWrapperDir.toFile()).createContext());
                    final WorkflowManager tempWorkflow = 
                            WORKFLOW_MANAGER.createAndAddProject("Execution of " + nc.getNameWithID(), creationHelper);
                    
                    // copy the node from the current WF to the temp workflow
                    final WorkflowCopyContent content = new WorkflowCopyContent();
                    content.setNodeIDs(nc.getID());
                    final NodeID targetNodeID = tempWorkflow.copyFromAndPasteHere(nc.getParent(), content).getNodeIDs()[0];
                    final int targetNodeIDSuffix = targetNodeID.getIndex();
                    final NodeContainer targetNode = tempWorkflow.getNodeContainer(targetNodeID);
                    
                    // TODO: for nodes taking files, we need FileInput/Output not CSVReader/Writer
                    
                    // connect a CSVReader to each input
                    for (int i = 0, n = nc.getNrInPorts(); i < n; i++) {
                        
                    }
                    
                    // connect a CSVWriter to each output
                    for (int i = 0, n = nc.getNrOutPorts(); i < n; i++) {
                        
                    }
                    
                    // TODO: we need to add the flow variables as an input/output 
                    // file as well... perhaps add a
                    // couple of auxiliary nodes: one as "input flow variables" and one as
                    // "output flow variables" because we need to serialize this to disk
                    // somehow
                    
                    // NOTE: According to Thorsten, on our conversation on the 27.02. in Berlin
                    // NodeSettings can be written/read to/from an xml file
                    // Flow variables cannot be serialized, but dumped into a file (name, type, value)
                    // Using org.knime.base.node.io.table.read.ReadTableNodeFactory and
                    // org.knime.base.node.io.table.write.WriteTableNodeFactory should take care
                    // of knime data tables for all inputs/outputs... of course, for URIPortObjects
                    // we might want to use the file itself, and not the value of the URI ;)
                    
                    // for non-buffereddatatable I should use TableModel, ModelTable... also maybe
                    // take a look at how ports are serialized when written to disk when they
                    // are exported!
                    
                } else if(nc instanceof SubNodeContainer) {
                    // TODO: yeah... now what????
                    
                    // right now we can handle the parameters, but not the
                    // in-/output ports
                    // TODO: how to do this for non GKN???
//                    final SingleNodeContainer snc = (SingleNodeContainer) nc;
//                    final NodeModel nm = snc.getNodeModel();
//                    if (nm instanceof GenericKnimeNodeModel) {
//                        final GenericKnimeNodeModel gknModel = (GenericKnimeNodeModel) nm;
//                        for (final String key : gknModel.getNodeConfiguration().getParameterKeys()) {
//                            final Object paramValue = gknModel.getNodeConfiguration().getParameter(key).getValue();
//                            final String strValue = paramValue == null ? "" : paramValue.toString();
//                            job.setParam(key, strValue);
//                        }
//                    }
//                    workflow.addJob(job);
//                    jobMap.put(id, job);
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
//            if (sourceContainer instanceof SingleNodeContainer) {
//                final SingleNodeContainer snc = (SingleNodeContainer) sourceContainer;
//                if (snc.getNodeModel() instanceof GenericKnimeNodeModel) {
//                    // channel
//                    final GenericKnimeNodeModel sourceModel = (GenericKnimeNodeModel) snc.getNodeModel();
//                    // get the ports into arrays
//                    // the data is the one coming from the source port
//                    outputData = inputData = "output_" + sourceModel.getNodeConfiguration().getOutputPorts().get(sourcePortNr).getName()
//                            + '.' + sourceModel.getOutputType(sourcePortNr);
//                    outputName = sourceModel.getNodeConfiguration().getOutputPorts().get(sourcePortNr).getName();
//
//                } else if (snc.getNodeModel() instanceof MimeFileImporterNodeModel) {
//                    // input file
//                    final MimeFileImporterNodeModel nodeModel = (MimeFileImporterNodeModel) snc.getNodeModel();
//                    outputData = inputData = nodeModel.getContent();
//                }
//            }
//            // the input name is obtained from the target port
//            if (targetContainer instanceof SingleNodeContainer) {
//                final SingleNodeContainer snc = (SingleNodeContainer) targetContainer;
//                if (snc.getNodeModel() instanceof GenericKnimeNodeModel) {
//                    final GenericKnimeNodeModel targetModel = (GenericKnimeNodeModel) snc.getNodeModel();
//                    inputName = targetModel.getNodeConfiguration().getInputPorts().get(targetPortNr).getName();
//                }
//            }
//            input.setData(inputData);
//            input.setName(inputName);
//            final Output output = sourceJob.getOutput(sourcePortNr);
//            final Destination dest = new Destination(targetJob, targetPortNr);
//            output.addDestination(dest);
//            output.setData(outputData);
//            output.setName(outputName);
//
//            if (LOGGER.isDebugEnabled()) {
//                LOGGER.debug("Added connection: " + sourceJob + "->" + targetJob);
//            }
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
    
    private Job createJobWrapper(final NativeNodeContainer nativeNodeContainer) {
        final String name = nativeNodeContainer.getName();
        final NodeID id = nativeNodeContainer.getID();
        final String description = getNodeDescription(nativeNodeContainer);
        final Job.JobType jobType = getJobType(nativeNodeContainer);
        
        // the "real" ports do not include that weird flow variable
        // or whatever that is
        // anyways, node containers have always an extra in-/output
        // port, this is why nc.getNrInPorts() and
        // nc.getNrOutPorts() are deducted one
        
        // we define a "job wrapper" that will sit around the generated
        // knime mini-wf composed of one single node
        final Job jobWrapper = new Job();
        jobWrapper.setId(id.toString());
        jobWrapper.setName(name);
        jobWrapper.setDescription(description);
        jobWrapper.setJobType(jobType);
        
        return jobWrapper;
    }
    
    private void setGraphicElements(final Job jobWrapper, final NodeContainerEditPart editPart) {
        final Rectangle nodeBounds = editPart.getFigure().getBounds();
        jobWrapper.setX(nodeBounds.x);
        jobWrapper.setY(nodeBounds.y);
        
    }
    
    private JobType getJobType(final NativeNodeContainer nativeNodeContainer) {
        Job.JobType jobType = Job.JobType.Normal;
        if ("com.workflowconversion.knime2guse.nodes.flow.listzip.ListZipLoopEndNodeModel".equals(nativeNodeContainer.getNodeModel().getClass().getName())) {
            jobType = Job.JobType.Collector;
        }
        if ("com.workflowconversion.knime2guse.nodes.flow.listzip.ListZipLoopStartNodeModel".equals(nativeNodeContainer.getNodeModel().getClass().getName())) {
            jobType = Job.JobType.Generator;
        }
        return jobType;
    }

    private String getNodeDescription(final NativeNodeContainer nativeNodeContainer) {
        // we assume that all nodes have some sort of name
        String nodeDescription = nativeNodeContainer.getName();
        // let's try our luck and find for a nicer description
        final Element descriptionElement = nativeNodeContainer.getNode().getFactory().getXMLDescription();
        if (descriptionElement != null) {
            // extract the shortDescription element
            final NodeList nodeList = descriptionElement.getElementsByTagName("shortDescription");
            if (nodeList.getLength() > 0) {
                nodeDescription = nodeList.item(0).getTextContent();
            }
        }
        
        return nodeDescription;
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
