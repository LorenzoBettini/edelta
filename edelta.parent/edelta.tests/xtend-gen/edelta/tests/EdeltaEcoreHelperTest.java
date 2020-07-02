package edelta.tests;

import com.google.inject.Inject;
import edelta.edelta.EdeltaProgram;
import edelta.resource.derivedstate.EdeltaAccessibleElements;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProviderCustom;
import edelta.util.EdeltaEcoreHelper;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
@SuppressWarnings("all")
public class EdeltaEcoreHelperTest extends EdeltaAbstractTest {
  @Inject
  @Extension
  private EdeltaEcoreHelper _edeltaEcoreHelper;
  
  @Test
  public void testProgramENamedElements() {
    Iterable<? extends ENamedElement> _programENamedElements = this._edeltaEcoreHelper.getProgramENamedElements(this.parseWithTestEcores(this._inputs.referencesToMetamodels()));
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("FooClass");
    _builder.newLine();
    _builder.append("FooDataType");
    _builder.newLine();
    _builder.append("FooEnum");
    _builder.newLine();
    _builder.append("myAttribute");
    _builder.newLine();
    _builder.append("myReference");
    _builder.newLine();
    _builder.append("FooEnumLiteral");
    _builder.newLine();
    _builder.append("BarClass");
    _builder.newLine();
    _builder.append("BarDataType");
    _builder.newLine();
    _builder.append("myAttribute");
    _builder.newLine();
    _builder.append("myReference");
    _builder.newLine();
    _builder.append("foo");
    _builder.newLine();
    _builder.append("bar");
    _builder.newLine();
    this.assertNamedElements(_programENamedElements, _builder);
  }
  
  @Test
  public void testProgramENamedElementsWithSubPackages() {
    Iterable<? extends ENamedElement> _programENamedElements = this._edeltaEcoreHelper.getProgramENamedElements(this.parseWithTestEcoreWithSubPackage(this._inputs.referenceToMetamodelWithSubPackage()));
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("MainFooClass");
    _builder.newLine();
    _builder.append("MainFooDataType");
    _builder.newLine();
    _builder.append("MainFooEnum");
    _builder.newLine();
    _builder.append("MyClass");
    _builder.newLine();
    _builder.append("myAttribute");
    _builder.newLine();
    _builder.append("myReference");
    _builder.newLine();
    _builder.append("FooEnumLiteral");
    _builder.newLine();
    _builder.append("myClassAttribute");
    _builder.newLine();
    _builder.append("mainsubpackage");
    _builder.newLine();
    _builder.append("MainSubPackageFooClass");
    _builder.newLine();
    _builder.append("MyClass");
    _builder.newLine();
    _builder.append("mySubPackageAttribute");
    _builder.newLine();
    _builder.append("mySubPackageReference");
    _builder.newLine();
    _builder.append("myClassAttribute");
    _builder.newLine();
    _builder.append("subsubpackage");
    _builder.newLine();
    _builder.append("MyClass");
    _builder.newLine();
    _builder.append("mainpackage");
    _builder.newLine();
    this.assertNamedElements(_programENamedElements, _builder);
  }
  
