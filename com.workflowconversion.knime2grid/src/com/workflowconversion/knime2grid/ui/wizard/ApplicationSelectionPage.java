package com.workflowconversion.knime2grid.ui.wizard;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;

import com.workflowconversion.knime2grid.model.Job;
import com.workflowconversion.knime2grid.model.Workflow;
import com.workflowconversion.knime2grid.resource.Application;
import com.workflowconversion.knime2grid.resource.Resource;
import com.workflowconversion.knime2grid.resource.ResourceProvider;
import com.workflowconversion.knime2grid.resource.impl.XMLBasedResourceProvider;

public class ApplicationSelectionPage extends WizardPage {
	private static final int HORIZONTAL_SPACING = 9;

	private static final NodeLogger LOG = NodeLogger.getLogger(ApplicationSelectionPage.class);

	private final Collection<Application> currentRemoteApplications;
	private final Job[] localJobs;

	/**
	 * Constructor.
	 */
	public ApplicationSelectionPage(final Workflow workflow) {
		super("com.workflowconversion.knime2grid.ui.wizard.ApplicationSelectionPage", "Configure your jobs",
				ImageRepository.getImageDescriptor(SharedImages.Node));
		Validate.notNull(workflow, "workflow is required and cannot be null");

		this.localJobs = workflow.getJobs().toArray(new Job[] {});
		currentRemoteApplications = new LinkedList<Application>();
	}

