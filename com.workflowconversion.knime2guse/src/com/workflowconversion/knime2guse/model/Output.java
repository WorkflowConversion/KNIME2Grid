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

import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.lang.Validate;

/**
 * 
 * 
 * @author Luis de la Garza
 */
public class Output {

	private final Collection<Destination> destinations = new LinkedList<Destination>();
	private Object data;
	private String name;
	private String extension;
	private int x;
	private int y;
	private boolean generator;

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

	public boolean isGenerator() {
		return generator;
	}

	public void setGenerator(boolean generator) {
		this.generator = generator;
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
	public Object getData() {
		return data;
	}

	/**
	 * @param data
	 *            the data to set
	 */
	public void setData(final Object data) {
		this.data = data;
	}

	/**
	 * 
	 * @param destination
	 *            destination.
	 */
	public void addDestination(final Destination destination) {
		destinations.add(destination);
	}

	/**
	 * @return the destinations
	 */
	public Collection<Destination> getDestinations() {
		return destinations;
	}

	public void clearDestinations() {
		destinations.clear();
	}

	public static class Destination {
		private Job target;
		private int targetPortNr;

		public Destination(final Job target, final int targetPortNr) {
			Validate.notNull(target, "target cannot be null");
			Validate.isTrue(targetPortNr > -1, "targetPortNr cannot be negative", targetPortNr);
			this.target = target;
			this.targetPortNr = targetPortNr;
		}

		/**
		 * @return the target
		 */
		public Job getTarget() {
			return target;
		}

		/**
		 * @return the targetPortNr
		 */
		public int getTargetPortNr() {
			return targetPortNr;
		}

		public void setTarget(Job target) {
			this.target = target;
		}

		public void setTargetPortNr(int targetPortNr) {
			this.targetPortNr = targetPortNr;
		}

	}

}
