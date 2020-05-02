package edelta.tests;

import edelta.edelta.EdeltaProgram;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProviderCustom;
import edelta.util.EdeltaModelUtil;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
@SuppressWarnings("all")
public class EdeltaModelUtilTest extends EdeltaAbstractTest {
  @Test
  public void testGetProgram() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      Assert.assertSame(it, EdeltaModelUtil.getProgram(this.lastModifyEcoreOperation(it)));
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
}
