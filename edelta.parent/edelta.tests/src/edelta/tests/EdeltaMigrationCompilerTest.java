package edelta.tests;

import static org.eclipse.xtext.xbase.lib.Conversions.unwrapArray;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.filter;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.isEmpty;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.resource.FileExtensionProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.util.JavaVersion;
import org.eclipse.xtext.xbase.lib.Pair;
import org.eclipse.xtext.xbase.testing.CompilationTestHelper;
import org.eclipse.xtext.xbase.testing.TemporaryFolder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

import edelta.tests.injectors.EdeltaInjectorProviderTestableDerivedStateComputer;
import edelta.testutils.EdeltaTestUtils;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderTestableDerivedStateComputer.class)
public class EdeltaMigrationCompilerTest extends EdeltaAbstractTest {
	@Rule
	@Inject
	public TemporaryFolder temporaryFolder;

	@Inject
	private CompilationTestHelper compilationTestHelper;

	@Inject
	private FileExtensionProvider extensionProvider;

	@Before
	public void setup() {
		compilationTestHelper.setJavaVersion(JavaVersion.JAVA17);
	}

	@Test
	public void testCompilationOfMigration() throws Exception {
		checkCompilation("""
		package foo;

		migrations {
			nsURI "http://foo" to "http://foo/v2"
		}

		modifyEcore aTest epackage foo {
			val fooClass = ecoreref(FooClass)
			val mySubClass = fooClass.addNewSubclass("MySubClass")
			fooClass.abstract = true
			// adjust model migration
			modelMigration[
				createInstanceRule(
					isRelatedTo(fooClass),
					[oldObj|
						return createInstance(mySubClass)
					]
				)
			]
		}
		""","""
		package foo;

		import edelta.lib.EdeltaDefaultRuntime;
		import edelta.lib.EdeltaEcoreUtil;
		import edelta.lib.EdeltaModelMigrator;
		import edelta.lib.EdeltaRuntime;
		import java.util.function.Consumer;
		import org.eclipse.emf.ecore.EClass;
		import org.eclipse.emf.ecore.EObject;
		import org.eclipse.emf.ecore.EPackage;

		@SuppressWarnings("all")
		public class MyFile0 extends EdeltaDefaultRuntime {
		  public MyFile0(final EdeltaRuntime other) {
		    super(other);
		  }
		  
		  public void aTest(final EPackage it) {
		    final EClass fooClass = getEClass("foo", "FooClass");
		    final EClass mySubClass = this.stdLib.addNewSubclass(fooClass, "MySubClass");
		    fooClass.setAbstract(true);
		    final Consumer<EdeltaModelMigrator> _function = (EdeltaModelMigrator it_1) -> {
		      final EdeltaModelMigrator.EObjectFunction _function_1 = (EObject oldObj) -> {
		        return EdeltaEcoreUtil.createInstance(mySubClass);
		      };
		      it_1.createInstanceRule(
		        it_1.<EClass>isRelatedTo(fooClass), _function_1);
		    };
		    this.modelMigration(_function);
		  }
		  
		  @Override
		  public void performSanityChecks() throws Exception {
		    ensureEPackageIsLoadedByNsURI("foo", "http://foo");
		  }
		  
		  @Override
		  protected void doExecute() throws Exception {
		    aTest(getEPackage("foo"));
		    getEPackage("foo").setNsURI("http://foo/v2");
		  }
		}
		"""
		);
	}

	private void checkCompilation(CharSequence input, CharSequence expectedGeneratedJava) throws Exception {
		checkCompilation(input, expectedGeneratedJava, true);
	}

	private void checkCompilation(CharSequence input, CharSequence expectedGeneratedJava,
			boolean checkValidationErrors) throws Exception {
		var rs = createResourceSet(input);
		checkCompilation(rs, expectedGeneratedJava, checkValidationErrors);
	}

	private void checkCompilation(ResourceSet rs, CharSequence expectedGeneratedJava,
			boolean checkValidationErrors) {
		compilationTestHelper.compile(rs, it -> {
			if (checkValidationErrors) {
				assertNoValidationErrors(it);
			}
			if (expectedGeneratedJava != null) {
				assertGeneratedJavaCode(it, expectedGeneratedJava);
			}
			if (checkValidationErrors) {
				assertGeneratedJavaCodeCompiles(it);
			}
		});
	}

	private void assertNoValidationErrors(CompilationTestHelper.Result res) {
		var allErrors = filter(res.getErrorsAndWarnings(), it -> it.getSeverity() == Severity.ERROR);
		if (!isEmpty(allErrors)) {
			throw new IllegalStateException(
				"One or more resources contained errors : " +
					Joiner.on(",").join(allErrors));
		}
	}

	private void assertGeneratedJavaCode(CompilationTestHelper.Result r, CharSequence expected) {
		assertEquals(expected.toString(),
			EdeltaTestUtils.removeCR(r.getSingleGeneratedCode()));
	}

	private Class<?> assertGeneratedJavaCodeCompiles(CompilationTestHelper.Result r) {
		return r.getCompiledClass();
	}

	private ResourceSet createResourceSet(CharSequence... inputs) throws Exception {
		var pairs = createInputPairs(inputs);
		@SuppressWarnings("unchecked")
		var rs = compilationTestHelper
			.resourceSet(((Pair<String, ? extends CharSequence>[])
					unwrapArray(pairs, Pair.class)));
		addEPackageForTests(rs);
		return rs;
	}

	private List<Pair<String, CharSequence>> createInputPairs(CharSequence[] inputs) {
		var result = new ArrayList<Pair<String, CharSequence>>();
		for (int i = 0; i < inputs.length; i++) {
			result.add(Pair.of(
				"MyFile" + i + "." + 
					extensionProvider.getPrimaryFileExtension(),
				inputs[i]));
		}
		return result;
	}
}
