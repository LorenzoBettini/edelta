package edelta.lib;

import java.util.Map;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.util.EcoreUtil.Copier;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * A custom {@link Copier} for EMF model (not Ecores, instances of Ecores)
 * migration. Without changing the values of the objects of the model, it takes
 * care of copying models of some Ecores as models of some other Ecores, which
 * are the exact copy of the original Ecores.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaModelCopier extends Copier {
	private static final long serialVersionUID = 1L;

	private BiMap<EObject, EObject> ecoreCopyMap;

	public EdeltaModelCopier(Map<EObject, EObject> ecoreCopyMap) {
		// by default useOriginalReferences is true, but this breaks
		// our migration strategy: if a reference refers something that
		// in the evolved model has been removed, it must NOT refer to
		// the old object
		super(true, false);
		this.ecoreCopyMap = HashBiMap.create(ecoreCopyMap);
	}

	/**
	 * An object can be explicitly copied after a containment reference
	 * became a non-containment reference, we must first check whether it
	 * has already been copied.
	 */
	@Override
	public EObject copy(EObject eObject) {
		var alreadyCopied = get(eObject);
		if (alreadyCopied != null)
			return alreadyCopied;
		return super.copy(eObject);
	}

	@Override
	protected EClass getTarget(EClass eClass) {
		return getMapped(eClass);
	}

	@Override
	protected EStructuralFeature getTarget(EStructuralFeature eStructuralFeature) {
		return getMapped(eStructuralFeature);
	}

	/**
	 * Handles values for enums differently, since they are objects, so we must
	 * retrieve the corresponding mapped enum literal, or we'll get a
	 * {@link ClassCastException}.
	 */
	@Override
	protected void copyAttributeValue(EAttribute eAttribute, EObject eObject, Object value, Setting setting) {
		var dataType = eAttribute.getEAttributeType();
		if (dataType instanceof EEnum) {
			value = getMapped((EEnumLiteral) value);
		}
		super.copyAttributeValue(eAttribute, eObject, value, setting);
	}

	private <T extends EObject> T getMapped(T o) {
		var value = ecoreCopyMap.get(o);
		@SuppressWarnings("unchecked")
		var mapped = (T) value;
		if (isNotThereAnymore(mapped))
			return null;
		return mapped;
	}

	@SuppressWarnings("unchecked")
	public <T extends EObject> T getOriginal(T o) {
		return (T) ecoreCopyMap.inverse().get(o);
	}

	private boolean isStillThere(EObject target) {
		return target != null && target.eResource() != null;
	}

	private boolean isNotThereAnymore(EObject target) {
		return target == null || target.eResource() == null;
	}

	public boolean isRelatedTo(ENamedElement origEcoreElement, ENamedElement evolvedEcoreElement) {
		return isStillThere(evolvedEcoreElement) &&
			wasRelatedTo(origEcoreElement, evolvedEcoreElement);
	}

	public boolean wasRelatedTo(ENamedElement origEcoreElement, ENamedElement evolvedEcoreElement) {
		return origEcoreElement == ecoreCopyMap.inverse().get(evolvedEcoreElement);
	}
}