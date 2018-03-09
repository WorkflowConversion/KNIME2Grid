package com.workflowconversion.knime2grid.handlers;

import org.apache.commons.lang.Validate;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.knime.workbench.editor2.WorkflowEditor;

import com.workflowconversion.knime2grid.KnimeWorkflowExporterActivator;
import com.workflowconversion.knime2grid.export.workflow.KnimeWorkflowExporterProvider;
import com.workflowconversion.knime2grid.ui.wizard.WorkflowExportWizard;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class ConvertWorkflowHandler extends AbstractHandler {

	private static final int SIZING_WIZARD_WIDTH = 470;
	private static final int SIZING_WIZARD_HEIGHT = 550;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		Validate.notNull(workbenchWindow, "workbenchWindow is null. This is probably a bug and should be reported.");

		final Shell parent = workbenchWindow.getShell();
		final WorkflowEditor workflowEditor = (WorkflowEditor) workbenchWindow.getActivePage().getActiveEditor();
		if (workflowEditor == null) {
			MessageDialog.openInformation(parent, "KNIME - Workflow Conversion", "Please select a workflow editor.");
			return null;
		}

		final WorkflowExportWizard wizard = new WorkflowExportWizard(workflowEditor,
				KnimeWorkflowExporterProvider.getInstance().getWorkflowExporters(),
				KnimeWorkflowExporterProvider.getInstance().getNodeConverters(),
				KnimeWorkflowExporterProvider.getInstance().getSourceConverters());

		try {
			wizard.validateWorkflowBeforeExport();
		} catch (final Exception e) {
			// show an error window
			final IStatus status = new Status(IStatus.ERROR, KnimeWorkflowExporterActivator.PLUGIN_ID,
					"Workflow is not valid for conversion.");
			ErrorDialog.openError(parent, "KNIME - Workflow Conversion",
					"Could not convert workflow. Reason:\n" + e.getMessage(), status);
			return null;
		}
		final WizardDialog dialog = new WizardDialog(parent, wizard);
		dialog.create();
		dialog.getShell().setSize(Math.max(SIZING_WIZARD_WIDTH, dialog.getShell().getSize().x), SIZING_WIZARD_HEIGHT);
		dialog.open();

		// according to the javadoc, return value must be null
		return null;
	}
}
