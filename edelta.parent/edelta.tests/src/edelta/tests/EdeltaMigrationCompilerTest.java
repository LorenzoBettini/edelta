package edelta.tests;

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
		}
		"""
		);
	}

	@Override
	protected ResourceSet createResourceSet(CharSequence... inputs) throws Exception {
		var rs = super.createResourceSet(inputs);
		addEPackageForTests2(rs);
		return rs;
	}

}
