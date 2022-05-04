package edelta.lib;

import static edelta.lib.EdeltaEcoreUtil.wrapAsCollection;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.util.EcoreUtil.Copier;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.xtext.xbase.lib.Functions.Function3;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure3;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * Handles the migration of EMF models while migrating Ecore models.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaModelMigrator {

	/**
	 * A custom {@link Copier} for EMF model (not Ecores, instances of Ecores)
	 * migration. Without changing the values of the objects of the model, it takes
	 * care of copying models of some Ecores as models of some other Ecores, which
	 * are the exact copy of the original Ecores.
	 * 
	 * @author Lorenzo Bettini
	 *
	 */
	public static class EdeltaModelCopier extends Copier {
		private static final long serialVersionUID = 1L;

		private transient BiMap<EObject, EObject> ecoreCopyMap;

		/**
		 * Creates a custom copier for migrating EMF models based on the passed map
		 * where the key is the original Ecore element and the value is the evolved
		 * Ecore element
		 * 
		 * @param ecoreCopyMap
		 */
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
			if (isStillThere(mapped))
				return mapped;
			return null;
		}

		/**
		 * Returns the Ecore element of the original Ecore corresponding to the passed
		 * Ecore element of the evolved Ecore.
		 * 
		 * @param <T>
		 * @param o
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public <T extends EObject> T getOriginal(T o) {
			return (T) ecoreCopyMap.inverse().get(o);
		}

		private boolean isStillThere(EObject target) {
			return target != null && target.eResource() != null;
		}

		public boolean isRelatedTo(ENamedElement origEcoreElement, ENamedElement evolvedEcoreElement) {
			return isStillThere(evolvedEcoreElement) &&
				wasRelatedTo(origEcoreElement, evolvedEcoreElement);
		}

		public boolean wasRelatedTo(ENamedElement origEcoreElement, ENamedElement evolvedEcoreElement) {
			return origEcoreElement == ecoreCopyMap.inverse().get(evolvedEcoreElement);
		}

		@Override
		public boolean equals(Object o) {
			return false;
		}

		@Override
		public int hashCode() {
			return super.hashCode();
		}
	}

	private EdeltaModelManager originalModelManager;
	private EdeltaModelManager evolvingModelManager;
	private Map<EObject, EObject> mapOfCopiedEcores;
	private EdeltaModelCopier modelCopier;

	@FunctionalInterface
	public static interface CopyProcedure
			extends Procedure3<EStructuralFeature, EObject, EObject> {
	}

	@FunctionalInterface
	public static interface AttributeTransformer
			extends Function3<EAttribute, EObject, Object, Object> {
	}

	@FunctionalInterface
	public static interface AttributeValueTransformer
			extends Function<Object, Object> {

	}

	@FunctionalInterface
	public static interface FeatureMigrator
			extends Function3<EStructuralFeature, EObject, EObject, EStructuralFeature> {
	}

	@FunctionalInterface
	public static interface EObjectFunction
			extends UnaryOperator<EObject> {
	}

	public EdeltaModelMigrator(EdeltaModelManager originalModelManager) {
		this.originalModelManager = originalModelManager;
		this.evolvingModelManager = new EdeltaModelManager();
		this.mapOfCopiedEcores = evolvingModelManager.copyEcores(originalModelManager);
		this.modelCopier = new EdeltaModelCopier(
				mapOfCopiedEcores);
	}

	public EdeltaModelManager getEvolvingModelManager() {
		return evolvingModelManager;
	}

	/**
	 * This simulates what the final model migration should do.
	 */
	public void copyModels() {
		copyModels(modelCopier, originalModelManager, evolvingModelManager);
	}

	private void copyModels(EdeltaModelCopier edeltaModelCopier, EdeltaModelManager from, EdeltaModelManager into) {
		var models = from.getModelResources();
		for (var resource : models) {
			var originalResource = (XMIResource) resource;
			var newResource = into.createModelResource(originalResource);
			var root = originalResource.getContents().get(0);
			var copy = edeltaModelCopier.copy(root);
			if (copy != null)
				newResource.getContents().add(copy);
		}
		edeltaModelCopier.copyReferences();
	}

	/**
	 * When the attribute predicate matches, copy the attribute value after applying
	 * the transformation implemented by the function (which takes the old value as
	 * argument and has to return the transformed new value).
	 * <p>
	 * Differently from
	 * {@link #transformAttributeValueRule(Predicate, AttributeTransformer)}, the
	 * old attribute and the old object are not passed to compute the transformed
	 * value.
	 * 
	 * @param predicate
	 * @param function
	 */
	public void transformAttributeValueRule(Predicate<EAttribute> predicate,
			AttributeValueTransformer function) {
		modelCopier = new EdeltaModelCopier(mapOfCopiedEcores) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void copyAttributeValue(EAttribute eAttribute,
					EObject eObject, Object value, Setting setting) {
				if (predicate.test(eAttribute))
					value = function.apply(value);
				super.copyAttributeValue(eAttribute, eObject, value, setting);
			}
		};
		copyModels(modelCopier, originalModelManager, evolvingModelManager);
		updateMigrationContext();
	}

	/**
	 * When the attribute predicate matches, copy the attribute value after applying
	 * the transformation implemented by the function (which takes as arguments, the
	 * old attribute, the old object and the old value and has to return the
	 * transformed new value).
	 * <p>
	 * Differently from
	 * {@link #transformAttributeValueRule(Predicate, AttributeValueTransformer)},
	 * the old attribute and the old object, besides the old value, can be used to
	 * compute the transformed value.
	 * 
	 * @param predicate
	 * @param function
	 */
	public void transformAttributeValueRule(Predicate<EAttribute> predicate,
			AttributeTransformer function) {
		modelCopier = new EdeltaModelCopier(mapOfCopiedEcores) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void copyAttributeValue(EAttribute eAttribute,
					EObject eObject, Object value, Setting setting) {
				if (predicate.test(eAttribute)) {
					value = function.apply(eAttribute, eObject, value);
				}
				super.copyAttributeValue(eAttribute, eObject, value, setting);
			}
		};
		copyModels(modelCopier, originalModelManager, evolvingModelManager);
		updateMigrationContext();
	}

	/**
	 * When the feature predicate matches, perform the copy of the feature value
	 * applying the passed {@link CopyProcedure}, which takes the old feature, the
	 * old object and the new object (where the value for the feature must be set).
	 * 
	 * @param predicate
	 * @param procedure
	 */
	public void copyRule(Predicate<EStructuralFeature> predicate,
			CopyProcedure procedure) {
		copyRule(predicate, procedure, null);
	}

	/**
	 * When the feature predicate matches, perform the copy of the feature value
	 * applying the passed {@link CopyProcedure}, which takes the old feature, the
	 * old object and the new object (where the value for the feature must be set).
	 * After the copy has been done for the feature, also executes the passed
	 * {@link Runnable}, which can be used to do some cleanup (for example, if you
	 * merge features into a single one, once the old object values have been used
	 * to create the new merged value, you can use this postCopy to remove the
	 * merged values).
	 * 
	 * @param predicate
	 * @param procedure
	 * @param postCopy
	 */
	public void copyRule(Predicate<EStructuralFeature> predicate,
			EdeltaModelMigrator.CopyProcedure procedure,
			Runnable postCopy) {
		modelCopier = new EdeltaModelCopier(mapOfCopiedEcores) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void copyContainment(EReference eReference,
					EObject eObject, EObject copyEObject) {
				applyCopyRuleOrElse(eReference, eObject, copyEObject,
					() -> super.copyContainment(eReference, eObject, copyEObject));
			}

			@Override
			protected void copyAttribute(EAttribute eAttribute,
					EObject eObject, EObject copyEObject) {
				applyCopyRuleOrElse(eAttribute, eObject, copyEObject,
					() -> super.copyAttribute(eAttribute, eObject, copyEObject));
			}

			@Override
			protected void copyReference(EReference eReference,
					EObject eObject, EObject copyEObject) {
				applyCopyRuleOrElse(eReference, eObject, copyEObject,
					() -> super.copyReference(eReference, eObject, copyEObject));
			}

			private void applyCopyRuleOrElse(EStructuralFeature feature,
					EObject eObject, EObject copyEObject, Runnable runnable) {
				if (predicate.test(feature))
					procedure.apply(feature, eObject, copyEObject);
				else
					runnable.run();
			}
		};
		copyModels(modelCopier, originalModelManager, evolvingModelManager);
		if (postCopy != null)
			postCopy.run();
		updateMigrationContext();
	}

	/**
	 * When the feature predicate matches, uses the function: the function takes as
	 * argument the old Ecore feature (the one that matched the predicate), the old
	 * object and the new object of the new Ecore, and has to return the feature (of
	 * the new Ecore) to use to set the new object feature value.
	 * <p>
	 * This is useful when in the evolved Ecore a feature is replaced with
	 * another feature with a few changed elements.
	 * 
	 * @param predicate
	 * @param function
	 */
	public void featureMigratorRule(Predicate<EStructuralFeature> predicate,
			FeatureMigrator function) {
		modelCopier = new EdeltaModelCopier(mapOfCopiedEcores) {
			private static final long serialVersionUID = 1L;

			@Override
			protected Setting getTarget(EStructuralFeature eStructuralFeature,
					EObject eObject, EObject copyEObject) {
				EStructuralFeature targetEStructuralFeature = null;
				if (predicate.test(eStructuralFeature))
					targetEStructuralFeature = function.apply(eStructuralFeature, eObject, copyEObject);
				return targetEStructuralFeature == null ?
					super.getTarget(eStructuralFeature, eObject, copyEObject)
					: ((InternalEObject) copyEObject).eSetting(targetEStructuralFeature);
			}
		};
		copyModels(modelCopier, originalModelManager, evolvingModelManager);
		updateMigrationContext();
	}

	/**
	 * Maps the old Ecore feature to the new Ecore feature.
	 * <p>
	 * This is useful when in the evolved Ecore a feature is replaced with
	 * another feature.
	 * 
	 * @param from
	 * @param to
	 */
	public void mapFeatureRule(EStructuralFeature from,
			EStructuralFeature to) {
		mapFeaturesRule(Collections.singletonList(from), to);
	}

	/**
	 * Maps the old Ecore features to the new Ecore feature.
	 * <p>
	 * This is useful when in the evolved Ecore a feature is replaced with
	 * another feature.
	 * 
	 * @param from
	 * @param to
	 */
	public void mapFeaturesRule(Collection<EStructuralFeature> from,
			EStructuralFeature to) {
		modelCopier = new EdeltaModelCopier(mapOfCopiedEcores) {
			private static final long serialVersionUID = 1L;

			@Override
			protected EStructuralFeature getTarget(EStructuralFeature eStructuralFeature) {
				if (wasRelatedToAtLeastOneOf(eStructuralFeature, from))
					return to;
				return super.getTarget(eStructuralFeature);
			}
		};
		copyModels(modelCopier, originalModelManager, evolvingModelManager);
		updateMigrationContext();
	}

	/**
	 * When the class predicate matches, uses the passed function to create an
	 * instance of the new object corresponding to the passed old object. In this
	 * case, the creation of the instance (and possibly the setting of its values)
	 * is completely up to you.
	 * 
	 * @param predicate
	 * @param function
	 */
	public void createInstanceRule(Predicate<EClass> predicate,
			EObjectFunction function) {
		modelCopier = new EdeltaModelCopier(mapOfCopiedEcores) {
			private static final long serialVersionUID = 1L;

			@Override
			protected EObject createCopy(EObject eObject) {
				if (predicate.test(eObject.eClass()))
					return function.apply(eObject);
				return super.createCopy(eObject);
			}
		};
		copyModels(modelCopier, originalModelManager, evolvingModelManager);
		updateMigrationContext();
	}

	private void updateMigrationContext() {
		// here we copy the Ecores and models that have been migrated
		var backup = new EdeltaModelManager();
		// first create a copy of the evolved ecores
		// map: orig -> copy
		// orig are the evolved ecores, copy are the backup Ecores
		var map = backup.copyEcores(evolvingModelManager);
		// now create a copy of the evolved models
		// we have to use our custom Copier because that will correctly
		// create copies of models referring to the backup ecores
		copyModels(new EdeltaModelCopier(map), evolvingModelManager, backup);
		// now we need an inverted map, because the backup is meant to become the
		// new originals, for the next model migrations
		mapOfCopiedEcores = HashBiMap.create(map).inverse();
		modelCopier = new EdeltaModelCopier(mapOfCopiedEcores);
		evolvingModelManager.clearModels();
		// the original model manager is updated with the copies we have just created
		originalModelManager = backup;

		// we must set the SettingDelegate to null
		// to force its recreation, so that it takes into
		// consideration possible changes of the feature, e.g., multiplicity
		EdeltaResourceUtils.getEPackagesStream(
			evolvingModelManager.getEcoreResources())
			.flatMap(EdeltaUtils::getEStructuralFeaturesStream)
			.forEach(f -> 
				((EStructuralFeature.Internal)f).setSettingDelegate(null));
	}

	/**
	 * Returns the migrated version of a model object; if
	 * it hasn't been migrated yet, it will migrate it.
	 * 
	 * @param o
	 * @return
	 */
	public EObject getMigrated(EObject o) {
		return modelCopier.copy(o);
	}

	/**
	 * Returns the migrated version of the model objects; if
	 * any of them hasn't been migrated yet, it will migrate them.
	 * 
	 * @param o
	 * @return
	 */
	public <T> Collection<T> getMigrated(Collection<? extends T> objects) {
		return modelCopier.copyAll(objects);
	}

	/**
	 * Returns the migrated version of a metamodel element,
	 * given an original metamodel element.
	 * 
	 * @param <T>
	 * @param o
	 * @return
	 */
	public <T extends ENamedElement> T getMigrated(T o) {
		return modelCopier.getMapped(o);
	}

	/**
	 * Returns the original version of a metamodel element,
	 * given a migrated metamodel element.
	 * 
	 * @param <T>
	 * @param o
	 * @return
	 */
	public <T extends ENamedElement> T getOriginal(T o) {
		return modelCopier.getOriginal(o);
	}

	public boolean isRelatedTo(ENamedElement origEcoreElement,
			ENamedElement evolvedEcoreElement) {
		return modelCopier.isRelatedTo(origEcoreElement, evolvedEcoreElement);
	}

	public <T extends ENamedElement> Predicate<T> isRelatedTo(T evolvedEcoreElement) {
		return origEcoreElement -> isRelatedTo(origEcoreElement, evolvedEcoreElement);
	}

	public boolean wasRelatedTo(ENamedElement origEcoreElement,
			ENamedElement evolvedEcoreElement) {
		return modelCopier.wasRelatedTo(origEcoreElement, evolvedEcoreElement);
	}

	public <T extends ENamedElement> Predicate<T> wasRelatedTo(T evolvedEcoreElement) {
		return origEcoreElement -> wasRelatedTo(origEcoreElement, evolvedEcoreElement);
	}

	public boolean wasRelatedToAtLeastOneOf(ENamedElement origEcoreElement,
			Collection<? extends ENamedElement> evolvedEcoreElements) {
		return evolvedEcoreElements.stream()
			.anyMatch(e -> wasRelatedTo(origEcoreElement, e));
	}

	public <T extends ENamedElement> Predicate<T> wasRelatedToAtLeastOneOf(
			Collection<? extends ENamedElement> evolvedEcoreElements) {
		return origEcoreElement -> wasRelatedToAtLeastOneOf(origEcoreElement, evolvedEcoreElements);
	}

	public EdeltaModelMigrator.CopyProcedure multiplicityAwareCopy(EStructuralFeature feature) {
		if (feature instanceof EReference) {
			// for reference we must first propagate the copy
			// especially in case of collections
			return (EStructuralFeature oldFeature, EObject oldObj, EObject newObj) -> 
				EdeltaEcoreUtil.setValueForFeature(
					newObj,
					feature,
					// use the upper bound of the destination feature, since it might
					// be different from the original one
					getMigrated(
						EdeltaEcoreUtil
							.wrapAsCollection(oldObj.eGet(oldFeature), feature.getUpperBound()))
				);
		}
		return (EStructuralFeature oldFeature, EObject oldObj, EObject newObj) -> {
			// if the multiplicity changes and the type of the attribute changes we might
			// end up with a list with a single default value.
			// if instead we check that the original value of the object for the feature
			// is set we avoid such a situation.
			if (oldObj.eIsSet(oldFeature))
				// if we come here the old feature was set
				EdeltaEcoreUtil.setValueForFeature(
					newObj,
					feature,
					// use the upper bound of the destination feature, since it might
					// be different from the original one
					EdeltaEcoreUtil
						.getValueForFeature(oldObj, oldFeature, feature.getUpperBound())
				);
		};
	}

	/**
	 * Returns an {@link AttributeTransformer} that automatically takes care of the
	 * multiplicity of the attribute and applies the passed transformer to transform
	 * the value or values.
	 * 
	 * @param attribute
	 * @param transformer
	 * @return
	 */
	public AttributeTransformer multiplicityAwareTranformer(EAttribute attribute,
			UnaryOperator<Object> transformer) {
		return (feature, oldObj, oldValue) ->
			// if we come here the old attribute was set
			EdeltaEcoreUtil.unwrapCollection(
				// use the upper bound of the destination attribute, since it might
				// be different from the original one
				EdeltaEcoreUtil.wrapAsCollection(oldValue, attribute.getUpperBound())
					.stream()
					.map(transformer)
					.collect(Collectors.toList()),
				attribute
			);
	}

	/**
	 * Returns a {@link CopyProcedure} that automatically takes care of the
	 * multiplicity of the reference and applies the passed transformer to transform
	 * the {@link EObject} or {@link EObject}s.
	 * 
	 * @param reference
	 * @param transformer
	 * @return
	 */
	public CopyProcedure multiplicityAwareCopy(EReference reference,
			EObjectFunction transformer) {
		return (oldFeature, oldObj, newObj) ->
			EdeltaEcoreUtil.setValueForFeature(
				newObj,
				reference,
				// for reference we must first propagate the copy
				// especially in case of collections
				getMigrated(
					wrapAsCollection(oldObj.eGet(oldFeature), reference.getUpperBound()))
					.stream()
					.map(EObject.class::cast)
					.map(transformer)
					.collect(Collectors.toList())
				// use the upper bound of the destination attribute, since it might
				// be different from the original one
			);
	}
}