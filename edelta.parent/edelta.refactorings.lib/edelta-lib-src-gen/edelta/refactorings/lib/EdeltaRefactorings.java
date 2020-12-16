package edelta.refactorings.lib;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaLibrary;
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
  
  public EAttribute addMandatoryAttr(final EClass eClass, final String attrname, final EDataType dataType) {
    final Consumer<EAttribute> _function = (EAttribute it) -> {
      it.setLowerBound(1);
    };
    return EdeltaLibrary.addNewEAttribute(eClass, attrname, dataType, _function);
  }
  
  public EReference mergeReferences(final String newReferenceName, final EClass newReferenceType, final List<EReference> refs) {
    this.removeFeaturesFromContainingClass(refs);
    return EdeltaLibrary.newEReference(newReferenceName, newReferenceType);
  }
  
  public EAttribute mergeAttributes(final String newAttrName, final EDataType newAttributeType, final List<EAttribute> attrs) {
    this.removeFeaturesFromContainingClass(attrs);
    return EdeltaLibrary.newEAttribute(newAttrName, newAttributeType);
  }
  
  public void removeFeaturesFromContainingClass(final List<? extends EStructuralFeature> features) {
    final Consumer<EStructuralFeature> _function = (EStructuralFeature it) -> {
      EList<EStructuralFeature> _eStructuralFeatures = it.getEContainingClass().getEStructuralFeatures();
      _eStructuralFeatures.remove(it);
    };
    features.forEach(_function);
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
   * @param superclass where to pull up a single instance of the passed attributes
   * @param the attributes that are expected to be the same; the first element will be
   * pulled up in the superclass
   */
  public boolean extractIntoSuperclass(final EClass superclass, final List<EAttribute> attrs) {
    boolean _xblockexpression = false;
    {
      final EAttribute extracted_attr = IterableExtensions.<EAttribute>head(attrs);
      for (final EAttribute attr : attrs) {
        EClass _eContainingClass = attr.getEContainingClass();
        final Procedure1<EClass> _function = (EClass it) -> {
          EdeltaLibrary.addESuperType(it, superclass);
          EList<EStructuralFeature> _eStructuralFeatures = it.getEStructuralFeatures();
          _eStructuralFeatures.remove(attr);
        };
        ObjectExtensions.<EClass>operator_doubleArrow(_eContainingClass, _function);
      }
      EList<EStructuralFeature> _eStructuralFeatures = superclass.getEStructuralFeatures();
      _xblockexpression = _eStructuralFeatures.add(extracted_attr);
    }
    return _xblockexpression;
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
    final Consumer<EReference> _function_2 = (EReference it) -> {
      EdeltaLibrary.makeSingleRequired(it);
      this.makeContainmentBidirectional(it);
    };
    EdeltaLibrary.addNewEReference(owner, newReferenceName, extracted, _function_2);
    final Consumer<EStructuralFeature> _function_3 = (EStructuralFeature it) -> {
      EdeltaLibrary.moveTo(it, extracted);
    };
    features.forEach(_function_3);
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
      final Consumer<EReference> _function = (EReference it) -> {
        EdeltaLibrary.makeBidirectional(it, reference);
        EdeltaLibrary.makeSingleRequired(it);
      };
      _xblockexpression = EdeltaLibrary.addNewEReference(referredType, this.fromTypeToFeatureName(owner), owner, _function);
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
    final Consumer<EReference> _function = (EReference it) -> {
      EdeltaLibrary.makeSingleRequired(it);
    };
    final EReference extractedRef = EdeltaLibrary.addNewEReference(extracted, 
      this.fromTypeToFeatureName(reference.getEType()), reference.getEReferenceType(), _function);
    final EReference eOpposite = reference.getEOpposite();
    if ((eOpposite != null)) {
      eOpposite.setEType(extracted);
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
      opposite.setEType(owner);
      EdeltaLibrary.makeBidirectional(reference, opposite);
    }
    EdeltaLibrary.removeElement(cl);
  }
  
  /**
   * Given a non empty list of {@link EStructuralFeature}, which are known to
   * appear in several classes as duplicates, extracts a new common superclass,
   * with the duplicate feature,
   * adds the extracted superclass to the classes with the duplicate
   * feature and removes the duplicate feature from each class.
   * 
   * @param duplicates
   */
  public void extractSuperclass(final List<? extends EStructuralFeature> duplicates) {
    final EStructuralFeature feature = IterableExtensions.head(duplicates);
    final EPackage containingEPackage = feature.getEContainingClass().getEPackage();
    String _firstUpper = StringExtensions.toFirstUpper(feature.getName());
    String _plus = (_firstUpper + "Element");
    final String superClassName = this.ensureEClassifierNameIsUnique(containingEPackage, _plus);
    final Consumer<EClass> _function = (EClass it) -> {
      it.setAbstract(true);
      EdeltaLibrary.addEStructuralFeature(it, EcoreUtil.<EStructuralFeature>copy(feature));
    };
    final EClass superclass = EdeltaLibrary.addNewEClass(containingEPackage, superClassName, _function);
    for (final EStructuralFeature duplicate : duplicates) {
      EClass _eContainingClass = duplicate.getEContainingClass();
      final Procedure1<EClass> _function_1 = (EClass it) -> {
        EdeltaLibrary.addESuperType(it, superclass);
        EList<EStructuralFeature> _eStructuralFeatures = it.getEStructuralFeatures();
        _eStructuralFeatures.remove(duplicate);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_eContainingClass, _function_1);
    }
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
      {
        EReference _key = redundant.getKey();
        _key.setEOpposite(redundant.getValue());
        EReference _value = redundant.getValue();
        _value.setEOpposite(redundant.getKey());
      }
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
      EcoreUtil.removeAll(subClasses);
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
