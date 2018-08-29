package com.workflowconversion.knime2grid.model;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.lang.Validate;

import com.genericworkflownodes.knime.parameter.FileListParameter;
import com.genericworkflownodes.knime.parameter.FileParameter;
import com.genericworkflownodes.knime.parameter.IFileParameter;
import com.workflowconversion.knime2grid.exception.ApplicationException;

public abstract class Port implements GraphicElement {

	// coordinates
	private int x;
	private int y;
	// port name
	private String name;
	// IFileParameter instances do not contain the file itself, rather, just a String with the path (or URI)
	private IFileParameter associatedFileParameter;
	private final ArrayList<File> associatedFiles = new ArrayList<>();

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
	@Override
	public int getX() {
		return x;
	}

	/**
	 * @param x
	 *            the x to set
	 */
	@Override
	public void setX(final int x) {
		this.x = x;
	}

	/**
	 * @return the y
	 */
	@Override
	public int getY() {
		return y;
	}

	/**
	 * @param y
	 *            the y to set
	 */
	@Override
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
		Validate.notEmpty(name, "name cannot be null or empty");
		Validate.notEmpty(name.trim(), "name cannot contain only whitespace");
		this.name = name;
	}

	/**
	 * @return the data
	 */
	public IFileParameter getAssociatedFileParameter() {
		return associatedFileParameter;
	}

	/**
	 * @param associatedFileParameter
	 *            the data to set
	 */
	public void setAssociatedFileParameter(final IFileParameter associatedFileParameter) {
		Validate.notNull(associatedFileParameter, "data is required and cannot be null");
		this.associatedFileParameter = associatedFileParameter;
		updateAssociatedFiles();
	}

	private void updateAssociatedFiles() {
		associatedFiles.clear();
		if (associatedFileParameter instanceof FileParameter) {
			associatedFiles.add(new File(((FileParameter) associatedFileParameter).getValue()));
		} else if (associatedFileParameter instanceof FileListParameter) {
			for (final String filePath : ((FileListParameter) associatedFileParameter).getValue()) {
				associatedFiles.add(new File(filePath));
			}
		} else {
			throw new ApplicationException(
					String.format("Unrecognized file parameter type: %s. This is a bug and should be reported.", this.associatedFileParameter.getClass()));
		}
	}

	/**
	 * @return the associated files to this port.
	 */
	public ArrayList<File> getAssociatedFiles() {
		return new ArrayList<>(associatedFiles);
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
	public void setOriginalPortNr(final int originalPortNr) {
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
	public void setPortNr(final int portNr) {
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
	public void setConnectionType(final ConnectionType connectionType) {
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
	public void setMultiFile(final boolean multiFile) {
		this.multiFile = multiFile;
	}

}
