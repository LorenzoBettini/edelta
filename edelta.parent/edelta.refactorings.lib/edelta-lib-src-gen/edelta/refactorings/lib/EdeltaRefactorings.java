package edelta.refactorings.lib;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaLibrary;
import edelta.refactorings.lib.helper.EdeltaFeatureDifferenceFinder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ListExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Pair;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure2;
import org.eclipse.xtext.xbase.lib.StringExtensions;

@SuppressWarnings("all")
public class EdeltaRefactorings extends AbstractEdelta {
  public EdeltaRefactorings() {
    
  }
  
  public EdeltaRefactorings(final AbstractEdelta other) {
    super(other);
  }
  
  public EAttribute addMandatoryAttribute(final EClass eClass, final String attributeName, final EDataType dataType) {
    final Consumer<EAttribute> _function = (EAttribute it) -> {
      EdeltaLibrary.makeSingleRequired(it);
    };
    return EdeltaLibrary.addNewEAttribute(eClass, attributeName, dataType, _function);
  }
  
  public EReference addMandatoryReference(final EClass eClass, final String referenceName, final EClass type) {
    final Consumer<EReference> _function = (EReference it) -> {
      EdeltaLibrary.makeSingleRequired(it);
    };
    return EdeltaLibrary.addNewEReference(eClass, referenceName, type, _function);
  }
  
