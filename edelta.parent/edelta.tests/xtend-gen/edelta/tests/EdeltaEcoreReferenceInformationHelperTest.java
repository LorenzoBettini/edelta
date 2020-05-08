package edelta.tests;

import com.google.inject.Inject;
import edelta.edelta.EdeltaEcoreReference;
import edelta.edelta.EdeltaProgram;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter;
import edelta.util.EdeltaEcoreReferenceInformationHelper;
import edelta.util.EdeltaEcoreReferenceState;
import java.util.function.Function;
import org.assertj.core.api.Assertions;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter.class)
@SuppressWarnings("all")
public class EdeltaEcoreReferenceInformationHelperTest extends EdeltaAbstractTest {
  @Inject
  private EdeltaEcoreReferenceInformationHelper informationHelper;
  
  @Test
  public void testWhenAlreadySetThenReturnsTheStoredInformation() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaEcoreReference _lastEcoreRef = this.lastEcoreRef(this.parseWithTestEcore(_builder));
    final Procedure1<EdeltaEcoreReference> _function = (EdeltaEcoreReference it) -> {
      final EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation info1 = this.informationHelper.getOrComputeInformation(it);
      final EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation info2 = this.informationHelper.getOrComputeInformation(it);
      Assertions.<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation>assertThat(info1).isNotNull();
      Assertions.<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation>assertThat(info2).isNotNull();
      Assertions.<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation>assertThat(info1).isSameAs(info2);
    };
    ObjectExtensions.<EdeltaEcoreReference>operator_doubleArrow(_lastEcoreRef, _function);
  }
  
  @Test
  public void testReferenceToEPackage() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaEcoreReference _lastEcoreRef = this.lastEcoreRef(this.parseWithTestEcore(_builder));
    final Procedure1<EdeltaEcoreReference> _function = (EdeltaEcoreReference it) -> {
      final EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation info = this.informationHelper.getOrComputeInformation(it);
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_1 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getType();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_2 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getEPackageName();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_3 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getEClassifierName();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_4 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getENamedElementName();
      };
      Assertions.<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation>assertThat(info).<String>returns("EPackage", _function_1).<String>returns("foo", _function_2).<String>returns(null, _function_3).<String>returns(null, _function_4);
    };
    ObjectExtensions.<EdeltaEcoreReference>operator_doubleArrow(_lastEcoreRef, _function);
  }
  
  @Test
  public void testReferenceToSubPackage() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mainpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mainpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(mainsubpackage)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaEcoreReference _lastEcoreRef = this.lastEcoreRef(this.parseWithTestEcoreWithSubPackage(_builder));
    final Procedure1<EdeltaEcoreReference> _function = (EdeltaEcoreReference it) -> {
      final EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation info = this.informationHelper.getOrComputeInformation(it);
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_1 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getType();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_2 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getEPackageName();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_3 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getEClassifierName();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_4 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getENamedElementName();
      };
      Assertions.<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation>assertThat(info).<String>returns("EPackage", _function_1).<String>returns("mainpackage.mainsubpackage", _function_2).<String>returns(null, _function_3).<String>returns(null, _function_4);
    };
    ObjectExtensions.<EdeltaEcoreReference>operator_doubleArrow(_lastEcoreRef, _function);
  }
  
  @Test
  public void testReferenceToEStructuralFeatureWithSubPackage() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mainpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mainpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(mySubPackageAttribute)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaEcoreReference _lastEcoreRef = this.lastEcoreRef(this.parseWithTestEcoreWithSubPackage(_builder));
    final Procedure1<EdeltaEcoreReference> _function = (EdeltaEcoreReference it) -> {
      final EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation info = this.informationHelper.getOrComputeInformation(it);
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_1 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getType();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_2 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getEPackageName();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_3 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getEClassifierName();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_4 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getENamedElementName();
      };
      Assertions.<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation>assertThat(info).<String>returns("EAttribute", _function_1).<String>returns("mainpackage.mainsubpackage", _function_2).<String>returns("MainSubPackageFooClass", _function_3).<String>returns("mySubPackageAttribute", _function_4);
    };
    ObjectExtensions.<EdeltaEcoreReference>operator_doubleArrow(_lastEcoreRef, _function);
  }
  
  @Test
  public void testReferenceToSubPackageWithCycle() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mainpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mainpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(subsubpackage)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaEcoreReference _lastEcoreRef = this.lastEcoreRef(this.parseWithTestEcoreWithSubPackage(_builder));
    final Procedure1<EdeltaEcoreReference> _function = (EdeltaEcoreReference it) -> {
      ENamedElement _enamedelement = it.getEnamedelement();
      final EPackage subpackage = ((EPackage) _enamedelement);
      EList<EPackage> _eSubpackages = subpackage.getESubpackages();
      EPackage _eSuperPackage = subpackage.getESuperPackage();
      _eSubpackages.add(_eSuperPackage);
      final EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation info = this.informationHelper.getOrComputeInformation(it);
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_1 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getType();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_2 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getEPackageName();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_3 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getEClassifierName();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_4 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getENamedElementName();
      };
      Assertions.<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation>assertThat(info).<String>returns("EPackage", _function_1).<String>returns("subsubpackage", _function_2).<String>returns(null, _function_3).<String>returns(null, _function_4);
    };
    ObjectExtensions.<EdeltaEcoreReference>operator_doubleArrow(_lastEcoreRef, _function);
  }
  
  @Test
  public void testReferenceToEClassifier() {
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
    EdeltaEcoreReference _lastEcoreRef = this.lastEcoreRef(this.parseWithTestEcore(_builder));
    final Procedure1<EdeltaEcoreReference> _function = (EdeltaEcoreReference it) -> {
      final EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation info = this.informationHelper.getOrComputeInformation(it);
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_1 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getType();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_2 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getEPackageName();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_3 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getEClassifierName();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_4 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getENamedElementName();
      };
      Assertions.<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation>assertThat(info).<String>returns("EClass", _function_1).<String>returns("foo", _function_2).<String>returns("FooClass", _function_3).<String>returns(null, _function_4);
    };
    ObjectExtensions.<EdeltaEcoreReference>operator_doubleArrow(_lastEcoreRef, _function);
  }
  
  @Test
  public void testReferenceToEEnumLiteral() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(FooEnumLiteral)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaEcoreReference _lastEcoreRef = this.lastEcoreRef(this.parseWithTestEcore(_builder));
    final Procedure1<EdeltaEcoreReference> _function = (EdeltaEcoreReference it) -> {
      final EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation info = this.informationHelper.getOrComputeInformation(it);
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_1 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getType();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_2 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getEPackageName();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_3 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getEClassifierName();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_4 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getENamedElementName();
      };
      Assertions.<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation>assertThat(info).<String>returns("EEnumLiteral", _function_1).<String>returns("foo", _function_2).<String>returns("FooEnum", _function_3).<String>returns("FooEnumLiteral", _function_4);
    };
    ObjectExtensions.<EdeltaEcoreReference>operator_doubleArrow(_lastEcoreRef, _function);
  }
  
  @Test
  public void testReferenceToEStructuralFeature() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(myReference)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaEcoreReference _lastEcoreRef = this.lastEcoreRef(this.parseWithTestEcore(_builder));
    final Procedure1<EdeltaEcoreReference> _function = (EdeltaEcoreReference it) -> {
      final EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation info = this.informationHelper.getOrComputeInformation(it);
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_1 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getType();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_2 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getEPackageName();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_3 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getEClassifierName();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_4 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getENamedElementName();
      };
      Assertions.<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation>assertThat(info).<String>returns("EReference", _function_1).<String>returns("foo", _function_2).<String>returns("FooClass", _function_3).<String>returns("myReference", _function_4);
    };
    ObjectExtensions.<EdeltaEcoreReference>operator_doubleArrow(_lastEcoreRef, _function);
  }
  
  @Test
  public void testReferenceToUnresolved() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(unknown)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaEcoreReference _lastEcoreRef = this.lastEcoreRef(this.parseWithTestEcore(_builder));
    final Procedure1<EdeltaEcoreReference> _function = (EdeltaEcoreReference it) -> {
      final EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation info = this.informationHelper.getOrComputeInformation(it);
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_1 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getType();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_2 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getEPackageName();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_3 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getEClassifierName();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_4 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getENamedElementName();
      };
      Assertions.<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation>assertThat(info).<String>returns("ENamedElement", _function_1).<String>returns("", _function_2).<String>returns("", _function_3).<String>returns("", _function_4);
    };
    ObjectExtensions.<EdeltaEcoreReference>operator_doubleArrow(_lastEcoreRef, _function);
  }
  
  @Test
  public void testAfterChange() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(myAttribute)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaEcoreReference _lastEcoreRef = this.lastEcoreRef(this.parseWithTestEcore(_builder));
    final Procedure1<EdeltaEcoreReference> _function = (EdeltaEcoreReference it) -> {
      EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation info = this.informationHelper.getOrComputeInformation(it);
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_1 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getType();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_2 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getEPackageName();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_3 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getEClassifierName();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_4 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getENamedElementName();
      };
      Assertions.<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation>assertThat(info).<String>returns("EAttribute", _function_1).<String>returns("foo", _function_2).<String>returns("FooClass", _function_3).<String>returns("myAttribute", _function_4);
      ENamedElement _enamedelement = it.getEnamedelement();
      final EAttribute attr = ((EAttribute) _enamedelement);
      attr.setName("renamed");
      EClass _eContainingClass = attr.getEContainingClass();
      _eContainingClass.setName("Renamed");
      info = this.informationHelper.getOrComputeInformation(it);
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_5 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getType();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_6 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getEPackageName();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_7 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getEClassifierName();
      };
      final Function<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation, String> _function_8 = (EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation it_1) -> {
        return it_1.getENamedElementName();
      };
      Assertions.<EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation>assertThat(info).<String>returns("EAttribute", _function_5).<String>returns("foo", _function_6).<String>returns("FooClass", _function_7).<String>returns("myAttribute", _function_8);
    };
    ObjectExtensions.<EdeltaEcoreReference>operator_doubleArrow(_lastEcoreRef, _function);
  }
  
  private EdeltaEcoreReference lastEcoreRef(final EdeltaProgram p) {
    return this.lastEcoreReferenceExpression(p).getReference();
  }
}
