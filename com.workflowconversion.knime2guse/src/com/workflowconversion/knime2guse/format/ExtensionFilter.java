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
package com.workflowconversion.knime2guse.format;

/**
 * Simple bean holding the filter pattern (e.g., *.zip) and the description
 * associated with it.
 * 
 * @author Luis de la Garza
 */
public class ExtensionFilter {

	private final String filter;
	private final String description;

	/**
	 * @param filter
	 *            A pattern, such as {@code *.zip}, {@code *.*}, etc.
	 * @param description
	 *            A human readable description of this extension filter, such as
	 *            {@code ZIP Archive}.
	 */
	public ExtensionFilter(final String filter, final String description) {
		super();
		this.filter = filter;
		this.description = description;
	}

	/**
	 * @return the filter
	 */
	public String getFilter() {
		return filter;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

}
