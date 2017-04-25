package com.workflowconversion.knime2guse.export.io.impl;

import org.knime.base.node.io.table.read.ReadTableNodeModel;

public class TableReaderConverter extends AbstractSourceConverter {

	public TableReaderConverter() {
		super("filename", ReadTableNodeModel.class.getCanonicalName());
	}

}
