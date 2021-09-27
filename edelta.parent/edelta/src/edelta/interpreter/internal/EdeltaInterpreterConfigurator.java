/**
 * 
 */
package edelta.interpreter.internal;

import java.net.URL;
import java.net.URLClassLoader;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.xtext.resource.XtextResourceSet;

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
					final IJavaProject jp = (IJavaProject) context;
					String[] runtimeClassPath = JavaRuntime.computeDefaultRuntimeClassPath(jp);
					URL[] urls = new URL[runtimeClassPath.length];
					for (int i = 0; i < urls.length; i++) {
						urls[i] = new URL(runtimeClassPath[i]);
					}
					URLClassLoader cl = new URLClassLoader(
							urls,
							parentClassLoader);
					interpreter.setClassLoader(cl);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
