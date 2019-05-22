package com.workflowconversion.knime2grid.export.io.impl;

import com.genericworkflownodes.knime.nodes.io.importer.MimeFileImporterNodeModel;

public class MimeFileImporterConverter extends AbstractSourceConverter {

	public MimeFileImporterConverter() {
		super("FILENAME", MimeFileImporterNodeModel.class.getCanonicalName());
	}

}
