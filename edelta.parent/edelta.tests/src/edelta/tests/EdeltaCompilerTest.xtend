package edelta.tests

import com.google.common.base.Joiner
import com.google.inject.Inject
import edelta.testutils.EdeltaTestUtils
import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.xtext.diagnostics.Severity
import org.eclipse.xtext.resource.FileExtensionProvider
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.util.JavaVersion
import org.eclipse.xtext.xbase.lib.util.ReflectExtensions
import org.eclipse.xtext.xbase.testing.CompilationTestHelper
import org.eclipse.xtext.xbase.testing.CompilationTestHelper.Result
import org.eclipse.xtext.xbase.testing.TemporaryFolder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import static edelta.testutils.EdeltaTestUtils.*

import static extension org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderTestableDerivedStateComputer)
class EdeltaCompilerTest extends EdeltaAbstractTest {

	@Rule @Inject public TemporaryFolder temporaryFolder
	@Inject extension CompilationTestHelper compilationTestHelper
	@Inject FileExtensionProvider extensionProvider
	@Inject extension ReflectExtensions

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
			
			import edelta.lib.AbstractEdelta;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  public MyFile0() {
			    
			  }
			  
			  public MyFile0(final AbstractEdelta other) {
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
			
			import edelta.lib.AbstractEdelta;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  public MyFile0() {
			    
			  }
			  
			  public MyFile0(final AbstractEdelta other) {
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
			
			import edelta.lib.AbstractEdelta;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  public MyFile0() {
			    
			  }
			  
			  public MyFile0(final AbstractEdelta other) {
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
			
			import edelta.lib.AbstractEdelta;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  public MyFile0() {
			    
			  }
			  
			  public MyFile0(final AbstractEdelta other) {
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
			
			import edelta.lib.AbstractEdelta;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  public MyFile0() {
			    
			  }
			  
			  public MyFile0(final AbstractEdelta other) {
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
			
			import edelta.lib.AbstractEdelta;
			import org.eclipse.emf.ecore.EClass;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  public MyFile0() {
			    
			  }
			  
			  public MyFile0(final AbstractEdelta other) {
			    super(other);
			  }
			  
			  public EClass bar(final String s) {
			    return this.lib.newEClass(s);
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
			
			import edelta.lib.AbstractEdelta;
			import java.util.function.Consumer;
			import org.eclipse.emf.common.util.EList;
			import org.eclipse.emf.ecore.EClass;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  public MyFile0() {
			    
			  }
			  
			  public MyFile0(final AbstractEdelta other) {
			    super(other);
			  }
			  
			  public EClass bar(final String s) {
			    final Consumer<EClass> _function = (EClass it) -> {
			      EList<EClass> _eSuperTypes = it.getESuperTypes();
			      EClass _newEClass = this.lib.newEClass("Base");
			      _eSuperTypes.add(_newEClass);
			    };
			    return this.lib.newEClass(s, _function);
			  }
			}
			'''
		)
	}

	@Test
	def void testProgramWithMainExpression() {
		programWithMainExpression.checkCompilation(
			'''
			package foo;
			
			import edelta.lib.AbstractEdelta;
			import org.eclipse.emf.ecore.EClass;
			import org.eclipse.xtext.xbase.lib.InputOutput;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  public MyFile0() {
			    
			  }
			  
			  public MyFile0(final AbstractEdelta other) {
			    super(other);
			  }
			  
			  public EClass bar(final String s) {
			    return this.lib.newEClass(s);
			  }
			  
			  @Override
			  protected void doExecute() throws Exception {
			    InputOutput.<EClass>println(this.bar("foo"));
			  }
			}
			'''
		)
	}

	@Test
	def void testCompilationOfEcoreReferenceExpression() {
		ecoreReferenceExpressions.checkCompilation(
			'''
			package foo;
			
			import edelta.lib.AbstractEdelta;
			import org.eclipse.emf.ecore.EAttribute;
			import org.eclipse.emf.ecore.EClass;
			import org.eclipse.emf.ecore.EEnum;
			import org.eclipse.emf.ecore.EEnumLiteral;
			import org.eclipse.emf.ecore.EPackage;
			import org.eclipse.emf.ecore.EReference;
			import org.eclipse.xtext.xbase.lib.InputOutput;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  public MyFile0() {
			    
			  }
			  
			  public MyFile0(final AbstractEdelta other) {
			    super(other);
			  }
			  
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			  
			  @Override
			  protected void doExecute() throws Exception {
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
			}
			'''
		)
	}

	@Test
	def void testCompilationOfEclassExpressionWithNonExistantEClass() {
		"println(ecoreref(Foo))".checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.AbstractEdelta;
			import org.eclipse.emf.ecore.ENamedElement;
			import org.eclipse.xtext.xbase.lib.InputOutput;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  public MyFile0() {
			    
			  }
			  
			  public MyFile0(final AbstractEdelta other) {
			    super(other);
			  }
			  
			  @Override
			  protected void doExecute() throws Exception {
			    InputOutput.<ENamedElement>println(getENamedElement("", "", ""));
			  }
			}
			''',
			false
		)
	}

	@Test
	def void testCreateEClass() {
		createEClass.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.AbstractEdelta;
			import org.eclipse.emf.common.util.EList;
			import org.eclipse.emf.ecore.EClass;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  public MyFile0() {
			    
			  }
			  
			  public MyFile0(final AbstractEdelta other) {
			    super(other);
			  }
			  
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			  
			  @Override
			  protected void doExecute() throws Exception {
			    createEClass("foo", "MyNewClass", createList(this::_createEClass_MyNewClass_in_foo));
			    createEClass("foo", "MyDerivedNewClass", createList(this::_createEClass_MyDerivedNewClass_in_foo));
			  }
			  
			  public void _createEClass_MyNewClass_in_foo(final EClass it) {
			  }
			  
			  public void _createEClass_MyDerivedNewClass_in_foo(final EClass it) {
			    EList<EClass> _eSuperTypes = it.getESuperTypes();
			    _eSuperTypes.add(getEClass("foo", "MyNewClass"));
			  }
			}
			'''
		)
	}

	@Test
	def void testCreateEClassWithSuperTypes() {
		createEClassWithSuperTypes.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.AbstractEdelta;
			import org.eclipse.emf.ecore.EClass;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  public MyFile0() {
			    
			  }
			  
			  public MyFile0(final AbstractEdelta other) {
			    super(other);
			  }
			  
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			  
			  @Override
			  protected void doExecute() throws Exception {
			    createEClass("foo", "MyNewClass", 
			      createList(
			        c -> {
			          c.getESuperTypes().add(getEClass("foo", "FooClass"));
			        },
			        this::_createEClass_MyNewClass_in_foo
			      )
			    );
			  }
			  
			  public void _createEClass_MyNewClass_in_foo(final EClass it) {
			  }
			}
			'''
		)
	}

