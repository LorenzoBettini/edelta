package edelta.tests;

import edelta.edelta.EdeltaProgram;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProviderCustom;
import edelta.util.EdeltaModelUtil;
import org.assertj.core.api.Assertions;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
@SuppressWarnings("all")
public class EdeltaModelUtilTest extends EdeltaAbstractTest {
  @Test
  public void testGetProgram() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      Assert.assertSame(it, EdeltaModelUtil.getProgram(this.lastModifyEcoreOperation(it)));
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testHasCycleInSuperPackageWithNoCycle() {
    final EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;
    EPackage _createEPackage = ecoreFactory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      EList<EPackage> _eSubpackages = it.getESubpackages();
      EPackage _createEPackage_1 = ecoreFactory.createEPackage();
      final Procedure1<EPackage> _function_1 = (EPackage it_1) -> {
        EList<EPackage> _eSubpackages_1 = it_1.getESubpackages();
        EPackage _createEPackage_2 = ecoreFactory.createEPackage();
        _eSubpackages_1.add(_createEPackage_2);
      };
      EPackage _doubleArrow = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage_1, _function_1);
      _eSubpackages.add(_doubleArrow);
    };
    final EPackage ePackage = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    Assert.assertFalse(
      EdeltaModelUtil.hasCycleInSuperPackage(
        IterableExtensions.<EPackage>head(IterableExtensions.<EPackage>head(ePackage.getESubpackages()).getESubpackages())));
  }
  
  @Test
  public void testHasCycleInSuperPackageWithCycle() {
    final EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;
    EPackage _createEPackage = ecoreFactory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      EList<EPackage> _eSubpackages = it.getESubpackages();
      EPackage _createEPackage_1 = ecoreFactory.createEPackage();
      final Procedure1<EPackage> _function_1 = (EPackage it_1) -> {
        EList<EPackage> _eSubpackages_1 = it_1.getESubpackages();
        EPackage _createEPackage_2 = ecoreFactory.createEPackage();
        _eSubpackages_1.add(_createEPackage_2);
      };
      EPackage _doubleArrow = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage_1, _function_1);
      _eSubpackages.add(_doubleArrow);
    };
    final EPackage ePackage = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    final EPackage subSubPackage = IterableExtensions.<EPackage>head(IterableExtensions.<EPackage>head(ePackage.getESubpackages()).getESubpackages());
    EList<EPackage> _eSubpackages = subSubPackage.getESubpackages();
    _eSubpackages.add(ePackage);
    Assert.assertTrue(
      EdeltaModelUtil.hasCycleInSuperPackage(subSubPackage));
  }
  
  @Test
  public void testFindRootSuperPackage() {
    final EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;
    EPackage _createEPackage = ecoreFactory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      EList<EPackage> _eSubpackages = it.getESubpackages();
      EPackage _createEPackage_1 = ecoreFactory.createEPackage();
      final Procedure1<EPackage> _function_1 = (EPackage it_1) -> {
        EList<EPackage> _eSubpackages_1 = it_1.getESubpackages();
        EPackage _createEPackage_2 = ecoreFactory.createEPackage();
        _eSubpackages_1.add(_createEPackage_2);
      };
      EPackage _doubleArrow = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage_1, _function_1);
      _eSubpackages.add(_doubleArrow);
    };
    final EPackage rootPackage = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    Assertions.<EPackage>assertThat(EdeltaModelUtil.findRootSuperPackage(IterableExtensions.<EPackage>head(IterableExtensions.<EPackage>head(rootPackage.getESubpackages()).getESubpackages()))).isSameAs(rootPackage);
    Assertions.<EPackage>assertThat(EdeltaModelUtil.findRootSuperPackage(IterableExtensions.<EPackage>head(rootPackage.getESubpackages()))).isSameAs(rootPackage);
    Assertions.<EPackage>assertThat(EdeltaModelUtil.findRootSuperPackage(rootPackage)).isNull();
  }
}
