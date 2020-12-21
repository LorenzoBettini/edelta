package edelta.refactorings.lib;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaLibrary;
import edelta.refactorings.lib.helper.EstructuralFeatureEqualityHelper;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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
  
  public EReference mergeReferences(final String newReferenceName, final EClass newReferenceType, final List<EReference> refs) {
    EdeltaLibrary.removeAllElements(refs);
    return EdeltaLibrary.newEReference(newReferenceName, newReferenceType);
  }
  
  public EAttribute mergeAttributes(final String newAttrName, final EDataType newAttributeType, final List<EAttribute> attrs) {
    EdeltaLibrary.removeAllElements(attrs);
    return EdeltaLibrary.newEAttribute(newAttrName, newAttributeType);
  }
  
  public void introduceSubclasses(final EClass containingclass, final EAttribute attr, final EEnum enumType) {
    containingclass.setAbstract(true);
    EList<EEnumLiteral> _eLiterals = enumType.getELiterals();
    for (final EEnumLiteral subc : _eLiterals) {
      {
        final Consumer<EClass> _function = (EClass it) -> {
          EdeltaLibrary.addESuperType(it, containingclass);
        };
        EdeltaLibrary.addNewEClass(containingclass.getEPackage(), subc.getLiteral(), _function);
        EList<EStructuralFeature> _eStructuralFeatures = containingclass.getEStructuralFeatures();
        _eStructuralFeatures.remove(attr);
      }
    }
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
        it.setAbstract(true);
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
    final EStructuralFeature feature = IterableExtensions.head(duplicates);
    final EstructuralFeatureEqualityHelper equality = new EstructuralFeatureEqualityHelper();
    final Function1<EStructuralFeature, Boolean> _function = (EStructuralFeature it) -> {
      return Boolean.valueOf(((feature != it) && (!equality.equals(feature, it))));
    };
    final EStructuralFeature different = IterableExtensions.findFirst(duplicates, _function);
    if ((different != null)) {
      String _eObjectRepr = EdeltaLibrary.getEObjectRepr(feature);
      String _plus = (("The two features are not equal:\n" + 
        "  ") + _eObjectRepr);
      String _plus_1 = (_plus + "\n");
      String _plus_2 = (_plus_1 + 
        "  ");
      String _eObjectRepr_1 = EdeltaLibrary.getEObjectRepr(different);
      String _plus_3 = (_plus_2 + _eObjectRepr_1);
      String _plus_4 = (_plus_3 + "\n");
      String _plus_5 = (_plus_4 + 
        "  different for ");
      String _eObjectRepr_2 = EdeltaLibrary.getEObjectRepr(equality.getDifference());
      String _plus_6 = (_plus_5 + _eObjectRepr_2);
      this.showError(different, _plus_6);
      return;
    }
    final Function1<EStructuralFeature, Boolean> _function_1 = (EStructuralFeature it) -> {
      boolean _contains = it.getEContainingClass().getESuperTypes().contains(dest);
      return Boolean.valueOf((!_contains));
    };
    final Iterable<? extends EStructuralFeature> wrongFeatures = IterableExtensions.filter(duplicates, _function_1);
    boolean _isEmpty = IterableExtensions.isEmpty(wrongFeatures);
    boolean _not = (!_isEmpty);
    if (_not) {
      final Consumer<EStructuralFeature> _function_2 = (EStructuralFeature it) -> {
        String _eObjectRepr_3 = EdeltaLibrary.getEObjectRepr(it.getEContainingClass());
        String _plus_7 = ("Not a direct subclass of destination: " + _eObjectRepr_3);
        this.showError(it, _plus_7);
      };
      wrongFeatures.forEach(_function_2);
      return;
    }
    EdeltaLibrary.copyTo(feature, dest);
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
      it.setAbstract(true);
    };
    classes.forEach(_function);
  }
  
  /**
   * Turns the given EClasses to NON abstract
   */
  public void makeConcrete(final Iterable<EClass> classes) {
    final Consumer<EClass> _function = (EClass it) -> {
      it.setAbstract(false);
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
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
  }
}
