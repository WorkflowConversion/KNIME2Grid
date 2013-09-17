/**
 * Copyright (c) 2012, Luis de la Garza.
 *
 * This file is part of KnimeWorkflowExporter.
 * 
 * GenericKnimeNodes is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.genericworkflownodes.knime.workflowexporter.export;

import java.io.File;

import com.genericworkflownodes.knime.workflowexporter.export.ui.KnimeWorkflowExporterInformationProvider;
import com.genericworkflownodes.knime.workflowexporter.model.Workflow;

/**
 * Interface that declares the methods to export workflows into other formats.
 * Each implementation offers a format.
 * 
 * @author Luis de la Garza
 */
public interface KnimeWorkflowExporter extends
		KnimeWorkflowExporterInformationProvider {

	/**
	 * Exports the given workflow into the passed file.
	 * 
	 * @param workflow
	 *            The workflow to export.
	 * @param destination
	 *            The destination file.
	 */
	void export(Workflow workflow, File destination) throws Exception;

}
