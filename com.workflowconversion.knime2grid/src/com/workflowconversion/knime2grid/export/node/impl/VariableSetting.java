package com.workflowconversion.knime2grid.export.node.impl;

/**
 * Simple class that maps a setting name to a flow variable name.
 * 
 * @author delagarza
 *
 */
public class VariableSetting {
	private final String settingName;
	private final String flowVariableName;
	// even if the variable is to hold a temporary value, the action of loading the settings into
	// a node causes validation, so the temporary value should be a valid one! (e.g., some nodes
	// taking file paths as parameters validate that the file has a valid mime extension)
	private final String tempVariableValue;

	public VariableSetting(final String settingName, final String flowVariableName) {
		this(settingName, flowVariableName, "tmpvalue");
	}

	public VariableSetting(final String settingName, final String flowVariableName, final String tempVariableValue) {
		this.settingName = settingName;
		this.flowVariableName = flowVariableName;
		this.tempVariableValue = tempVariableValue;
	}

	public String getSettingName() {
		return settingName;
	}

	public String getFlowVariableName() {
		return flowVariableName;
	}

	public String getTempVariableValue() {
		return tempVariableValue;
	}
}
