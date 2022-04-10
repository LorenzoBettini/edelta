/**
 * 
 */
package edelta.lib;

import static edelta.lib.EdeltaUtils.getEObjectRepr;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

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
import org.eclipse.emf.ecore.util.EcoreUtil;

import edelta.lib.EdeltaModelMigrator.EObjectFunction;

/**
 * Standard library methods, for example, for adding, copying, moving
 * elements.
 * 
 * @author Lorennzo Bettini
 *
 */
public class EdeltaStandardLibrary extends EdeltaRuntime {

	private static EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;

	/**
	 * @param other
	 */
	public EdeltaStandardLibrary(EdeltaRuntime other) {
		super(other);
	}

	public EdeltaStandardLibrary(EdeltaModelManager modelManager) {
		super(modelManager);
	}

	public void addEStructuralFeature(EClass eClass, EStructuralFeature eStructuralFeature) {
		var eStructuralFeatures = eClass.getEStructuralFeatures();
		var existing = eClass.getEStructuralFeature(eStructuralFeature.getName());
		checkAlreadyExistingWithTheSameName(eClass, eStructuralFeature, existing);
		eStructuralFeatures.add(eStructuralFeature);
	}

	private void addEClassifier(EPackage ePackage, EClassifier eClassifier) {
		var eClassifiers = ePackage.getEClassifiers();
		var existing = ePackage.getEClassifier(eClassifier.getName());
		checkAlreadyExistingWithTheSameName(ePackage, eClassifier, existing);
		eClassifiers.add(eClassifier);
	}