	@Test
	def void testCreateEClassWithSuperTypes2() {
		createEClassWithSuperTypes2.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.AbstractEdelta;
			import org.eclipse.emf.ecore.EClass;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  public MyFile0() {
			    
			  }
			  
			  public MyFile0(final AbstractEdelta other) {
			    super(other);
			  }
			  
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			  
			  @Override
			  protected void doExecute() throws Exception {
			    createEClass("foo", "BaseClass", createList(this::_createEClass_BaseClass_in_foo));
			    createEClass("foo", "MyNewClass", 
			      createList(
			        c -> {
			          c.getESuperTypes().add(getEClass("foo", "FooClass"));
			          c.getESuperTypes().add(getEClass("foo", "BaseClass"));
			        },
			        this::_createEClass_MyNewClass_in_foo
			      )
			    );
			  }
			  
			  public void _createEClass_BaseClass_in_foo(final EClass it) {
			  }
			  
			  public void _createEClass_MyNewClass_in_foo(final EClass it) {
			  }
			}
			'''
		)
	}

	@Test
	def void testReferenceToCreatedEClass() {
		referenceToCreatedEClass.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.AbstractEdelta;
			import org.eclipse.emf.ecore.EClass;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  public MyFile0() {
			    
			  }
			  
			  public MyFile0(final AbstractEdelta other) {
			    super(other);
			  }
			  
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			  
			  @Override
			  protected void doExecute() throws Exception {
			    createEClass("foo", "NewClass", createList(this::_createEClass_NewClass_in_foo));
			    getEClass("foo", "NewClass");
			  }
			  
			  public void _createEClass_NewClass_in_foo(final EClass it) {
			  }
			}
			'''
		)
	}

	@Test
	def void testReferenceToCreatedEClassRenamed() {
		// the name of the created EClass is changed
		// in the initialization block and the interpreter is executed
		// thus, we can access them both.
		// TODO: we should issue an error on the original reference which
		// is not valid anymore
		referenceToCreatedEClassRenamed.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.AbstractEdelta;
			import org.eclipse.emf.ecore.EClass;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  public MyFile0() {
			    
			  }
			  
			  public MyFile0(final AbstractEdelta other) {
			    super(other);
			  }
			  
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			  
			  @Override
			  protected void doExecute() throws Exception {
			    createEClass("foo", "NewClass", createList(this::_createEClass_NewClass_in_foo));
			    getEClass("foo", "NewClass");
			    getEClass("foo", "changed");
			  }
			  
			  public void _createEClass_NewClass_in_foo(final EClass it) {
			    it.setName("changed");
			  }
			}
			'''
		)
	}

	@Test
	def void testReferenceToChangeEClassRenamed() {
		// the name of the created EClass is changed
		// in the initialization block and the interpreter is executed
		// thus, we can access them both.
		referenceToChangedEClassRenamed.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.AbstractEdelta;
			import org.eclipse.emf.ecore.EClass;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  public MyFile0() {
			    
			  }
			  
			  public MyFile0(final AbstractEdelta other) {
			    super(other);
			  }
			  
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			  
			  @Override
			  protected void doExecute() throws Exception {
			    changeEClass("foo", "FooClass", createList(this::_changeEClass_FooClass_in_foo));
			    getEClass("foo", "FooClass");
			    getEClass("foo", "changed");
			  }
			  
			  public void _changeEClass_FooClass_in_foo(final EClass it) {
			    it.setName("changed");
			  }
			}
			'''
		)
	}

	@Test
	def void testReferenceToCreatedEAttributeRenamed() {
		// the name of the created EAttribute is changed
		// in the initialization block and the interpreter is executed
		// thus, we can access them both.
		// TODO: we should issue an error on the original reference which
		// is not valid anymore
		referenceToCreatedEAttributeRenamed.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.AbstractEdelta;
			import org.eclipse.emf.ecore.EAttribute;
			import org.eclipse.emf.ecore.EClass;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  public MyFile0() {
			    
			  }
			  
			  public MyFile0(final AbstractEdelta other) {
			    super(other);
			  }
			  
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			  
			  @Override
			  protected void doExecute() throws Exception {
			    createEClass("foo", "NewClass", createList(this::_createEClass_NewClass_in_foo));
			    getEAttribute("foo", "NewClass", "newAttribute");
			    getEAttribute("foo", "NewClass", "changed");
			  }
			  
			  public void _createEClass_NewClass_in_foo(final EClass it) {
			    {
			      createEAttribute(it, "newAttribute", 
			        createList(
			          a -> a.setEType(getEDataType("foo", "FooDataType")),
			          this::_createEAttribute_newAttribute_in_createEClass_NewClass_in_foo
			        )
			      );
			      createEAttribute(it, "newAttribute2", 
			        createList(
			          a -> a.setEType(getEDataType("foo", "FooDataType")),
			          this::_createEAttribute_newAttribute2_in_createEClass_NewClass_in_foo
			        )
			      );
			    }
			  }
			  
			  public void _createEAttribute_newAttribute_in_createEClass_NewClass_in_foo(final EAttribute it) {
			    it.setName("changed");
			  }
			  
			  public void _createEAttribute_newAttribute2_in_createEClass_NewClass_in_foo(final EAttribute it) {
			  }
			}
			'''
		)
	}

	@Test
	def void testReferenceToCreatedEAttributeRenamedInChangedEClass() {
		// the name of the created EAttribute is changed
		// in the initialization block and the interpreter is executed
		// thus, we can access them both.
		// TODO: we should issue an error on the original reference which
		// is not valid anymore
		referenceToCreatedEAttributeRenamedInChangedEClass.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.AbstractEdelta;
			import org.eclipse.emf.ecore.EAttribute;
			import org.eclipse.emf.ecore.EClass;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  public MyFile0() {
			    
			  }
			  
			  public MyFile0(final AbstractEdelta other) {
			    super(other);
			  }
			  
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			  
			  @Override
			  protected void doExecute() throws Exception {
			    changeEClass("foo", "FooClass", createList(this::_changeEClass_FooClass_in_foo));
			    getEAttribute("foo", "FooClass", "newAttribute");
			    getEAttribute("foo", "FooClass", "changed");
			  }
			  
			  public void _changeEClass_FooClass_in_foo(final EClass it) {
			    createEAttribute(it, "newAttribute", 
			      createList(
			        a -> a.setEType(getEDataType("foo", "FooDataType")),
			        this::_createEAttribute_newAttribute_in_changeEClass_FooClass_in_foo
			      )
			    );
			  }
			  
			  public void _createEAttribute_newAttribute_in_changeEClass_FooClass_in_foo(final EAttribute it) {
			    it.setName("changed");
			  }
			}
			'''
		)
	}

	@Test
	def void testExecutionCreateEClassWithSuperTypes2() {
		createEClassWithSuperTypes2.checkCompiledCodeExecution(
			'''
			<?xml version="1.0" encoding="UTF-8"?>
			<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="foo" nsURI="http://foo" nsPrefix="foo">
			  <eClassifiers xsi:type="ecore:EClass" name="FooClass"/>
			  <eClassifiers xsi:type="ecore:EClass" name="FooDerivedClass" eSuperTypes="#//FooClass"/>
			  <eClassifiers xsi:type="ecore:EDataType" name="FooDataType" instanceClassName="java.lang.String"/>
			  <eClassifiers xsi:type="ecore:EClass" name="BaseClass"/>
			  <eClassifiers xsi:type="ecore:EClass" name="MyNewClass" eSuperTypes="#//FooClass #//BaseClass"/>
			</ecore:EPackage>
			''',
			true
		)
	}

	@Test
	def void testUseAs() {
		useAs.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.AbstractEdelta;
			import edelta.tests.additional.MyCustomEdelta;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  private MyCustomEdelta my;
			  
			  public MyFile0() {
			    my = new MyCustomEdelta(this);
			  }
			  
			  public MyFile0(final AbstractEdelta other) {
			    super(other);
			  }
			  
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			  
			  @Override
			  protected void doExecute() throws Exception {
			    this.my.myMethod();
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
			
			my.myMethod()
		'''.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.AbstractEdelta;
			import edelta.tests.additional.MyCustomEdelta;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  private Object my;
			  
			  public MyFile0() {
			     = new MyCustomEdelta(this);
			    my = new (this);
			  }
			  
			  public MyFile0(final AbstractEdelta other) {
			    super(other);
			  }
			  
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			  
			  @Override
			  protected void doExecute() throws Exception {
			    this.my./* name is null */;
			  }
			}
			''',
			false
		)
	}

	@Test
	def void testUseAsExecution() {
		// the new created EClass is created by calling a method
		// of a custom Edelta implementation that is used in the program
		useAs2.checkCompiledCodeExecution(
			'''
			<?xml version="1.0" encoding="UTF-8"?>
			<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="foo" nsURI="http://foo" nsPrefix="foo">
			  <eClassifiers xsi:type="ecore:EClass" name="FooClass"/>
			  <eClassifiers xsi:type="ecore:EClass" name="FooDerivedClass" eSuperTypes="#//FooClass"/>
			  <eClassifiers xsi:type="ecore:EDataType" name="FooDataType" instanceClassName="java.lang.String"/>
			  <eClassifiers xsi:type="ecore:EClass" name="ANewClass"/>
			</ecore:EPackage>
			''',
			true
		)
	}

	@Test
	def void testReferenceToChangedEClassRenamed() {
		referenceToChangedEClassWithANewName.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.AbstractEdelta;
			import org.eclipse.emf.ecore.EAttribute;
			import org.eclipse.emf.ecore.EClass;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  public MyFile0() {
			    
			  }
			  
			  public MyFile0(final AbstractEdelta other) {
			    super(other);
			  }
			  
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			  
			  @Override
			  protected void doExecute() throws Exception {
			    changeEClass("foo", "FooClass", 
			      createList(
			        c -> c.setName("RenamedClass"),
			        this::_changeEClass_FooClass_in_foo
			      )
			    );
			    getEClass("foo", "RenamedClass");
			  }
			  
			  public void _changeEClass_FooClass_in_foo(final EClass it) {
			    createEAttribute(it, "anotherAttr", 
			      createList(
			        a -> a.setEType(getEDataType("foo", "FooDataType")),
			        this::_createEAttribute_anotherAttr_in_changeEClass_FooClass_in_foo
			      )
			    );
			  }
			  
			  public void _createEAttribute_anotherAttr_in_changeEClass_FooClass_in_foo(final EAttribute it) {
			  }
			}
			'''
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
			
			import edelta.lib.AbstractEdelta;
			import java.util.function.Consumer;
			import org.eclipse.emf.common.util.EList;
			import org.eclipse.emf.ecore.EClass;
			import org.eclipse.emf.ecore.EClassifier;
			import org.eclipse.emf.ecore.EPackage;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  public MyFile0() {
			    
			  }
			  
			  public MyFile0(final AbstractEdelta other) {
			    super(other);
			  }
			  
			  public void aModificationTest(final EPackage it) {
			    EList<EClassifier> _eClassifiers = it.getEClassifiers();
			    final Consumer<EClass> _function = (EClass it_1) -> {
			      EList<EClass> _eSuperTypes = it_1.getESuperTypes();
			      EClass _newEClass = this.lib.newEClass("Base");
			      _eSuperTypes.add(_newEClass);
			    };
			    EClass _newEClass = this.lib.newEClass("ANewClass", _function);
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
	def void testExecutionChangeEClassWithNewName() {
		referenceToChangedEClassWithANewName.checkCompiledCodeExecution(
			'''
			<?xml version="1.0" encoding="UTF-8"?>
			<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="foo" nsURI="http://foo" nsPrefix="foo">
			  <eClassifiers xsi:type="ecore:EClass" name="RenamedClass">
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
	def void testCompilationAfterInterpretationCreateEClassStealingAttribute() {
		createEClassStealingAttribute.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.AbstractEdelta;
			import org.eclipse.emf.common.util.EList;
			import org.eclipse.emf.ecore.EAttribute;
			import org.eclipse.emf.ecore.EClass;
			import org.eclipse.emf.ecore.EStructuralFeature;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  public MyFile0() {
			    
			  }
			  
			  public MyFile0(final AbstractEdelta other) {
			    super(other);
			  }
			  
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			  
			  @Override
			  protected void doExecute() throws Exception {
			    createEClass("foo", "NewClass", createList(this::_createEClass_NewClass_in_foo));
			  }
			  
			  public void _createEClass_NewClass_in_foo(final EClass it) {
			    {
			      final EAttribute attr = getEAttribute("foo", "FooClass", "myAttribute");
			      EList<EStructuralFeature> _eStructuralFeatures = it.getEStructuralFeatures();
			      _eStructuralFeatures.add(attr);
			    }
			  }
			}
			'''
			// Note:
			// it must be getEAttribute("foo", "FooClass", "myAttribute")
			// since we use the originalENamedElement
			// (the interpeter changed the container of myAttribute to NewClass)
		)
	}

	@Test
	def void testCompilationAfterInterpretationChangeEClassRemovingAttribute() {
		changeEClassRemovingAttribute.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.AbstractEdelta;
			import org.eclipse.emf.common.util.EList;
			import org.eclipse.emf.ecore.EAttribute;
			import org.eclipse.emf.ecore.EClass;
			import org.eclipse.emf.ecore.EStructuralFeature;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  public MyFile0() {
			    
			  }
			  
			  public MyFile0(final AbstractEdelta other) {
			    super(other);
			  }
			  
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			  
			  @Override
			  protected void doExecute() throws Exception {
			    changeEClass("foo", "FooClass", createList(this::_changeEClass_FooClass_in_foo));
			  }
			  
			  public void _changeEClass_FooClass_in_foo(final EClass it) {
			    {
			      final EAttribute attr = getEAttribute("foo", "FooClass", "myAttribute");
			      EList<EStructuralFeature> _eStructuralFeatures = it.getEStructuralFeatures();
			      _eStructuralFeatures.remove(attr);
			    }
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
	def void testCompilationAfterInterpretationCreateEClassAddingAttributeUsingLibMethod() {
		createEClassAndAddEAttributeUsingLibMethodAndReference.
		checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.AbstractEdelta;
			import java.util.function.Consumer;
			import org.eclipse.emf.common.util.EList;
			import org.eclipse.emf.ecore.EAttribute;
			import org.eclipse.emf.ecore.EClass;
			import org.eclipse.emf.ecore.EStructuralFeature;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  public MyFile0() {
			    
			  }
			  
			  public MyFile0(final AbstractEdelta other) {
			    super(other);
			  }
			  
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			  
			  @Override
			  protected void doExecute() throws Exception {
			    createEClass("foo", "NewClass", createList(this::_createEClass_NewClass_in_foo));
			    getEAttribute("foo", "NewClass", "newTestAttr");
			  }
			  
			  public void _createEClass_NewClass_in_foo(final EClass it) {
			    EList<EStructuralFeature> _eStructuralFeatures = it.getEStructuralFeatures();
			    final Consumer<EAttribute> _function = (EAttribute it_1) -> {
			      it_1.setEType(getEDataType("foo", "FooDataType"));
			    };
			    EAttribute _newEAttribute = this.lib.newEAttribute("newTestAttr", _function);
			    _eStructuralFeatures.add(_newEAttribute);
			  }
			}
			'''
		)
	}

	@Test
	def void testCompilationOfPersonListExample() {
		val rs = createResourceSetWithEcore(
			PERSON_LIST_ECORE, PERSON_LIST_ECORE_PATH,
			personListExample
		)
		rs.
		checkCompilation(
			'''
			package gssi.personexample;
			
			import edelta.lib.AbstractEdelta;
			import gssi.refactorings.MMrefactorings;
			import java.util.Collections;
			import org.eclipse.emf.common.util.EList;
			import org.eclipse.emf.ecore.EAttribute;
			import org.eclipse.emf.ecore.EClass;
			import org.eclipse.emf.ecore.EDataType;
			import org.eclipse.emf.ecore.EEnum;
			import org.eclipse.emf.ecore.EReference;
			import org.eclipse.emf.ecore.EStructuralFeature;
			import org.eclipse.xtext.xbase.lib.CollectionLiterals;
			
			@SuppressWarnings("all")
			public class Example extends AbstractEdelta {
			  private MMrefactorings refactorings;
			  
			  public Example() {
			    refactorings = new MMrefactorings(this);
			  }
			  
			  public Example(final AbstractEdelta other) {
			    super(other);
			  }
			  
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("PersonList");
			    ensureEPackageIsLoaded("ecore");
			  }
			  
			  @Override
			  protected void doExecute() throws Exception {
			    changeEClass("PersonList", "Person", createList(this::_changeEClass_Person_in_PersonList));
			    createEClass("PersonList", "Place", createList(this::_createEClass_Place_in_PersonList));
			    createEClass("PersonList", "WorkingPosition", createList(this::_createEClass_WorkingPosition_in_PersonList));
			    changeEClass("PersonList", "List", createList(this::_changeEClass_List_in_PersonList));
			  }
			  
			  public void _changeEClass_Person_in_PersonList(final EClass it) {
			    {
			      EDataType _eAttributeType = getEAttribute("PersonList", "Person", "gender").getEAttributeType();
			      this.refactorings.introduceSubclasses(
			        getEAttribute("PersonList", "Person", "gender"), 
			        ((EEnum) _eAttributeType), it);
			      EList<EStructuralFeature> _eStructuralFeatures = it.getEStructuralFeatures();
			      EAttribute _mergeAttributes = this.refactorings.mergeAttributes("name", 
			        getEAttribute("PersonList", "Person", "firstname").getEType(), 
			        Collections.<EAttribute>unmodifiableList(CollectionLiterals.<EAttribute>newArrayList(getEAttribute("PersonList", "Person", "firstname"), getEAttribute("PersonList", "Person", "lastname"))));
			      _eStructuralFeatures.add(_mergeAttributes);
			    }
			  }
			  
			  public void _createEClass_Place_in_PersonList(final EClass it) {
			    {
			      it.setAbstract(true);
			      this.refactorings.extractSuperclass(it, 
			        Collections.<EAttribute>unmodifiableList(CollectionLiterals.<EAttribute>newArrayList(getEAttribute("PersonList", "LivingPlace", "address"), getEAttribute("PersonList", "WorkPlace", "address"))));
			    }
			  }
			  
			  public void _createEClass_WorkingPosition_in_PersonList(final EClass it) {
			    {
			      createEAttribute(it, "description", 
			        createList(
			          a -> a.setEType(getEDataType("ecore", "EString")),
			          this::_createEAttribute_description_in_createEClass_WorkingPosition_in_PersonList
			        )
			      );
			      this.refactorings.extractMetaClass(it, getEReference("PersonList", "Person", "works"), "position", "works");
			    }
			  }
			  
			  public void _createEAttribute_description_in_createEClass_WorkingPosition_in_PersonList(final EAttribute it) {
			  }
			  
			  public void _changeEClass_List_in_PersonList(final EClass it) {
			    EList<EStructuralFeature> _eStructuralFeatures = it.getEStructuralFeatures();
			    EReference _mergeReferences = this.refactorings.mergeReferences("places", 
			      getEClass("PersonList", "Place"), 
			      Collections.<EReference>unmodifiableList(CollectionLiterals.<EReference>newArrayList(getEReference("PersonList", "List", "wplaces"), getEReference("PersonList", "List", "lplaces"))));
			    _eStructuralFeatures.add(_mergeReferences);
			  }
			}
			''',
			true
		)
	}

	def private checkCompilation(CharSequence input, CharSequence expectedGeneratedJava) {
		checkCompilation(input, expectedGeneratedJava, true)
	}

	def private checkCompilation(CharSequence input, CharSequence expectedGeneratedJava,
		boolean checkValidationErrors) {
		val rs = createResourceSet(input)
		checkCompilation(rs, expectedGeneratedJava, checkValidationErrors)
	}
	
	private def void checkCompilation(ResourceSet rs, CharSequence expectedGeneratedJava, boolean checkValidationErrors) {
		rs.compile [
			if (checkValidationErrors) {
				assertNoValidationErrors
			}
			if (expectedGeneratedJava !== null) {
				assertGeneratedJavaCode(expectedGeneratedJava)
			}
			if (checkValidationErrors) {
				assertGeneratedJavaCodeCompiles
			}
		]
	}

	private def assertNoValidationErrors(Result it) {
		val allErrors = getErrorsAndWarnings.filter[severity == Severity.ERROR]
		if (!allErrors.empty) {
			throw new IllegalStateException(
				"One or more resources contained errors : " + Joiner.on(',').join(allErrors)
			);
		}
	}

	def private assertGeneratedJavaCode(CompilationTestHelper.Result r, CharSequence expected) {
		expected.toString.assertEquals(r.singleGeneratedCode)
	}

	def private assertGeneratedJavaCodeCompiles(CompilationTestHelper.Result r) {
		r.compiledClass // check Java compilation succeeds
	}

	def private createResourceSet(CharSequence... inputs) {
		val pairs = newArrayList() => [
			list |
			inputs.forEach[e, i|
				list += "MyFile" + i + "." + 
					extensionProvider.getPrimaryFileExtension() -> e
			]
		]
		val rs = resourceSet(pairs)
		addEPackageForTests(rs)
		return rs
	}

	def private createResourceSetWithEcore(String ecoreName, String ecorePath, CharSequence input) {
		val pairs = newArrayList(
			"EcoreForTests.ecore" -> EdeltaTestUtils.loadFile(ECORE_PATH),
			ecoreName -> EdeltaTestUtils.loadFile(ecorePath),
			"Example." + 
					extensionProvider.getPrimaryFileExtension() -> input
		)
		val rs = resourceSet(pairs)
		return rs
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
			val edeltaObj = genClass.newInstance
			// load ecore files
			edeltaObj.invoke("loadEcoreFile", #["testecores/foo.ecore"])
			edeltaObj.invoke("execute")
			edeltaObj.invoke("saveModifiedEcores", #[MODIFIED])
			compareSingleFileContents(MODIFIED+"/foo.ecore", expectedGeneratedEcore.toString)
		]
	}

	def private void wipeModifiedDirectoryContents() {
		cleanDirectory(MODIFIED);
	}

}