  /**
   * Merges the given features into a single new feature in the containing class.
   * The features must be compatible (same containing class, same type, same cardinality, etc).
   * 
   * @param newFeatureName
   * @param features
   * @return the new feature added to the containing class of the features
   */
  public EStructuralFeature mergeFeatures(final String newFeatureName, final Collection<EStructuralFeature> features) {
    final EdeltaFeatureDifferenceFinder diffFinder = new EdeltaFeatureDifferenceFinder().ignoringName();
    boolean _checkNoDifferences = this.checkNoDifferences(features, diffFinder, "The two features cannot be merged");
    boolean _not = (!_checkNoDifferences);
    if (_not) {
      return null;
    }
    final EStructuralFeature feature = IterableExtensions.<EStructuralFeature>head(features);
    final EClass owner = feature.getEContainingClass();
    final EStructuralFeature copy = EdeltaLibrary.copyToAs(feature, owner, newFeatureName);
    EdeltaLibrary.removeAllElements(features);
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
    final EdeltaFeatureDifferenceFinder diffFinder = new EdeltaFeatureDifferenceFinder().ignoringName().ignoringType();
    if (((!this.checkCompliant(feature, features)) || 
      (!this.checkNoDifferences(Iterables.<EStructuralFeature>concat(Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(feature)), features), diffFinder, "The two features cannot be merged")))) {
      return null;
    }
    EdeltaLibrary.removeAllElements(features);
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
    final EStructuralFeature copy = EdeltaLibrary.copyToAs(feature, owner, newFeatureName, type);
    this.mergeFeatures(copy, features);
    return copy;
  }
  
  /**
   * Given an EAttribute, expected to have an EEnum type, creates a subclass of
   * the containing class for each value of the referred EEnum.
   * The attribute will then be removed and so will the EEnum.
   * The original containing EClass is made abstract.
   * 
   * @param attr
   * @return the collection of created subclasses
   */
  public Collection<EClass> enumToSubclasses(final EAttribute attr) {
    final EDataType type = attr.getEAttributeType();
    if ((type instanceof EEnum)) {
      final ArrayList<EClass> createdSubclasses = CollectionLiterals.<EClass>newArrayList();
      final EClass owner = attr.getEContainingClass();
      EdeltaLibrary.makeAbstract(owner);
      EList<EEnumLiteral> _eLiterals = ((EEnum)type).getELiterals();
      for (final EEnumLiteral subc : _eLiterals) {
        final Consumer<EClass> _function = (EClass it) -> {
          EdeltaLibrary.addESuperType(it, owner);
        };
        EClass _addNewEClass = EdeltaLibrary.addNewEClass(owner.getEPackage(), subc.getLiteral(), _function);
        createdSubclasses.add(_addNewEClass);
      }
      EdeltaLibrary.removeElement(type);
      return createdSubclasses;
    } else {
      String _eObjectRepr = EdeltaLibrary.getEObjectRepr(type);
      String _plus = ("Not an EEnum: " + _eObjectRepr);
      this.showError(attr, _plus);
      return null;
    }
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
    final EClass superclass = IterableExtensions.<EClass>head(IterableExtensions.<EClass>head(subclasses).getESuperTypes());
    final EPackage ePackage = superclass.getEPackage();
    final Consumer<EEnum> _function = (EEnum it) -> {
      final Procedure2<EClass, Integer> _function_1 = (EClass subClass, Integer index) -> {
        final String enumLiteralName = this.ensureEClassifierNameIsUnique(ePackage, subClass.getName().toUpperCase());
        EEnumLiteral _addNewEEnumLiteral = EdeltaLibrary.addNewEEnumLiteral(it, enumLiteralName);
        final Procedure1<EEnumLiteral> _function_2 = (EEnumLiteral it_1) -> {
          it_1.setValue((index).intValue());
        };
        ObjectExtensions.<EEnumLiteral>operator_doubleArrow(_addNewEEnumLiteral, _function_2);
      };
      IterableExtensions.<EClass>forEach(subclasses, _function_1);
    };
    final EEnum enum_ = EdeltaLibrary.addNewEEnum(ePackage, name, _function);
    final EAttribute attribute = EdeltaLibrary.addNewEAttribute(superclass, this.fromTypeToFeatureName(enum_), enum_);
    EdeltaLibrary.makeConcrete(superclass);
    EdeltaLibrary.removeAllElements(subclasses);
    return attribute;
  }
  
  /**
   * @param name the name for the extracted class
   * @param features the features to extract
   * @param newReferenceName the new name for the reference from the owner class to the
   * extracted class
   * @return the extracted metaclass
   */
  public EClass extractClass(final String name, final Collection<EStructuralFeature> features, final String newReferenceName) {
    final Function1<EStructuralFeature, EClass> _function = (EStructuralFeature it) -> {
      return it.getEContainingClass();
    };
    final Set<EClass> owners = IterableExtensions.<EClass>toSet(IterableExtensions.<EStructuralFeature, EClass>map(features, _function));
    boolean _isEmpty = owners.isEmpty();
    if (_isEmpty) {
      return null;
    }
    int _size = owners.size();
    boolean _greaterThan = (_size > 1);
    if (_greaterThan) {
      final Consumer<EClass> _function_1 = (EClass owner) -> {
        String _eObjectRepr = EdeltaLibrary.getEObjectRepr(owner);
        String _plus = ("Extracted features must belong to the same class: " + _eObjectRepr);
        this.showError(owner, _plus);
      };
      owners.forEach(_function_1);
      return null;
    }
    final EClass owner = IterableExtensions.<EClass>head(owners);
    final EClass extracted = EdeltaLibrary.addNewEClass(owner.getEPackage(), name);
    EReference _addMandatoryReference = this.addMandatoryReference(owner, newReferenceName, extracted);
    final Procedure1<EReference> _function_2 = (EReference it) -> {
      this.makeContainmentBidirectional(it);
    };
    ObjectExtensions.<EReference>operator_doubleArrow(_addMandatoryReference, _function_2);
    EdeltaLibrary.moveAllTo(features, extracted);
    return extracted;
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
      reference.setContainment(true);
      final EClass owner = reference.getEContainingClass();
      final EClass referredType = reference.getEReferenceType();
      EReference _addMandatoryReference = this.addMandatoryReference(referredType, this.fromTypeToFeatureName(owner), owner);
      final Procedure1<EReference> _function = (EReference it) -> {
        EdeltaLibrary.makeBidirectional(it, reference);
      };
      _xblockexpression = ObjectExtensions.<EReference>operator_doubleArrow(_addMandatoryReference, _function);
    }
    return _xblockexpression;
  }
  
  /**
   * @param name the name for the extracted class
   * @param reference the reference to turn into a reference to the extracted class
   * @return the extracted class
   */
  public EClass referenceToClass(final String name, final EReference reference) {
    boolean _checkNotContainment = this.checkNotContainment(reference, 
      "Cannot apply referenceToClass on containment reference");
    boolean _not = (!_checkNotContainment);
    if (_not) {
      return null;
    }
    final EPackage ePackage = reference.getEContainingClass().getEPackage();
    final EClass extracted = EdeltaLibrary.addNewEClass(ePackage, name);
    final EReference extractedRef = this.addMandatoryReference(extracted, 
      this.fromTypeToFeatureName(reference.getEType()), reference.getEReferenceType());
    final EReference eOpposite = reference.getEOpposite();
    if ((eOpposite != null)) {
      EdeltaLibrary.makeBidirectional(eOpposite, extractedRef);
    }
    reference.setEType(extracted);
    this.makeContainmentBidirectional(reference);
    return extracted;
  }
  
  public void classToReference(final EClass cl) {
    final Function1<EStructuralFeature.Setting, Boolean> _function = (EStructuralFeature.Setting it) -> {
      EStructuralFeature _eStructuralFeature = it.getEStructuralFeature();
      return Boolean.valueOf(Objects.equal(_eStructuralFeature, getEReference("ecore", "EReference", "eReferenceType")));
    };
    final Function1<EStructuralFeature.Setting, EObject> _function_1 = (EStructuralFeature.Setting it) -> {
      return it.getEObject();
    };
    final Iterable<EReference> references = Iterables.<EReference>filter(IterableExtensions.<EStructuralFeature.Setting, EObject>map(IterableExtensions.<EStructuralFeature.Setting>filter(EcoreUtil.UsageCrossReferencer.find(cl, cl.getEPackage()), _function), _function_1), EReference.class);
    boolean _isEmpty = IterableExtensions.isEmpty(references);
    if (_isEmpty) {
      String _eObjectRepr = EdeltaLibrary.getEObjectRepr(cl);
      String _plus = ("The EClass is not referred: " + _eObjectRepr);
      this.showError(cl, _plus);
      return;
    } else {
      final Function1<EReference, Boolean> _function_2 = (EReference it) -> {
        return Boolean.valueOf(it.isContainment());
      };
      int _size = IterableExtensions.size(IterableExtensions.<EReference>filter(references, _function_2));
      boolean _greaterThan = (_size > 1);
      if (_greaterThan) {
        final Function1<EReference, String> _function_3 = (EReference it) -> {
          String _eObjectRepr_1 = EdeltaLibrary.getEObjectRepr(it);
          return ("  " + _eObjectRepr_1);
        };
        String _join = IterableExtensions.join(IterableExtensions.<EReference, String>map(references, _function_3), "\n");
        String _plus_1 = ("The EClass is referred by more than one container:\n" + _join);
        this.showError(cl, _plus_1);
        return;
      }
    }
    final EReference reference = IterableExtensions.<EReference>head(references);
    final EClass owner = reference.getEContainingClass();
    final Function1<EStructuralFeature, Boolean> _function_4 = (EStructuralFeature it) -> {
      EClassifier _eType = it.getEType();
      return Boolean.valueOf(Objects.equal(_eType, owner));
    };
    final EStructuralFeature referenceToOwner = IterableExtensions.<EStructuralFeature>head(IterableExtensions.<EStructuralFeature>filter(cl.getEStructuralFeatures(), _function_4));
    final Function1<EStructuralFeature, Boolean> _function_5 = (EStructuralFeature it) -> {
      return Boolean.valueOf((it != referenceToOwner));
    };
    final List<EReference> otherReferences = IterableExtensions.<EReference>toList(Iterables.<EReference>filter(IterableExtensions.<EStructuralFeature>filter(cl.getEStructuralFeatures(), _function_5), EReference.class));
    boolean _isEmpty_1 = otherReferences.isEmpty();
    if (_isEmpty_1) {
      String _eObjectRepr_1 = EdeltaLibrary.getEObjectRepr(cl);
      String _plus_2 = ("Missing reference to target type: " + _eObjectRepr_1);
      this.showError(cl, _plus_2);
      return;
    }
    int _size_1 = otherReferences.size();
    boolean _greaterThan_1 = (_size_1 > 1);
    if (_greaterThan_1) {
      final Function1<EReference, String> _function_6 = (EReference it) -> {
        String _eObjectRepr_2 = EdeltaLibrary.getEObjectRepr(it);
        return ("  " + _eObjectRepr_2);
      };
      String _join_1 = IterableExtensions.join(ListExtensions.<EReference, String>map(otherReferences, _function_6), "\n");
      String _plus_3 = ("Too many references to target type:\n" + _join_1);
      this.showError(cl, _plus_3);
      return;
    }
    final EReference referenceToTarget = IterableExtensions.<EReference>head(otherReferences);
    reference.setEType(referenceToTarget.getEType());
    reference.setContainment(false);
    final EReference opposite = referenceToTarget.getEOpposite();
    if ((opposite != null)) {
      EdeltaLibrary.makeBidirectional(reference, opposite);
    }
    EdeltaLibrary.removeElement(cl);
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
  public EClass extractSuperclass(final List<? extends EStructuralFeature> duplicates) {
    EClass _xblockexpression = null;
    {
      final EStructuralFeature feature = IterableExtensions.head(duplicates);
      final EPackage containingEPackage = feature.getEContainingClass().getEPackage();
      String _firstUpper = StringExtensions.toFirstUpper(feature.getName());
      String _plus = (_firstUpper + "Element");
      final String superClassName = this.ensureEClassifierNameIsUnique(containingEPackage, _plus);
      _xblockexpression = this.extractSuperclass(superClassName, duplicates);
    }
    return _xblockexpression;
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
  public EClass extractSuperclass(final String name, final List<? extends EStructuralFeature> duplicates) {
    EClass _xblockexpression = null;
    {
      final EStructuralFeature feature = IterableExtensions.head(duplicates);
      final EPackage containingEPackage = feature.getEContainingClass().getEPackage();
      final Consumer<EClass> _function = (EClass it) -> {
        EdeltaLibrary.makeAbstract(it);
        final Function1<EStructuralFeature, EClass> _function_1 = (EStructuralFeature it_1) -> {
          return it_1.getEContainingClass();
        };
        final Consumer<EClass> _function_2 = (EClass c) -> {
          EdeltaLibrary.addESuperType(c, it);
        };
        ListExtensions.map(duplicates, _function_1).forEach(_function_2);
        this.pullUpFeatures(it, duplicates);
      };
      _xblockexpression = EdeltaLibrary.addNewEClass(containingEPackage, name, _function);
    }
    return _xblockexpression;
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
  public void pullUpFeatures(final EClass dest, final List<? extends EStructuralFeature> duplicates) {
    final EdeltaFeatureDifferenceFinder diffFinder = new EdeltaFeatureDifferenceFinder().ignoringContainingClass();
    boolean _checkNoDifferences = this.checkNoDifferences(duplicates, diffFinder, "The two features are not equal");
    boolean _not = (!_checkNoDifferences);
    if (_not) {
      return;
    }
    final Function1<EStructuralFeature, Boolean> _function = (EStructuralFeature it) -> {
      boolean _contains = it.getEContainingClass().getESuperTypes().contains(dest);
      return Boolean.valueOf((!_contains));
    };
    final Iterable<? extends EStructuralFeature> wrongFeatures = IterableExtensions.filter(duplicates, _function);
    boolean _isEmpty = IterableExtensions.isEmpty(wrongFeatures);
    boolean _not_1 = (!_isEmpty);
    if (_not_1) {
      final Consumer<EStructuralFeature> _function_1 = (EStructuralFeature it) -> {
        String _eObjectRepr = EdeltaLibrary.getEObjectRepr(it.getEContainingClass());
        String _plus = ("Not a direct subclass of destination: " + _eObjectRepr);
        this.showError(it, _plus);
      };
      wrongFeatures.forEach(_function_1);
      return;
    }
    EdeltaLibrary.copyTo(IterableExtensions.head(duplicates), dest);
    EdeltaLibrary.removeAllElements(duplicates);
  }
  
  /**
   * Ensures that the proposed classifier name is unique within the specified
   * package; if not, it appends an incremental index until the name
   * is actually unique
   */
  public String ensureEClassifierNameIsUnique(final EPackage ePackage, final String proposedName) {
    String className = proposedName;
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
  
  /**
   * Fix all the passed redundant containers (in the shape of pairs)
   * by setting the eOpposite property.
   * 
   * That is, given the pair r1 -> r2, then r2 is set as the opposite
   * reference of r1 and viceversa.
   */
  public void redundantContainerToEOpposite(final Iterable<Pair<EReference, EReference>> redundantContainers) {
    for (final Pair<EReference, EReference> redundant : redundantContainers) {
      EdeltaLibrary.makeBidirectional(redundant.getKey(), redundant.getValue());
    }
  }
  
  /**
   * Given a map with key an EClass and value a list of its subclasses,
   * generates an EEnum (in the EClass' package) representing the inheritance relation
   * (the name is the name of the key EClass with "Type" suffix),
   * with an EEnumLiteral for each subclass (the name is the name
   * of the subclass in uppercase); the subclasses are removed, and the
   * key EClass is added an EAttribute with the created EEnum as type
   * (the name is the name of the EEnum, first letter lowercase with "Type"
   * suffix).
   * 
   * For example, give "Base" -> {"Derived1", "Derived2" } as input
   * it creates the EEnum "BaseType" with literals "DERIVED1", "DERIVED2",
   * it adds to "Base" the EAttribute "baseType" of type "BaseType".
   * The EClasses "Derived1" and "Derived2" are removed from the package.
   */
  public void classificationByHierarchyToEnum(final Map<EClass, List<EClass>> classificationsByHierarchy) {
    final BiConsumer<EClass, List<EClass>> _function = (EClass superClass, List<EClass> subClasses) -> {
      final EPackage ePackage = superClass.getEPackage();
      String _name = superClass.getName();
      String _plus = (_name + "Type");
      final String enumName = this.ensureEClassifierNameIsUnique(ePackage, _plus);
      final Consumer<EEnum> _function_1 = (EEnum it) -> {
        final Procedure2<EClass, Integer> _function_2 = (EClass subClass, Integer index) -> {
          final String enumLiteralName = this.ensureEClassifierNameIsUnique(ePackage, subClass.getName().toUpperCase());
          EEnumLiteral _addNewEEnumLiteral = EdeltaLibrary.addNewEEnumLiteral(it, enumLiteralName);
          final Procedure1<EEnumLiteral> _function_3 = (EEnumLiteral it_1) -> {
            it_1.setValue(((index).intValue() + 1));
          };
          ObjectExtensions.<EEnumLiteral>operator_doubleArrow(_addNewEEnumLiteral, _function_3);
        };
        IterableExtensions.<EClass>forEach(subClasses, _function_2);
      };
      final EEnum enum_ = EdeltaLibrary.addNewEEnum(ePackage, enumName, _function_1);
      String _lowerCase = superClass.getName().toLowerCase();
      String _plus_1 = (_lowerCase + "Type");
      EdeltaLibrary.addNewEAttribute(superClass, _plus_1, enum_);
      EdeltaLibrary.removeAllElements(subClasses);
    };
    classificationsByHierarchy.forEach(_function);
  }
  
  /**
   * Turns the given EClasses to abstract
   */
  public void makeAbstract(final Iterable<EClass> classes) {
    final Consumer<EClass> _function = (EClass it) -> {
      EdeltaLibrary.makeAbstract(it);
    };
    classes.forEach(_function);
  }
  
  /**
   * Turns the given EClasses to NON abstract
   */
  public void makeConcrete(final Iterable<EClass> classes) {
    final Consumer<EClass> _function = (EClass it) -> {
      EdeltaLibrary.makeConcrete(it);
    };
    classes.forEach(_function);
  }
  
  public String fromTypeToFeatureName(final EClassifier type) {
    return StringExtensions.toFirstLower(type.getName());
  }
  
  /**
   * @param reference the reference that must not be a containment reference
   * @param message the message to show in case the reference
   * is a containment reference
   * @return true if the passed reference is not a containment reference
   */
  public boolean checkNotContainment(final EReference reference, final String message) {
    final boolean containment = reference.isContainment();
    if (containment) {
      String _eObjectRepr = EdeltaLibrary.getEObjectRepr(reference);
      String _plus = ((message + ": ") + _eObjectRepr);
      this.showError(reference, _plus);
    }
    return (!containment);
  }
  
  /**
   * Makes sure that there are no differences in the passed features,
   * using the specified differenceFinder, otherwise it shows an error message
   * with the details of the differences.
   * 
   * @param features
   * @param differenceFinder
   * @param errorMessage
   * @return true if there are no differences
   */
  public boolean checkNoDifferences(final Iterable<? extends EStructuralFeature> features, final EdeltaFeatureDifferenceFinder differenceFinder, final String errorMessage) {
    final EStructuralFeature feature = IterableExtensions.head(features);
    final Function1<EStructuralFeature, Boolean> _function = (EStructuralFeature it) -> {
      return Boolean.valueOf(((feature != it) && (!differenceFinder.equals(feature, it))));
    };
    final EStructuralFeature different = IterableExtensions.findFirst(features, _function);
    if ((different != null)) {
      String _differenceDetails = differenceFinder.getDifferenceDetails();
      String _plus = ((errorMessage + ":\n") + _differenceDetails);
      this.showError(different, _plus);
      return false;
    }
    return true;
  }
  
  /**
   * Makes sure that the features have types that are subtypes of the
   * specified feature.
   * 
   * @param feature
   * @param features
   * @return true if they are all compliant
   */
  public boolean checkCompliant(final EStructuralFeature feature, final Collection<? extends EStructuralFeature> features) {
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
      String _eObjectRepr = EdeltaLibrary.getEObjectRepr(feature.getEType());
      String _plus = ("features not compliant with type " + _eObjectRepr);
      String _plus_1 = (_plus + ":\n");
      final Function1<EStructuralFeature, String> _function_3 = (EStructuralFeature it) -> {
        String _eObjectRepr_1 = EdeltaLibrary.getEObjectRepr(it);
        String _plus_2 = ("  " + _eObjectRepr_1);
        String _plus_3 = (_plus_2 + ": ");
        String _eObjectRepr_2 = EdeltaLibrary.getEObjectRepr(it.getEType());
        return (_plus_3 + _eObjectRepr_2);
      };
      String _join = IterableExtensions.join(IterableExtensions.map(nonCompliant, _function_3), "\n");
      String _plus_2 = (_plus_1 + _join);
      this.showError(feature, _plus_2);
      return false;
    }
    return true;
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
  }
}
