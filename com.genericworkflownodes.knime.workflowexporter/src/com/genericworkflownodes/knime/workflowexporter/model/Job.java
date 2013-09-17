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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang.Validate;

/**
 * 
 * 
 * @author Luis de la Garza
 */
public class Job {

	private final int x;
	private final int y;
	private final Input[] inputs;
	private final Output[] outputs;
	private final Map<String, String> params;
	private final String id;
	private final String name;
	private final String description;

	private Job(final String id, final String name, final String description,
			final int nInputs, final int nOutputs, final int x, final int y) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.x = x;
		this.y = y;
		this.inputs = new Input[nInputs];
		this.outputs = new Output[nOutputs];
		this.params = new TreeMap<String, String>();
		// init all inputs/outputs
		for (int i = 0; i < nInputs; i++) {
			this.inputs[i] = new Input();
		}
		for (int i = 0; i < nOutputs; i++) {
			this.outputs[i] = new Output();
		}
	}

	public void setParam(final String name, final String value) {
		params.put(name, value);
	}

	public Set<String> getParamNames() {
		return new TreeSet<String>(params.keySet());
	}

	public String getParam(final String name) {
		return params.get(name);
	}

	/**
	 * @return the x
	 */
	public int getX() {
		return x;
	}

	/**
	 * @return the y
	 */
	public int getY() {
		return y;
	}

	public int getNrInputs() {
		return inputs.length;
	}

	public Input getInput(final int index) {
		Validate.isTrue(index > -1 && index < inputs.length, "Invalid index",
				index);
		return inputs[index];
	}

	public void setInput(final Input input, final int index) {
		inputs[index] = input;
	}

	public int getNrOutputs() {
		return outputs.length;
	}

	public Output getOutput(final int index) {
		Validate.isTrue(index > -1 && index < outputs.length, "Invalid index",
				index);
		return outputs[index];
	}

	public void setOutput(final Output output, final int index) {
		outputs[index] = output;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	public String generateCommandLine() {
		// TODO: fix this... this is just for the proof of concept
		final StringBuilder builder = new StringBuilder();
		for (final Input input : inputs) {
			if (input.getSource() == null) {
				// node that has not been connected, no command line needed
				continue;
			}
			// use only the last part of the filename
			appendToCommandLine(input.getName(), getFileName(input.getData()),
					builder);
		}
		for (final Output output : outputs) {
			if (output.getDestinations().isEmpty()) {
				// node has not been connected, don't include it in the command
				// line
				continue;
			}
			appendToCommandLine(output.getName(),
					getFileName(output.getData()), builder);
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

	private void appendToCommandLine(final String name, final String value,
			final StringBuilder builder) {
		final String[] splitName = name.split("\\.");
		builder.append('-');
		if (splitName.length > 1) {
			builder.append(splitName[1]);
		} else {
			builder.append(splitName[0]);
		}
		if (value != null) {
			builder.append(' ').append(value);
		}
		builder.append(' ');
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Job [id=" + id + ", name=" + name + ", description="
				+ description + "]";
	}

	public static class JobBuilder {
		private int x = 0;
		private int y = 0;
		private int nInputs = 0;
		private int nOutputs = 0;
		private String id;
		private String name;
		private String description;
		private String commandLine;

		public JobBuilder setX(final int x) {
			this.x = x;
			return this;
		}

		public JobBuilder setY(final int y) {
			this.y = y;
			return this;
		}

		public JobBuilder setNrInputs(final int nInputs) {
			Validate.isTrue(nInputs > -1, "nInputs cannot be negative", nInputs);
			this.nInputs = nInputs;
			return this;
		}

		public JobBuilder setNrOutputs(final int nOutputs) {
			Validate.isTrue(nOutputs > -1, "nOutputs cannot be negative",
					nOutputs);
			this.nOutputs = nOutputs;
			return this;
		}

		public JobBuilder setId(final String id) {
			Validate.notEmpty(id, "id cannot be empty");
			this.id = id;
			return this;
		}

		public JobBuilder setName(final String name) {
			Validate.notEmpty(name, "name cannot be empty");
			this.name = name;
			return this;
		}

		public JobBuilder setDescription(final String description) {
			this.description = description;
			return this;
		}

		public JobBuilder setCommandLine(final String commandLine) {
			this.commandLine = commandLine;
			return this;
		}

		public Job build() {
			return new Job(id, name, description, nInputs, nOutputs, x, y);
		}
	}

}
