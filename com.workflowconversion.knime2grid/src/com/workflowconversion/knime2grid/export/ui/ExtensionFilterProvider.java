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
package com.workflowconversion.knime2grid.export.ui;

import java.util.Collection;

import com.workflowconversion.knime2grid.format.ExtensionFilter;


/**
 * Since several export formats require several file formats, this interface
 * defines associated file formats.
 * 
 * @author Luis de la Garza
 */
public interface ExtensionFilterProvider {

	/**
	 * @return The associated file extensions for the implementation.
	 */
	Collection<ExtensionFilter> getExtensionFilters();
}
