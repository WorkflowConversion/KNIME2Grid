package com.workflowconversion.knime2grid.model;

import org.apache.commons.lang.Validate;

import com.genericworkflownodes.knime.parameter.IFileParameter;

public abstract class Port implements GraphicElement {

	// coordinates
	private int x;
	private int y;
	// port name
	private String name;
	// while some ports receive/generate data through channels, some of them
	// already contain the data
	private IFileParameter data;
	// the original index of this port in the KNIME node
	private int originalPortNr;
	// the port number in the job
	private int portNr;
	// whether this is a multi-input/output port
	private boolean multiFile = false;
	// the connection type of this port
	private ConnectionType connectionType = ConnectionType.NotAssigned;

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
	public void setX(int x) {
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
	public void setY(int y) {
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
	public void setName(String name) {
		Validate.notEmpty(name, "name cannot be null or empty");
		Validate.notEmpty(name.trim(), "name cannot contain only whitespace");
		this.name = name;
	}

	/**
	 * @return the data
	 */
	public IFileParameter getData() {
		return data;
	}

	/**
	 * @param data
	 *            the data to set
	 */
	public void setData(IFileParameter data) {
		Validate.notNull(data, "data is required and cannot be null");
		this.data = data;
	}

	/**
	 * @return the originalPortNr
	 */
	public int getOriginalPortNr() {
		return originalPortNr;
	}

	/**
	 * @param originalPortNr
	 *            the originalPortNr to set
	 */
	public void setOriginalPortNr(int originalPortNr) {
		this.originalPortNr = originalPortNr;
	}

	/**
	 * @return the portNr
	 */
	public int getPortNr() {
		return portNr;
	}

	/**
	 * @param portNr
	 *            the portNr to set
	 */
	public void setPortNr(int portNr) {
		this.portNr = portNr;
	}

	/**
	 * @return the connectionType
	 */
	public ConnectionType getConnectionType() {
		return connectionType;
	}

	/**
	 * @param connectionType
	 *            the connectionType to set
	 */
	public void setConnectionType(ConnectionType connectionType) {
		this.connectionType = connectionType;
	}

	/**
	 * @return the multiFile
	 */
	public boolean isMultiFile() {
		return multiFile;
	}

	/**
	 * @param multiFile
	 *            the multiFile to set
	 */
	public void setMultiFile(boolean multiFile) {
		this.multiFile = multiFile;
	}

}
