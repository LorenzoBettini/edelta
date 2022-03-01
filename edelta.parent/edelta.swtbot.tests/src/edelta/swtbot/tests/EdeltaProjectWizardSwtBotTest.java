package edelta.swtbot.tests;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Lorenzo Bettini
 *
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class EdeltaProjectWizardSwtBotTest extends EdeltaAbstractSwtbotTest {

	@Test
	public void canCreateANewProject() throws CoreException, OperationCanceledException, InterruptedException {
		bot.menu("File").menu("New").menu("Project...").click();

		SWTBotShell shell = bot.shell("New Project");
		shell.activate();
		SWTBotTreeItem categoryNode = bot.tree().expandNode(CATEGORY_NAME);
		categoryNode.select("Edelta Project");
		bot.button("Next >").click();

		bot.textWithLabel("Project name:").setText(TEST_PROJECT);

		bot.button("Finish").click();

		// creation of a project might require some time
		bot.waitUntil(shellCloses(shell), SWTBotPreferences.TIMEOUT);
		assertTrue("Project doesn't exist: " + TEST_PROJECT, isProjectCreated(TEST_PROJECT));

//		System.out.println("Waiting for build...");
//		waitForBuild();
		System.out.println("Waiting for auto build...");
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
		assertErrorsInProject(0);
		getProjectTreeItem(TEST_PROJECT)
			.expand()
			.expandNode("edelta-gen", "com.example", "Example.java");
	}

}
