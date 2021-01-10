package edelta.tests;

import com.google.inject.Inject;
import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaProgram;
import edelta.navigation.EdeltaNavigationTargetHelper;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProviderCustom;
import org.assertj.core.api.Assertions;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
@SuppressWarnings("all")
public class EdeltaNavigationTargetHelperTest extends EdeltaAbstractTest {
  @Inject
  private EdeltaNavigationTargetHelper navigationTargetHelper;
  
  @Test
  public void testNotEdeltaEcoreReference() throws Exception {
    Assertions.<EObject>assertThat(this.navigationTargetHelper.getTarget(EcoreFactory.eINSTANCE.createEClass()));
  }
  
  @Test
  public void testTargetInTheImportedMetamodel() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(FooClass)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final EObject target = this.navigationTargetHelper.getTarget(IterableExtensions.<EdeltaEcoreReferenceExpression>head(this.getAllEcoreReferenceExpressions(it)).getReference());
      final EClassifier original = this.getEClassiferByName(IterableExtensions.<EPackage>head(it.getMetamodels()), "FooClass");
      Assertions.<EObject>assertThat(target).isNotNull();
      Assertions.<EObject>assertThat(target).isSameAs(original);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testTargetAsXExpression() throws Exception {
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
    _builder.append("ecoreref(NewClass)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final EObject target = this.navigationTargetHelper.getTarget(IterableExtensions.<EdeltaEcoreReferenceExpression>head(this.getAllEcoreReferenceExpressions(it)).getReference());
      final XExpression exp = IterableExtensions.<XExpression>head(this.getBlock(this.lastModifyEcoreOperation(it).getBody()).getExpressions());
      Assertions.<EObject>assertThat(target).isNotNull();
      Assertions.<EObject>assertThat(target).isSameAs(exp);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testTargetOfForwardReference() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(NewClass)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass\")");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final EObject target = this.navigationTargetHelper.getTarget(IterableExtensions.<EdeltaEcoreReferenceExpression>head(this.getAllEcoreReferenceExpressions(it)).getReference());
      final XExpression exp = IterableExtensions.<XExpression>last(this.getBlock(this.lastModifyEcoreOperation(it).getBody()).getExpressions());
      Assertions.<EObject>assertThat(target).isNotNull();
      Assertions.<EObject>assertThat(target).isSameAs(exp);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
}
