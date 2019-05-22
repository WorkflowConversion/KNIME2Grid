package com.workflowconversion.knime2grid.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.workflowconversion.knime2grid.resource.Application;
import com.workflowconversion.knime2grid.resource.Queue;
import com.workflowconversion.knime2grid.resource.Resource;

/**
 * Convenience methods to generate system-wide keys for applications, queues.
 * 
 * @author delagarza
 *
 */
public class KeyUtils {

	/**
	 * Generates a key for the given application.
	 * 
	 * @param application
	 *            the application.
	 * @return A key, not necessarily system-wide, that represents the given application.
	 */
	public static String generate(final Application application) {
		Validate.notNull(application,
				"application cannot be null, this seems to be a coding problem and should be reported.");
		return generateApplicationKey(application.getName(), application.getVersion(), application.getPath());
	}

	/**
	 * Generates a key for an application.
	 * 
	 * @param name
	 *            the name.
	 * @param version
	 *            the version.
	 * @param path
	 *            the path.
	 * @return A key, not necessarily system-wide, for an application represented by the given fields.
	 */
	public static String generateApplicationKey(final String name, final String version, final String path) {
		return "application:name=" + StringUtils.trimToEmpty(name) + "_version=" + StringUtils.trimToEmpty(version)
				+ "_path=" + StringUtils.trimToEmpty(path);
	}

	/**
	 * Generates a key for the given resource.
	 * 
	 * @param resource
	 *            the resource.
	 * @return A system-wide key that represents the given resource.
	 */
	public static String generate(final Resource resource) {
		Validate.notNull(resource,
				"resource cannot be null, this seems to be a coding problem and should be reported.");
		return generateResourceKey(resource.getName(), resource.getType());
	}

	/**
	 * Generates a system-wide key for the given resource fields.
	 * 
	 * @param name
	 *            the resource name.
	 * @param type
	 *            the resource type.
	 * @return a system-wide key for a resource represented by the given fields.
	 */
	public static String generateResourceKey(final String name, final String type) {
		return "resource:name=" + StringUtils.trimToEmpty(name) + "_type=" + StringUtils.trimToEmpty(type);
	}

	/**
	 * Generates a key for the given queue.
	 * 
	 * @param queue
	 *            the queue.
	 * @return a key, not necessarily system-wide, for the given queue.
	 */
	public static String generate(final Queue queue) {
		Validate.notNull(queue, "queue cannot be null, this seems to be a coding problem and should be reported.");
		return generateQueueKey(queue.getName());
	}

	/**
	 * Generates a queue for a queue represented by the given fields.
	 * 
	 * @param name
	 *            the name.
	 * @return a key, not necessarily system-wide, for a queue.
	 */
	public static String generateQueueKey(final String name) {
		return "queue:name=" + StringUtils.trimToEmpty(name);
	}
}
