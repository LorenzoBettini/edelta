/**
 * 
 */
package edelta.resource;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.EClassImpl;

import edelta.edelta.EdeltaEcoreReference;

/**
 * Customization of {@link EClassImpl} with additional superclasses, which are
 * {@link EdeltaEcoreReference}, whose actual resolution is delayed; this is
 * used to create instances of {@link EClass} as derived state.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaDerivedStateEClass extends EClassImpl {

	private List<EdeltaEcoreReference> ecoreReferences;

	public EdeltaDerivedStateEClass(List<EdeltaEcoreReference> ecoreReferences) {
		super();
		this.ecoreReferences = ecoreReferences;
	}

	@Override
	public EList<EClass> getESuperTypes() {
		if (eSuperTypes == null) {
			super.getESuperTypes();
			eSuperTypes.addAll(
				ecoreReferences.
					stream().
					filter(ref -> ref.getEnamedelement() instanceof EClass).
					map(ref -> (EClass) ref.getEnamedelement()).
					collect(Collectors.toList())
			);
		}
		return eSuperTypes;
	}
}
