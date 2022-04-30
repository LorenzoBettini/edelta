package edelta.refactorings.lib;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaEcoreUtil;
import edelta.lib.EdeltaModelMigrator;
import edelta.lib.EdeltaRuntime;
import edelta.lib.EdeltaUtils;
import edelta.refactorings.lib.helper.EdeltaFeatureDifferenceFinder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.emf.common.util.EList;
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
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ListExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure2;
import org.eclipse.xtext.xbase.lib.StringExtensions;

@SuppressWarnings("all")
public class EdeltaRefactorings extends EdeltaDefaultRuntime {
  public EdeltaRefactorings(final EdeltaRuntime other) {
    super(other);
  }
  
  public EAttribute addMandatoryAttribute(final EClass eClass, final String attributeName, final EDataType dataType) {
    final Consumer<EAttribute> _function = (EAttribute it) -> {
      EdeltaUtils.makeSingleRequired(it);
    };
    return this.stdLib.addNewEAttribute(eClass, attributeName, dataType, _function);
  }
  
  public EReference addMandatoryReference(final EClass eClass, final String referenceName, final EClass type) {
    final Consumer<EReference> _function = (EReference it) -> {
      EdeltaUtils.makeSingleRequired(it);
    };
    return this.stdLib.addNewEReference(eClass, referenceName, type, _function);
  }
  
  /**
   * Merges the given attributes into a single new attribute in the containing class.
   * The attributes must be compatible (same containing class, same type, same cardinality, etc).
   * 
   * @param newAttributeName
   * @param attributes
   * @param valueMerger is used to merge the values of the original
   * features in the new model
   * @return the new attribute added to the containing class of the attributes
   */
  public EAttribute mergeAttributes(final String newAttributeName, final Collection<EAttribute> attributes, final Function<Collection<?>, Object> valueMerger) {
    final EAttribute firstFeature = IterableExtensions.<EAttribute>head(attributes);
    final EAttribute mergedFeature = this.<EAttribute>mergeFeatures(newAttributeName, attributes);
    final Consumer<EdeltaModelMigrator> _function = (EdeltaModelMigrator it) -> {
      final EdeltaModelMigrator.CopyProcedure _function_1 = (EStructuralFeature feature, EObject oldObj, EObject newObj) -> {
        final Function<EAttribute, EAttribute> _function_2 = (EAttribute a) -> {
          return it.<EAttribute>getOriginal(a);
        };
        Stream<EAttribute> originalFeatures = attributes.stream().<EAttribute>map(_function_2);
        final Function<EAttribute, Object> _function_3 = (EAttribute f) -> {
          return oldObj.eGet(f);
        };
        List<Object> oldValues = originalFeatures.<Object>map(_function_3).collect(Collectors.<Object>toList());
        Object merged = valueMerger.apply(oldValues);
        newObj.eSet(mergedFeature, merged);
      };
      it.copyRule(
        it.<EStructuralFeature>wasRelatedTo(firstFeature), _function_1);
    };
    this.modelMigration(_function);
    return mergedFeature;
  }
  
  /**
   * Merges the given references into a single new reference in the containing class.
   * The references must be compatible (same containing class, same type, same cardinality, etc).
   * 
   * @param newReferenceName
   * @param references
   * @param valueMerger if not null, it is used to merge the values of the original
   * features in the new model
   * @param postCopy executed after the model migrations
   * @return the new reference added to the containing class of the references
   */
  public EReference mergeReferences(final String newReferenceName, final Collection<EReference> references, final Function<Collection<EObject>, EObject> valueMerger, final Runnable postCopy) {
    final EReference firstFeature = IterableExtensions.<EReference>head(references);
    final EReference mergedFeature = this.<EReference>mergeFeatures(newReferenceName, references);
    final Consumer<EdeltaModelMigrator> _function = (EdeltaModelMigrator it) -> {
      final EdeltaModelMigrator.CopyProcedure _function_1 = (EStructuralFeature feature, EObject oldObj, EObject newObj) -> {
        final Function<EReference, EReference> _function_2 = (EReference a) -> {
          return it.<EReference>getOriginal(a);
        };
        Stream<EReference> originalFeatures = references.stream().<EReference>map(_function_2);
        final Function<EReference, EObject> _function_3 = (EReference f) -> {
          return EdeltaEcoreUtil.getValueAsEObject(oldObj, f);
        };
        List<EObject> oldValues = originalFeatures.<EObject>map(_function_3).collect(Collectors.<EObject>toList());
        EObject merged = valueMerger.apply(it.<EObject>getMigrated(oldValues));
        newObj.eSet(mergedFeature, merged);
      };
      it.copyRule(
        it.<EStructuralFeature>wasRelatedTo(firstFeature), _function_1, postCopy);
    };
    this.modelMigration(_function);
    return mergedFeature;
  }
  
  /**
   * Merges the given features into a single new feature in the containing class.
   * The features must be compatible (same containing class, same type, same cardinality, etc).
   * 
   * @param <T>
   * @param newFeatureName
   * @param features
   * @return the new feature added to the containing class of the features
   */
  public <T extends EStructuralFeature> T mergeFeatures(final String newFeatureName, final Collection<T> features) {
    this.checkNoDifferences(features, 
      new EdeltaFeatureDifferenceFinder().ignoringName(), 
      "The two features cannot be merged");
    final T feature = IterableExtensions.<T>head(features);
    final EClass owner = feature.getEContainingClass();
    final T copy = this.stdLib.<T>copyToAs(feature, owner, newFeatureName);
    EdeltaUtils.removeAllElements(features);
    return copy;
  }
  
