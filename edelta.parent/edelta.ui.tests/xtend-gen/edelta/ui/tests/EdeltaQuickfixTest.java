package edelta.ui.tests;

import com.google.inject.Inject;
import edelta.ui.tests.EdeltaUiInjectorProvider;
import edelta.ui.tests.utils.EdeltaPluginProjectHelper;
import edelta.validation.EdeltaValidator;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.testing.AbstractQuickfixTest;
import org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaUiInjectorProvider.class)
@SuppressWarnings("all")
public class EdeltaQuickfixTest extends AbstractQuickfixTest {
  @Inject
  private EdeltaPluginProjectHelper projectHelper;
  
  @Override
  protected String getFileName() {
    String _fileName = super.getFileName();
    return ("src/" + _fileName);
  }
  
  @Before
  public void setup() {
    try {
      this.projectHelper.createEdeltaPluginProject(this.getProjectName());
      String _projectName = this.getProjectName();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
      _builder.newLine();
      _builder.append("<ecore:EPackage xmi:version=\"2.0\" xmlns:xmi=\"http://www.omg.org/XMI\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
      _builder.newLine();
      _builder.append("    ");
      _builder.append("xmlns:ecore=\"http://www.eclipse.org/emf/2002/Ecore\" name=\"mainpackage\" nsURI=\"http://my.mainpackage.org\" nsPrefix=\"mainpackage\">");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("<eClassifiers xsi:type=\"ecore:EClass\" name=\"MyClass\">");
      _builder.newLine();
      _builder.append("    ");
      _builder.append("<eStructuralFeatures xsi:type=\"ecore:EAttribute\" name=\"myAttribute\" eType=\"ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString\"/>");
      _builder.newLine();
      _builder.append("    ");
      _builder.append("<eStructuralFeatures xsi:type=\"ecore:EReference\" name=\"myReference\" eType=\"ecore:EClass http://www.eclipse.org/emf/2002/Ecore#//EObject\"/>");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("</eClassifiers>");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("<eSubpackages name=\"subpackage\" nsURI=\"http://mysubpackage\" nsPrefix=\"subpackage\">");
      _builder.newLine();
      _builder.append("    ");
      _builder.append("<eClassifiers xsi:type=\"ecore:EClass\" name=\"MySubPackageClass\"/>");
      _builder.newLine();
      _builder.append("    ");
      _builder.append("<eClassifiers xsi:type=\"ecore:EClass\" name=\"MyClass\">");
      _builder.newLine();
      _builder.append("      ");
      _builder.append("<eStructuralFeatures xsi:type=\"ecore:EAttribute\" name=\"myAttribute\" eType=\"ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString\"/>");
      _builder.newLine();
      _builder.append("      ");
      _builder.append("<eStructuralFeatures xsi:type=\"ecore:EReference\" name=\"myReference\" eType=\"ecore:EClass http://www.eclipse.org/emf/2002/Ecore#//EObject\"/>");
      _builder.newLine();
      _builder.append("    ");
      _builder.append("</eClassifiers>");
      _builder.newLine();
      _builder.append("    ");
      _builder.append("<eSubpackages name=\"subsubpackage\" nsURI=\"http://mysubsubpackage\" nsPrefix=\"subsubpackage\">");
      _builder.newLine();
      _builder.append("      ");
      _builder.append("<eClassifiers xsi:type=\"ecore:EClass\" name=\"MyClass\">");
      _builder.newLine();
      _builder.append("        ");
      _builder.append("<eStructuralFeatures xsi:type=\"ecore:EAttribute\" name=\"myAttribute\" eType=\"ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString\"/>");
      _builder.newLine();
      _builder.append("        ");
      _builder.append("<eStructuralFeatures xsi:type=\"ecore:EReference\" name=\"myReference\" eType=\"ecore:EClass http://www.eclipse.org/emf/2002/Ecore#//EObject\"/>");
      _builder.newLine();
      _builder.append("      ");
      _builder.append("</eClassifiers>");
      _builder.newLine();
      _builder.append("    ");
      _builder.append("</eSubpackages>");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("</eSubpackages>");
      _builder.newLine();
      _builder.append("</ecore:EPackage>");
      _builder.newLine();
      IResourcesSetupUtil.createFile(_projectName, 
        "src/MySubPackages", "ecore", _builder.toString());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void fixSubPackageImport() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mainpackage.subpackage\"");
    _builder.newLine();
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("metamodel \"mainpackage\"");
    _builder_1.newLine();
    AbstractQuickfixTest.Quickfix _quickfix = new AbstractQuickfixTest.Quickfix("Import root EPackage", 
      "Import root EPackage \'mainpackage\'", _builder_1.toString());
    this.testQuickfixesOn(_builder, EdeltaValidator.INVALID_SUBPACKAGE_IMPORT, _quickfix);
  }
  
  @Test
  public void fixSubPackageImportWithSeveralImports() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.append("metamodel \"mainpackage.subpackage.subsubpackage\"");
    _builder.newLine();
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("metamodel \"foo\"");
    _builder_1.newLine();
    _builder_1.append("metamodel \"mainpackage\"");
    _builder_1.newLine();
    AbstractQuickfixTest.Quickfix _quickfix = new AbstractQuickfixTest.Quickfix("Import root EPackage", 
      "Import root EPackage \'mainpackage\'", _builder_1.toString());
    this.testQuickfixesOn(_builder, EdeltaValidator.INVALID_SUBPACKAGE_IMPORT, _quickfix);
  }
  
