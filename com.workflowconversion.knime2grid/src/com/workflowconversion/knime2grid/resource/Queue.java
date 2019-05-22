package com.workflowconversion.knime2grid.resource;

import java.io.Serializable;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.StringUtils;

/**
 * Simple wrapper for resource queues.
 * 
 * It is assumed that instances of these classes will exist only within a single thread, meaning: the builder pattern is
 * an overkill. However, making classes threadsafe is always a good practice, plus, using the builder pattern it is
 * guaranteed that all instances of this class will be valid (i.e., they won't contain non-allowed values for members,
 * such as a {@code null} name or id.
 * 
 * @author delagarza
 *
 */
public class Queue implements Comparable<Queue>, Serializable {
	private static final long serialVersionUID = -1202346412388738016L;

	private final String name;

	private Queue(final String name) {
		Validate.isTrue(StringUtils.isNotBlank(name),
				"name cannot be null, empty or contain only whitespaces; this is a coding problem and should be reported.");
		this.name = name;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Queue other = (Queue) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Queue [name=" + name + "]";
	}

	@Override
	public int compareTo(final Queue other) {
		return name.compareTo(other.name);
	}

	public enum Field implements FormField {
		Name(64, "name", "Name");

		private final int maxLength;
		private final String memberName;
		private final String displayName;

		private Field(final int maxLength, final String memberName, final String displayName) {
			this.maxLength = maxLength;
			this.memberName = memberName;
			this.displayName = displayName;
		}

		@Override
		public int getMaxLength() {
			return maxLength;
		}

		@Override
		public String getMemberName() {
			return memberName;
		}

		@Override
		public String getDisplayName() {
			return displayName;
		}
	}

	/**
	 * Queue builder.
	 * 
	 * @author delagarza
	 *
	 */
	public static class Builder {
		private String name;

		/**
		 * Sets the queue name.
		 * 
		 * @param name
		 *            the name.
		 * @return {@code this} builder.
		 */
		public Builder withName(final String name) {
			this.name = name;
			return this;
		}

		/**
		 * Builds a new queue.
		 * 
		 * @return a new queue.
		 */
		public Queue newInstance() {
			return new Queue(name);
		}
	}

}
