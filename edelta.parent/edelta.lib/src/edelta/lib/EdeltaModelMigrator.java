package edelta.lib;

import java.util.Collection;
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
import org.eclipse.emf.ecore.resource.Resource;
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

	private String basedir;
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
			extends Function<EObject, EObject> {
	}

	public EdeltaModelMigrator(String basedir,
			EdeltaModelManager originalModelManager,
			EdeltaModelManager evolvingModelManager) {
		this.basedir = basedir;
		this.originalModelManager = originalModelManager;
		this.evolvingModelManager = evolvingModelManager;
		this.mapOfCopiedEcores = evolvingModelManager.copyEcores(originalModelManager, basedir);
		this.modelCopier = new EdeltaModelCopier(
				mapOfCopiedEcores);
	}

	/**
	 * This simulates what the final model migration should do.
	 * 
	 * IMPORTANT: the original Ecores and models must be in a subdirectory
	 * of the directory that stores the modified Ecores.
	 * 
	 * It is crucial to strip the original path and use the baseDir
	 * to create the new {@link Resource} URI, so that, upon saving,
	 * the schema location is computed correctly.
	 * 
	 * @param baseDir
	 */
	public void copyModels(String baseDir) {
		copyModels(modelCopier, baseDir, originalModelManager, evolvingModelManager);
	}

	private void copyModels(EdeltaModelCopier edeltaModelCopier, String baseDir,
			EdeltaModelManager from, EdeltaModelManager into) {
		var models = from.getModelResources();
		for (var resource : models) {
			var originalResource = (XMIResource) resource;
			var fileName = EdeltaResourceUtils.getFileName(originalResource);
			var newResource = into.createModelResource
				(baseDir + fileName, originalResource);
			var root = originalResource.getContents().get(0);
			var copy = edeltaModelCopier.copy(root);
			if (copy != null)
				newResource.getContents().add(copy);
		}
		edeltaModelCopier.copyReferences();
	}

	public void transformAttributeValueRule(Predicate<EAttribute> predicate,
			EdeltaModelMigrator.AttributeValueTransformer function) {
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
		copyModels(modelCopier, basedir,
				originalModelManager, evolvingModelManager);
		updateMigrationContext();
	}

	public void transformAttributeValueRule(Predicate<EAttribute> predicate,
			EdeltaModelMigrator.AttributeTransformer function) {
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
		copyModels(modelCopier, basedir,
				originalModelManager, evolvingModelManager);
		updateMigrationContext();
	}

	public void copyRule(Predicate<EStructuralFeature> predicate,
			EdeltaModelMigrator.CopyProcedure procedure) {
		copyRule(predicate, procedure, null);
	}

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
		copyModels(modelCopier, basedir,
				originalModelManager, evolvingModelManager);
		if (postCopy != null)
			postCopy.run();
		updateMigrationContext();
	}

	public void featureMigratorRule(Predicate<EStructuralFeature> predicate,
			EdeltaModelMigrator.FeatureMigrator function) {
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
		copyModels(modelCopier, basedir,
				originalModelManager, evolvingModelManager);
		updateMigrationContext();
	}

	public void createInstanceRule(Predicate<EClass> predicate,
			EdeltaModelMigrator.EObjectFunction function) {
		modelCopier = new EdeltaModelCopier(mapOfCopiedEcores) {
			private static final long serialVersionUID = 1L;

			@Override
			protected EObject createCopy(EObject eObject) {
				if (predicate.test(eObject.eClass()))
					return function.apply(eObject);
				return super.createCopy(eObject);
			}
		};
		copyModels(modelCopier, basedir,
				originalModelManager, evolvingModelManager);
		updateMigrationContext();
	}

	private void updateMigrationContext() {
		// here we copy the Ecores and models that have been migrated
		var backup = new EdeltaModelManager();
		// first create a copy of the evolved ecores
		// map: orig -> copy
		// orig are the evolved ecores, copy are the backup Ecores
		var map = backup.copyEcores(evolvingModelManager, basedir);
		// now create a copy of the evolved models
		// we have to use our custom Copier because that will correctly
		// create copies of models referring to the backup ecores
		copyModels(new EdeltaModelCopier(map), basedir, evolvingModelManager, backup);
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
		EdeltaResourceUtils.getEPackages(
			evolvingModelManager.getEcoreResources()).stream()
			.flatMap(p -> EdeltaUtils.allEClasses(p).stream())
			.flatMap(c -> c.getEStructuralFeatures().stream())
			.forEach(f -> ((EStructuralFeature.Internal)f).setSettingDelegate(null));
	}

	public EObject getMigrated(EObject o) {
		return modelCopier.copy(o);
	}

	public <T> Collection<T> getMigrated(Collection<? extends T> objects) {
		return modelCopier.copyAll(objects);
	}

	public <T extends EObject> T getOriginal(T o) {
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

	public EdeltaModelMigrator.AttributeTransformer multiplicityAwareTranformer(EAttribute attribute,
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

}