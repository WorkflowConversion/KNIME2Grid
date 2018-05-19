package com.workflowconversion.knime2grid.resource.impl;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.knime.core.node.NodeLogger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.workflowconversion.knime2grid.resource.Application;
import com.workflowconversion.knime2grid.resource.Queue;
import com.workflowconversion.knime2grid.resource.Resource;
import com.workflowconversion.knime2grid.resource.ResourceProvider;

/**
 * Reads an XML file and makes it available via the {@link ResourceProvider} API.
 * 
 * File format:
 * 
 * <pre>
 * {@code
 * <resources>
 *   <resource name="pbs-cluster.university.eu" type="pbs">
 *     <application name="SampleApp" version="1.1" path="/usr/bin/sampleapp" description="Sample app"/>
 *     <application name="Sleepy" version="1.2" path="/usr/bin/sleep" description="Sample app, again"/>
 *     <queue name="default"/>
 *   </resource>
 *   <resource name="moab-cluster.university.eu" type="moab">
 *     <application name="MagicSauce" version="2.1" path="/share/bin/magic" description="Nobel prize, here I come!"/>
 *     <queue name="fast_jobs"/>
 *     <queue name="slow_jobs"/>
 *   </resource>
 * </resources>
 * }
 * </pre>
 * 
 * @author delagarza
 *
 */
public class XMLBasedResourceProvider implements ResourceProvider {
	private static final long serialVersionUID = 4561821596227933825L;
	private static final NodeLogger LOG = NodeLogger.getLogger(XMLBasedResourceProvider.class);

	private final File xmlFile;
	private final Collection<Resource> resources;

	/**
	 * Constructor.
	 * 
	 * @param xmlFileLocation
	 *            the path to an XML file containing resources.
	 */
	public XMLBasedResourceProvider(final File xmlFile) {
		Validate.notNull(xmlFile, "xmlFile cannot be null.");
		this.xmlFile = xmlFile;
		this.resources = new LinkedList<Resource>();
	}

	@Override
	public String getName() {
		return "XML-based resource provider";
	}

	@Override
	public void init() throws Exception {
		LOG.info(String.format("Loading resources from %s", xmlFile.getAbsolutePath()));
		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		final Document doc = dBuilder.parse(xmlFile);
		// get all <resource> nodes, then get all <application> nodes for each one
		final NodeList resourcesNodeList = doc.getElementsByTagName("resource");
		for (int i = 0; i < resourcesNodeList.getLength(); i++) {
			// <resource name="pbs-cluster.university.eu" type="pbs">
			final Element resourceElement = (Element) resourcesNodeList.item(i);
			final Resource.Builder resourceBuilder = new Resource.Builder();
			resourceBuilder.withName(StringUtils.trimToEmpty(resourceElement.getAttribute("name")));
			resourceBuilder.withType(StringUtils.trimToEmpty(resourceElement.getAttribute("type")));
			final Resource resource = resourceBuilder.newInstance();
			LOG.info(String.format("Added resource. Type=%s, name=%s", resourceElement.getAttribute("type"), resourceElement.getAttribute("name")));

			final NodeList applicationsNodeList = resourceElement.getElementsByTagName("application");
			for (int j = 0; j < applicationsNodeList.getLength(); j++) {
				// <application name="SampleApp" version="1.1" path="/usr/bin/sampleapp" description="Sample app"/>
				final Element applicationElement = (Element) applicationsNodeList.item(j);
				final Application.Builder applicationBuilder = new Application.Builder();
				applicationBuilder.withName(StringUtils.trimToEmpty(applicationElement.getAttribute("name")));
				applicationBuilder.withVersion(StringUtils.trimToEmpty(applicationElement.getAttribute("version")));
				applicationBuilder.withPath(StringUtils.trimToEmpty(applicationElement.getAttribute("path")));
				applicationBuilder.withDescription(StringUtils.trimToEmpty(applicationElement.getAttribute("description")));
				applicationBuilder.withOwningResource(resource);
				resource.addApplication(applicationBuilder.newInstance());
				LOG.info(String.format("Added application. Name=%s, version=%s", applicationElement.getAttribute("name"),
						applicationElement.getAttribute("version")));
			}

			final NodeList queuesNodeList = resourceElement.getElementsByTagName("queue");
			for (int j = 0; j < queuesNodeList.getLength(); j++) {
				// <queue name="default"/>
				final Element queueElement = (Element) queuesNodeList.item(j);
				final Queue.Builder queueBuilder = new Queue.Builder();
				queueBuilder.withName(StringUtils.trimToEmpty(queueElement.getAttribute("name")));
				resource.addQueue(queueBuilder.newInstance());
				LOG.info(String.format("Added queue. Name=%s", queueElement.getAttribute("name")));
			}

			resources.add(resource);
		}
	}

	@Override
	public Collection<Resource> getResources() {
		return Collections.unmodifiableCollection(this.resources);
	}
}
