package edelta.ui.tests;

import com.google.inject.Injector;
import edelta.ui.internal.EdeltaActivator;
import edelta.ui.tests.EdeltaUiInjectorProvider;
import edelta.ui.tests.utils.EdeltaPluginProjectHelper;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.testing.Flaky;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.utils.EditorUtils;
import org.eclipse.xtext.ui.testing.AbstractContentAssistTest;
import org.eclipse.xtext.ui.testing.ContentAssistProcessorTestBuilder;
import org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function0;
import org.eclipse.xtext.xbase.lib.InputOutput;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaUiInjectorProvider.class)
@SuppressWarnings("all")
public class EdeltaContentAssistTest extends AbstractContentAssistTest {
  private static IJavaProject pluginJavaProject;
  
  @Rule
  public Flaky.Rule testRule = new Flaky.Rule();
  
  private final String cursor = new Function0<String>() {
    @Override
    public String apply() {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("<|>");
      return _builder.toString();
    }
  }.apply();
  
  @BeforeClass
  public static void setUp() {
    EdeltaPluginProjectHelper.closeWelcomePage();
    final Injector injector = EdeltaActivator.getInstance().getInjector(EdeltaActivator.EDELTA_EDELTA);
    final EdeltaPluginProjectHelper projectHelper = injector.<EdeltaPluginProjectHelper>getInstance(EdeltaPluginProjectHelper.class);
    EdeltaContentAssistTest.pluginJavaProject = projectHelper.createEdeltaPluginProject(EdeltaPluginProjectHelper.PROJECT_NAME);
    IResourcesSetupUtil.waitForBuild();
  }
  
