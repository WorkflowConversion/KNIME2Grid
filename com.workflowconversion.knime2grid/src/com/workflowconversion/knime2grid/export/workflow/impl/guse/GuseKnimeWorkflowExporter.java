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
package com.workflowconversion.knime2grid.export.workflow.impl.guse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.NodeLogger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.genericworkflownodes.knime.commandline.CommandLineElement;
import com.genericworkflownodes.knime.parameter.FileParameter;
import com.workflowconversion.knime2grid.KnimeWorkflowExporterActivator;
import com.workflowconversion.knime2grid.export.workflow.KnimeWorkflowExporter;
import com.workflowconversion.knime2grid.format.ExtensionFilter;
import com.workflowconversion.knime2grid.model.ConnectionType;
import com.workflowconversion.knime2grid.model.Input;
import com.workflowconversion.knime2grid.model.Job;
import com.workflowconversion.knime2grid.model.Output;
import com.workflowconversion.knime2grid.model.Workflow;

/**
 * Exports to gUse/WS-PGRADE grids.
 * 
 * @author Luis de la Garza
 */
public class GuseKnimeWorkflowExporter implements KnimeWorkflowExporter {

	private final static NodeLogger LOGGER = NodeLogger.getLogger(GuseKnimeWorkflowExporter.class);

