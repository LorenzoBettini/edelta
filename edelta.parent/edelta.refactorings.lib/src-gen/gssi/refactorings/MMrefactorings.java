package gssi.refactorings;

import edelta.lib.AbstractEdelta;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

@SuppressWarnings("all")
public class MMrefactorings extends AbstractEdelta {
  public MMrefactorings() {
    
  }
  
  public MMrefactorings(final AbstractEdelta other) {
    super(other);
  }
  
  public EAttribute addMandatoryAttr(final EClass eClass, final String attrname, final EDataType dataType) {
    final Consumer<EAttribute> _function = (EAttribute it) -> {
      it.setLowerBound(1);
      this.lib.addEAttribute(eClass, it);
    };
    return this.lib.newEAttribute(attrname, dataType, _function);
  }
  
  public EReference mergeReferences(final String newAttrName, final EClass etype, final List<EReference> refs) {
    final Consumer<EReference> _function = (EReference it) -> {
    };
    final EReference newRef = this.lib.newEReference(newAttrName, etype, _function);
    this.removeFeaturesFromContainingClass(refs);
    return newRef;
  }
  
  public EAttribute mergeAttributes(final String newAttrName, final EDataType dataType, final List<EAttribute> attrs) {
    final Consumer<EAttribute> _function = (EAttribute it) -> {
    };
    final EAttribute newAttr = this.lib.newEAttribute(newAttrName, dataType, _function);
    this.removeFeaturesFromContainingClass(attrs);
    return newAttr;
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
          EList<EClass> _eSuperTypes = it.getESuperTypes();
          _eSuperTypes.add(containingclass);
        };
        this.lib.addNewEClass(containingclass.getEPackage(), subc.getLiteral(), _function);
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
  public boolean extractSuperclass(final EClass superclass, final List<EAttribute> attrs) {
    boolean _xblockexpression = false;
    {
      final EAttribute extracted_attr = IterableExtensions.<EAttribute>head(attrs);
      for (final EAttribute attr : attrs) {
        {
          EList<EClass> _eSuperTypes = attr.getEContainingClass().getESuperTypes();
          _eSuperTypes.add(superclass);
          EList<EStructuralFeature> _eStructuralFeatures = attr.getEContainingClass().getEStructuralFeatures();
          _eStructuralFeatures.remove(attr);
        }
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
    final EReference ref_in = this.lib.newEReference(inReferenceName, extractedClass, _function);
    final Consumer<EReference> _function_1 = (EReference it) -> {
      it.setLowerBound(1);
      it.setUpperBound(1);
      it.setEOpposite(ref_in);
    };
    final EReference old_ref = this.lib.newEReference(f.getName(), f.getEReferenceType(), _function_1);
    this.lib.addEReference(extractedClass, old_ref);
    ref_in.setEOpposite(old_ref);
    EReference _eOpposite = f.getEOpposite();
    _eOpposite.setLowerBound(1);
    EReference _eOpposite_1 = f.getEOpposite();
    _eOpposite_1.setUpperBound(1);
    this.lib.addEReference(extractedClass, f.getEOpposite());
    this.lib.addEReference(f.getEReferenceType(), ref_in);
    f.setEType(extractedClass);
    f.setContainment(true);
    f.setName(outReferenceName);
  }
}
