package edelta.dependency.analyzer.ui.handlers;

import java.io.InputStream;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.epsilon.picto.PictoView;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.xtext.util.StringInputStream;

import GraphMM.Repository;
import edelta.dependency.analyzer.EdeltaDependencyAnalizer;
import edelta.dependency.analyzer.ui.internal.EdeltaDependencyAnalyzerUiUtils;

/**
 * Runs the dependency analysis on the selected ecore file and the other ecore
 * files in the same directory, creates the {@link Repository} model, saves it,
 * generates the corresponding picto file and opens the EMF editor and Picto
 * view.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaRunDependencyAnalysisHandler extends AbstractHandler {

	private static final int MONITOR_CHILD_WORK = 20;
	private static final int MONITOR_WORK = 100;

	private static final String PICTO_FILE_CONTENTS = """
		<?nsuri picto?>
		<picto
		  transformation="platform:/plugin/edelta.dependency.analyzer.picto/picto/ecosystem2graphd3.egx">
		</picto>
		""";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		EdeltaDependencyAnalyzerUiUtils.executeOnIFileSelection(event, (workspaceFile, monitor) -> {
			var subMonitor = SubMonitor.convert(monitor, "Dependency Analysis", MONITOR_WORK);
			subMonitor.beginTask("Analyze Ecores", MONITOR_CHILD_WORK);
			var fileSystemFile = workspaceFile.getLocation().toFile();
			var path = fileSystemFile.getAbsolutePath();
			var edeltaDependencyAnalizer = new EdeltaDependencyAnalizer();
			var repository = edeltaDependencyAnalizer.analyzeEPackage(path);
			subMonitor.worked(MONITOR_CHILD_WORK);

			var project = workspaceFile.getProject();
			var projectPath = project.getLocation().toFile().getAbsolutePath();
			var parentDirectoryName = fileSystemFile.getParentFile().getName();
			var outputPath = "/analysis/results/" + parentDirectoryName;
			var generatedModelName = workspaceFile.getName() + ".graphmm";
			edeltaDependencyAnalizer.saveRepository(repository, projectPath + outputPath, generatedModelName);

			project.refreshLocal(IResource.DEPTH_INFINITE, subMonitor.newChild(MONITOR_CHILD_WORK));

			var generatedFile = project.getFile(outputPath + "/" + generatedModelName);
			var generatedPictoFile = project.getFile(outputPath + "/" + generatedModelName + ".picto");
			generatedPictoFile.delete(true, subMonitor.newChild(MONITOR_CHILD_WORK));
			try (InputStream stream = new StringInputStream(PICTO_FILE_CONTENTS, generatedPictoFile.getCharset(true))) {
				generatedPictoFile.create(stream, true, subMonitor.newChild(MONITOR_CHILD_WORK));
			}

			var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			page.openEditor(new FileEditorInput(generatedFile),
					"org.eclipse.emf.ecore.presentation.ReflectiveEditorID");

			page.showView(PictoView.ID);
			subMonitor.done();
		});
		return null;
	}
}
