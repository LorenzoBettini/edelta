package edelta.refactorings.lib;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaLibrary;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
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
   * @param extractedClass the created EClass created representing the extracted metaclass
   * @param f
   * @param inReferenceName
   * @param outReferenceName
   */
  public void extractMetaClass(final EClass extractedClass, final EReference f, final String inReferenceName, final String outReferenceName) {
    final Consumer<EReference> _function = (EReference it) -> {
      it.setLowerBound(f.getEOpposite().getLowerBound());
      it.setUpperBound(1);
    };
    final EReference ref_in = EdeltaLibrary.newEReference(inReferenceName, extractedClass, _function);
    final Consumer<EReference> _function_1 = (EReference it) -> {
      it.setLowerBound(1);
      it.setUpperBound(1);
      it.setEOpposite(ref_in);
    };
    final EReference old_ref = EdeltaLibrary.newEReference(f.getName(), f.getEReferenceType(), _function_1);
    EdeltaLibrary.addEReference(extractedClass, old_ref);
    ref_in.setEOpposite(old_ref);
    EReference _eOpposite = f.getEOpposite();
    _eOpposite.setLowerBound(1);
    EReference _eOpposite_1 = f.getEOpposite();
    _eOpposite_1.setUpperBound(1);
    EdeltaLibrary.addEReference(extractedClass, f.getEOpposite());
    EdeltaLibrary.addEReference(f.getEReferenceType(), ref_in);
    f.setEType(extractedClass);
    f.setContainment(true);
    f.setName(outReferenceName);
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
  public void concreteBaseMetaclassToAbstract(final Iterable<EClass> concreteAbstractMetaclasses) {
    final Consumer<EClass> _function = (EClass it) -> {
      it.setAbstract(true);
    };
    concreteAbstractMetaclasses.forEach(_function);
  }
  
  /**
   * Turns the given EClasses to NON abstract
   */
  public void abstractBaseMetaclassToConcrete(final Iterable<EClass> abstractConcreteMetaclasses) {
    final Consumer<EClass> _function = (EClass it) -> {
      it.setAbstract(false);
    };
    abstractConcreteMetaclasses.forEach(_function);
  }
}
