package com.workflowconversion.knime2guse.export.io.impl;

import java.io.File;

import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowManager;

import com.genericworkflownodes.knime.parameter.FileParameter;
import com.genericworkflownodes.knime.parameter.IFileParameter;
import com.workflowconversion.knime2guse.export.io.SourceConverter;
import com.workflowconversion.knime2guse.export.workflow.ConverterUtils;

/**
 * Abstract class that is the base source converters.
 * 
 * @author delagarza
 *
 */
public abstract class AbstractSourceConverter implements SourceConverter {

	private final String contentLocationPropertyName;
	private final String nodeModelClassName;

	/**
	 * @param contentLocationPropertyName
	 * @param nodeModelClassName
	 */
	public AbstractSourceConverter(final String contentLocationPropertyName, final String nodeModelClassName) {
		this.contentLocationPropertyName = contentLocationPropertyName;
		this.nodeModelClassName = nodeModelClassName;
	}

	@Override
	public boolean canHandle(final NodeContainer sourceNodeContainer) {
		return ConverterUtils.nodeModelMatchesClass(sourceNodeContainer, nodeModelClassName);
	}

	@Override
	public IFileParameter convert(final NodeContainer sourceNodeContainer, final WorkflowManager workflowManager)
			throws Exception {
		final NodeSettings modelSettings = ConverterUtils.getModelSettings(sourceNodeContainer, workflowManager);
		final String urlSetting = modelSettings.getString(contentLocationPropertyName);
		final File content = ConverterUtils.copyContent(urlSetting);
		return new FileParameter("unused", content.getCanonicalPath());
	}

}
