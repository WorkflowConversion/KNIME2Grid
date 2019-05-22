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
 * This a simple object that contains all information related to a single job. Not all of the fields will be known at
 * instantiation, so as the conversion advances, different fields will be populated (e.g., fields such as {@link #x} or
 * {@link #y} depend on the target platform).
 * 
 * It's worth noting that instances of this class don't <i>know</i> how to generate the command line needed to execute
 * them, rather, this is set by an external process, due to the fact that the command line depends on the type of Job
 * (i.e., if this is a GKN job, it'll look different than a KNIME job).
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

	// the command line to execute this job... in general, to execute this job one
	// would append
	// path, executableName and commandLine
	private Collection<CommandLineElement> commandLine;

	// application and queue that have been associated to this job, if any
	private Application associatedApplication;
	private Queue remoteQueue;

	private JobType jobType;

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

	public JobType getJobType() {
		return jobType;
	}

	public void setJobType(final JobType jobType) {
		this.jobType = jobType;
	}

	public void clearInputs() {
		inputsByName.clear();
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
		final int portNr = getNextPortNumber();

		if (inputsByName.containsKey(input.getName())) {
			throw new InvalidParameterException("This job already has an input named: " + input.getName());
		}
		if (inputsByPortNr.containsKey(portNr)) {
			throw new InvalidParameterException("This job already has an input with the port number: " + portNr);
		}
		if (inputsByOriginalPortNr.containsKey(input.getOriginalPortNr())) {
			throw new InvalidParameterException("This job already has an input with the original port number: " + input.getOriginalPortNr());
		}

		input.setPortNr(portNr);
		inputsByName.put(input.getName(), input);
		inputsByPortNr.put(portNr, input);
		inputsByOriginalPortNr.put(input.getOriginalPortNr(), input);
	}

	public Input getInputByName(final String inputName) {
		final Input input = inputsByName.get(inputName);
		if (input == null) {
			throw new NullPointerException("Input " + inputName + " does not exist.");
		}
		return input;
	}

	// public Input getInputByPortNr(final int portNr) {
	// final Input input = inputsByPortNr.get(portNr);
	// if (input == null) {
	// throw new NullPointerException("Input with portNr " + portNr + " does not exist.");
	// }
	// return input;
	// }

	public Input getInputByOriginalPortNr(final int originalPortNr) {
		return inputsByOriginalPortNr.get(originalPortNr);
	}

	public Collection<Input> getInputs() {
		return inputsByPortNr.values();
	}

	public void addOutput(final Output output) {
		final int portNr = getNextPortNumber();
		if (outputsByName.containsKey(output.getName())) {
			throw new InvalidParameterException("This job already has an input named: " + output.getName());
		}
		if (outputsByPortNr.containsKey(portNr)) {
			throw new InvalidParameterException("This job already has an input with the port number: " + portNr);
		}
		if (outputsByOriginalPortNr.containsKey(output.getOriginalPortNr())) {
			throw new InvalidParameterException("This job already has an input with the original port number: " + output.getOriginalPortNr());
		}

		output.setPortNr(portNr);
		outputsByName.put(output.getName(), output);
		outputsByPortNr.put(portNr, output);
		outputsByOriginalPortNr.put(output.getOriginalPortNr(), output);
	}

	private int getNextPortNumber() {
		// gUSE requires absolute port numbers, there are no specific input/output port numbers
		return inputsByPortNr.size() + outputsByPortNr.size();
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
		this.associatedApplication = remoteApplication;
	}

	public void clearRemoteApplication() {
		this.associatedApplication = null;
	}

	public Application getRemoteApplication() {
		return associatedApplication;
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
		return "Job [id=" + id + ", name=" + name + ", description=" + description + ", remoteApplication=" + associatedApplication + ", remoteQueue="
				+ remoteQueue + "]";
	}

}
