package com.workflowconversion.knime2guse;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.workflowconversion.knime2guse.export.KnimeWorkflowExporter;
import com.workflowconversion.knime2guse.export.KnimeWorkflowExporterProvider;
import com.workflowconversion.knime2guse.export.handler.NodeContainerConverter;
import com.workflowconversion.knime2guse.export.handler.impl.DefaultKnimeNodeConverter;
import com.workflowconversion.knime2guse.export.handler.impl.GenericKnimeNodeConverter;
import com.workflowconversion.knime2guse.export.impl.BashKnimeWorkflowExporter;
import com.workflowconversion.knime2guse.export.impl.GuseKnimeWorkflowExporter;

/**
 * The activator class controls the plug-in life cycle
 */
public class KnimeWorkflowExporterActivator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.workflowconversion.knime2guse"; //$NON-NLS-1$

	// The shared instance
	private static KnimeWorkflowExporterActivator plugin;

	/**
	 * The constructor
	 */
	public KnimeWorkflowExporterActivator() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		final Collection<KnimeWorkflowExporter> knownExporters = new LinkedList<KnimeWorkflowExporter>();
		knownExporters.add(new GuseKnimeWorkflowExporter());
		knownExporters.add(new BashKnimeWorkflowExporter());
		final Collection<NodeContainerConverter> nodeConverters = new LinkedList<NodeContainerConverter>();
		// the order in which we add the handlers IS important!!!
		// so we need to from most to less specific
		nodeConverters.add(new GenericKnimeNodeConverter());
		nodeConverters.add(new DefaultKnimeNodeConverter());
		KnimeWorkflowExporterProvider.initInstance(Collections.unmodifiableCollection(knownExporters), Collections.unmodifiableCollection(nodeConverters));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static KnimeWorkflowExporterActivator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 * 
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
