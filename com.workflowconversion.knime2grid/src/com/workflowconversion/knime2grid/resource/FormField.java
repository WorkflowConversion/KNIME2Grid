package com.workflowconversion.knime2grid.resource;

/**
 * Tables display fields from classes in forms (i.e., forms to add. This interface defines the minimal requirements a
 * field must have in order to be properly displayed.
 * 
 * @author delagarza
 *
 */
public interface FormField {

	/**
	 * @return the maximum length, in characters, a field can have.
	 */
	public int getMaxLength();

	/**
	 * @return the name of the class member associated with this field.
	 */
	public String getMemberName();

	/**
	 * @return the name to display on tables/dialogs.
	 */
	public String getDisplayName();

	/**
	 * @return the name of this field.
	 */
	public String name();
}
