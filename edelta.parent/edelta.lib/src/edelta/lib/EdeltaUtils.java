/**
 * 
 */
package edelta.lib;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
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

/**
 * Library functions to be reused in Edelta programs.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaUtils {

	private static EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;

	private EdeltaUtils() {
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
		var c = ecoreFactory.createEClass();
		c.setName(name);
		safeRunInitializer(initializer, c);
		return c;
	}

	public static EAttribute newEAttribute(String name, EDataType dataType) {
		return newEAttribute(name, dataType, null);
	}

	public static EAttribute newEAttribute(String name, EDataType dataType, Consumer<EAttribute> initializer) {
		var e = ecoreFactory.createEAttribute();
		e.setName(name);
		e.setEType(dataType);
		safeRunInitializer(initializer, e);
		return e;
	}

	public static EReference newEReference(String name, EClass referenceType) {
		return newEReference(name, referenceType, null);
	}

	public static EReference newEReference(String name, EClass referenceType, Consumer<EReference> initializer) {
		var e = ecoreFactory.createEReference();
		e.setName(name);
		e.setEType(referenceType);
		safeRunInitializer(initializer, e);
		return e;
	}

	public static EDataType newEDataType(String name, String instanceTypeName) {
		return newEDataType(name, instanceTypeName, null);
	}

	public static EDataType newEDataType(String name, String instanceTypeName, Consumer<EDataType> initializer) {
		var e = ecoreFactory.createEDataType();
		e.setName(name);
		e.setInstanceTypeName(instanceTypeName);
		safeRunInitializer(initializer, e);
		return e;
	}

	public static EEnum newEEnum(String name) {
		return newEEnum(name, null);
	}

	public static EEnum newEEnum(String name, Consumer<EEnum> initializer) {
		var e = ecoreFactory.createEEnum();
		e.setName(name);
		safeRunInitializer(initializer, e);
		return e;
	}

	public static EEnumLiteral newEEnumLiteral(String name) {
		return newEEnumLiteral(name, null);
	}

	public static EEnumLiteral newEEnumLiteral(String name, Consumer<EEnumLiteral> initializer) {
		var e = ecoreFactory.createEEnumLiteral();
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
		var builder = new StringBuilder();
		var current = e;
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

	public static void removeESuperType(EClass subClass, EClass superClass) {
		subClass.getESuperTypes().remove(superClass);
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
	 * @see EdeltaUtils#removeElement(ENamedElement)
	 */
	public static void removeAllElements(Collection<? extends EObject> elements) {
		elements.stream().forEach(EdeltaUtils::removeFutureDanglingReferences);
		EcoreUtil.removeAll(elements);
	}

	private static void removeFutureDanglingReferences(EObject element) {
		if (element instanceof EClassifier) {
			var classifier = (EClassifier) element;
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
			var reference = (EReference) element;
			if (reference.getEOpposite() != null)
				reference.getEOpposite().setEOpposite(null);
		}
		removeAllElements(element.eContents());
	}

	/**
	 * Returns a list of all the {@link EClass}es of the specified
	 * {@link EPackage} and of the packages in the same {@link Resource}
	 * and {@link ResourceSet}.
	 * 
	 * @param ePackage
	 * @return an empty list if the ePackage is null
	 * @see #packagesToInspect(EClassifier)
	 */
	public static List<EClass> allEClasses(EPackage ePackage) {
		return allEClassesStream(ePackage)
			.collect(Collectors.toList());
	}

	/**
	 * Returns a stream of all the {@link EClass}es of the specified
	 * {@link EPackage} and of the packages in the same {@link Resource}
	 * and {@link ResourceSet}.
	 * 
	 * @param ePackage
	 * @return an empty stream if the ePackage is null
	 * @see #packagesToInspect(EClassifier)
	 */
	public static Stream<EClass> allEClassesStream(EPackage ePackage) {
		if (ePackage == null)
			return Stream.empty();
		return filterByType(
			packagesToInspect(ePackage).stream()
					.flatMap(p -> p.getEClassifiers().stream()),
				EClass.class);
	}

	/**
	 * Returns a list of all the {@link EClass}es of the specified
	 * {@link EPackage} only.
	 * 
	 * @param ePackage
	 * @return an empty list if the ePackage is null
	 */
	public static List<EClass> getEClasses(EPackage ePackage) {
		return getEClassesStream(ePackage)
			.collect(Collectors.toList());
	}

	/**
	 * Returns a stream of all the {@link EClass}es of the specified
	 * {@link EPackage} only.
	 * 
	 * @param ePackage
	 * @return an empty stream if the ePackage is null
	 */
	public static Stream<EClass> getEClassesStream(EPackage ePackage) {
		if (ePackage == null)
			return Stream.empty();
		return filterByType(
				ePackage.getEClassifiers().stream(),
				EClass.class);
	}

	/**
	 * Returns a list of all the {@link EStructuralFeature}s of the specified
	 * {@link EPackage} and of the packages in the same {@link Resource}
	 * and {@link ResourceSet}.
	 * 
	 * @param ePackage
	 * @return an empty list if the ePackage is null
	 * @see #packagesToInspect(EClassifier)
	 */
	public static List<EStructuralFeature> allEStructuralFeatures(EPackage ePackage) {
		return allEStructuralFeaturesStream(ePackage)
				.collect(Collectors.toList());
	}

	/**
	 * Returns a stream of all the {@link EStructuralFeature}s of the specified
	 * {@link EPackage} and of the packages in the same {@link Resource}
	 * and {@link ResourceSet}.
	 * 
	 * @param ePackage
	 * @return an empty stream if the ePackage is null
	 * @see #packagesToInspect(EClassifier)
	 */
	public static Stream<EStructuralFeature> allEStructuralFeaturesStream(EPackage ePackage) {
		return allEClassesStream(ePackage)
				.flatMap(c -> c.getEStructuralFeatures().stream());
	}

	/**
	 * Returns a list of all the {@link EStructuralFeature}s of the specified
	 * {@link EPackage} only.
	 * 
	 * @param ePackage
	 * @return an empty list if the ePackage is null
	 */
	public static List<EStructuralFeature> getEStructuralFeatures(EPackage ePackage) {
		return getEStructuralFeaturesStream(ePackage)
				.collect(Collectors.toList());
	}

	/**
	 * Returns a stream of all the {@link EStructuralFeature}s of the specified
	 * {@link EPackage} only.
	 * 
	 * @param ePackage
	 * @return an empty stream if the ePackage is null
	 */
	public static Stream<EStructuralFeature> getEStructuralFeaturesStream(EPackage ePackage) {
		return getEClassesStream(ePackage)
				.flatMap(c -> c.getEStructuralFeatures().stream());
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
	 * {@link EPackage}s that can be inspected, for example, to search for
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
		return packagesToInspect(ePackage);
	}

	/**
	 * Given an {@link EPackage} it returns a {@link Collection} of
	 * {@link EPackage}s that can be inspected, for example, to search for
	 * references. It always returns a collection. If the package is
	 * not contained in a {@link Resource} then the collection contains only the
	 * package. Otherwise it collects all the packages in all resources in the
	 * {@link ResourceSet} (if there is one). The {@link EcorePackage} is never
	 * part of the returned collection.
	 * 
	 * @param ePackage
	 * @return
	 */
	public static Collection<EPackage> packagesToInspect(EPackage ePackage) {
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

	/**
	 * Returns the collection of the {@link EPackage}s used by the passed
	 * {@link EPackage}.
	 * 
	 * @param ePackage
	 * @return
	 */
	public static Collection<EPackage> usedPackages(EPackage ePackage) {
		var map = EcoreUtil.CrossReferencer.find(List.of(ePackage));
		// the keys are the EObjects that are used by elements of this package
		return filterByType(map.keySet().stream(), EClassifier.class) // only the used EClassifiers...
			.map(EClassifier::getEPackage) // ...to get their packages
			.filter(Objects::nonNull) // safety condition
			.filter(notEcore()) // skip the Ecore.ecore
			.filter(p -> !Objects.equals(ePackage, p)) // different from our package
			.collect(Collectors.toSet()); // just one occurrence of each used package
	}

	private static List<EPackage> filterEPackages(Stream<EObject> objectsStream) {
		return filterByType(objectsStream, EPackage.class)
				.filter(notEcore())
				.collect(Collectors.toList());
	}

	private static Predicate<? super EPackage> notEcore() {
		return p -> !EcorePackage.eNS_URI.equals(p.getNsURI());
	}

	private static <T, R> Stream<R> filterByType(Stream<T> stream, Class<R> desiredType) {
		return stream
				.filter(desiredType::isInstance)
				.map(desiredType::cast);
	}

	/**
	 * Returns the closest containing {@link EPackage}. If the given object is an
	 * {@link EPackage}, then the object itself will be returned. If no container
	 * object is of {@link EPackage}, then {@code null} will be returned.
	 * 
	 * @param ele
	 * @return
	 */
	public static EPackage getEContainingPackage(ENamedElement ele) {
		return getContainerOfType(ele, EPackage.class);
	}

	/**
	 * Returns the closest {@link EObject#eContainer() container object} of the
	 * requested type. If the given object is an instance of the requested type,
	 * then the object itself will be returned. If no container object is of the
	 * requested type, then {@code null} will be returned.
	 * 
	 * @param <T>
	 * @param ele
	 * @param type
	 * @return
	 */
	public static <T extends ENamedElement> T getContainerOfType(ENamedElement ele, Class<T> type) {
		for (EObject e = ele; e != null; e = e.eContainer())
			if (type.isInstance(e))
				return type.cast(e);

		return null;
	}
}