  @AfterClass
  public static void tearDown() {
    try {
      IProject _project = EdeltaContentAssistTest.pluginJavaProject.getProject();
      NullProgressMonitor _nullProgressMonitor = new NullProgressMonitor();
      _project.delete(true, _nullProgressMonitor);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @After
  public void after() {
    EdeltaContentAssistTest.closeEditors();
  }
  
  @Override
  public IJavaProject getJavaProject(final ResourceSet resourceSet) {
    return EdeltaContentAssistTest.pluginJavaProject;
  }
  
  /**
   * We need a real resource otherwise the Ecores are not found
   * (they are collected using visible containers)
   */
  @Override
  public XtextResource getResourceFor(final InputStream inputStream) {
    try {
      InputStreamReader _inputStreamReader = new InputStreamReader(inputStream);
      final String result = new BufferedReader(_inputStreamReader).lines().collect(Collectors.joining("\n"));
      final XtextEditor editor = this.openEditor(
        IResourcesSetupUtil.createFile(
          (EdeltaPluginProjectHelper.PROJECT_NAME + "/src/Test.edelta"), result));
      final IUnitOfWork<XtextResource, XtextResource> _function = (XtextResource it) -> {
        return it;
      };
      return editor.getDocument().<XtextResource>readOnly(_function);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  protected XtextEditor openEditor(final IFile file) {
    final IEditorPart openEditor = this.openEditor(file, EdeltaActivator.EDELTA_EDELTA);
    return EditorUtils.getXtextEditor(openEditor);
  }
  
  private IEditorPart openEditor(final IFile file, final String editorId) {
    try {
      IWorkbenchPage _activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
      FileEditorInput _fileEditorInput = new FileEditorInput(file);
      return _activePage.openEditor(_fileEditorInput, editorId);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public static void closeEditors() {
    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(false);
  }
  
  @Test
  public void testMetamodelsEcore() {
    try {
      this.newBuilder().append("metamodel ").assertProposal("\"ecore\"");
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testMetamodelsLocalToProject() {
    try {
      this.newBuilder().append("metamodel ").assertProposal("\"mypackage\"");
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testMetamodelsInThePresenceOfSubpackages() {
    try {
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
      IResourcesSetupUtil.createFile((EdeltaPluginProjectHelper.PROJECT_NAME + "/model/MySubPackages.ecore"), _builder.toString());
      IResourcesSetupUtil.waitForBuild();
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("\"annotation\"");
      _builder_1.newLine();
      _builder_1.append("\"data\"");
      _builder_1.newLine();
      _builder_1.append("\"ecore\"");
      _builder_1.newLine();
      _builder_1.append("\"mainpackage\"");
      _builder_1.newLine();
      _builder_1.append("\"mypackage\"");
      _builder_1.newLine();
      _builder_1.append("\"namespace\"");
      _builder_1.newLine();
      _builder_1.append("\"type\"");
      _builder_1.newLine();
      this.newBuilder().append("metamodel ").assertText(
        this.fromLinesOfStringsToStringArray(_builder_1));
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testNoNSURIProposalMetamodels() {
    try {
      this.newBuilder().append("metamodel <|>").assertNoProposalAtCursor("\"http://my.package.org\"");
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testUnqualifiedEcoreReference() {
    try {
      ContentAssistProcessorTestBuilder _newBuilder = this.newBuilder();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("metamodel \"mypackage\"");
      _builder.newLine();
      _builder.append("modifyEcore aTest epackage mypackage { ");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("ecoreref(");
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("MyBaseClass");
      _builder_1.newLine();
      _builder_1.append("MyClass");
      _builder_1.newLine();
      _builder_1.append("MyDataType");
      _builder_1.newLine();
      _builder_1.append("MyDerivedClass");
      _builder_1.newLine();
      _builder_1.append("myAttribute");
      _builder_1.newLine();
      _builder_1.append("myBaseAttribute");
      _builder_1.newLine();
      _builder_1.append("myBaseReference");
      _builder_1.newLine();
      _builder_1.append("myDerivedAttribute");
      _builder_1.newLine();
      _builder_1.append("myDerivedReference");
      _builder_1.newLine();
      _builder_1.append("myReference");
      _builder_1.newLine();
      _builder_1.append("mypackage");
      _builder_1.newLine();
      _newBuilder.append(_builder.toString()).assertText(this.fromLinesOfStringsToStringArray(_builder_1));
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testQualifiedEcoreReference() {
    try {
      this.newBuilder().append("metamodel \"mypackage\"\n\t\t\t\tmodifyEcore aTest epackage mypackage {\n\t\t\t\t\tecoreref(MyClass.").assertText("myAttribute", "myReference");
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testEClassifierAfterCreatingAnEClass() {
    try {
      ContentAssistProcessorTestBuilder _newBuilder = this.newBuilder();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("metamodel \"mypackage\"");
      _builder.newLine();
      _builder.newLine();
      _builder.append("modifyEcore aTest epackage mypackage {");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("addNewEClass(\"AAA\")");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("ecoreref(");
      _newBuilder.append(_builder.toString()).assertProposal("AAA");
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testEClassifierAfterRenamingAnEClass() {
    try {
      ContentAssistProcessorTestBuilder _newBuilder = this.newBuilder();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("metamodel \"mypackage\"");
      _builder.newLine();
      _builder.newLine();
      _builder.append("modifyEcore aTest epackage mypackage {");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("ecoreref(MyClass).name = \"Renamed\"");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("ecoreref(");
      _newBuilder.append(_builder.toString()).assertProposal("Renamed");
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  @Flaky
  public void testCreatedEAttributeDuringInterpretationIsProposed() {
    try {
      InputOutput.<String>println("*** Executing testCreatedEAttributeDuringInterpretationIsProposed...");
      ContentAssistProcessorTestBuilder _newBuilder = this.newBuilder();
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
      _builder.append("modifyEcore aTest epackage mypackage {");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("myNewAttribute(addNewEClass(\"A\"), \"foo\")");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("ecoreref(");
      _newBuilder.append(_builder.toString()).assertProposal("foo");
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testUnqualifiedEcoreReferenceBeforeRemovalOfEClass() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mypackage\"");
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mypackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(");
    _builder.append(this.cursor, "\t");
    _builder.append(");");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.append("EClassifiers -= ecoreref(MyClass)");
    _builder.newLine();
    _builder.append("}");
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("MyBaseClass");
    _builder_1.newLine();
    _builder_1.append("MyClass");
    _builder_1.newLine();
    _builder_1.append("MyDataType");
    _builder_1.newLine();
    _builder_1.append("MyDerivedClass");
    _builder_1.newLine();
    _builder_1.append("myAttribute");
    _builder_1.newLine();
    _builder_1.append("myBaseAttribute");
    _builder_1.newLine();
    _builder_1.append("myBaseReference");
    _builder_1.newLine();
    _builder_1.append("myDerivedAttribute");
    _builder_1.newLine();
    _builder_1.append("myDerivedReference");
    _builder_1.newLine();
    _builder_1.append("myReference");
    _builder_1.newLine();
    _builder_1.append("mypackage");
    _builder_1.newLine();
    this.testContentAssistant(_builder, 
      ((List<String>)Conversions.doWrapArray(this.fromLinesOfStringsToStringArray(_builder_1))));
  }
  
  @Test
  public void testQualifiedEcoreReferenceBeforeRemovalOfEClass() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mypackage\"");
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mypackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(MyClass.");
    _builder.append(this.cursor, "\t");
    _builder.append(");");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.append("EClassifiers -= ecoreref(MyClass)");
    _builder.newLine();
    _builder.append("}");
    this.testContentAssistant(_builder, Collections.<String>unmodifiableList(CollectionLiterals.<String>newArrayList("myAttribute", "myReference")));
  }
  
  @Test
  public void testQualifiedEcoreReferenceBeforeRemovalOfEStructuralFeature() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mypackage\"");
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mypackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(MyClass.");
    _builder.append(this.cursor, "\t");
    _builder.append(");");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.append("ecoreref(MyClass).EStructuralFeatures -= ecoreref(myReference)");
    _builder.newLine();
    _builder.append("}");
    this.testContentAssistant(_builder, Collections.<String>unmodifiableList(CollectionLiterals.<String>newArrayList("myAttribute", "myReference")));
  }
  
  @Test
  public void testQualifiedEcoreReferenceBeforeAdditionOfEStructuralFeature() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mypackage\"");
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mypackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(MyClass.");
    _builder.append(this.cursor, "\t");
    _builder.append(");");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.append("ecoreref(MyClass).addNewEAttribute(\"myNewAttribute\", null)");
    _builder.newLine();
    _builder.append("}");
    this.testContentAssistant(_builder, Collections.<String>unmodifiableList(CollectionLiterals.<String>newArrayList("myAttribute", "myReference")));
  }
  
  @Test
  public void testUnqualifiedEcoreReferenceAfterRemoval() {
    try {
      ContentAssistProcessorTestBuilder _newBuilder = this.newBuilder();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("metamodel \"mypackage\"");
      _builder.newLine();
      _builder.append("modifyEcore aTest epackage mypackage { ");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("EClassifiers -= ecoreref(MyBaseClass)");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("ecoreref(");
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("MyClass");
      _builder_1.newLine();
      _builder_1.append("MyDataType");
      _builder_1.newLine();
      _builder_1.append("MyDerivedClass");
      _builder_1.newLine();
      _builder_1.append("myAttribute");
      _builder_1.newLine();
      _builder_1.append("myDerivedAttribute");
      _builder_1.newLine();
      _builder_1.append("myDerivedReference");
      _builder_1.newLine();
      _builder_1.append("myReference");
      _builder_1.newLine();
      _builder_1.append("mypackage");
      _builder_1.newLine();
      _newBuilder.append(_builder.toString()).assertText(this.fromLinesOfStringsToStringArray(_builder_1));
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testQualifiedEcoreReferenceAfterRemovalOfEStructuralFeature() {
    try {
      ContentAssistProcessorTestBuilder _newBuilder = this.newBuilder();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("metamodel \"mypackage\"");
      _builder.newLine();
      _builder.append("modifyEcore aTest epackage mypackage { ");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("ecoreref(MyBaseClass).EStructuralFeatures -= ecoreref(myBaseAttribute)");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("ecoreref(MyBaseClass.");
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("myBaseReference");
      _builder_1.newLine();
      _newBuilder.append(_builder.toString()).assertText(this.fromLinesOfStringsToStringArray(_builder_1));
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testQualifiedEcoreReferenceAfterAdditionOfEStructuralFeature() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mypackage\"");
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mypackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(MyClass).addNewEAttribute(\"myNewAttribute\", null)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(MyClass.");
    _builder.append(this.cursor, "\t");
    _builder.append(");");
    _builder.newLineIfNotEmpty();
    _builder.append("}");
    this.testContentAssistant(_builder, Collections.<String>unmodifiableList(CollectionLiterals.<String>newArrayList("myAttribute", "myReference", "myNewAttribute")));
  }
  
  @Test
  public void testUnqualifiedEcoreReferenceAfterRename() {
    try {
      ContentAssistProcessorTestBuilder _newBuilder = this.newBuilder();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("metamodel \"mypackage\"");
      _builder.newLine();
      _builder.append("modifyEcore aTest epackage mypackage { ");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("ecoreref(MyBaseClass).name = \"Renamed\"");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("ecoreref(");
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("MyClass");
      _builder_1.newLine();
      _builder_1.append("MyDataType");
      _builder_1.newLine();
      _builder_1.append("MyDerivedClass");
      _builder_1.newLine();
      _builder_1.append("Renamed");
      _builder_1.newLine();
      _builder_1.append("myAttribute");
      _builder_1.newLine();
      _builder_1.append("myBaseAttribute");
      _builder_1.newLine();
      _builder_1.append("myBaseReference");
      _builder_1.newLine();
      _builder_1.append("myDerivedAttribute");
      _builder_1.newLine();
      _builder_1.append("myDerivedReference");
      _builder_1.newLine();
      _builder_1.append("myReference");
      _builder_1.newLine();
      _builder_1.append("mypackage");
      _builder_1.newLine();
      _newBuilder.append(_builder.toString()).assertText(this.fromLinesOfStringsToStringArray(_builder_1));
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  private String[] fromLinesOfStringsToStringArray(final CharSequence strings) {
    return strings.toString().replaceAll("\r", "").split("\n");
  }
  
  private void testContentAssistant(final CharSequence text, final List<String> expectedProposals) {
    try {
      final int cursorPosition = text.toString().indexOf(this.cursor);
      final String content = text.toString().replace(this.cursor, "");
      this.newBuilder().append(content).assertTextAtCursorPosition(cursorPosition, ((String[])Conversions.unwrapArray(expectedProposals, String.class)));
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
}
