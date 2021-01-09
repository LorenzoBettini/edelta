package edelta.tests;

import com.google.common.base.Objects;
import com.google.inject.Inject;
import edelta.edelta.EdeltaEcoreQualifiedReference;
import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaProgram;
import edelta.interpreter.EdeltaInterpreterRuntimeException;
import edelta.resource.derivedstate.EdeltaCopiedEPackagesMap;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProviderTestableDerivedStateComputer;
import edelta.tests.additional.TestableEdeltaDerivedStateComputer;
import java.util.Collection;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.resource.DerivedStateAwareResource;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderTestableDerivedStateComputer.class)
@SuppressWarnings("all")
public class EdeltaDerivedStateComputerTest extends EdeltaAbstractTest {
  @Inject
  @Extension
  private TestableEdeltaDerivedStateComputer _testableEdeltaDerivedStateComputer;
  
  @Inject
  @Extension
  private EdeltaDerivedStateHelper _edeltaDerivedStateHelper;
  
  @Test
  public void testCopiedEPackages() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package test");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.append("metamodel \"bar\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest1 epackage foo {}");
    _builder.newLine();
    _builder.append("modifyEcore aTest2 epackage bar {}");
    _builder.newLine();
    final EdeltaProgram program = this.parseWithTestEcores(_builder);
    final Collection<EPackage> packages = this._edeltaDerivedStateHelper.getCopiedEPackagesMap(program.eResource()).values();
    final Function1<EPackage, String> _function = (EPackage it) -> {
      return it.getName();
    };
    Assertions.<String>assertThat(IterableExtensions.<EPackage, String>map(packages, _function)).containsExactlyInAnyOrder("foo", "bar");
  }
  
  @Test
  public void testCopiedEPackagesWithSingleModifyEcore() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package test");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.append("metamodel \"bar\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest1 epackage foo {}");
    _builder.newLine();
    final EdeltaProgram program = this.parseWithTestEcores(_builder);
    final Collection<EPackage> packages = this._edeltaDerivedStateHelper.getCopiedEPackagesMap(program.eResource()).values();
    final Function1<EPackage, String> _function = (EPackage it) -> {
      return it.getName();
    };
    Assertions.<String>assertThat(IterableExtensions.<EPackage, String>map(packages, _function)).containsExactlyInAnyOrder("foo", "bar");
  }
  
  @Test
  public void testCopiedEPackagesWhenDuplicateImports() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package test");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest1 epackage foo {}");
    _builder.newLine();
    _builder.append("modifyEcore aTest2 epackage foo {}");
    _builder.newLine();
    final EdeltaProgram program = this.parseWithTestEcore(_builder);
    final Collection<EPackage> packages = this._edeltaDerivedStateHelper.getCopiedEPackagesMap(program.eResource()).values();
    final Function1<EPackage, String> _function = (EPackage it) -> {
      return it.getName();
    };
    Assertions.<String>assertThat(IterableExtensions.<EPackage, String>map(packages, _function)).containsExactly("foo");
  }
  
  @Test
  public void testCopiedEPackagesWhenUnresolvedPackages() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package test");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"unresolved\"");
    _builder.newLine();
    _builder.append("metamodel \"unresolved\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest1 epackage unresolved {}");
    _builder.newLine();
    _builder.append("modifyEcore aTest2 epackage unresolved {}");
    _builder.newLine();
    final EdeltaProgram program = this.parseWithTestEcore(_builder);
    final Collection<EPackage> packages = this._edeltaDerivedStateHelper.getCopiedEPackagesMap(program.eResource()).values();
    Assertions.<EPackage>assertThat(packages).hasSize(1);
  }
  
  @Test
  public void testCopiedEPackagesWithReferences() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package test");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"testecoreforreferences1\"");
    _builder.newLine();
    _builder.append("metamodel \"testecoreforreferences2\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest1 epackage testecoreforreferences1 {}");
    _builder.newLine();
    _builder.append("modifyEcore aTest2 epackage testecoreforreferences2 {}");
    _builder.newLine();
    final EdeltaProgram program = this.parseWithTestEcoresWithReferences(_builder);
    final Collection<EPackage> packages = this._edeltaDerivedStateHelper.getCopiedEPackagesMap(program.eResource()).values();
    Assertions.<EPackage>assertThat(packages).hasSize(2);
    final EPackage testecoreforreferences1 = this.<EPackage>getByName(packages, "testecoreforreferences1");
    final EPackage testecoreforreferences2 = this.<EPackage>getByName(packages, "testecoreforreferences2");
    final EClass person = this.getEClassByName(testecoreforreferences1, "Person");
    final EClass workplace = this.getEClassByName(testecoreforreferences2, "WorkPlace");
    Assert.assertSame(
      this.getEReferenceByName(person, "works").getEOpposite(), 
      this.getEReferenceByName(workplace, "persons"));
    Assert.assertSame(
      this.getEReferenceByName(person, "works"), 
      this.getEReferenceByName(workplace, "persons").getEOpposite());
  }
  
  @Test
  public void testInvalidDirectSubPackageAreNotCopied() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package test");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"mainpackage.mainsubpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mainsubpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final EdeltaProgram program = this.parseWithTestEcoreWithSubPackage(_builder);
    final Collection<EPackage> packages = this._edeltaDerivedStateHelper.getCopiedEPackagesMap(program.eResource()).values();
    Assertions.<EPackage>assertThat(packages).isEmpty();
  }
  
  @Test
  public void testInstallDerivedStateDuringPreIndexingPhase() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package test");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest1 epackage foo {}");
    _builder.newLine();
    final EdeltaProgram program = this.parseWithTestEcore(_builder);
    Resource _eResource = program.eResource();
    final DerivedStateAwareResource resource = ((DerivedStateAwareResource) _eResource);
    Resource _eResource_1 = program.eResource();
    this._testableEdeltaDerivedStateComputer.installDerivedState(((DerivedStateAwareResource) _eResource_1), true);
    EObject _last = IterableExtensions.<EObject>last(resource.getContents());
    Assert.assertEquals("test.__synthetic0", ((JvmGenericType) _last).getIdentifier());
  }
  
  @Test
  public void testDerivedStateForModifyEcoreWithMissingPackage() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package test");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest");
    _builder.newLine();
    final EdeltaProgram program = this.parseWithTestEcore(_builder);
    Resource _eResource = program.eResource();
    final DerivedStateAwareResource resource = ((DerivedStateAwareResource) _eResource);
    EObject _last = IterableExtensions.<EObject>last(resource.getContents());
    Assert.assertEquals("test.__synthetic0", ((JvmGenericType) _last).getIdentifier());
  }
  
  @Test
  public void testDerivedStateIsCorrectlyDiscarded() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package test");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest1 epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"First\")");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final EdeltaProgram program = this.parseWithTestEcore(_builder);
    Resource _eResource = program.eResource();
    final DerivedStateAwareResource resource = ((DerivedStateAwareResource) _eResource);
    Assert.assertEquals("First", this.getLastCopiedEPackageLastEClass(program).getName());
    program.getModifyEcoreOperations().clear();
    resource.discardDerivedState();
    EObject _last = IterableExtensions.<EObject>last(resource.getContents());
    Assert.assertEquals("test.__synthetic0", ((JvmGenericType) _last).getIdentifier());
  }
  
  @Test
  public void testDerivedStateIsCorrectlyDiscardedAndUnloaded() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package test");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest1 epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"First\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(First)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final EdeltaProgram program = this.parseWithTestEcore(_builder);
    Resource _eResource = program.eResource();
    final DerivedStateAwareResource resource = ((DerivedStateAwareResource) _eResource);
    final EClass derivedStateEClass = this.getLastCopiedEPackageLastEClass(program);
    final ENamedElement eclassRef = this.getEdeltaEcoreReferenceExpression(IterableExtensions.<XExpression>last(this.getBlock(this.lastModifyEcoreOperation(program).getBody()).getExpressions())).getReference().getEnamedelement();
    Assert.assertSame(derivedStateEClass, eclassRef);
    Assert.assertEquals("First", derivedStateEClass.getName());
    Assert.assertFalse("should be resolved now", eclassRef.eIsProxy());
    program.getModifyEcoreOperations().remove(0);
    resource.discardDerivedState();
    Assert.assertSame(derivedStateEClass, eclassRef);
    Assert.assertTrue("should be a proxy now", eclassRef.eIsProxy());
  }
  
  @Test
  public void testAdaptersAreRemovedFromDerivedEPackagesAfterUnloading() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package test");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest1 epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"First\")");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final EdeltaProgram program = this.parseWithTestEcore(_builder);
    Resource _eResource = program.eResource();
    final DerivedStateAwareResource resource = ((DerivedStateAwareResource) _eResource);
    final EdeltaCopiedEPackagesMap nameToCopiedEPackageMap = this._edeltaDerivedStateHelper.getCopiedEPackagesMap(resource);
    Assert.assertFalse(resource.eAdapters().isEmpty());
    Assert.assertFalse(nameToCopiedEPackageMap.isEmpty());
    EList<Adapter> _eAdapters = IterableExtensions.<EPackage>head(nameToCopiedEPackageMap.values()).eAdapters();
    AdapterImpl _adapterImpl = new AdapterImpl();
    _eAdapters.add(_adapterImpl);
    final Function1<EPackage, Boolean> _function = (EPackage it) -> {
      boolean _isEmpty = it.eAdapters().isEmpty();
      return Boolean.valueOf((!_isEmpty));
    };
    Assert.assertTrue(IterableExtensions.<EPackage>forall(nameToCopiedEPackageMap.values(), _function));
    this._testableEdeltaDerivedStateComputer.unloadDerivedPackages(nameToCopiedEPackageMap);
    Assert.assertFalse(nameToCopiedEPackageMap.isEmpty());
    Assert.assertFalse(resource.eAdapters().isEmpty());
    final Function1<EPackage, Boolean> _function_1 = (EPackage it) -> {
      return Boolean.valueOf(it.eAdapters().isEmpty());
    };
    Assert.assertTrue(IterableExtensions.<EPackage>forall(nameToCopiedEPackageMap.values(), _function_1));
  }
  
  @Test
  public void testMapsAreClearedAfterDiscarding() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package test");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest1 epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"First\")");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final EdeltaProgram program = this.parseWithTestEcore(_builder);
    Resource _eResource = program.eResource();
    final DerivedStateAwareResource resource = ((DerivedStateAwareResource) _eResource);
    final EdeltaCopiedEPackagesMap nameToCopiedEPackageMap = this._edeltaDerivedStateHelper.getCopiedEPackagesMap(resource);
    Assert.assertFalse(resource.eAdapters().isEmpty());
    Assert.assertFalse(nameToCopiedEPackageMap.isEmpty());
    program.getModifyEcoreOperations().clear();
    resource.discardDerivedState();
    Assert.assertTrue(nameToCopiedEPackageMap.isEmpty());
    Assert.assertFalse(resource.eAdapters().isEmpty());
  }
  
  @Test
  public void testSourceElementOfNull() {
    Assert.assertNull(this._testableEdeltaDerivedStateComputer.getPrimarySourceElement(null));
  }
  
  @Test
  public void testSourceElementOfNotDerived() {
    try {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("package test");
      _builder.newLine();
      Assert.assertNull(
        this._testableEdeltaDerivedStateComputer.getPrimarySourceElement(this.parseHelper.parse(_builder)));
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testCopiedEPackageWithRenamedEClass() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package test");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.FooClass).name = \"Renamed\"");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final EdeltaProgram program = this.parseWithTestEcore(_builder);
    final EClass derivedEClass = this.getLastCopiedEPackageFirstEClass(program);
    Assert.assertEquals("Renamed", derivedEClass.getName());
  }
  
  @Test
  public void testDerivedStateForCreatedEAttributeInChangeEClassWithNewName() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package test");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.FooClass) => [");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("name = \"Renamed\"");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("addNewEAttribute(\"newAttribute\", ecoreref(FooDataType))");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final EdeltaProgram program = this.parseWithTestEcore(_builder);
    final EClass derivedEClass = this.getLastCopiedEPackageFirstEClass(program);
    EStructuralFeature _last = IterableExtensions.<EStructuralFeature>last(derivedEClass.getEStructuralFeatures());
    final EAttribute derivedEAttribute = ((EAttribute) _last);
    Assert.assertEquals("newAttribute", derivedEAttribute.getName());
    Assert.assertEquals("Renamed", derivedEAttribute.getEContainingClass().getName());
  }
  
  @Test
  public void testInterpretedCreateEClassAndStealEAttribute() {
    final EdeltaProgram program = this.parseWithTestEcore(this._inputs.createEClassStealingAttribute());
    final EClass ec = this.getLastCopiedEPackageFirstEClass(program, "NewClass");
    Assert.assertEquals("NewClass", ec.getName());
    final EStructuralFeature attr = IterableExtensions.<EStructuralFeature>head(ec.getEStructuralFeatures());
    Assert.assertEquals("myAttribute", attr.getName());
    this.validationTestHelper.validate(program);
    this.validationTestHelper.assertNoErrors(program);
    final EdeltaEcoreQualifiedReference ecoreref = this.getEcoreRefInManipulationExpressionBlock(program);
    ENamedElement _enamedelement = ecoreref.getQualification().getEnamedelement();
    EClass eClass = ((EClass) _enamedelement);
    ENamedElement _enamedelement_1 = ecoreref.getEnamedelement();
    EAttribute eAttr = ((EAttribute) _enamedelement_1);
    this.assertEClassContainsFeature(eClass, eAttr, false);
    ENamedElement _originalEnamedelement = this._edeltaDerivedStateHelper.getOriginalEnamedelement(ecoreref.getQualification());
    eClass = ((EClass) _originalEnamedelement);
    ENamedElement _originalEnamedelement_1 = this._edeltaDerivedStateHelper.getOriginalEnamedelement(ecoreref);
    eAttr = ((EAttribute) _originalEnamedelement_1);
    this.assertEClassContainsFeature(eClass, eAttr, true);
  }
  
  @Test
  public void testInterpretedRemovedEClassDoesNotTouchTheOriginalEcore() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(FooClass).EPackage.EClassifiers.remove(ecoreref(FooClass))");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final EdeltaProgram program = this.parseWithTestEcore(_builder);
    final EClass derivedEClass = this.getLastCopiedEPackageLastEClass(program);
    Assert.assertEquals("NewClass", derivedEClass.getName());
    this.validationTestHelper.validate(program);
    this.validationTestHelper.assertNoErrors(program);
    final Function1<EClassifier, Boolean> _function = (EClassifier it) -> {
      String _name = it.getName();
      return Boolean.valueOf(Objects.equal(_name, "FooClass"));
    };
    Assert.assertNull(IterableExtensions.<EClassifier>findFirst(IterableExtensions.<EPackage>head(this.getCopiedEPackages(program)).getEClassifiers(), _function));
    final Function1<EClassifier, Boolean> _function_1 = (EClassifier it) -> {
      String _name = it.getName();
      return Boolean.valueOf(Objects.equal(_name, "FooClass"));
    };
    Assert.assertNotNull(IterableExtensions.<EClassifier>findFirst(this.getEPackageByName(program, "foo").getEClassifiers(), _function_1));
  }
  
  @Test
  public void testContentAdapter() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(FooClass).EPackage.EClassifiers.remove(ecoreref(FooClass))");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final EdeltaProgram program = this.parseWithTestEcore(_builder);
    final ThrowableAssert.ThrowingCallable _function = () -> {
      EClassifier _head = IterableExtensions.<EClassifier>head(IterableExtensions.<EPackage>head(program.getMetamodels()).getEClassifiers());
      _head.setName("bar");
    };
    Assertions.assertThatThrownBy(_function).isInstanceOf(EdeltaInterpreterRuntimeException.class).hasMessageContaining("Unexpected notification");
  }
  
  @Test
  public void testPersonListExampleModifyEcore() {
    final EdeltaProgram prog = this.parseWithLoadedEcore((EdeltaAbstractTest.METAMODEL_PATH + EdeltaAbstractTest.PERSON_LIST_ECORE), 
      this._inputs.personListExampleModifyEcore());
    this.validationTestHelper.assertNoErrors(prog);
  }
  
  private EdeltaEcoreQualifiedReference getEcoreRefInManipulationExpressionBlock(final EdeltaProgram program) {
    return this.getEdeltaEcoreQualifiedReference(IterableExtensions.<EdeltaEcoreReferenceExpression>last(EcoreUtil2.<EdeltaEcoreReferenceExpression>getAllContentsOfType(this.lastModifyEcoreOperation(program).getBody(), EdeltaEcoreReferenceExpression.class)).getReference());
  }
  
  private void assertEClassContainsFeature(final EClass c, final EStructuralFeature f, final boolean expected) {
    Assert.assertEquals(Boolean.valueOf(expected), 
      Boolean.valueOf(c.getEStructuralFeatures().contains(f)));
  }
}