	private <C extends ENamedElement, E extends ENamedElement> void
			checkAlreadyExistingWithTheSameName(C container, E toAdd, E existing) {
		if (existing != null) {
			var errorMessage = getEObjectRepr(container) +
					" already contains " +
					existing.eClass().getName() +
					" " +
					getEObjectRepr(existing);
			showError(toAdd, errorMessage);
			throw new IllegalArgumentException(errorMessage);
		}
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

	public EClass addNewEClassAsSibling(EClassifier sibling, String name) {
		return addNewEClassAsSibling(sibling, name, null);
	}

	public EClass addNewEClassAsSibling(EClassifier sibling, String name, Consumer<EClass> initializer) {
		return addNewEClass(sibling.getEPackage(), name, initializer);
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

	public EEnum addNewEEnumAsSibling(EClassifier sibling, String name) {
		return addNewEEnumAsSibling(sibling, name, null);
	}

	public EEnum addNewEEnumAsSibling(EClassifier sibling, String name, Consumer<EEnum> initializer) {
		return addNewEEnum(sibling.getEPackage(), name, initializer);
	}

	public void addEEnumLiteral(EEnum eEnum, EEnumLiteral eEnumLiteral) {
		var eLiterals = eEnum.getELiterals();
		var existing = eEnum.getEEnumLiteral(eEnumLiteral.getName());
		checkAlreadyExistingWithTheSameName(eEnum, eEnumLiteral, existing);
		eLiterals.add(eEnumLiteral);
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

	public EEnumLiteral addNewEEnumLiteralAsSibling(EEnumLiteral sibling, String name) {
		return addNewEEnumLiteralAsSibling(sibling, name, null);
	}

	public EEnumLiteral addNewEEnumLiteralAsSibling(EEnumLiteral sibling, String name, Consumer<EEnumLiteral> initializer) {
		return addNewEEnumLiteral(sibling.getEEnum(), name, initializer);
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

	public EDataType addNewEDataTypeAsSibling(EClassifier sibling, String name, String instanceTypeName) {
		return addNewEDataTypeAsSibling(sibling, name, instanceTypeName, null);
	}

	public EDataType addNewEDataTypeAsSibling(EClassifier sibling, String name, String instanceTypeName, Consumer<EDataType> initializer) {
		return addNewEDataType(sibling.getEPackage(), name, instanceTypeName, initializer);
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

	public EAttribute addNewEAttributeAsSibling(EStructuralFeature sibling, String name, EDataType dataType) {
		return addNewEAttributeAsSibling(sibling, name, dataType, null);
	}

	public EAttribute addNewEAttributeAsSibling(EStructuralFeature sibling, String name, EDataType dataType, Consumer<EAttribute> initializer) {
		return addNewEAttribute(sibling.getEContainingClass(), name, dataType, initializer);
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

	public EReference addNewEReferenceAsSibling(EStructuralFeature sibling, String name, EClass referenceType) {
		return addNewEReferenceAsSibling(sibling, name, referenceType, null);
	}

	public EReference addNewEReferenceAsSibling(EStructuralFeature sibling, String name, EClass referenceType, Consumer<EReference> initializer) {
		return addNewEReference(sibling.getEContainingClass(), name, referenceType, initializer);
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

	/**
	 * Copies the specified {@link EStructuralFeature} into the specified
	 * {@link EClass}, using {@link EcoreUtil#copy(EObject)}.
	 * 
	 * @param feature
	 * @param eClassDest
	 * @return the copied feature
	 * @see EcoreUtil#copy(EObject)
	 */
	public EStructuralFeature copyTo(EStructuralFeature feature, EClass eClassDest) {
		var copy = EcoreUtil.copy(feature);
		addEStructuralFeature(eClassDest, copy);
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
	public EStructuralFeature copyToAs(EStructuralFeature feature, EClass eClassDest, String name) {
		var copy = EcoreUtil.copy(feature);
		copy.setName(name);
		addEStructuralFeature(eClassDest, copy);
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
	public EStructuralFeature copyToAs(EStructuralFeature feature, EClass eClassDest, String name,
			EClassifier type) {
		var copy = EcoreUtil.copy(feature);
		copy.setName(name);
		copy.setEType(type);
		addEStructuralFeature(eClassDest, copy);
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
	public void copyAllTo(Collection<EStructuralFeature> features, EClass eClassDest) {
		var elements = EcoreUtil.copyAll(features);
		checkAlreadyExistingWithTheSameName(eClassDest, elements);
		eClassDest.getEStructuralFeatures().addAll(elements);
	}

	private void checkAlreadyExistingWithTheSameName(EClass eClassDest, Collection<EStructuralFeature> elements) {
		for (var element : elements) {
			var existing = eClassDest.getEStructuralFeature(element.getName());
			checkAlreadyExistingWithTheSameName(eClassDest, element, existing);
		}
	}

	/**
	 * Moves the specified {@link EStructuralFeature} into the specified
	 * {@link EClass}.
	 * 
	 * @param feature
	 * @param eClassDest
	 */
	public void moveTo(EStructuralFeature feature, EClass eClassDest) {
		addEStructuralFeature(eClassDest, feature);
	}

	/**
	 * Moves the specified {@link EStructuralFeature}s into the specified
	 * {@link EClass}.
	 * 
	 * @param features
	 * @param eClassDest
	 */
	public void moveAllTo(Collection<EStructuralFeature> features, EClass eClassDest) {
		checkAlreadyExistingWithTheSameName(eClassDest, features);
		eClassDest.getEStructuralFeatures().addAll(features);
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
	public EReference createOpposite(EReference reference, String name, EClass target) {
		return addNewEReference(target, name, reference.getEContainingClass(),
			newRef -> EdeltaUtils.makeBidirectional(newRef, reference)
		);
	}

	/**
	 * Changes the {@link EDataType} of the given {@link EAttribute}; concerning the model migration,
	 * it applies the specified singleValueTransformer. During the model migration
	 * the multiplicity of the attribute is taken care automatically, so the value transformer
	 * must only take care of transforming a single value.
	 * 
	 * @param attribute
	 * @param newType
	 * @param singleValueTransformer
	 */
	public void changeType(EAttribute attribute, EDataType newType, UnaryOperator<Object> singleValueTransformer) {
		attribute.setEType(newType);
		// and adjust model migration
		modelMigration(modelMigrator ->
			modelMigrator.transformAttributeValueRule(
				modelMigrator.isRelatedTo(attribute),
				modelMigrator
					.multiplicityAwareTranformer(attribute, singleValueTransformer)
			)
		);
	}

	/**
	 * Changes the {@link EClass} type of the given {@link EReference}; concerning
	 * the model migration, it applies the specified referredObjectTransformer.
	 * During the model migration the multiplicity of the reference is taken care
	 * automatically, so the referredObjectTransformer must only take care of
	 * returning a new single object to be referred, using the previously referred
	 * object somehow.
	 * 
	 * @param reference
	 * @param newType
	 * @param referredObjectTransformer
	 */
	public void changeType(EReference reference, EClass newType, EObjectFunction referredObjectTransformer) {
		changeType(reference, newType, referredObjectTransformer, null);
	}

	/**
	 * Changes the {@link EClass} type of the given {@link EReference}; concerning
	 * the model migration, it applies the specified referredObjectTransformer.
	 * During the model migration the multiplicity of the reference is taken care
	 * automatically, so the referredObjectTransformer must only take care of
	 * returning a new single object to be referred, using the previously referred
	 * object somehow.
	 * 
	 * @param reference
	 * @param newType
	 * @param referredObjectTransformer
	 * @param postCopy                  {@link Runnable} that will be executed after
	 *                                  the migration of the model, e.g., for
	 *                                  cleanup and stale objects removal (shared
	 *                                  non-containment references not used anymore
	 *                                  can be deleted in this runnable)
	 */
	public void changeType(EReference reference, EClass newType, EObjectFunction referredObjectTransformer,
			Runnable postCopy) {
		reference.setEType(newType);
		modelMigration(modelMigrator ->
			modelMigrator.copyRule(
				modelMigrator.isRelatedTo(reference),
				modelMigrator
					.multiplicityAwareCopy(reference, referredObjectTransformer),
				postCopy
			)
		);
	}

	/**
	 * Changes this feature to single (upper = 1); the model is migrated as well,
	 * taking the first element of the old list (the other elements in the old list
	 * are lost).
	 * 
	 * @param feature
	 */
	public void changeToSingle(EStructuralFeature feature) {
		feature.setUpperBound(1);
		migrationWithMultiplicityAwareCopy(feature);
	}

	/**
	 * Changes this feature to multiple (upper = -1); the model is migrated as well,
	 * creating an empty list or a list with a single element.
	 * 
	 * @param feature
	 */
	public void changeToMultiple(EStructuralFeature feature) {
		changeToMultiple(feature, -1);
	}

	/**
	 * Changes this feature to multiple with the given upperBound; the model is
	 * migrated as well, creating an empty list or a list with at most upperBound
	 * elements.
	 * 
	 * @param feature
	 * @param upperBound
	 */
	public void changeToMultiple(EStructuralFeature feature, int upperBound) {
		feature.setUpperBound(upperBound);
		migrationWithMultiplicityAwareCopy(feature);
	}

	private void migrationWithMultiplicityAwareCopy(EStructuralFeature feature) {
		modelMigration(modelMigrator ->
			modelMigrator.copyRule(
				modelMigrator.isRelatedTo(feature),
				modelMigrator.multiplicityAwareCopy(feature)
			)
		);
	}

	private <T extends ENamedElement> void safeRunInitializer(Consumer<T> initializer, T e) {
		if (initializer != null) {
			initializer.accept(e);
		}
	}

}
