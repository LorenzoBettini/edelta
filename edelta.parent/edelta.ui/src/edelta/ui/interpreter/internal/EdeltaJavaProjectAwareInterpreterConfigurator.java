/**
 * 
 */
package edelta.ui.interpreter.internal;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.xbase.lib.Exceptions;

import com.google.inject.Inject;

import edelta.interpreter.EdeltaInterpreter;

/**
 * Sets the classloader of the interpreter so that if finds classes in a Java
 * project, taking dependencies (runtime classpath) into consideration.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaJavaProjectAwareInterpreterConfigurator {

	@Inject
	private ClassLoader parentClassLoader;

	public void configureInterpreter(EdeltaInterpreter interpreter, Resource resource) {
		var set = resource.getResourceSet();
		if (set instanceof XtextResourceSet xtextResourceSet) {
			Object context = xtextResourceSet.getClasspathURIContext();
			if (context instanceof IJavaProject javaProject) {
				try {
					var classPathEntries = JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
					var urlList = new ArrayList<>();
					for (int i = 0; i < classPathEntries.length; i++) {
						String entry = classPathEntries[i];
						IPath path = new Path(entry);
						urlList.add(path.toFile().toURI().toURL());
					}
					URLClassLoader cl = new URLClassLoader(
							urlList.toArray(new URL[urlList.size()]),
							parentClassLoader);
					interpreter.setClassLoader(cl);
				} catch (Exception e) {
					throw Exceptions.sneakyThrow(e);
				}
			}
		}
	}
}
