package edelta.swtbot.tests;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;
import static org.junit.Assert.assertTrue;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
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

		// maybe before we were not waiting for auto build,
		System.out.println("Waiting for auto build...");
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, null);
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
		System.out.println("Auto build done.");
		assertErrorsInProject(0);
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(TEST_PROJECT);
		bot.waitUntil(new ICondition() {
			@Override
			public boolean test() throws Exception {
				var expectedSrcGenFolderSubDir = "edelta-gen/com/example";
				var srcGenFolder = project.getFolder(expectedSrcGenFolderSubDir);
				System.out.println("contents of " + srcGenFolder);
				System.out.println(Stream.of(srcGenFolder.members())
						.map(IResource::getName)
						.collect(Collectors.joining("\n")));
				var genfile = srcGenFolder.getFile("Example.java");
				return genfile.exists();
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
