/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids  <sdavids@gmx.de> - Fix for Bug 132156 [Dialogs] Progress Preferences dialog problems
 *******************************************************************************/
package net.schwehla.matrosdms.progress.internal;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import net.schwehla.matrosdms.progress.IProgressConstants;

/**
 * The JobsViewPreferenceDialog is the dialog that
 * allows the user to set the preferences.
 */
public class JobsViewPreferenceDialog extends ViewSettingsDialog {

	private BooleanFieldEditor showSystemJob;
	private BooleanFieldEditor runInBackground;
	private IPreferenceStore preferenceStore;


	/**
	 * Create a new instance of the receiver.
	 * @param parentShell
	 * @param preferenceStore
	 */
	public JobsViewPreferenceDialog(Shell parentShell, IPreferenceStore preferenceStore) {
		super(parentShell);
		this.preferenceStore = preferenceStore;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(ProgressMessages.JobsViewPreferenceDialog_Title);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite top = (Composite) super.createDialogArea(parent);

		Composite editArea = new Composite(top, SWT.NONE);
		editArea.setLayout(new GridLayout());
		editArea.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

		runInBackground = new BooleanFieldEditor(IProgressConstants.RUN_IN_BACKGROUND, ProgressMessages.JobsViewPreferenceDialog_RunInBackground, editArea);
		runInBackground.setPreferenceName(IProgressConstants.RUN_IN_BACKGROUND);
		runInBackground.setPreferenceStore(preferenceStore);
		runInBackground.load();

		showSystemJob = new BooleanFieldEditor(IProgressConstants.SHOW_SYSTEM_JOBS, ProgressMessages.JobsViewPreferenceDialog_ShowSystemJobs, editArea);
		showSystemJob.setPreferenceName(IProgressConstants.SHOW_SYSTEM_JOBS);
		showSystemJob.setPreferenceStore(preferenceStore);
		showSystemJob.load();

		Dialog.applyDialogFont(top);

		return top;
	}

	@Override
	protected void okPressed() {
		runInBackground.store();
		showSystemJob.store();
		super.okPressed();
	}

	@Override
	protected void performDefaults() {
		runInBackground.loadDefault();
		showSystemJob.loadDefault();
	}
}
