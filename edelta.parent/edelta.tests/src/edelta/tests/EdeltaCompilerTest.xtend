package edelta.tests

import com.google.inject.Inject
import edelta.lib.EdeltaDefaultRuntime
import edelta.lib.EdeltaIssuePresenter
import edelta.lib.EdeltaModelManager
import edelta.lib.EdeltaRuntime
import edelta.tests.injectors.EdeltaInjectorProviderTestableDerivedStateComputer
import java.util.List
import java.util.function.Consumer
import org.eclipse.emf.ecore.ENamedElement
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.util.JavaVersion
import org.eclipse.xtext.xbase.testing.CompilationTestHelper
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import static edelta.testutils.EdeltaTestUtils.*
import static org.assertj.core.api.Assertions.*
import org.eclipse.xtext.testing.TemporaryFolder

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderTestableDerivedStateComputer)
class EdeltaCompilerTest extends EdeltaAbstractCompilerTest {

	@Rule @Inject public TemporaryFolder temporaryFolder
	@Inject extension CompilationTestHelper compilationTestHelper

	static final String MODIFIED = "modified";

	@Before
	def void setup() {
		compilationTestHelper.javaVersion = JavaVersion.JAVA8
	}

	@Test
	def void testEmptyProgram() {
		''''''.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			
			@SuppressWarnings("all")
			public class MyFile0 extends EdeltaDefaultRuntime {
			  public MyFile0(final EdeltaRuntime other) {
			    super(other);
			  }
			}
			'''
		)
	}

	@Test
	def void testIncompleteProgram() {
		'''metamodel '''.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			
			@SuppressWarnings("all")
			public class MyFile0 extends EdeltaDefaultRuntime {
			  public MyFile0(final EdeltaRuntime other) {
			    super(other);
			  }
			}
			''',
			false
		)
	}

	@Test
	def void testProgramWithPackage() {
		'''
		package foo
		'''.checkCompilation(
			'''
			package foo;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			
			@SuppressWarnings("all")
			public class MyFile0 extends EdeltaDefaultRuntime {
			  public MyFile0(final EdeltaRuntime other) {
			    super(other);
			  }
			}
			'''
		)
	}

	@Test
	def void testOperationWithInferredReturnType() {
		operationWithInferredReturnType.checkCompilation(
			'''
			package foo;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			
			@SuppressWarnings("all")
			public class MyFile0 extends EdeltaDefaultRuntime {
			  public MyFile0(final EdeltaRuntime other) {
			    super(other);
			  }
			
			  public boolean bar(final String s) {
			    return s.isEmpty();
			  }
			}
			'''
		)
	}

	@Test
	def void testOperationWithReturnType() {
		operationWithReturnType.checkCompilation(
			'''
			package foo;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			
			@SuppressWarnings("all")
			public class MyFile0 extends EdeltaDefaultRuntime {
			  public MyFile0(final EdeltaRuntime other) {
			    super(other);
			  }
			
			  public boolean bar(final String s) {
			    return s.isEmpty();
			  }
			}
			'''
		)
	}

	@Test
	def void testOperationAccessingLib() {
		operationAccessingLib.checkCompilation(
			'''
			package foo;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaEcoreUtil;
			import edelta.lib.EdeltaRuntime;
			import edelta.lib.EdeltaUtils;
			import java.util.function.Consumer;
			import org.eclipse.emf.ecore.EObject;
			
			@SuppressWarnings("all")
			public class MyFile0 extends EdeltaDefaultRuntime {
			  public MyFile0(final EdeltaRuntime other) {
			    super(other);
			  }
			
			  public EObject bar(final String s) {
			    EObject _xblockexpression = null;
			    {
			      EdeltaUtils.newEClass(s);
			      final Consumer<EObject> _function = (EObject it) -> {
			      };
			      _xblockexpression = EdeltaEcoreUtil.createInstance(getEClass("foo", "FooClass"), _function);
			    }
			    return _xblockexpression;
			  }
			
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			}
			'''
		)
	}

	@Test
	def void testOperationNewEClassWithInitializer() {
		operationNewEClassWithInitializer.checkCompilation(
			'''
			package foo;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			import edelta.lib.EdeltaUtils;
			import java.util.function.Consumer;
			import org.eclipse.emf.common.util.EList;
			import org.eclipse.emf.ecore.EClass;
			
			@SuppressWarnings("all")
			public class MyFile0 extends EdeltaDefaultRuntime {
			  public MyFile0(final EdeltaRuntime other) {
			    super(other);
			  }
			
			  public EClass bar(final String s) {
			    final Consumer<EClass> _function = (EClass it) -> {
			      EList<EClass> _eSuperTypes = it.getESuperTypes();
			      EClass _newEClass = EdeltaUtils.newEClass("Base");
			      _eSuperTypes.add(_newEClass);
			    };
			    return EdeltaUtils.newEClass(s, _function);
			  }
			}
			'''
		)
	}

	@Test
	def void testCompilationOfEcoreReferenceExpression() {
		'''
		package foo;
		
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			ecoreref(foo)
			println(ecoreref(foo))
			ecoreref(FooClass)
			println(ecoreref(FooClass))
			ecoreref(myAttribute)
			println(ecoreref(myAttribute))
			ecoreref(FooEnum)
			println(ecoreref(FooEnum))
			ecoreref(FooEnumLiteral)
			println(ecoreref(FooEnumLiteral))
			val ref = ecoreref(myReference)
		}
		'''.checkCompilation(
			'''
			package foo;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			import org.eclipse.emf.ecore.EAttribute;
			import org.eclipse.emf.ecore.EClass;
			import org.eclipse.emf.ecore.EEnum;
			import org.eclipse.emf.ecore.EEnumLiteral;
			import org.eclipse.emf.ecore.EPackage;
			import org.eclipse.emf.ecore.EReference;
			import org.eclipse.xtext.xbase.lib.InputOutput;
			
			@SuppressWarnings("all")
			public class MyFile0 extends EdeltaDefaultRuntime {
			  public MyFile0(final EdeltaRuntime other) {
			    super(other);
			  }
			
			  public void aTest(final EPackage it) {
			    getEPackage("foo");
			    InputOutput.<EPackage>println(getEPackage("foo"));
			    getEClass("foo", "FooClass");
			    InputOutput.<EClass>println(getEClass("foo", "FooClass"));
			    getEAttribute("foo", "FooClass", "myAttribute");
			    InputOutput.<EAttribute>println(getEAttribute("foo", "FooClass", "myAttribute"));
			    getEEnum("foo", "FooEnum");
			    InputOutput.<EEnum>println(getEEnum("foo", "FooEnum"));
			    getEEnumLiteral("foo", "FooEnum", "FooEnumLiteral");
			    InputOutput.<EEnumLiteral>println(getEEnumLiteral("foo", "FooEnum", "FooEnumLiteral"));
			    final EReference ref = getEReference("foo", "FooClass", "myReference");
			  }
			
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			
			  @Override
			  protected void doExecute() throws Exception {
			    aTest(getEPackage("foo"));
			  }
			}
			'''
		)
	}

	@Test
	def void testCompilationOfEclassExpressionWithNonExistantEClass() {
		'''
		package foo;
		
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			ecoreref(NonExistent)
		}
		'''.checkCompilation(
			'''
			package foo;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			import org.eclipse.emf.ecore.EPackage;
			
			@SuppressWarnings("all")
			public class MyFile0 extends EdeltaDefaultRuntime {
			  public MyFile0(final EdeltaRuntime other) {
			    super(other);
			  }
			
			  public void aTest(final EPackage it) {
			    getENamedElement("", "", "");
			  }
			
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			
			  @Override
			  protected void doExecute() throws Exception {
			    aTest(getEPackage("foo"));
			  }
			}
			''',
			false
		)
	}

	@Test
	def void testReferenceToCreatedEClass() {
		referenceToCreatedEClass.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			import org.eclipse.emf.ecore.EPackage;
			
			@SuppressWarnings("all")
			public class MyFile0 extends EdeltaDefaultRuntime {
			  public MyFile0(final EdeltaRuntime other) {
			    super(other);
			  }
			
			  public void aTest(final EPackage it) {
			    this.stdLib.addNewEClass(it, "NewClass");
			  }
			
			  public void anotherTest(final EPackage it) {
			    getEClass("foo", "NewClass");
			  }
			
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			
			  @Override
			  protected void doExecute() throws Exception {
			    aTest(getEPackage("foo"));
			    anotherTest(getEPackage("foo"));
			  }
			}
			'''
		)
	}

	@Test
	def void testReferenceToCreatedEClassRenamed() {
		// the name of the created EClass is changed
		// in the initialization block and the interpreter is executed
		// thus, we can access them both, but both with the new name.
		// However, a validation error is issued
		// see: https://github.com/LorenzoBettini/edelta/issues/113
		referenceToCreatedEClassRenamed.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			import org.eclipse.emf.ecore.EPackage;
			
			@SuppressWarnings("all")
			public class MyFile0 extends EdeltaDefaultRuntime {
			  public MyFile0(final EdeltaRuntime other) {
			    super(other);
			  }
			
			  public void creation(final EPackage it) {
			    this.stdLib.addNewEClass(it, "NewClass");
			  }
			
			  public void renaming(final EPackage it) {
			    getEClass("foo", "NewClass").setName("changed");
			  }
			
			  public void accessing(final EPackage it) {
			    getEClass("foo", "changed");
			    getEClass("foo", "changed");
			  }
			
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			
			  @Override
			  protected void doExecute() throws Exception {
			    creation(getEPackage("foo"));
			    renaming(getEPackage("foo"));
			    accessing(getEPackage("foo"));
			  }
			}
			''',
			false
		)
	}

	@Test
	def void testUseAs() {
		useAsCustomEdeltaCreatingEClass.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			import edelta.tests.additional.MyCustomEdelta;
			import org.eclipse.emf.ecore.EPackage;
			
			@SuppressWarnings("all")
			public class MyFile0 extends EdeltaDefaultRuntime {
			  private MyCustomEdelta my;
			
			  public MyFile0(final EdeltaRuntime other) {
			    super(other);
			    my = new MyCustomEdelta(this);
			  }
			
			  public void aTest(final EPackage it) {
			    this.my.createANewEAttribute(
			      this.my.createANewEClass());
			  }
			
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			
			  @Override
			  protected void doExecute() throws Exception {
			    aTest(getEPackage("foo"));
			  }
			}
			'''
		)
	}

	@Test
	def void testUseAsExtension() {
		useAsCustomEdeltaAsExtensionCreatingEClass.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			import edelta.tests.additional.MyCustomEdelta;
			import org.eclipse.emf.ecore.EPackage;
			import org.eclipse.xtext.xbase.lib.Extension;
			
			@SuppressWarnings("all")
			public class MyFile0 extends EdeltaDefaultRuntime {
			  @Extension
			  private MyCustomEdelta my;
			
			  public MyFile0(final EdeltaRuntime other) {
			    super(other);
			    my = new MyCustomEdelta(this);
			  }
			
			  public void aTest(final EPackage it) {
			    this.my.createANewEAttribute(this.my.createANewEClass());
			  }
			
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			
			  @Override
			  protected void doExecute() throws Exception {
			    aTest(getEPackage("foo"));
			  }
			}
			'''
		)
	}

	@Test
	def void testInvalidUseAs() {
		'''
			import edelta.tests.additional.MyCustomEdelta
			
			metamodel "foo"
			
			use MyCustomEdelta as
			use as my
			
			modifyEcore aTest epackage foo {
				my.myMethod()
			}
		'''.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			import edelta.tests.additional.MyCustomEdelta;
			import org.eclipse.emf.ecore.EPackage;
			
			@SuppressWarnings("all")
			public class MyFile0 extends EdeltaDefaultRuntime {
			  private Object my;
			
			  public MyFile0(final EdeltaRuntime other) {
			    super(other);
			     = new MyCustomEdelta(this);
			    my = new (this);
			  }
			
			  public void aTest(final EPackage it) {
			    this.my._myMethod;
			  }
			
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			
			  @Override
			  protected void doExecute() throws Exception {
			    aTest(getEPackage("foo"));
			  }
			}
			''',
			false
		)
	}

	@Test
	def void testUseAsExecution() {
		// the new created EClass and EAttribute are created by calling a method
		// of a custom Edelta implementation that is used in the program
		useAsCustomEdeltaCreatingEClass.checkCompiledCodeExecution(
			'''
			<?xml version="1.0" encoding="UTF-8"?>
			<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="foo" nsURI="http://foo" nsPrefix="foo">
			  <eClassifiers xsi:type="ecore:EClass" name="FooClass"/>
			  <eClassifiers xsi:type="ecore:EClass" name="FooDerivedClass" eSuperTypes="#//FooClass"/>
			  <eClassifiers xsi:type="ecore:EDataType" name="FooDataType" instanceClassName="java.lang.String"/>
			  <eClassifiers xsi:type="ecore:EClass" name="ANewClass">
			    <eStructuralFeatures xsi:type="ecore:EAttribute" name="aNewAttr" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
			  </eClassifiers>
			</ecore:EPackage>
			''',
			true
		)
	}

	@Test
	def void testUseAsExtensionExecution() {
		// the new created EClass and EAttribute are created by calling a method
		// of a custom Edelta implementation that is used in the program
		useAsCustomEdeltaAsExtensionCreatingEClass.checkCompiledCodeExecution(
			'''
			<?xml version="1.0" encoding="UTF-8"?>
			<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="foo" nsURI="http://foo" nsPrefix="foo">
			  <eClassifiers xsi:type="ecore:EClass" name="FooClass"/>
			  <eClassifiers xsi:type="ecore:EClass" name="FooDerivedClass" eSuperTypes="#//FooClass"/>
			  <eClassifiers xsi:type="ecore:EDataType" name="FooDataType" instanceClassName="java.lang.String"/>
			  <eClassifiers xsi:type="ecore:EClass" name="ANewClass">
			    <eStructuralFeatures xsi:type="ecore:EAttribute" name="aNewAttr" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
			  </eClassifiers>
			</ecore:EPackage>
			''',
			true
		)
	}

	@Test
	def void testStatefulUseAsExecution() {
		// the new created EClass and EAttribute are created by calling a method
		// of a custom Edelta implementation that is used in the program
		useAsCustomStatefulEdeltaCreatingEClass.checkCompiledCodeExecution(
			'''
			<?xml version="1.0" encoding="UTF-8"?>
			<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="foo" nsURI="http://foo" nsPrefix="foo">
			  <eClassifiers xsi:type="ecore:EClass" name="FooClass"/>
			  <eClassifiers xsi:type="ecore:EClass" name="FooDerivedClass" eSuperTypes="#//FooClass"/>
			  <eClassifiers xsi:type="ecore:EDataType" name="FooDataType" instanceClassName="java.lang.String"/>
			  <eClassifiers xsi:type="ecore:EClass" name="ANewClass1">
			    <eStructuralFeatures xsi:type="ecore:EAttribute" name="aNewAttr2" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
			  </eClassifiers>
			  <eClassifiers xsi:type="ecore:EClass" name="ANewClass3">
			    <eStructuralFeatures xsi:type="ecore:EAttribute" name="aNewAttr4" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
			  </eClassifiers>
			</ecore:EPackage>
			''',
			true
		)
	}

	@Test
	def void testModifyEcore() {
		'''
			metamodel "foo"
			
			modifyEcore aModificationTest epackage foo {
				EClassifiers += newEClass("ANewClass") [
					ESuperTypes += newEClass("Base")
				]
			}
		'''.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			import edelta.lib.EdeltaUtils;
			import java.util.function.Consumer;
			import org.eclipse.emf.common.util.EList;
			import org.eclipse.emf.ecore.EClass;
			import org.eclipse.emf.ecore.EClassifier;
			import org.eclipse.emf.ecore.EPackage;
			
			@SuppressWarnings("all")
			public class MyFile0 extends EdeltaDefaultRuntime {
			  public MyFile0(final EdeltaRuntime other) {
			    super(other);
			  }
			
			  public void aModificationTest(final EPackage it) {
			    EList<EClassifier> _eClassifiers = it.getEClassifiers();
			    final Consumer<EClass> _function = (EClass it_1) -> {
			      EList<EClass> _eSuperTypes = it_1.getESuperTypes();
			      EClass _newEClass = EdeltaUtils.newEClass("Base");
			      _eSuperTypes.add(_newEClass);
			    };
			    EClass _newEClass = EdeltaUtils.newEClass("ANewClass", _function);
			    _eClassifiers.add(_newEClass);
			  }
			
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			
			  @Override
			  protected void doExecute() throws Exception {
			    aModificationTest(getEPackage("foo"));
			  }
			}
			'''
		)
	}

	@Test
	def void testCompilationRenamedClassInModifyEcore() {
		'''
			metamodel "foo"
			
			modifyEcore modifyFoo epackage foo {
				ecoreref(foo.FooClass).name = "RenamedClass"
				ecoreref(RenamedClass).EStructuralFeatures +=
					newEAttribute("anotherAttr", ecoreref(FooDataType))
				ecoreref(RenamedClass).abstract = true
				ecoreref(foo.RenamedClass) => [abstract = true]
			}
		'''.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			import edelta.lib.EdeltaUtils;
			import org.eclipse.emf.common.util.EList;
			import org.eclipse.emf.ecore.EAttribute;
			import org.eclipse.emf.ecore.EClass;
			import org.eclipse.emf.ecore.EPackage;
			import org.eclipse.emf.ecore.EStructuralFeature;
			import org.eclipse.xtext.xbase.lib.ObjectExtensions;
			import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
			
			@SuppressWarnings("all")
			public class MyFile0 extends EdeltaDefaultRuntime {
			  public MyFile0(final EdeltaRuntime other) {
			    super(other);
			  }
			
			  public void modifyFoo(final EPackage it) {
			    getEClass("foo", "FooClass").setName("RenamedClass");
			    EList<EStructuralFeature> _eStructuralFeatures = getEClass("foo", "RenamedClass").getEStructuralFeatures();
			    EAttribute _newEAttribute = EdeltaUtils.newEAttribute("anotherAttr", getEDataType("foo", "FooDataType"));
			    _eStructuralFeatures.add(_newEAttribute);
			    getEClass("foo", "RenamedClass").setAbstract(true);
			    final Procedure1<EClass> _function = (EClass it_1) -> {
			      it_1.setAbstract(true);
			    };
			    ObjectExtensions.<EClass>operator_doubleArrow(
			      getEClass("foo", "RenamedClass"), _function);
			  }
			
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			
			  @Override
			  protected void doExecute() throws Exception {
			    modifyFoo(getEPackage("foo"));
			  }
			}
			'''
		)
	}

	@Test
	def void testExecutionRenamedClassInModifyEcore() {
		'''
			metamodel "foo"
			
			modifyEcore modifyFoo epackage foo {
				ecoreref(foo.FooClass).name = "RenamedClass"
				ecoreref(RenamedClass).EStructuralFeatures +=
					newEAttribute("anotherAttr", ecoreref(FooDataType))
				ecoreref(RenamedClass).abstract = true
			}
		'''.checkCompiledCodeExecution(
			'''
			<?xml version="1.0" encoding="UTF-8"?>
			<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="foo" nsURI="http://foo" nsPrefix="foo">
			  <eClassifiers xsi:type="ecore:EClass" name="RenamedClass" abstract="true">
			    <eStructuralFeatures xsi:type="ecore:EAttribute" name="anotherAttr" eType="#//FooDataType"/>
			  </eClassifiers>
			  <eClassifiers xsi:type="ecore:EClass" name="FooDerivedClass" eSuperTypes="#//RenamedClass"/>
			  <eClassifiers xsi:type="ecore:EDataType" name="FooDataType" instanceClassName="java.lang.String"/>
			</ecore:EPackage>
			''',
			true
		)
	}

	@Test
	def void testCompilationAfterInterpretationOfCreatedEClassStealingAttribute() {
		createEClassStealingAttribute.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			import java.util.function.Consumer;
			import org.eclipse.emf.ecore.EClass;
			import org.eclipse.emf.ecore.EPackage;
			
			@SuppressWarnings("all")
			public class MyFile0 extends EdeltaDefaultRuntime {
			  public MyFile0(final EdeltaRuntime other) {
			    super(other);
			  }
			
			  public void aTest(final EPackage it) {
			    final Consumer<EClass> _function = (EClass it_1) -> {
			      this.stdLib.addEAttribute(it_1, getEAttribute("foo", "FooClass", "myAttribute"));
			    };
			    this.stdLib.addNewEClass(it, "NewClass", _function);
			  }
			
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			
			  @Override
			  protected void doExecute() throws Exception {
			    aTest(getEPackage("foo"));
			  }
			}
			'''
			// Note:
			// it must be getEAttribute("foo", "FooClass", "myAttribute")
			// since we use the originalENamedElement
			// (the interpreter changed the container of myAttribute to NewClass)
		)
	}

	@Test
	def void testCompilationAfterInterpretationChangeEClassRemovingAttribute() {
		changeEClassRemovingAttribute.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			import org.eclipse.emf.common.util.EList;
			import org.eclipse.emf.ecore.EPackage;
			import org.eclipse.emf.ecore.EStructuralFeature;
			
			@SuppressWarnings("all")
			public class MyFile0 extends EdeltaDefaultRuntime {
			  public MyFile0(final EdeltaRuntime other) {
			    super(other);
			  }
			
			  public void aTest(final EPackage it) {
			    EList<EStructuralFeature> _eStructuralFeatures = getEClass("foo", "FooClass").getEStructuralFeatures();
			    _eStructuralFeatures.remove(getEAttribute("foo", "FooClass", "myAttribute"));
			  }
			
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			
			  @Override
			  protected void doExecute() throws Exception {
			    aTest(getEPackage("foo"));
			  }
			}
			'''
			// Note:
			// it must be getEAttribute("foo", "FooClass", "myAttribute")
			// since we use the originalENamedElement
			// (the interpeter removes the myAttribute from FooClass)
		)
	}

	@Test
	def void testCompilationOfModifyEcoreCallingLibMethods() {
		modifyEcoreUsingLibMethods.
		checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			import java.util.function.Consumer;
			import org.eclipse.emf.ecore.EAttribute;
			import org.eclipse.emf.ecore.EClass;
			import org.eclipse.emf.ecore.EEnum;
			import org.eclipse.emf.ecore.EEnumLiteral;
			import org.eclipse.emf.ecore.EPackage;
			import org.eclipse.emf.ecore.EReference;
			
			@SuppressWarnings("all")
			public class MyFile0 extends EdeltaDefaultRuntime {
			  public MyFile0(final EdeltaRuntime other) {
			    super(other);
			  }
			
			  public void aTest(final EPackage it) {
			    final Consumer<EClass> _function = (EClass it_1) -> {
			      final Consumer<EAttribute> _function_1 = (EAttribute it_2) -> {
			        it_2.setLowerBound(1);
			      };
			      this.stdLib.addNewEAttribute(it_1, "ANewAttribute", getEDataType("foo", "FooDataType"), _function_1);
			      final Consumer<EReference> _function_2 = (EReference it_2) -> {
			        it_2.setLowerBound(1);
			      };
			      this.stdLib.addNewEReference(it_1, "ANewReference", getEClass("foo", "FooClass"), _function_2);
			    };
			    this.stdLib.addNewEClass(it, "ANewClass", _function);
			    final Consumer<EEnum> _function_1 = (EEnum it_1) -> {
			      final Consumer<EEnumLiteral> _function_2 = (EEnumLiteral it_2) -> {
			        it_2.setValue(10);
			      };
			      this.stdLib.addNewEEnumLiteral(it_1, "ANewEnumLiteral", _function_2);
			    };
			    this.stdLib.addNewEEnum(it, "ANewEnum", _function_1);
			    this.stdLib.addNewEDataType(it, "ANewDataType", "java.lang.String");
			    getEClass("foo", "ANewClass");
			    getEAttribute("foo", "ANewClass", "ANewAttribute");
			    getEReference("foo", "ANewClass", "ANewReference");
			    getEEnum("foo", "ANewEnum");
			    getEEnumLiteral("foo", "ANewEnum", "ANewEnumLiteral");
			    getEDataType("foo", "ANewDataType");
			  }
			
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			
			  @Override
			  protected void doExecute() throws Exception {
			    aTest(getEPackage("foo"));
			  }
			}
			'''
		)
	}

	@Test
	def void testCompilationEcorerefWhenAttributeRemovedFromOriginalContainer() {
		'''
			metamodel "foo"
			
			modifyEcore modifyFoo epackage foo {
				ecoreref(foo.FooClass).name = "Renamed"
				ecoreref(foo.Renamed).EStructuralFeatures.remove(ecoreref(foo.Renamed.myAttribute))
			}
		'''.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			import org.eclipse.emf.common.util.EList;
			import org.eclipse.emf.ecore.EPackage;
			import org.eclipse.emf.ecore.EStructuralFeature;
			
			@SuppressWarnings("all")
			public class MyFile0 extends EdeltaDefaultRuntime {
			  public MyFile0(final EdeltaRuntime other) {
			    super(other);
			  }
			
			  public void modifyFoo(final EPackage it) {
			    getEClass("foo", "FooClass").setName("Renamed");
			    EList<EStructuralFeature> _eStructuralFeatures = getEClass("foo", "Renamed").getEStructuralFeatures();
			    _eStructuralFeatures.remove(getEAttribute("foo", "Renamed", "myAttribute"));
			  }
			
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			
			  @Override
			  protected void doExecute() throws Exception {
			    modifyFoo(getEPackage("foo"));
			  }
			}
			'''
		)
	}

	@Test
	def void testCompilationEcorerefWhenElementRemovedFromEcoreReference() {
		// EcoreUtil.delete sets to null also the ENamedElement of the ecoreref
		// see https://github.com/LorenzoBettini/edelta/issues/271
		'''
			import static org.eclipse.emf.ecore.util.EcoreUtil.delete
			
			metamodel "foo"
			
			modifyEcore modifyFoo epackage foo {
				delete(ecoreref(FooClass))
			}
		'''.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			import org.eclipse.emf.ecore.EPackage;
			import org.eclipse.emf.ecore.util.EcoreUtil;
			
			@SuppressWarnings("all")
			public class MyFile0 extends EdeltaDefaultRuntime {
			  public MyFile0(final EdeltaRuntime other) {
			    super(other);
			  }
			
			  public void modifyFoo(final EPackage it) {
			    EcoreUtil.delete(getEClass("foo", "FooClass"));
			  }
			
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			
			  @Override
			  protected void doExecute() throws Exception {
			    modifyFoo(getEPackage("foo"));
			  }
			}
			'''
		)
	}

	@Test
	def void testCompilationOfComplexOperationsWithSubPackages() {
		'''
			metamodel "foo"
			
			modifyEcore modifyFoo epackage foo {
				addNewESubpackage("anewsubpackage", "aprefix", "aURI") [
					addNewESubpackage("anestedsubpackage", "aprefix2", "aURI2") [
						addNewEClass("ANestedSubPackageClass")
					]
				]
				ecoreref(anewsubpackage).addNewEClass("NewClass") [
					EStructuralFeatures +=
						newEReference("newTestRef", ecoreref(ANestedSubPackageClass))
				]
				ecoreref(NewClass).name = "RenamedClass"
				ecoreref(RenamedClass).getEStructuralFeatures +=
					newEAttribute("added", ecoreref(FooDataType))
			}
		'''.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			import edelta.lib.EdeltaUtils;
			import java.util.function.Consumer;
			import org.eclipse.emf.common.util.EList;
			import org.eclipse.emf.ecore.EAttribute;
			import org.eclipse.emf.ecore.EClass;
			import org.eclipse.emf.ecore.EPackage;
			import org.eclipse.emf.ecore.EReference;
			import org.eclipse.emf.ecore.EStructuralFeature;
			
			@SuppressWarnings("all")
			public class MyFile0 extends EdeltaDefaultRuntime {
			  public MyFile0(final EdeltaRuntime other) {
			    super(other);
			  }
			
			  public void modifyFoo(final EPackage it) {
			    final Consumer<EPackage> _function = (EPackage it_1) -> {
			      final Consumer<EPackage> _function_1 = (EPackage it_2) -> {
			        this.stdLib.addNewEClass(it_2, "ANestedSubPackageClass");
			      };
			      this.stdLib.addNewESubpackage(it_1, "anestedsubpackage", "aprefix2", "aURI2", _function_1);
			    };
			    this.stdLib.addNewESubpackage(it, "anewsubpackage", "aprefix", "aURI", _function);
			    final Consumer<EClass> _function_1 = (EClass it_1) -> {
			      EList<EStructuralFeature> _eStructuralFeatures = it_1.getEStructuralFeatures();
			      EReference _newEReference = EdeltaUtils.newEReference("newTestRef", getEClass("foo.anewsubpackage.anestedsubpackage", "ANestedSubPackageClass"));
			      _eStructuralFeatures.add(_newEReference);
			    };
			    this.stdLib.addNewEClass(getEPackage("foo.anewsubpackage"), "NewClass", _function_1);
			    getEClass("foo.anewsubpackage", "NewClass").setName("RenamedClass");
			    EList<EStructuralFeature> _eStructuralFeatures = getEClass("foo.anewsubpackage", "RenamedClass").getEStructuralFeatures();
			    EAttribute _newEAttribute = EdeltaUtils.newEAttribute("added", getEDataType("foo", "FooDataType"));
			    _eStructuralFeatures.add(_newEAttribute);
			  }
			
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			
			  @Override
			  protected void doExecute() throws Exception {
			    modifyFoo(getEPackage("foo"));
			  }
			}
			'''
		)
	}

	@Test
	def void testCompilationOfRenameReferencesAcrossEPackagesWithRealEcoreFiles() {
		// it is crucial to use real ecore files so that we mimick what happens in
		// the workbench and make sure that original ecores are not modified.
		val rs = createResourceSetWithEcores(
		#[TEST1_REFS_ECORE, TEST2_REFS_ECORE],
		'''
			package test
			
			metamodel "testecoreforreferences1"
			metamodel "testecoreforreferences2"

			modifyEcore aTest1 epackage testecoreforreferences1 {
				// renames WorkPlace.persons to renamedPersons
				ecoreref(Person.works).EOpposite.name = "renamedPersons"
			}
			modifyEcore aTest2 epackage testecoreforreferences2 {
				// renames Person.works to renamedWorks
				// using the already renamed feature (was persons)
				ecoreref(renamedPersons).EOpposite.name = "renamedWorks"
			}
		''')
		rs.addEPackagesWithReferencesForTests
		checkCompilation(rs,
			'''
			package test;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			import org.eclipse.emf.ecore.EPackage;
			import org.eclipse.emf.ecore.EReference;
			
			@SuppressWarnings("all")
			public class Example extends EdeltaDefaultRuntime {
			  public Example(final EdeltaRuntime other) {
			    super(other);
			  }
			
			  public void aTest1(final EPackage it) {
			    EReference _eOpposite = getEReference("testecoreforreferences1", "Person", "works").getEOpposite();
			    _eOpposite.setName("renamedPersons");
			  }
			
			  public void aTest2(final EPackage it) {
			    EReference _eOpposite = getEReference("testecoreforreferences2", "WorkPlace", "renamedPersons").getEOpposite();
			    _eOpposite.setName("renamedWorks");
			  }
			
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("testecoreforreferences1");
			    ensureEPackageIsLoaded("testecoreforreferences2");
			  }
			
			  @Override
			  protected void doExecute() throws Exception {
			    aTest1(getEPackage("testecoreforreferences1"));
			    aTest2(getEPackage("testecoreforreferences2"));
			  }
			}
			''',
			true
		)
	}

	@Test
	def void testExecutionOfRenameReferencesAcrossEPackagesWithRealEcoreFiles() {
		checkCompiledCodeExecution(
			#[TEST1_REFS_ECORE, TEST2_REFS_ECORE],
			'''
				package test

				metamodel "testecoreforreferences1"
				metamodel "testecoreforreferences2"

				modifyEcore aTest1 epackage testecoreforreferences1 {
					// renames WorkPlace.persons to renamedPersons
					ecoreref(Person.works).EOpposite.name = "renamedPersons"
				}
				modifyEcore aTest2 epackage testecoreforreferences2 {
					// renames Person.works to renamedWorks
					// using the already renamed feature (was persons)
					ecoreref(renamedPersons).EOpposite.name = "renamedWorks"
				}
			''',
			#[
				TEST1_REFS_ECORE ->
				'''
				<?xml version="1.0" encoding="UTF-8"?>
				<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="testecoreforreferences1" nsURI="http://my.testecoreforreferences1.org"
				    nsPrefix="testecoreforreferences1">
				  <eClassifiers xsi:type="ecore:EClass" name="Person">
				    <eStructuralFeatures xsi:type="ecore:EAttribute" name="firstname" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
				    <eStructuralFeatures xsi:type="ecore:EAttribute" name="lastname" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
				    <eStructuralFeatures xsi:type="ecore:EReference" name="renamedWorks" lowerBound="1"
				        eType="ecore:EClass TestEcoreForReferences2.ecore#//WorkPlace" eOpposite="TestEcoreForReferences2.ecore#//WorkPlace/renamedPersons"/>
				  </eClassifiers>
				</ecore:EPackage>
				''',
				TEST2_REFS_ECORE ->
				'''
				<?xml version="1.0" encoding="UTF-8"?>
				<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="testecoreforreferences2" nsURI="http://my.testecoreforreferences2.org"
				    nsPrefix="testecoreforreferences2">
				  <eClassifiers xsi:type="ecore:EClass" name="WorkPlace">
				    <eStructuralFeatures xsi:type="ecore:EReference" name="renamedPersons" upperBound="-1"
				        eType="ecore:EClass TestEcoreForReferences1.ecore#//Person" eOpposite="TestEcoreForReferences1.ecore#//Person/renamedWorks"/>
				    <eStructuralFeatures xsi:type="ecore:EAttribute" name="address" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
				  </eClassifiers>
				</ecore:EPackage>
				'''
			],
			true
		)
	}

	@Test
	def void testCompilationOfRenameReferencesAcrossEPackagesSingleModifyEcore() {
		// it is crucial to use real ecore files so that we mimic what happens in
		// the workbench and make sure that original ecores are not modified.
		val rs = createResourceSetWithEcores(
		#[TEST1_REFS_ECORE, TEST2_REFS_ECORE],
		'''
			package test
			
			metamodel "testecoreforreferences1"
			metamodel "testecoreforreferences2"

			modifyEcore aTest1 epackage testecoreforreferences1 {
				// renames WorkPlace.persons to renamedPersons
				ecoreref(Person.works).EOpposite.name = "renamedPersons"
				// renames Person.works to renamedWorks
				// using the already renamed feature (was persons)
				ecoreref(renamedPersons).EOpposite.name = "renamedWorks"
			}
		''')
		rs.addEPackagesWithReferencesForTests
		checkCompilation(rs,
			'''
			package test;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			import org.eclipse.emf.ecore.EPackage;
			import org.eclipse.emf.ecore.EReference;
			
			@SuppressWarnings("all")
			public class Example extends EdeltaDefaultRuntime {
			  public Example(final EdeltaRuntime other) {
			    super(other);
			  }
			
			  public void aTest1(final EPackage it) {
			    EReference _eOpposite = getEReference("testecoreforreferences1", "Person", "works").getEOpposite();
			    _eOpposite.setName("renamedPersons");
			    EReference _eOpposite_1 = getEReference("testecoreforreferences2", "WorkPlace", "renamedPersons").getEOpposite();
			    _eOpposite_1.setName("renamedWorks");
			  }
			
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("testecoreforreferences1");
			    ensureEPackageIsLoaded("testecoreforreferences2");
			  }
			
			  @Override
			  protected void doExecute() throws Exception {
			    aTest1(getEPackage("testecoreforreferences1"));
			  }
			}
			''',
			true
		)
	}

	@Test
	def void testExecutionOfRenameReferencesAcrossEPackagesSingleModifyEcore() {
		checkCompiledCodeExecution(
			#[TEST1_REFS_ECORE, TEST2_REFS_ECORE],
			'''
				package test

				metamodel "testecoreforreferences1"
				metamodel "testecoreforreferences2"

				modifyEcore aTest1 epackage testecoreforreferences1 {
					// renames WorkPlace.persons to renamedPersons
					ecoreref(Person.works).EOpposite.name = "renamedPersons"
					// renames Person.works to renamedWorks
					// using the already renamed feature (was persons)
					ecoreref(renamedPersons).EOpposite.name = "renamedWorks"
				}
			''',
			#[
				TEST1_REFS_ECORE ->
				'''
				<?xml version="1.0" encoding="UTF-8"?>
				<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="testecoreforreferences1" nsURI="http://my.testecoreforreferences1.org"
				    nsPrefix="testecoreforreferences1">
				  <eClassifiers xsi:type="ecore:EClass" name="Person">
				    <eStructuralFeatures xsi:type="ecore:EAttribute" name="firstname" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
				    <eStructuralFeatures xsi:type="ecore:EAttribute" name="lastname" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
				    <eStructuralFeatures xsi:type="ecore:EReference" name="renamedWorks" lowerBound="1"
				        eType="ecore:EClass TestEcoreForReferences2.ecore#//WorkPlace" eOpposite="TestEcoreForReferences2.ecore#//WorkPlace/renamedPersons"/>
				  </eClassifiers>
				</ecore:EPackage>
				''',
				TEST2_REFS_ECORE ->
				'''
				<?xml version="1.0" encoding="UTF-8"?>
				<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="testecoreforreferences2" nsURI="http://my.testecoreforreferences2.org"
				    nsPrefix="testecoreforreferences2">
				  <eClassifiers xsi:type="ecore:EClass" name="WorkPlace">
				    <eStructuralFeatures xsi:type="ecore:EReference" name="renamedPersons" upperBound="-1"
				        eType="ecore:EClass TestEcoreForReferences1.ecore#//Person" eOpposite="TestEcoreForReferences1.ecore#//Person/renamedWorks"/>
				    <eStructuralFeatures xsi:type="ecore:EAttribute" name="address" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
				  </eClassifiers>
				</ecore:EPackage>
				'''
			],
			true
		)
	}

	@Test
	def void testExecutionOfModificationsOfMetamodelsAcrossSeveralFilesIntroducingDepOnAnotherMetamodel() {
		checkCompiledCodeExecutionWithSeveralFiles(
			#[SIMPLE_ECORE, ANOTHER_SIMPLE_ECORE],
			#[
				'''
					import org.eclipse.emf.ecore.EClass
					
					package test1
					
					metamodel "simple"
					
					def setBaseClass(EClass c) : void {
						c.getESuperTypes += ecoreref(SimpleClass)
					}
				''',
				'''
					import org.eclipse.emf.ecore.EClass
					import test1.MyFile0
					
					package test2
					
					metamodel "anothersimple"
					
					use test1.MyFile0 as extension my
					
					modifyEcore aModificationTest epackage anothersimple {
						// the other file's operation will set the
						// base class of this package class to another package class
						ecoreref(AnotherSimpleClass).setBaseClass
						// now anothersimple refers to simple
						// now modify the abstract property of the
						// superclass in the other package
						ecoreref(AnotherSimpleClass).ESuperTypes.head.abstract = true
					}
				'''
			],
			"test2.MyFile1",
			#[
				SIMPLE_ECORE ->
				'''
				<?xml version="1.0" encoding="UTF-8"?>
				<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="simple" nsURI="http://www.simple" nsPrefix="simple">
				  <eClassifiers xsi:type="ecore:EClass" name="SimpleClass" abstract="true"/>
				</ecore:EPackage>
				''',
				ANOTHER_SIMPLE_ECORE ->
				'''
				<?xml version="1.0" encoding="UTF-8"?>
				<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="anothersimple" nsURI="http://www.anothersimple" nsPrefix="anothersimple">
				  <eClassifiers xsi:type="ecore:EClass" name="AnotherSimpleClass" eSuperTypes="Simple.ecore#//SimpleClass"/>
				</ecore:EPackage>
				'''
			],
			true
		)
	}

	@Test
	def void testExecutionOfModificationsOfMetamodelsAcrossSeveralFilesIntroducingMutualDepOnAnotherMetamodel() {
		checkCompiledCodeExecutionWithSeveralFiles(
			#[SIMPLE_ECORE, ANOTHER_SIMPLE_ECORE],
			#[
				'''
					import org.eclipse.emf.ecore.EClass
					
					package test1
					
					metamodel "simple"
					
					def setBaseClass(EClass c) : void {
						c.getESuperTypes += ecoreref(SimpleClass)
					}
				''',
				'''
					import org.eclipse.emf.ecore.EClass
					import test1.MyFile0
					
					package test2
					
					metamodel "anothersimple"
					
					use test1.MyFile0 as extension my
					
					modifyEcore aModificationTest epackage anothersimple {
						// the other file's operation will set the
						// base class of this package class to another package class
						ecoreref(AnotherSimpleClass).setBaseClass
						// now anothersimple refers to simple (created dependency)
					
						val referenceToSuperClass = ecoreref(AnotherSimpleClass).ESuperTypes.head
					
						// also add a reference to the other epackage
						ecoreref(AnotherSimpleClass)
							.addNewEReference(
								"aReferenceToSimpleClass",
								referenceToSuperClass
							)
					
						// now modify the superclass in the other package
						// introducing a mutual dependency
						referenceToSuperClass
							.addNewEReference("aReferenceToAnotherSimpleClass", ecoreref(AnotherSimpleClass)) [
								// also make the references bidirectional
								EOpposite = ecoreref(aReferenceToSimpleClass)
								ecoreref(aReferenceToSimpleClass).EOpposite = it
							]
					}
				'''
			],
			"test2.MyFile1",
			#[
				SIMPLE_ECORE ->
				'''
				<?xml version="1.0" encoding="UTF-8"?>
				<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="simple" nsURI="http://www.simple" nsPrefix="simple">
				  <eClassifiers xsi:type="ecore:EClass" name="SimpleClass">
				    <eStructuralFeatures xsi:type="ecore:EReference" name="aReferenceToAnotherSimpleClass"
				        eType="ecore:EClass AnotherSimple.ecore#//AnotherSimpleClass" eOpposite="AnotherSimple.ecore#//AnotherSimpleClass/aReferenceToSimpleClass"/>
				  </eClassifiers>
				</ecore:EPackage>
				''',
				ANOTHER_SIMPLE_ECORE ->
				'''
				<?xml version="1.0" encoding="UTF-8"?>
				<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="anothersimple" nsURI="http://www.anothersimple" nsPrefix="anothersimple">
				  <eClassifiers xsi:type="ecore:EClass" name="AnotherSimpleClass" eSuperTypes="Simple.ecore#//SimpleClass">
				    <eStructuralFeatures xsi:type="ecore:EReference" name="aReferenceToSimpleClass"
				        eType="ecore:EClass Simple.ecore#//SimpleClass" eOpposite="Simple.ecore#//SimpleClass/aReferenceToAnotherSimpleClass"/>
				  </eClassifiers>
				</ecore:EPackage>
				'''
			],
			true
		)
	}

	@Test
	def void testExecutionOfComplexOperationsWithSubPackages() {
		'''
			metamodel "foo"
			
			modifyEcore modifyFoo epackage foo {
				addNewESubpackage("anewsubpackage", "anewsubpackage", "http://anewsubpackage") [
					addNewESubpackage("anestedsubpackage", "anestedsubpackage", "http://anestedsubpackage") [
						addNewEClass("ANestedSubPackageClass")
					]
				]
				ecoreref(anewsubpackage).addNewEClass("NewClass") [
					EStructuralFeatures +=
						newEReference("newTestRef", ecoreref(ANestedSubPackageClass))
				]
				ecoreref(NewClass).name = "RenamedClass"
				ecoreref(RenamedClass).getEStructuralFeatures +=
					newEAttribute("added", ecoreref(FooDataType))
			}
		'''.checkCompiledCodeExecution(
			'''
			<?xml version="1.0" encoding="UTF-8"?>
			<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="foo" nsURI="http://foo" nsPrefix="foo">
			  <eClassifiers xsi:type="ecore:EClass" name="FooClass"/>
			  <eClassifiers xsi:type="ecore:EClass" name="FooDerivedClass" eSuperTypes="#//FooClass"/>
			  <eClassifiers xsi:type="ecore:EDataType" name="FooDataType" instanceClassName="java.lang.String"/>
			  <eSubpackages name="anewsubpackage" nsURI="http://anewsubpackage" nsPrefix="anewsubpackage">
			    <eClassifiers xsi:type="ecore:EClass" name="RenamedClass">
			      <eStructuralFeatures xsi:type="ecore:EReference" name="newTestRef" eType="#//anewsubpackage/anestedsubpackage/ANestedSubPackageClass"/>
			      <eStructuralFeatures xsi:type="ecore:EAttribute" name="added" eType="#//FooDataType"/>
			    </eClassifiers>
			    <eSubpackages name="anestedsubpackage" nsURI="http://anestedsubpackage" nsPrefix="anestedsubpackage">
			      <eClassifiers xsi:type="ecore:EClass" name="ANestedSubPackageClass"/>
			    </eSubpackages>
			  </eSubpackages>
			</ecore:EPackage>
			''',
			true
		)
	}

	@Test
	def void testCompilationOfSeveralFilesWithUseAs() {
		#[
		'''
			import org.eclipse.emf.ecore.EClass
			import org.eclipse.emf.ecore.EcorePackage

			package test1
			
			def enrichWithReference(EClass c, String prefix) : void {
				c.addNewEReference(prefix + "Ref",
					EcorePackage.eINSTANCE.EObject)
			}
		''',
		'''
			import org.eclipse.emf.ecore.EClass
			import org.eclipse.emf.ecore.EcorePackage
			import test1.MyFile0

			package test2
			
			use test1.MyFile0 as extension my

			def enrichWithAttribute(EClass c, String prefix) : void {
				c.addNewEAttribute(prefix + "Attr",
					EcorePackage.eINSTANCE.EString)
				c.enrichWithReference(prefix)
			}
		''',
		'''
			import org.eclipse.emf.ecore.EClass
			import test2.MyFile1
			
			package test3
			
			metamodel "foo"
			
			use test2.MyFile1 as extension my
			
			modifyEcore aModificationTest epackage foo {
				ecoreref(FooClass)
					.enrichWithAttribute("prefix")
				// attribute and reference are added by the calls
				// to external operations!
				ecoreref(prefixAttr).changeable = true
				ecoreref(prefixRef).containment = true
			}
		'''].checkCompilationOfSeveralFiles(
			#[
				"test3.MyFile2" ->
				'''
				package test3;
				
				import edelta.lib.EdeltaDefaultRuntime;
				import edelta.lib.EdeltaRuntime;
				import org.eclipse.emf.ecore.EPackage;
				import org.eclipse.xtext.xbase.lib.Extension;
				import test2.MyFile1;
				
				@SuppressWarnings("all")
				public class MyFile2 extends EdeltaDefaultRuntime {
				  @Extension
				  private MyFile1 my;
				
				  public MyFile2(final EdeltaRuntime other) {
				    super(other);
				    my = new MyFile1(this);
				  }
				
				  public void aModificationTest(final EPackage it) {
				    this.my.enrichWithAttribute(getEClass("foo", "FooClass"), "prefix");
				    getEAttribute("foo", "FooClass", "prefixAttr").setChangeable(true);
				    getEReference("foo", "FooClass", "prefixRef").setContainment(true);
				  }
				
				  @Override
				  public void performSanityChecks() throws Exception {
				    ensureEPackageIsLoaded("foo");
				  }
				
				  @Override
				  protected void doExecute() throws Exception {
				    aModificationTest(getEPackage("foo"));
				  }
				}
				'''
			]
		)
	}

	@Test
	def void testExecutionOfSeveralFilesWithUseAs() {
		checkCompiledCodeExecutionWithSeveralFiles(
			#[SIMPLE_ECORE],
			#[
			'''
				import org.eclipse.emf.ecore.EClass
				import org.eclipse.emf.ecore.EcorePackage

				package test1
				
				def enrichWithReference(EClass c, String prefix) : void {
					c.addNewEReference(prefix + "Ref",
						EcorePackage.eINSTANCE.EObject)
				}
			''',
			'''
				import org.eclipse.emf.ecore.EClass
				import org.eclipse.emf.ecore.EcorePackage
				import test1.MyFile0

				package test2
				
				use test1.MyFile0 as extension my

				def enrichWithAttribute(EClass c, String prefix) : void {
					c.addNewEAttribute(prefix + "Attr",
						EcorePackage.eINSTANCE.EString)
					c.enrichWithReference(prefix)
				}
			''',
			'''
				import org.eclipse.emf.ecore.EClass
				import test2.MyFile1
				
				package test3
				
				metamodel "simple"
				
				use test2.MyFile1 as extension my
				
				modifyEcore aModificationTest epackage simple {
					ecoreref(SimpleClass)
						.enrichWithAttribute("prefix")
					// attribute and reference are added by the calls
					// to external operations!
					ecoreref(prefixAttr).changeable = true
					ecoreref(prefixRef).containment = true
				}
			'''],
			"test3.MyFile2",
			#[
				SIMPLE_ECORE ->
				'''
				<?xml version="1.0" encoding="UTF-8"?>
				<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="simple" nsURI="http://www.simple" nsPrefix="simple">
				  <eClassifiers xsi:type="ecore:EClass" name="SimpleClass">
				    <eStructuralFeatures xsi:type="ecore:EAttribute" name="prefixAttr" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
				    <eStructuralFeatures xsi:type="ecore:EReference" name="prefixRef" eType="ecore:EClass http://www.eclipse.org/emf/2002/Ecore#//EObject"
				        containment="true"/>
				  </eClassifiers>
				</ecore:EPackage>
				'''
			],
			true
		)
	}

	@Test
	def void testExecutionOfSeveralFilesWithUseAsAndIssuePresenter() {
		// see https://github.com/LorenzoBettini/edelta/issues/348
		val collectedWarnings = newArrayList
		checkCompiledCodeExecutionWithSeveralFiles(
			#[SIMPLE_ECORE],
			#[
			'''
				import org.eclipse.emf.ecore.EClass
				import org.eclipse.emf.ecore.EcorePackage

				package test1
				
				def enrichWithReference(EClass c, String prefix) : void {
					showWarning(
						c,
						"Adding reference with " + prefix)
					c.addNewEReference(prefix + "Ref",
						EcorePackage.eINSTANCE.EObject)
				}
			''',
			'''
				import org.eclipse.emf.ecore.EClass
				import org.eclipse.emf.ecore.EcorePackage
				import test1.MyFile0

				package test2
				
				use test1.MyFile0 as extension my

				def enrichWithAttribute(EClass c, String prefix) : void {
					showWarning(
						c,
						"Adding attribute with " + prefix)
					c.addNewEAttribute(prefix + "Attr",
						EcorePackage.eINSTANCE.EString)
					c.enrichWithReference(prefix)
				}
			''',
			'''
				import org.eclipse.emf.ecore.EClass
				import test2.MyFile1
				
				package test3
				
				metamodel "simple"
				
				use test2.MyFile1 as extension my
				
				modifyEcore aModificationTest epackage simple {
					val simpleClass = ecoreref(SimpleClass)
					showWarning(
						simpleClass,
						"Modifying " + simpleClass.name)
					ecoreref(SimpleClass)
						.enrichWithAttribute("prefix")
					// attribute and reference are added by the calls
					// to external operations!
					ecoreref(prefixAttr).changeable = true
					ecoreref(prefixRef).containment = true
				}
			'''],
			"test3.MyFile2",
			#[
				SIMPLE_ECORE ->
				'''
				<?xml version="1.0" encoding="UTF-8"?>
				<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="simple" nsURI="http://www.simple" nsPrefix="simple">
				  <eClassifiers xsi:type="ecore:EClass" name="SimpleClass">
				    <eStructuralFeatures xsi:type="ecore:EAttribute" name="prefixAttr" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
				    <eStructuralFeatures xsi:type="ecore:EReference" name="prefixRef" eType="ecore:EClass http://www.eclipse.org/emf/2002/Ecore#//EObject"
				        containment="true"/>
				  </eClassifiers>
				</ecore:EPackage>
				'''
			],
			true,
			[ edelta |
				edelta.issuePresenter = new EdeltaIssuePresenter() {
					override showError(ENamedElement problematicObject, String message) {
					}

					override showWarning(ENamedElement problematicObject, String message) {
						collectedWarnings += message
					}
				}
			]
		)
		assertThat(collectedWarnings)
			.containsExactly(
				"Modifying SimpleClass",
				"Adding attribute with prefix",
				"Adding reference with prefix"
			)
	}

	@Test
	def void testCompilationOfNonAmbiguousEcorerefAfterRemoval() {
		val rs = createResourceSet(
		'''
			import org.eclipse.emf.ecore.EAttribute
			import org.eclipse.emf.ecore.EReference
			
			metamodel "mainpackage"
			
			modifyEcore aTest epackage mainpackage {
				addNewEClass("ANewClass") [
					addNewEAttribute("created", null)
				]
				addNewEClass("AnotherNewClass") [
					addNewEReference("created", null)
				]
				EClassifiers -= ecoreref(ANewClass)
				// "created" is not ambiguous anymore
				// and it's correctly typed (EReference, not EAttribute)
				val EReference r = ecoreref(created) // OK
			}
		''')
		rs.addEPackageWithSubPackageForTests
		checkCompilation(rs,
			'''
			package edelta;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			import java.util.function.Consumer;
			import org.eclipse.emf.common.util.EList;
			import org.eclipse.emf.ecore.EClass;
			import org.eclipse.emf.ecore.EClassifier;
			import org.eclipse.emf.ecore.EPackage;
			import org.eclipse.emf.ecore.EReference;
			
			@SuppressWarnings("all")
			public class MyFile0 extends EdeltaDefaultRuntime {
			  public MyFile0(final EdeltaRuntime other) {
			    super(other);
			  }
			
			  public void aTest(final EPackage it) {
			    final Consumer<EClass> _function = (EClass it_1) -> {
			      this.stdLib.addNewEAttribute(it_1, "created", null);
			    };
			    this.stdLib.addNewEClass(it, "ANewClass", _function);
			    final Consumer<EClass> _function_1 = (EClass it_1) -> {
			      this.stdLib.addNewEReference(it_1, "created", null);
			    };
			    this.stdLib.addNewEClass(it, "AnotherNewClass", _function_1);
			    EList<EClassifier> _eClassifiers = it.getEClassifiers();
			    _eClassifiers.remove(getEClass("mainpackage", "ANewClass"));
			    final EReference r = getEReference("mainpackage", "AnotherNewClass", "created");
			  }
			
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("mainpackage");
			  }
			
			  @Override
			  protected void doExecute() throws Exception {
			    aTest(getEPackage("mainpackage"));
			  }
			}
			''',
			true
		)
	}

	@Test def void testEcoreRefExpForCreatedEClassInInitializer() {
		'''
			import org.eclipse.emf.ecore.EcoreFactory
			
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("NewClass") [
					// even though the name is set after the initializer
					// is executed we can still refer the newly created EClass
					ecoreref(NewClass).abstract = true
					ecoreref(NewClass).name = "Renamed"
				]
				ecoreref(Renamed)
			}
		'''.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			import java.util.function.Consumer;
			import org.eclipse.emf.ecore.EClass;
			import org.eclipse.emf.ecore.EPackage;
			
			@SuppressWarnings("all")
			public class MyFile0 extends EdeltaDefaultRuntime {
			  public MyFile0(final EdeltaRuntime other) {
			    super(other);
			  }
			
			  public void aTest(final EPackage it) {
			    final Consumer<EClass> _function = (EClass it_1) -> {
			      getEClass("foo", "NewClass").setAbstract(true);
			      getEClass("foo", "NewClass").setName("Renamed");
			    };
			    this.stdLib.addNewEClass(it, "NewClass", _function);
			    getEClass("foo", "Renamed");
			  }
			
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			
			  @Override
			  protected void doExecute() throws Exception {
			    aTest(getEPackage("foo"));
			  }
			}
			'''
		)
	}

	@Test
	def void testCompilationOfPersonListExampleModifyEcore() {
		val rs = createResourceSetWithEcores(
			#[PERSON_LIST_ECORE],
			personListExampleModifyEcore
		)
		rs.
		checkCompilation(
			'''
			package edelta.personlist.example;
			
			import edelta.lib.EdeltaDefaultRuntime;
			import edelta.lib.EdeltaRuntime;
			import edelta.refactorings.lib.EdeltaRefactorings;
			import java.util.Collections;
			import org.eclipse.emf.ecore.EAttribute;
			import org.eclipse.emf.ecore.EClass;
			import org.eclipse.emf.ecore.EPackage;
			import org.eclipse.emf.ecore.EStructuralFeature;
			import org.eclipse.xtext.xbase.lib.CollectionLiterals;
			import org.eclipse.xtext.xbase.lib.Extension;
			import org.eclipse.xtext.xbase.lib.ObjectExtensions;
			import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
			
			@SuppressWarnings("all")
			public class Example extends EdeltaDefaultRuntime {
			  @Extension
			  private EdeltaRefactorings refactorings;
			
			  public Example(final EdeltaRuntime other) {
			    super(other);
			    refactorings = new EdeltaRefactorings(this);
			  }
			
			  public void improvePerson(final EPackage it) {
			    this.refactorings.enumToSubclasses(getEAttribute("PersonList", "Person", "gender"));
			    this.refactorings.<EAttribute>mergeFeatures("name", 
			      Collections.<EAttribute>unmodifiableList(CollectionLiterals.<EAttribute>newArrayList(getEAttribute("PersonList", "Person", "firstname"), getEAttribute("PersonList", "Person", "lastname"))));
			  }
			
			  public void introducePlace(final EPackage it) {
			    this.refactorings.extractSuperclass("Place", 
			      Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(getEAttribute("PersonList", "LivingPlace", "address"), getEAttribute("PersonList", "WorkPlace", "address"))));
			  }
			
			  public void introduceWorkingPosition(final EPackage it) {
			    EClass _referenceToClass = this.refactorings.referenceToClass("WorkingPosition", getEReference("PersonList", "Person", "works"));
			    final Procedure1<EClass> _function = (EClass it_1) -> {
			      this.stdLib.addNewEAttribute(it_1, "description", getEDataType("ecore", "EString"));
			    };
			    ObjectExtensions.<EClass>operator_doubleArrow(_referenceToClass, _function);
			    getEReference("PersonList", "WorkPlace", "persons").setName("position");
			  }
			
			  public void improveList(final EPackage it) {
			    this.refactorings.mergeFeatures("places", 
			      getEClass("PersonList", "Place"), 
			      Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(getEReference("PersonList", "List", "wplaces"), getEReference("PersonList", "List", "lplaces"))));
			  }
			
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("PersonList");
			    ensureEPackageIsLoaded("ecore");
			  }
			
			  @Override
			  protected void doExecute() throws Exception {
			    improvePerson(getEPackage("PersonList"));
			    introducePlace(getEPackage("PersonList"));
			    introduceWorkingPosition(getEPackage("PersonList"));
			    improveList(getEPackage("PersonList"));
			  }
			}
			''',
			true
		)
	}

	def private checkCompiledCodeExecution(CharSequence input, CharSequence expectedGeneratedEcore,
			boolean checkValidationErrors) {
		wipeModifiedDirectoryContents
		val rs = createResourceSet(input)
		rs.compile [
			if (checkValidationErrors) {
				assertNoValidationErrors
			}
			if (checkValidationErrors) {
				assertGeneratedJavaCodeCompiles
			}
			val genClass = compiledClass
			val modelManager = new EdeltaModelManager
			val defaultEdelta = new EdeltaDefaultRuntime(modelManager)
			val edeltaObj = genClass
				.getDeclaredConstructor(EdeltaRuntime)
				.newInstance(defaultEdelta) as EdeltaRuntime
			// load ecore files
			modelManager.loadEcoreFile("testecores/foo.ecore")
			edeltaObj.execute
			modelManager.saveEcores(MODIFIED)
			assertFileContents(MODIFIED+"/foo.ecore", expectedGeneratedEcore.toString)
		]
	}

	def private checkCompiledCodeExecution(List<String> ecoreNames,
			CharSequence input,
			List<Pair<CharSequence, CharSequence>> expectedModifiedEcores,
			boolean checkValidationErrors) {
		wipeModifiedDirectoryContents
		val rs = createResourceSetWithEcores(ecoreNames, input)
		rs.compile [
			if (checkValidationErrors) {
				assertNoValidationErrors
			}
			if (checkValidationErrors) {
				assertGeneratedJavaCodeCompiles
			}
			val genClass = compiledClass
			checkExecutionAndAssertExpectedModifiedEcores(genClass,
				ecoreNames, expectedModifiedEcores, []
			)
		]
	}

	def private checkCompiledCodeExecutionWithSeveralFiles(List<String> ecoreNames,
			List<CharSequence> inputs,
			String classToExecute,
			List<Pair<CharSequence, CharSequence>> expectedModifiedEcores,
			boolean checkValidationErrors) {
		checkCompiledCodeExecutionWithSeveralFiles(ecoreNames,
			inputs, classToExecute, expectedModifiedEcores, checkValidationErrors,
			[]
		)
	}

	def private checkCompiledCodeExecutionWithSeveralFiles(List<String> ecoreNames,
			List<CharSequence> inputs,
			String classToExecute,
			List<Pair<CharSequence, CharSequence>> expectedModifiedEcores,
			boolean checkValidationErrors,
			Consumer<EdeltaRuntime> instanceConsumer) {
		wipeModifiedDirectoryContents
		val rs = createResourceSetWithEcoresAndSeveralInputs(ecoreNames, inputs)
		rs.compile [
			if (checkValidationErrors) {
				assertNoValidationErrors
			}
			if (checkValidationErrors) {
				assertGeneratedJavaCodeCompiles
			}
			val genClass = getCompiledClass(classToExecute)
			checkExecutionAndAssertExpectedModifiedEcores(genClass,
				ecoreNames, expectedModifiedEcores, instanceConsumer
			)
		]
	}

	private def void checkExecutionAndAssertExpectedModifiedEcores(Class<?> genClass,
		List<String> ecoreNames, List<Pair<CharSequence, CharSequence>> expectedModifiedEcores,
		Consumer<EdeltaRuntime> instanceConsumer
	) {
		val modelManager = new EdeltaModelManager
		val mainEdelta = new EdeltaDefaultRuntime(modelManager)
		val edeltaObj = genClass
			.getDeclaredConstructor(EdeltaRuntime)
			.newInstance(mainEdelta) as EdeltaRuntime
		instanceConsumer.accept(edeltaObj)
		// load ecore files
		for (ecoreName : ecoreNames) {
			modelManager.loadEcoreFile(METAMODEL_PATH + ecoreName)
		}
		edeltaObj.execute
		modelManager.saveEcores(MODIFIED)
		for (expected : expectedModifiedEcores) {
			assertFileContents(
				MODIFIED+"/"+expected.key,
				expected.value.toString
			)
		}
	}

	def private void wipeModifiedDirectoryContents() {
		cleanDirectory(MODIFIED);
	}

}
