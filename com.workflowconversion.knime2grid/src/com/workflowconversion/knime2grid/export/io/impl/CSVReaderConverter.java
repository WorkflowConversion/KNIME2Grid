package com.workflowconversion.knime2grid.export.io.impl;

import com.workflowconversion.knime2grid.export.workflow.ConverterUtils;

public class CSVReaderConverter extends AbstractSourceConverter {

	public CSVReaderConverter() {
		super("url", ConverterUtils.CSVREADER_CLASS_NAME);
	}
}
