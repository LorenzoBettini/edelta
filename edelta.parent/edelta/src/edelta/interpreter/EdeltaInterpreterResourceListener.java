package edelta.interpreter;

import static org.eclipse.emf.ecore.EcorePackage.Literals.*;

import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.xtext.linking.impl.XtextLinkingDiagnostic;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.validation.EObjectDiagnosticImpl;
import org.eclipse.xtext.xbase.XExpression;

import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.lib.EdeltaLibrary;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.resource.derivedstate.EdeltaENamedElementXExpressionMap;
import edelta.resource.derivedstate.EdeltaModifiedElements;
import edelta.util.EdeltaModelUtil;
import edelta.validation.EdeltaValidator;

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

	private EdeltaInterpreterDiagnosticHelper diagnosticHelper;

	private EdeltaModifiedElements modifiedElements;

	public EdeltaInterpreterResourceListener(IResourceScopeCache cache, Resource resource,
			EdeltaDerivedStateHelper derivedStateHelper,
			EdeltaInterpreterDiagnosticHelper diagnosticHelper) {
		this.cache = cache;
		this.resource = resource;
		enamedElementXExpressionMap = derivedStateHelper.getEnamedElementXExpressionMap(resource);
		modifiedElements = derivedStateHelper.getModifiedElements(resource);
		this.diagnosticHelper = diagnosticHelper;
	}

	@Override
	public void notifyChanged(Notification notification) {
		super.notifyChanged(notification);
		final var feature = notification.getFeature();
		final var notifier = notification.getNotifier();
		final var newValue = notification.getNewValue();
		if (feature == ENAMED_ELEMENT__NAME) {
			enamedElementXExpressionMap.put(
				(ENamedElement) notifier,
				currentExpression);
		} else {
			if (notification.getEventType() == Notification.ADD &&
					newValue instanceof ENamedElement) {
				enamedElementXExpressionMap.put(
					(ENamedElement) newValue,
					currentExpression);
				// cycles must be detected after performing the
				// association to the current expression, otherwise
				// the error won't be placed on the correct expression
				checkCycles(feature, newValue);
			}
		}
		if (!notification.isTouch()) {
			updateModifiedElements(notifier);
			updateModifiedElements(newValue);
		}
		cache.clear(resource);
		clearIssues(resource.getErrors());
		clearIssues(resource.getWarnings());
	}

	private void updateModifiedElements(Object element) {
		/*
		 * we have to avoid stack overflows, by checking the result of add in case of
		 * cycles; we break the cycle for supertype hierarchy, but when doing so we get
		 * another notification and in that case the cycle it's still there
		 */
		if (element instanceof ENamedElement) {
			final var enamedElement = (ENamedElement) element;
			if (modifiedElements.add(enamedElement))
				updateModifiedElements(enamedElement.eContainer());
		} else if (element instanceof Collection<?>) {
			((Collection<?>)element).stream().forEach(this::updateModifiedElements);
		}
	}

	private void checkCycles(final Object feature, final Object newValue) {
		if (feature == EPACKAGE__ESUBPACKAGES) {
			var subPackage = (EPackage) newValue;
			if (EdeltaModelUtil.hasCycleInSuperPackage(subPackage)) {
				diagnosticHelper.addError(subPackage, EdeltaValidator.EPACKAGE_CYCLE,
					"Cycle in superpackage/subpackage: " +
						EdeltaLibrary.getEObjectRepr(subPackage));
				// break the cycle to avoid problems due to
				// loop in containment (in other Xtext components, e.g., scoping)
				subPackage.getESuperPackage()
					.getESubpackages()
					.remove(subPackage);
			}
		} else if (feature == ECLASS__ESUPER_TYPES) {
			var eClass = (EClass) newValue;
			if (EdeltaModelUtil.hasCycleInHierarchy(eClass)) {
				diagnosticHelper.addError(eClass, EdeltaValidator.ECLASS_CYCLE,
					"Cycle in inheritance hierarchy: " +
						EdeltaLibrary.getEObjectRepr(eClass));
			}
		}
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
