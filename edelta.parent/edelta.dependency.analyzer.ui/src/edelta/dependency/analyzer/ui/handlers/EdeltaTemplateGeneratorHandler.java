package edelta.dependency.analyzer.ui.handlers;

import java.io.InputStream;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.xtext.util.StringInputStream;

import edelta.dependency.analyzer.EdeltaDependencyAnalizer;
import edelta.dependency.analyzer.EdeltaDependencyAnalyzerUtils;
import edelta.dependency.analyzer.EdeltaMetamodelDependencies;
import edelta.dependency.analyzer.ui.internal.EdeltaDependencyAnalyzerUiUtils;
import edelta.ui.internal.EdeltaActivator;

/**
 * Runs the dependency analysis on the selected ecore file and the other ecore
 * files in the same directory, creates a template edelta file.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaTemplateGeneratorHandler extends AbstractHandler {

	private static final int MONITOR_CHILD_WORK = 20;
	private static final int MONITOR_WORK = 100;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		EdeltaDependencyAnalyzerUiUtils.executeOnIFileSelection(event, (workspaceFile, monitor) -> {
			var subMonitor = SubMonitor.convert(monitor, "Generate Template File", MONITOR_WORK);
			subMonitor.beginTask("Analyze Ecores", MONITOR_CHILD_WORK);
			var fileSystemFile = workspaceFile.getLocation().toFile();
			var path = fileSystemFile.getAbsolutePath();
			var edeltaDependencyAnalizer = new EdeltaDependencyAnalizer();
			var repository = edeltaDependencyAnalizer.analyzeEPackage(path);
			var dependencies = EdeltaDependencyAnalyzerUtils.computeMetamodelDependencies(repository);
			subMonitor.worked(MONITOR_CHILD_WORK);

			var project = workspaceFile.getProject();
			var outputPath = "src";
			var workspaceFileName = workspaceFile.getName();
			var generatedEdeltaFileName =
				workspaceFileName.substring(0, workspaceFileName.lastIndexOf('.'))
				+ "Template.edelta";

			var generatedFile = project.getFile(outputPath + "/" + generatedEdeltaFileName);
			generatedFile.delete(true, subMonitor.newChild(MONITOR_CHILD_WORK));
			var contents = edeltaFileContents(dependencies);
			try (InputStream stream = new StringInputStream(contents, generatedFile.getCharset(true))) {
				generatedFile.create(stream, true, subMonitor.newChild(MONITOR_CHILD_WORK));
			}

			project.refreshLocal(IResource.DEPTH_INFINITE, subMonitor.newChild(MONITOR_CHILD_WORK));

			var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			page.openEditor(new FileEditorInput(generatedFile),
					EdeltaActivator.EDELTA_EDELTA);
			subMonitor.done();
		});
		return null;
	}

	private String edeltaFileContents(EdeltaMetamodelDependencies dependencies) {
		var stringBuilder = new StringBuilder();
		stringBuilder.append("// This is just an initial edelta file:\n");
		stringBuilder.append("// make sure you rename it before editing it\n");
		stringBuilder.append("// since it will be removed on the next run.\n\n");
		stringBuilder.append("metamodel \"" + dependencies.getHighlighted().getName() + "\"\n");
		stringBuilder.append(dependencies.getDependencies().stream()
				.map(d -> "metamodel \"" + d.getName() + "\"")
				.collect(Collectors.joining("\n")));
		return stringBuilder.toString();
	}
}
