package edelta.swtbot.tests;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;
import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.waitForBuild;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
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
		bot.waitUntil(new ICondition() {
			@Override
			public boolean test() throws Exception {
				System.out.println("### File -> New -> Project...");
				bot.menu("File").menu("New").menu("Project...").click();
				return true;
			}

			@Override
			public void init(SWTBot bot) {
			}

			@Override
			public String getFailureMessage() {
				return "Failed File -> New -> Project...";
			}
		});

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

		bot.waitUntil(new ICondition() {
			@Override
			public boolean test() throws Exception {
				System.out.println("Waiting for the plugin model...");
				return PDECore.getDefault().getModelManager().isInitialized();
			}

			@Override
			public void init(SWTBot bot) {
			}

			@Override
			public String getFailureMessage() {
				return "Failed waiting for inizialize of plugin models";
			}
		});

		// building seems to be flaky, so better to try again in case of failure
		bot.waitUntil(new ICondition() {
			@Override
			public boolean test() throws Exception {
				System.out.println("**** WAITING FOR BUILD...");
				waitForBuild();
				Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, null);
				Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
				System.out.println("**** BUILD DONE");
				assertErrorsInProject(0);
				return true;
			}

			@Override
			public void init(SWTBot bot) {
			}

			@Override
			public String getFailureMessage() {
				return "build failed";
			}
		});

		bot.waitUntil(new ICondition() {
			@Override
			public boolean test() throws Exception {
				System.out.println("*** expanding " + TEST_PROJECT);
				getProjectTreeItem(TEST_PROJECT)
					.expand()
					.expandNode("edelta-gen", "com.example", "Example.java");
//				var expectedSrcGenFolderSubDir = "edelta-gen/com/example";
//				var srcGenFolder = project.getFolder(expectedSrcGenFolderSubDir);
//				System.out.println("contents of " + srcGenFolder);
//				System.out.println(Stream.of(srcGenFolder.members())
//						.map(IResource::getName)
//						.collect(Collectors.joining("\n")));
//				var genfile = srcGenFolder.getFile("Example.java");
				return true;
			}

			@Override
			public void init(SWTBot bot) {
			}

			@Override
			public String getFailureMessage() {
				return "Example.java does not exist";
			}
		});
	}
}
