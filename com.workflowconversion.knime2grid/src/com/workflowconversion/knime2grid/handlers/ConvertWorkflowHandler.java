package com.workflowconversion.knime2grid.handlers;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;

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
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainerState;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.WorkflowEditor;

import com.workflowconversion.knime2grid.KnimeWorkflowExporterActivator;
import com.workflowconversion.knime2grid.export.io.SourceConverter;
import com.workflowconversion.knime2grid.export.node.NodeContainerConverter;
import com.workflowconversion.knime2grid.export.workflow.InternalModelConverter;
import com.workflowconversion.knime2grid.export.workflow.KnimeWorkflowExporterProvider;
import com.workflowconversion.knime2grid.model.Workflow;
import com.workflowconversion.knime2grid.ui.wizard.WorkflowExportWizard;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class ConvertWorkflowHandler extends AbstractHandler {

	private static final NodeLogger LOG = NodeLogger.getLogger(ConvertWorkflowHandler.class);
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

		final Workflow workflow;
		try {
			workflow = extractWorkflowFromEditor(workflowEditor, KnimeWorkflowExporterProvider.getInstance().getNodeConverters(),
					KnimeWorkflowExporterProvider.getInstance().getSourceConverters());
		} catch (final Exception e) {
			final IStatus status = new Status(IStatus.ERROR, KnimeWorkflowExporterActivator.PLUGIN_ID, "Workflow is not valid for conversion.");
			ErrorDialog.openError(parent, "KNIME - Workflow Conversion", "Could not convert workflow. Reason:\n" + e.getMessage(), status);
			return null;
		}

		final WorkflowExportWizard wizard = new WorkflowExportWizard(workflow, KnimeWorkflowExporterProvider.getInstance().getWorkflowExporters());

		final WizardDialog dialog = new WizardDialog(parent, wizard);
		dialog.create();
		dialog.getShell().setSize(Math.max(SIZING_WIZARD_WIDTH, dialog.getShell().getSize().x), SIZING_WIZARD_HEIGHT);
		dialog.open();

		// according to the javadoc, return value must be null
		return null;
	}

	private Workflow extractWorkflowFromEditor(final WorkflowEditor workflowEditor, final Collection<NodeContainerConverter> nodeConverters,
			final Collection<SourceConverter> sourceConverters) throws Exception {
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
			if (LOG.isDebugEnabled()) {
				LOG.debug("Node " + nodeContainer + " is in state " + state);
			}
			// allow only properly configured / already executed nodes to be
			// converted
			if (!(state.isConfigured() || state.isExecuted())) {
				invalidNodes.add(nodeContainer.getNameWithID());
			}
		}
		if (!invalidNodes.isEmpty()) {
			final String newLine = System.getProperty("line.separator");
			final StringBuilder error = new StringBuilder("The workflow cannot be converted because the following node(s) have not been properly configured:");
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
		final InternalModelConverter converter = new InternalModelConverter(workflowEditor, nodeConverters, sourceConverters);
		return converter.convert();
	}
}
