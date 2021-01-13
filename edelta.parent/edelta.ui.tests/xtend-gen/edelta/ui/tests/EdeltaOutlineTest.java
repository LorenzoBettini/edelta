package edelta.ui.tests;

import com.google.inject.Inject;
import edelta.ui.internal.EdeltaActivator;
import edelta.ui.tests.EdeltaUiInjectorProvider;
import edelta.ui.tests.utils.EdeltaPluginProjectHelper;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.testing.Flaky;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.testing.AbstractOutlineTest;
import org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.InputOutput;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaUiInjectorProvider.class)
@SuppressWarnings("all")
public class EdeltaOutlineTest extends AbstractOutlineTest {
  @Inject
  private EdeltaPluginProjectHelper edeltaProjectHelper;
  
  @Rule
  public Flaky.Rule testRule = new Flaky.Rule();
  
  @Override
  protected String getEditorId() {
    return EdeltaActivator.EDELTA_EDELTA;
  }
  
  @Override
  protected IJavaProject createjavaProject(final String projectName) throws CoreException {
    try {
      return this.edeltaProjectHelper.createEdeltaPluginProject(AbstractOutlineTest.TEST_PROJECT);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testOutlineWithNoContents() {
    try {
      StringConcatenation _builder = new StringConcatenation();
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("test");
      _builder_1.newLine();
      this.assertAllLabels(_builder, _builder_1);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testOutlineWithOperation() {
    try {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("def createClass(String name) {");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("newEClass(name)");
      _builder.newLine();
      _builder.append("}");
      _builder.newLine();
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("test");
      _builder_1.newLine();
      _builder_1.append("  ");
      _builder_1.append("createClass(String) : EClass");
      _builder_1.newLine();
      this.assertAllLabels(_builder, _builder_1);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  @Flaky
  public void testOutlineWithCreateEClassInModifyEcore() {
    try {
      InputOutput.<String>println("*** Executing testOutlineWithCreateEClassInModifyEcore...");
      IResourcesSetupUtil.waitForBuild();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("import org.eclipse.emf.ecore.EClass");
      _builder.newLine();
      _builder.newLine();
      _builder.append("metamodel \"mypackage\"");
      _builder.newLine();
      _builder.append("// don\'t rely on ecore, since the input files are not saved");
      _builder.newLine();
      _builder.append("// during the test, thus external libraries are not seen");
      _builder.newLine();
      _builder.append("// metamodel \"ecore\"");
      _builder.newLine();
      _builder.newLine();
      _builder.append("def myNewAttribute(EClass c, String name) {");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("c.EStructuralFeatures += newEAttribute(name, ecoreref(MyDataType))");
      _builder.newLine();
      _builder.append("}");
      _builder.newLine();
      _builder.newLine();
      _builder.append("modifyEcore aModification epackage mypackage {");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("EClassifiers += newEClass(\"A\")");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("myNewAttribute(ecoreref(A), \"foo\")");
      _builder.newLine();
      _builder.append("}");
      _builder.newLine();
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("test");
      _builder_1.newLine();
      _builder_1.append("  ");
      _builder_1.append("myNewAttribute(EClass, String) : boolean");
      _builder_1.newLine();
      _builder_1.append("  ");
      _builder_1.append("aModification(EPackage) : void");
      _builder_1.newLine();
      _builder_1.append("  ");
      _builder_1.append("mypackage");
      _builder_1.newLine();
      _builder_1.append("    ");
      CharSequence _allOtherContents = this.allOtherContents();
      _builder_1.append(_allOtherContents, "    ");
      _builder_1.newLineIfNotEmpty();
      _builder_1.append("    ");
      _builder_1.append("A");
      _builder_1.newLine();
      _builder_1.append("      ");
      _builder_1.append("foo : MyDataType");
      _builder_1.newLine();
      this.assertAllLabels(_builder, _builder_1);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  @Flaky
  public void testOutlineWithCreateEClassInModifyEcoreAndInterpretedNewAttributeWithUseAs() {
    try {
      InputOutput.<String>println("*** Executing testOutlineWithCreateEClassInModifyEcoreAndInterpretedNewAttributeWithUseAs...");
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("import org.eclipse.emf.ecore.EClass");
      _builder.newLine();
      _builder.newLine();
      _builder.append("package com.example");
      _builder.newLine();
      _builder.newLine();
      _builder.append("metamodel \"mypackage\"");
      _builder.newLine();
      _builder.newLine();
      _builder.append("def myNewAttribute(EClass c, String name) {");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("c.EStructuralFeatures += newEAttribute(name, ecoreref(MyDataType))");
      _builder.newLine();
      _builder.append("}");
      _builder.newLine();
      IResourcesSetupUtil.createFile(
        (AbstractOutlineTest.TEST_PROJECT + "/src/Refactorings.edelta"), _builder.toString());
      IResourcesSetupUtil.waitForBuild();
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("package com.example");
      _builder_1.newLine();
      _builder_1.newLine();
      _builder_1.append("metamodel \"mypackage\"");
      _builder_1.newLine();
      _builder_1.append("// don\'t rely on ecore, since the input files are not saved");
      _builder_1.newLine();
      _builder_1.append("// during the test, thus external libraries are not seen");
      _builder_1.newLine();
      _builder_1.append("// metamodel \"ecore\"");
      _builder_1.newLine();
      _builder_1.newLine();
      _builder_1.append("use Refactorings as my");
      _builder_1.newLine();
      _builder_1.newLine();
      _builder_1.append("modifyEcore aModification epackage mypackage {");
      _builder_1.newLine();
      _builder_1.append("\t");
      _builder_1.append("EClassifiers += newEClass(\"A\")");
      _builder_1.newLine();
      _builder_1.append("\t");
      _builder_1.append("my.myNewAttribute(ecoreref(A), \"foo\")");
      _builder_1.newLine();
      _builder_1.append("}");
      _builder_1.newLine();
      StringConcatenation _builder_2 = new StringConcatenation();
      _builder_2.append("com.example");
      _builder_2.newLine();
      _builder_2.append("  ");
      _builder_2.append("aModification(EPackage) : void");
      _builder_2.newLine();
      _builder_2.append("  ");
      _builder_2.append("mypackage");
      _builder_2.newLine();
      _builder_2.append("    ");
      CharSequence _allOtherContents = this.allOtherContents();
      _builder_2.append(_allOtherContents, "    ");
      _builder_2.newLineIfNotEmpty();
      _builder_2.append("    ");
      _builder_2.append("A");
      _builder_2.newLine();
      _builder_2.append("      ");
      _builder_2.append("foo : MyDataType");
      _builder_2.newLine();
      this.assertAllLabels(_builder_1, _builder_2);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  @Flaky
  public void testOutlineWithRemovedElementsInModifyEcore() {
    try {
      InputOutput.<String>println("*** Executing testOutlineWithRemovedElementsInModifyEcore...");
      IResourcesSetupUtil.waitForBuild();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("import org.eclipse.emf.ecore.EClass");
      _builder.newLine();
      _builder.newLine();
      _builder.append("metamodel \"mypackage\"");
      _builder.newLine();
      _builder.newLine();
      _builder.append("modifyEcore aModification epackage mypackage {");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("EClassifiers -= ecoreref(MyClass)");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("ecoreref(MyDerivedClass).ESuperTypes -= ecoreref(MyBaseClass)");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("EClassifiers -= ecoreref(MyBaseClass)");
      _builder.newLine();
      _builder.append("}");
      _builder.newLine();
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("test");
      _builder_1.newLine();
      _builder_1.append("  ");
      _builder_1.append("aModification(EPackage) : void");
      _builder_1.newLine();
      _builder_1.append("  ");
      _builder_1.append("mypackage");
      _builder_1.newLine();
      _builder_1.append("    ");
      _builder_1.append("MyDataType [java.lang.String]");
      _builder_1.newLine();
      _builder_1.append("    ");
      _builder_1.append("MyDerivedClass");
      _builder_1.newLine();
      _builder_1.append("      ");
      _builder_1.append("myDerivedAttribute : EString");
      _builder_1.newLine();
      _builder_1.append("      ");
      _builder_1.append("myDerivedReference : EObject");
      _builder_1.newLine();
      this.assertAllLabels(_builder, _builder_1);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  @Flaky
  public void testOutlineWhenNoElementIsModifiedThenTheEPackageIsNotShown() {
    try {
      InputOutput.<String>println("*** Executing testOutlineWhenNoElementIsModified...");
      IResourcesSetupUtil.waitForBuild();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("metamodel \"mypackage\"");
      _builder.newLine();
      _builder.newLine();
      _builder.append("modifyEcore aModification epackage mypackage {");
      _builder.newLine();
      _builder.newLine();
      _builder.append("}");
      _builder.newLine();
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("test");
      _builder_1.newLine();
      _builder_1.append("  ");
      _builder_1.append("aModification(EPackage) : void");
      _builder_1.newLine();
      this.assertAllLabels(_builder, _builder_1);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  private CharSequence allOtherContents() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("MyClass");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("myAttribute : EString");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("myReference : EObject");
    _builder.newLine();
    _builder.append("MyDataType [java.lang.String]");
    _builder.newLine();
    _builder.append("MyBaseClass");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("myBaseAttribute : EString");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("myBaseReference : EObject");
    _builder.newLine();
    _builder.append("MyDerivedClass -> MyBaseClass");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("myDerivedAttribute : EString");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("myDerivedReference : EObject");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("MyBaseClass");
    _builder.newLine();
    return _builder;
  }
}
