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

import org.apache.commons.lang.Validate;
import org.eclipse.ui.internal.dialogs.ExportWizard;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;

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

	private final Workflow workflow;
	// pages
	private final WorkflowExportPage workflowExportPage;
	private final ApplicationSelectionPage applicationSelectionPage;

	/**
	 * Constructor.
	 */
	public WorkflowExportWizard(final Workflow workflow, final Collection<KnimeWorkflowExporter> exporters) {
		Validate.notNull(workflow, "workflow is required and cannot be null");
		Validate.notEmpty(exporters, "exporter is required and cannot be null or empty");
		workflowExportPage = new WorkflowExportPage(exporters);
		applicationSelectionPage = new ApplicationSelectionPage(workflow);

		setWindowTitle("Export a workflow to other engines");
		setDefaultPageImageDescriptor(ImageRepository.getImageDescriptor(SharedImages.ExportBig));
		setNeedsProgressMonitor(true);

		this.workflow = workflow;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.internal.dialogs.ExportWizard#addPages()
	 */
	@Override
	public void addPages() {
		super.addPage(applicationSelectionPage);
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

		try {
			// perform the export
			final KnimeWorkflowExporter exporter = workflowExportPage.getSelectedExporter();
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Exporting using " + exporter + ", destinationFile=" + workflowExportPage.getDestinationFile());
			}
			exporter.export(workflow, new File(workflowExportPage.getDestinationFile()));
			return true;
		} catch (final Exception e) {
			LOGGER.error("Could not export workflow", e);
			e.printStackTrace();
			return false;
		}
	}
}
