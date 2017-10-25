package gssi.refactorings;

import edelta.lib.AbstractEdelta;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
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
  
  public EAttribute addMandatoryAttr(final String attrname, final EClassifier etype, final EClass mc) {
    final Consumer<EAttribute> _function = (EAttribute it) -> {
      it.setEType(etype);
      it.setLowerBound(1);
    };
    final EAttribute a = this.lib.newEAttribute(attrname, _function);
    EList<EStructuralFeature> _eStructuralFeatures = mc.getEStructuralFeatures();
    _eStructuralFeatures.add(a);
    return a;
  }
  
  public EReference mergeReferences(final String newAttrName, final EClassifier etype, final List<EReference> refs) {
    final Consumer<EReference> _function = (EReference it) -> {
      it.setEType(etype);
    };
    final EReference newRef = this.lib.newEReference(newAttrName, _function);
    for (final EReference r : refs) {
      EList<EStructuralFeature> _eStructuralFeatures = r.getEContainingClass().getEStructuralFeatures();
      _eStructuralFeatures.remove(r);
    }
    return newRef;
  }
  
  public EAttribute mergeAttributes(final String newAttrName, final EClassifier etype, final List<EAttribute> attrs) {
    final Consumer<EAttribute> _function = (EAttribute it) -> {
      it.setEType(etype);
    };
    final EAttribute newAttr = this.lib.newEAttribute(newAttrName, _function);
    for (final EAttribute a : attrs) {
      EList<EStructuralFeature> _eStructuralFeatures = a.getEContainingClass().getEStructuralFeatures();
      _eStructuralFeatures.remove(a);
    }
    return newAttr;
  }
  
  public void introduceSubclasses(final EAttribute attr, final EEnum attr_type, final EClass containingclass) {
    containingclass.setAbstract(true);
    final EEnum subclasses = attr_type;
    EList<EEnumLiteral> _eLiterals = subclasses.getELiterals();
    for (final EEnumLiteral subc : _eLiterals) {
      {
        EList<EClassifier> _eClassifiers = containingclass.getEPackage().getEClassifiers();
        final Consumer<EClass> _function = (EClass it) -> {
          EList<EClass> _eSuperTypes = it.getESuperTypes();
          _eSuperTypes.add(containingclass);
        };
        EClass _newEClass = this.lib.newEClass(subc.getLiteral(), _function);
        _eClassifiers.add(_newEClass);
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
   * @param extracted_class the created EClass created representing the extracted metaclass
   * @param f
   * @param _in
   * @param _out
   */
  public void extractMetaClass(final EClass extracted_class, final EReference f, final String _in, final String _out) {
    final Consumer<EReference> _function = (EReference it) -> {
      it.setEType(extracted_class);
      it.setLowerBound(f.getEOpposite().getLowerBound());
      it.setUpperBound(1);
    };
    final EReference ref_in = this.lib.newEReference(_in, _function);
    final Consumer<EReference> _function_1 = (EReference it) -> {
      it.setLowerBound(1);
      it.setUpperBound(1);
      it.setEType(f.getEType());
      it.setEOpposite(ref_in);
    };
    final EReference old_ref = this.lib.newEReference(f.getName(), _function_1);
    EList<EStructuralFeature> _eStructuralFeatures = extracted_class.getEStructuralFeatures();
    _eStructuralFeatures.add(old_ref);
    ref_in.setEOpposite(old_ref);
    EReference _eOpposite = f.getEOpposite();
    _eOpposite.setLowerBound(1);
    EReference _eOpposite_1 = f.getEOpposite();
    _eOpposite_1.setUpperBound(1);
    EList<EStructuralFeature> _eStructuralFeatures_1 = extracted_class.getEStructuralFeatures();
    EReference _eOpposite_2 = f.getEOpposite();
    _eStructuralFeatures_1.add(_eOpposite_2);
    EList<EStructuralFeature> _eStructuralFeatures_2 = f.getEReferenceType().getEStructuralFeatures();
    _eStructuralFeatures_2.add(ref_in);
    f.setEType(extracted_class);
    f.setContainment(true);
    f.setName(_out);
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
  }
}
