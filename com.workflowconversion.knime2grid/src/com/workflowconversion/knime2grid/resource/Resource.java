package com.workflowconversion.knime2grid.resource;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.workflowconversion.knime2grid.exception.DuplicateApplicationException;
import com.workflowconversion.knime2grid.exception.DuplicateQueueException;
import com.workflowconversion.knime2grid.utils.KeyUtils;

/**
 * Class representing a computing resource, such as a computing cluster.
 * 
 * {@link Resource} classes contain a list of {@link Application} and a list of queues.
 * 
 * It is assumed that instances of these classes will exist only within a single thread, meaning: the builder pattern is
 * an overkill. However, making classes threadsafe is always a good practice, plus, using the builder pattern it is
 * guaranteed that all instances of this class will be valid (i.e., they won't contain non-allowed values for members,
 * such as a {@code null} name or id.
 * 
 * @author delagarza
 *
 */
public class Resource implements Serializable {

	private static final long serialVersionUID = -2174466858733103521L;

	// i.e. unicore, moab, lsf, etc.
	private final String type;
	private final String name;

	private final Map<String, Application> applications;
	private final Map<String, Queue> queues;

	private Resource(final String type, final String name, final Collection<Queue> queues) {
		Validate.isTrue(StringUtils.isNotBlank(type), "type cannot be null, empty or contain only whitespace characters.");
		Validate.isTrue(StringUtils.isNotBlank(name), "name cannot be null, empty or contain only whitespace characters.");
		this.type = type;
		this.name = name;

		this.applications = new TreeMap<String, Application>();
		this.queues = new TreeMap<String, Queue>();

		// copy the contents of the collection!
		fillQueues(queues);
	}

	private void fillQueues(final Collection<Queue> queues) {
		if (queues != null) {
			for (final Queue queue : queues) {
				this.queues.put(KeyUtils.generate(queue), queue);
			}
		}
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the applications
	 */
	public Collection<Application> getApplications() {
		return Collections.unmodifiableCollection(applications.values());
	}

	/**
	 * @param application
	 *            application to add.
	 * @throws DuplicateApplicationException
	 *             if an application with the same name, version, path already exists in this resource.
	 */
	public void addApplication(final Application application) throws DuplicateApplicationException {
		Validate.notNull(application, "application cannot be null");
		final String key = KeyUtils.generate(application);
		if (!applications.containsKey(key)) {
			applications.put(key, application);
		} else {
			throw new DuplicateApplicationException(application);
		}
	}

	/**
	 * Returns the application that matches the given fields.
	 * 
	 * @param name
	 *            the application name.
	 * @param version
	 *            the application version.
	 * @param path
	 *            the application path.
	 * @return the application with the given fields, or {@code null} if the application is not contained in this
	 *         resource.
	 */
	public Application getApplication(final String name, final String version, final String path) {
		return applications.get(KeyUtils.generateApplicationKey(name, version, path));
	}

	/**
	 * @return the queues
	 */
	public Collection<Queue> getQueues() {
		return Collections.unmodifiableCollection(queues.values());
	}

	/**
	 * Adds a queue to this resource.
	 * 
	 * @param queue
	 *            the queue to add.
	 */
	public void addQueue(final Queue queue) {
		Validate.notNull(queue, "queue cannot be null");
		final String key = KeyUtils.generate(queue);
		if (!queues.containsKey(key)) {
			queues.put(key, queue);
		} else {
			throw new DuplicateQueueException(queue);
		}
	}

	/**
	 * Returns the queue that matches the given name.
	 * 
	 * @param name
	 *            the name of the queue to search for.
	 * @return the queue with the given name, or {@code null} if the queue is not contained in this resource.
	 */
	public Queue getQueue(final String name) {
		return queues.get(KeyUtils.generateQueueKey(name));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Resource [type=" + type + ", name=" + name + ", applications=" + applications + ", queues=" + queues + "]";
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
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		final Resource other = (Resource) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	/**
	 * Enum listing the fields of computing resources.
	 * 
	 * @author delagarza
	 *
	 */
	public static enum Field implements FormField {

		Applications(1, "applications", "Applications"), Queues(1, "queues", "Queues"), Name(256, "name", "Name"), Type(64, "type", "Type");

		private final int maxLength;
		private final String displayName;
		private final String memberName;

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
	 * Resource builder.
	 * 
	 * @author delagarza
	 *
	 */
	public static class Builder {
		private String type;
		private String name;
		private Collection<Queue> queues;

		/**
		 * @param type
		 *            the resource type.
		 * @return {@code this} builder.
		 */
		public Builder withType(final String type) {
			this.type = type;
			return this;
		}

		/**
		 * @return the type.
		 */
		public String getType() {
			return this.type;
		}

		/**
		 * @param name
		 *            the resource name
		 * @return {@code this} builder.
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
		 * @param queues
		 *            the resource queues.
		 * @return {@code this} builder.
		 */
		public Builder withQueues(final Collection<Queue> queues) {
			this.queues = queues;
			return this;
		}

		/**
		 * @return a new instance of {@link Resource}.
		 */
		public Resource newInstance() {
			return new Resource(type, name, queues);
		}
	}
}
