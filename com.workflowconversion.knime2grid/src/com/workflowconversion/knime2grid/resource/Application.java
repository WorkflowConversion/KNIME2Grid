package com.workflowconversion.knime2grid.resource;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/**
 * Simple object that contains all of the information an application requires to be executed on gUSE.
 * 
 * It is assumed that instances of these classes will exist only within a single thread, meaning: the builder pattern is an overkill. However, making classes
 * threadsafe is always a good practice, plus, using the builder pattern it is guaranteed that all instances of this class will be valid (i.e., they won't
 * contain non-allowed values for members, such as a {@code null} name or id.
 * 
 * @author delagarza
 */
public class Application implements Serializable {

	private static final long serialVersionUID = -8200132807492156967L;

	private final String name;
	private final String version;
	private final String path;
	private final String description;
	private final Resource owningResource;

	private Application(final String name, final String version, final String path, final String description, final Resource owningResource) {
		Validate.isTrue(StringUtils.isNotBlank(name),
				"name cannot be null, empty or contain only whitespace characters; this is a coding problem and should be reported.");
		Validate.isTrue(StringUtils.isNotBlank(version),
				"version cannot be null, empty or contain only whitespace characters; this is a coding problem and should be reported.");
		Validate.isTrue(StringUtils.isNotBlank(path),
				"path cannot be null, empty or contain only whitespace characters; this is a coding problem and should be reported.");
		Validate.notNull(owningResource, "owningResource cannot be null");
		this.name = name;
		this.version = version;
		this.path = path;
		// no need to validate description
		this.description = StringUtils.trimToEmpty(description);
		this.owningResource = owningResource;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @return the resource owning this application.
	 */
	public Resource getOwningResource() {
		return owningResource;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Application [name=" + name + ", version=" + version + ", description=" + description + ", path=" + path + "]";
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
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
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
		final Application other = (Application) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	/**
	 * An enum listing the fields of an application.
	 * 
	 * @author delagarza
	 */
	public static enum Field implements FormField {
		/**
		 * Name of the application.
		 */
		Name(256, "name", "Name"),
		/**
		 * Description.
		 */
		Description(512, "description", "Description"),
		/**
		 * Path on which the application is found.
		 */
		Path(512, "path", "Path"),
		/**
		 * Version of the application.
		 */
		Version(16, "version", "Version");

		private final int maxLength;
		private final String memberName;
		private final String displayName;

		private Field(final int maxLength, final String memberName, final String displayName) {
			this.maxLength = maxLength;
			this.memberName = memberName;
			this.displayName = displayName;
		}

		/**
		 * Returns the maximum length of this field.
		 * 
		 * @return the maximum length of this field.
		 */
		@Override
		public int getMaxLength() {
			return maxLength;
		}

		/**
		 * Returns the internal name of this field. This is the name of the member in the {@link Application} class.
		 * 
		 * @return the member name of this field.
		 */
		@Override
		public String getMemberName() {
			return memberName;
		}

		/**
		 * A <i>nice</i> name that can be presented to the end user.
		 * 
		 * @return the display name.
		 */
		@Override
		public String getDisplayName() {
			return displayName;
		}
	}

	/**
	 * Application builder.
	 * 
	 * @author delagarza
	 *
	 */
	public static class Builder {
		private String name = "";
		private String version = "";
		private String path = "";
		private String description = "";
		private Resource owningResource;

		/**
		 * @param name
		 *            the application name.
		 * @return a reference to {@code this} builder.
		 */
		public Builder withName(final String name) {
			this.name = name;
			return this;
		}

		/**
		 * @return the name.
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * @param version
		 *            the version.
		 * @return a reference to {@code this} builder.
		 */
		public Builder withVersion(final String version) {
			this.version = version;
			return this;
		}

		/**
		 * @return the version.
		 */
		public String getVersion() {
			return this.version;
		}

		/**
		 * @param path
		 *            the path.
		 * @return a reference to {@code this} builder.
		 */
		public Builder withPath(final String path) {
			this.path = path;
			return this;
		}

		/**
		 * @return the path.
		 */
		public String getPath() {
			return this.path;
		}

		/**
		 * @param description
		 *            the description
		 * @return a reference to {@code this} builder.
		 */
		public Builder withDescription(final String description) {
			this.description = description;
			return this;
		}

		/**
		 * @param owningResource
		 *            resource owning the app that will be created.
		 * @return a reference to {@code this} builder.
		 */
		public Builder withOwningResource(final Resource owningResource) {
			this.owningResource = owningResource;
			return this;
		}

		/**
		 * @return the resource owning this application.
		 */
		public Resource getOwningResource() {
			return this.owningResource;
		}

		/**
		 * @return a new instance of an {@link Application}.
		 */
		public Application newInstance() {
			return new Application(name, version, path, description, owningResource);
		}
	}

}
