/**
 * 
 */
package edelta.refactorings.lib.helper;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil.EqualityHelper;

/**
 * Relies on a custom implementation of {@link EqualityHelper} that does not
 * consider the name when checking for equality (since we want to check whether
 * features have the same type, multiplicity, etc.); it also stores and make
 * available, in case of non equality, the {@link EStructuralFeature} that made
 * the equality fail.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EstructuralFeatureCharacteristicEqualityHelper {

	private EStructuralFeature difference;

	public EStructuralFeature getDifference() {
		return difference;
	}

	public boolean equals(EObject eObject1, EObject eObject2) {
		return new EqualityHelper() {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean haveEqualFeature(EObject eObject1, EObject eObject2, EStructuralFeature feature) {
				if (feature.equals(EcorePackage.Literals.ENAMED_ELEMENT__NAME))
					return true;
				boolean haveEqualFeature = super.haveEqualFeature(eObject1, eObject2, feature);
				if (!haveEqualFeature) {
					difference = feature;
				}
				return haveEqualFeature;
			}

		}.equals(eObject1, eObject2);
	}

}
