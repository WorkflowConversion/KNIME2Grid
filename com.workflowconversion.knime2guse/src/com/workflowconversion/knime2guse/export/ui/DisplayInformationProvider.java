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
package com.workflowconversion.knime2guse.export.ui;

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * It is a common task to display objects in various UI components. Often the
 * needed information of those objects is just an id and some extra information.
 * This interface defines the methods that implementations should have to be
 * used generically.
 * 
 * @author Luis de la Garza
 */
public interface DisplayInformationProvider {

	/**
	 * ID. It should be unique in a certain context.
	 * 
	 * @return The id.
	 */
	String getId();

	/**
	 * @return The long description.
	 */
	String getLongDescription();

	/**
	 * @return The short description.
	 */
	String getShortDescription();

	/**
	 * @return The image descriptor.
	 */
	ImageDescriptor getImageDescriptor();
}
