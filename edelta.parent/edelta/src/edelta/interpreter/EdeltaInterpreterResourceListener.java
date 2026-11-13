package edelta.interpreter;

import static org.eclipse.emf.ecore.EcorePackage.Literals.ECLASS__ESUPER_TYPES;
import static org.eclipse.emf.ecore.EcorePackage.Literals.ENAMED_ELEMENT__NAME;
import static org.eclipse.emf.ecore.EcorePackage.Literals.EPACKAGE__ESUBPACKAGES;

import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.xtext.linking.impl.XtextLinkingDiagnostic;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.validation.EObjectDiagnosticImpl;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.validation.IssueCodes;

import edelta.lib.EdeltaUtils;
import edelta.resource.derivedstate.EdeltaCopiedEPackagesMap;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.resource.derivedstate.EdeltaENamedElementXExpressionMap;
import edelta.resource.derivedstate.EdeltaModifiedElements;
import edelta.resource.derivedstate.EdeltaModifiedFeatures;
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

	private final IResourceScopeCache cache;

	private final Resource resource;

	private final EdeltaENamedElementXExpressionMap enamedElementXExpressionMap;

	private XExpression currentExpression;

	private final EdeltaInterpreterDiagnosticHelper diagnosticHelper;

	private final EdeltaModifiedElements modifiedElements;
	private final EdeltaModifiedFeatures modifiedFeatures;
	private final EdeltaCopiedEPackagesMap copiedEPackagesMap;

	public EdeltaInterpreterResourceListener(IResourceScopeCache cache, Resource resource,
			EdeltaDerivedStateHelper derivedStateHelper,
			EdeltaInterpreterDiagnosticHelper diagnosticHelper) {
		this.cache = cache;
		this.resource = resource;
		enamedElementXExpressionMap = derivedStateHelper.getEnamedElementXExpressionMap(resource);
		modifiedElements = derivedStateHelper.getModifiedElements(resource);
		modifiedFeatures = derivedStateHelper.getModifiedFeatures(resource);
		copiedEPackagesMap = derivedStateHelper.getCopiedEPackagesMap(resource);
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
					newValue instanceof ENamedElement enamedElement) {
				enamedElementXExpressionMap.put(
						enamedElement, currentExpression);
				// cycles must be detected after performing the
				// association to the current expression, otherwise
				// the error won't be placed on the correct expression
				checkCycles(feature, newValue);
			}
		}
		if (!notification.isTouch()) {
			if (notifier instanceof EObject object && feature instanceof EStructuralFeature structuralFeature) {
				modifiedFeatures.record(object, structuralFeature, currentExpression);
				if (structuralFeature instanceof EReference reference && reference.isContainment()) {
					recordInitialValues(newValue);
				}
			}
			updateModifiedElements(notifier);
			updateModifiedElements(newValue);
		}
		cache.clear(resource);
		clearIssues(resource.getErrors());
		clearIssues(resource.getWarnings());
	}

	private void recordInitialValues(Object value) {
		if (value instanceof EObject object && copiedEPackagesMap.getOriginal(object) == null) {
			modifiedFeatures.recordInitial(object, currentExpression);
		} else if (value instanceof Collection<?> values) {
			values.forEach(this::recordInitialValues);
		}
	}

	private void updateModifiedElements(Object element) {
		/*
		 * we have to avoid stack overflows, by checking the result of add in case of
		 * cycles; we break the cycle for supertype hierarchy, but when doing so we get
		 * another notification and in that case the cycle it's still there
		 */
		if (element instanceof final ENamedElement enamedElement) {
			if (modifiedElements.add(enamedElement))
				updateModifiedElements(enamedElement.eContainer());
		} else if (element instanceof EObject object) {
			updateModifiedElements(object.eContainer());
		} else if (element instanceof Collection<?> elements) {
			elements.forEach(this::updateModifiedElements);
		}
	}

	private void checkCycles(final Object feature, final Object newValue) {
		if (feature == EPACKAGE__ESUBPACKAGES) {
			var subPackage = (EPackage) newValue;
			if (EdeltaModelUtil.hasCycleInSuperPackage(subPackage)) {
				diagnosticHelper.addError(subPackage, EdeltaValidator.EPACKAGE_CYCLE,
					"Cycle in superpackage/subpackage: " +
						EdeltaUtils.getEObjectRepr(subPackage));
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
						EdeltaUtils.getEObjectRepr(eClass));
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
		if (d instanceof EObjectDiagnosticImpl diagnosticImpl) {
			// during typing if the EdeltaEcoreReferenceExpression.EdeltaEcoreReference
			// is not resolved, RootResolvedTypes will put an error of type mismatch
			// the interpreter might change the metamodel and later the ecoreref
			// might become valid, so we must remove previous errors when the metamodel
			// changes.
			// Note that this is not only the case for a diagnostic on an ecoreref itself,
			// but also for other expressions that might have a type mismatch
			// due to an ecoreref.
			// See https://github.com/LorenzoBettini/edelta/issues/560
			return IssueCodes.INCOMPATIBLE_TYPES.equals(diagnosticImpl.getCode());
		}
		return false;
	}
}
