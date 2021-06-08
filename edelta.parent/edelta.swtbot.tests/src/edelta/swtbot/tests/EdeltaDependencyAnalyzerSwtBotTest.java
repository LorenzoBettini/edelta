package edelta.swtbot.tests;

import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.waitForBuild;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import edelta.swtbot.tests.utils.ProjectImportUtil;

/**
 * @author Lorenzo Bettini
 *
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class EdeltaDependencyAnalyzerSwtBotTest extends EdeltaAbstractSwtbotTest {

	private static String TEST_PROJECT = "edelta.dependency.analyzer.swtbot.tests.project";

	@BeforeClass
	public static void importTestProject() throws Exception {
		ProjectImportUtil.importJavaProject(TEST_PROJECT);
		waitForBuild();
	}

	@Test
	public void canAnalyzeEcoreFiles() throws CoreException {
		assertErrorsInProject(0);
		getProjectTreeItem(TEST_PROJECT)
			.expand()
			.expandNode("model", "PersonsMM.ecore").select()
			.contextMenu("Edelta").menu("Analyze Ecore Files").click();
		bot.editorByTitle("PersonsMM.ecore.graphmm");
		getProjectTreeItem(TEST_PROJECT)
			.expand()
			.expandNode("analysis", "results", "model", "PersonsMM.ecore.graphmm");
	}

}
