/**
 * Copyright (c) 2013, Luis de la Garza.
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
package com.workflowconversion.knime2grid.model;

import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.Validate;
import org.knime.core.node.workflow.NodeID;

import com.genericworkflownodes.knime.commandline.CommandLineElement;
import com.workflowconversion.knime2grid.resource.Application;
import com.workflowconversion.knime2grid.resource.Queue;

/**
 * This a simple object that contains all information related to a single job. Not all of the fields will be known at instantiation, so as the conversion
 * advances, different fields will be populated (e.g., fields such as {@link #x} or {@link #y} depend on the target platform).
 * 
 * It's worth noting that instances of this class don't <i>know</i> how to generate the command line needed to execute them, rather, this is set by an external
 * process, due to the fact that the command line depends on the type of Job (i.e., if this is a GKN job, it'll look different than a KNIME job).
 * 
 * @author Luis de la Garza
 */
public class Job implements GraphicElement {
	// coordinates, might not even be used in some formats (i.e., bash)
	private int x;
	private int y;

	private final Map<String, Input> inputsByName;
	private final Map<Integer, Input> inputsByPortNr;
	private final Map<Integer, Input> inputsByOriginalPortNr;

	private final Map<String, Output> outputsByName;
	private final Map<Integer, Output> outputsByPortNr;
	private final Map<Integer, Output> outputsByOriginalPortNr;

	private final Map<String, String> params;

	private NodeID id;
	private String name;
	private String description;

	// it makes sense to split this information in two parts... the executableName
	// quite likely stays constant across platforms, whereas the path changes
	private String executableName;
	private String executablePath;

	// the command line to execute this job... in general, to execute this job one would append
	// path, executableName and commandLine
	private Collection<CommandLineElement> commandLine;

	// remote application and queue that have been associated to this job, if any
	private Application remoteApplication;
	private Queue remoteQueue;

	private boolean ignored = false;

	public Job() {
		this.inputsByName = new TreeMap<String, Input>();
		this.inputsByPortNr = new TreeMap<Integer, Input>();
		this.inputsByOriginalPortNr = new TreeMap<Integer, Input>();

		this.outputsByName = new TreeMap<String, Output>();
		this.outputsByPortNr = new TreeMap<Integer, Output>();
		this.outputsByOriginalPortNr = new TreeMap<Integer, Output>();

		this.params = new TreeMap<String, String>();

		this.commandLine = Collections.<CommandLineElement>emptyList();
	}

	public boolean isIgnored() {
		return ignored;
	}

	public void clearInputs() {
		inputsByName.clear();
	}

	public void setIgnored(final boolean ignored) {
		this.ignored = ignored;
	}

	@Override
	public int getX() {
		return x;
	}

	@Override
	public void setX(final int x) {
		this.x = x;
	}

	@Override
	public int getY() {
		return y;
	}

	@Override
	public void setY(final int y) {
		this.y = y;
	}

	public void addInput(final Input input) {
		if (inputsByName.put(input.getName(), input) != null) {
			throw new InvalidParameterException("This job already has an input named: " + input.getName());
		}
		final int portNr = inputsByPortNr.size();
		if (inputsByPortNr.put(portNr, input) != null) {
			throw new InvalidParameterException("This job already has an input with the port number: " + portNr);
		}
		input.setPortNr(portNr);
		if (inputsByOriginalPortNr.put(input.getOriginalPortNr(), input) != null) {
			throw new InvalidParameterException("This job already has an input with the original port number: " + input.getOriginalPortNr());
		}
	}

	public Input getInputByName(final String inputName) {
		final Input input = inputsByName.get(inputName);
		if (input == null) {
			throw new NullPointerException("Input " + inputName + " does not exist.");
		}
		return input;
	}

	public Input getInputByPortNr(final int portNr) {
		final Input input = inputsByPortNr.get(portNr);
		if (input == null) {
			throw new NullPointerException("Input with portNr " + portNr + " does not exist.");
		}
		return input;
	}

	public Input getInputByOriginalPortNr(final int originalPortNr) {
		return inputsByOriginalPortNr.get(originalPortNr);
	}

	public Collection<Input> getInputs() {
		return inputsByPortNr.values();
	}

	public void addOutput(final Output output) {
		if (outputsByName.put(output.getName(), output) != null) {
			throw new InvalidParameterException("This job already has an input named: " + output.getName());
		}
		final int portNr = outputsByPortNr.size();
		if (outputsByPortNr.put(portNr, output) != null) {
			throw new InvalidParameterException("This job already has an input with the port number: " + portNr);
		}
		output.setPortNr(portNr);
		if (outputsByOriginalPortNr.put(output.getOriginalPortNr(), output) != null) {
			throw new InvalidParameterException("This job already has an input with the original port number: " + output.getOriginalPortNr());
		}
	}

	public Output getOutputByPortNr(final int portNr) {
		final Output output = outputsByPortNr.get(portNr);
		if (output == null) {
			throw new NullPointerException("Output with port number " + portNr + " does not exist.");
		}
		return output;
	}

	public Output getOutputByOriginalPortNr(final int originalPortNr) {
		return outputsByOriginalPortNr.get(originalPortNr);
	}

	public Collection<Output> getOutputs() {
		return outputsByPortNr.values();
	}

	public int getNrInputs() {
		return inputsByName.size();
	}

	public int getNrOutputs() {
		return outputsByName.size();
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParam(final String paramName, final String paramValue) {
		params.put(paramName, paramValue);
	}

	public NodeID getId() {
		return id;
	}

	public void setId(final NodeID id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public String getExecutableName() {
		return executableName;
	}

	public void setExecutableName(final String executableName) {
		this.executableName = executableName;
	}

	public String getExecutablePath() {
		return executablePath;
	}

	public void setExecutablePath(final String executablePath) {
		this.executablePath = executablePath;
	}

	public Collection<CommandLineElement> getCommandLine() {
		return commandLine;
	}

	public void setCommandLine(final Collection<CommandLineElement> commandLine) {
		this.commandLine = commandLine;
	}

	public void addParameter(final String key, final String value) {
		this.params.put(key, value);
	}

	public void setRemoteApplication(final Application remoteApplication) {
		Validate.notNull(remoteApplication, "remoteApplication cannot be null");
		this.remoteApplication = remoteApplication;
	}

	public void clearRemoteApplication() {
		this.remoteApplication = null;
	}

	public Application getRemoteApplication() {
		return remoteApplication;
	}

	public void setRemoteQueue(final Queue remoteQueue) {
		Validate.notNull(remoteQueue, "remoteQueue cannot be null");
		this.remoteQueue = remoteQueue;
	}

	public void clearRemoteQueue() {
		this.remoteQueue = null;
	}

	public Queue getRemoteQueue() {
		return remoteQueue;
	}

	@Override
	public String toString() {
		return "Job [id=" + id + ", name=" + name + ", description=" + description + ", remoteApplication=" + remoteApplication + ", remoteQueue=" + remoteQueue
				+ "]";
	}

}
