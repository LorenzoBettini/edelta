/**
 * 
 */
package edelta.lib;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

import com.google.common.collect.Iterables;

/**
 * Library functions to be reused in Edelta programs.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaLibrary {

	private static EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;

	private EdeltaLibrary() {
	}

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

	public static EClass addNewAbstractEClass(EPackage ePackage, String name) {
		return addNewAbstractEClass(ePackage, name, null);
	}

	public static EClass addNewAbstractEClass(EPackage ePackage, String name, Consumer<EClass> initializer) {
		return addNewEClass(ePackage, name, c -> {
			c.setAbstract(true);
			safeRunInitializer(initializer, c);
		});
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

	public static EReference addNewContainmentEReference(EClass eClass, String name, EClass referenceType) {
		return addNewContainmentEReference(eClass, name, referenceType, null);
	}

	public static EReference addNewContainmentEReference(EClass eClass, String name, EClass referenceType, Consumer<EReference> initializer) {
		return addNewEReference(eClass, name, referenceType, r -> {
			r.setContainment(true);
			safeRunInitializer(initializer, r);
		});
	}

	public static void addESuperType(EClass subClass, EClass superClass) {
		subClass.getESuperTypes().add(superClass);
	}

	public static EClass addNewSubclass(EClass superClass, String name) {
		return addNewSubclass(superClass, name, null);
	}

	public static EClass addNewSubclass(EClass superClass, String name, Consumer<EClass> initializer) {
		return addNewEClass(superClass.getEPackage(), name, c -> {
			c.getESuperTypes().add(superClass);
			safeRunInitializer(initializer, c);
		});
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

	/**
	 * Removes the {@link ENamedElement} and recursively its contents; it also makes
	 * sure that no dangling references are left in the Ecore model: if we remove an
	 * {@link EClassifier} it will also be removed as superclass, and
	 * {@link EReference}s that have such a type will be removed as well; moreover,
	 * if an {@link EReference} is removed, also its EOpposite will be removed.
	 * 
	 * This is meant to provide an alternative, and hopefully more efficient,
	 * implementation to {@link EcoreUtil#delete(EObject, boolean)}.
	 * 
	 * @param element
	 */
	public static void removeElement(ENamedElement element) {
		removeFutureDanglingReferences(element);
		EcoreUtil.remove(element);
	}

	/**
	 * This is meant to provide an alternative, and hopefully more efficient,
	 * implementation to {@link EcoreUtil#deleteAll(Collection, boolean)}.
	 * 
	 * @param elements
	 * @see EdeltaLibrary#removeElement(ENamedElement)
	 */
	public static void removeAllElements(Collection<? extends EObject> elements) {
		elements.stream().forEach(EdeltaLibrary::removeFutureDanglingReferences);
		EcoreUtil.removeAll(elements);
	}

	private static void removeFutureDanglingReferences(EObject element) {
		if (element instanceof EClassifier) {
			EClassifier classifier = (EClassifier) element;
			// first remove possible references to this classifier as type
			final var allEClasses = allEClasses(classifier.getEPackage());
			final var featuresToRemove = allEClasses.stream()
				.flatMap(c -> c.getEStructuralFeatures().stream())
				.filter(f -> f.getEType() == classifier)
				.collect(Collectors.toList());
			removeAllElements(featuresToRemove);
			// then remove possible superclass relations referring this classifier
			for (EClass eClass : allEClasses) {
				eClass.getESuperTypes().remove(classifier);
			}
		} else if (element instanceof EReference) {
			EReference reference = (EReference) element;
			if (reference.getEOpposite() != null)
				reference.getEOpposite().setEOpposite(null);
		}
		removeAllElements(element.eContents());
	}

	/**
	 * Returns a list of all the {@link EClass}es of the specified
	 * {@link EPackage}.
	 * 
	 * @param ePackage
	 * @return an empty list if the ePackage is null
	 */
	public static List<EClass> allEClasses(EPackage ePackage) {
		if (ePackage == null)
			return Collections.emptyList();
		return IterableExtensions.toList(
			Iterables.filter(ePackage.getEClassifiers(), EClass.class));
	}

	/**
	 * Copies the specified {@link EStructuralFeature} into the specified
	 * {@link EClass}, using {@link EcoreUtil#copy(EObject)}.
	 * 
	 * @param feature
	 * @param eClassDest
	 * @return the copied feature
	 * @see EcoreUtil#copy(EObject)
	 */
	public static EStructuralFeature copyTo(EStructuralFeature feature, EClass eClassDest) {
		EStructuralFeature copy = EcoreUtil.copy(feature);
		eClassDest.getEStructuralFeatures().add(copy);
		return copy;
	}

	/**
	 * Copies the specified {@link EStructuralFeature} into the specified
	 * {@link EClass}, using {@link EcoreUtil#copy(EObject)}, but changing its name.
	 * 
	 * @param feature
	 * @param eClassDest
	 * @param name
	 * @return the copied feature
	 * @see EcoreUtil#copy(EObject)
	 */
	public static EStructuralFeature copyToAs(EStructuralFeature feature, EClass eClassDest, String name) {
		EStructuralFeature copy = EcoreUtil.copy(feature);
		copy.setName(name);
		eClassDest.getEStructuralFeatures().add(copy);
		return copy;
	}

	/**
	 * Copies the specified {@link EStructuralFeature} into the specified
	 * {@link EClass}, using {@link EcoreUtil#copy(EObject)}, but changing its name
	 * and type.
	 * 
	 * @param feature
	 * @param eClassDest
	 * @param name
	 * @param type
	 * @return the copied feature
	 * @see EcoreUtil#copy(EObject)
	 */
	public static EStructuralFeature copyToAs(EStructuralFeature feature, EClass eClassDest, String name,
			EClassifier type) {
		EStructuralFeature copy = EcoreUtil.copy(feature);
		copy.setName(name);
		copy.setEType(type);
		eClassDest.getEStructuralFeatures().add(copy);
		return copy;
	}

	/**
	 * Copies the specified {@link EStructuralFeature}s into the specified
	 * {@link EClass}, using {@link EcoreUtil#copyAll(Collection)}.
	 * 
	 * @param features
	 * @param eClassDest
	 * @see EcoreUtil#copyAll(Collection)
	 */
	public static void copyAllTo(Collection<EStructuralFeature> features, EClass eClassDest) {
		eClassDest.getEStructuralFeatures().addAll(EcoreUtil.copyAll(features));
	}

	/**
	 * Moves the specified {@link EStructuralFeature} into the specified
	 * {@link EClass}.
	 * 
	 * @param feature
	 * @param eClassDest
	 */
	public static void moveTo(EStructuralFeature feature, EClass eClassDest) {
		eClassDest.getEStructuralFeatures().add(feature);
	}

	/**
	 * Moves the specified {@link EStructuralFeature}s into the specified
	 * {@link EClass}.
	 * 
	 * @param features
	 * @param eClassDest
	 */
	public static void moveAllTo(Collection<EStructuralFeature> features, EClass eClassDest) {
		eClassDest.getEStructuralFeatures().addAll(features);
	}

	/**
	 * Sets the EOpposite property of the two references and sets the EReferenceType
	 * accordingly (that is, if <tt>C1.r1</tt> and <tt>C2.r2</tt> are made
	 * bidirectional, then, <tt>C1.r1</tt> will have type <tt>C2</tt> and
	 * <tt>C2.r2</tt> will have type <tt>C1</tt>; existing opposite relations are
	 * removed and set to null.
	 * 
	 * @param ref1
	 * @param ref2
	 */
	public static void makeBidirectional(EReference ref1, EReference ref2) {
		dropOpposite(ref1);
		dropOpposite(ref2);
		makeBidirectionalInternal(ref1, ref2);
		makeBidirectionalInternal(ref2, ref1);
	}

	/**
	 * Sets the EOpposite property to null; if the reference has an EOpposite,
	 * the EOpposite of the EOpposite is also set to null. No references are removed.
	 * 
	 * @param reference
	 */
	public static void dropOpposite(EReference reference) {
		final var existingOpposite = reference.getEOpposite();
		if (existingOpposite != null)
			existingOpposite.setEOpposite(null);
		reference.setEOpposite(null);
	}

	/**
	 * Removes the possible EOpposite of the passed reference:
	 * corresponds to {@link #dropOpposite(EReference)} and
	 * {@link #removeElement(ENamedElement)} passing both the possible EOpposite
	 * reference.
	 * 
	 * @param reference
	 */
	public static void removeOpposite(EReference reference) {
		final var opposite = reference.getEOpposite();
		if (opposite != null) {
			dropOpposite(opposite);
			removeElement(opposite);
		}
	}

	/**
	 * Creates a new EOpposite of the passed reference (dropping possibly
	 * existing one), with the correct type (that is, the result of
	 * {@link EReference#getEContainingClass()} and adding it as a reference in
	 * the specified target {@link EClass}.
	 * 
	 * @param reference
	 * @param name
	 * @return the created EOpposite reference
	 */
	public static EReference createOpposite(EReference reference, String name, EClass target) {
		return addNewEReference(target, name, reference.getEContainingClass(),
			newRef -> makeBidirectional(newRef, reference)
		);
	}

	private static void makeBidirectionalInternal(EReference r1, EReference r2) {
		r1.setEOpposite(r2);
		r1.setEType(r2.getEContainingClass());
	}

	/**
	 * Makes this {@link EClass} abstract
	 * 
	 * @param feature
	 */
	public static void makeAbstract(EClass cl) {
		cl.setAbstract(true);
	}

	/**
	 * Makes this {@link EClass} concrete (that is, not abstract)
	 * 
	 * @param feature
	 */
	public static void makeConcrete(EClass cl) {
		cl.setAbstract(false);
	}

	/**
	 * Makes this feature required as a single-valued (lower = upper = 1)
	 * 
	 * @param feature
	 */
	public static void makeSingleRequired(EStructuralFeature feature) {
		makeRequired(feature);
		makeSingle(feature);
	}

	/**
	 * Makes this feature single (upper = 1)
	 * 
	 * @param feature
	 */
	public static void makeSingle(EStructuralFeature feature) {
		feature.setUpperBound(1);
	}

	/**
	 * Makes this feature required (lower = 1)
	 * 
	 * @param feature
	 */
	public static void makeRequired(EStructuralFeature feature) {
		feature.setLowerBound(1);
	}

	/**
	 * Makes this feature multiple (upper = -1)
	 * 
	 * @param feature
	 */
	public static void makeMultiple(EStructuralFeature feature) {
		feature.setUpperBound(-1);
	}

	/**
	 * Makes this reference a containment reference.
	 * 
	 * @param reference
	 */
	public static void makeContainment(EReference reference) {
		reference.setContainment(true);
	}

	/**
	 * Makes this reference a non-containment reference.
	 * 
	 * @param reference
	 */
	public static void dropContainment(EReference reference) {
		reference.setContainment(false);
	}

	/**
	 * Given an {@link EClassifier} it returns a {@link Collection} of
	 * {@link EPackage} that can be inspected, for example, to search for
	 * references. It always returns a collection. In case the classifier is not
	 * contained in any package the returned collection is null. If the package is
	 * not contained in a {@link Resource} then the collection contains only the
	 * package. Otherwise it collects all the packages in all resources in the
	 * {@link ResourceSet} (if there is one). The {@link EcorePackage} is never
	 * part of the returned collection.
	 * 
	 * @param e
	 * @return
	 */
	public static Collection<EPackage> packagesToInspect(EClassifier e) {
		var ePackage = e.getEPackage();
		if (ePackage == null)
			return Collections.emptyList();
		var resource = ePackage.eResource();
		if (resource == null)
			return Collections.singleton(ePackage);
		var resourceSet = resource.getResourceSet();
		if (resourceSet == null)
			return filterEPackages(resource.getContents().stream());
		var flatContents = resourceSet.getResources().stream()
			.flatMap(r -> r.getContents().stream());
		return filterEPackages(flatContents);
	}

	private static List<EPackage> filterEPackages(Stream<EObject> objectsStream) {
		return objectsStream
				.filter(EPackage.class::isInstance)
				.map(EPackage.class::cast)
				.filter(p -> !EcorePackage.eNS_URI.equals(p.getNsURI()))
				.collect(Collectors.toList());
	}
}
