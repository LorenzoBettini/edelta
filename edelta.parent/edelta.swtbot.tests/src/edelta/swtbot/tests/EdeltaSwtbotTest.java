package edelta.swtbot.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;
import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.root;
import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.waitForBuild;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.epsilon.picto.PictoView;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import edelta.ui.testutils.EdeltaUiTestUtils;

/**
 * Better to have all SWTBot tests in a single file, since if executed in
 * separate files in some order, some tests fail to find File menu, Show View,
 * etc.
 * 
 * @author Lorenzo Bettini
 *
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class EdeltaSwtbotTest {

	private static final String MY_TEST_PROJECT = "MyTestProject";
	private static final String PROJECT_EXPLORER = "Project Explorer";
	private static final String CATEGORY_NAME = "Edelta";
	private static SWTWorkbenchBot bot;

	private static final List<IStatus> pictoErrors = new CopyOnWriteArrayList<>();
	private static final ILogListener pictoLogListener = (status, plugin) -> {
		if (plugin.startsWith("org.eclipse.epsilon") && status.matches(IStatus.ERROR)) {
			pictoErrors.add(status);
		}
	};

	private static final String PERSONS_MM_ECORE = "PersonsMM.ecore";
	private static final String PERSONS_MM_ECORE_GRAPHMM = "PersonsMM.ecore.graphmm";
	private static final String PERSONS_MM_TEMPLATE_EDELTA = "PersonsMMTemplate.edelta";

	@BeforeClass
	public static void beforeClass() {
		bot = new SWTWorkbenchBot();
		pictoErrors.clear();
		Platform.addLogListener(pictoLogListener);

		EdeltaUiTestUtils.closeWelcomePage();
		EdeltaUiTestUtils.openProjectExplorer();
	}

	@AfterClass
	public static void afterClass() {
		try {
			bot.resetWorkbench();
		} finally {
			Platform.removeLogListener(pictoLogListener);
		}
	}

	@After
	public void runAfterEveryTest() throws InvocationTargetException, InterruptedException {
		// Picto's asynchronous jobs retain the editor and its workspace file.
		// Finish rendering and close the view before deleting those resources.
		waitForPictoJobs();
		for (var view : bot.views()) {
			if (PictoView.ID.equals(view.getViewReference().getId())) {
				view.close();
			}
		}
		bot.closeAllEditors();
		EdeltaUiTestUtils.cleanWorkspace();
		waitForBuild();
		waitForPictoJobs();
		assertThat(pictoErrors).as("Asynchronous Picto/Epsilon errors").isEmpty();
	}

	/** Wait without blocking the UI thread, which Picto rendering jobs also need. */
	private void waitForPictoJobs() {
		bot.waitUntil(new DefaultCondition() {
			@Override
			public boolean test() {
				return Stream.of(Job.getJobManager().find(null))
					.noneMatch(job -> job.getClass().getName()
						.startsWith(PictoView.class.getName() + "$"));
			}

			@Override
			public String getFailureMessage() {
				return "Picto rendering jobs did not finish";
			}
		});
	}

	protected void disableBuildAutomatically() {
		clickOnBuildAutomatically(false);
	}

	protected void enableBuildAutomatically() {
		clickOnBuildAutomatically(true);
	}

	private void clickOnBuildAutomatically(boolean shouldBeEnabled) {
		if (buildAutomaticallyMenu().isChecked() == shouldBeEnabled)
			return;
		// see http://www.eclipse.org/forums/index.php/mv/msg/165852/#msg_525521
		// for the reason why we need to specify 1
		buildAutomaticallyMenu().click();
		assertEquals(shouldBeEnabled, buildAutomaticallyMenu().isChecked());
	}

	private SWTBotMenu buildAutomaticallyMenu() {
		return bot.menu("Project", 1).menu("Build Automatically");
	}

	protected boolean isProjectCreated(String name) {
		try {
			getProjectTreeItem(name);
			return true;
		} catch (WidgetNotFoundException e) {
			return false;
		}
	}

	protected boolean isFileCreated(String project, String... filePath) {
		try {
			getProjectTreeItem(project).expand().expandNode(filePath);
			return true;
		} catch (WidgetNotFoundException e) {
			return false;
		}
	}

	protected static SWTBotTree getProjectTree() {
		SWTBotView packageExplorer = getProjectExplorer();
		return packageExplorer.bot().tree();
	}

	protected static SWTBotView getProjectExplorer() {
		return bot.viewByTitle(PROJECT_EXPLORER);
	}

	protected SWTBotTreeItem getProjectTreeItem(String myTestProject) {
		return getProjectTree().getTreeItem(myTestProject);
	}

	protected void assertErrorsInProject(int numOfErrors) throws CoreException {
		IMarker[] markers = root().findMarkers(IMarker.PROBLEM, true,
				IResource.DEPTH_INFINITE);
		List<IMarker> errorMarkers = new LinkedList<IMarker>();
		for (int i = 0; i < markers.length; i++) {
			IMarker iMarker = markers[i];
			if (iMarker.getAttribute(IMarker.SEVERITY).toString()
					.equals("" + IMarker.SEVERITY_ERROR)) {
				errorMarkers.add(iMarker);
			}
		}
		assertEquals(
				"error markers: " + printMarkers(errorMarkers), numOfErrors,
				errorMarkers.size());
	}

	private String printMarkers(List<IMarker> errorMarkers) {
		StringBuffer buffer = new StringBuffer();
		for (IMarker iMarker : errorMarkers) {
			try {
				buffer.append(iMarker.getAttribute(IMarker.MESSAGE) + "\n");
				buffer.append(iMarker.getAttribute(IMarker.SEVERITY) + "\n");
			} catch (CoreException e) {
				// nothing to do
			}
		}
		return buffer.toString();
	}

	protected void edeltaContextMenu(String project, String ecoreFile, String menu) {
		bot.waitUntil(new ICondition() {
			@Override
			public boolean test() throws Exception {
				System.out.println("### menu for "
						+ project + " " + ecoreFile + " " + menu);
				getProjectTreeItem(project)
					.expand()
					.expandNode("model", ecoreFile).select()
					.contextMenu("Edelta").menu(menu).click();
				return true;
			}

			@Override
			public void init(SWTBot bot) {
				// nothing to do
			}

			@Override
			public String getFailureMessage() {
				return "Cannot find menu for "
					+ project + " " + ecoreFile + " " + menu;
			}
		});
	}

	private void waitingForBuild() {
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
				// nothing to do
			}

			@Override
			public String getFailureMessage() {
				return "build failed";
			}
		});
	}

	private void waitingForPluginModel() {
		bot.waitUntil(new ICondition() {
			@Override
			public boolean test() throws Exception {
				System.out.println("Waiting for the plugin model...");
				return PDECore.getDefault().getModelManager().isInitialized();
			}

			@Override
			public void init(SWTBot bot) {
				// nothing to do
			}

			@Override
			public String getFailureMessage() {
				return "Failed waiting for inizialize of plugin models";
			}
		});
	}

	private void fileNewProject() {
		bot.waitUntil(new ICondition() {
			@Override
			public boolean test() throws Exception {
				System.out.println("### File -> New -> Project...");
				bot.menu("File").menu("New").menu("Project...").click();
				return true;
			}

			@Override
			public void init(SWTBot bot) {
				// nothing to do
			}

			@Override
			public String getFailureMessage() {
				return "Failed File -> New -> Project...";
			}
		});
	}

	@Test
	public void canCreateANewExampleProject() throws OperationCanceledException {
		fileNewProject();

		SWTBotShell shell = bot.shell("New Project");
		shell.activate();
		SWTBotTreeItem categoryNode = bot.tree().expandNode(CATEGORY_NAME);
		categoryNode.select("Edelta Project");

		bot.button("Next >").click();

		bot.textWithLabel("Project name:").setText(MY_TEST_PROJECT);

		bot.button("Next >").click();

		bot.table().select("Edelta Example Project");

		bot.button("Finish").click();

		// creation of a project might require some time
		bot.waitUntil(shellCloses(shell), SWTBotPreferences.TIMEOUT);
		assertTrue("Project doesn't exist: " + MY_TEST_PROJECT, isProjectCreated(MY_TEST_PROJECT));

		waitingForPluginModel();

		waitingForBuild();

		bot.waitUntil(new ICondition() {
			@Override
			public boolean test() throws Exception {
				System.out.println("*** expanding " + MY_TEST_PROJECT);
				getProjectTreeItem(MY_TEST_PROJECT)
					.expand()
					.expandNode("edelta-gen", "com.example", "Example.java");
				return true;
			}

			@Override
			public void init(SWTBot bot) {
				// nothing to do
			}

			@Override
			public String getFailureMessage() {
				return "Example.java does not exist";
			}
		});
	}

	@Test
	public void canCreateANewEmptyProject() throws OperationCanceledException {
		fileNewProject();

		SWTBotShell shell = bot.shell("New Project");
		shell.activate();
		SWTBotTreeItem categoryNode = bot.tree().expandNode(CATEGORY_NAME);
		categoryNode.select("Edelta Project");

		bot.button("Next >").click();

		bot.textWithLabel("Project name:").setText(MY_TEST_PROJECT);

		bot.button("Next >").click();

		bot.table().select("Edelta Empty Project");

		bot.button("Finish").click();

		// creation of a project might require some time
		bot.waitUntil(shellCloses(shell), SWTBotPreferences.TIMEOUT);
		assertTrue("Project doesn't exist: " + MY_TEST_PROJECT, isProjectCreated(MY_TEST_PROJECT));

		waitingForPluginModel();

		waitingForBuild();
	}

	@Test
	public void canRunAnEdeltaFileAsJavaApplication() throws CoreException, OperationCanceledException, InterruptedException, InvocationTargetException {
		final String TEST_PROJECT = "edelta.testprojects.first";
		EdeltaUiTestUtils.importJavaProject("../" + TEST_PROJECT);
		waitingForPluginModel();
		waitingForBuild();
		SWTBotTreeItem tree = getProjectTreeItem(TEST_PROJECT)
				.expand()
				.expandNode("src")
				.expandNode("com.example1")
				.getNode("ExampleRunnable.edelta");
		checkLaunchContextMenu(tree.contextMenu("Run As"));
		checkLaunchContextMenu(tree.contextMenu("Debug As"));
	}

	private void checkLaunchContextMenu(SWTBotMenu contextMenu) {
		try {
			contextMenu.menu("1 Edelta Application");
		} catch (WidgetNotFoundException e) {
			System.out.println("MENUS: " + contextMenu.menuItems());
			throw e;
		}
	}

	@Test
	public void canAnalyzeEcoreFiles() throws Exception {
		assertPictoRendersDependencyAnalysis(openDependencyAnalysis());
	}

	@Test
	public void canReopenDependencyAnalysisAfterClosingEditor() throws Exception {
		var view = openDependencyAnalysis();
		assertPictoRendersDependencyAnalysis(view);
		var picto = bot.getDisplay().syncCall(() ->
			(PictoView) view.getViewReference().getView(false));
		bot.editorByTitle(PERSONS_MM_ECORE_GRAPHMM).close();
		waitForPictoJobs();
		assertThat(bot.getDisplay().syncCall(picto::getEditor)).isNull();

		edeltaContextMenu("edelta.dependency.analyzer.swtbot.tests.project",
			PERSONS_MM_ECORE, "Analyze Ecore Files");
		assertPictoRendersDependencyAnalysis(view);
	}

	/** Open the generated model and its Picto view through the real context-menu handler. */
	private SWTBotView openDependencyAnalysis() throws Exception {
		final String TEST_PROJECT = "edelta.dependency.analyzer.swtbot.tests.project";
		EdeltaUiTestUtils.importJavaProject("../" + TEST_PROJECT);
		waitForBuild();
		assertErrorsInProject(0);
		edeltaContextMenu(TEST_PROJECT, PERSONS_MM_ECORE, "Analyze Ecore Files");
		bot.editorByTitle(PERSONS_MM_ECORE_GRAPHMM);
		getProjectTreeItem(TEST_PROJECT)
			.expand()
			.expandNode("analysis", "results", "model", PERSONS_MM_ECORE_GRAPHMM);
		return bot.viewByPartName("Picto");
	}

	/** Verify the selected Picto view and its generated HTML after rendering has finished. */
	private void assertPictoRendersDependencyAnalysis(final SWTBotView view) {
		var picto = bot.getDisplay().syncCall(() ->
			(PictoView) view.getViewReference().getView(false));
		waitForPictoJobs();
		// A cancelled initial rendering can activate Epsilon's Console view.
		// Select the desired view explicitly, as a user would after opening Picto.
		view.setFocus();
		// Resolve and select against the same tree input on the UI thread: Picto
		// can replace its tree items when an editor reload triggers rendering.
		bot.getDisplay().syncExec(() -> picto.selectViewTree(
			List.of(picto.getViewTree().getName(), "Repository")));
		bot.waitUntil(new DefaultCondition() {
			@Override
			public boolean test() {
				return bot.getDisplay().syncCall(() -> picto.getEditor() != null
					&& picto.getActiveView() != null
					&& "Repository".equals(picto.getActiveView().getName()));
			}

			@Override
			public String getFailureMessage() {
				return "Picto did not select the Repository visualization";
			}
		});
		waitForPictoJobs();
		assertThat(bot.getDisplay().syncCall(() ->
			picto.getActiveView().getContent().getText()))
			.contains("\"Persons\"", "\"Subscription\"", "\"WebApp\"");
		assertThat(view.isActive()).as("Picto stays active after rendering").isTrue();
		assertThat(pictoErrors).as("Picto rendering errors").isEmpty();
	}

	@Test
	public void canGenerateTemplateFiles() throws Exception {
		final String TEST_PROJECT = "edelta.dependency.analyzer.swtbot.tests.project";
		EdeltaUiTestUtils.importJavaProject("../" + TEST_PROJECT);
		waitForBuild();
		assertErrorsInProject(0);
		edeltaContextMenu(TEST_PROJECT, PERSONS_MM_ECORE, "Generate Edelta Template File");
		waitForBuild();
		assertErrorsInProject(0);
		bot.editorByTitle(PERSONS_MM_TEMPLATE_EDELTA);
	}
}
