/**
 * 
 */
package edelta.interpreter.internal;

import static com.google.common.collect.Sets.newHashSet;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.xtext.resource.XtextResourceSet;

import com.google.inject.Inject;

import edelta.interpreter.IEdeltaInterpreter;

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

	public void configureInterpreter(IEdeltaInterpreter interpreter, Resource resource) {
		ResourceSet set = resource.getResourceSet();
		if (set instanceof XtextResourceSet) {
			Object context = ((XtextResourceSet) set).getClasspathURIContext();
			if (context instanceof IJavaProject) {
				try {
					final IJavaProject jp = (IJavaProject) context;
					// String[] runtimeClassPath =
					// JavaRuntime.computeDefaultRuntimeClassPath(jp);
					// URL[] urls = new URL[runtimeClassPath.length];
					// for (int i = 0; i < urls.length; i++) {
					// urls[i] = new URL(runtimeClassPath[i]);
					// }
					// cl = new URLClassLoader(urls);
					IClasspathEntry[] classpath = jp.getResolvedClasspath(true);
					final IWorkspaceRoot root = jp.getProject().getWorkspace().getRoot();
					Set<URL> urls = newHashSet();
					for (int i = 0; i < classpath.length; i++) {
						final IClasspathEntry entry = classpath[i];
						if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
							IPath outputLocation = entry.getOutputLocation();
							if (outputLocation == null) {
								outputLocation = jp.getOutputLocation();
							}
							IFolder folder = root.getFolder(outputLocation);
							if (folder.exists()) {
								urls.add(new URL(folder.getRawLocationURI().toASCIIString() + "/"));
							}
						} else if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
							IPath outputLocation = entry.getOutputLocation();
							if (outputLocation == null) {
								IProject project = (IProject) root.getProject(entry.getPath().toString());
								IJavaProject javaProject = JavaCore.create(project);
								if (javaProject != null)
									outputLocation = javaProject.getOutputLocation();
							}
							if (outputLocation != null) {
								IFolder folder = root.getFolder(outputLocation);
								if (folder.exists()) {
									urls.add(new URL(folder.getRawLocationURI().toASCIIString() + "/"));
								}
							} else {
								urls.add(entry.getPath().toFile().toURI().toURL());
							}
						} else {
							urls.add(entry.getPath().toFile().toURI().toURL());
						}
					}
					URLClassLoader cl = new URLClassLoader(
							urls.toArray(new URL[urls.size()]),
							parentClassLoader);
					interpreter.setClassLoader(cl);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
