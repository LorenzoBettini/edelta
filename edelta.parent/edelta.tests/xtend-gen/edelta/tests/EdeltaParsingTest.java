package edelta.tests;

import edelta.edelta.EdeltaEcoreQualifiedReference;
import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaPackage;
import edelta.edelta.EdeltaProgram;
import edelta.edelta.EdeltaUseAs;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProvider.class)
@SuppressWarnings("all")
public class EdeltaParsingTest extends EdeltaAbstractTest {
  @Test
  public void testEmptyProgram() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    final EdeltaProgram result = this.parseHelper.parse(_builder);
    Assert.assertNotNull(result);
  }
  
  @Test
  public void testSingleMetamodel() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"ecore\"");
    _builder.newLine();
    final EdeltaProgram result = this.parseHelper.parse(_builder);
    Assert.assertNotNull(result);
  }
  
  @Test
  public void testTwoMetamodels() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"ecore\"");
    _builder.newLine();
    _builder.append("metamodel \"type\"");
    _builder.newLine();
    final EdeltaProgram result = this.parseHelper.parse(_builder);
    Assert.assertNotNull(result);
  }
  
  @Test
  public void testDirectEcoreReference() throws Exception {
    EdeltaEcoreReferenceExpression _ecoreReferenceExpression = this.getEcoreReferenceExpression("foo");
    final Procedure1<EdeltaEcoreReferenceExpression> _function = (EdeltaEcoreReferenceExpression it) -> {
      Assert.assertNotNull(this.getEdeltaEcoreDirectReference(it.getReference()).getEnamedelement());
    };
    ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_ecoreReferenceExpression, _function);
  }
  
  @Test
  public void testDirectEcoreReferenceIncomplete() throws Exception {
    EdeltaEcoreReferenceExpression _ecoreReferenceExpression = this.getEcoreReferenceExpression("");
    final Procedure1<EdeltaEcoreReferenceExpression> _function = (EdeltaEcoreReferenceExpression it) -> {
      Assert.assertNull(this.getEdeltaEcoreDirectReference(it.getReference()).getEnamedelement());
    };
    ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_ecoreReferenceExpression, _function);
  }
  
  @Test
  public void testQualifiedEcoreReference() throws Exception {
    EdeltaEcoreQualifiedReference _edeltaEcoreQualifiedReference = this.getEdeltaEcoreQualifiedReference(this.getEcoreReferenceExpression("foo.bar").getReference());
    final Procedure1<EdeltaEcoreQualifiedReference> _function = (EdeltaEcoreQualifiedReference it) -> {
      Assert.assertEquals("foo", this.getTextualRepresentation(it.getQualification()));
      Assert.assertEquals("bar", this.getTextualReferenceRepresentation(it));
      Assert.assertEquals("foo.bar", this.getTextualRepresentation(it));
    };
    ObjectExtensions.<EdeltaEcoreQualifiedReference>operator_doubleArrow(_edeltaEcoreQualifiedReference, _function);
  }
  
  @Test
  public void testQualifiedEcoreReference2() throws Exception {
    EdeltaEcoreQualifiedReference _edeltaEcoreQualifiedReference = this.getEdeltaEcoreQualifiedReference(this.getEcoreReferenceExpression("foo.bar.baz").getReference());
    final Procedure1<EdeltaEcoreQualifiedReference> _function = (EdeltaEcoreQualifiedReference it) -> {
      Assert.assertEquals("foo.bar", this.getTextualRepresentation(it.getQualification()));
      Assert.assertEquals("baz", this.getTextualReferenceRepresentation(it));
      Assert.assertEquals("foo.bar.baz", this.getTextualRepresentation(it));
    };
    ObjectExtensions.<EdeltaEcoreQualifiedReference>operator_doubleArrow(_edeltaEcoreQualifiedReference, _function);
  }
  
  @Test
  public void testQualifiedEcoreReferenceIncomplete() throws Exception {
    EdeltaEcoreQualifiedReference _edeltaEcoreQualifiedReference = this.getEdeltaEcoreQualifiedReference(this.getEcoreReferenceExpression("foo.").getReference());
    final Procedure1<EdeltaEcoreQualifiedReference> _function = (EdeltaEcoreQualifiedReference it) -> {
      Assert.assertEquals("foo", this.getTextualRepresentation(it.getQualification()));
      Assert.assertNull(it.getEnamedelement());
      Assert.assertEquals("foo.", this.getTextualRepresentation(it));
    };
    ObjectExtensions.<EdeltaEcoreQualifiedReference>operator_doubleArrow(_edeltaEcoreQualifiedReference, _function);
  }
  
  @Test
  public void testEdeltaUseAsIncomplete() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("use");
    _builder.newLine();
    EdeltaUseAs _head = IterableExtensions.<EdeltaUseAs>head(this.parseHelper.parse(_builder).getUseAsClauses());
    final Procedure1<EdeltaUseAs> _function = (EdeltaUseAs it) -> {
      Assert.assertNull(it.getType());
      Assert.assertNull(it.getName());
    };
    ObjectExtensions.<EdeltaUseAs>operator_doubleArrow(_head, _function);
  }
  
  @Test
  public void testEdeltaUseAsIncompleteNoType() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("use as foo");
    _builder.newLine();
    EdeltaUseAs _head = IterableExtensions.<EdeltaUseAs>head(this.parseHelper.parse(_builder).getUseAsClauses());
    final Procedure1<EdeltaUseAs> _function = (EdeltaUseAs it) -> {
      Assert.assertNull(it.getType());
      Assert.assertNotNull(it.getName());
    };
    ObjectExtensions.<EdeltaUseAs>operator_doubleArrow(_head, _function);
  }
  
  @Test
  public void testEdeltaUseAsIncompleteNoName() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("use Foo as ");
    _builder.newLine();
    EdeltaUseAs _head = IterableExtensions.<EdeltaUseAs>head(this.parseHelper.parse(_builder).getUseAsClauses());
    final Procedure1<EdeltaUseAs> _function = (EdeltaUseAs it) -> {
      Assert.assertNotNull(it.getType());
      Assert.assertNull(it.getName());
    };
    ObjectExtensions.<EdeltaUseAs>operator_doubleArrow(_head, _function);
  }
  
  @Test
  public void testEdeltaUseAs() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("use Foo as foo");
    _builder.newLine();
    EdeltaUseAs _head = IterableExtensions.<EdeltaUseAs>head(this.parseHelper.parse(_builder).getUseAsClauses());
    final Procedure1<EdeltaUseAs> _function = (EdeltaUseAs it) -> {
      Assert.assertNotNull(it.getType());
      Assert.assertNotNull(it.getName());
    };
    ObjectExtensions.<EdeltaUseAs>operator_doubleArrow(_head, _function);
  }
  
  private EdeltaEcoreReferenceExpression getEcoreReferenceExpression(final CharSequence ecoreRefArg) throws Exception {
    return this.getEdeltaEcoreReferenceExpression(IterableExtensions.<XExpression>last(this.getBlock(this.lastModifyEcoreOperation(this.parseHelper.parse(this.textForEcoreRef(ecoreRefArg))).getBody()).getExpressions()));
  }
  
  private CharSequence textForEcoreRef(final CharSequence ecoreRefArg) throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(");
    _builder.append(ecoreRefArg, "\t");
    _builder.newLineIfNotEmpty();
    return _builder;
  }
  
  private String getTextualRepresentation(final EObject o) {
    return NodeModelUtils.getTokenText(NodeModelUtils.findActualNodeFor(o));
  }
  
  private String getTextualReferenceRepresentation(final EObject o) {
    return NodeModelUtils.getTokenText(
      IterableExtensions.<INode>head(NodeModelUtils.findNodesForFeature(o, EdeltaPackage.Literals.EDELTA_ECORE_REFERENCE__ENAMEDELEMENT)));
  }
}
