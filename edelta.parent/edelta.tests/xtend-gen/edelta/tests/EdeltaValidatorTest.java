package edelta.tests;

import edelta.edelta.EdeltaPackage;
import edelta.edelta.EdeltaProgram;
import edelta.lib.AbstractEdelta;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.injectors.EdeltaInjectorProviderCustom;
import edelta.validation.EdeltaValidator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.diagnostics.Diagnostic;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.eclipse.xtext.xbase.validation.IssueCodes;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
@SuppressWarnings("all")
public class EdeltaValidatorTest extends EdeltaAbstractTest {
  @Test
  public void testEmptyProgram() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    this.validationTestHelper.assertNoErrors(this.parseHelper.parse(_builder));
  }
  
  @Test
  public void testCanReferToMetamodel() throws Exception {
    this.validationTestHelper.assertNoErrors(this.parseWithTestEcore(this.inputs.referenceToMetamodel()));
  }
  
  @Test
  public void testUseImportedJavaTypes() throws Exception {
    this.validationTestHelper.assertNoErrors(this.parseHelper.parse(this.inputs.useImportedJavaTypes()));
  }
  
  @Test
  public void testReferenceToCreatedEClass() throws Exception {
    this.validationTestHelper.assertNoErrors(this.parseWithTestEcore(this.inputs.referenceToCreatedEClass()));
  }
  
  @Test
  public void testReferenceToCreatedEAttribute() throws Exception {
    this.validationTestHelper.assertNoErrors(this.parseWithTestEcore(this.inputs.referenceToCreatedEAttributeRenamed()));
  }
  
  @Test
  public void testValidUseAs() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import edelta.tests.additional.MyCustomEdelta;");
    _builder.newLine();
    _builder.append("use MyCustomEdelta as foo");
    _builder.newLine();
    this.validationTestHelper.assertNoIssues(this.parseHelper.parse(_builder));
  }
  
  @Test
  public void testInvalidUseAsNotAnEdelta() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import java.util.List;");
    _builder.newLine();
    _builder.append("use List as foo");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parse = this.parseHelper.parse(input);
    int _lastIndexOf = input.lastIndexOf("List");
    String _name = AbstractEdelta.class.getName();
    String _plus = ("Not a valid type: must be an " + _name);
    this.validationTestHelper.assertError(_parse, 
      EdeltaPackage.Literals.EDELTA_USE_AS, 
      EdeltaValidator.TYPE_MISMATCH, _lastIndexOf, 4, _plus);
  }
  
  @Test
  public void testInvalidUseAsAbstractEdelta() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import edelta.tests.additional.MyCustomAbstractEdelta;");
    _builder.newLine();
    _builder.append("use MyCustomAbstractEdelta as foo");
    _builder.newLine();
    final String input = _builder.toString();
    this.validationTestHelper.assertError(this.parseHelper.parse(input), 
      EdeltaPackage.Literals.EDELTA_USE_AS, 
      EdeltaValidator.TYPE_MISMATCH, 
      input.lastIndexOf("MyCustomAbstractEdelta"), "MyCustomAbstractEdelta".length(), 
      "Cannot be an abstract type");
  }
  
  @Test
  public void testInvalidUseAsUnresolvedProxy() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("use Unknown as foo");
    _builder.newLine();
    final String input = _builder.toString();
    this.assertErrorsAsStrings(this.parseHelper.parse(input), 
      "Unknown cannot be resolved to a type.");
  }
  
  @Test
  public void testUnresolvedEcoreReference() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(NonExistant)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(FooClass)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    this.assertErrorsAsStrings(this.parseWithTestEcore(_builder), "NonExistant cannot be resolved.");
  }
  
  @Test
  public void testNoDanglingReferencesAfterInterpretation() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.FooClass).EPackage.EClassifiers.remove(ecoreref(foo.FooClass))");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    this.validationTestHelper.assertNoErrors(this.parseWithTestEcore(_builder));
  }
  
  @Test
  public void testCallMethodOnRenanedEClassInModifyEcore() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.FooClass).name = \"RenamedClass\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(RenamedClass).getEAllStructuralFeatures");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final EdeltaProgram prog = this.parseWithTestEcore(_builder);
    this.validationTestHelper.assertNoErrors(prog);
  }
  
  @Test
  public void testCallMethodOnQualifiedRenanedEClassInModifyEcore() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.FooClass).name = \"RenamedClass\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.RenamedClass).getEAllStructuralFeatures");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final EdeltaProgram prog = this.parseWithTestEcore(_builder);
    this.validationTestHelper.assertNoErrors(prog);
  }
  
  @Test
  public void testCallNonExistingMethodOnRenanedEClassInModifyEcore() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.FooClass).name = \"RenamedClass\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(RenamedClass).nonExistant(\"an arg\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(RenamedClass).sugarSet = \"an arg\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("\"a string\".sugarSet = \"an arg\"");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final EdeltaProgram prog = this.parseWithTestEcore(_builder);
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("The method nonExistant(String) is undefined for the type EClass");
    _builder_1.newLine();
    _builder_1.append("The method sugarSet(String) is undefined for the type EClass");
    _builder_1.newLine();
    _builder_1.append("The method sugarSet(String) is undefined for the type String");
    _builder_1.newLine();
    this.assertErrorsAsStrings(prog, _builder_1);
  }
  
  @Test
  public void testReferenceToAddedAttributeofRenamedClassInModifyEcore() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.FooClass).name = \"RenamedClass\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(RenamedClass).EStructuralFeatures.add(");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("newEAttribute(\"addedAttribute\", ecoreref(FooDataType)))");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(RenamedClass.addedAttribute)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    this.validationTestHelper.assertNoErrors(this.parseWithTestEcore(_builder));
  }
  
  @Test
  public void testReferenceToAddedAttributeofRenamedClassInModifyEcore2() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EClass");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.FooClass).name = \"RenamedClass\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(RenamedClass).EStructuralFeatures += newEAttribute(\"added\", ecoreref(FooDataType))");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(RenamedClass.added)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    this.validationTestHelper.assertNoErrors(this.parseWithTestEcore(_builder));
  }
  
  @Test
  public void testReferenceToRenamedClassInModifyEcore() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EClass");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.FooClass).name = \"RenamedClass\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.RenamedClass) => [abstract = true]");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.RenamedClass).setAbstract(true)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.RenamedClass).abstract = true");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    this.validationTestHelper.assertNoErrors(this.parseWithTestEcore(_builder));
  }
  
  @Test
  public void testReferenceToUnknownEPackageInModifyEcore() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EClass");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    this.validationTestHelper.assertError(this.parseWithTestEcore(_builder), 
      EdeltaPackage.eINSTANCE.getEdeltaModifyEcoreOperation(), 
      Diagnostic.LINKING_DIAGNOSTIC, 
      "foo cannot be resolved.");
  }
  
  @Test
  public void testValidLibMethodsInModifyEcore() throws Exception {
    this.validationTestHelper.assertNoErrors(this.parseWithTestEcore(this.inputs.modifyEcoreUsingLibMethods()));
  }
  
  @Test
  public void testDuplicateDeclarations() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import java.util.List");
    _builder.newLine();
    _builder.append("import org.eclipse.emf.ecore.EPackage");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("def myFun(List<Integer> l) {}");
    _builder.newLine();
    _builder.append("def myFun(List<String> l) {}");
    _builder.newLine();
    _builder.append("def anotherFun(List<String> l) {} // OK, different params");
    _builder.newLine();
    _builder.append("def anotherDuplicate(EPackage p) {} // conflicts with modifyEcore");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {}");
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {}");
    _builder.newLine();
    _builder.append("modifyEcore anotherDuplicate epackage foo {} // implicit Java method param: EPackage");
    _builder.newLine();
    _builder.append("modifyEcore anotherFun epackage foo {} // OK, different params");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      this.validationTestHelper.assertError(it, 
        EdeltaPackage.eINSTANCE.getEdeltaOperation(), 
        EdeltaValidator.DUPLICATE_DECLARATION, 
        input.indexOf("anotherDuplicate"), "anotherDuplicate".length(), 
        "Duplicate definition \'anotherDuplicate\'");
      this.validationTestHelper.assertError(it, 
        EdeltaPackage.eINSTANCE.getEdeltaModifyEcoreOperation(), 
        EdeltaValidator.DUPLICATE_DECLARATION, 
        input.lastIndexOf("anotherDuplicate"), "anotherDuplicate".length(), 
        "Duplicate definition \'anotherDuplicate\'");
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("Duplicate definition \'aTest\'");
      _builder_1.newLine();
      _builder_1.append("Duplicate definition \'aTest\'");
      _builder_1.newLine();
      _builder_1.append("Duplicate definition \'anotherDuplicate\'");
      _builder_1.newLine();
      _builder_1.append("Duplicate definition \'anotherDuplicate\'");
      _builder_1.newLine();
      _builder_1.append("Duplicate definition \'myFun\'");
      _builder_1.newLine();
      _builder_1.append("Duplicate definition \'myFun\'");
      _builder_1.newLine();
      this.assertErrorsAsStrings(it, _builder_1);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testDuplicateMetamodelImport() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.append("metamodel \"bar\"");
    _builder.newLine();
    _builder.append("metamodel \"nonexistent\"");
    _builder.newLine();
    _builder.append("metamodel \"nonexistent\" // also check unresolved imports");
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcores = this.parseWithTestEcores(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      this.validationTestHelper.assertError(it, 
        EdeltaPackage.eINSTANCE.getEdeltaProgram(), 
        EdeltaValidator.DUPLICATE_METAMODEL_IMPORT, 
        input.lastIndexOf("\"nonexistent\""), "\"nonexistent\"".length(), 
        "Duplicate metamodel import \"nonexistent\"");
      this.validationTestHelper.assertError(it, 
        EdeltaPackage.eINSTANCE.getEdeltaProgram(), 
        EdeltaValidator.DUPLICATE_METAMODEL_IMPORT, 
        input.lastIndexOf("\"foo\""), "\"foo\"".length(), 
        "Duplicate metamodel import \"foo\"");
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcores, _function);
  }
  
  @Test
  public void testInvalidSubPackageImportedMetamodel() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mainpackage.mainsubpackage\"");
    _builder.newLine();
    final String input = _builder.toString();
    final int start = input.indexOf("\"");
    EdeltaProgram _parseWithTestEcoreWithSubPackage = this.parseWithTestEcoreWithSubPackage(input);
    EClass _edeltaProgram = EdeltaPackage.eINSTANCE.getEdeltaProgram();
    int _lastIndexOf = input.lastIndexOf("\"");
    int _minus = (_lastIndexOf - start);
    int _plus = (_minus + 1);
    this.validationTestHelper.assertError(_parseWithTestEcoreWithSubPackage, _edeltaProgram, 
      EdeltaValidator.INVALID_SUBPACKAGE_IMPORT, start, _plus, 
      "Invalid subpackage import \'mainsubpackage\'");
  }
  
  @Test
  public void testInvalidModifyEcoreOfSubPackage() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mainpackage.mainsubpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mainsubpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    final int start = input.lastIndexOf("mainsubpackage");
    EdeltaProgram _parseWithTestEcoreWithSubPackage = this.parseWithTestEcoreWithSubPackage(input);
    EClass _edeltaModifyEcoreOperation = EdeltaPackage.eINSTANCE.getEdeltaModifyEcoreOperation();
    int _indexOf = input.indexOf(" {");
    int _minus = (_indexOf - start);
    this.validationTestHelper.assertError(_parseWithTestEcoreWithSubPackage, _edeltaModifyEcoreOperation, 
      EdeltaValidator.INVALID_SUBPACKAGE_MODIFICATION, start, _minus, 
      "Invalid direct subpackage modification \'mainsubpackage\'");
  }
  
  @Test
  public void testTypeMismatchOfEcoreRefExp() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EClass");
    _builder.newLine();
    _builder.append("import org.eclipse.emf.ecore.EPackage");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.FooClass).name = \"RenamedClass\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("val EClass c = ecoreref(RenamedClass) // OK after interpretation");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("val EPackage p = ecoreref(RenamedClass) // ERROR also after interpretation");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      this.validationTestHelper.assertError(it, 
        EdeltaPackage.Literals.EDELTA_ECORE_REFERENCE_EXPRESSION, 
        IssueCodes.INCOMPATIBLE_TYPES, 
        input.lastIndexOf("ecoreref(RenamedClass)"), "ecoreref(RenamedClass)".length(), 
        "Type mismatch: cannot convert from EClass to EPackage");
      this.assertErrorsAsStrings(it, "Type mismatch: cannot convert from EClass to EPackage");
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testAccessToNotYetExistingElement() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(ANewClass) // doesn\'t exist yet");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(NonExisting) // doesn\'t exist at all");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"ANewClass\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(ANewClass) // this is OK");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      this.validationTestHelper.assertError(it, 
        EdeltaPackage.Literals.EDELTA_ECORE_DIRECT_REFERENCE, 
        EdeltaValidator.INTERPRETER_ACCESS_NOT_YET_EXISTING_ELEMENT, 
        input.indexOf("ANewClass"), 
        "ANewClass".length(), 
        "Element not yet available in this context: foo.ANewClass");
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("Element not yet available in this context: foo.ANewClass");
      _builder_1.newLine();
      _builder_1.append("NonExisting cannot be resolved.");
      _builder_1.newLine();
      this.assertErrorsAsStrings(it, _builder_1);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testAccessToNotYetExistingElementInComplexExpression() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// doesn\'t exist yet");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(ANewClass).ESuperTypes = ");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("ecoreref(ANewSuperClass) // doesn\'t exist yet");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"ANewClass\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"ANewSuperClass\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(ANewClass) // this is OK");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(ANewSuperClass) // this is OK");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("Element not yet available in this context: foo.ANewClass");
      _builder_1.newLine();
      _builder_1.append("Element not yet available in this context: foo.ANewSuperClass");
      _builder_1.newLine();
      _builder_1.append("The method ESuperTypes(EClass) is undefined for the type EClass");
      _builder_1.newLine();
      this.assertErrorsAsStrings(it, _builder_1);
      this.validationTestHelper.assertError(it, 
        EdeltaPackage.Literals.EDELTA_ECORE_DIRECT_REFERENCE, 
        EdeltaValidator.INTERPRETER_ACCESS_NOT_YET_EXISTING_ELEMENT, 
        input.indexOf("ANewClass"), 
        "ANewClass".length(), 
        "Element not yet available in this context: foo.ANewClass");
      this.validationTestHelper.assertError(it, 
        EdeltaPackage.Literals.EDELTA_ECORE_DIRECT_REFERENCE, 
        EdeltaValidator.INTERPRETER_ACCESS_NOT_YET_EXISTING_ELEMENT, 
        input.indexOf("ANewSuperClass"), 
        "ANewSuperClass".length(), 
        "Element not yet available in this context: foo.ANewSuperClass");
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testAccessToNotYetExistingElementInComplexExpression2() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// doesn\'t exist yet");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(ANewClass).ESuperTypes += ");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("ecoreref(ANewSuperClass) // doesn\'t exist yet");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"ANewClass\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"ANewSuperClass\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(ANewClass) // this is OK");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(ANewSuperClass) // this is OK");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("Element not yet available in this context: foo.ANewClass");
      _builder_1.newLine();
      _builder_1.append("Element not yet available in this context: foo.ANewSuperClass");
      _builder_1.newLine();
      this.assertErrorsAsStrings(it, _builder_1);
      this.validationTestHelper.assertError(it, 
        EdeltaPackage.Literals.EDELTA_ECORE_DIRECT_REFERENCE, 
        EdeltaValidator.INTERPRETER_ACCESS_NOT_YET_EXISTING_ELEMENT, 
        input.indexOf("ANewClass"), 
        "ANewClass".length(), 
        "Element not yet available in this context: foo.ANewClass");
      this.validationTestHelper.assertError(it, 
        EdeltaPackage.Literals.EDELTA_ECORE_DIRECT_REFERENCE, 
        EdeltaValidator.INTERPRETER_ACCESS_NOT_YET_EXISTING_ELEMENT, 
        input.indexOf("ANewSuperClass"), 
        "ANewSuperClass".length(), 
        "Element not yet available in this context: foo.ANewSuperClass");
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
}