  @Test
  public void testProgramWithCreatedEClassENamedElements() {
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(this._inputs.referenceToCreatedEClass());
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      Iterable<? extends ENamedElement> _programENamedElements = this._edeltaEcoreHelper.getProgramENamedElements(it);
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("FooClass");
      _builder.newLine();
      _builder.append("FooDataType");
      _builder.newLine();
      _builder.append("FooEnum");
      _builder.newLine();
      _builder.append("NewClass");
      _builder.newLine();
      _builder.append("myAttribute");
      _builder.newLine();
      _builder.append("myReference");
      _builder.newLine();
      _builder.append("FooEnumLiteral");
      _builder.newLine();
      _builder.append("FooClass");
      _builder.newLine();
      _builder.append("FooDataType");
      _builder.newLine();
      _builder.append("FooEnum");
      _builder.newLine();
      _builder.append("myAttribute");
      _builder.newLine();
      _builder.append("myReference");
      _builder.newLine();
      _builder.append("FooEnumLiteral");
      _builder.newLine();
      _builder.append("foo");
      _builder.newLine();
      this.assertNamedElements(_programENamedElements, _builder);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testProgramCopiedENamedElementsWithCreatedEClass() {
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(this._inputs.referenceToCreatedEClass());
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      EdeltaAccessibleElements _computeAccessibleElements = this._edeltaEcoreHelper.computeAccessibleElements(it);
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("foo.FooClass.myAttribute");
      _builder.newLine();
      _builder.append("foo.FooClass.myReference");
      _builder.newLine();
      _builder.append("foo");
      _builder.newLine();
      _builder.append("foo.NewClass");
      _builder.newLine();
      _builder.append("foo.FooEnum");
      _builder.newLine();
      _builder.append("foo.FooEnum.FooEnumLiteral");
      _builder.newLine();
      _builder.append("foo.FooClass");
      _builder.newLine();
      _builder.append("foo.FooDataType");
      _builder.newLine();
      this.assertQualifiedNames(_computeAccessibleElements, _builder);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testProgramCopiedENamedElementsWithRemovedEClass() {
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(this._inputs.referenceToEClassRemoved());
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      EdeltaAccessibleElements _computeAccessibleElements = this._edeltaEcoreHelper.computeAccessibleElements(it);
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("foo");
      _builder.newLine();
      _builder.append("foo.FooEnum");
      _builder.newLine();
      _builder.append("foo.FooEnum.FooEnumLiteral");
      _builder.newLine();
      _builder.append("foo.FooDataType");
      _builder.newLine();
      this.assertQualifiedNames(_computeAccessibleElements, _builder);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testEPackageENamedElements() {
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(this._inputs.referenceToMetamodel());
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      Iterable<? extends ENamedElement> _eNamedElements = this._edeltaEcoreHelper.getENamedElements(this.getEPackageByName(it, "foo"), it);
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("FooClass");
      _builder.newLine();
      _builder.append("FooDataType");
      _builder.newLine();
      _builder.append("FooEnum");
      _builder.newLine();
      this.assertNamedElements(_eNamedElements, _builder);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testEPackageENamedElementsWithSubPackages() {
    EdeltaProgram _parseWithTestEcoreWithSubPackage = this.parseWithTestEcoreWithSubPackage(this._inputs.referenceToMetamodelWithSubPackage());
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      Iterable<? extends ENamedElement> _eNamedElements = this._edeltaEcoreHelper.getENamedElements(this.getEPackageByName(it, "mainpackage"), it);
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("MainFooClass");
      _builder.newLine();
      _builder.append("MainFooDataType");
      _builder.newLine();
      _builder.append("MainFooEnum");
      _builder.newLine();
      _builder.append("MyClass");
      _builder.newLine();
      _builder.append("mainsubpackage");
      _builder.newLine();
      this.assertNamedElements(_eNamedElements, _builder);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcoreWithSubPackage, _function);
  }
  
  @Test
  public void testEPackageENamedElementsWithNewSubPackages() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewESubpackage(\"anewsubpackage\", \"aprefix\", \"aURI\") [");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("addNewEClass(\"AddedInSubpackage\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      Iterable<? extends ENamedElement> _eNamedElements = this._edeltaEcoreHelper.getENamedElements(
        IterableExtensions.<EPackage>head(IterableExtensions.<EPackage>head(this.getCopiedEPackages(it)).getESubpackages()), it);
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("AddedInSubpackage");
      _builder_1.newLine();
      this.assertNamedElements(_eNamedElements, _builder_1);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testSubPackageEPackageENamedElementsWithSubPackages() {
    EdeltaProgram _parseWithTestEcoreWithSubPackage = this.parseWithTestEcoreWithSubPackage(this._inputs.referenceToMetamodelWithSubPackage());
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      Iterable<? extends ENamedElement> _eNamedElements = this._edeltaEcoreHelper.getENamedElements(
        IterableExtensions.<EPackage>head(this.getEPackageByName(it, "mainpackage").getESubpackages()), it);
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("MainSubPackageFooClass");
      _builder.newLine();
      _builder.append("MyClass");
      _builder.newLine();
      _builder.append("subsubpackage");
      _builder.newLine();
      this.assertNamedElements(_eNamedElements, _builder);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcoreWithSubPackage, _function);
  }
  
  @Test
  public void testSubSubPackageEPackageENamedElementsWithSubPackages() {
    EdeltaProgram _parseWithTestEcoreWithSubPackage = this.parseWithTestEcoreWithSubPackage(this._inputs.referenceToMetamodelWithSubPackage());
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      Iterable<? extends ENamedElement> _eNamedElements = this._edeltaEcoreHelper.getENamedElements(
        IterableExtensions.<EPackage>head(IterableExtensions.<EPackage>head(this.getEPackageByName(it, "mainpackage").getESubpackages()).getESubpackages()), it);
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("MyClass");
      _builder.newLine();
      this.assertNamedElements(_eNamedElements, _builder);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcoreWithSubPackage, _function);
  }
  
  @Test
  public void testEPackageENamedElementsWithCycleInSubPackages() {
    EdeltaProgram _parseWithTestEcoreWithSubPackage = this.parseWithTestEcoreWithSubPackage(this._inputs.referenceToMetamodelWithSubPackage());
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final EPackage mainpackage = this.getEPackageByName(it, "mainpackage");
      final EPackage subsubpackage = IterableExtensions.<EPackage>head(IterableExtensions.<EPackage>head(mainpackage.getESubpackages()).getESubpackages());
      EList<EPackage> _eSubpackages = subsubpackage.getESubpackages();
      _eSubpackages.add(mainpackage);
      Iterable<? extends ENamedElement> _eNamedElements = this._edeltaEcoreHelper.getENamedElements(subsubpackage, it);
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("MyClass");
      _builder.newLine();
      _builder.append("mainpackage");
      _builder.newLine();
      _builder.append("MyClass");
      _builder.newLine();
      _builder.append("mainpackage");
      _builder.newLine();
      this.assertNamedElements(_eNamedElements, _builder);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcoreWithSubPackage, _function);
  }
  
