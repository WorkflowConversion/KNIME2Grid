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
package com.workflowconversion.knime2guse.model;

import java.io.File;

import org.apache.commons.lang.Validate;

/**
 * 
 * 
 * @author Luis de la Garza
 */
public class Input {

	private Job source;
	private int sourcePortNr;
	private String name;
	private String extension;
	// some input ports are not connected to output ports and they already contain the needed data
	private File data;
	private int x;
	private int y;
	private boolean collector = false;
	private boolean ctd = false;

	/**
	 * @return the extension
	 */
	public String getExtension() {
		return extension;
	}

	/**
	 * @param extension
	 *            the extension to set
	 */
	public void setExtension(String extension) {
		this.extension = extension;
	}

	public boolean isCollector() {
		return collector;
	}

	public void setCollector(boolean collector) {
		this.collector = collector;
	}

	/**
	 * @return the x
	 */
	public int getX() {
		return x;
	}

	/**
	 * @param x
	 *            the x to set
	 */
	public void setX(final int x) {
		this.x = x;
	}

	/**
	 * @return the y
	 */
	public int getY() {
		return y;
	}

	/**
	 * @param y
	 *            the y to set
	 */
	public void setY(final int y) {
		this.y = y;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * @return the data
	 */
	public File getData() {
		return data;
	}

	/**
	 * @param data
	 *            the data to set
	 */
	public void setData(final File data) {
		this.data = data;
	}

	/**
	 * @return the source
	 */
	public Job getSource() {
		return source;
	}

	/**
	 * @param ctd
	 *            whether this input is a ctd file.
	 */
	public void setCTD(final boolean ctd) {
		this.ctd = ctd;
	}

	/**
	 * @return whether this input is a ctd file.
	 */
	public boolean isCTD() {
		return this.ctd;
	}

	/**
	 * Sets the source.
	 * 
	 * @param source
	 *            The source job.
	 * @param sourcePortNr
	 *            The port index in the source job.
	 * @throws {@link NullPointerException} if {@code source} is {@code null}.
	 */
	public void setSource(final Job source, final int sourcePortNr) {
		Validate.notNull(source, "source cannot be null");
		this.source = source;
		this.sourcePortNr = sourcePortNr;
	}

	/**
	 * @return the portNr
	 */
	public int getSourcePortNr() {
		return sourcePortNr;
	}
}
