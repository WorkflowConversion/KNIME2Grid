/**
 * Copyright (c) 2012, Luis de la Garza.
 *
 * This file is part of KnimeWorkflowExporter.
 * 
 * GenericKnimeNodes is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.workflowconversion.knime2guse.export;

import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.Validate;

import com.workflowconversion.knime2guse.export.handler.NodeContainerConverter;

/**
 * 
 * 
 * @author Luis de la Garza
 */
public class KnimeWorkflowExporterProvider {

	private static KnimeWorkflowExporterProvider INSTANCE;

	private final Collection<KnimeWorkflowExporter> workflowExporters;
	private final Collection<NodeContainerConverter> nodeConverters;
	private final static Lock LOCK = new ReentrantLock();

	/**
	 * Initializes the instance. This method is to be called only once!.
	 * 
	 * @param availableExporters
	 *            The available exporters.
	 */
	public static void initInstance(final Collection<KnimeWorkflowExporter> availableExporters, final Collection<NodeContainerConverter> handlers) {
		LOCK.lock();
		try {
			if (INSTANCE != null) {
				throw new IllegalStateException("INSTANCE has already been initialized.");
			}
			INSTANCE = new KnimeWorkflowExporterProvider(availableExporters, handlers);
		} finally {
			LOCK.unlock();
		}
	}

	/**
	 * Gets the instance of this singleton.
	 * 
	 * @return The instance.
	 */
	public static KnimeWorkflowExporterProvider getInstance() {
		LOCK.lock();
		try {
			if (INSTANCE == null) {
				throw new IllegalStateException("initInstance() should be called before calling getInstance()");
			}
			return INSTANCE;
		} finally {
			LOCK.unlock();
		}
	}

	private KnimeWorkflowExporterProvider(final Collection<KnimeWorkflowExporter> availableExporters, final Collection<NodeContainerConverter> handlers) {
		Validate.notEmpty(availableExporters, "availableExporters cannot be null or empty");
		this.workflowExporters = availableExporters;
		this.nodeConverters = handlers;
	}

	/**
	 * @return The available exporters
	 */
	public Collection<KnimeWorkflowExporter> getWorkflowExporters() {
		return this.workflowExporters;
	}

	public Collection<NodeContainerConverter> getNodeConverters() {
		return this.nodeConverters;
	}
}
