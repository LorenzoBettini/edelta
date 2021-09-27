/**
 * 
 */
package edelta.interpreter.internal;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.xbase.lib.Exceptions;

import com.google.inject.Inject;

import edelta.interpreter.EdeltaInterpreter;

/**
 * Sets the classloader of the interpreter so that if finds classes in
 * a Java project.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaInterpreterConfigurator {

	@Inject
	private ClassLoader parentClassLoader;

	public void configureInterpreter(EdeltaInterpreter interpreter, Resource resource) {
		ResourceSet set = resource.getResourceSet();
		if (set instanceof XtextResourceSet) {
			Object context = ((XtextResourceSet) set).getClasspathURIContext();
			if (context instanceof IJavaProject) {
				try {
					final IJavaProject javaProject = (IJavaProject) context;
					String[] classPathEntries = JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
					List<URL> urlList = new ArrayList<>();
					for (int i = 0; i < classPathEntries.length; i++) {
						String entry = classPathEntries[i];
						IPath path = new Path(entry);
						URL url;
						url = path.toFile().toURI().toURL();
						urlList.add(url);
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
