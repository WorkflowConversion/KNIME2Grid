package com.workflowconversion.knime2guse.export.io.impl;

import com.genericworkflownodes.knime.nodes.io.importer.MimeFileImporterNodeModel;

public class MimeFileImporterConverter extends AbstractSourceConverter {

	public MimeFileImporterConverter() {
		super("FILENAME", MimeFileImporterNodeModel.class.getCanonicalName());
	}

}
