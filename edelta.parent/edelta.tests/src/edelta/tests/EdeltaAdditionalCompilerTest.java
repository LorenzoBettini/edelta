package edelta.tests;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.util.JavaVersion;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import edelta.tests.injectors.EdeltaInjectorProviderTestableDerivedStateComputer;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderTestableDerivedStateComputer.class)
public class EdeltaAdditionalCompilerTest extends EdeltaAbstractCompilerTest {
	@Before
	public void setup() {
		compilationTestHelper.setJavaVersion(JavaVersion.JAVA8);
	}

	@Test
	public void testCompilationOfTypeParameters() throws Exception {
		checkCompilation("""
		import java.util.Collection
		import java.util.List
		import org.eclipse.emf.ecore.EStructuralFeature
		import org.eclipse.emf.ecore.EAttribute
		import org.eclipse.emf.ecore.EReference

		package foo

		metamodel "foo"

		def <T>
			exampleWithoutBound(String newFeatureName, Collection<T> features) : T {
			// just an example
			features.head
		}

		def <T extends EStructuralFeature>
			exampleWithBound(String newFeatureName, Collection<T> features) : T {
			// just an example
			features.head
		}

		modifyEcore aTest epackage foo {
			val EAttribute a = exampleWithBound("anAttribute", List.of(ecoreref(myAttribute)))
			val a1 = exampleWithBound("anAttribute", List.of(ecoreref(myAttribute)))
			val EReference r = exampleWithBound("aReference", List.of(ecoreref(myReference)))
			val r1 = exampleWithBound("aReference", List.of(ecoreref(myReference)))
			val String s = exampleWithoutBound("aReference", List.of("a string"))
			val s1 = exampleWithoutBound("aReference", List.of("a string"))
		}
		""","""
		package foo;
		
		import edelta.lib.EdeltaDefaultRuntime;
		import edelta.lib.EdeltaRuntime;
		import java.util.Collection;
		import java.util.List;
		import org.eclipse.emf.ecore.EAttribute;
		import org.eclipse.emf.ecore.EPackage;
		import org.eclipse.emf.ecore.EReference;
		import org.eclipse.emf.ecore.EStructuralFeature;
		import org.eclipse.xtext.xbase.lib.IterableExtensions;
		
		@SuppressWarnings("all")
		public class MyFile0 extends EdeltaDefaultRuntime {
		  public MyFile0(final EdeltaRuntime other) {
		    super(other);
		  }
		
		  public <T> T exampleWithoutBound(final String newFeatureName, final Collection<T> features) {
		    return IterableExtensions.<T>head(features);
		  }
		
		  public <T extends EStructuralFeature> T exampleWithBound(final String newFeatureName, final Collection<T> features) {
		    return IterableExtensions.<T>head(features);
		  }
		
		  public void aTest(final EPackage it) {
		    final EAttribute a = this.<EAttribute>exampleWithBound("anAttribute", List.<EAttribute>of(getEAttribute("foo", "FooClass", "myAttribute")));
		    final EAttribute a1 = this.<EAttribute>exampleWithBound("anAttribute", List.<EAttribute>of(getEAttribute("foo", "FooClass", "myAttribute")));
		    final EReference r = this.<EReference>exampleWithBound("aReference", List.<EReference>of(getEReference("foo", "FooClass", "myReference")));
		    final EReference r1 = this.<EReference>exampleWithBound("aReference", List.<EReference>of(getEReference("foo", "FooClass", "myReference")));
		    final String s = this.<String>exampleWithoutBound("aReference", List.<String>of("a string"));
		    final String s1 = this.<String>exampleWithoutBound("aReference", List.<String>of("a string"));
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
		"""
		);
	}

	@Test
	public void testCompilationOfModelMigration() throws Exception {
		checkCompilation("""
		package foo;

		metamodel "foo"

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
		    ensureEPackageIsLoaded("foo");
		  }
		  
		  @Override
		  protected void doExecute() throws Exception {
		    aTest(getEPackage("foo"));
		  }
		}
		"""
		);
	}
}
