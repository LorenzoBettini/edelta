package edelta.ui.outline;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.xbase.XExpression;

import com.google.inject.Inject;

import edelta.edelta.EdeltaProgram;
import edelta.resource.derivedstate.EdeltaCopiedEPackagesMap;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.resource.derivedstate.EdeltaModifiedFeatures;
import edelta.resource.derivedstate.EdeltaModelPreview;

/**
 * Determines whether a final preview row has direct, verified change evidence.
 * Styling is independent of whether one source expression can be selected.
 *
 * @author Lorenzo Bettini
 */
public class EdeltaModelOutlineStyling {
	@Inject
	private EdeltaDerivedStateHelper derivedStateHelper;

	/**
	 * Checks Ecore row evidence and the exact final EObject/feature migration token.
	 *
	 * @param program the current Edelta program
	 * @param owner the final preview EObject
	 * @param feature the feature represented by the row
	 * @return whether the property row is proven changed
	 */
	public boolean isChangedProperty(EdeltaProgram program, EObject owner,
			EStructuralFeature feature) {
		var source = program.eResource();
		var copiedPackages = derivedStateHelper.getCopiedEPackagesMap(source);
		var modifiedFeatures = derivedStateHelper.getModifiedFeatures(source);
		if (hasVisibleEcoreChange(program, feature, copiedPackages, modifiedFeatures)) {
			return true;
		}
		return derivedStateHelper.getModelPreview(source)
			.getMigrationExpression(owner, feature) != null;
	}

	/**
	 * Checks direct class-name evidence or eligible rows on this object only.
	 *
	 * @param program the current Edelta program
	 * @param modelElement the final preview EObject
	 * @return whether the object row is proven changed
	 */
	public boolean isChangedObject(EdeltaProgram program, EObject modelElement) {
		var source = program.eResource();
		var copiedPackages = derivedStateHelper.getCopiedEPackagesMap(source);
		var modifiedFeatures = derivedStateHelper.getModifiedFeatures(source);
		var eClass = modelElement.eClass();
		if (hasChangedClassName(program, eClass, copiedPackages, modifiedFeatures)) {
			return true;
		}
		var preview = derivedStateHelper.getModelPreview(source);
		for (var feature : eClass.getEAllStructuralFeatures()) {
			if (isDisplayedProperty(feature) &&
					(hasVisibleEcoreChange(program, feature, copiedPackages, modifiedFeatures) ||
						preview.getMigrationExpression(modelElement, feature) != null)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasChangedClassName(EdeltaProgram program, EClass eClass,
			EdeltaCopiedEPackagesMap copiedPackages, EdeltaModifiedFeatures modifiedFeatures) {
		XExpression expression = EdeltaModelOutlineNavigation.classExpression(eClass,
			copiedPackages.getOriginal(eClass),
			modifiedFeatures.get(eClass, EcorePackage.Literals.ENAMED_ELEMENT__NAME),
			derivedStateHelper.getLastResponsibleExpression(program, eClass));
		return expression != null;
	}

	private boolean hasVisibleEcoreChange(EdeltaProgram program, EStructuralFeature feature,
			EdeltaCopiedEPackagesMap copiedPackages, EdeltaModifiedFeatures modifiedFeatures) {
		var original = copiedPackages.getOriginal(feature);
		if (!(original instanceof EStructuralFeature originalFeature)) {
			return derivedStateHelper.getLastResponsibleExpression(program, feature) != null;
		}
		if (hasRecordedDifference(feature, originalFeature, modifiedFeatures,
				EcorePackage.Literals.ENAMED_ELEMENT__NAME)) {
			return true;
		}
		if (feature instanceof EReference) {
			return hasRecordedDifference(feature, originalFeature, modifiedFeatures,
					EcorePackage.Literals.ETYPED_ELEMENT__LOWER_BOUND)
				|| hasRecordedDifference(feature, originalFeature, modifiedFeatures,
					EcorePackage.Literals.ETYPED_ELEMENT__UPPER_BOUND);
		}
		return feature.isMany() != originalFeature.isMany()
			&& hasRecordedDifference(feature, originalFeature, modifiedFeatures,
				EcorePackage.Literals.ETYPED_ELEMENT__UPPER_BOUND);
	}

	private boolean hasRecordedDifference(EStructuralFeature feature,
			EStructuralFeature originalFeature, EdeltaModifiedFeatures modifiedFeatures,
			EStructuralFeature metadata) {
		var change = modifiedFeatures.get(feature, metadata);
		return change != null && !change.initial()
			&& !java.util.Objects.equals(feature.eGet(metadata), originalFeature.eGet(metadata));
	}

	private boolean isDisplayedProperty(EStructuralFeature feature) {
		if (feature instanceof EAttribute) {
			return true;
		}
		var reference = (EReference) feature;
		return !reference.isContainment() && !reference.isContainer();
	}
}
