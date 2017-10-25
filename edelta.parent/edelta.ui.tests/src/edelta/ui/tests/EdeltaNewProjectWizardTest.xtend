package edelta.ui.tests

import com.google.inject.Inject
import com.google.inject.Provider
import edelta.testutils.PDETargetPlatformUtils
import edelta.ui.tests.utils.EdeltaTestableNewProjectWizard
import edelta.ui.tests.utils.PluginProjectHelper
import org.eclipse.jface.viewers.StructuredSelection
import org.eclipse.jface.wizard.Wizard
import org.eclipse.jface.wizard.WizardDialog
import org.eclipse.ui.PlatformUI
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.ui.testing.AbstractWorkbenchTest
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.*

@RunWith(XtextRunner)
@InjectWith(EdeltaUiInjectorProvider)
class EdeltaNewProjectWizardTest extends AbstractWorkbenchTest {

	@Inject protected PluginProjectHelper projectHelper

	@Inject Provider<EdeltaTestableNewProjectWizard> wizardProvider

	/**
	 * Create the wizard dialog, open it and press Finish.
	 */
	def protected int createAndFinishWizardDialog(Wizard wizard) {
		val dialog = new WizardDialog(wizard.shell, wizard) {
			override open() {
				val thread = new Thread("Press Finish") {
					override run() {
						// wait for the shell to become active
						var attempt = 0
						while (getShell() === null && (attempt++) < 5) {
							println("Waiting for shell to become active")
							Thread.sleep(5000)
						}
						getShell().getDisplay().syncExec[
							println("perform finish")
							//finishPressed();
							wizard.performFinish
							println("finish performed")
							println("closing shell")
							getShell().close;
						]
						attempt = 0
						while (getShell() !== null && (attempt++) < 5) {
							println("Waiting for shell to be disposed")
							Thread.sleep(5000)
						}
					}
				};
				thread.start();
				return super.open();
			}
		};
		return dialog.open();
	}

	@BeforeClass
	def static void beforeClass() {
		PDETargetPlatformUtils.setTargetPlatform();
	}

	@Test def void testEdeltaNewProjectWizard() {
		println("Creating new project wizard...")
		val wizard = wizardProvider.get
		wizard.init(PlatformUI.getWorkbench(), new StructuredSelection());
		println("Using wizard...")
		createAndFinishWizardDialog(wizard)
		val project = root.getProject(EdeltaTestableNewProjectWizard.TEST_PROJECT)
		assertTrue(project.exists())
		println("Waiting for build...")
		waitForBuild
		projectHelper.assertNoErrors
		println("No errors in project, OK!")
	}
}
