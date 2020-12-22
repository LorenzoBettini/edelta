/**
 * 
 */
package edelta.refactorings.lib.helper;

import static edelta.lib.EdeltaLibrary.getEObjectRepr;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
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

	public boolean equals(EObject eObject1, EObject eObject2) {
		return new EqualityHelper() {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean haveEqualFeature(EObject eObject1, EObject eObject2, EStructuralFeature feature) {
				if (featuresToIgnore.contains(feature))
					return true;
				var haveEqualFeature = super.haveEqualFeature(eObject1, eObject2, feature);
				if (!haveEqualFeature) {
					details.append(getEObjectRepr(feature) + ":\n");
					appendDetails(eObject1, feature);
					appendDetails(eObject2, feature);
				}
				return haveEqualFeature;
			}

			private void appendDetails(EObject eObject, EStructuralFeature feature) {
				details.append("  ");
				details.append(getEObjectRepr(eObject));
				details.append(": ");
				details.append(stringRepresentation(eObject, feature) + "\n");
			}

			private String stringRepresentation(EObject eObject1, EStructuralFeature feature) {
				var value = eObject1.eGet(feature);
				if (value instanceof EObject) {
					return getEObjectRepr((EObject) value);
				}
				return "" + value;
			}

		}.equals(eObject1, eObject2);
	}

	public EdeltaFeatureDifferenceFinder ignoring(EStructuralFeature featureToIgnore) {
		this.featuresToIgnore.add(featureToIgnore);
		return this;
	}

	public EdeltaFeatureDifferenceFinder ignoringName() {
		return ignoring(EcorePackage.Literals.ENAMED_ELEMENT__NAME);
	}

	public String getDifferenceDetails() {
		return this.details.toString();
	}

}
