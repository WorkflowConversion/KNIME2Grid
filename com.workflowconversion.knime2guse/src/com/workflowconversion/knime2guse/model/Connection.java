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

/**
 * 
 * 
 * @author Luis de la Garza
 */
public class Connection {

	private Job job;
	private int portNr;

	/**
	 * @return the destination
	 */
	public Job getJob() {
		return job;
	}

	/**
	 * @return the portNr
	 */
	public int getPortNr() {
		return portNr;
	}

	/**
	 * @param job
	 *            the job to set
	 */
	public void setJob(final Job job) {
		this.job = job;
	}

	/**
	 * @param portNr
	 *            the portNr to set
	 */
	public void setPortNr(final int portNr) {
		this.portNr = portNr;
	}

	// private final Job source;
	// private final int sourceOutputPortNr;
	// private final Job dest;
	// private final int destInputPortNr;
	//
	// private Connection(final Job source, final int sourceOutputPortNr,
	// final Job dest, final int destInputPortNr) {
	// this.source = source;
	// this.sourceOutputPortNr = sourceOutputPortNr;
	// this.dest = dest;
	// this.destInputPortNr = destInputPortNr;
	// }
	//
	// /**
	// * @return the source
	// */
	// public Job getSource() {
	// return source;
	// }
	//
	// /**
	// * @return the sourceOutputPortNr
	// */
	// public int getSourceOutputPortNr() {
	// return sourceOutputPortNr;
	// }
	//
	// /**
	// * @return the dest
	// */
	// public Job getDest() {
	// return dest;
	// }
	//
	// /**
	// * @return the destInputPortNr
	// */
	// public int getDestInputPortNr() {
	// return destInputPortNr;
	// }
	//
	// public static class ConnectionBuilder {
	// private Job source;
	// private int sourceOutputPortNr;
	// private Job dest;
	// private int destInputPortNr;
	//
	// public ConnectionBuilder setSource(final Job source) {
	// this.source = source;
	// return this;
	// }
	//
	// public ConnectionBuilder setSourceOutputPortNr(
	// final int sourceOutputPortNr) {
	// Validate.isTrue(sourceOutputPortNr > -1,
	// "sourceOutputPortNr cannot be negative", sourceOutputPortNr);
	// this.sourceOutputPortNr = sourceOutputPortNr;
	// return this;
	// }
	//
	// public ConnectionBuilder setDest(final Job dest) {
	// this.dest = dest;
	// return this;
	// }
	//
	// public ConnectionBuilder setDestInputPortNr(final int destInputPortNr) {
	// Validate.isTrue(destInputPortNr > -1,
	// "destInputPortNr cannot be negative", destInputPortNr);
	// this.destInputPortNr = destInputPortNr;
	// return this;
	// }
	//
	// public Connection build() {
	// return new Connection(source, sourceOutputPortNr, dest,
	// destInputPortNr);
	// }
	// }
}
