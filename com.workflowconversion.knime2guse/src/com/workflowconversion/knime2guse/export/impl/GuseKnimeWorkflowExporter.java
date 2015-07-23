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
package com.workflowconversion.knime2guse.export.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.Validate;
import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowManager;

import com.workflowconversion.knime2guse.KnimeWorkflowExporterActivator;
import com.workflowconversion.knime2guse.export.KnimeWorkflowExporter;
import com.workflowconversion.knime2guse.format.ExtensionFilter;
import com.workflowconversion.knime2guse.model.Input;
import com.workflowconversion.knime2guse.model.Job;
import com.workflowconversion.knime2guse.model.Output;
import com.workflowconversion.knime2guse.model.Workflow;
import com.workflowconversion.knime2guse.model.Output.Destination;

/**
 * Exports to gUse/WS-PGRADE grids.
 * 
 * @author Luis de la Garza
 */
public class GuseKnimeWorkflowExporter implements KnimeWorkflowExporter {

	private final static NodeLogger LOGGER = NodeLogger.getLogger(GuseKnimeWorkflowExporter.class);

	private String exportMode;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.workflowconversion.knime2guse.export.ui.DisplayInformationProvider#getId
	 * ()
	 */
	@Override
	public String getId() {
		return this.getClass().getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.workflowconversion.knime2guse.export.ui.DisplayInformationProvider#
	 * getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return "Converts KNIME workflows in a format understood by gUse/WS-PGRADE managed grids.";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.workflowconversion.knime2guse.export.ui.DisplayInformationProvider#
	 * getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "gUse / WS-PGRADE";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.workflowconversion.knime2guse.export.KnimeWorkflowExporter#export(org
	 * .knime.core.node.workflow.WorkflowManager, java.io.File)
	 */
	@Override
	public void export(final Workflow workflow, final File destination) throws Exception {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("exporting using " + getShortDescription() + " to [" + destination.getAbsolutePath() + "]");
		}

		final StringBuilder builder = new StringBuilder();
		generateWorkflowXml(workflow, builder);

