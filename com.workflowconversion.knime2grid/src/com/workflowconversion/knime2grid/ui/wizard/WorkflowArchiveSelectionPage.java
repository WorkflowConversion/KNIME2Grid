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
package com.workflowconversion.knime2grid.ui.wizard;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.knime.workbench.core.util.AbstractImExPage;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;

/**
 * Based on {@link AbstractImExPage} but slightly modified to properly handle
 * {@link WizardPage#isPageComplete()}.
 * 
 * @author Luis de la Garza
 */
public class WorkflowArchiveSelectionPage extends WizardPage {

	private Text m_fileDestination;

	private String m_filename;

	private final List<String> m_extensions = new ArrayList<String>();

	private final List<String> m_extensionDescriptions = new ArrayList<String>();

	/**
	 * Creates a new wizard page.
	 * 
	 * @param title
	 *            the page's title
	 * @param description
	 *            the page's description
	 * @param export
	 *            <code>true</code> if this should be an export page,
	 *            <code>false</code> if it should be an import page
	 * 
	 */
	public WorkflowArchiveSelectionPage(final String title,
			final String description) {
		super("wizardPage", title, ImageRepository
				.getImageDescriptor(SharedImages.ExportBig));
		setDescription(description);
		m_extensions.add("*.*");
		m_extensionDescriptions.add("All files");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void createControl(final Composite parent) {
		final Composite container = new Composite(parent, SWT.NULL);
		// place components vertically
		container.setLayout(new GridLayout(1, false));

		final Group exportGroup = new Group(container, SWT.NONE);
		exportGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		final GridLayout layout = new GridLayout();
		exportGroup.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		final Label label = new Label(exportGroup, SWT.NULL);
		label.setText("Select file to export to:");

		m_fileDestination = new Text(exportGroup, SWT.BORDER | SWT.SINGLE);
		final GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		m_fileDestination.setLayoutData(gd);

		// in case users want to manually enter the destination, change the page
		// status
		m_fileDestination.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent e) {
				if (e.getSource() == m_fileDestination) {
					setPageComplete(StringUtils.isNotBlank(m_fileDestination
							.getText()));
				}
			}
		});

		final Button selectFileButton = new Button(exportGroup, SWT.PUSH);
		selectFileButton.setText("Select...");
		selectFileButton.setToolTipText("Opens a file selection dialog.");
		selectFileButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(final SelectionEvent se) {
				final FileDialog fileDialog = new FileDialog(getShell(),
						SWT.SAVE);

				fileDialog.setFilterExtensions(m_extensions
						.toArray(new String[0]));
				fileDialog.setFilterNames(m_extensionDescriptions
						.toArray(new String[0]));
				if (m_extensions.size() > 1) {
					fileDialog.setFilterIndex(1);
				}

				fileDialog.setText("Specify the export file.");
				if (m_filename != null) {
					fileDialog.setFileName(m_filename);
				}
				final String filePath = fileDialog.open();
				if (StringUtils.isNotBlank(filePath)) {
					m_filename = filePath;
					m_fileDestination.setText(filePath);
					setPageComplete(true);
				} else {
					setPageComplete(false);
				}
			}

			@Override
			public void widgetDefaultSelected(final SelectionEvent se) {
				widgetSelected(se);
			}
		});

		setControl(container);
	}

	/**
	 * Returns the destination XML file.
	 * 
	 * @return a filename
	 */
	public String getFile() {
		return m_fileDestination.getText();
	}

	@Override
	public boolean isPageComplete() {
		return StringUtils.isNotBlank(getFile());
	}

	/**
	 * Adds a new file filter for the open/save dialog.
	 * 
	 * @param extension
	 *            a file file extension, e.g. "*.xml"; multiple extensions in
	 *            one filter can be separated by semicolon, e.g. "*.xml;*.svg"
	 * @param description
	 *            a description for the filter
	 */
	public void addFileExtensionFilter(final String extension,
			final String description) {
		m_extensions.add(extension);
		m_extensionDescriptions.add(description);
	}
}
