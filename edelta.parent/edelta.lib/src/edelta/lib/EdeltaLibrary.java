/**
 * 
 */
package edelta.lib;

import java.util.function.Consumer;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;

/**
 * Library functions to be reused in Edelta programs.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaLibrary {

	private EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;

	private <T extends ENamedElement> void safeRunInitializer(Consumer<T> initializer, T e) {
		if (initializer != null) {
			initializer.accept(e);
		}
	}

	public EClass newEClass(String name) {
		return newEClass(name, null);
	}

	public EClass newEClass(String name, Consumer<EClass> initializer) {
		EClass c = ecoreFactory.createEClass();
		c.setName(name);
		safeRunInitializer(initializer, c);
		return c;
	}

	public EAttribute newEAttribute(String name, EDataType dataType) {
		return newEAttribute(name, dataType, null);
	}

	public EAttribute newEAttribute(String name, EDataType dataType, Consumer<EAttribute> initializer) {
		EAttribute e = ecoreFactory.createEAttribute();
		e.setName(name);
		e.setEType(dataType);
		safeRunInitializer(initializer, e);
		return e;
	}

	public EReference newEReference(String name) {
		return newEReference(name, null, null);
	}

	public EReference newEReference(String name, Consumer<EReference> initializer) {
		EReference e = ecoreFactory.createEReference();
		e.setName(name);
		safeRunInitializer(initializer, e);
		return e;
	}

	public EReference newEReference(String name, EClass referenceType) {
		return newEReference(name, referenceType, null);
	}

	public EReference newEReference(String name, EClass referenceType, Consumer<EReference> initializer) {
		EReference e = ecoreFactory.createEReference();
		e.setName(name);
		e.setEType(referenceType);
		safeRunInitializer(initializer, e);
		return e;
	}

	public EDataType newEDataType(String name) {
		return newEDataType(name, null, null);
	}

	public EDataType newEDataType(String name, Consumer<EDataType> initializer) {
		EDataType e = ecoreFactory.createEDataType();
		e.setName(name);
		safeRunInitializer(initializer, e);
		return e;
	}

	public EDataType newEDataType(String name, String instanceTypeName) {
		return newEDataType(name, instanceTypeName, null);
	}

	public EDataType newEDataType(String name, String instanceTypeName, Consumer<EDataType> initializer) {
		EDataType e = ecoreFactory.createEDataType();
		e.setName(name);
		e.setInstanceTypeName(instanceTypeName);
		safeRunInitializer(initializer, e);
		return e;
	}

	public EEnum newEEnum(String name) {
		return newEEnum(name, null);
	}

	public EEnum newEEnum(String name, Consumer<EEnum> initializer) {
		EEnum e = ecoreFactory.createEEnum();
		e.setName(name);
		safeRunInitializer(initializer, e);
		return e;
	}

	public EEnumLiteral newEEnumLiteral(String name) {
		return newEEnumLiteral(name, null);
	}

	public EEnumLiteral newEEnumLiteral(String name, Consumer<EEnumLiteral> initializer) {
		EEnumLiteral e = ecoreFactory.createEEnumLiteral();
		e.setName(name);
		safeRunInitializer(initializer, e);
		return e;
	}

	/**
	 * Returns a String representation based on the containment relation.
	 * 
	 * @param e
	 * @return
	 */
	public String getEObjectRepr(EObject e) {
		String info = e.toString();
		if (e instanceof ENamedElement) {
			info = ((ENamedElement) e).getName();
		}
		return e.eContainer() != null ? getEObjectRepr(e.eContainer()) + ":" + info : info;
	}

	public void addEStructuralFeature(EClass eClass, EStructuralFeature eStructuralFeature) {
		eClass.getEStructuralFeatures().add(eStructuralFeature);
	}

	private void addEClassifier(EPackage ePackage, EClassifier eClassifier) {
		ePackage.getEClassifiers().add(eClassifier);
	}

	public void addEClass(EPackage ePackage, EClass eClass) {
		addEClassifier(ePackage, eClass);
	}

	public EClass addNewEClass(EPackage ePackage, String name) {
		return addNewEClass(ePackage, name, null);
	}

	public EClass addNewEClass(EPackage ePackage, String name, Consumer<EClass> initializer) {
		EClass c = ecoreFactory.createEClass();
		c.setName(name);
		addEClass(ePackage, c);
		safeRunInitializer(initializer, c);
		return c;
	}

	public void addEEnum(EPackage ePackage, EEnum eEnum) {
		addEClassifier(ePackage, eEnum);
	}

	public EEnum addNewEEnum(EPackage ePackage, String name) {
		return addNewEEnum(ePackage, name, null);
	}

	public EEnum addNewEEnum(EPackage ePackage, String name, Consumer<EEnum> initializer) {
		EEnum e = ecoreFactory.createEEnum();
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
		EEnumLiteral e = ecoreFactory.createEEnumLiteral();
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
		EDataType e = ecoreFactory.createEDataType();
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
		EAttribute e = ecoreFactory.createEAttribute();
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
		EReference e = ecoreFactory.createEReference();
		e.setName(name);
		e.setEType(referenceType);
		addEReference(eClass, e);
		safeRunInitializer(initializer, e);
		return e;
	}

	public void addESuperType(EClass subClass, EClass superClass) {
		subClass.getESuperTypes().add(superClass);
	}

	public void removeESuperType(EClass subClass, EClass superClass) {
		subClass.getESuperTypes().remove(superClass);
	}

	public EPackage addNewESubpackage(EPackage superPackage, String name, String nsPrefix, String nsURI) {
		return addNewESubpackage(superPackage, name, nsPrefix, nsURI, null);
	}

	public EPackage addNewESubpackage(EPackage superPackage, String name, String nsPrefix, String nsURI,
			Consumer<EPackage> initializer) {
		EPackage newSubpackage = ecoreFactory.createEPackage();
		newSubpackage.setName(name);
		newSubpackage.setNsPrefix(nsPrefix);
		newSubpackage.setNsURI(nsURI);
		superPackage.getESubpackages().add(newSubpackage);
		safeRunInitializer(initializer, newSubpackage);
		return newSubpackage;
	}

}
