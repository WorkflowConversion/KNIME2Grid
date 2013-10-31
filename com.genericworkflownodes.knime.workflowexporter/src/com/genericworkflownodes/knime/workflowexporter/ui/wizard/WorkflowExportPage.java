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
package com.genericworkflownodes.knime.workflowexporter.ui.wizard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.ui.wizards.export.WorkflowExportWizard;

import com.genericworkflownodes.knime.workflowexporter.export.KnimeWorkflowExporter;
import com.genericworkflownodes.knime.workflowexporter.export.ui.DisplayInformationProvider;
import com.genericworkflownodes.knime.workflowexporter.format.ExtensionFilter;

/**
 * Class based on {@link WorkflowExportWizard}.
 * 
 * The main difference is that this page opens a single-selection dialog and
 * does not display a tree viewer.
 * 
 * @author Luis de la Garza
 */
public class WorkflowExportPage extends WizardPage {
    /**
	 * 
	 */
    private static final int HORIZONTAL_SPACING = 9;
    private final ArrayList<KnimeWorkflowExporter> exporters;
    private int selectedIndex;
    private String destinationFilePath;
    private String exportMode;

    /**
     * @param pageName
     * @param selection
     */
    public WorkflowExportPage(final Collection<KnimeWorkflowExporter> exporters) {
	super("com.genericworkflownodes.knime.ui.wizard.WorkflowExportPage", "Select the destination format", ImageRepository
		.getImageDescriptor(SharedImages.ExportBig));
	Validate.notEmpty(exporters, "exporters cannot be null or empty");
	this.exporters = new ArrayList<KnimeWorkflowExporter>(exporters);
	this.selectedIndex = -1;
    }

    /**
     * Gets the selected exporter. If no exporter has been selected yet, an
     * IllegalStateException will be thrown!
     * 
     * @return The selected exporter.
     */
    public KnimeWorkflowExporter getSelectedExporter() {
	if (selectedIndex == -1) {
	    throw new IllegalStateException("No exporter has been selected yet!");
	}
	return exporters.get(selectedIndex);
    }

