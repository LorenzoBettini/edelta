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

		migrate "http://foo" to "http://foo/v2"
		migrate "http://bar" to "http://bar/v2"

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
		import edelta.lib.EdeltaEngine;
		import edelta.lib.EdeltaModelMigrator;
		import edelta.lib.EdeltaRuntime;
		import edelta.lib.annotation.EdeltaGenerated;
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
		
		  @Override
		  public List<String> getMigratedNsURIs() {
		    return List.of(
		      "http://foo",
		      "http://bar"
		    );
		  }
		
		  @Override
		  public List<String> getMigratedEcorePaths() {
		    return List.of(
		      "/foo.ecore",
		      "/bar.ecore"
		    );
		  }
		
		  @EdeltaGenerated
		  public static void main(final String[] args) throws Exception {
		    var engine = new EdeltaEngine(MyFile0::new);
		    engine.loadEcoreFile("foo.ecore",
		      MyFile0.class.getResourceAsStream("/foo.ecore"));
		    engine.loadEcoreFile("bar.ecore",
		      MyFile0.class.getResourceAsStream("/bar.ecore"));
		    engine.execute();
		    engine.save("modified");
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

		migrate "http://www.simple" to "http://www.simple/v2"
		migrate "http://www.anothersimple" to "http://www.anothersimple/v2"

		modifyEcore aTest epackage simple {
			ecoreref(SimpleClass).name = "RenamedSimpleClass"
		}
		""");
		checkCompilation(rs, """
		package foo;
		
		import edelta.lib.EdeltaDefaultRuntime;
		import edelta.lib.EdeltaEngine;
		import edelta.lib.EdeltaRuntime;
		import edelta.lib.annotation.EdeltaGenerated;
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
		
		  @Override
		  public List<String> getMigratedNsURIs() {
		    return List.of(
		      "http://www.simple",
		      "http://www.anothersimple"
		    );
		  }
		
		  @Override
		  public List<String> getMigratedEcorePaths() {
		    return List.of(
		      "/Simple.ecore",
		      "/AnotherSimple.ecore"
		    );
		  }
		
		  @EdeltaGenerated
		  public static void main(final String[] args) throws Exception {
		    var engine = new EdeltaEngine(Example::new);
		    engine.loadEcoreFile("Simple.ecore",
		      Example.class.getResourceAsStream("/Simple.ecore"));
		    engine.loadEcoreFile("AnotherSimple.ecore",
		      Example.class.getResourceAsStream("/AnotherSimple.ecore"));
		    engine.execute();
		    engine.save("modified");
		  }
		}
		""",
		true);
	}

	@Test
	public void testCompilationOfMigrationWithRealEcoreFilesInEcoreVersionsDirectory() throws Exception {
		var rs = createResourceSetWithEcores(List.of(
				ECOREVERSIONS + ECORE_IN_ECORE_VERSIONS_ECORE,
				ECOREVERSIONS_V1 + ECORE_IN_ECORE_VERSIONS_SUBDIR_ECORE),
		"""
		package foo;

		migrate "http://www.simple.in.ecoreversions" to "http://www.simple/v2"
		migrate "http://www.simple.in.ecoreversions.v1" to "http://www.anothersimple/v2"

		modifyEcore aTest epackage simpleinecoreversions {
			ecoreref(simpleinecoreversions.SimpleClass).name = "RenamedSimpleClass"
		}
		modifyEcore aTest2 epackage simpleinecoreversionsv1 {
			ecoreref(simpleinecoreversionsv1.SimpleClass).name = "RenamedSimpleClass"
		}
		""");
		checkCompilation(rs, """
		package foo;
		
		import edelta.lib.EdeltaDefaultRuntime;
		import edelta.lib.EdeltaEngine;
		import edelta.lib.EdeltaRuntime;
		import edelta.lib.annotation.EdeltaGenerated;
		import java.util.List;
		import org.eclipse.emf.ecore.EPackage;
		
		@SuppressWarnings("all")
		public class Example extends EdeltaDefaultRuntime {
		  public Example(final EdeltaRuntime other) {
		    super(other);
		  }
		
		  public void aTest(final EPackage it) {
		    getEClass("simpleinecoreversions", "SimpleClass").setName("RenamedSimpleClass");
		  }
		
		  public void aTest2(final EPackage it) {
		    getEClass("simpleinecoreversionsv1", "SimpleClass").setName("RenamedSimpleClass");
		  }
		
		  @Override
		  public void performSanityChecks() throws Exception {
		    ensureEPackageIsLoadedByNsURI("simpleinecoreversions", "http://www.simple.in.ecoreversions");
		    ensureEPackageIsLoadedByNsURI("simpleinecoreversionsv1", "http://www.simple.in.ecoreversions.v1");
		  }
		
		  @Override
		  protected void doExecute() throws Exception {
		    aTest(getEPackage("simpleinecoreversions"));
		    aTest2(getEPackage("simpleinecoreversionsv1"));
		    getEPackage("simpleinecoreversions").setNsURI("http://www.simple/v2");
		    getEPackage("simpleinecoreversionsv1").setNsURI("http://www.anothersimple/v2");
		  }
		
		  @Override
		  public List<String> getMigratedNsURIs() {
		    return List.of(
		      "http://www.simple.in.ecoreversions",
		      "http://www.simple.in.ecoreversions.v1"
		    );
		  }
		
		  @Override
		  public List<String> getMigratedEcorePaths() {
		    return List.of(
		      "/EcoreInEcoreVersions.ecore",
		      "/EcoreInEcoreVersionsSubdir.ecore"
		    );
		  }
		
		  @EdeltaGenerated
		  public static void main(final String[] args) throws Exception {
		    var engine = new EdeltaEngine(Example::new);
		    engine.loadEcoreFile("EcoreInEcoreVersions.ecore",
		      Example.class.getResourceAsStream("/EcoreInEcoreVersions.ecore"));
		    engine.loadEcoreFile("EcoreInEcoreVersionsSubdir.ecore",
		      Example.class.getResourceAsStream("/EcoreInEcoreVersionsSubdir.ecore"));
		    engine.execute();
		    engine.save("modified");
		  }
		}
		""",
		true);
	}

	@Test
	public void testAlwaysGenerateChangeOfNsURI() throws Exception {
		var rs = createResourceSetWithEcores(List.of(SIMPLE_ECORE, ANOTHER_SIMPLE_ECORE),
		"""
		package foo;

		migrate "http://www.simple" to "http://www.simple/v2"
		migrate "http://www.anothersimple" to "http://www.anothersimple/v2"
		""");
		checkCompilation(rs, """
		package foo;
		
		import edelta.lib.EdeltaDefaultRuntime;
		import edelta.lib.EdeltaEngine;
		import edelta.lib.EdeltaRuntime;
		import edelta.lib.annotation.EdeltaGenerated;
		import java.util.List;
		
		@SuppressWarnings("all")
		public class Example extends EdeltaDefaultRuntime {
		  public Example(final EdeltaRuntime other) {
		    super(other);
		  }
		
		  @Override
		  public void performSanityChecks() throws Exception {
		    ensureEPackageIsLoadedByNsURI("simple", "http://www.simple");
		    ensureEPackageIsLoadedByNsURI("anothersimple", "http://www.anothersimple");
		  }
		
		  @Override
		  protected void doExecute() throws Exception {
		    getEPackage("simple").setNsURI("http://www.simple/v2");
		    getEPackage("anothersimple").setNsURI("http://www.anothersimple/v2");
		  }
		
		  @Override
		  public List<String> getMigratedNsURIs() {
		    return List.of(
		      "http://www.simple",
		      "http://www.anothersimple"
		    );
		  }
		
		  @Override
		  public List<String> getMigratedEcorePaths() {
		    return List.of(
		      "/Simple.ecore",
		      "/AnotherSimple.ecore"
		    );
		  }
		
		  @EdeltaGenerated
		  public static void main(final String[] args) throws Exception {
		    var engine = new EdeltaEngine(Example::new);
		    engine.loadEcoreFile("Simple.ecore",
		      Example.class.getResourceAsStream("/Simple.ecore"));
		    engine.loadEcoreFile("AnotherSimple.ecore",
		      Example.class.getResourceAsStream("/AnotherSimple.ecore"));
		    engine.execute();
		    engine.save("modified");
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
