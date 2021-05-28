package edelta.ui.tests;

import edelta.ui.internal.EdeltaActivator;
import edelta.ui.tests.utils.ProjectImportUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.testing.Flaky;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.testing.AbstractOutlineTest;
import org.eclipse.xtext.ui.testing.AbstractWorkbenchTest;
import org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaUiInjectorProvider.class)
@SuppressWarnings("all")
public class EdeltaOutlineTest extends AbstractOutlineTest {
  @Rule
  public Flaky.Rule testRule = new Flaky.Rule();
  
  @BeforeClass
  public static void setTestProjectName() {
    AbstractOutlineTest.TEST_PROJECT = "edelta.ui.tests.project";
  }
  
  /**
   * Avoids deleting project
   */
  @Override
  public void setUp() {
    try {
      this.createjavaProject(AbstractOutlineTest.TEST_PROJECT);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  /**
   * Avoids deleting project
   */
  @Override
  public void tearDown() {
    this.waitForEventProcessing();
    AbstractWorkbenchTest.closeEditors();
    this.waitForEventProcessing();
  }
  
  @Override
  protected String getEditorId() {
    return EdeltaActivator.EDELTA_EDELTA;
  }
  
  @Override
  protected IJavaProject createjavaProject(final String projectName) throws CoreException {
    try {
      final IJavaProject javaProject = ProjectImportUtil.importJavaProject(AbstractOutlineTest.TEST_PROJECT);
      IResourcesSetupUtil.waitForBuild();
      return javaProject;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testOutlineWithNoContents() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("test");
    _builder.newLine();
    this.assertAllLabels("", _builder);
  }
  
  @Test
  public void testOutlineWithOperation() throws Exception {
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
  }
  
  @Test
  @Flaky
  public void testOutlineWithCreateEClassInModifyEcore() throws Exception {
    System.out.println("*** Executing testOutlineWithCreateEClassInModifyEcore...");
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
    _builder_1.append("MyClass");
    _builder_1.newLine();
    _builder_1.append("      ");
    _builder_1.append("myAttribute : EString");
    _builder_1.newLine();
    _builder_1.append("      ");
    _builder_1.append("myReference : EObject");
    _builder_1.newLine();
    _builder_1.append("    ");
    _builder_1.append("MyDataType [java.lang.String]");
    _builder_1.newLine();
    _builder_1.append("    ");
    _builder_1.append("MyBaseClass");
    _builder_1.newLine();
    _builder_1.append("      ");
    _builder_1.append("myBaseAttribute : EString");
    _builder_1.newLine();
    _builder_1.append("      ");
    _builder_1.append("myBaseReference : EObject");
    _builder_1.newLine();
    _builder_1.append("    ");
    _builder_1.append("MyDerivedClass -> MyBaseClass");
    _builder_1.newLine();
    _builder_1.append("      ");
    _builder_1.append("myDerivedAttribute : EString");
    _builder_1.newLine();
    _builder_1.append("      ");
    _builder_1.append("myDerivedReference : EObject");
    _builder_1.newLine();
    _builder_1.append("      ");
    _builder_1.append("MyBaseClass");
    _builder_1.newLine();
    _builder_1.append("    ");
    _builder_1.append("A");
    _builder_1.newLine();
    _builder_1.append("      ");
    _builder_1.append("foo : MyDataType");
    _builder_1.newLine();
    this.assertAllLabels(_builder, _builder_1);
  }
  
  @Test
  @Flaky
  public void testOutlineWithCreateEClassInModifyEcoreAndInterpretedNewAttributeWithUseAs() throws Exception {
    System.out.println("*** Executing testOutlineWithCreateEClassInModifyEcoreAndInterpretedNewAttributeWithUseAs...");
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
    _builder_2.append("MyClass");
    _builder_2.newLine();
    _builder_2.append("      ");
    _builder_2.append("myAttribute : EString");
    _builder_2.newLine();
    _builder_2.append("      ");
    _builder_2.append("myReference : EObject");
    _builder_2.newLine();
    _builder_2.append("    ");
    _builder_2.append("MyDataType [java.lang.String]");
    _builder_2.newLine();
    _builder_2.append("    ");
    _builder_2.append("MyBaseClass");
    _builder_2.newLine();
    _builder_2.append("      ");
    _builder_2.append("myBaseAttribute : EString");
    _builder_2.newLine();
    _builder_2.append("      ");
    _builder_2.append("myBaseReference : EObject");
    _builder_2.newLine();
    _builder_2.append("    ");
    _builder_2.append("MyDerivedClass -> MyBaseClass");
    _builder_2.newLine();
    _builder_2.append("      ");
    _builder_2.append("myDerivedAttribute : EString");
    _builder_2.newLine();
    _builder_2.append("      ");
    _builder_2.append("myDerivedReference : EObject");
    _builder_2.newLine();
    _builder_2.append("      ");
    _builder_2.append("MyBaseClass");
    _builder_2.newLine();
    _builder_2.append("    ");
    _builder_2.append("A");
    _builder_2.newLine();
    _builder_2.append("      ");
    _builder_2.append("foo : MyDataType");
    _builder_2.newLine();
    this.assertAllLabels(_builder_1, _builder_2);
  }
  
  @Test
  @Flaky
  public void testOutlineWithRemovedElementsInModifyEcore() throws Exception {
    System.out.println("*** Executing testOutlineWithRemovedElementsInModifyEcore...");
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
  }
  
  @Test
  @Flaky
  public void testOutlineWhenNoElementIsModifiedThenTheEPackageIsNotShown() throws Exception {
    System.out.println("*** Executing testOutlineWhenNoElementIsModified...");
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
  }
}
