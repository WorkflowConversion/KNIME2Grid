package com.workflowconversion.knime2guse.export.io.impl;

import com.workflowconversion.knime2guse.export.workflow.ConverterUtils;

public class CSVReaderConverter extends AbstractSourceConverter {

	public CSVReaderConverter() {
		super("url", ConverterUtils.CSVREADER_CLASS_NAME);
	}
}
