/**
 * 
 */
package edelta.lib;

import static edelta.lib.EdeltaUtils.getEObjectRepr;

import java.util.function.Consumer;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;

/**
 * Standard library methods, for example, for adding, copying, moving
 * elements.
 * 
 * @author Lorennzo Bettini
 *
 */
public class EdeltaStandardLibrary extends AbstractEdelta {

	private static EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;

	public EdeltaStandardLibrary() {
		super();
	}

	/**
	 * @param other
	 */
	public EdeltaStandardLibrary(AbstractEdelta other) {
		super(other);
	}

	public void addEStructuralFeature(EClass eClass, EStructuralFeature eStructuralFeature) {
		var eStructuralFeatures = eClass.getEStructuralFeatures();
		var existing = eClass.getEStructuralFeature(eStructuralFeature.getName());
		if (existing != null) {
			var errorMessage = getEObjectRepr(eClass) +
					" already contains " +
					existing.eClass().getName() +
					" " +
					getEObjectRepr(existing);
			showError(eStructuralFeature, errorMessage);
			throw new IllegalArgumentException(errorMessage);
		}
		eStructuralFeatures.add(eStructuralFeature);
	}

	private void addEClassifier(EPackage ePackage, EClassifier eClassifier) {
		var eClassifiers = ePackage.getEClassifiers();
		var existing = ePackage.getEClassifier(eClassifier.getName());
		if (existing != null) {
			var errorMessage = getEObjectRepr(ePackage) +
					" already contains " +
					existing.eClass().getName() +
					" " +
					getEObjectRepr(existing);
			showError(eClassifier, errorMessage);
			throw new IllegalArgumentException(errorMessage);
		}
		eClassifiers.add(eClassifier);
	}

	public void addEClass(EPackage ePackage, EClass eClass) {
		addEClassifier(ePackage, eClass);
	}

	public EClass addNewEClass(EPackage ePackage, String name) {
		return addNewEClass(ePackage, name, null);
	}

	public EClass addNewEClass(EPackage ePackage, String name, Consumer<EClass> initializer) {
		var c = ecoreFactory.createEClass();
		c.setName(name);
		addEClass(ePackage, c);
		safeRunInitializer(initializer, c);
		return c;
	}

	public EClass addNewAbstractEClass(EPackage ePackage, String name) {
		return addNewAbstractEClass(ePackage, name, null);
	}

	public EClass addNewAbstractEClass(EPackage ePackage, String name, Consumer<EClass> initializer) {
		return addNewEClass(ePackage, name, c -> {
			c.setAbstract(true);
			safeRunInitializer(initializer, c);
		});
	}

	public void addEEnum(EPackage ePackage, EEnum eEnum) {
		addEClassifier(ePackage, eEnum);
	}

	public EEnum addNewEEnum(EPackage ePackage, String name) {
		return addNewEEnum(ePackage, name, null);
	}

	public EEnum addNewEEnum(EPackage ePackage, String name, Consumer<EEnum> initializer) {
		var e = ecoreFactory.createEEnum();
		e.setName(name);
		addEEnum(ePackage, e);
		safeRunInitializer(initializer, e);
		return e;
	}

	public void addEEnumLiteral(EEnum eEnum, EEnumLiteral eEnumLiteral) {
		eEnum.getELiterals().add(eEnumLiteral);
	}

	public EEnumLiteral addNewEEnumLiteral(EEnum eEnum, String name) {
		return addNewEEnumLiteral(eEnum, name, null);
	}

	public EEnumLiteral addNewEEnumLiteral(EEnum eEnum, String name, Consumer<EEnumLiteral> initializer) {
		var e = ecoreFactory.createEEnumLiteral();
		e.setName(name);
		addEEnumLiteral(eEnum, e);
		safeRunInitializer(initializer, e);
		return e;
	}

	public void addEDataType(EPackage ePackage, EDataType eDataType) {
		addEClassifier(ePackage, eDataType);
	}

	public EDataType addNewEDataType(EPackage ePackage, String name, String instanceTypeName) {
		return addNewEDataType(ePackage, name, instanceTypeName, null);
	}

	public EDataType addNewEDataType(EPackage ePackage, String name, String instanceTypeName, Consumer<EDataType> initializer) {
		var e = ecoreFactory.createEDataType();
		e.setName(name);
		e.setInstanceTypeName(instanceTypeName);
		addEDataType(ePackage, e);
		safeRunInitializer(initializer, e);
		return e;
	}

	public void addEAttribute(EClass eClass, EAttribute eAttribute) {
		addEStructuralFeature(eClass, eAttribute);
	}

	public EAttribute addNewEAttribute(EClass eClass, String name, EDataType dataType) {
		return addNewEAttribute(eClass, name, dataType, null);
	}

	public EAttribute addNewEAttribute(EClass eClass, String name, EDataType dataType, Consumer<EAttribute> initializer) {
		var e = ecoreFactory.createEAttribute();
		e.setName(name);
		e.setEType(dataType);
		addEAttribute(eClass, e);
		safeRunInitializer(initializer, e);
		return e;
	}

	public void addEReference(EClass eClass, EReference eReference) {
		addEStructuralFeature(eClass, eReference);
	}

	public EReference addNewEReference(EClass eClass, String name, EClass referenceType) {
		return addNewEReference(eClass, name, referenceType, null);
	}

	public EReference addNewEReference(EClass eClass, String name, EClass referenceType, Consumer<EReference> initializer) {
		var e = ecoreFactory.createEReference();
		e.setName(name);
		e.setEType(referenceType);
		addEReference(eClass, e);
		safeRunInitializer(initializer, e);
		return e;
	}

	public EReference addNewContainmentEReference(EClass eClass, String name, EClass referenceType) {
		return addNewContainmentEReference(eClass, name, referenceType, null);
	}

	public EReference addNewContainmentEReference(EClass eClass, String name, EClass referenceType, Consumer<EReference> initializer) {
		return addNewEReference(eClass, name, referenceType, r -> {
			r.setContainment(true);
			safeRunInitializer(initializer, r);
		});
	}

	public void addESuperType(EClass subClass, EClass superClass) {
		subClass.getESuperTypes().add(superClass);
	}

	public EClass addNewSubclass(EClass superClass, String name) {
		return addNewSubclass(superClass, name, null);
	}

	public EClass addNewSubclass(EClass superClass, String name, Consumer<EClass> initializer) {
		return addNewEClass(superClass.getEPackage(), name, c -> {
			c.getESuperTypes().add(superClass);
			safeRunInitializer(initializer, c);
		});
	}

	public void removeESuperType(EClass subClass, EClass superClass) {
		subClass.getESuperTypes().remove(superClass);
	}

	public EPackage addNewESubpackage(EPackage superPackage, String name, String nsPrefix, String nsURI) {
		return addNewESubpackage(superPackage, name, nsPrefix, nsURI, null);
	}

	public EPackage addNewESubpackage(EPackage superPackage, String name, String nsPrefix, String nsURI,
			Consumer<EPackage> initializer) {
		var newSubpackage = ecoreFactory.createEPackage();
		newSubpackage.setName(name);
		newSubpackage.setNsPrefix(nsPrefix);
		newSubpackage.setNsURI(nsURI);
		superPackage.getESubpackages().add(newSubpackage);
		safeRunInitializer(initializer, newSubpackage);
		return newSubpackage;
	}

	private <T extends ENamedElement> void safeRunInitializer(Consumer<T> initializer, T e) {
		if (initializer != null) {
			initializer.accept(e);
		}
	}

}
