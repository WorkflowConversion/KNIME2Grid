package com.workflowconversion.knime2guse.export.io.impl;

import java.util.LinkedList;
import java.util.List;

import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowManager;

import com.genericworkflownodes.knime.nodes.io.listimporter.ListMimeFileImporterNodeModel;
import com.genericworkflownodes.knime.parameter.FileListParameter;
import com.genericworkflownodes.knime.parameter.IFileParameter;
import com.workflowconversion.knime2guse.export.workflow.ConverterUtils;

public class ListMimeFileImporterConverter extends AbstractSourceConverter {

	private final static String LOCATION_PROPERTY_NAME = "FILENAME";

	public ListMimeFileImporterConverter() {
		super(LOCATION_PROPERTY_NAME, ListMimeFileImporterNodeModel.class.getCanonicalName());
	}

	@Override
	public IFileParameter convert(final NodeContainer sourceNodeContainer, final WorkflowManager workflowManager)
			throws Exception {
		// we need to extract the filenames in an array
		final NodeSettings modelSettings = ConverterUtils.getModelSettings(sourceNodeContainer, workflowManager);
		final String[] filenames = modelSettings.getStringArray(LOCATION_PROPERTY_NAME);
		final List<String> correctedFilenames = new LinkedList<String>();
		for (final String filename : filenames) {
			correctedFilenames.add(ConverterUtils.copyContent(filename).getCanonicalPath());
		}

		return new FileListParameter("unused", correctedFilenames);
	}

}
