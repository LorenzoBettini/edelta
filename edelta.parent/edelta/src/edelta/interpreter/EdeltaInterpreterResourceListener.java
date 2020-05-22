package edelta.interpreter;

import static org.eclipse.emf.ecore.EcorePackage.Literals.ENAMED_ELEMENT__NAME;

import java.util.List;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.xtext.linking.impl.XtextLinkingDiagnostic;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.validation.EObjectDiagnosticImpl;
import org.eclipse.xtext.xbase.XExpression;

import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.resource.derivedstate.EdeltaENamedElementXExpressionMap;

/**
 * Listens for changes on a {@link Resource} and performs tasks accordingly.
 * 
 * For example, it clears the {@link IResourceScopeCache} of the specified
 * {@link Resource} and removes issues added by the type system properly.
 * Scoping and type computation will be performed on the next access, taking now
 * into consideration the new information collected during interpretation.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaInterpreterResourceListener extends EContentAdapter {

	private IResourceScopeCache cache;

	private Resource resource;

	private EdeltaENamedElementXExpressionMap enamedElementXExpressionMap;

	private XExpression currentExpression;

	public EdeltaInterpreterResourceListener(IResourceScopeCache cache, Resource resource,
			EdeltaENamedElementXExpressionMap enamedElementXExpressionMap) {
		this.cache = cache;
		this.resource = resource;
		this.enamedElementXExpressionMap = enamedElementXExpressionMap;
	}

	@Override
	public void notifyChanged(Notification notification) {
		super.notifyChanged(notification);
		if (notification.getFeature() == ENAMED_ELEMENT__NAME) {
			enamedElementXExpressionMap.put(
				(ENamedElement) notification.getNotifier(),
				currentExpression);
		} else if (notification.getEventType() == Notification.ADD &&
				notification.getNewValue() instanceof ENamedElement) {
			enamedElementXExpressionMap.put(
				(ENamedElement) notification.getNewValue(),
				currentExpression);
		}
		cache.clear(resource);
		clearIssues(resource.getErrors());
		clearIssues(resource.getWarnings());
	}

	public void setCurrentExpression(XExpression currentExpression) {
		this.currentExpression = currentExpression;
	}

	private void clearIssues(final List<Diagnostic> issues) {
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