		final ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(destination));
		zipOutputStream.putNextEntry(new ZipEntry("workflow.xml"));

		zipOutputStream.write(formatXml(builder.toString()).getBytes());

		zipOutputStream.closeEntry();
		zipOutputStream.close();
	}

	private String formatXml(final String unformattedXml) throws TransformerException {
		final Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		// initialize StreamResult with File object to save to file
		final StreamResult result = new StreamResult(new StringWriter());
		final StreamSource source = new StreamSource(new StringReader(unformattedXml));
		transformer.transform(source, result);
		return result.getWriter().toString();
	}

	/**
	 * @param workflow
	 */
	private void generateWorkflowXml(final Workflow workflow, final StringBuilder builder) {
		fixWorkflowForGuse(workflow);
		final String wfName = "KNIME_Export" + System.currentTimeMillis();
		builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
		builder.append("<workflow");
		addAttribute(builder, "download", "all");
		addAttribute(builder, "export", "proj");
		addAttribute(builder, "mainabst", "");
		addAttribute(builder, "maingraf", wfName);
		addAttribute(builder, "mainreal", wfName);
		addAttribute(builder, "name", wfName);
		builder.append('>');
		builder.append("<graf");
		addAttribute(builder, "name", wfName);
		addAttribute(builder, "text", "Proof of concept. KNIME does it!");
		builder.append('>');
		for (final Job job : workflow.getJobs()) {
			if (job.isIgnored()) {
				continue;
			}
			builder.append("<job");
			addAttribute(builder, "name", job.getName());
			addAttribute(builder, "text", job.getDescription());
			addAttribute(builder, "x", job.getX());
			addAttribute(builder, "y", job.getY());
			builder.append('>');
			// inputs
			int totalIgnoredInputs = 0;
			for (int i = 0, n = job.getNrInputs(); i < n; i++) {
				final Input input = job.getInput(i);
				if (ignoreInput(input)) {
					totalIgnoredInputs++;
				}
			}
			int ignoredInputsSoFar = 0;
			for (int i = 0, n = job.getNrInputs(); i < n; i++) {
				final Input input = job.getInput(i);
				if (ignoreInput(input)) {
					ignoredInputsSoFar++;
				}
				final Job source = input.getSource();
				String preJob = "";
				String preOutput = "";
				// in the case of inputs whose source is the 'Input File' node
				// we can use this as REAL inputs in wspgrade (i.e., the user
				// would have to actually upload something)
				// in wspgrade, if prejob and preoutput attributes are empty,
				// it means that the port needs to be configured, since it's not
				// a channel
				// if (!"Input File".equals(source.getName())) {
				if (source != null) {
					preJob = source.getName();
					preOutput = Integer.toString(input.getSourcePortNr() + source.getNrInputs() - totalIgnoredInputs);
				}
				// }
				builder.append("<input");
				addAttribute(builder, "name", input.getName());
				addAttribute(builder, "prejob", preJob);
				addAttribute(builder, "preoutput", preOutput);
				addAttribute(builder, "seq", i - ignoredInputsSoFar);
				addAttribute(builder, "text", "Port description");
				addAttribute(builder, "x", input.getX());
				addAttribute(builder, "y", input.getY());
				builder.append("/>");
			}
			// outputs
			int ignoredOutputsSoFar = 0;
			for (int i = 0, n = job.getNrOutputs(); i < n; i++) {
				final Output output = job.getOutput(i);
				if (ignoreOutput(output)) {
					ignoredOutputsSoFar++;
					continue;
				}
				builder.append("<output");
				addAttribute(builder, "name", output.getName());
				addAttribute(builder, "seq", i - ignoredOutputsSoFar + (job.getNrInputs() - totalIgnoredInputs));
				addAttribute(builder, "text", "Description of Port");
				addAttribute(builder, "x", output.getX());
				addAttribute(builder, "y", output.getY());
				builder.append("/>");
			}
			builder.append("</job>");
		}
		builder.append("</graf>");
		builder.append("<real");
		addAttribute(builder, "abst", "");
		addAttribute(builder, "graf", wfName);
		addAttribute(builder, "name", wfName);
		addAttribute(builder, "text", "KNIME Exporter");
		builder.append('>');
		for (final Job job : workflow.getJobs()) {
			if (job.isIgnored()) {
				continue;
			}
			builder.append("<job");
			addAttribute(builder, "name", job.getName());
			addAttribute(builder, "text", job.getDescription());
			addAttribute(builder, "x", job.getX());
			addAttribute(builder, "y", job.getY());
			builder.append('>');
			// inputs
			int ignoredInputs = 0;
			for (int i = 0, n = job.getNrInputs(); i < n; i++) {
				final Input input = job.getInput(i);
				if (ignoreInput(input)) {
					ignoredInputs++;
					continue;
				}
				final Job source = input.getSource();
				String preJob = source.getName();
				String preOutput = Integer.toString(input.getSourcePortNr());
				builder.append("<input");
				addAttribute(builder, "name", input.getName());
				addAttribute(builder, "prejob", preJob);
				addAttribute(builder, "preoutput", preOutput);
				addAttribute(builder, "seq", i - ignoredInputs);
				addAttribute(builder, "text", "Port description");
				// TODO: x, y???
				addAttribute(builder, "x", input.getX());
				addAttribute(builder, "y", input.getY());
				builder.append(">");
				addConcretePortProperty(builder, "eparam", input.isCollector() ? "1" : "0");
				final String waiting = input.isCollector() ? "all" : "one";
				addConcretePortProperty(builder, "waitingtmp", waiting);
				addConcretePortProperty(builder, "waiting", waiting);
				addConcretePortProperty(builder, "intname", input.getName());
				addConcretePortProperty(builder, "dpid", "0");
				addConcretePortProperty(builder, "pequaltype", "0");
				builder.append("</input>");
			}
			// outputs
			int ignoredOutputs = 0;
			for (int i = 0, n = job.getNrOutputs(); i < n; i++) {
				final Output output = job.getOutput(i);
				if (ignoreOutput(output)) {
					ignoredOutputs++;
					continue;
				}
				builder.append("<output");
				addAttribute(builder, "name", output.getName());
				addAttribute(builder, "seq", i - ignoredOutputs);
				addAttribute(builder, "text", "Description of Port");
				addAttribute(builder, "x", output.getX());
				addAttribute(builder, "y", output.getY());
				builder.append(">");
				final String mainCount = output.isGenerator() ? "2" : "1";
				addConcretePortProperty(builder, "maincount0", mainCount);
				addConcretePortProperty(builder, "intname", output.getName());
				addConcretePortProperty(builder, "type0", "permanent");
				addConcretePortProperty(builder, "maincount", mainCount);
				builder.append("</output>");
			}

			addExecutionProperty(builder, "type", "Sequence");
			// addExecutionProperty(builder, "params",
			// job.generateCommandLine());
			addExecutionProperty(builder, "params", "");
			addExecutionProperty(builder, "jobistype", "binary");
			addExecutionProperty(builder, "jobmanager", findJobManager(job));
			addMiddlewareSpecificProperties(builder);

			builder.append("</job>");
		}
		builder.append("</real>");
		builder.append("</workflow>");
	}

	private void fixWorkflowForGuse(Workflow workflow) {
		final Map<String, Integer> nameOccurrenceMap = new TreeMap<String, Integer>();
		for (final Job job : workflow.getJobs()) {
			fixJobName(job);
			Integer nameOccurrences = nameOccurrenceMap.get(job.getName());
			if (nameOccurrences == null) {
				nameOccurrences = 1;
				nameOccurrenceMap.put(job.getName(), nameOccurrences);
			} else {
				final String oldName = job.getName();
				job.setName(oldName + nameOccurrences);
				nameOccurrenceMap.put(oldName, nameOccurrences + 1);
			}
			// if job is a generator job, we will:
			// 1. move connection from the source of its input port
			// to the input port of the target of its output port
			// 2. don't add the generator job to the workflow
			if (job.getJobType() == Job.JobType.Generator) {
				// get the input port from the "generator" job (right now,
				// limited to 1!!!!)
				final Input generatorInput = job.getInput(0);
				// get the output port from the "generator" job (right now,
				// limited to 1!!!)
				final Output generatorOutput = job.getOutput(0);
				// connect the source of the collector input to the target of
				// the generator output
				final Destination sourceDestination = generatorInput.getSource().getOutput(0).getDestinations().iterator().next();
				sourceDestination.setTarget(generatorOutput.getDestinations().iterator().next().getTarget());
				sourceDestination.setTargetPortNr(0);
				// remove the connection between the target job and the
				// generator output
				generatorOutput.getDestinations().iterator().next().getTarget().getInput(0).setSource(generatorInput.getSource());
				// flag the port from the source as collector port
				generatorInput.getSource().getOutput(0).setGenerator(true);
				// "remove" the job by ignoring it
				job.setIgnored(true);
			} else if (job.getJobType() == Job.JobType.Collector) {
				// get the input port from the "collector" job
				final Input collectorInput = job.getInput(0);
				// get the output port from the "collector" job
				final Output collectorOutput = job.getOutput(0);
				// connect the source of the input to the target of the
				// collector output
				final Destination sourceDestination = collectorInput.getSource().getOutput(0).getDestinations().iterator().next();
				sourceDestination.setTarget(collectorOutput.getDestinations().iterator().next().getTarget());
				sourceDestination.setTargetPortNr(0);
				// remove the connection between the target job and the
				// collector output
				collectorOutput.getDestinations().iterator().next().getTarget().getInput(0).setSource(collectorInput.getSource());
				// flag the port as collector
				collectorOutput.getDestinations().iterator().next().getTarget().getInput(0).setCollector(true);
				// "remove" the job by ignoring it
				job.setIgnored(true);
			} else if ("FileMerger".equals(job.getName())) {
				// prepare the target job
				final Job targetJob = job.getOutput(0).getDestinations().iterator().next().getTarget();
				targetJob.clearInputs();
				for (int i = 0; i < job.getNrOutputs(); i++) {
					final Job sourceJob = job.getInput(i).getSource();
					sourceJob.getOutput(i).clearDestinations();
					final Destination dest = new Destination(targetJob, i);
					sourceJob.getOutput(i).addDestination(dest);
					targetJob.getInput(i).setSource(sourceJob);
				}
				// ignore this job
				job.setIgnored(true);
			}
		}
	}

	private void fixJobName(Job job) {
		// guse does not allow job names with spaces among other things
		final String name = job.getName();
		if (name.indexOf(' ') >= 0) {
			job.setName(name.replace(" ", ""));
		}
	}

	private void addMiddlewareSpecificProperties(final StringBuilder builder) {
		if ("moab".equals(exportMode)) {
			addExecutionProperty(builder, "gridtype", "moab");
			addExecutionProperty(builder, "resource", "hpc-uni");
			addExecutionProperty(builder, "grid", "hpc-bw.uni-tuebingen.de");
		} else if ("UNICORE".equals(exportMode)) {
			addExecutionProperty(builder, "gridtype", "unicore");
			addExecutionProperty(builder, "resource", "flavus.informatik.uni-tuebingen.de:8090");
			addExecutionProperty(builder, "grid", "flavus.informatik.uni-tuebingen.de:8090");
			addDescriptionProperty(builder, "unicore.keyWalltime", "30");
			addDescriptionProperty(builder, "unicore.keyMemory", "2000");
		}
	}

	private boolean ignoreInput(final Input input) {
		// ignore unconnected inputs
		return input.getSource() == null;
	}

	private boolean ignoreOutput(final Output output) {
		// ignore unconnected outputs
		return output.getDestinations().isEmpty();
	}

	private String findJobManager(final Job job) {
		if ("UNICORE".equals(exportMode)) {
			return job.getName() + " 1.0.0";
		} else {
			return "-";
		}
	}

	private void addAttribute(final StringBuilder builder, final String name, final Object value) {
		builder.append(" ").append(name).append("=\"").append(value).append("\" ");
	}

	private void addDescriptionProperty(final StringBuilder builder, final String key, final String value) {
		builder.append("<description");
		addAttribute(builder, "key", key);
		addAttribute(builder, "value", value);
		builder.append("/>");
	}

	private void addConcretePortProperty(final StringBuilder builder, final String key, final String value) {
		builder.append("<port_prop");
		addAttribute(builder, "desc", "null");
		addAttribute(builder, "inh", "null");
		addAttribute(builder, "key", key);
		addAttribute(builder, "label", "null");
		addAttribute(builder, "value", value);
		builder.append("/>");
	}

	private void addExecutionProperty(final StringBuilder builder, final String key, final String value) {
		builder.append("<execute");
		addAttribute(builder, "desc", "null");
		addAttribute(builder, "inh", "null");
		addAttribute(builder, "key", key);
		addAttribute(builder, "label", "null");
		addAttribute(builder, "value", value);
		builder.append("/>");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.workflowconversion.knime2guse.export.ui.DisplayInformationProvider#
	 * getImageDescriptor()
	 */
	@Override
	public ImageDescriptor getImageDescriptor() {
		return KnimeWorkflowExporterActivator.getImageDescriptor("images/exporters/guse.png");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.workflowconversion.knime2guse.export.ui.ExtensionFilterProvider#
	 * getExtensionFilters()
	 */
	@Override
	public Collection<ExtensionFilter> getExtensionFilters() {
		return Arrays.asList(new ExtensionFilter("*.zip", "ZIP Archive"));
	}

	private interface GuseElement {
		void appendXml(StringBuilder builder);
	}

	@Override
	public Collection<String> getSupportedExportModes() {
		// TODO Auto-generated method stub
		return Arrays.asList("moab", "UNICORE");
	}

	@Override
	public void setExportMode(final String exportMode) {
		Validate.notEmpty(exportMode, "exportMode cannot be null or empty");
		this.exportMode = exportMode;
	}

	// // this is just a proof of concept... check how to read these from the
	// real
	// // knime nodes
	// private static class GuseProperties {
	// private final Map<String, String> properties;
	//
	// GuseProperties() {
	// properties = new TreeMap<String, String>();
	// }
	//
	// void setProperty(final String name, final String value) {
	// properties.put(name, value);
	// }
	//
	// String getProperty(final String name) {
	// return properties.get(name);
	// }
	//
	// Set<String> getPropertyNames() {
	// return new TreeSet<String>(properties.keySet());
	// }
	// }
	//
	// private static class RealInputPortProperties extends GuseProperties {
	// RealInputPortProperties() {
	// // preload some standards
	// super();
	// setProperty("eparam", "1");
	// setProperty("waitingtmp", "all");
	// setProperty("waiting", "all");
	// setProperty("dpid", "0");
	// setProperty("prequaltype", "0");
	// }
	//
	// void setInternalName(final String name) {
	// setProperty("intname", name);
	// }
	// }
	//
	// private static class RealOutputPortProperties extends GuseProperties {
	// RealOutputPortProperties() {
	// super();
	// // preload some standards
	// setProperty("maincount0", "1");
	// setProperty("type0", "permanent");
	// setProperty("maincount", "1");
	// }
	//
	// void setInternalName(final String name) {
	// setProperty("intname", name);
	// }
	// }
	//
	// private static class ExecutionProperties extends GuseProperties {
	// ExecutionProperties() {
	// super();
	// setProperty("gridtype", "unicore");
	// setProperty("jobistype", "binary");
	// setProperty("resource", "hamlet.zih.tu-dresden.de:8081");
	// setProperty("grid", "flavus.informatik.uni-tuebingen.de:8090");
	// setProperty("type", "Sequence");
	// }
	//
	// void setCommandLine(final String commandLine) {
	// setProperty("params", commandLine);
	// }
	// }
	//
	// private static class GraphOutputPortProperties extends GuseProperties {
	// public GraphOutputPortProperties() {
	// super();
	// }
	// }
	//
	// private static class GraphInputPortProperties extends GuseProperties {
	//
	// }
	//
	// private static class GuseOutputPort {
	// final RealOutputPortProperties realOutputProperties;
	// final GraphOutputPortProperties graphOutputProperties;
	// final Output output;
	//
	// GuseOutputPort(final Output output) {
	// this.output = output;
	// realOutputProperties = new RealOutputPortProperties();
	// graphOutputProperties = new GraphOutputPortProperties();
	// }
	// }
	//
	// private static class GuseInputPort {
	// final RealInputPortProperties realInputProperties;
	// final GraphInputPortProperties graphInputProperties;
	// final Input input;
	//
	// GuseInputPort(final Input input) {
	// this.input = input;
	// realInputProperties = new RealInputPortProperties();
	// graphInputProperties = new GraphInputPortProperties();
	// }
	// }
	//
	// private static class GuseGraphJob {
	// Collection<Guse>
	// }
	//
	// private static class GuseConcreteJob {
	// final ExecutionProperties executionProperties;
	// final RealInputPortProperties realInputProperties;
	// final RealOutputPortProperties realOutputProperties;
	// final Job job;
	//
	// GuseConcreteJob(final Job job) {
	// this.job = job;
	// executionProperties = new ExecutionProperties();
	// realInputProperties = new RealInputPortProperties();
	// realOutputProperties = new RealOutputPortProperties();
	// }
	// }
	//
	// private static class GuseGraphSection {
	// final GuseProperties properties;
	// Collection<GuseGraphJob> jobs;
	//
	// GuseGraphSection(final String name, final String description) {
	// jobs = new LinkedList<GuseKnimeWorkflowExporter.GuseGraphJob>();
	// properties = new GuseProperties();
	// properties.setProperty("name", name);
	// properties.setProperty("text", description);
	// }
	//
	// void addJob(final GuseGraphJob job) {
	// jobs.add(job);
	// }
	// }
	//
	// private static class GuseConcreteSection {
	//
	// }
	//
	// private static class GuseWorkflow implements GuseElement{
	// final Collection<GuseConcreteJob> concreteJobs;
	// final GuseGraphSection
	// final GuseProperties properties;
	//
	// GuseWorkflow(final String name) {
	// concreteJobs = new
	// LinkedList<GuseKnimeWorkflowExporter.GuseConcreteJob>();
	// graphJobs = new LinkedList<GuseKnimeWorkflowExporter.GuseGraphJob>();
	// properties = new GuseProperties();
	// properties.setProperty("download", "all");
	// properties.setProperty("export", "work");
	// properties.setProperty("mainabst", "");
	// properties.setProperty("name", name);
	// properties.setProperty("maingraf", name);
	// properties.setProperty("mainreal", name);
	// }
	//
	// void addConcreteJob(final GuseConcreteJob job) {
	// concreteJobs.add(job);
	// }
	//
	// void addGraphJob(final GuseGraphJob job) {
	// graphJobs.add(job);
	// }
	//
	// }

}
