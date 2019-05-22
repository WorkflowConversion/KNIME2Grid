package com.workflowconversion.knime2grid.model;

/**
 * Interface that allows implementations to define methods to specify coordinates.
 * 
 * @author delagarza
 *
 */
public interface GraphicElement {

	/**
	 * Sets the value of the x coordinate.
	 * 
	 * @param x
	 */
	public void setX(final int x);

	/**
	 * Sets the value of the y coordinate.
	 * 
	 * @param y
	 */
	public void setY(final int y);

	/**
	 * Gets the value of the x coordinate.
	 * 
	 * @return
	 */
	public int getX();

	/**
	 * Gets the value of the y coordinate.
	 * 
	 * @return
	 */
	public int getY();

}
