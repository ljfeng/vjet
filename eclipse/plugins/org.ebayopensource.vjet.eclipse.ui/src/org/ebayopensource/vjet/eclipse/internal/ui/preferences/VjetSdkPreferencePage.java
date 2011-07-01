/*******************************************************************************
 * Copyright (c) 2005-2011 eBay Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.ebayopensource.vjet.eclipse.internal.ui.preferences;

import org.ebayopensource.dsf.jst.ts.util.ISdkEnvironment;
import org.ebayopensource.vjet.eclipse.core.sdk.VjetSdkRuntime;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.dltk.mod.ui.util.SWTFactory;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

public class VjetSdkPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
							
	// JRE Block
	private InstalledSdksBlock fJREBlock;
	private Link fCompliance;
		
	/**
	 * Constructor
	 */
	public VjetSdkPreferencePage() {
		super("VJET SDK");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	/**
	 * Find & verify the default VM.
	 */
	private void initDefaultVM() {
		ISdkEnvironment realDefault= VjetSdkRuntime.getDefaultSdkInstall();
		if (realDefault != null) {
			ISdkEnvironment[] vms= fJREBlock.getJREs();
			for (int i = 0; i < vms.length; i++) {
				ISdkEnvironment fakeVM= vms[i];
				if (fakeVM.equals(realDefault)) {
					verifyDefaultVM(fakeVM);
					break;
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite ancestor) {
		initializeDialogUnits(ancestor);
		
		noDefaultAndApplyButton();
		
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		ancestor.setLayout(layout);
		
		SWTFactory.createWrapLabel(ancestor, "", 1, 300);
//		SWTFactory.createWrapLabel(ancestor, JREMessages.JREsPreferencePage_2, 1, 300);
		SWTFactory.createVerticalSpacer(ancestor, 1);
		
		fJREBlock = new InstalledSdksBlock();
		fJREBlock.createControl(ancestor);
		Control control = fJREBlock.getControl();
		GridData data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 1;
		control.setLayoutData(data);
		
//		fJREBlock.restoreColumnSettings(JDIDebugUIPlugin.getDefault().getDialogSettings(), IJavaDebugHelpContextIds.JRE_PREFERENCE_PAGE);
					
		fCompliance = new Link(ancestor, SWT.NONE);
		fCompliance.setText("VJET SDK");
		fCompliance.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fCompliance.setVisible(false);
		fCompliance.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}
			public void widgetSelected(SelectionEvent e) {openCompliancePreferencePage();}
		});
//		PlatformUI.getWorkbench().getHelpSystem().setHelp(ancestor, IJavaDebugHelpContextIds.JRE_PREFERENCE_PAGE);		
		initDefaultVM();
		fJREBlock.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				ISdkEnvironment install = getCurrentDefaultVM();
				if (install == null) {
					setValid(false);
//					setErrorMessage(JREMessages.JREsPreferencePage_13); 
					setErrorMessage("Error"); 
				} else {
					//if we change the VM make sure the compliance level supports 
					//generated class files
					String compliance = getCurrentCompilerCompliance();
					if(!supportsCurrentCompliance(install, compliance)) {
						setMessage("Warning", IMessageProvider.WARNING);
//						setMessage(MessageFormat.format(JREMessages.JREsPreferencePage_0, new String[] {compliance}), IMessageProvider.WARNING);
						fCompliance.setVisible(true);
					}
					else {
						setMessage(null);
						fCompliance.setVisible(false);
					}
					setValid(true);
					setErrorMessage(null);
				}
			}
		});
		applyDialogFont(ancestor);
		return ancestor;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#isValid()
	 */
	public boolean isValid() {
		String compliance = getCurrentCompilerCompliance();
		if(!supportsCurrentCompliance(getCurrentDefaultVM(), compliance)) {
//			setMessage(MessageFormat.format(JREMessages.JREsPreferencePage_0, new String[] {compliance}), IMessageProvider.WARNING);
			setMessage("", IMessageProvider.WARNING);
//			fCompliance.setVisible(true);
		}
		else {
			setMessage(null);
//			fCompliance.setVisible(false);
		}
		return super.isValid();
	}

	/**
	 * Opens the <code>CompliancePreferencePage</code>
	 * @since 3.3
	 */
	private void openCompliancePreferencePage() {
		String compliancepage = "org.eclipse.jdt.ui.preferences.CompliancePreferencePage"; //$NON-NLS-1$
		IWorkbenchPreferenceContainer wpc = (IWorkbenchPreferenceContainer)getContainer();
		if (wpc != null) {
			wpc.openPage(compliancepage, null);
		} else {
			SWTFactory.showPreferencePage(compliancepage);
		}
	}
	
	/**
	 * @return the current compiler compliance level
	 * @since 3.3
	 */
	private String getCurrentCompilerCompliance() {
		IEclipsePreferences setting = new InstanceScope().getNode(JavaCore.PLUGIN_ID);
		if(getContainer() instanceof IWorkbenchPreferenceContainer) {
			IEclipsePreferences wcs = ((IWorkbenchPreferenceContainer)getContainer()).getWorkingCopyManager().getWorkingCopy(setting);
			return wcs.get(JavaCore.COMPILER_COMPLIANCE, (String) JavaCore.getDefaultOptions().get(JavaCore.COMPILER_COMPLIANCE));
		} else {
			return JavaCore.getOption(JavaCore.COMPILER_COMPLIANCE);
		}
		
	}
	
	/**
	 * Determines if the vm version will run the currently compiled code based on the compiler compliance lvl
	 * @param vm the vm install
	 * @param compliance the current compiler compliance level
	 * @return true if the selected vm will run the current code, false otherwise
	 * @since 3.3
	 */
	private boolean supportsCurrentCompliance(ISdkEnvironment vm, String compliance) {
//		if(vm instanceof ISdkEnvironment) {
//			ISdkEnvironment install = (ISdkEnvironment) vm;
//			String vmver = install.getJavaVersion();
//			if(vmver == null) {
//				//if we cannot get a version from the VM we must return true, and let the runtime
//				//error sort it out
//				return true;
//			}
//			int val = compliance.compareTo(vmver); 
//			return  val < 0 || val == 0;
//		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	public boolean performOk() {
		final boolean[] canceled = new boolean[] {false};
		BusyIndicator.showWhile(null, new Runnable() {
			public void run() {
				ISdkEnvironment defaultVM = getCurrentDefaultVM();
				ISdkEnvironment[] vms = fJREBlock.getJREs();
//				JREsUpdater updater = new JREsUpdater();
//				if (!updater.updateJRESettings(vms, defaultVM)) {
//					canceled[0] = true;
//				}
			}
		});
		
		if(canceled[0]) {
			return false;
		}
		
		// save column widths
//		IDialogSettings settings = JDIDebugUIPlugin.getDefault().getDialogSettings();
//		fJREBlock.saveColumnSettings(settings, IJavaDebugHelpContextIds.JRE_PREFERENCE_PAGE);
		
		return super.performOk();
	}	
	
	protected IJavaModel getJavaModel() {
		return JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
	}

	/**
	 * Verify that the specified VM can be a valid default VM.  This amounts to verifying
	 * that all of the VM's library locations exist on the file system.  If this fails,
	 * remove the VM from the table and try to set another default.
	 */
	private void verifyDefaultVM(ISdkEnvironment vm) {
		if (vm != null) {
			
			// Verify that all of the specified VM's library locations actually exist
//			LibraryLocation[] locations= VjetSdkRuntime.getLibraryLocations(vm);
//			boolean exist = true;
//			for (int i = 0; i < locations.length; i++) {
//				exist = exist && new File(locations[i].getSystemLibraryPath().toOSString()).exists();
//			}
//			
//			// If all library locations exist, check the corresponding entry in the list,
//			// otherwise remove the VM
//			if (exist) {
//				fJREBlock.setCheckedJRE(vm);
//			} else {
//				fJREBlock.removeJREs(new ISdkEnvironment[]{vm});
//				ISdkEnvironment def = VjetSdkRuntime.getDefaultSdkInstall();
//				if (def == null) {
//					fJREBlock.setCheckedJRE(null);
//				} else {
//					fJREBlock.setCheckedJRE(def);
//				}
//				ErrorDialog.openError(getControl().getShell(), JREMessages.JREsPreferencePage_1, JREMessages.JREsPreferencePage_10, new Status(IStatus.ERROR, IJavaDebugUIConstants.PLUGIN_ID, IJavaDebugUIConstants.INTERNAL_ERROR, JREMessages.JREsPreferencePage_11, null)); //  
//				return;
//			}
		} else {
			fJREBlock.setCheckedJRE(null);
		}
	}
	
	private ISdkEnvironment getCurrentDefaultVM() {
		return fJREBlock.getCheckedJRE();
	}
}
