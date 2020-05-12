package edelta.interpreter;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.xtext.linking.impl.XtextLinkingDiagnostic;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.validation.EObjectDiagnosticImpl;

import edelta.edelta.EdeltaEcoreReferenceExpression;

/**
 * Listens for changes and clears the {@link IResourceScopeCache} of the
 * specified {@link Resource} and removes issues added by the type system
 * properly.
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
		clearIssues(resource.getErrors());
		clearIssues(resource.getWarnings());
	}

	private void clearIssues(final EList<Diagnostic> issues) {
		issues.removeIf(this::isToBeRemoved);
	}

	private boolean isToBeRemoved(Diagnostic d) {
		if (d instanceof EdeltaInterpreterDiagnostic)
			return false;
		if (d instanceof XtextLinkingDiagnostic)
			return true;
		if (d instanceof EObjectDiagnosticImpl) {
			// during typing if the EdeltaEcoreReferenceExpression.EdeltaEcoreReference
			// is not resolved, RootResolvedTypes will put an error of type mismatch
			// the interpreter might change the metamodel and later the ecoreref
			// might become valid, so we must remove previous errors when the metamodel
			// changes
			return ((EObjectDiagnosticImpl) d).getProblematicObject() instanceof EdeltaEcoreReferenceExpression;
		}
		return false;
	}
}
