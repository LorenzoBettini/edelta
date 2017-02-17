/*
 * generated by Xtext 2.10.0
 */
package edelta.tests

import com.google.common.base.Joiner
import com.google.inject.Inject
import org.eclipse.xtext.diagnostics.Severity
import org.eclipse.xtext.resource.FileExtensionProvider
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.xbase.testing.CompilationTestHelper
import org.eclipse.xtext.xbase.testing.CompilationTestHelper.Result
import org.eclipse.xtext.xbase.testing.TemporaryFolder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import static extension org.junit.Assert.*
import org.junit.Before
import org.eclipse.xtext.util.JavaVersion

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaCompilerTest extends EdeltaAbstractTest {

	@Rule @Inject public TemporaryFolder temporaryFolder
	@Inject extension CompilationTestHelper compilationTestHelper
	@Inject private FileExtensionProvider extensionProvider

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
			import org.eclipse.emf.ecore.EReference;
			import org.eclipse.xtext.xbase.lib.InputOutput;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			  
			  @Override
			  protected void doExecute() throws Exception {
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
	def void testReferenceToCreatedEClass() {
		referenceToCreatedEClass.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.AbstractEdelta;
			import org.eclipse.emf.ecore.EClass;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
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
	def void testReferenceToCreatedEAttribute() {
		referenceToCreatedEAttribute.checkCompilation(
			'''
			package edelta;
			
			import edelta.lib.AbstractEdelta;
			import org.eclipse.emf.ecore.EAttribute;
			import org.eclipse.emf.ecore.EClass;
			
			@SuppressWarnings("all")
			public class MyFile0 extends AbstractEdelta {
			  @Override
			  public void performSanityChecks() throws Exception {
			    ensureEPackageIsLoaded("foo");
			  }
			  
			  @Override
			  protected void doExecute() throws Exception {
			    createEClass("foo", "NewClass", createList(this::_createEClass_NewClass_in_foo));
			    getEAttribute("foo", "NewClass", "newAttribute");
			  }
			  
			  public void _createEClass_NewClass_in_foo(final EClass it) {
			    {
			      createEAttribute(it, "newAttribute", createList(this::_createEAttribute_newAttribute_in_createEClass_NewClass_in_foo));
			      createEAttribute(it, "newAttribute2", createList(this::_createEAttribute_newAttribute2_in_createEClass_NewClass_in_foo));
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

	def private checkCompilation(CharSequence input, CharSequence expectedGeneratedJava) {
		checkCompilation(input, expectedGeneratedJava, true)
	}

	def private checkCompilation(CharSequence input, CharSequence expectedGeneratedJava,
		boolean checkValidationErrors) {
		val rs = createResourceSet(input)
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
}
