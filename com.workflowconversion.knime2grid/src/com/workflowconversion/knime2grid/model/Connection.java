package com.workflowconversion.knime2grid.model;

import org.knime.core.node.workflow.ConnectionID;
import org.knime.core.node.workflow.NodeID;

/**
 * Defines a connection between two jobs.
 * 
 * @author delagarza
 *
 */
public class Connection {

	// id of this connection
	private final ConnectionID connectionId;
	// ids of the nodes involved in this connection
	private final NodeID sourceId;
	private final NodeID destId;
	// original source/dest port numbers are the port numbers given in the KNIME workflow
	private final int originalSourcePort;
	private final int originalDestPort;
	// final source/dest port numbers (the ones belonging to the internal model jobs)
	private int sourcePort;
	private int destPort;

	/**
	 * @param connectionId
	 * @param sourceId
	 * @param destId
	 * @param originalSourcePort
	 * @param originalDestPort
	 */
	public Connection(final ConnectionID connectionId, final NodeID sourceId, final NodeID destId,
			final int originalSourcePort, final int originalDestPort) {
		this.connectionId = connectionId;
		this.sourceId = sourceId;
		this.destId = destId;
		this.originalSourcePort = originalSourcePort;
		this.originalDestPort = originalDestPort;
	}

	/**
	 * @return the connectionId
	 */
	public ConnectionID getConnectionId() {
		return connectionId;
	}

	/**
	 * @return the sourcePort
	 */
	public int getSourcePort() {
		return sourcePort;
	}

	/**
	 * @param sourcePort
	 *            the sourcePort to set
	 */
	public void setSourcePort(int sourcePort) {
		this.sourcePort = sourcePort;
	}

	/**
	 * @return the destPort
	 */
	public int getDestPort() {
		return destPort;
	}

	/**
	 * @param destPort
	 *            the destPort to set
	 */
	public void setDestPort(int destPort) {
		this.destPort = destPort;
	}

	/**
	 * @return the sourceId
	 */
	public NodeID getSourceId() {
		return sourceId;
	}

	/**
	 * @return the destId
	 */
	public NodeID getDestId() {
		return destId;
	}

	/**
	 * @return the originalSourcePort
	 */
	public int getOriginalSourcePort() {
		return originalSourcePort;
	}

	/**
	 * @return the originalDestPort
	 */
	public int getOriginalDestPort() {
		return originalDestPort;
	}

}
