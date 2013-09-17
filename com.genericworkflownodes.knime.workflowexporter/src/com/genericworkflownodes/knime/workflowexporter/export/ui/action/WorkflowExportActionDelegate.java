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
package com.genericworkflownodes.knime.workflowexporter.export.ui.action;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.knime.workbench.editor2.WorkflowEditor;

import com.genericworkflownodes.knime.workflowexporter.export.KnimeWorkflowExporterProvider;
import com.genericworkflownodes.knime.workflowexporter.ui.wizard.WorkflowExportWizard;


/**
 * 
 * 
 * @author Luis de la Garza
 */
public class WorkflowExportActionDelegate implements IEditorActionDelegate {
	private static final int SIZING_WIZARD_WIDTH = 470;
	private static final int SIZING_WIZARD_HEIGHT = 550;

	private WorkflowEditor workflowEditor;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	@Override
	public void run(final IAction action) {
		if (workflowEditor == null) {
			throw new IllegalStateException(
					"There is no workflowEditor set! Cannot continue.");
		}
		final IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		if (workbenchWindow == null) {
			// not sure what should happen here
			return;
		}

		final WorkflowExportWizard wizard = new WorkflowExportWizard(
				workflowEditor, KnimeWorkflowExporterProvider.getInstance()
						.getAvailableExporters());

		final Shell parent = workbenchWindow.getShell();
		final WizardDialog dialog = new WizardDialog(parent, wizard);
		dialog.create();
		dialog.getShell().setSize(
				Math.max(SIZING_WIZARD_WIDTH, dialog.getShell().getSize().x),
				SIZING_WIZARD_HEIGHT);
		dialog.open();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action
	 * .IAction, org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void selectionChanged(final IAction action,
			final ISelection selection) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface
	 * .action.IAction, org.eclipse.ui.IEditorPart)
	 */
	@Override
	public void setActiveEditor(final IAction action,
			final IEditorPart targetEditor) {
		if (targetEditor == null) {
			return;
		}
		if (!(targetEditor instanceof WorkflowEditor)) {
			throw new IllegalArgumentException(
					"This action expects a WorkflowEditor");
		}
		workflowEditor = (WorkflowEditor) targetEditor;
	}

}
