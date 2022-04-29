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
public class EdeltaAdditionalCompilerTest extends EdeltaAbstractTest {
	@Rule
	@Inject
	public TemporaryFolder temporaryFolder;

	@Inject
	private CompilationTestHelper compilationTestHelper;

	@Inject
	private FileExtensionProvider extensionProvider;

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

		def <T extends EStructuralFeature>
			mergeFeatures(String newFeatureName, Collection<T> features) : T {
			// just an example
			features.head
		}

		modifyEcore aTest epackage foo {
			val EAttribute a = mergeFeatures("anAttribute", List.of(ecoreref(myAttribute)))
			val EReference r = mergeFeatures("aReference", List.of(ecoreref(myReference)))
		}
		""","""
		package foo;
		
		import edelta.lib.AbstractEdelta;
		import edelta.lib.EdeltaDefaultRuntime;
		import java.util.Collection;
		import java.util.List;
		import org.eclipse.emf.ecore.EAttribute;
		import org.eclipse.emf.ecore.EPackage;
		import org.eclipse.emf.ecore.EReference;
		import org.eclipse.emf.ecore.EStructuralFeature;
		import org.eclipse.xtext.xbase.lib.IterableExtensions;
		
		@SuppressWarnings("all")
		public class MyFile0 extends EdeltaDefaultRuntime {
		  public MyFile0() {
		
		  }
		
		  public MyFile0(final AbstractEdelta other) {
		    super(other);
		  }
		
		  public <T extends EStructuralFeature> T mergeFeatures(final String newFeatureName, final Collection<T> features) {
		    return IterableExtensions.<T>head(features);
		  }
		
		  public void aTest(final EPackage it) {
		    final EAttribute a = this.<EAttribute>mergeFeatures("anAttribute", List.<EAttribute>of(getEAttribute("foo", "FooClass", "myAttribute")));
		    final EReference r = this.<EReference>mergeFeatures("aReference", List.<EReference>of(getEReference("foo", "FooClass", "myReference")));
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
		var singleGeneratedCode = EdeltaTestUtils.removeCR(r.getSingleGeneratedCode());
		assertEquals(expected.toString(),
			// see https://github.com/eclipse/xtext-extras/issues/772
			// remove whe Xtext 2.27.0 is released
			// lines with only spaces are skipped in Java text blocks
			// but they are present in the generated code so we need to remove them
			// do that twice for possible empty constructor's body which has four spaces
			singleGeneratedCode
				.replace("  \n", "\n")
				.replace("  \n", "\n"));
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
