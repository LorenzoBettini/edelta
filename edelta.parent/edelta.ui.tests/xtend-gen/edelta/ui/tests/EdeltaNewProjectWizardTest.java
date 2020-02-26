package edelta.ui.tests;

import com.google.inject.Inject;
import com.google.inject.Provider;
import edelta.ui.tests.EdeltaUiInjectorProvider;
import edelta.ui.tests.utils.EdeltaTestableNewProjectWizard;
import edelta.ui.tests.utils.PluginProjectHelper;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.xtext.testing.Flaky;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.testing.AbstractWorkbenchTest;
import org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.InputOutput;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaUiInjectorProvider.class)
@SuppressWarnings("all")
public class EdeltaNewProjectWizardTest extends AbstractWorkbenchTest {
  @Inject
  protected PluginProjectHelper projectHelper;
  
  @Inject
  private Provider<EdeltaTestableNewProjectWizard> wizardProvider;
  
  @Rule
  public Flaky.Rule testRule = new Flaky.Rule();
  
  /**
   * Create the wizard dialog, open it and press Finish.
   */
  protected int createAndFinishWizardDialog(final Wizard wizard) {
    Shell _shell = wizard.getShell();
    final WizardDialog dialog = new WizardDialog(_shell, wizard) {
      @Override
      public int open() {
        final Thread thread = new Thread("Press Finish") {
          @Override
          public void run() {
            try {
              int attempt = 0;
              while (((getShell() == null) && (attempt++ < 5))) {
                {
                  InputOutput.<String>println("Waiting for shell to become active");
                  Thread.sleep(5000);
                }
              }
              final Runnable _function = () -> {
                InputOutput.<String>println("perform finish");
                getWizard().performFinish();
                InputOutput.<String>println("finish performed");
                InputOutput.<String>println("closing shell");
                getShell().close();
              };
              getShell().getDisplay().syncExec(_function);
              attempt = 0;
              while (((getShell() != null) && (attempt++ < 5))) {
                {
                  InputOutput.<String>println("Waiting for shell to be disposed");
                  Thread.sleep(5000);
                }
              }
            } catch (Throwable _e) {
              throw Exceptions.sneakyThrow(_e);
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
  public void testEdeltaNewProjectWizard() {
    InputOutput.<String>println("*** Executing testEdeltaNewProjectWizard...");
    InputOutput.<String>println("Creating new project wizard...");
    final EdeltaTestableNewProjectWizard wizard = this.wizardProvider.get();
    IWorkbench _workbench = PlatformUI.getWorkbench();
    StructuredSelection _structuredSelection = new StructuredSelection();
    wizard.init(_workbench, _structuredSelection);
    InputOutput.<String>println("Using wizard...");
    this.createAndFinishWizardDialog(wizard);
    final IProject project = IResourcesSetupUtil.root().getProject(EdeltaTestableNewProjectWizard.TEST_PROJECT);
    Assert.assertTrue(project.exists());
    InputOutput.<String>println("Waiting for build...");
    IResourcesSetupUtil.waitForBuild();
    this.projectHelper.assertNoErrors();
    InputOutput.<String>println("No errors in project, OK!");
  }
}
