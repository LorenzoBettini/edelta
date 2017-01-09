/**
 * 
 */
package edelta.ui.tests.utils;

import org.eclipse.xtext.ui.wizard.IExtendedProjectInfo;
import org.eclipse.xtext.ui.wizard.IProjectCreator;

import com.google.inject.Inject;

import edelta.ui.wizard.EdeltaNewProjectWizard;

/**
 * Manually set the project name (usually set in the dialog text edit)
 * 
 * @author Lorenzo Bettini
 */
public class EdeltaTestableNewProjectWizard extends EdeltaNewProjectWizard {

	public static final String TEST_PROJECT = "TestProject";

	@Inject
	public EdeltaTestableNewProjectWizard(IProjectCreator projectCreator) {
		super(projectCreator);
	}

	@Override
	public IExtendedProjectInfo getProjectInfo() {
		IExtendedProjectInfo projectInfo = super.getProjectInfo();
		projectInfo.setProjectName(TEST_PROJECT);
		return projectInfo;
	}
}
