/**
 * Copyright (c) 2012, Luis de la Garza.
 *
 * This file is part of testFragment.
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
package com.workflowconversion.knime2grid.ui.wizard;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;

import org.apache.commons.lang.Validate;
import org.eclipse.ui.internal.dialogs.ExportWizard;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainerState;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.editor2.WorkflowEditor;

import com.workflowconversion.knime2grid.export.io.SourceConverter;
import com.workflowconversion.knime2grid.export.node.NodeContainerConverter;
import com.workflowconversion.knime2grid.export.workflow.InternalModelConverter;
import com.workflowconversion.knime2grid.export.workflow.KnimeWorkflowExporter;
import com.workflowconversion.knime2grid.model.Workflow;

/**
 * 
 * 
 * @author Luis de la Garza
 */
// Eclipse discourages to use ExportWizard because of its dependencies.
// Since this fragment/plug-in is to be used in KNIME, there is no danger
// that the required dependency won't be included
@SuppressWarnings("restriction")
public class WorkflowExportWizard extends ExportWizard {

	protected static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowExportWizard.class);

	private final WorkflowEditor workflowEditor;
	private final Collection<NodeContainerConverter> nodeConverters;
	private final Collection<SourceConverter> sourceConverters;
	// pages
	private final WorkflowExportPage workflowExportPage;

	/**
	 * Constructor.
	 */
	public WorkflowExportWizard(final WorkflowEditor workflowEditor, final Collection<KnimeWorkflowExporter> exporters,
			final Collection<NodeContainerConverter> nodeConverters,
			final Collection<SourceConverter> sourceConverters) {
		Validate.notNull(workflowEditor, "workflowEditor is required and cannot be null");
		Validate.notEmpty(exporters, "exporter is required and cannot be null or empty");
		workflowExportPage = new WorkflowExportPage(exporters);

		setWindowTitle("Export a Workflow to other platforms");
		setDefaultPageImageDescriptor(ImageRepository.getImageDescriptor(SharedImages.ExportBig));
		setNeedsProgressMonitor(true);

		this.workflowEditor = workflowEditor;
		this.nodeConverters = nodeConverters;
		this.sourceConverters = sourceConverters;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.internal.dialogs.ExportWizard#addPages()
	 */
	@Override
	public void addPages() {
		super.addPage(workflowExportPage);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.internal.dialogs.ExportWizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		// check if we can finish in the first place
		if (!canFinish()) {
			return false;
		}

		// Obtain currently active workflow editor
		final InternalModelConverter converter = new InternalModelConverter(workflowEditor, nodeConverters,
				sourceConverters);
		try {
			final Workflow workflow = converter.convert();
			// perform the export
			final KnimeWorkflowExporter exporter = workflowExportPage.getSelectedExporter();
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info(
						"Exporting using " + exporter + ", destinationFile=" + workflowExportPage.getDestinationFile());
			}
			exporter.export(workflow, new File(workflowExportPage.getDestinationFile()));
			return true;
		} catch (final Exception e) {
			LOGGER.error("Could not export workflow", e);
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean canFinish() {
		boolean canFinish = super.canFinish();
		if (canFinish) {
			try {
				validateWorkflowBeforeExport();
			} catch (final Exception e) {
				canFinish = false;
			}
		}
		return canFinish;
	}

	public void validateWorkflowBeforeExport() {
		final Optional<WorkflowManager> workflowManagerWrapper = workflowEditor.getWorkflowManager();
		if (!workflowManagerWrapper.isPresent()) {
			throw new NullPointerException(
					"workflowEditor.getWorkflowManager() returned an empty Optional<WorkflowManager>. This is probably a bug and should be reported.");
		}
		final WorkflowManager workflowManager = workflowManagerWrapper.get();
		// check that each node is at least valid
		final Collection<String> invalidNodes = new LinkedList<String>();
		for (final NodeContainer nodeContainer : workflowManager.getNodeContainers()) {
			final NodeContainerState state = nodeContainer.getNodeContainerState();
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Node " + nodeContainer + " is in state " + state);
			}
			// allow only properly configured / already executed nodes to be
			// converted
			if (!(state.isConfigured() || state.isExecuted())) {
				invalidNodes.add(nodeContainer.getNameWithID());
			}
		}
		if (!invalidNodes.isEmpty()) {
			final String newLine = System.getProperty("line.separator");
			final StringBuilder error = new StringBuilder(
					"The workflow cannot be converted because the following node(s) have not been properly configured:");
			error.append(newLine).append(newLine);
			boolean first = true;
			for (final String invalidNode : invalidNodes) {
				// prepend a comma to all elements, except for the first one
				if (!first) {
					error.append(", ");
				}
				error.append(invalidNode);
				first = false;
			}
			throw new RuntimeException(error.toString());
		}
	}

}