  /**
   * Merges the given features into the single given existing feature in the containing class.
   * The features must be compatible (same containing class, same type, same cardinality, etc)
   * and their types must be subtypes of the specified feature.
   * 
   * @param feature the features will be merged into this feature
   * @param features
   */
  public EStructuralFeature mergeFeatures(final EStructuralFeature feature, final Collection<EStructuralFeature> features) {
    this.checkCompliant(feature, features);
    Iterable<EStructuralFeature> _plus = Iterables.<EStructuralFeature>concat(Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(feature)), features);
    this.checkNoDifferences(_plus, 
      new EdeltaFeatureDifferenceFinder().ignoringName().ignoringType(), 
      "The two features cannot be merged");
    EdeltaUtils.removeAllElements(features);
    return feature;
  }
  
  /**
   * Merges the given features into a single new feature, with the given type, in the containing class.
   * The features must be compatible (same containing class, same type, same cardinality, etc)
   * and their types must be subtypes of the specified type.
   * 
   * @param newFeatureName
   * @param type
   * @param features
   */
  public EStructuralFeature mergeFeatures(final String newFeatureName, final EClassifier type, final Collection<EStructuralFeature> features) {
    final EStructuralFeature feature = IterableExtensions.<EStructuralFeature>head(features);
    final EClass owner = feature.getEContainingClass();
    final EStructuralFeature copy = this.stdLib.<EStructuralFeature>copyToAs(feature, owner, newFeatureName, type);
    this.mergeFeatures(copy, features);
    return copy;
  }
  
  /**
   * Split the given attribute into several attributes with the same type
   * as the original one, using the specified names. The original attribute
   * will be removed. The passed valueSplitter is used to migrate the
   * original object into the corresponding split ones.
   * 
   * @param attribute
   * @param newNames
   * @param valueSplitter
   * @return the collection of features
   */
  public Collection<EAttribute> splitAttribute(final EAttribute attribute, final Collection<String> newNames, final Function<Object, Collection<?>> valueSplitter) {
    final Collection<EAttribute> splitAttributes = this.<EAttribute>splitFeature(attribute, newNames);
    final Consumer<EdeltaModelMigrator> _function = (EdeltaModelMigrator it) -> {
      final EdeltaModelMigrator.CopyProcedure _function_1 = (EStructuralFeature feature, EObject oldObj, EObject newObj) -> {
        Object oldValue = oldObj.eGet(feature);
        Iterator<?> splittedValues = valueSplitter.apply(oldValue).iterator();
        for (final EAttribute splitFeature : splitAttributes) {
          {
            boolean _hasNext = splittedValues.hasNext();
            boolean _not = (!_hasNext);
            if (_not) {
              return;
            }
            newObj.eSet(splitFeature, splittedValues.next());
          }
        }
      };
      it.copyRule(
        it.<EStructuralFeature>wasRelatedTo(attribute), _function_1);
    };
    this.modelMigration(_function);
    return splitAttributes;
  }
  
  /**
   * Split the given reference into several references with the same type
   * as the original one, using the specified names. The original reference
   * will be removed. The passed valueSplitter is used to migrate the
   * original object into the corresponding split ones.
   * 
   * @param reference
   * @param newNames
   * @param valueSplitter
   * @param postCopy executed after the model migrations
   * @return the collection of features
   */
  public Collection<EReference> splitReference(final EReference reference, final Collection<String> newNames, final Function<EObject, Collection<EObject>> valueSplitter, final Runnable postCopy) {
    final Collection<EReference> splitReferences = this.<EReference>splitFeature(reference, newNames);
    final Consumer<EdeltaModelMigrator> _function = (EdeltaModelMigrator it) -> {
      final EdeltaModelMigrator.CopyProcedure _function_1 = (EStructuralFeature feature, EObject oldObj, EObject newObj) -> {
        EObject oldValue = it.getMigrated(
          EdeltaEcoreUtil.getValueAsEObject(oldObj, feature));
        Iterator<EObject> splittedValues = valueSplitter.apply(oldValue).iterator();
        for (final EReference splitFeature : splitReferences) {
          {
            boolean _hasNext = splittedValues.hasNext();
            boolean _not = (!_hasNext);
            if (_not) {
              return;
            }
            newObj.eSet(splitFeature, splittedValues.next());
          }
        }
      };
      it.copyRule(
        it.<EStructuralFeature>wasRelatedTo(reference), _function_1, postCopy);
    };
    this.modelMigration(_function);
    return splitReferences;
  }
  
  /**
   * Split the given feature into several features with the same type
   * as the original one, using the specified names. The original feature
   * will be removed.
   * 
   * @param <T>
   * @param featureToSplit
   * @param newFeatureNames
   * @return the collection of features
   */
  public <T extends EStructuralFeature> Collection<T> splitFeature(final T featureToSplit, final Collection<String> newFeatureNames) {
    this.checkNotMany(featureToSplit, 
      "Cannot split \'many\' feature");
    this.checkNoBidirectionalReferences(Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(featureToSplit)), 
      "Cannot split a bidirectional reference");
    final EClass owner = featureToSplit.getEContainingClass();
    final Function<String, T> _function = (String newName) -> {
      return this.stdLib.<T>copyToAs(featureToSplit, owner, newName);
    };
    List<T> splitFeatures = newFeatureNames.stream().<T>map(_function).collect(Collectors.<T>toList());
    EdeltaUtils.removeElement(featureToSplit);
    return splitFeatures;
  }
  
  /**
   * Given an EAttribute, expected to have an EEnum type, creates a subclass of
   * the containing class for each value of the referred EEnum
   * (each subclass is given a name corresponding to the the EEnumLiteral,
   * all lowercase but the first letter, for example, given the literal
   * "LITERAL1", the subclass is given the name "Literal1").
   * The attribute will then be removed and so will the EEnum.
   * The original containing EClass is made abstract.
   * 
   * @param attr
   * @return the collection of created subclasses
   */
  public Collection<EClass> enumToSubclasses(final EAttribute attr) {
    final EDataType type = attr.getEAttributeType();
    if ((type instanceof EEnum)) {
      final EClass owner = attr.getEContainingClass();
      final Function1<EEnumLiteral, String> _function = (EEnumLiteral it) -> {
        return StringExtensions.toFirstUpper(it.toString().toLowerCase());
      };
      final EdeltaModelMigrator.EObjectFunction _function_1 = (EObject oldObj) -> {
        final String literalValue = EdeltaEcoreUtil.getValueFromFeatureName(oldObj, attr.getName()).toString();
        final EClass correspondingSubclass = EdeltaUtils.findSiblingByName(owner, StringExtensions.toFirstUpper(literalValue.toLowerCase()));
        return EdeltaEcoreUtil.createInstance(correspondingSubclass);
      };
      final Collection<EClass> createdSubclasses = this.introduceSubclasses(owner, 
        IterableExtensions.<String>toList(ListExtensions.<EEnumLiteral, String>map(((EEnum)type).getELiterals(), _function)), _function_1);
      EdeltaUtils.removeElement(type);
      return createdSubclasses;
    } else {
      String _eObjectRepr = EdeltaUtils.getEObjectRepr(type);
      String _plus = ("Not an EEnum: " + _eObjectRepr);
      this.showError(attr, _plus);
      return null;
    }
  }
  
  /**
   * Creates the classes with the given names as subclasses of the passed
   * superClass, which will then be made abstract; the objectMigrator is applied
   * for migrating objects that were originally instances of the superClass.
   * 
   * @param superClass
   * @param name
   * @param objectMigration
   */
  public Collection<EClass> introduceSubclasses(final EClass superClass, final Collection<String> names, final EdeltaModelMigrator.EObjectFunction objectMigrator) {
    EdeltaUtils.makeAbstract(superClass);
    final Function1<String, EClass> _function = (String name) -> {
      return this.stdLib.addNewSubclass(superClass, name);
    };
    final List<EClass> subclasses = IterableExtensions.<EClass>toList(IterableExtensions.<String, EClass>map(names, _function));
    final Consumer<EdeltaModelMigrator> _function_1 = (EdeltaModelMigrator it) -> {
      it.createInstanceRule(
        it.<EClass>isRelatedTo(superClass), objectMigrator);
    };
    this.modelMigration(_function_1);
    return subclasses;
  }
  
  /**
   * Given a collection of subclasses, which are expected to be direct subclasses of
   * an EClass, say superclass, generates an EEnum (in the superclass' package)
   * with the specified name, representing the inheritance relation,
   * with an EEnumLiteral for each subclass (the name is the name
   * of the subclass in uppercase); the subclasses are removed, and
   * an attributed is added to the superclass with the created EEnum as type
   * (the name is the name of the EEnum, first letter lowercase).
   * 
   * For example, given the name "BaseType" and the collection of classes
   * {"Derived1", "Derived2"} subclasses of the superclass "Base",
   * it creates the EEnum "BaseType" with literals "DERIVED1", "DERIVED2",
   * (the values will be incremental numbers starting from 0,
   * according to the order of the subclasses in the collection)
   * it adds to "Base" the EAttribute "baseType" of type "BaseType".
   * The EClasses "Derived1" and "Derived2" are removed from the package.
   * 
   * @param name the name for the created EEnum
   * @param subclasses
   * @return the created EAttribute
   */
  public EAttribute subclassesToEnum(final String name, final Collection<EClass> subclasses) {
    this.checkNoFeatures(subclasses);
    final EClass superclass = this.getSingleDirectSuperclass(subclasses);
    final Consumer<EEnum> _function = (EEnum it) -> {
      final Procedure2<EClass, Integer> _function_1 = (EClass subClass, Integer index) -> {
        final String enumLiteralName = subClass.getName().toUpperCase();
        EEnumLiteral _addNewEEnumLiteral = this.stdLib.addNewEEnumLiteral(it, enumLiteralName);
        final Procedure1<EEnumLiteral> _function_2 = (EEnumLiteral it_1) -> {
          it_1.setValue((index).intValue());
        };
        ObjectExtensions.<EEnumLiteral>operator_doubleArrow(_addNewEEnumLiteral, _function_2);
      };
      IterableExtensions.<EClass>forEach(subclasses, _function_1);
    };
    final EEnum enum_ = this.stdLib.addNewEEnumAsSibling(superclass, name, _function);
    final EAttribute attribute = this.stdLib.addNewEAttribute(superclass, this.fromTypeToFeatureName(enum_), enum_);
    EdeltaUtils.makeConcrete(superclass);
    EdeltaUtils.removeAllElements(subclasses);
    final Consumer<EdeltaModelMigrator> _function_1 = (EdeltaModelMigrator it) -> {
      final EdeltaModelMigrator.EObjectFunction _function_2 = (EObject oldObj) -> {
        final EEnumLiteral enumLiteralName = enum_.getEEnumLiteral(
          oldObj.eClass().getName().toUpperCase());
        final Consumer<EObject> _function_3 = (EObject newObj) -> {
          newObj.eSet(attribute, enumLiteralName);
        };
        return EdeltaEcoreUtil.createInstance(superclass, _function_3);
      };
      it.createInstanceRule(
        it.<EClass>wasRelatedToAtLeastOneOf(subclasses), _function_2);
    };
    this.modelMigration(_function_1);
    return attribute;
  }
  
  /**
   * Extracts the specified features into a new class with the given name.
   * The features must belong to the same class.
   * In the containing class a containment required reference to
   * the extracted class will be created (its name will be the name
   * of the extracted class with the first letter lowercase).
   * 
   * @param name the name for the extracted class
   * @param features the features to extract
   * @return the added EReference to the extracted metaclass
   */
  public EReference extractClass(final String name, final Collection<EStructuralFeature> features) {
    boolean _isEmpty = features.isEmpty();
    if (_isEmpty) {
      return null;
    }
    this.checkNoBidirectionalReferences(features, 
      "Cannot extract bidirectional references");
    final EClass owner = this.findSingleOwner(features);
    final EClass extracted = this.stdLib.addNewEClassAsSibling(owner, name);
    EReference _addMandatoryReference = this.addMandatoryReference(owner, StringExtensions.toFirstLower(name), extracted);
    final Procedure1<EReference> _function = (EReference it) -> {
      this.makeContainmentBidirectional(it);
    };
    final EReference reference = ObjectExtensions.<EReference>operator_doubleArrow(_addMandatoryReference, _function);
    this.stdLib.moveAllTo(features, extracted);
    return reference;
  }
  
  /**
   * Inlines the features of the specified class into the single class
   * that has a containment reference to the specified class.
   * The specified class will then be removed.
   * 
   * @param cl
   * @return the features of the original class
   */
  public List<EStructuralFeature> inlineClass(final EClass cl) {
    return this.inlineClass(cl, "");
  }
  
  /**
   * Inlines the features of the specified class into the single class
   * that has a containment reference to the specified class.
   * The specified class will then be removed.
   * 
   * @param cl
   * @param prefix the prefix for the names of the inlined features
   * @return the features of the original class
   */
  public List<EStructuralFeature> inlineClass(final EClass cl, final String prefix) {
    final EReference ref = this.findSingleContainmentReferenceToThisClass(cl);
    this.checkNotMany(ref, 
      "Cannot inline in a \'many\' reference");
    final Function1<EStructuralFeature, Boolean> _function = (EStructuralFeature it) -> {
      EReference _eOpposite = ref.getEOpposite();
      return Boolean.valueOf((it != _eOpposite));
    };
    final List<EStructuralFeature> featuresToInline = IterableExtensions.<EStructuralFeature>toList(IterableExtensions.<EStructuralFeature>filter(cl.getEStructuralFeatures(), _function));
    final Consumer<EStructuralFeature> _function_1 = (EStructuralFeature it) -> {
      String _name = it.getName();
      String _plus = (prefix + _name);
      it.setName(_plus);
    };
    featuresToInline.forEach(_function_1);
    this.stdLib.moveAllTo(featuresToInline, ref.getEContainingClass());
    EdeltaUtils.removeElement(cl);
    return featuresToInline;
  }
  
  /**
   * Makes the EReference, which is assumed to be already part of an EClass,
   * a single required containment reference, adds to the referred
   * type, which is assumed to be set, an opposite required single reference.
   * @param reference
   */
  public EReference makeContainmentBidirectional(final EReference reference) {
    EReference _xblockexpression = null;
    {
      EdeltaUtils.makeContainment(reference);
      final EClass owner = reference.getEContainingClass();
      final EClass referredType = reference.getEReferenceType();
      EReference _addMandatoryReference = this.addMandatoryReference(referredType, this.fromTypeToFeatureName(owner), owner);
      final Procedure1<EReference> _function = (EReference it) -> {
        EdeltaUtils.makeBidirectional(it, reference);
      };
      _xblockexpression = ObjectExtensions.<EReference>operator_doubleArrow(_addMandatoryReference, _function);
    }
    return _xblockexpression;
  }
  
  /**
   * Replaces an EReference with an EClass (with the given name, the same package
   * as the package of the reference's containing class),
   * updating possible opposite reference,
   * so that a relation can be extended with additional features.
   * The original reference will be made a containment reference,
   * (its other properties will not be changed)
   * to the added EClass (and made bidirectional).
   * 
   * For example, given
   * <pre>
   *    b2    b1
   * A <-------> C
   * </pre>
   * 
   * (where the opposite "b2" might not be present)
   * if we pass "b1" and the name "B", then the result will be
   * 
   * <pre>
   *    a     b1    b2    c
   * A <-------> B <------> C
   * </pre>
   * 
   * where "b1" will be a containment reference.
   * Note the names inferred for the new additional opposite references.
   * 
   * @param name the name for the extracted class
   * @param reference the reference to turn into a reference to the extracted class
   * @return the extracted class
   */
  public EClass referenceToClass(final String name, final EReference reference) {
    this.checkNotContainment(reference, 
      "Cannot apply referenceToClass on containment reference");
    final EPackage ePackage = reference.getEContainingClass().getEPackage();
    final EClass extracted = this.stdLib.addNewEClass(ePackage, name);
    final EReference extractedRef = this.addMandatoryReference(extracted, 
      this.fromTypeToFeatureName(reference.getEType()), reference.getEReferenceType());
    final EReference eOpposite = reference.getEOpposite();
    if ((eOpposite != null)) {
      EdeltaUtils.makeBidirectional(eOpposite, extractedRef);
    }
    reference.setEType(extracted);
    this.makeContainmentBidirectional(reference);
    final Consumer<EdeltaModelMigrator> _function = (EdeltaModelMigrator it) -> {
      final Predicate<EStructuralFeature> _function_1 = (EStructuralFeature feature) -> {
        return (it.isRelatedTo(feature, reference) || 
          it.isRelatedTo(feature, eOpposite));
      };
      final EdeltaModelMigrator.CopyProcedure _function_2 = (EStructuralFeature feature, EObject oldObj, EObject newObj) -> {
        boolean _isRelatedTo = it.isRelatedTo(feature, eOpposite);
        if (_isRelatedTo) {
          return;
        }
        Collection<Object> oldValueOrValues = EdeltaEcoreUtil.getValueForFeature(oldObj, feature, 
          reference.getUpperBound());
        final Function<Object, EObject> _function_3 = (Object oldValue) -> {
          final EObject copy = it.getMigrated(((EObject) oldValue));
          final Consumer<EObject> _function_4 = (EObject o) -> {
            o.eSet(extractedRef, copy);
          };
          return EdeltaEcoreUtil.createInstance(extracted, _function_4);
        };
        List<EObject> copies = oldValueOrValues.stream().<EObject>map(_function_3).collect(Collectors.<EObject>toList());
        EdeltaEcoreUtil.setValueForFeature(newObj, reference, copies);
      };
      it.copyRule(_function_1, _function_2);
    };
    this.modelMigration(_function);
    return extracted;
  }
  
  /**
   * Given an EClass, which is meant to represent a relation,
   * removes such a class, transforming the relation into an EReference.
   * 
   * For example, given
   * <pre>
   *    a     b1    b2    c
   * A <-------> B <------> C
   * </pre>
   * 
   * (where the opposites "a" and "b2" might not be present)
   * if we pass "B", then the result will be
   * <pre>
   *    b2    b1
   * A <-------> C
   * </pre>
   * 
   * @param cl
   * @return the EReference that now represents the relation, that is,
   * the EReference originally of type cl ("b1" above)
   */
  public EReference classToReference(final EClass cl) {
    final EReference reference = this.findSingleContainmentAmongReferencesToThisClass(cl);
    final EClass owner = reference.getEContainingClass();
    final EReference referenceToTarget = this.findSingleReferenceNotOfType(cl, owner);
    reference.setEType(referenceToTarget.getEType());
    EdeltaUtils.dropContainment(reference);
    final EReference opposite = referenceToTarget.getEOpposite();
    if ((opposite != null)) {
      EdeltaUtils.makeBidirectional(reference, opposite);
    }
    EdeltaUtils.removeElement(cl);
    return reference;
  }
  
  /**
   * Given a non empty list of {@link EStructuralFeature}, which are known to
   * appear in several classes as duplicates, extracts a new common superclass,
   * with the duplicate feature,
   * adds the extracted class as the superclass of the classes with the duplicate
   * feature and removes the duplicate feature from such each class.
   * 
   * The name of the extracted class is the name of the feature, with the first
   * letter capitalized and the "Element" suffix (example, if the feature is
   * "name" the extracted class will be called "NameElement").
   * An additional number can be
   * added as a suffix to avoid name clashes with existing classes.
   * 
   * @param duplicates
   */
  public EClass extractSuperclass(final List<EStructuralFeature> duplicates) {
    final EStructuralFeature feature = IterableExtensions.<EStructuralFeature>head(duplicates);
    String _firstUpper = StringExtensions.toFirstUpper(feature.getName());
    String _plus = (_firstUpper + "Element");
    final String superClassName = this.ensureEClassifierNameIsUnique(feature, _plus);
    return this.extractSuperclass(superClassName, duplicates);
  }
  
  /**
   * Given a non empty list of {@link EStructuralFeature}, which are known to
   * appear in several classes as duplicates, extracts a new common superclass,
   * with the given name, with the duplicate feature,
   * adds the extracted class as the superclass of the classes with the duplicate
   * feature and removes the duplicate feature from such each class.
   * 
   * @param name
   * @param duplicates
   */
  public EClass extractSuperclass(final String name, final List<EStructuralFeature> duplicates) {
    final EStructuralFeature feature = IterableExtensions.<EStructuralFeature>head(duplicates);
    final Consumer<EClass> _function = (EClass it) -> {
      EdeltaUtils.makeAbstract(it);
      final Function1<EStructuralFeature, EClass> _function_1 = (EStructuralFeature it_1) -> {
        return it_1.getEContainingClass();
      };
      final Consumer<EClass> _function_2 = (EClass c) -> {
        this.stdLib.addESuperType(c, it);
      };
      ListExtensions.<EStructuralFeature, EClass>map(duplicates, _function_1).forEach(_function_2);
      this.pullUpFeatures(it, duplicates);
    };
    return this.stdLib.addNewEClassAsSibling(feature.getEContainingClass(), name, _function);
  }
  
  /**
   * Given a non empty list of {@link EStructuralFeature}, which are known to
   * appear in several subclasses as duplicates, pulls them up in
   * the given common superclass
   * (and removes the duplicate feature from each subclass).
   * 
   * @param dest
   * @param duplicates
   */
  public EStructuralFeature pullUpFeatures(final EClass dest, final Collection<EStructuralFeature> duplicates) {
    this.checkNoDifferences(duplicates, 
      new EdeltaFeatureDifferenceFinder().ignoringContainingClass(), 
      "The two features are not equal");
    final Function1<EStructuralFeature, EClass> _function = (EStructuralFeature it) -> {
      return it.getEContainingClass();
    };
    this.checkAllDirectSubclasses(dest, IterableExtensions.<EClass>toList(IterableExtensions.<EStructuralFeature, EClass>map(duplicates, _function)));
    final EStructuralFeature pulledUp = this.stdLib.<EStructuralFeature>copyTo(IterableExtensions.<EStructuralFeature>head(duplicates), dest);
    EdeltaUtils.removeAllElements(duplicates);
    final Consumer<EdeltaModelMigrator> _function_1 = (EdeltaModelMigrator it) -> {
      it.mapFeaturesRule(duplicates, pulledUp);
    };
    this.modelMigration(_function_1);
    return pulledUp;
  }
  
  /**
   * Given a feature and a non empty list of {@link EClass}, which are known to
   * be direct subclasses of the containing class of the feature, pushes the feature down in
   * the given common subclasses
   * (and removes the feature from the original containing class).
   * 
   * @param featureToPush
   * @param subClasses
   */
  public Collection<EStructuralFeature> pushDownFeature(final EStructuralFeature featureToPush, final List<EClass> subClasses) {
    this.checkAllDirectSubclasses(featureToPush.getEContainingClass(), subClasses);
    final HashMap<EClass, EStructuralFeature> pushedDownFeatures = new HashMap<EClass, EStructuralFeature>();
    for (final EClass subClass : subClasses) {
      {
        EStructuralFeature pushedDown = EcoreUtil.<EStructuralFeature>copy(featureToPush);
        pushedDownFeatures.put(subClass, pushedDown);
        subClass.getEStructuralFeatures().add(0, pushedDown);
      }
    }
    EdeltaUtils.removeElement(featureToPush);
    final Consumer<EdeltaModelMigrator> _function = (EdeltaModelMigrator it) -> {
      final EdeltaModelMigrator.FeatureMigrator _function_1 = (EStructuralFeature feature, EObject oldObj, EObject newObj) -> {
        return pushedDownFeatures.get(newObj.eClass());
      };
      it.featureMigratorRule(
        it.<EStructuralFeature>wasRelatedTo(featureToPush), _function_1);
    };
    this.modelMigration(_function);
    return pushedDownFeatures.values();
  }
  
  /**
   * Ensures that the proposed classifier name is unique within the containing package of
   * the passed context; if not, it appends an incremental index until the name
   * is actually unique
   */
  public String ensureEClassifierNameIsUnique(final ENamedElement context, final String proposedName) {
    String className = proposedName;
    EPackage ePackage = EdeltaUtils.getEContainingPackage(context);
    final Function1<EClassifier, String> _function = (EClassifier it) -> {
      return it.getName();
    };
    final List<String> currentEClassifiersNames = IterableExtensions.<String>sort(ListExtensions.<EClassifier, String>map(ePackage.getEClassifiers(), _function));
    int counter = 1;
    while (currentEClassifiersNames.contains(className)) {
      String _className = className;
      int _plusPlus = counter++;
      className = (_className + Integer.valueOf(_plusPlus));
    }
    return className;
  }
  
  public String fromTypeToFeatureName(final EClassifier type) {
    return StringExtensions.toFirstLower(type.getName());
  }
  
  /**
   * Makes sure that this is not a containment reference,
   * otherwise it shows an error message
   * and throws an IllegalArgumentException.
   * 
   * @param reference the reference that must not be a containment reference
   * @param errorMessage the message to show in case the reference
   * is a containment reference
   */
  public void checkNotContainment(final EReference reference, final String errorMessage) {
    boolean _isContainment = reference.isContainment();
    if (_isContainment) {
      String _eObjectRepr = EdeltaUtils.getEObjectRepr(reference);
      final String message = ((errorMessage + ": ") + _eObjectRepr);
      this.showError(reference, message);
      throw new IllegalArgumentException(message);
    }
  }
  
  /**
   * Makes sure that this is not a multi element (upperBound > 1),
   * otherwise it shows an error message
   * and throws an IllegalArgumentException.
   * 
   * @param element the element that must not be multi
   * @param errorMessage the message to show in case the check fails
   */
  public void checkNotMany(final ETypedElement element, final String errorMessage) {
    boolean _isMany = element.isMany();
    if (_isMany) {
      String _eObjectRepr = EdeltaUtils.getEObjectRepr(element);
      final String message = ((errorMessage + ": ") + _eObjectRepr);
      this.showError(element, message);
      throw new IllegalArgumentException(message);
    }
  }
  
  /**
   * Makes sure that the passed collection does not have EReferences
   * with an EOpposite. Otherwise shows an error (using
   * also the passed errorMessage) with the details of the bidirectional references and
   * throws an IllegalArgumentException
   */
  public void checkNoBidirectionalReferences(final Collection<EStructuralFeature> features, final String errorMessage) {
    final Function1<EReference, Boolean> _function = (EReference it) -> {
      EReference _eOpposite = it.getEOpposite();
      return Boolean.valueOf((_eOpposite != null));
    };
    final Iterable<EReference> bidirectionalReferences = IterableExtensions.<EReference>filter(Iterables.<EReference>filter(features, EReference.class), _function);
    boolean _isEmpty = IterableExtensions.isEmpty(bidirectionalReferences);
    boolean _not = (!_isEmpty);
    if (_not) {
      final Function1<EReference, String> _function_1 = (EReference it) -> {
        String _eObjectRepr = EdeltaUtils.getEObjectRepr(it);
        return ("  " + _eObjectRepr);
      };
      String _join = IterableExtensions.join(IterableExtensions.<EReference, String>map(bidirectionalReferences, _function_1), "\n");
      final String message = ((errorMessage + ":\n") + _join);
      this.showError(IterableExtensions.<EReference>head(bidirectionalReferences), message);
      throw new IllegalArgumentException(message);
    }
  }
  
  /**
   * Makes sure that there are no differences in the passed features,
   * using the specified differenceFinder, otherwise it shows an error message
   * with the details of the differences and throws an IllegalArgumentException.
   * 
   * @param features
   * @param differenceFinder
   * @param errorMessage
   * @return true if there are no differences
   */
  public void checkNoDifferences(final Iterable<? extends EStructuralFeature> features, final EdeltaFeatureDifferenceFinder differenceFinder, final String errorMessage) {
    final EStructuralFeature feature = IterableExtensions.head(features);
    final Function1<EStructuralFeature, Boolean> _function = (EStructuralFeature it) -> {
      return Boolean.valueOf(((feature != it) && (!differenceFinder.equals(feature, it))));
    };
    final EStructuralFeature different = IterableExtensions.findFirst(features, _function);
    if ((different != null)) {
      String _differenceDetails = differenceFinder.getDifferenceDetails();
      final String message = ((errorMessage + ":\n") + _differenceDetails);
      this.showError(different, message);
      throw new IllegalArgumentException(message);
    }
  }
  
  /**
   * Makes sure that all the passed classes are direct subclasses of
   * the passed class.
   * 
   * @param superClass
   * @param classes
   */
  public void checkAllDirectSubclasses(final EClass superClass, final Collection<EClass> classes) {
    final Function1<EClass, Boolean> _function = (EClass it) -> {
      boolean _contains = it.getESuperTypes().contains(superClass);
      return Boolean.valueOf((!_contains));
    };
    final Iterable<EClass> nonDirectSubclasses = IterableExtensions.<EClass>filter(classes, _function);
    boolean _isEmpty = IterableExtensions.isEmpty(nonDirectSubclasses);
    boolean _not = (!_isEmpty);
    if (_not) {
      final Consumer<EClass> _function_1 = (EClass it) -> {
        String _eObjectRepr = EdeltaUtils.getEObjectRepr(superClass);
        String _plus = ("Not a direct subclass of: " + _eObjectRepr);
        this.showError(it, _plus);
      };
      nonDirectSubclasses.forEach(_function_1);
      throw new IllegalArgumentException("Not all direct subclasses");
    }
  }
  
  /**
   * Makes sure that the features have types that are subtypes of the
   * specified feature, if not, shows
   * error information and throws an IllegalArgumentException.
   * 
   * @param feature
   * @param features
   */
  public void checkCompliant(final EStructuralFeature feature, final Collection<? extends EStructuralFeature> features) {
    Predicate<EStructuralFeature> _xifexpression = null;
    if ((feature instanceof EReference)) {
      final Predicate<EStructuralFeature> _function = (EStructuralFeature other) -> {
        boolean _xifexpression_1 = false;
        if ((other instanceof EReference)) {
          _xifexpression_1 = ((EReference)feature).getEReferenceType().isSuperTypeOf(((EReference)other).getEReferenceType());
        } else {
          _xifexpression_1 = false;
        }
        return _xifexpression_1;
      };
      _xifexpression = _function;
    } else {
      final Predicate<EStructuralFeature> _function_1 = (EStructuralFeature other) -> {
        EClassifier _eType = feature.getEType();
        EClassifier _eType_1 = other.getEType();
        return (_eType == _eType_1);
      };
      _xifexpression = _function_1;
    }
    final Predicate<EStructuralFeature> compliance = _xifexpression;
    final Function1<EStructuralFeature, Boolean> _function_2 = (EStructuralFeature it) -> {
      boolean _test = compliance.test(it);
      return Boolean.valueOf((!_test));
    };
    final Iterable<? extends EStructuralFeature> nonCompliant = IterableExtensions.filter(features, _function_2);
    boolean _isEmpty = IterableExtensions.isEmpty(nonCompliant);
    boolean _not = (!_isEmpty);
    if (_not) {
      String _eObjectRepr = EdeltaUtils.getEObjectRepr(feature.getEType());
      String _plus = ("features not compliant with type " + _eObjectRepr);
      String _plus_1 = (_plus + ":\n");
      final Function1<EStructuralFeature, String> _function_3 = (EStructuralFeature it) -> {
        String _eObjectRepr_1 = EdeltaUtils.getEObjectRepr(it);
        String _plus_2 = ("  " + _eObjectRepr_1);
        String _plus_3 = (_plus_2 + ": ");
        String _eObjectRepr_2 = EdeltaUtils.getEObjectRepr(it.getEType());
        return (_plus_3 + _eObjectRepr_2);
      };
      String _join = IterableExtensions.join(IterableExtensions.map(nonCompliant, _function_3), "\n");
      final String message = (_plus_1 + _join);
      this.showError(feature, message);
      throw new IllegalArgumentException(message);
    }
  }
  
  /**
   * Makes sure the passed EClasses have no features, if not, shows
   * error information and throws an IllegalArgumentException.
   * 
   * @param classes
   */
  public void checkNoFeatures(final Collection<EClass> classes) {
    final Function1<EClass, Boolean> _function = (EClass c) -> {
      final EList<EStructuralFeature> features = c.getEStructuralFeatures();
      final boolean empty = features.isEmpty();
      if ((!empty)) {
        String _eObjectRepr = EdeltaUtils.getEObjectRepr(c);
        String _plus = ("Not an empty class: " + _eObjectRepr);
        String _plus_1 = (_plus + ":\n");
        final Function1<EStructuralFeature, String> _function_1 = (EStructuralFeature it) -> {
          String _eObjectRepr_1 = EdeltaUtils.getEObjectRepr(it);
          return ("  " + _eObjectRepr_1);
        };
        String _join = IterableExtensions.join(ListExtensions.<EStructuralFeature, String>map(features, _function_1), "\n");
        String _plus_2 = (_plus_1 + _join);
        this.showError(c, _plus_2);
      }
      return Boolean.valueOf((!empty));
    };
    final List<EClass> classesWithFeatures = IterableExtensions.<EClass>toList(IterableExtensions.<EClass>filter(classes, _function));
    boolean _isEmpty = classesWithFeatures.isEmpty();
    boolean _not = (!_isEmpty);
    if (_not) {
      throw new IllegalArgumentException("Classes not empty");
    }
  }
  
  /**
   * Finds, among all references to the given EClass, the single containment reference in the
   * EClass' package's resource set, performing validation (that is,
   * no reference is found, or more than one containment reference is found) checks and in case
   * show errors and throws an IllegalArgumentException.
   * 
   * Note that several references to the class are allowed: the important thing
   * is that exactly one is a containment reference.
   * 
   * @param cl
   */
  public EReference findSingleContainmentAmongReferencesToThisClass(final EClass cl) {
    final Iterable<EReference> references = this.findReferencesToThisClass(cl);
    final Function1<EReference, Boolean> _function = (EReference it) -> {
      return Boolean.valueOf(it.isContainment());
    };
    int _size = IterableExtensions.size(IterableExtensions.<EReference>filter(references, _function));
    boolean _greaterThan = (_size > 1);
    if (_greaterThan) {
      final Function1<EReference, String> _function_1 = (EReference it) -> {
        String _eObjectRepr = EdeltaUtils.getEObjectRepr(it);
        return ("  " + _eObjectRepr);
      };
      String _join = IterableExtensions.join(IterableExtensions.<EReference, String>map(references, _function_1), "\n");
      final String message = ("The EClass is referred by more than one container:\n" + _join);
      this.showError(cl, message);
      throw new IllegalArgumentException(message);
    }
    return IterableExtensions.<EReference>head(references);
  }
  
  /**
   * Finds all the EReferences to the given EClass in the
   * EClass' package's resource set. If no such references are
   * found it throws an IllegalArgumentException.
   * 
   * @param cl
   */
  public Iterable<EReference> findReferencesToThisClass(final EClass cl) {
    final Iterable<EReference> references = this.allReferencesToThisClass(cl);
    boolean _isEmpty = IterableExtensions.isEmpty(references);
    if (_isEmpty) {
      String _eObjectRepr = EdeltaUtils.getEObjectRepr(cl);
      final String message = ("The EClass is not referred: " + _eObjectRepr);
      this.showError(cl, message);
      throw new IllegalArgumentException(message);
    }
    return references;
  }
  
  /**
   * Returns all the EReferences to the given EClass in the
   * EClass' package's resource set.
   * 
   * @param cl
   */
  public Iterable<EReference> allReferencesToThisClass(final EClass cl) {
    final Function1<EStructuralFeature.Setting, EObject> _function = (EStructuralFeature.Setting it) -> {
      return it.getEObject();
    };
    return Iterables.<EReference>filter(ListExtensions.<EStructuralFeature.Setting, EObject>map(this.allUsagesOfThisClass(cl), _function), EReference.class);
  }
  
  /**
   * Finds the single usage of this class and it must be a
   * containment reference. Otherwise it show errors and throws an IllegalArgumentException.
   * 
   * Note that several references to the class are allowed: the important thing
   * is that exactly one is a containment reference.
   * 
   * @param cl
   */
  public EReference findSingleContainmentReferenceToThisClass(final EClass cl) {
    return this.getAsContainmentReference(this.findSingleUsageOfThisClass(cl));
  }
  
  /**
   * Finds the single usage the given EClass in the
   * EClass' package's resource set, performing validation (that is,
   * no usage is found, or more than one) checks and in case
   * show errors and throws an IllegalArgumentException.
   * 
   * @param cl
   */
  public EObject findSingleUsageOfThisClass(final EClass cl) {
    final List<EStructuralFeature.Setting> usages = this.allUsagesOfThisClass(cl);
    boolean _isEmpty = usages.isEmpty();
    if (_isEmpty) {
      String _eObjectRepr = EdeltaUtils.getEObjectRepr(cl);
      final String message = ("The EClass is not used: " + _eObjectRepr);
      this.showError(cl, message);
      throw new IllegalArgumentException(message);
    }
    int _size = usages.size();
    boolean _greaterThan = (_size > 1);
    if (_greaterThan) {
      final Function1<EStructuralFeature.Setting, String> _function = (EStructuralFeature.Setting it) -> {
        String _eObjectRepr_1 = EdeltaUtils.getEObjectRepr(it.getEObject());
        String _plus = ("  " + _eObjectRepr_1);
        String _plus_1 = (_plus + "\n");
        String _plus_2 = (_plus_1 + 
          "    ");
        String _eObjectRepr_2 = EdeltaUtils.getEObjectRepr(it.getEStructuralFeature());
        return (_plus_2 + _eObjectRepr_2);
      };
      String _join = IterableExtensions.join(ListExtensions.<EStructuralFeature.Setting, String>map(usages, _function), "\n");
      final String message_1 = ("The EClass is used by more than one element:\n" + _join);
      this.showError(cl, message_1);
      throw new IllegalArgumentException(message_1);
    }
    return IterableExtensions.<EStructuralFeature.Setting>head(usages).getEObject();
  }
  
  /**
   * Makes sure that the passed EObject represent a containment EReference
   * otherwise shows an error and throws an IllegalArgumentException
   * 
   * @param o
   * @return the containment EReference if it is a containment reference
   */
  public EReference getAsContainmentReference(final EObject o) {
    if ((o instanceof EReference)) {
      boolean _isContainment = ((EReference)o).isContainment();
      boolean _not = (!_isContainment);
      if (_not) {
        String _eObjectRepr = EdeltaUtils.getEObjectRepr(o);
        final String message = ("Not a containment reference: " + _eObjectRepr);
        this.showError(((ENamedElement)o), message);
        throw new IllegalArgumentException(message);
      }
      return ((EReference)o);
    }
    String _eObjectRepr_1 = EdeltaUtils.getEObjectRepr(o);
    final String message_1 = ("Not a reference: " + _eObjectRepr_1);
    this.showError(((ENamedElement) o), message_1);
    throw new IllegalArgumentException(message_1);
  }
  
  /**
   * Returns all the usages of the given EClass in the
   * EClass' package's resource set.
   * 
   * @param cl
   */
  public List<EStructuralFeature.Setting> allUsagesOfThisClass(final EClass cl) {
    final Function1<EStructuralFeature.Setting, Boolean> _function = (EStructuralFeature.Setting it) -> {
      EObject _eObject = it.getEObject();
      return Boolean.valueOf((_eObject instanceof ENamedElement));
    };
    final Function1<EStructuralFeature.Setting, Boolean> _function_1 = (EStructuralFeature.Setting it) -> {
      boolean _isDerived = it.getEStructuralFeature().isDerived();
      return Boolean.valueOf((!_isDerived));
    };
    return IterableExtensions.<EStructuralFeature.Setting>toList(IterableExtensions.<EStructuralFeature.Setting>filter(IterableExtensions.<EStructuralFeature.Setting>filter(EcoreUtil.UsageCrossReferencer.find(cl, EdeltaUtils.packagesToInspect(cl)), _function), _function_1));
  }
  
  /**
   * Finds the single EReference, in the EReferences of the given EClass,
   * with a type different from the given type, performing validation (that is,
   * no reference is found, or more than one) checks and in case
   * show errors and throws an IllegalArgumentException
   * 
   * @param cl
   * @param target
   */
  public EReference findSingleReferenceNotOfType(final EClass cl, final EClass type) {
    final Function1<EReference, Boolean> _function = (EReference it) -> {
      EClassifier _eType = it.getEType();
      return Boolean.valueOf((_eType != type));
    };
    final List<EReference> otherReferences = IterableExtensions.<EReference>toList(IterableExtensions.<EReference>filter(cl.getEReferences(), _function));
    boolean _isEmpty = otherReferences.isEmpty();
    if (_isEmpty) {
      String _eObjectRepr = EdeltaUtils.getEObjectRepr(type);
      final String message = ("No references not of type " + _eObjectRepr);
      this.showError(cl, message);
      throw new IllegalArgumentException(message);
    }
    int _size = otherReferences.size();
    boolean _greaterThan = (_size > 1);
    if (_greaterThan) {
      String _eObjectRepr_1 = EdeltaUtils.getEObjectRepr(type);
      String _plus = ("Too many references not of type " + _eObjectRepr_1);
      String _plus_1 = (_plus + 
        ":\n");
      final Function1<EReference, String> _function_1 = (EReference it) -> {
        String _eObjectRepr_2 = EdeltaUtils.getEObjectRepr(it);
        return ("  " + _eObjectRepr_2);
      };
      String _join = IterableExtensions.join(ListExtensions.<EReference, String>map(otherReferences, _function_1), "\n");
      final String message_1 = (_plus_1 + _join);
      this.showError(cl, message_1);
      throw new IllegalArgumentException(message_1);
    }
    return IterableExtensions.<EReference>head(otherReferences);
  }
  
  /**
   * Finds and returns the single containing class of the passed features.
   * If there's more than one containing class throws an IllegalArgumentException.
   */
  public EClass findSingleOwner(final Collection<EStructuralFeature> features) {
    final Function1<EStructuralFeature, EClass> _function = (EStructuralFeature it) -> {
      return it.getEContainingClass();
    };
    final Map<EClass, List<EStructuralFeature>> owners = IterableExtensions.<EClass, EStructuralFeature>groupBy(features, _function);
    int _size = owners.size();
    boolean _greaterThan = (_size > 1);
    if (_greaterThan) {
      final Function1<Map.Entry<EClass, List<EStructuralFeature>>, String> _function_1 = (Map.Entry<EClass, List<EStructuralFeature>> it) -> {
        final String reprForClass = EdeltaUtils.getEObjectRepr(it.getKey());
        this.showError(it.getKey(), 
          ("Extracted features must belong to the same class: " + reprForClass));
        final Function1<EStructuralFeature, String> _function_2 = (EStructuralFeature it_1) -> {
          String _eObjectRepr = EdeltaUtils.getEObjectRepr(it_1);
          return ("    " + _eObjectRepr);
        };
        String _join = IterableExtensions.join(ListExtensions.<EStructuralFeature, String>map(it.getValue(), _function_2), "\n");
        return ((("  " + reprForClass) + ":\n") + _join);
      };
      String _join = IterableExtensions.join(IterableExtensions.<Map.Entry<EClass, List<EStructuralFeature>>, String>map(owners.entrySet(), _function_1), "\n");
      final String message = ("Multiple containing classes:\n" + _join);
      throw new IllegalArgumentException(message);
    }
    return IterableExtensions.<EStructuralFeature>head(features).getEContainingClass();
  }
  
  /**
   * Checks that the passed subclasses have all exactly one superclass
   * and that it is the same and returns that as a result. It also checks
   * that such a common superclass has no further subclasses.
   * 
   * In case of failure, besides reporting errors, it throws an
   * IllegalArgumentException.
   */
  public EClass getSingleDirectSuperclass(final Collection<EClass> subclasses) {
    final Function1<EClass, Boolean> _function = (EClass it) -> {
      int _size = it.getESuperTypes().size();
      return Boolean.valueOf((_size != 1));
    };
    final Iterable<EClass> invalid = IterableExtensions.<EClass>filter(subclasses, _function);
    boolean _isEmpty = IterableExtensions.isEmpty(invalid);
    boolean _not = (!_isEmpty);
    if (_not) {
      final Consumer<EClass> _function_1 = (EClass it) -> {
        final EList<EClass> superclasses = it.getESuperTypes();
        String _eObjectRepr = EdeltaUtils.getEObjectRepr(it);
        String _plus = ("Expected one superclass: " + _eObjectRepr);
        String _plus_1 = (_plus + " instead of:\n");
        String _xifexpression = null;
        boolean _isEmpty_1 = superclasses.isEmpty();
        if (_isEmpty_1) {
          _xifexpression = "  empty";
        } else {
          final Function1<EClass, String> _function_2 = (EClass it_1) -> {
            String _eObjectRepr_1 = EdeltaUtils.getEObjectRepr(it_1);
            return ("  " + _eObjectRepr_1);
          };
          _xifexpression = IterableExtensions.join(ListExtensions.<EClass, String>map(superclasses, _function_2), "\n");
        }
        String _plus_2 = (_plus_1 + _xifexpression);
        this.showError(it, _plus_2);
      };
      invalid.forEach(_function_1);
      throw new IllegalArgumentException("Wrong superclasses");
    }
    final EClass result = IterableExtensions.<EClass>head(IterableExtensions.<EClass>head(subclasses).getESuperTypes());
    final Function1<EClass, Boolean> _function_2 = (EClass it) -> {
      EClass _head = IterableExtensions.<EClass>head(it.getESuperTypes());
      return Boolean.valueOf((_head != result));
    };
    final Iterable<EClass> differences = IterableExtensions.<EClass>filter(subclasses, _function_2);
    boolean _isEmpty_1 = IterableExtensions.isEmpty(differences);
    boolean _not_1 = (!_isEmpty_1);
    if (_not_1) {
      final Consumer<EClass> _function_3 = (EClass it) -> {
        String _eObjectRepr = EdeltaUtils.getEObjectRepr(it);
        String _plus = ("Wrong superclass of " + _eObjectRepr);
        String _plus_1 = (_plus + ":\n");
        String _plus_2 = (_plus_1 + 
          "  Expected: ");
        String _eObjectRepr_1 = EdeltaUtils.getEObjectRepr(result);
        String _plus_3 = (_plus_2 + _eObjectRepr_1);
        String _plus_4 = (_plus_3 + "\n");
        String _plus_5 = (_plus_4 + 
          "  Actual  : ");
        String _eObjectRepr_2 = EdeltaUtils.getEObjectRepr(IterableExtensions.<EClass>head(it.getESuperTypes()));
        final String message = (_plus_5 + _eObjectRepr_2);
        this.showError(it, message);
      };
      differences.forEach(_function_3);
      throw new IllegalArgumentException("Wrong superclasses");
    }
    final Set<EClass> additionalSubclasses = IterableExtensions.<EClass>toSet(this.directSubclasses(result));
    additionalSubclasses.removeAll(IterableExtensions.<EClass>toSet(subclasses));
    boolean _isEmpty_2 = additionalSubclasses.isEmpty();
    boolean _not_2 = (!_isEmpty_2);
    if (_not_2) {
      final Function1<EClass, String> _function_4 = (EClass it) -> {
        String _eObjectRepr = EdeltaUtils.getEObjectRepr(it);
        return ("  " + _eObjectRepr);
      };
      String _join = IterableExtensions.join(IterableExtensions.<EClass, String>map(additionalSubclasses, _function_4), "\n");
      final String message = ("The class has additional subclasses:\n" + _join);
      this.showError(result, message);
      throw new IllegalArgumentException(message);
    }
    return result;
  }
  
  public Iterable<EClass> directSubclasses(final EClass cl) {
    final Function1<EStructuralFeature.Setting, Boolean> _function = (EStructuralFeature.Setting it) -> {
      EStructuralFeature _eStructuralFeature = it.getEStructuralFeature();
      return Boolean.valueOf(Objects.equal(_eStructuralFeature, getEReference("ecore", "EClass", "eSuperTypes")));
    };
    final Function1<EStructuralFeature.Setting, EClass> _function_1 = (EStructuralFeature.Setting it) -> {
      EObject _eObject = it.getEObject();
      return ((EClass) _eObject);
    };
    return IterableExtensions.<EStructuralFeature.Setting, EClass>map(IterableExtensions.<EStructuralFeature.Setting>filter(this.allUsagesOfThisClass(cl), _function), _function_1);
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
  }
}
