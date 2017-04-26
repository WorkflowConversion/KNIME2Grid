package com.workflowconversion.knime2grid;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.workflowconversion.knime2grid.export.io.SourceConverter;
import com.workflowconversion.knime2grid.export.io.impl.CSVReaderConverter;
import com.workflowconversion.knime2grid.export.io.impl.ListMimeFileImporterConverter;
import com.workflowconversion.knime2grid.export.io.impl.MimeFileImporterConverter;
import com.workflowconversion.knime2grid.export.io.impl.PortObjectReaderConverter;
import com.workflowconversion.knime2grid.export.io.impl.TableReaderConverter;
import com.workflowconversion.knime2grid.export.node.NodeContainerConverter;
import com.workflowconversion.knime2grid.export.node.impl.DefaultKnimeNodeConverter;
import com.workflowconversion.knime2grid.export.node.impl.GenericKnimeNodeConverter;
import com.workflowconversion.knime2grid.export.node.impl.LoopNodeConverter;
import com.workflowconversion.knime2grid.export.workflow.KnimeWorkflowExporter;
import com.workflowconversion.knime2grid.export.workflow.KnimeWorkflowExporterProvider;
import com.workflowconversion.knime2grid.export.workflow.impl.bash.BashKnimeWorkflowExporter;
import com.workflowconversion.knime2grid.export.workflow.impl.guse.GuseKnimeWorkflowExporter;

/**
 * The activator class controls the plug-in life cycle
 */
public class KnimeWorkflowExporterActivator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.workflowconversion.knime2grid"; //$NON-NLS-1$

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
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext )
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		final Collection<KnimeWorkflowExporter> knownExporters = new LinkedList<KnimeWorkflowExporter>();
		knownExporters.add(new GuseKnimeWorkflowExporter());
		knownExporters.add(new BashKnimeWorkflowExporter());

		// the order in which we add the handlers IS important!!!
		// so we need to arrange the converters from most to less specific
		final Collection<NodeContainerConverter> nodeConverters = new LinkedList<NodeContainerConverter>();
		nodeConverters.add(new LoopNodeConverter());
		nodeConverters.add(new GenericKnimeNodeConverter());
		nodeConverters.add(new DefaultKnimeNodeConverter());

		// here the order is not very important, since we are targeting specific implementations,
		// however, PortObjectReader takes all kinds of PortObjects, so it shold be the last one
		final Collection<SourceConverter> sourceConverters = new LinkedList<SourceConverter>();
		sourceConverters.add(new CSVReaderConverter());
		sourceConverters.add(new MimeFileImporterConverter());
		sourceConverters.add(new TableReaderConverter());
		sourceConverters.add(new PortObjectReaderConverter());
		sourceConverters.add(new ListMimeFileImporterConverter());

		KnimeWorkflowExporterProvider.initInstance(Collections.unmodifiableCollection(knownExporters),
				Collections.unmodifiableCollection(nodeConverters),
				Collections.unmodifiableCollection(sourceConverters));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext )
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {
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
	 * Returns an image descriptor for the image file at the given plug-in relative path
	 * 
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(final String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
