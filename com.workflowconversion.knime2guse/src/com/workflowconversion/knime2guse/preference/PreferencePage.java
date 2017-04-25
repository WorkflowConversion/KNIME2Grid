package com.workflowconversion.knime2guse.preference;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	// TODO: check which tools have been installed via GKN to display their "remote" paths
	// Map<String, List<ExternalTool>> plugin2tools = PluginPreferenceToolLocator.getToolLocatorService().getToolsByPlugin();
	// List<ExternalTool> tools = plugin2tools.get(m_pluginName);
	// checkout package com.genericworkflownodes.knime.toolfinderservice

	// TODO: this should be a page in which GKN tools are displayed along with their "remote" paths, so we need a
	// TableViewer... also the location of KNIME should be displayed... we probably need only two columns
	// also, each of this configurations should be named, so users can have several configurations, because in real
	// life one might export workflows to different clusters and the paths might vary
	// also, this configuration values should be made available when exporting the workflow... oh god, so much to do
	@Override
	protected void createFieldEditors() {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(IWorkbench workbench) {
		// TODO Auto-generated method stub

	}

}
