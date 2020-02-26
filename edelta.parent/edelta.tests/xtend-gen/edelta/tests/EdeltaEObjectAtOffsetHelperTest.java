package edelta.tests;

import com.google.inject.Inject;
import edelta.edelta.EdeltaProgram;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProviderCustom;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.resource.EObjectAtOffsetHelper;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.util.TextRegion;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure2;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
@SuppressWarnings("all")
public class EdeltaEObjectAtOffsetHelperTest extends EdeltaAbstractTest {
  @Inject
  @Extension
  private EObjectAtOffsetHelper _eObjectAtOffsetHelper;
  
  @Test
  public void testWithoutManipulationExpressions() {
    final Procedure2<EdeltaProgram, EObject> _function = (EdeltaProgram it, EObject linked) -> {
      Assert.assertSame(IterableExtensions.<EPackage>head(it.getMetamodels()).getEClassifier("FooClass"), linked);
    };
    this.resolveAtOffset("ecoreref(FooClass)", _function);
  }
  
  @Test
  public void testUnresolved() {
    final Procedure2<EdeltaProgram, EObject> _function = (EdeltaProgram it, EObject linked) -> {
      Assert.assertTrue(linked.eIsProxy());
    };
    this.resolveAtOffset("ecoreref(NonExistant)", _function);
  }
  
  @Test
  public void testNonEcoreReference() {
    final Procedure2<EdeltaProgram, EObject> _function = (EdeltaProgram it, EObject linked) -> {
      Assert.assertFalse(this.getVariableDeclaration(this.getBlockLastExpression(this.lastModifyEcoreOperation(it).getBody())).getType().getType().eIsProxy());
    };
    this.resolveAtOffset("val String s = null", "Str", _function);
  }
  
  private void resolveAtOffset(final CharSequence input, final Procedure2<? super EdeltaProgram, ? super EObject> tester) {
    this.resolveAtOffset(input, "ecoreref(", tester);
  }
  
  private void resolveAtOffset(final CharSequence body, final String stringRegion, final Procedure2<? super EdeltaProgram, ? super EObject> tester) {
    final CharSequence input = this.inputInsideModifyEcoreWithTestMetamodelFoo(body);
    final EdeltaProgram prog = this.parseWithTestEcore(input);
    int _lastIndexOf = input.toString().lastIndexOf(stringRegion);
    int _length = stringRegion.length();
    int _plus = (_lastIndexOf + _length);
    final int offset = (_plus + 1);
    Resource _eResource = prog.eResource();
    TextRegion _textRegion = new TextRegion(offset, 0);
    final INode crossRefNode = this._eObjectAtOffsetHelper.getCrossReferenceNode(((XtextResource) _eResource), _textRegion);
    final EObject crossLinkedEObject = this._eObjectAtOffsetHelper.getCrossReferencedElement(crossRefNode);
    tester.apply(prog, crossLinkedEObject);
  }
}
