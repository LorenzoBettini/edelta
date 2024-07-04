package edelta.tests;

import java.util.List;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.util.JavaVersion;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import edelta.tests.injectors.EdeltaInjectorProviderTestableDerivedStateComputer;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderTestableDerivedStateComputer.class)
public class EdeltaMigrationCompilerTest extends EdeltaAbstractCompilerTest {

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
			nsURI "http://bar" to "http://bar/v2"
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
		import java.util.List;
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
		    ensureEPackageIsLoadedByNsURI("bar", "http://bar");
		  }
		
		  @Override
		  protected void doExecute() throws Exception {
		    aTest(getEPackage("foo"));
		    getEPackage("foo").setNsURI("http://foo/v2");
		    getEPackage("bar").setNsURI("http://bar/v2");
		  }
		
		  public List<String> getMigratedNsURIs() {
		    return List.of(
		      "http://foo",
		      "http://bar"
		    );
		  }
		
		  public List<String> getEcorePaths() {
		    return List.of(
		      "/foo.ecore",
		      "/bar.ecore"
		    );
		  }
		}
		"""
		);
	}

	@Test
	public void testCompilationOfMigrationWithRealEcoreFiles() throws Exception {
		var rs = createResourceSetWithEcores(List.of(SIMPLE_ECORE, ANOTHER_SIMPLE_ECORE),
		"""
		package foo;

		migrations {
			nsURI "http://www.simple" to "http://www.simple/v2"
			nsURI "http://www.anothersimple" to "http://www.anothersimple/v2"
		}

		modifyEcore aTest epackage simple {
			ecoreref(SimpleClass).name = "RenamedSimpleClass"
		}
		""");
		checkCompilation(rs, """
		package foo;
		
		import edelta.lib.EdeltaDefaultRuntime;
		import edelta.lib.EdeltaRuntime;
		import java.util.List;
		import org.eclipse.emf.ecore.EPackage;
		
		@SuppressWarnings("all")
		public class Example extends EdeltaDefaultRuntime {
		  public Example(final EdeltaRuntime other) {
		    super(other);
		  }
		
		  public void aTest(final EPackage it) {
		    getEClass("simple", "SimpleClass").setName("RenamedSimpleClass");
		  }
		
		  @Override
		  public void performSanityChecks() throws Exception {
		    ensureEPackageIsLoadedByNsURI("simple", "http://www.simple");
		    ensureEPackageIsLoadedByNsURI("anothersimple", "http://www.anothersimple");
		  }
		
		  @Override
		  protected void doExecute() throws Exception {
		    aTest(getEPackage("simple"));
		    getEPackage("simple").setNsURI("http://www.simple/v2");
		    getEPackage("anothersimple").setNsURI("http://www.anothersimple/v2");
		  }
		
		  public List<String> getMigratedNsURIs() {
		    return List.of(
		      "http://www.simple",
		      "http://www.anothersimple"
		    );
		  }
		
		  public List<String> getEcorePaths() {
		    return List.of(
		      "/Simple.ecore",
		      "/AnotherSimple.ecore"
		    );
		  }
		}
		""",
		true);
	}

	@Override
	protected ResourceSet createResourceSet(CharSequence... inputs) throws Exception {
		var rs = super.createResourceSet(inputs);
		addEPackageForTests2(rs);
		return rs;
	}

}
