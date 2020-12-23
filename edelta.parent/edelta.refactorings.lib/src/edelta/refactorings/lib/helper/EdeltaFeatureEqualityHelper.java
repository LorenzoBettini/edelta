/**
 * 
 */
package edelta.refactorings.lib.helper;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil.EqualityHelper;

/**
 * Relies on a custom implementation of {@link EqualityHelper} that does not
 * consider eContainingClass when checking for equality (since we want to check
 * whether features in different classes can be considered the same).
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaFeatureEqualityHelper {

	public boolean equals(EObject eObject1, EObject eObject2) {
		return new EqualityHelper() {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean haveEqualReference(EObject eObject1, EObject eObject2, EReference reference) {
				if (reference.equals(EcorePackage.Literals.ESTRUCTURAL_FEATURE__ECONTAINING_CLASS)) {
					return true;
				}
				return super.haveEqualReference(eObject1, eObject2, reference);
			}

		}.equals(eObject1, eObject2);
	}

}