	@Override
	public void createControl(final Composite parent) {
		final Composite container = new Composite(parent, SWT.NULL);
		final GridLayout containerLayout = new GridLayout(1, false);
		container.setLayout(containerLayout);
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// ------- resources.xml location
		final Group resourcesFileLocationGroup = new Group(container, SWT.NULL);
		final GridLayout resourcesFileLocationGroupLayout = new GridLayout(3, false);
		resourcesFileLocationGroup.setText("Select the location of your resources.xml file");
		resourcesFileLocationGroupLayout.horizontalSpacing = HORIZONTAL_SPACING;
		resourcesFileLocationGroup.setLayout(resourcesFileLocationGroupLayout);
		resourcesFileLocationGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		// -- () Local [ ] [Browse...]
		final Button localRadioButton = new Button(resourcesFileLocationGroup, SWT.RADIO);
		localRadioButton.setText("Local");

		final Text localText = new Text(resourcesFileLocationGroup, SWT.BORDER | SWT.SINGLE);
		localText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		final Button browseButton = new Button(resourcesFileLocationGroup, SWT.PUSH);
		browseButton.setText("Browse...");
		// -- () Remote [ ]
		final Button remoteRadioButton = new Button(resourcesFileLocationGroup, SWT.RADIO);
		remoteRadioButton.setText("Remote");

		final Text remoteText = new Text(resourcesFileLocationGroup, SWT.BORDER | SWT.SINGLE);
		final GridData remoteTextGridData = new GridData(GridData.FILL_HORIZONTAL);
		remoteTextGridData.horizontalSpan = 2;
		remoteText.setLayoutData(remoteTextGridData);

		// [Refresh]
		final Button refreshButton = new Button(resourcesFileLocationGroup, SWT.PUSH);
		refreshButton.setText("Refresh resources");
		final GridData refreshButtonGridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
		refreshButtonGridData.horizontalSpan = 3;
		refreshButton.setLayoutData(refreshButtonGridData);

		// ----------- group displaying nodes and remote apps
		final Group nodesTableGroup = new Group(container, SWT.NULL);
		final GridLayout nodesTableGroupLayout = new GridLayout(1, false);
		nodesTableGroup.setText("Match local KNIME Nodes to remote resources");
		nodesTableGroupLayout.horizontalSpacing = HORIZONTAL_SPACING;
		nodesTableGroup.setLayout(nodesTableGroupLayout);
		nodesTableGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		final Table localToRemoteTable = new Table(nodesTableGroup, SWT.BORDER | SWT.H_SCROLL);
		localToRemoteTable.setHeaderVisible(true);
		final String[] columnTitles = { "Local KNIME Node", "Remote Resource" };
		for (int i = 0; i < columnTitles.length; i++) {
			TableColumn column = new TableColumn(localToRemoteTable, SWT.NULL);
			column.setText(columnTitles[i]);
			column.setWidth(350);
		}

		final Button applyButton = new Button(nodesTableGroup, SWT.PUSH);
		applyButton.setText("Apply");
		applyButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));

		// populate the table
		for (final Job job : localJobs) {
			final TableItem row = new TableItem(localToRemoteTable, SWT.NONE);
			row.setText(0, String.format("%s (id: %s)", job.getName(), job.getId().toString()));
		}
		final TableItem[] tableItems = localToRemoteTable.getItems();
		for (int i = 0; i < tableItems.length; i++) {
			final TableEditor comboEditor = new TableEditor(localToRemoteTable);
			final CCombo combo = new CCombo(localToRemoteTable, SWT.NONE);
			combo.setText("Selecte a remote application");
			comboEditor.horizontalAlignment = SWT.LEFT;
			comboEditor.grabHorizontal = true;
			comboEditor.setEditor(combo, tableItems[i], 1);
			tableItems[i].setData("combo", combo);
		}

		// bind UI controls to actions
		localRadioButton.addSelectionListener(new CustomSelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.getSource() == localRadioButton) {
					localText.setEnabled(true);
					browseButton.setEnabled(true);
					remoteText.setEnabled(false);
				}
			}
		});

		remoteRadioButton.addSelectionListener(new CustomSelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.getSource() == remoteRadioButton) {
					remoteText.setEnabled(true);
					localText.setEnabled(false);
					browseButton.setEnabled(false);
				}
			}
		});

		browseButton.addSelectionListener(new CustomSelectionListener() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (e.getSource() == browseButton) {
					final FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
					fileDialog.setText("Specify the location of a valid resources.xml file");
					fileDialog.setFilterExtensions(new String[] { "*.xml" });
					fileDialog.setFilterNames(new String[] { "XML Documents" });

					final String filePath = fileDialog.open();
					if (StringUtils.isNotBlank(filePath)) {
						localText.setText(StringUtils.trimToEmpty(filePath));
					}
				}
			}
		});

		refreshButton.addSelectionListener(new CustomSelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.getSource() == refreshButton) {
					final File resourcesFile;
					boolean success = false;
					try {
						if (localRadioButton.getSelection()) {
							resourcesFile = new File(StringUtils.trimToEmpty(localText.getText()));
						} else {
							resourcesFile = downloadResourcesFile(StringUtils.trim(remoteText.getText()));
						}
						final ResourceProvider resourceProvider = new XMLBasedResourceProvider(resourcesFile);
						resourceProvider.init();
						refreshRemoteApplicationsList(resourceProvider.getResources());
						success = true;
					} catch (Exception ex) {
						LOG.error("Could not load resources file.", ex);
					}
					if (!success) {
						MessageDialog.openError(getShell(), "KNIME - Workflow Conversion",
								"Invalid resources file provided. Check the logs for more information.");
					} else {
						for (int i = 0; i < tableItems.length; i++) {
							localJobs[i].clearRemoteApplication();
							final TableItem row = tableItems[i];
							final CCombo combo = (CCombo) row.getData("combo");
							combo.removeAll();
							combo.setText("Select a remote application");
							for (final Application application : currentRemoteApplications) {
								combo.add(String.format("%s, version %s (%s)", application.getName(), application.getVersion(),
										application.getOwningResource().getName()));

							}
						}
					}
				}
			}
		});

		applyButton.addSelectionListener(new CustomSelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.getSource() == applyButton) {
					LOG.info("Saving selected remote applications");
					final Application[] remoteApplications = currentRemoteApplications.toArray(new Application[] {});
					for (int i = 0; i < tableItems.length; i++) {
						final CCombo combo = (CCombo) tableItems[i].getData("combo");
						final int selectedIndex = combo.getSelectionIndex();
						// user selected something
						if (selectedIndex >= 0) {
							localJobs[i].setRemoteApplication(remoteApplications[i]);
						}
					}
				}
			}
		});

		// select by default
		localRadioButton.setSelection(true);
		localRadioButton.notifyListeners(SWT.Selection, new Event());
		setControl(container);
	}

	private void refreshRemoteApplicationsList(final Collection<Resource> remoteResources) {
		// "flatten" the incoming resources structure to a list of apps
		currentRemoteApplications.clear();
		for (final Resource resource : remoteResources) {
			for (final Application application : resource.getApplications()) {
				currentRemoteApplications.add(application);
			}
		}
	}

	private File downloadResourcesFile(final String url) throws Exception {
		LOG.info(String.format("Downloading resources file from %s", url));
		final URL remoteResourcesUrl = new URL(url);
		final ReadableByteChannel byteChannel = Channels.newChannel(remoteResourcesUrl.openStream());

		final File resourcesFile = File.createTempFile("com_workflow_resources", ".xml");
		resourcesFile.deleteOnExit();

		try (final FileOutputStream outputStream = new FileOutputStream(resourcesFile)) {
			outputStream.getChannel().transferFrom(byteChannel, 0, Long.MAX_VALUE);
		}

		return resourcesFile;
	}

	private static abstract class CustomSelectionListener implements SelectionListener {

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}
	}

}
