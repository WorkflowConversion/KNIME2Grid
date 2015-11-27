package com.workflowconversion.knime2guse.export.handler.impl;

/**
 * Simple class that maps a setting name to a flow variable name.
 * @author delagarza
 *
 */
public class VariableSetting {
	private final String settingName;
	private final String flowVariableName;
	
	public VariableSetting(final String settingName, final String flowVariableName) {
		this.settingName = settingName;
		this.flowVariableName = flowVariableName;
	}

	public String getSettingName() {
		return settingName;
	}

	public String getFlowVariableName() {
		return flowVariableName;
	}
}
