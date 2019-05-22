package com.workflowconversion.knime2grid.resource;

import java.io.Serializable;
import java.util.Collection;

/**
 * Interface for resource providers.
 * 
 * Configured resources are defined in an xml file {@code dci-bridge.xml} and are read-only accessible via {@code dci_bridge_service}.
 * 
 * Adding resources through implementations of this interface is not allowed. However, each resource can have associated applications and, depending on the type
 * of resource, it might be possible to store applications through a resource provider. Queues associated to resources are also read-only.
 * 
 * A common usage of a resource provider is to populate tables that display information about computing resources in a user-friendly way. Implementations should
 * be thread safe and as stateless as possible. This is due to the fact that a certain resource provider might be visible by more than one thread
 * simultaneously. If an implementation stores state in a certain form, problems with the consistency of the data could arise.
 * 
 * @author delagarza
 * 
 * @see MiddlewareProvider
 *
 */
public interface ResourceProvider extends Serializable {

	/**
	 * @return the name of this resource provider.
	 */
	public String getName();

	/**
	 * Initializes this instance. Using an initialization method enables implementations to have simple constructors without complicated initialization
	 * routines.
	 * 
	 * @throws Exception
	 *             if there are errors during initialization.
	 */
	public void init() throws Exception;

	/**
	 * Returns all computing resources for the user directly from the persistence layer. Implementations should not store references to the returned resources
	 * in order to avoid inconsistencies.
	 * 
	 * @return All computing resources.
	 */
	public Collection<Resource> getResources();
}
