/**
 * 
 */
package edelta.refactorings.lib.helper;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil.EqualityHelper;

/**
 * Custom implementation that does not consider eContainingClass when checking
 * for equality (since we want to check whether features in different classes
 * can be considered the same); it also stores and make available, in case
 * of non equality, the {@link EStructuralFeature} that made the equality fail.
 * 
 * @author Lorenzo Bettini
 *
 */
@SuppressWarnings("serial")
public class EstructuralFeatureEqualityHelper extends EqualityHelper {

	private EStructuralFeature difference;

	public EStructuralFeature getDifference() {
		return difference;
	}

	@Override
	protected boolean haveEqualFeature(EObject eObject1, EObject eObject2, EStructuralFeature feature) {
		boolean haveEqualFeature = super.haveEqualFeature(eObject1, eObject2, feature);
		if (!haveEqualFeature) {
			this.difference = feature;
		}
		return haveEqualFeature;
	}

	@Override
	protected boolean haveEqualReference(EObject eObject1, EObject eObject2, EReference reference) {
		if (reference.equals(EcorePackage.Literals.ESTRUCTURAL_FEATURE__ECONTAINING_CLASS)) {
			return true;
		}
		return super.haveEqualReference(eObject1, eObject2, reference);
	}
}
