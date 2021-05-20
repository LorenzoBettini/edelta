package edelta.ui.tests;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.xtext.testing.Flaky;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.testing.AbstractWorkbenchTest;
import org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;
import com.google.inject.Provider;

import edelta.ui.tests.utils.EdeltaTestableNewProjectWizard;
import edelta.ui.tests.utils.PluginProjectHelper;

/**
 * This test requires an empty workspace
 * 
 * @author Lorenzo Bettini
 *
 */
@RunWith(XtextRunner.class)
@InjectWith(EdeltaUiInjectorProvider.class)
public class EdeltaNewProjectWizardTest extends AbstractWorkbenchTest {
	@Inject
	private Provider<EdeltaTestableNewProjectWizard> wizardProvider;

	@Rule
	public Flaky.Rule testRule = new Flaky.Rule();

	/**
	 * Create the wizard dialog, open it and press Finish.
	 */
	protected int createAndFinishWizardDialog(final Wizard wizard) {
		final WizardDialog dialog = new WizardDialog(wizard.getShell(), wizard) {
			@Override
			public int open() {
				final Thread thread = new Thread("Press Finish") {
					@Override
					public void run() {
						// wait for the shell to become active
						int attempt = 0;
						while (getShell() == null && attempt++ < 5) {
							System.out.println("Waiting for shell to become active");
							sleep();
						}
						getShell().getDisplay().syncExec(() -> {
							System.out.println("perform finish");
							getWizard().performFinish();
							System.out.println("finish performed");
							System.out.println("closing shell");
							getShell().close();
						});
						attempt = 0;
						while (getShell() != null && attempt++ < 5) {
							System.out.println("Waiting for shell to be disposed");
							sleep();
						}
					}

					@SuppressWarnings("all")
					private void sleep() {
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							throw Exceptions.sneakyThrow(e);
						}
					}
				};
				thread.start();
				return super.open();
			}
		};
		return dialog.open();
	}

	@Test
	@Flaky
	public void testEdeltaNewProjectWizard() throws CoreException {
		System.out.println("*** Executing testEdeltaNewProjectWizard...");
		System.out.println("Creating new project wizard...");
		final EdeltaTestableNewProjectWizard wizard = wizardProvider.get();
		wizard.init(PlatformUI.getWorkbench(), new StructuredSelection());
		System.out.println("Using wizard...");
		createAndFinishWizardDialog(wizard);
		final IProject project =
			IResourcesSetupUtil.root()
				.getProject(EdeltaTestableNewProjectWizard.TEST_PROJECT);
		Assert.assertTrue(project.exists());
		System.out.println("Waiting for build...");
		IResourcesSetupUtil.waitForBuild();
		PluginProjectHelper.assertNoErrors();
		System.out.println("No errors in project, OK!");
	}
}
