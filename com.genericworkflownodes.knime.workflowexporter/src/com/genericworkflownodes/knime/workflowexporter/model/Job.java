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
package com.genericworkflownodes.knime.workflowexporter.model;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * 
 * 
 * @author Luis de la Garza
 */
public class Job {

    private int x;
    private int y;
    private final Map<Integer, Input> inputs;
    private final Map<Integer, Output> outputs;
    private final Map<String, String> params;
    private String id;
    private String name;
    private String description;
    private JobType jobType;
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

    public Input getInput(final int inputNr) {
	Input input = inputs.get(inputNr);
	if (input == null) {
	    input = new Input();
	    inputs.put(inputNr, input);
	}
	return input;
    }

    public Output getOutput(final int outputNr) {
	Output output = outputs.get(outputNr);
	if (output == null) {
	    output = new Output();
	    outputs.put(outputNr, output);
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

    public void setId(String id) {
	this.id = id;
    }

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public String getDescription() {
	return description;
    }

    public void setDescription(String description) {
	this.description = description;
    }

    public JobType getJobType() {
	return jobType;
    }

    public void setJobType(JobType jobType) {
	this.jobType = jobType;
    }

    public String generateCommandLine() {
	// TODO: fix this... this is just for the proof of concept
	final StringBuilder builder = new StringBuilder();
	for (final Input input : inputs.values()) {
	    if (input.getSource() == null) {
		// node that has not been connected, no command line needed
		continue;
	    }
	    // use only the last part of the filename
	    // FIXME: better control of this scenario (ziploopstart/end)
	    if (input.getName() != null && input.getData() != null) {
		appendToCommandLine(input.getName(), getFileName(input.getData()), builder);
	    }
	}
	for (final Output output : outputs.values()) {
	    if (output.getDestinations().isEmpty()) {
		// node has not been connected, don't include it in the command
		// line
		continue;
	    }
	    // FIXME: better control of this scenario (ziploopstart/end)
	    if (output.getName() != null && output.getData() != null) {
		appendToCommandLine(output.getName(), getFileName(output.getData()), builder);
	    }
	}
	for (final Entry<String, String> entry : params.entrySet()) {
	    appendToCommandLine(entry.getKey(), entry.getValue(), builder);
	}
	return builder.toString();
    }

    private String getFileName(final Object data) {
	final File file = new File(data.toString());
	return file.getName();
    }

    private void appendToCommandLine(final String name, final String value, final StringBuilder builder) {
	// FIXME: blatantly ignoring parameter "version"
	// possible fixes: don't include version in the ctd? tag it with an
	// attribute (e.g., "internal")
	if (!name.endsWith("version")) {
	    final String[] splitName = name.split("\\.");
	    builder.append('-');
	    if (splitName.length > 1) {
		// the name of the parameter is, for instance:
		// FeatureFinderCentroided.1.algorithm.intensity.bins
		// so we need to get to the "algorithm..." part somehow
		final int startIndex;
		if (name.equals(splitName[0])) {
		    startIndex = 2;
		} else {
		    startIndex = 1;
		}
		for (int i = startIndex; i < splitName.length; i++) {
		    if (i > startIndex) {
			builder.append(':');
		    }
		    builder.append(splitName[i]);
		}
	    } else {
		builder.append(splitName[0]);
	    }
	    builder.append(' ');
	    if (value != null) {
		builder.append(value);
	    }
	}
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