	private static final String ATTRIBUTE_VALUE = "value";
	private static final String ATTRIBUTE_KEY = "key";
	private static final String NODE_JOB_DESCRIPTION = "description";
	private static final String NODE_JOB_EXECUTE = "execute";
	private static final String ATTRIBUTE_JOB_NAME = "name";
	private static final String PROPERTY_CURRENT_PARAMETERS = "params";
	private static final String PROPERTY_PREFIX = "workflowconversion.";
	private static final String PROPERTY_APP_NAME = PROPERTY_PREFIX + "appName";
	private static final String PROPERTY_APP_VERSION = PROPERTY_PREFIX + "appVersion";
	private static final String PROPERTY_ORIGINAL_PARAMETERS = PROPERTY_PREFIX + PROPERTY_CURRENT_PARAMETERS;
	private final static String WORKFLOW_ID_FORMAT = "yyyy_MM_dd_hh_mm_ss_SSS";

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.workflowconversion.knime2grid.export.ui.DisplayInformationProvider #getId ()
	 */
	@Override
	public String getId() {
		return this.getClass().getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.workflowconversion.knime2grid.export.ui.DisplayInformationProvider# getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return "Converts KNIME workflows in a format compatible with gUSE/WS-PGRADE.";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.workflowconversion.knime2grid.export.ui.DisplayInformationProvider# getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "gUSE / WS-PGRADE";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.workflowconversion.knime2grid.export.KnimeWorkflowExporter#export(org .knime.core.node.workflow.WorkflowManager, java.io.File)
	 */
	// the structure of a gUSE archive is as follows:
	// workflow.zip
	// |
	// |- workflow.xml (file containing the workflow definition, more on this
	// below)
	// |- [any_name] (directory containing jobs)
	// |
	// |- [job_one_name] (directory containing port folders of a job)
	// | |
	// | |- execute.bin (script executing the job, must be named execute.bin)
	// | |
	// | |- [0] (directory containing an input file, named after port number)
	// | | |
	// | | |--0 (the file must be named 0)
	// | |
	// | |-[1] (directory containing an input file, named after port number)
	// | |
	// | |--0 (the file must be named 0)
	// |
	// |- [job_two_name] (another directory containing port folders of a job)
	//
	// the structure of workflow.xml is roughly as given below:
	// <workflow>
	// <graf name="testgraph2>
	// <job name="Mixer">
	// <input name="words" prejob="" preoutput="" seq="0"/>
	// <input name="numbers" prejob="" preoutput="" seq="1"/>
	// <output name="combined" seq="2"/>
	// </job>
	// <job name="Modifier">
	// <input name="wordsnumbers" prejob="Mixer" preoutput="1" seq="0"/>
	// <output name="finalresult" seq="1"/>
	// </job>
	// </graf>
	// <real>
	// <job name="Mixer">
	// <input name="words" prejob="" preoutput="" seq="0">
	// <port_prop key="file" value="C:/fakepath/words.txt"/>
	// <port_prop key="intname" value="words"/>
	// </input>
	// <input name="numbers" prejob="" preoutput="" seq="1">
	// <port_prop key="file" value="C:/fakepath/numbers.txt"/>
	// <port_prop key="intname" value="numbers"/>
	// </input>
	// <output name="combined" seq="2"/>
	// <execute key="gridtype" value="moab"/>
	// <execute key="resource" value="queue_name"/>
	// <execute key="binary" value="C:/fakepath/combiner.sh"/>
	// <execute key="jobistype" value="binary"/>
	// <execute key="grid" value="cluster.uni.com"/>
	// <execute key="params" value="-w words -n numbers -c combined"/>
	// </job>
	// <job name="Modifier">
	// <input name="wordsnumbers" prejob="Mixer" preoutput="1" seq="0"/>
	// <output name="finalresult" seq="1"/>
	// <execute key="gridtype" value="moab"/>
	// <execute key="resource" value="queue_name"/>
	// <execute key="binary" value="C:/fakepath/modifier.sh"/>
	// <execute key="jobistype" value="binary"/>
	// <execute key="grid" value="cluster.uni.com"/>
	// <execute key="params" value="-i wordsnumbers -o finalresult"/>
	// </job>
	// </real>
	// </workflow>
	//
	// workflow.xml, in this case, represents a workflow composed of two jobs
	// (Mixer, Modifier);
	// Mixer has two inputs and one output, which is connected to the only
	// Modifier's input.
	// Modifier has one channeled input and one output.
	//
	@Override
	public void export(final Workflow workflow, final File destination) throws Exception {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("exporting using " + getShortDescription() + " to [" + destination.getAbsolutePath() + "]");
		}

		fixWorkflowForGuse(workflow);

		final ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(destination));
		// first the workflow descriptor
		writeWorkflowDescriptor(workflow, zipOutputStream);
		writeWorkflowData(workflow, zipOutputStream);
		zipOutputStream.close();
	}

	private void writeWorkflowDescriptor(final Workflow workflow, final ZipOutputStream zipOutputStream)
			throws IOException, TransformerException, ParserConfigurationException {
		final StringBuilder builder = new StringBuilder();
		generateWorkflowXml(workflow, builder);
		zipOutputStream.putNextEntry(new ZipEntry("workflow.xml"));
		zipOutputStream.write(formatXml(builder.toString()).getBytes());
		zipOutputStream.closeEntry();
	}

	private void writeWorkflowData(final Workflow workflow, final ZipOutputStream zipOutputStream) throws IOException, TransformerException {
		// add a folder named after the workflow
		final String rootEntryName = workflow.getName() + '/';
		zipOutputStream.putNextEntry(new ZipEntry(rootEntryName));
		for (final Job job : workflow.getJobs()) {
			writeJob(rootEntryName, zipOutputStream, job);
		}
		zipOutputStream.closeEntry();
	}

	private void writeJob(final String rootEntryName, final ZipOutputStream zipOutputStream, final Job job) throws IOException {
		final String jobEntryName = rootEntryName + job.getName() + '/';
		zipOutputStream.putNextEntry(new ZipEntry(jobEntryName));
		// TODO: generate execute.bin
		writeInputs(jobEntryName, zipOutputStream, job);
		zipOutputStream.closeEntry();
	}

	private void writeInputs(final String rootEntryName, final ZipOutputStream zipOutputStream, final Job job) throws IOException {
		final String inputsFolderName = rootEntryName + "inputs/";
		zipOutputStream.putNextEntry(new ZipEntry(inputsFolderName));
		for (final Input input : job.getInputs()) {
			if (input.getConnectionType() == ConnectionType.UserProvided) {
				final String inputFolderName = inputsFolderName + input.getPortNr() + '/';
				zipOutputStream.putNextEntry(new ZipEntry(inputFolderName));
				zipOutputStream.putNextEntry(new ZipEntry(inputFolderName + '0'));
				zipOutputStream.write(Files.readAllBytes(Paths.get(((FileParameter) input.getData()).getValue())));
				zipOutputStream.closeEntry();
				zipOutputStream.closeEntry();
			}
		}
		zipOutputStream.closeEntry();
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
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 */
	private void generateWorkflowXml(final Workflow workflow, final StringBuilder builder) throws ParserConfigurationException, TransformerException {
		final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		final Document document = documentBuilder.newDocument();
		document.setXmlStandalone(false);
		document.setXmlVersion("1.0");

		final Element workflowElement = document.createElement("workflow");
		final String workflowName = "KNIME_Export" + System.currentTimeMillis();
		workflowElement.setAttribute("download", "all");
		workflowElement.setAttribute("export", "proj");
		workflowElement.setAttribute("mainabst", "");
		workflowElement.setAttribute("maingraf", workflowName);
		workflowElement.setAttribute("mainreal", workflowName);
		workflowElement.setAttribute("name", workflowName);

		final Element grafElement = document.createElement("graf");
		workflowElement.appendChild(grafElement);
		grafElement.setAttribute("name", workflowName);
		grafElement.setAttribute("text", "KNIME Workflow exported to gUSE format.");

		for (final Job job : workflow.getJobs()) {
			if (job.isIgnored()) {
				continue;
			}
			final Element jobElement = document.createElement("job");
			grafElement.appendChild(jobElement);
			jobElement.setAttribute("name", job.getName());
			jobElement.setAttribute("text", job.getDescription());
			jobElement.setAttribute("x", Integer.toString(job.getX()));
			jobElement.setAttribute("y", Integer.toString(job.getY()));

			addExecutionProperty(jobElement, "type", "Sequence");
			addExecutionProperty(jobElement, "params", StringEscapeUtils.escapeXml(generateCommandLine(job)));
			addExecutionProperty(jobElement, "jobistype", "binary");
			addExecutionProperty(jobElement, "jobmanager", "");
			// addMiddlewareSpecificProperties(builder);

			// inputs
			int totalIgnoredInputs = 0;
			for (int i = 0, n = job.getNrInputs(); i < n; i++) {
				final Input input = job.getInputByPortNr(i);
				if (ignoreInput(input)) {
					totalIgnoredInputs++;
				}
			}
			int ignoredInputsSoFar = 0;
			for (int i = 0, n = job.getNrInputs(); i < n; i++) {
				final Input input = job.getInputByPortNr(i);
				if (ignoreInput(input)) {
					ignoredInputsSoFar++;
					continue;
				}

				final Job source = workflow.getJob(input.getSourceId());
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
					// TODO: WTF?
					preOutput = Integer.toString(i + source.getNrInputs() - totalIgnoredInputs);
				}
				// }
				final Element inputElement = document.createElement("input");
				jobElement.appendChild(inputElement);
				inputElement.setAttribute("name", input.getName());
				inputElement.setAttribute("prejob", preJob);
				inputElement.setAttribute("preoutput", preOutput);
				inputElement.setAttribute("seq", Integer.toString(i - ignoredInputsSoFar));
				inputElement.setAttribute("text", "Port description");
				inputElement.setAttribute("x", Integer.toString(input.getX()));
				inputElement.setAttribute("y", Integer.toString(input.getY()));
			}
			// outputs
			int ignoredOutputsSoFar = 0;
			for (int i = 0, n = job.getNrOutputs(); i < n; i++) {
				final Output output = job.getOutputByPortNr(i);
				if (ignoreOutput(output)) {
					ignoredOutputsSoFar++;
					continue;
				}
				final Element outputElement = document.createElement("output");
				jobElement.appendChild(outputElement);
				outputElement.setAttribute("name", output.getName());
				outputElement.setAttribute("seq", Integer.toString(i - ignoredOutputsSoFar + (job.getNrInputs() - totalIgnoredInputs)));
				outputElement.setAttribute("text", "Description of Port");
				outputElement.setAttribute("x", Integer.toString(output.getX()));
				outputElement.setAttribute("y", Integer.toString(output.getY()));
			}
		}

		final Element realElement = document.createElement("real");
		workflowElement.appendChild(realElement);
		realElement.setAttribute("abst", "");
		realElement.setAttribute("graf", workflowName);
		realElement.setAttribute("name", workflowName);
		realElement.setAttribute("text", "Workflow generated by the KNIME2grid plug-in.");

		for (final Job job : workflow.getJobs()) {
			if (job.isIgnored()) {
				continue;
			}

			final Element jobElement = document.createElement("job");
			realElement.appendChild(jobElement);
			jobElement.setAttribute("name", job.getName());
			jobElement.setAttribute("text", job.getDescription());
			jobElement.setAttribute("x", Integer.toString(job.getX()));
			jobElement.setAttribute("y", Integer.toBinaryString(job.getY()));

			// inputs
			int ignoredInputs = 0;
			for (int i = 0, n = job.getNrInputs(); i < n; i++) {
				final Input input = job.getInputByPortNr(i);
				if (ignoreInput(input)) {
					ignoredInputs++;
					continue;
				}
				final Job source = workflow.getJob(input.getSourceId());
				final String preJob = source.getName();
				final String preOutput = Integer.toString(input.getSourcePortNr());

				final Element inputElement = document.createElement("input");
				jobElement.appendChild(inputElement);
				inputElement.setAttribute("name", input.getName());
				inputElement.setAttribute("prejob", preJob);
				inputElement.setAttribute("preoutput", preOutput);
				inputElement.setAttribute("seq", Integer.toString(i - ignoredInputs));
				inputElement.setAttribute("text", "Port description");
				// FIXME: x, y for ports?
				inputElement.setAttribute("x", Integer.toString(input.getX()));
				inputElement.setAttribute("y", Integer.toString(input.getY()));

				addConcretePortProperty(inputElement, "eparam", input.getConnectionType() == ConnectionType.Collector ? "1" : "0");
				final String waiting = input.getConnectionType() == ConnectionType.Collector ? "all" : "one";
				addConcretePortProperty(inputElement, "waitingtmp", waiting);
				addConcretePortProperty(inputElement, "waiting", waiting);
				addConcretePortProperty(inputElement, "intname", input.getName());
				addConcretePortProperty(inputElement, "dpid", "0");
				addConcretePortProperty(inputElement, "pequaltype", "0");
			}
			// outputs
			int ignoredOutputs = 0;
			for (int i = 0, n = job.getNrOutputs(); i < n; i++) {
				final Output output = job.getOutputByPortNr(i);
				if (ignoreOutput(output)) {
					ignoredOutputs++;
					continue;
				}

				final Element outputElement = document.createElement("output");
				jobElement.appendChild(outputElement);
				outputElement.setAttribute("name", output.getName());
				outputElement.setAttribute("seq", Integer.toString(i - ignoredOutputs));
				outputElement.setAttribute("text", "Description of Port");
				outputElement.setAttribute("x", Integer.toString(output.getX()));
				outputElement.setAttribute("y", Integer.toString(output.getY()));

				final String mainCount = output.getConnectionType() == ConnectionType.Generator ? "2" : "1";
				addConcretePortProperty(outputElement, "maincount0", mainCount);
				addConcretePortProperty(outputElement, "intname", output.getName());
				addConcretePortProperty(outputElement, "type0", "permanent");
				addConcretePortProperty(outputElement, "maincount", mainCount);
			}
		}

		// dump document into the passed string builder
		final TransformerFactory tf = TransformerFactory.newInstance();
		final Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.STANDALONE, "no");

		final StringWriter writer = new StringWriter();
		transformer.transform(new DOMSource(document), new StreamResult(writer));
		builder.append(writer.toString());
	}

	private String generateCommandLine(final Job job) {
		final StringBuilder builder = new StringBuilder();
		for (final CommandLineElement element : job.getCommandLine()) {
			builder.append(element.getStringRepresentation()).append(' ');
		}
		return builder.toString();
	}

	private void fixWorkflowForGuse(final Workflow workflow) {
		final Map<String, Integer> nameOccurrenceMap = new TreeMap<String, Integer>();
		for (final Job job : workflow.getJobs()) {
			fixJobName(job);
			fixDuplicateJobName(nameOccurrenceMap, job);
			fixFileLists(job);
		}
		// Nodes like ZipLoopStart/End will be collapsed into a port

	}

	// remove any weird characters not allowed in job names
	private void fixJobName(final Job job) {
		// guse does not allow job names with spaces among other things
		final String name = job.getName();
		if (name.indexOf(' ') >= 0) {
			job.setName(name.replace(" ", ""));
		}
	}

	// gUSE doesn't allow duplicate in the names of jobs, so that has to be
	// fixed
	private void fixDuplicateJobName(final Map<String, Integer> nameOccurrenceMap, final Job job) {
		Integer nameOccurrences = nameOccurrenceMap.get(job.getName());
		if (nameOccurrences == null) {
			nameOccurrences = 1;
			nameOccurrenceMap.put(job.getName(), nameOccurrences);
		} else {
			final String oldName = job.getName();
			job.setName(oldName + nameOccurrences);
			nameOccurrenceMap.put(oldName, nameOccurrences + 1);
		}
	}

	// gUSE doesn't support lists of files, we will use a custom script for such
	// jobs
	private void fixFileLists(final Job job) {

	}

	// // if job is a generator job, we will:
	// // 1. move connection from the source of its input port to the input port
	// of the target of its output
	// port
	// // 2. don't add the generator job to the workflow
	// if (job.getJobType() == Job.JobType.Generator) {
	// // get the input port from the "generator" job (right now, limited to
	// 1!!!!)
	// final Input generatorInput = job.getInputByPortNr(0);
	// // get the output port from the "generator" job (right now, limited to
	// 1!!!)
	// final Output generatorOutput = job.getOutputByPortNr(0);
	// // connect the source of the collector input to the target of the
	// generator output
	// final Destination sourceDestination =
	// workflow.getJob(generatorInput.getSourceId()).getOutputByPortNr(0)
	// .getDestinations().iterator().next();
	// sourceDestination.setTarget(generatorOutput.getDestinations().iterator().next().getTarget());
	// sourceDestination.setTargetPortNr(0);
	// // remove the connection between the target job and the generator output
	// // FIXME: this shitty line of code right here doesn't compile
	// //
	// generatorOutput.getDestinations().iterator().next().getTarget().getInput(0)
	// // .setSource(generatorInput.getSource());
	// // flag the port from the source as collector port
	// workflow.getJob(generatorInput.getSourceId()).getOutputByPortNr(0).setGenerator(true);
	// // "remove" the job by ignoring it
	// job.setIgnored(true);
	// } else if (job.getJobType() == Job.JobType.Collector) {
	// // get the input port from the "collector" job
	// final Input collectorInput = job.getInputByPortNr(0);
	// // get the output port from the "collector" job
	// final Output collectorOutput = job.getOutputByPortNr(0);
	// // connect the source of the input to the target of the
	// // collector output
	// final Destination sourceDestination =
	// workflow.getJob(collectorInput.getSourceId()).getOutputByPortNr(0)
	// .getDestinations().iterator().next();
	// sourceDestination.setTarget(collectorOutput.getDestinations().iterator().next().getTarget());
	// sourceDestination.setTargetPortNr(0);
	// // remove the connection between the target job and the
	// // collector output
	// // FIXME: this shit line of code right here doesn't compile
	// //
	// collectorOutput.getDestinations().iterator().next().getTarget().getInput(0)
	// // .setSource(collectorInput.getSource());
	// // flag the port as collector
	// collectorOutput.getDestinations().iterator().next().getTarget().getInputByPortNr(0).setCollector(true);
	// // "remove" the job by ignoring it
	// job.setIgnored(true);
	// } else if ("FileMerger".equals(job.getName())) {
	// // prepare the target job
	// final Job targetJob =
	// job.getOutputByPortNr(0).getDestinations().iterator().next().getTarget();
	// targetJob.clearInputs();
	// for (int i = 0; i < job.getNrOutputs(); i++) {
	// final Job sourceJob =
	// workflow.getJob(job.getInputByPortNr(i).getSourceId());
	// sourceJob.getOutputByPortNr(i).clearDestinations();
	// final Destination dest = new Destination(targetJob, i);
	// sourceJob.getOutputByPortNr(i).addDestination(dest);
	// // FIXME: this shitty line of code right here doesn't compile
	// // targetJob.getInput(i).setSource(sourceJob);
	// }
	// // ignore this job
	// job.setIgnored(true);
	// }
	// }
	/*
	 * private void addMiddlewareSpecificProperties(final StringBuilder builder) { if ("moab".equals(exportMode)) { addExecutionProperty(builder, "gridtype",
	 * "moab"); addExecutionProperty(builder, "resource", "hpc-uni"); addExecutionProperty(builder, "grid", "hpc-bw.uni-tuebingen.de"); } else if
	 * ("UNICORE".equals(exportMode)) { addExecutionProperty(builder, "gridtype", "unicore"); addExecutionProperty(builder, "resource",
	 * "flavus.informatik.uni-tuebingen.de:8090"); addExecutionProperty(builder, "grid", "flavus.informatik.uni-tuebingen.de:8090");
	 * addDescriptionProperty(builder, "unicore.keyWalltime", "30"); addDescriptionProperty(builder, "unicore.keyMemory", "2000"); } }
	 */
	private boolean ignoreInput(final Input input) {
		// ignore unconnected inputs
		return input.getConnectionType() == ConnectionType.UserProvided;
	}

	private boolean ignoreOutput(final Output output) {
		// ignore unconnected outputs
		return output.getDestinations().isEmpty();
	}

	private void addDescriptionProperty(final Element jobElement, final String key, final String value) {
		final Document document = jobElement.getOwnerDocument();
		final Element descriptionElement = document.createElement("description");
		jobElement.appendChild(descriptionElement);
		descriptionElement.setAttribute("key", key);
		descriptionElement.setAttribute("value", value);
	}

	private void addConcretePortProperty(final Element portElement, final String key, final String value) {
		final Document document = portElement.getOwnerDocument();
		final Element portPropertiesElement = document.createElement("port_prop");
		portElement.appendChild(portPropertiesElement);
		portPropertiesElement.setAttribute("desc", "null");
		portPropertiesElement.setAttribute("inh", "null");
		portPropertiesElement.setAttribute("key", key);
		portPropertiesElement.setAttribute("label", "null");
		portPropertiesElement.setAttribute("value", value);
	}

	private void addExecutionProperty(final Element jobElement, final String key, final String value) {
		final Document document = jobElement.getOwnerDocument();
		final Element executeElement = document.createElement("execute");
		jobElement.appendChild(executeElement);
		executeElement.setAttribute("desc", "null");
		executeElement.setAttribute("inh", "null");
		executeElement.setAttribute("key", key);
		executeElement.setAttribute("label", "null");
		executeElement.setAttribute("value", value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.workflowconversion.knime2grid.export.ui.DisplayInformationProvider# getImageDescriptor()
	 */
	@Override
	public ImageDescriptor getImageDescriptor() {
		return KnimeWorkflowExporterActivator.getImageDescriptor("images/exporters/guse.png");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.workflowconversion.knime2grid.export.ui.ExtensionFilterProvider# getExtensionFilters()
	 */
	@Override
	public Collection<ExtensionFilter> getExtensionFilters() {
		return Arrays.asList(new ExtensionFilter("*.zip", "ZIP Archive"));
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
