package edelta.tests;

import com.google.inject.Inject;
import edelta.edelta.EdeltaProgram;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProviderCustom;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.resource.ILocationInFileProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.util.ITextRegion;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.lib.Extension;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
@SuppressWarnings("all")
public class EdeltaLocationInFileProviderTest extends EdeltaAbstractTest {
  @Inject
  @Extension
  private ILocationInFileProvider _iLocationInFileProvider;
  
  @Test
  public void testCreatedEClass() {
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
    _builder.append("addNewEClass(\"NewClass\")");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    final EdeltaProgram program = this.parseWithTestEcore(input);
    final XExpression e = this.getBlockLastExpression(this.lastModifyEcoreOperation(program).getBody());
    final EClass derived = this.getLastCopiedEPackageLastEClass(program);
    final ITextRegion originalTextRegion = this._iLocationInFileProvider.getSignificantTextRegion(e);
    final ITextRegion derivedTextRegion = this._iLocationInFileProvider.getSignificantTextRegion(derived);
    Assert.assertEquals(originalTextRegion, derivedTextRegion);
  }
  
  @Test
  public void testOriginalEClass() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package test");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    final EdeltaProgram program = this.parseWithTestEcore(input);
    final EClass derived = this.getLastCopiedEPackageFirstEClass(program);
    final ITextRegion originalTextRegion = this._iLocationInFileProvider.getSignificantTextRegion(derived);
    Assert.assertSame(ITextRegion.EMPTY_REGION, originalTextRegion);
  }
}
