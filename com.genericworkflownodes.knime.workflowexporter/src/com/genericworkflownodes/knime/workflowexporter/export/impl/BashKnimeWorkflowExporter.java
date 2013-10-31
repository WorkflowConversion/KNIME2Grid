/**
 * Copyright (c) 2013, Luis de la Garza.
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
package com.genericworkflownodes.knime.workflowexporter.export.impl;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.jface.resource.ImageDescriptor;

import com.genericworkflownodes.knime.workflowexporter.KnimeWorkflowExporterActivator;
import com.genericworkflownodes.knime.workflowexporter.export.KnimeWorkflowExporter;
import com.genericworkflownodes.knime.workflowexporter.format.ExtensionFilter;
import com.genericworkflownodes.knime.workflowexporter.model.Workflow;

/**
 * Converts workflows into shell script.
 * 
 * @author Luis de la Garza
 */
public class BashKnimeWorkflowExporter implements KnimeWorkflowExporter {

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.genericworkflownodes.knime.export.ui.DisplayInformationProvider#getId
     * ()
     */
    @Override
    public String getId() {
	return BashKnimeWorkflowExporter.class.getName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.genericworkflownodes.knime.export.ui.DisplayInformationProvider#
     * getLongDescription()
     */
    @Override
    public String getLongDescription() {
	return "You can test your workflows outside KNIME using bash scripts.";
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.genericworkflownodes.knime.export.ui.DisplayInformationProvider#
     * getShortDescription()
     */
    @Override
    public String getShortDescription() {
	return "Bash Script";
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.genericworkflownodes.knime.export.ui.DisplayInformationProvider#
     * getImageDescriptor()
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
	return KnimeWorkflowExporterActivator.getImageDescriptor("images/exporters/bash.png");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.genericworkflownodes.knime.export.ui.ExtensionFilterProvider#
     * getExtensionFilters()
     */
    @Override
    public Collection<ExtensionFilter> getExtensionFilters() {
	return Arrays.asList(new ExtensionFilter("*.sh", "Bash Script"));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.genericworkflownodes.knime.export.KnimeWorkflowExporter#export(com
     * .genericworkflownodes.knime.model.Workflow, java.io.File)
     */
    @Override
    public void export(final Workflow workflow, final File destination) {
	// TODO Auto-generated method stub

    }

    @Override
    public Collection<String> getSupportedExportModes() {
	// TODO Auto-generated method stub
	return Collections.emptyList();
    }

    @Override
    public void setExportMode(String exportMode) {
	// TODO Auto-generated method stub

    }

}
