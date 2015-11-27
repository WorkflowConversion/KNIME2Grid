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
package com.workflowconversion.knime2guse.model;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import com.genericworkflownodes.knime.commandline.CommandLineElement;

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
public class Job {

	// coordinates, might not even be used in some formats (i.e., bash)
	private int x;
	private int y;

	private final Map<Integer, Input> inputs;
	private final Map<Integer, Output> outputs;
	private final Map<String, String> params;

	private String id;
	private String name;
	private String description;
	private JobType jobType;

	// it makes sense to split this information in two parts... the executableName
	// quite likely stays constant across platforms, whereas the path changes
	private String executableName;
	private String executablePath;

	// the command line to execute this job... in general, to execute this job one would append
	// path, executableName and commandLine
	private Collection<CommandLineElement> commandLine;

	private boolean ignored = false;

	public Job() {
		this.inputs = new TreeMap<Integer, Input>();
		this.outputs = new TreeMap<Integer, Output>();
		this.params = new TreeMap<String, String>();
	}

	public boolean isIgnored() {
		return ignored;
	}

	public void clearInputs() {
		inputs.clear();
	}

	public void setIgnored(boolean ignored) {
		this.ignored = ignored;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public void addInput(final Input input) {
		inputs.put(inputs.size(), input);
	}

	public Input getInput(final int inputNr) {
		Input input = inputs.get(inputNr);
		if (input == null) {
			throw new NullPointerException("Input number " + inputNr + " does not exist.");
		}
		return input;
	}

	public void addOutput(final Output output) {
		outputs.put(outputs.size(), output);
	}

	public Output getOutput(final int outputNr) {
		Output output = outputs.get(outputNr);
		if (output == null) {
			throw new NullPointerException("Output number " + outputNr + " does not exist.");
		}
		return output;
	}

	public int getNrInputs() {
		return inputs.size();
	}

	public int getNrOutputs() {
		return outputs.size();
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParam(final String paramName, final String paramValue) {
		params.put(paramName, paramValue);
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
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

	public JobType getJobType() {
		return jobType;
	}

	public void setJobType(final JobType jobType) {
		this.jobType = jobType;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Job [id=" + id + ", name=" + name + ", description=" + description + "]";
	}

	public enum JobType {
		Generator, Collector, Normal;
	}

}
