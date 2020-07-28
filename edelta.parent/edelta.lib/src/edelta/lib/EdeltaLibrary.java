/**
 * 
 */
package edelta.lib;

import java.util.HashSet;
import java.util.Set;
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

	private static EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;

	private static <T extends ENamedElement> void safeRunInitializer(Consumer<T> initializer, T e) {
		if (initializer != null) {
			initializer.accept(e);
		}
	}

	public static EClass newEClass(String name) {
		return newEClass(name, null);
	}

	public static EClass newEClass(String name, Consumer<EClass> initializer) {
		EClass c = ecoreFactory.createEClass();
		c.setName(name);
		safeRunInitializer(initializer, c);
		return c;
	}

	public static EAttribute newEAttribute(String name, EDataType dataType) {
		return newEAttribute(name, dataType, null);
	}

	public static EAttribute newEAttribute(String name, EDataType dataType, Consumer<EAttribute> initializer) {
		EAttribute e = ecoreFactory.createEAttribute();
		e.setName(name);
		e.setEType(dataType);
		safeRunInitializer(initializer, e);
		return e;
	}

	public static EReference newEReference(String name, EClass referenceType) {
		return newEReference(name, referenceType, null);
	}

	public static EReference newEReference(String name, EClass referenceType, Consumer<EReference> initializer) {
		EReference e = ecoreFactory.createEReference();
		e.setName(name);
		e.setEType(referenceType);
		safeRunInitializer(initializer, e);
		return e;
	}

	public static EDataType newEDataType(String name, String instanceTypeName) {
		return newEDataType(name, instanceTypeName, null);
	}

	public static EDataType newEDataType(String name, String instanceTypeName, Consumer<EDataType> initializer) {
		EDataType e = ecoreFactory.createEDataType();
		e.setName(name);
		e.setInstanceTypeName(instanceTypeName);
		safeRunInitializer(initializer, e);
		return e;
	}

	public static EEnum newEEnum(String name) {
		return newEEnum(name, null);
	}

	public static EEnum newEEnum(String name, Consumer<EEnum> initializer) {
		EEnum e = ecoreFactory.createEEnum();
		e.setName(name);
		safeRunInitializer(initializer, e);
		return e;
	}

	public static EEnumLiteral newEEnumLiteral(String name) {
		return newEEnumLiteral(name, null);
	}

	public static EEnumLiteral newEEnumLiteral(String name, Consumer<EEnumLiteral> initializer) {
		EEnumLiteral e = ecoreFactory.createEEnumLiteral();
		e.setName(name);
		safeRunInitializer(initializer, e);
		return e;
	}

	/**
	 * Returns a String representation (fully qualified) based on the containment
	 * relation (it handles cycles safely).
	 * 
	 * For example
	 * 
	 * <pre>
	 * getEObjectRepr(EcorePackage.eINSTANCE.getEClass_ESuperTypes())
	 * </pre>
	 * 
	 * will return
	 * 
	 * <pre>
	 * "ecore.EClass.eSuperTypes"
	 * </pre>
	 * 
	 * @param e
	 * @return
	 */
	public static String getEObjectRepr(EObject e) {
		Set<EObject> seen = new HashSet<>();
		StringBuilder builder = new StringBuilder();
		EObject current = e;
		while (current != null) {
			if (builder.length() > 0)
				builder.insert(0, ".");
			if (current instanceof ENamedElement) {
				builder.insert(0, ((ENamedElement) current).getName());
			} else {
				builder.insert(0, current.toString());
			}
			if (seen.contains(current))
				break;
			seen.add(current);
			current = current.eContainer();
		}
		return builder.toString();
	}

	public static void addEStructuralFeature(EClass eClass, EStructuralFeature eStructuralFeature) {
		eClass.getEStructuralFeatures().add(eStructuralFeature);
	}

	private static void addEClassifier(EPackage ePackage, EClassifier eClassifier) {
		ePackage.getEClassifiers().add(eClassifier);
	}

	public static void addEClass(EPackage ePackage, EClass eClass) {
		addEClassifier(ePackage, eClass);
	}

	public static EClass addNewEClass(EPackage ePackage, String name) {
		return addNewEClass(ePackage, name, null);
	}

	public static EClass addNewEClass(EPackage ePackage, String name, Consumer<EClass> initializer) {
		EClass c = ecoreFactory.createEClass();
		c.setName(name);
		addEClass(ePackage, c);
		safeRunInitializer(initializer, c);
		return c;
	}

	public static void addEEnum(EPackage ePackage, EEnum eEnum) {
		addEClassifier(ePackage, eEnum);
	}

	public static EEnum addNewEEnum(EPackage ePackage, String name) {
		return addNewEEnum(ePackage, name, null);
	}

	public static EEnum addNewEEnum(EPackage ePackage, String name, Consumer<EEnum> initializer) {
		EEnum e = ecoreFactory.createEEnum();
		e.setName(name);
		addEEnum(ePackage, e);
		safeRunInitializer(initializer, e);
		return e;
	}

	public static void addEEnumLiteral(EEnum eEnum, EEnumLiteral eEnumLiteral) {
		eEnum.getELiterals().add(eEnumLiteral);
	}

	public static EEnumLiteral addNewEEnumLiteral(EEnum eEnum, String name) {
		return addNewEEnumLiteral(eEnum, name, null);
	}

	public static EEnumLiteral addNewEEnumLiteral(EEnum eEnum, String name, Consumer<EEnumLiteral> initializer) {
		EEnumLiteral e = ecoreFactory.createEEnumLiteral();
		e.setName(name);
		addEEnumLiteral(eEnum, e);
		safeRunInitializer(initializer, e);
		return e;
	}

	public static void addEDataType(EPackage ePackage, EDataType eDataType) {
		addEClassifier(ePackage, eDataType);
	}

	public static EDataType addNewEDataType(EPackage ePackage, String name, String instanceTypeName) {
		return addNewEDataType(ePackage, name, instanceTypeName, null);
	}

	public static EDataType addNewEDataType(EPackage ePackage, String name, String instanceTypeName, Consumer<EDataType> initializer) {
		EDataType e = ecoreFactory.createEDataType();
		e.setName(name);
		e.setInstanceTypeName(instanceTypeName);
		addEDataType(ePackage, e);
		safeRunInitializer(initializer, e);
		return e;
	}

	public static void addEAttribute(EClass eClass, EAttribute eAttribute) {
		addEStructuralFeature(eClass, eAttribute);
	}

	public static EAttribute addNewEAttribute(EClass eClass, String name, EDataType dataType) {
		return addNewEAttribute(eClass, name, dataType, null);
	}

	public static EAttribute addNewEAttribute(EClass eClass, String name, EDataType dataType, Consumer<EAttribute> initializer) {
		EAttribute e = ecoreFactory.createEAttribute();
		e.setName(name);
		e.setEType(dataType);
		addEAttribute(eClass, e);
		safeRunInitializer(initializer, e);
		return e;
	}

	public static void addEReference(EClass eClass, EReference eReference) {
		addEStructuralFeature(eClass, eReference);
	}

	public static EReference addNewEReference(EClass eClass, String name, EClass referenceType) {
		return addNewEReference(eClass, name, referenceType, null);
	}

	public static EReference addNewEReference(EClass eClass, String name, EClass referenceType, Consumer<EReference> initializer) {
		EReference e = ecoreFactory.createEReference();
		e.setName(name);
		e.setEType(referenceType);
		addEReference(eClass, e);
		safeRunInitializer(initializer, e);
		return e;
	}

	public static void addESuperType(EClass subClass, EClass superClass) {
		subClass.getESuperTypes().add(superClass);
	}

	public static void removeESuperType(EClass subClass, EClass superClass) {
		subClass.getESuperTypes().remove(superClass);
	}

	public static EPackage addNewESubpackage(EPackage superPackage, String name, String nsPrefix, String nsURI) {
		return addNewESubpackage(superPackage, name, nsPrefix, nsURI, null);
	}

	public static EPackage addNewESubpackage(EPackage superPackage, String name, String nsPrefix, String nsURI,
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
