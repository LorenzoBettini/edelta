/**
 * 
 */
package edelta.ui.tests.utils;

import org.eclipse.xtext.ui.wizard.IExtendedProjectInfo;
import org.eclipse.xtext.ui.wizard.template.TemplateNewProjectWizard;

/**
 * Manually set the project name (usually set in the dialog text edit)
 * 
 * @author Lorenzo Bettini
 */
public class EdeltaTestableNewProjectWizard extends TemplateNewProjectWizard {

	public static final String TEST_PROJECT = "TestProject";

	@Override
	public IExtendedProjectInfo getProjectInfo() {
		IExtendedProjectInfo projectInfo = super.getProjectInfo();
		projectInfo.setProjectName(TEST_PROJECT);
		return projectInfo;
	}
}
