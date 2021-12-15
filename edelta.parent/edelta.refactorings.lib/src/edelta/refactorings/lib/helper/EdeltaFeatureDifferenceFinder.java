/**
 * 
 */
package edelta.refactorings.lib.helper;

import static edelta.lib.EdeltaUtils.getEObjectRepr;
import static org.eclipse.emf.ecore.EcorePackage.Literals.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil.EqualityHelper;

/**
 * Detects possible differences between two {@link EStructuralFeature}s.
 * 
 * One can specify the properties to ignore when comparing features. Details
 * about possible differences are also provided.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaFeatureDifferenceFinder {

	private List<EStructuralFeature> featuresToIgnore = new ArrayList<>();
	private StringBuilder details = new StringBuilder();

	public boolean equals(EStructuralFeature feature1, EStructuralFeature feature2) {
		var eClass1 = feature1.eClass();
		var eClass2 = feature2.eClass();
		if (eClass1 != eClass2) {
			details.append("different kinds:\n");
			appendDetails(feature1, getEObjectRepr(eClass1));
			appendDetails(feature2, getEObjectRepr(eClass2));
			return false;
		}
		return new EqualityHelper() {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean haveEqualFeature(EObject eObject1, EObject eObject2, EStructuralFeature feature) {
				if (eObject1 == feature1 && featuresToIgnore.contains(feature))
					return true;
				var haveEqualFeature = super.haveEqualFeature(eObject1, eObject2, feature);
				if (!haveEqualFeature) {
					appendDetails(eObject1, eObject2, feature);
				}
				return haveEqualFeature;
			}

		}.equals(feature1, feature2);
	}

	private void appendDetails(EObject eObject1, EObject eObject2, EStructuralFeature feature) {
		details.append(getEObjectRepr(feature) + ":\n");
		appendDetails(eObject1, feature);
		appendDetails(eObject2, feature);
	}

	private void appendDetails(EObject eObject, EStructuralFeature feature) {
		appendDetails(eObject, stringRepresentation(eObject, feature));
	}

	private void appendDetails(EObject eObject, String value) {
		details.append("  ");
		details.append(getEObjectRepr(eObject));
		details.append(": ");
		details.append(value + "\n");
	}

	private String stringRepresentation(EObject eObject1, EStructuralFeature feature) {
		var value = eObject1.eGet(feature);
		if (value instanceof EObject) {
			return getEObjectRepr((EObject) value);
		}
		return "" + value;
	}

	/**
	 * Ignores the specified feature when finding differences.
	 * 
	 * @param featureToIgnore
	 * @return
	 */
	public EdeltaFeatureDifferenceFinder ignoring(EStructuralFeature featureToIgnore) {
		this.featuresToIgnore.add(featureToIgnore);
		return this;
	}

	/**
	 * Shortcut for {@link #ignoring(ENAMED_ELEMENT__NAME)}
	 * 
	 * @return
	 */
	public EdeltaFeatureDifferenceFinder ignoringName() {
		return ignoring(ENAMED_ELEMENT__NAME);
	}

	/**
	 * Shortcut for {@link #ignoring(ESTRUCTURAL_FEATURE__ECONTAINING_CLASS)}
	 * 
	 * @return
	 */
	public EdeltaFeatureDifferenceFinder ignoringContainingClass() {
		return ignoring(ESTRUCTURAL_FEATURE__ECONTAINING_CLASS);
	}

	/**
	 * Shortcut for {@link #ignoring(ETYPED_ELEMENT__ETYPE)}
	 * 
	 * @return
	 */
	public EdeltaFeatureDifferenceFinder ignoringType() {
		return ignoring(ETYPED_ELEMENT__ETYPE);
	}

	/**
	 * Retrieves the string containing difference details if any.
	 * 
	 * @return
	 */
	public String getDifferenceDetails() {
		return this.details.toString();
	}

}
