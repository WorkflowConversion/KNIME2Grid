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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;
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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.NodeLogger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.genericworkflownodes.knime.commandline.CommandLineElement;
import com.genericworkflownodes.knime.parameter.FileListParameter;
import com.genericworkflownodes.knime.parameter.FileParameter;
import com.genericworkflownodes.knime.parameter.IFileParameter;
import com.workflowconversion.knime2grid.KnimeWorkflowExporterActivator;
import com.workflowconversion.knime2grid.exception.ApplicationException;
import com.workflowconversion.knime2grid.export.workflow.KnimeWorkflowExporter;
import com.workflowconversion.knime2grid.format.ExtensionFilter;
import com.workflowconversion.knime2grid.model.ConnectionType;
import com.workflowconversion.knime2grid.model.Input;
import com.workflowconversion.knime2grid.model.Job;
import com.workflowconversion.knime2grid.model.Output;
import com.workflowconversion.knime2grid.model.Port;
import com.workflowconversion.knime2grid.model.Workflow;
import com.workflowconversion.knime2grid.resource.Application;

/**
 * Exports KNIME workflows to WS-PGRADE format.
 * 
 * @author Luis de la Garza
 */
public class GuseKnimeWorkflowExporter implements KnimeWorkflowExporter {

	private final static NodeLogger LOGGER = NodeLogger.getLogger(GuseKnimeWorkflowExporter.class);
	private final static char ZIP_ENTRY_SEPARATOR = '/';

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
	 * @see com.workflowconversion.knime2grid.export.KnimeWorkflowExporter#export(org
	 * .knime.core.node.workflow.WorkflowManager, java.io.File)
	 */
	// the structure of a gUSE archive is as follows:
	// workflow.zip:
	// 1. workflow.xml - xml contianing the abstract and concrete definition of the
	// workflow (see example below)
	// 2. [any_name]/[job_name] - each job has a folder containing:
	// 3. [any_name]/[job_name]/execute.bin - a script that executes the job, name
	// must be execute.bin
	// 4. [any_name]/[job_name]/[port_number]/0 - input associated with the port
	// number [port_number], name must be 0, I shit you not
	/**
	 * use a pre block to avoid IDE formatters breaking indentation in the shown XML below
	 * 
	 * <pre>
	 * <workflow>
	 *   <graf name="testgraph2>
	 *     <job name="Mixer">
	 *       <input name="words" prejob="" preoutput="" seq="0"/>
	 *       <input name="numbers" prejob="" preoutput="" seq="1"/>
	 *       <output name="combined" seq="2"/>
	 *     </job>
	 *    <job name="Modifier">
	 *      <input name="wordsnumbers" prejob="Mixer" preoutput="1" seq="0"/>
	 *      <output name="finalresult" seq="1"/>
	 *    </job>
	 *  </graf>
	 *  <real>
	 *    <job name="Mixer">
	 *      <input name="words" prejob="" preoutput="" seq="0">
	 *        <port_prop key="file" value="C:/fakepath/words.txt"/>
	 *        <port_prop key="intname" value="words"/>
	 *      </input>
	 *      <input name="numbers" prejob="" preoutput="" seq="1">
	 *        <port_prop key="file" value="C:/fakepath/numbers.txt"/>
	 *        <port_prop key="intname" value="numbers"/>
	 *      </input>
	 *      <output name="combined" seq="2"/>
	 *      <execute key="gridtype" value="moab"/>
	 *      <execute key="resource" value="queue_name"/>
	 *      <execute key="binary" value="C:/fakepath/combiner.sh"/>
	 *      <execute key="jobistype" value="binary"/>
	 *      <execute key="grid" value="cluster.uni.com"/>
	 *      <!-- words, numbers and combined are internal names of ports
	 *           that were defined in the abstract (<graf> element) -->
	 *      <execute key="params" value="-w words -n numbers -c combined"/>
	 *    </job>
	 *    <job name="Modifier">
	 *      <input name="wordsnumbers" prejob="Mixer" preoutput="1" seq="0"/>
	 *      <output name="finalresult" seq="1"/>
	 *      <execute key="gridtype" value="moab"/>
	 *      <execute key="resource" value="queue_name"/>
	 *      <execute key="binary" value="C:/fakepath/modifier.sh"/>
	 *      <execute key="jobistype" value="binary"/>
	 *      <execute key="grid" value="cluster.uni.com"/>
	 *      <execute key="params" value="-i wordsnumbers -o finalresult"/>
	 *    </job>
	 *  </real>
	 *</workflow>
	 * </pre>
	 * 
	 * workflow.xml, in this case, represents a workflow composed of two jobs (Mixer, Modifier); Mixer has two inputs
	 * and one output, which is connected to the only Modifier's input. Modifier has one channeled input and one output.
	 */

	@Override
	public void export(final Workflow workflow, final File destination) throws Exception {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("exporting using " + getShortDescription() + " to [" + destination.getAbsolutePath() + "]");
		}

		validateWorkflow(workflow);
		fixWorkflowForGuse(workflow);

		final ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(destination));

		writeWorkflowDescriptor(workflow, zipOutputStream);
		writeJobs(workflow, zipOutputStream);
		zipOutputStream.close();
	}

	// check that all ports have been assigned
	private void validateWorkflow(final Workflow workflow) {
		for (final Job job : workflow.getJobs()) {
			// unassigned inputs are tricky, but unassigned outputs are irrelevant anyway
			for (final Input input : job.getInputs()) {
				if (input.getConnectionType() == ConnectionType.NotAssigned) {
					throw new ApplicationException("Not all inputs have been converted. This is a bug and should be reported.");
				}
			}
		}
	}

	private void fixWorkflowForGuse(final Workflow workflow) {
		final Map<String, Integer> nameOccurrenceMap = new TreeMap<String, Integer>();
		for (final Job job : workflow.getJobs()) {
			fixJobName(job);
			fixDuplicateJobName(nameOccurrenceMap, job);
		}
	}

	// remove any weird characters not allowed in job names
	private void fixJobName(final Job job) {
		// gUSE does not allow job names with spaces
		// TODO: check which other restrictions job names have
		final String name = job.getName();
		if (name.indexOf(' ') >= 0) {
			job.setName(name.replace(' ', '-'));
		}
	}

	// gUSE doesn't allow duplicate in the names of jobs, so that has to be fixed
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

	// gUSE requires ports to match the filenames, so we need to

	private void writeWorkflowDescriptor(final Workflow workflow, final ZipOutputStream zipOutputStream)
			throws IOException, TransformerException, ParserConfigurationException {
		final StringBuilder builder = new StringBuilder();
		generateWorkflowXml(workflow, builder);
		zipOutputStream.putNextEntry(new ZipEntry("workflow.xml"));
		zipOutputStream.write(formatXml(builder.toString()).getBytes());
		zipOutputStream.closeEntry();
	}

	private void writeJobs(final Workflow workflow, final ZipOutputStream zipOutputStream) throws IOException, TransformerException {
		// add a folder named after the workflow
		final String rootEntryName = workflow.getName() + ZIP_ENTRY_SEPARATOR;
		zipOutputStream.putNextEntry(new ZipEntry(rootEntryName));
		for (final Job job : workflow.getJobs()) {
			writeJob(rootEntryName, zipOutputStream, job);
		}
		zipOutputStream.closeEntry();
	}

	private void writeJob(final String rootEntryName, final ZipOutputStream zipOutputStream, final Job job) throws IOException {
		final String jobEntryName = rootEntryName + job.getName() + ZIP_ENTRY_SEPARATOR;
		zipOutputStream.putNextEntry(new ZipEntry(jobEntryName));
		writeExecuteBin(jobEntryName, zipOutputStream, job);
		if (hasInputs(job)) {
			writeInputs(jobEntryName, zipOutputStream, job);
		}
		zipOutputStream.closeEntry();
	}

	// gUSE requires an executable script named execute.bin (we use
	// job_srapper/zip_loop_start/zip_loop_end
	private void writeExecuteBin(final String rootEntryName, final ZipOutputStream zipOutputStream, final Job job) throws IOException {
		final String scriptContents;
		switch (job.getJobType()) {
			case Generator :
				scriptContents = generateGeneratorScript(job);
				break;
			case Collector :
				scriptContents = generateCollectorScript(job);
				break;
			default :
				scriptContents = generateDefaultScript(job);
		}
		zipOutputStream.putNextEntry(new ZipEntry(rootEntryName + "execute.bin"));
		zipOutputStream.write(scriptContents.getBytes());
		zipOutputStream.closeEntry();
	}

	private String generateGeneratorScript(final Job job) throws IOException {
		return loadScript("zip_loop_start.sh", "@@PORT_NAME@@", job.getInputByPortNr(0).getName());
	}

	private String generateCollectorScript(final Job job) throws IOException {
		return loadScript("zip_loop_end.sh", "@@BASE_PORT_NAME@@", job.getInputByPortNr(0).getName());
	}

	private String generateDefaultScript(final Job job) throws IOException {
		final StringBuilder fileListInputs = new StringBuilder();
		final StringBuilder fileListOutputs = new StringBuilder();

		for (final Input input : job.getInputs()) {
			if (input.isMultiFile()) {
				if (fileListInputs.length() > 0) {
					// not the first element, we can prepend a space
					fileListInputs.append(' ');
				}
				fileListInputs.append(input.getName());
			}
		}
		for (final Output output : job.getOutputs()) {
			if (output.isMultiFile()) {
				if (fileListOutputs.length() > 0) {
					fileListOutputs.append(' ');
				}
				fileListOutputs.append(output.getName());
			}
		}

		// script handles empty variables for input/output ports with filelist
		return loadScript("job_wrapper.sh", "@@EXECUTABLE@@", job.getRemoteApplication().getPath(), "@@INPUT_PORTS_WITH_FILELIST@@", fileListInputs.toString(),
				"@@OUTPUT_PORTS_WITH_FILELIST@@", fileListOutputs.toString());
	}

	// loads a script from file,
	// substitutions are given as varargs: key1, val1, key2, val2, key3, val3
	private String loadScript(final String scriptName, final String... substitutions) throws IOException {
		// at the very least check that we have an odd-numbered amount of varargs
		Validate.isTrue(substitutions.length % 2 == 0, "Substitutions are given as follows: key1, val1, key2, val2...; this is a bug!");
		try (final InputStream scriptStream = this.getClass().getResourceAsStream(scriptName)) {
			String scriptContents = IOUtils.toString(scriptStream, StandardCharsets.UTF_8.name());
			// yeah, we're doing it old-school
			for (int i = 0; i < substitutions.length; i += 2) {
				scriptContents = scriptContents.replaceAll(substitutions[i], substitutions[i + 1]);
			}
			return scriptContents;
		}

	}

	private boolean hasInputs(final Job job) {
		for (final Input input : job.getInputs()) {
			if (input.getConnectionType() == ConnectionType.UserProvided) {
				return true;
			}
		}
		return false;
	}

	// this method assumes that there are true inputs to write
	private void writeInputs(final String rootEntryName, final ZipOutputStream zipOutputStream, final Job job) throws IOException {
		final String jobInputsFolderName = rootEntryName + "inputs" + ZIP_ENTRY_SEPARATOR;
		zipOutputStream.putNextEntry(new ZipEntry(jobInputsFolderName));
		for (final Input input : job.getInputs()) {
			if (input.getConnectionType() == ConnectionType.UserProvided) {
				final String inputFolderName = jobInputsFolderName + input.getPortNr() + ZIP_ENTRY_SEPARATOR;
				// each input goes into its own folder and the filename must be named "0"
				zipOutputStream.putNextEntry(new ZipEntry(inputFolderName + '0'));
				writeInput(zipOutputStream, input);
				zipOutputStream.closeEntry();
			}
		}
		zipOutputStream.closeEntry();
	}

	private void writeInput(final ZipOutputStream zipOutputStream, final Input input) throws IOException {
		if (input.isMultiFile()) {
			zipOutputStream.write(getFileListAsArchiveBytes(input));

		} else {
			zipOutputStream.write(Files.readAllBytes(Paths.get(((FileParameter) input.getData()).getValue())));
		}
	}

	private byte[] getFileListAsArchiveBytes(final Input input) throws IOException {
		final FileListParameter data = (FileListParameter) input.getData();

		final ByteArrayOutputStream rawOutputStream = new ByteArrayOutputStream(4096);
		final GZIPOutputStream gzOutputStream = new GZIPOutputStream(new BufferedOutputStream(rawOutputStream));
		final TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(gzOutputStream);

		for (final String inputFileName : data.getValue()) {
			final File inputFile = new File(inputFileName);
			tarOutputStream.putArchiveEntry(new TarArchiveEntry(inputFile, inputFile.getName()));
			final FileInputStream fileStream = new FileInputStream(inputFile);
			final BufferedInputStream bufferedStream = new BufferedInputStream(fileStream);
			IOUtils.copy(bufferedStream, tarOutputStream);
			tarOutputStream.closeArchiveEntry();
			fileStream.close();
			bufferedStream.close();
		}

		tarOutputStream.close();

		return rawOutputStream.toByteArray();
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
		document.appendChild(workflowElement);
		final String workflowName = workflow.getName() + "KNIME_export_";
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

		// graph (abstract)
		for (final Job job : workflow.getJobs()) {
			final Element jobElement = document.createElement("job");
			grafElement.appendChild(jobElement);
			jobElement.setAttribute("name", job.getName());
			jobElement.setAttribute("text", job.getDescription());
			jobElement.setAttribute("x", Integer.toString(job.getX()));
			jobElement.setAttribute("y", Integer.toString(job.getY()));

			// inputs
			for (final Input input : job.getInputs()) {
				// in the case of inputs whose source is the 'Input File' node we can use this as REAL inputs in
				// wspgrade (i.e., the user would have to actually upload something) in wspgrade, if prejob and
				// preoutput attributes are empty, it means that the port needs to be configured,
				// since it's not a channel
				final Element inputElement = document.createElement("input");
				jobElement.appendChild(inputElement);
				inputElement.setAttribute("name", fixPortName(input));
				inputElement.setAttribute("prejob", getPreJob(workflow, input));
				inputElement.setAttribute("preoutput", getPreOutput(input));
				inputElement.setAttribute("seq", Integer.toString(input.getPortNr()));
				inputElement.setAttribute("text", "Port description");
				inputElement.setAttribute("x", Integer.toString(input.getX()));
				inputElement.setAttribute("y", Integer.toString(input.getY()));
			}
			// outputs
			for (final Output output : job.getOutputs()) {
				final Element outputElement = document.createElement("output");
				jobElement.appendChild(outputElement);
				outputElement.setAttribute("name", fixPortName(output));
				outputElement.setAttribute("seq", Integer.toString(output.getPortNr()));
				outputElement.setAttribute("text", "Description of Port");
				outputElement.setAttribute("x", Integer.toString(output.getX()));
				outputElement.setAttribute("y", Integer.toString(output.getY()));
			}
		}

		// concrete
		final Element realElement = document.createElement("real");
		workflowElement.appendChild(realElement);
		realElement.setAttribute("abst", "");
		realElement.setAttribute("graf", workflowName);
		realElement.setAttribute("name", workflowName);
		realElement.setAttribute("text", "Workflow generated by the KNIME2grid plug-in.");

		for (final Job job : workflow.getJobs()) {
			final Element jobElement = document.createElement("job");
			realElement.appendChild(jobElement);
			jobElement.setAttribute("name", job.getName());
			jobElement.setAttribute("text", job.getDescription());
			jobElement.setAttribute("x", Integer.toString(job.getX()));
			jobElement.setAttribute("y", Integer.toBinaryString(job.getY()));

			addExecutionProperty(jobElement, "type", "Sequence");
			addExecutionProperty(jobElement, "params", generateCommandLine(job));
			addExecutionProperty(jobElement, "jobistype", "binary");
			addMiddlewareSpecificProperties(jobElement, job);

			// inputs
			for (final Input input : job.getInputs()) {
				final Element inputElement = document.createElement("input");
				jobElement.appendChild(inputElement);
				inputElement.setAttribute("name", input.getName());
				inputElement.setAttribute("prejob", getPreJob(workflow, input));
				inputElement.setAttribute("preoutput", getPreOutput(input));
				inputElement.setAttribute("seq", Integer.toString(input.getPortNr()));
				inputElement.setAttribute("text", "Port description");
				// FIXME: x, y for ports? These values have to be scaled, but ain't nobody got
				// time for that
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
			for (final Output output : job.getOutputs()) {
				final Element outputElement = document.createElement("output");
				jobElement.appendChild(outputElement);
				outputElement.setAttribute("name", fixPortName(output));
				outputElement.setAttribute("seq", Integer.toString(output.getPortNr()));
				outputElement.setAttribute("text", "Description of Port");
				outputElement.setAttribute("x", Integer.toString(output.getX()));
				outputElement.setAttribute("y", Integer.toString(output.getY()));

				final String mainCount = output.getConnectionType() == ConnectionType.Generator ? "2" : "1";
				addConcretePortProperty(outputElement, "maincount0", mainCount);
				addConcretePortProperty(outputElement, "intname", fixPortName(output));
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
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		transformer.setOutputProperty(OutputKeys.STANDALONE, "no");

		final StringWriter writer = new StringWriter();
		transformer.transform(new DOMSource(document), new StreamResult(writer));
		builder.append(writer.toString());
	}

	private String getPreJob(final Workflow workflow, final Input input) {
		if (input.getConnectionType() != ConnectionType.UserProvided) {
			return workflow.getJob(input.getSourceId()).getName();
		}
		return "";
	}

	private String getPreOutput(final Input input) {
		if (input.getConnectionType() != ConnectionType.UserProvided) {
			return Integer.toString(input.getSourcePortNr());
		}
		return "";
	}

	// gUSE requires port names to match the associated filename (including extension)
	// our zip_loop_start/end scripts depend on the use of '_'
	private String fixPortName(final Port port) {
		final IFileParameter associatedFile = port.getData();
		String fixedPortName = port.getName();
		if (associatedFile != null) {
			if (associatedFile instanceof FileParameter) {
				fixedPortName += '.' + FilenameUtils.getExtension(((FileParameter) associatedFile).getValue());
			} else if (associatedFile instanceof FileListParameter) {
				// guse doesn't support file lists, so we will provide an archive
				fixedPortName += ".tar.gz";
			}
		}
		return fixedPortName;
	}

	private String generateCommandLine(final Job job) {
		final StringBuilder builder = new StringBuilder();
		for (final CommandLineElement element : job.getCommandLine()) {
			builder.append(element.getStringRepresentation()).append(' ');
		}
		return builder.toString();
	}

	private void addMiddlewareSpecificProperties(final Element jobElement, final Job job) {
		final Application remoteApplication = job.getRemoteApplication();
		if (remoteApplication != null) {
			addExecutionProperty(jobElement, "gridtype", remoteApplication.getOwningResource().getType());
			addExecutionProperty(jobElement, "resource", job.getRemoteQueue() != null ? job.getRemoteQueue().getName() : "");
			addExecutionProperty(jobElement, "grid", remoteApplication.getOwningResource().getName());
			// FIXME: include code handling UNICORE (keeping old prototype code here for
			// reference)
			// addExecutionProperty(builder, "gridtype", "unicore");
			// addExecutionProperty(builder, "resource",
			// "flavus.informatik.uni-tuebingen.de:8090");
			// addExecutionProperty(builder, "grid",
			// "flavus.informatik.uni-tuebingen.de:8090");
			// addExecutionProperty(builder, "jobmanager", remoteApplication.getName() + ' '
			// + remoteApplication.getVersion());
			// addDescriptionProperty(builder, "unicore.keyWalltime", "30");
			// addDescriptionProperty(builder, "unicore.keyMemory", "2000");
		}
	}

	private void addDescriptionProperty(final Element jobElement, final String key, final String value) {
		final Document document = jobElement.getOwnerDocument();
		final Element descriptionElement = document.createElement("description");
		jobElement.appendChild(descriptionElement);
		descriptionElement.setAttribute("key", StringEscapeUtils.escapeXml(key));
		descriptionElement.setAttribute("value", StringEscapeUtils.escapeXml(value));
	}

	private void addConcretePortProperty(final Element portElement, final String key, final String value) {
		final Document document = portElement.getOwnerDocument();
		final Element portPropertiesElement = document.createElement("port_prop");
		portElement.appendChild(portPropertiesElement);
		portPropertiesElement.setAttribute("desc", "null");
		portPropertiesElement.setAttribute("inh", "null");
		portPropertiesElement.setAttribute("label", "null");
		portPropertiesElement.setAttribute("key", StringEscapeUtils.escapeXml(key));
		portPropertiesElement.setAttribute("value", StringEscapeUtils.escapeXml(value));
	}

	private void addExecutionProperty(final Element jobElement, final String key, final String value) {
		final Document document = jobElement.getOwnerDocument();
		final Element executeElement = document.createElement("execute");
		jobElement.appendChild(executeElement);
		executeElement.setAttribute("desc", "null");
		executeElement.setAttribute("inh", "null");
		executeElement.setAttribute("label", "null");
		executeElement.setAttribute("key", StringEscapeUtils.escapeXml(key));
		executeElement.setAttribute("value", StringEscapeUtils.escapeXml(value));
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
}