    /**
     * @return the file to which the workflow will be exported.
     */
    public String getDestinationFile() {
	return destinationFilePath;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
     */
    @Override
    public boolean isPageComplete() {
	return selectedIndex != -1 && StringUtils.isNotBlank(destinationFilePath);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets
     * .Composite)
     */
    @Override
    public void createControl(final Composite parent) {
	final Composite container = new Composite(parent, SWT.NULL);
	final GridLayout containerLayout = new GridLayout(1, false);
	container.setLayout(containerLayout);
	container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

	// ------- format selection
	final Group selectFormatGroup = new Group(container, SWT.NULL);
	final GridLayout selectFormatGroupLayout = new GridLayout(2, false);
	selectFormatGroupLayout.horizontalSpacing = HORIZONTAL_SPACING;
	selectFormatGroup.setLayout(selectFormatGroupLayout);
	selectFormatGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

	final Label formatLabel = new Label(selectFormatGroup, SWT.HORIZONTAL | SWT.LEFT);
	formatLabel.setText("Select an export format:");
	// formatLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

	final Combo formatCombo = new Combo(selectFormatGroup, SWT.PUSH | SWT.DROP_DOWN);

	formatCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	for (final DisplayInformationProvider infoProvider : exporters) {
	    formatCombo.add(infoProvider.getShortDescription());
	}
	// ------------------------

	// ------- export mode
	final Group selectExportModeGroup = new Group(container, SWT.NULL);
	final GridLayout selectExportModeGroupLayout = new GridLayout(2, false);
	selectExportModeGroupLayout.horizontalSpacing = HORIZONTAL_SPACING;
	selectExportModeGroup.setLayout(selectExportModeGroupLayout);
	selectExportModeGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

	final Label exportModeLabel = new Label(selectExportModeGroup, SWT.HORIZONTAL | SWT.LEFT);
	exportModeLabel.setText("Select an export mode:");

	final Combo exportModeCombo = new Combo(selectExportModeGroup, SWT.PUSH | SWT.DROP_DOWN);
	exportModeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	// -------------------------

	// ------- file destination
	final Group selectDestinationGroup = new Group(container, SWT.NULL);
	final GridLayout selectDestinationGroupLayout = new GridLayout(3, false);
	selectDestinationGroupLayout.horizontalSpacing = HORIZONTAL_SPACING;
	selectDestinationGroup.setLayout(selectDestinationGroupLayout);
	selectDestinationGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

	final Label destinationLabel = new Label(selectDestinationGroup, SWT.NULL);
	destinationLabel.setText("Select an export destination:");

	final Text destinationText = new Text(selectDestinationGroup, SWT.BORDER | SWT.SINGLE);
	destinationText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

	final Button destinationButton = new Button(selectDestinationGroup, SWT.PUSH);
	destinationButton.setText("Browse...");
	// ------------------------

	// ---------- format information display
	final Group infoDisplayGroup = new Group(container, SWT.NULL);
	final GridLayout infoDisplayGroupLayout = new GridLayout(2, false);
	infoDisplayGroupLayout.horizontalSpacing = HORIZONTAL_SPACING;
	infoDisplayGroup.setLayout(infoDisplayGroupLayout);
	infoDisplayGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

	final Label image = new Label(infoDisplayGroup, SWT.NULL);
	image.setSize(150, 150);
	final Label description = new Label(infoDisplayGroup, SWT.NULL);
	description.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	// -------------------------------------

	// ------------------- listeners / action handlers
	// formatCombo.addModifyListener(new ModifyListener() {
	// @Override
	// public void modifyText(final ModifyEvent e) {
	// if (e.getSource() == formatCombo) {
	// selectedIndex = formatCombo.getSelectionIndex();
	// if (selectedIndex > -1) {
	// final KnimeWorkflowExporter selectedExporter =
	// exporters.get(selectedIndex);
	// // change the image and the description
	// final Image scaledPic = ImageDescriptor.createFromImageData(
	// selectedExporter.getImageDescriptor().getImageData().scaledTo(150,
	// 150)).createImage();
	// image.setImage(scaledPic);
	// description.setText(selectedExporter.getLongDescription());
	// //
	// final Collection<String> exportModes =
	// selectedExporter.getSupportedExportModes();
	// exportModeCombo.setEnabled(exportModes != null &&
	// exportModes.isEmpty());
	// exportModeCombo.removeAll();
	// for (final String exportMode :
	// selectedExporter.getSupportedExportModes()) {
	// exportModeCombo.add(exportMode);
	// }
	// }
	// }
	// updatePageComplete();
	// }
	// });

	formatCombo.addModifyListener(new ModifyListener() {
	    @Override
	    public void modifyText(final ModifyEvent e) {
		if (e.getSource() == formatCombo) {
		    selectedIndex = formatCombo.getSelectionIndex();
		    if (selectedIndex > -1) {
			final KnimeWorkflowExporter selectedExporter = exporters.get(selectedIndex);
			// change the image and the description
			final Image scaledPic = ImageDescriptor.createFromImageData(
				selectedExporter.getImageDescriptor().getImageData().scaledTo(150, 150)).createImage();
			image.setImage(scaledPic);
			description.setText(selectedExporter.getLongDescription());
			//
			final Collection<String> exportModes = selectedExporter.getSupportedExportModes();
			exportModeCombo.setEnabled(exportModes != null && !exportModes.isEmpty());
			exportModeCombo.removeAll();
			for (final String exportMode : selectedExporter.getSupportedExportModes()) {
			    exportModeCombo.add(exportMode);
			}
			if (exportModeCombo.isEnabled()) {
			    exportModeCombo.select(0);
			} else {
			    exportMode = null;
			}
		    }
		}
	    }
	});

	exportModeCombo.addModifyListener(new ModifyListener() {
	    @Override
	    public void modifyText(final ModifyEvent e) {
		if (e.getSource() == exportModeCombo) {
		    exportMode = exportModeCombo.getItem(exportModeCombo.getSelectionIndex());
		}
	    }
	});

	destinationText.addModifyListener(new ModifyListener() {
	    @Override
	    public void modifyText(final ModifyEvent e) {
		if (e.getSource() == destinationText) {
		    destinationFilePath = destinationText.getText();
		}
		updatePageComplete();
	    }
	});

	destinationButton.addSelectionListener(new SelectionListener() {
	    @Override
	    public void widgetSelected(final SelectionEvent e) {
		if (e.getSource() == destinationButton) {
		    final FileDialog fileDialog = new FileDialog(getShell(), SWT.SAVE);
		    fileDialog.setOverwrite(true);
		    final KnimeWorkflowExporter selectedExporter = exporters.get(selectedIndex);
		    final Collection<ExtensionFilter> extensionFilters = selectedExporter.getExtensionFilters();
		    final String[] filterExtensions = new String[extensionFilters.size()];
		    final String[] filterNames = new String[extensionFilters.size()];
		    int i = 0;
		    for (final Iterator<ExtensionFilter> it = extensionFilters.iterator(); it.hasNext(); i++) {
			final ExtensionFilter current = it.next();
			filterExtensions[i] = current.getFilter();
			filterNames[i] = current.getDescription();
		    }
		    fileDialog.setFilterExtensions(filterExtensions);
		    fileDialog.setFilterNames(filterNames);
		    if (extensionFilters.size() > 1) {
			fileDialog.setFilterIndex(1);
		    }

		    fileDialog.setText("Specify the export file.");

		    final String filePath = fileDialog.open();
		    if (StringUtils.isNotBlank(filePath)) {
			destinationFilePath = filePath;
			destinationText.setText(filePath);
		    }
		}
		updatePageComplete();
	    }

	    @Override
	    public void widgetDefaultSelected(final SelectionEvent e) {
		widgetSelected(e);
	    }
	});
	// -----------------------------------

	// make a default selection
	formatCombo.select(0);

	setControl(container);
    }

    private void updatePageComplete() {
	setPageComplete(selectedIndex > -1 && StringUtils.isNotBlank(destinationFilePath));
    }

    /**
     * Gets the currently selected export mode, or {@code null} if none has been
     * selected.
     * 
     * @return The export mode.
     */
    public String getExportMode() {
	return exportMode;
    }
}
