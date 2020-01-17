package edelta.interpreter;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.xtext.linking.impl.XtextLinkingDiagnostic;
import org.eclipse.xtext.util.IResourceScopeCache;

/**
 * Listens for changes and clears the {@link IResourceScopeCache} of the
 * specified {@link Resource} and removes the {@link XtextLinkingDiagnostic}
 * added by the type system.
 * 
 * Scoping and type computation will be performed on the next access, taking now
 * into consideration the new information collected during interpretation.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaInterpreterCleaner extends EContentAdapter {

	private IResourceScopeCache cache;

	private Resource resource;

	public EdeltaInterpreterCleaner(IResourceScopeCache cache, Resource resource) {
		this.cache = cache;
		this.resource = resource;
	}

	@Override
	public void notifyChanged(Notification notification) {
		super.notifyChanged(notification);
		cache.clear(resource);
		clearXtextLinkingIssues(resource.getErrors());
		clearXtextLinkingIssues(resource.getWarnings());
	}

	private void clearXtextLinkingIssues(final EList<Diagnostic> issues) {
		issues.removeIf(d -> d instanceof XtextLinkingDiagnostic);
	}
}