  @Test
  public void fixAccessToRenamedElement() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mainpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore renaming epackage mainpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(subsubpackage.MyClass.myAttribute).name = \"Renamed\"");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore access epackage mainpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(subsubpackage.MyClass.myAttribute)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("metamodel \"mainpackage\"");
    _builder_1.newLine();
    _builder_1.newLine();
    _builder_1.append("modifyEcore renaming epackage mainpackage {");
    _builder_1.newLine();
    _builder_1.append("\t");
    _builder_1.append("ecoreref(subsubpackage.MyClass.myAttribute).name = \"Renamed\"");
    _builder_1.newLine();
    _builder_1.append("}");
    _builder_1.newLine();
    _builder_1.newLine();
    _builder_1.append("modifyEcore access epackage mainpackage {");
    _builder_1.newLine();
    _builder_1.append("\t");
    _builder_1.append("ecoreref(mainpackage.subpackage.subsubpackage.MyClass.Renamed)");
    _builder_1.newLine();
    _builder_1.append("}");
    _builder_1.newLine();
    AbstractQuickfixTest.Quickfix _quickfix = new AbstractQuickfixTest.Quickfix("Use renamed element", 
      "Use renamed element \'mainpackage.subpackage.subsubpackage.MyClass.Renamed\'", _builder_1.toString());
    this.testQuickfixesOn(_builder, EdeltaValidator.INTERPRETER_ACCESS_RENAMED_ELEMENT, _quickfix);
  }
  
  @Test
  public void fixAmbiguousEcoreRef() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mainpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mainpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(MyClass)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("metamodel \"mainpackage\"");
    _builder_1.newLine();
    _builder_1.newLine();
    _builder_1.append("modifyEcore aTest epackage mainpackage {");
    _builder_1.newLine();
    _builder_1.append("\t");
    _builder_1.append("ecoreref(mainpackage.MyClass)");
    _builder_1.newLine();
    _builder_1.append("}");
    _builder_1.newLine();
    AbstractQuickfixTest.Quickfix _quickfix = new AbstractQuickfixTest.Quickfix(
      "Fix ambiguity with \'mainpackage.MyClass\'", 
      "Fix ambiguity with \'mainpackage.MyClass\'", _builder_1.toString());
    StringConcatenation _builder_2 = new StringConcatenation();
    _builder_2.append("metamodel \"mainpackage\"");
    _builder_2.newLine();
    _builder_2.newLine();
    _builder_2.append("modifyEcore aTest epackage mainpackage {");
    _builder_2.newLine();
    _builder_2.append("\t");
    _builder_2.append("ecoreref(mainpackage.subpackage.MyClass)");
    _builder_2.newLine();
    _builder_2.append("}");
    _builder_2.newLine();
    AbstractQuickfixTest.Quickfix _quickfix_1 = new AbstractQuickfixTest.Quickfix(
      "Fix ambiguity with \'mainpackage.subpackage.MyClass\'", 
      "Fix ambiguity with \'mainpackage.subpackage.MyClass\'", _builder_2.toString());
    StringConcatenation _builder_3 = new StringConcatenation();
    _builder_3.append("metamodel \"mainpackage\"");
    _builder_3.newLine();
    _builder_3.newLine();
    _builder_3.append("modifyEcore aTest epackage mainpackage {");
    _builder_3.newLine();
    _builder_3.append("\t");
    _builder_3.append("ecoreref(mainpackage.subpackage.subsubpackage.MyClass)");
    _builder_3.newLine();
    _builder_3.append("}");
    _builder_3.newLine();
    AbstractQuickfixTest.Quickfix _quickfix_2 = new AbstractQuickfixTest.Quickfix(
      "Fix ambiguity with \'mainpackage.subpackage.subsubpackage.MyClass\'", 
      "Fix ambiguity with \'mainpackage.subpackage.subsubpackage.MyClass\'", _builder_3.toString());
    this.testQuickfixesOn(_builder, EdeltaValidator.AMBIGUOUS_REFERENCE, _quickfix, _quickfix_1, _quickfix_2);
  }
}
