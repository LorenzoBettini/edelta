package edelta.dependency.analyzer.ui.handlers;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.epsilon.picto.PictoView;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.xtext.util.StringInputStream;

import edelta.dependency.analyzer.EdeltaDependencyAnalizer;

public class EdeltaRunDependencyAnalysisHandler extends AbstractHandler {

	private static final String PICTO_FILE_CONTENTS = "<?nsuri picto?>\n"
			+ "<picto\n"
			+ "  transformation=\"platform:/plugin/edelta.dependency.analyzer.picto/picto/ecosystem2graphd3.egx\">\n"
			+ "</picto>\n";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		var selection = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().getSelection();
		if (selection instanceof IStructuredSelection) {
			var workspaceFile = (IFile) ((IStructuredSelection) selection).getFirstElement();
			var fileSystemFile = workspaceFile.getLocation().toFile();
			var path = fileSystemFile.getAbsolutePath();
			var edeltaDependencyAnalizer = new EdeltaDependencyAnalizer();
			var project = workspaceFile.getProject();
			var projectPath = project.getLocation().toFile().getAbsolutePath();
			try {
				var repository = edeltaDependencyAnalizer.analyzeEPackage(path);
				var parentDirectoryName = fileSystemFile.getParentFile().getName();
				var outputPath = "/analysis/results/" + parentDirectoryName;
				var generatedModelName = workspaceFile.getName() + ".graphmm";
				edeltaDependencyAnalizer.saveRepository(repository,
					projectPath + outputPath, generatedModelName);
				project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
				var generatedFile = project.getFile(outputPath + "/" + generatedModelName);
				var generatedPictoFile = project.getFile(outputPath + "/" + generatedModelName + ".picto");
				generatedPictoFile.delete(true, new NullProgressMonitor());
				try (InputStream stream = new StringInputStream(PICTO_FILE_CONTENTS, generatedPictoFile.getCharset(true))) {
					generatedPictoFile.create(stream, true, new NullProgressMonitor());
				}
				var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				page.openEditor(new FileEditorInput(generatedFile), "org.eclipse.emf.ecore.presentation.ReflectiveEditorID");
				page.showView(PictoView.ID);
			} catch (IOException | CoreException e) {
				throw new ExecutionException(e.getMessage(), e);
			}
			
		}
		return null;
	}
}