  @Test
  public void testENamedElementsWithWithTheSameNameInSubPackages() {
    EdeltaProgram _parseWithTestEcoreWithSubPackage = this.parseWithTestEcoreWithSubPackage(this._inputs.referenceToMetamodelWithSubPackage());
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final EPackage mainpackage = this.getEPackageByName(it, "mainpackage");
      final EPackage subsubpackage = IterableExtensions.<EPackage>head(IterableExtensions.<EPackage>head(mainpackage.getESubpackages()).getESubpackages());
      Iterable<? extends ENamedElement> _eNamedElements = this._edeltaEcoreHelper.getENamedElements(this.getEClassiferByName(subsubpackage, "MyClass"), it);
      StringConcatenation _builder = new StringConcatenation();
      _builder.newLine();
      this.assertNamedElements(_eNamedElements, _builder);
      Iterable<? extends ENamedElement> _eNamedElements_1 = this._edeltaEcoreHelper.getENamedElements(this.getEClassiferByName(mainpackage, "MyClass"), it);
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("myClassAttribute");
      _builder_1.newLine();
      this.assertNamedElements(_eNamedElements_1, _builder_1);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcoreWithSubPackage, _function);
  }
  
  @Test
  public void testEPackageENamedElementsWithCreatedEClass() {
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(this._inputs.referenceToCreatedEClass());
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      Iterable<? extends ENamedElement> _eNamedElements = this._edeltaEcoreHelper.getENamedElements(this.getEPackageByName(it, "foo"), it);
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("FooClass");
      _builder.newLine();
      _builder.append("FooDataType");
      _builder.newLine();
      _builder.append("FooEnum");
      _builder.newLine();
      _builder.append("NewClass");
      _builder.newLine();
      _builder.append("FooClass");
      _builder.newLine();
      _builder.append("FooDataType");
      _builder.newLine();
      _builder.append("FooEnum");
      _builder.newLine();
      this.assertNamedElements(_eNamedElements, _builder);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testEPackageENamedElementsWithCreatedEClassWithoutCopiedEPackages() {
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(this._inputs.referenceToCreatedEClass());
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      Iterable<? extends ENamedElement> _eNamedElementsWithoutCopiedEPackages = this._edeltaEcoreHelper.getENamedElementsWithoutCopiedEPackages(this.getEPackageByName(it, "foo"), it);
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("FooClass");
      _builder.newLine();
      _builder.append("FooDataType");
      _builder.newLine();
      _builder.append("FooEnum");
      _builder.newLine();
      this.assertNamedElements(_eNamedElementsWithoutCopiedEPackages, _builder);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testEDataTypeENamedElements() {
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(this._inputs.referenceToMetamodel());
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      Iterable<? extends ENamedElement> _eNamedElements = this._edeltaEcoreHelper.getENamedElements(this.getEClassifierByName(it, "foo", "FooDataType"), it);
      StringConcatenation _builder = new StringConcatenation();
      _builder.newLine();
      this.assertNamedElements(_eNamedElements, _builder);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testENumENamedElements() {
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(this._inputs.referenceToMetamodel());
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      Iterable<? extends ENamedElement> _eNamedElements = this._edeltaEcoreHelper.getENamedElements(this.getEClassifierByName(it, "foo", "FooEnum"), it);
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("FooEnumLiteral");
      _builder.newLine();
      this.assertNamedElements(_eNamedElements, _builder);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testENumENamedElementsWithCreatedEClass() {
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(this._inputs.referenceToCreatedEClass());
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      Iterable<? extends ENamedElement> _eNamedElements = this._edeltaEcoreHelper.getENamedElements(this.getEClassifierByName(it, "foo", "FooEnum"), it);
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("FooEnumLiteral");
      _builder.newLine();
      _builder.append("FooEnumLiteral");
      _builder.newLine();
      this.assertNamedElements(_eNamedElements, _builder);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testENumENamedElementsWithCreatedEClassWithoutCopiedEPackages() {
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(this._inputs.referenceToCreatedEClass());
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      Iterable<? extends ENamedElement> _eNamedElementsWithoutCopiedEPackages = this._edeltaEcoreHelper.getENamedElementsWithoutCopiedEPackages(this.getEClassifierByName(it, "foo", "FooEnum"), it);
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("FooEnumLiteral");
      _builder.newLine();
      this.assertNamedElements(_eNamedElementsWithoutCopiedEPackages, _builder);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testNullENamedElements() {
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(this._inputs.referenceToMetamodel());
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      Assert.assertTrue(IterableExtensions.isEmpty(this._edeltaEcoreHelper.getENamedElements(null, it)));
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testEClassENamedElements() {
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(this._inputs.referenceToMetamodel());
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      Iterable<? extends ENamedElement> _eNamedElements = this._edeltaEcoreHelper.getENamedElements(this.getEClassifierByName(it, "foo", "FooClass"), it);
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("myAttribute");
      _builder.newLine();
      _builder.append("myReference");
      _builder.newLine();
      this.assertNamedElements(_eNamedElements, _builder);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testEClassENamedElementsWithCreatedEClass() {
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(this._inputs.referenceToCreatedEClass());
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      Iterable<? extends ENamedElement> _eNamedElements = this._edeltaEcoreHelper.getENamedElements(this.getEClassifierByName(it, "foo", "FooClass"), it);
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("myAttribute");
      _builder.newLine();
      _builder.append("myReference");
      _builder.newLine();
      _builder.append("myAttribute");
      _builder.newLine();
      _builder.append("myReference");
      _builder.newLine();
      this.assertNamedElements(_eNamedElements, _builder);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testEClassENamedElementsWithCreatedEClassWithoutCopiedEPackages() {
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(this._inputs.referenceToCreatedEClass());
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      Iterable<? extends ENamedElement> _eNamedElementsWithoutCopiedEPackages = this._edeltaEcoreHelper.getENamedElementsWithoutCopiedEPackages(this.getEClassifierByName(it, "foo", "FooClass"), it);
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("myAttribute");
      _builder.newLine();
      _builder.append("myReference");
      _builder.newLine();
      this.assertNamedElements(_eNamedElementsWithoutCopiedEPackages, _builder);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testGetAllEClasses() {
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(this._inputs.referenceToMetamodel());
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      Iterable<EClass> _allEClasses = this._edeltaEcoreHelper.getAllEClasses(this.getEPackageByName(it, "foo"));
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("FooClass");
      _builder.newLine();
      this.assertNamedElements(_allEClasses, _builder);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
}
