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
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.Pair;
import org.eclipse.xtext.xbase.testing.CompilationTestHelper;
import org.eclipse.xtext.xbase.testing.TemporaryFolder;
import org.junit.Rule;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

import edelta.testutils.EdeltaTestUtils;

/**
 * @author Lorenzo Bettini
 */
public abstract class EdeltaAbstractCompilerTest extends EdeltaAbstractTest {

	@Rule
	@Inject
	public TemporaryFolder temporaryFolder;
	@Inject
	@Extension
	protected CompilationTestHelper compilationTestHelper;
	@Inject
	private FileExtensionProvider extensionProvider;

	protected void checkCompilation(CharSequence input, CharSequence expectedGeneratedJava) throws Exception {
		checkCompilation(input, expectedGeneratedJava, true);
	}

	protected void checkCompilation(CharSequence input, CharSequence expectedGeneratedJava, boolean checkValidationErrors) throws Exception {
		var rs = createResourceSet(input);
		checkCompilation(rs, expectedGeneratedJava, checkValidationErrors);
	}

	protected void checkCompilation(ResourceSet rs, CharSequence expectedGeneratedJava, boolean checkValidationErrors) {
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

	protected void assertNoValidationErrors(CompilationTestHelper.Result res) {
		var allErrors = filter(res.getErrorsAndWarnings(), it -> it.getSeverity() == Severity.ERROR);
		if (!isEmpty(allErrors)) {
			throw new IllegalStateException(
				"One or more resources contained errors : " +
					Joiner.on(",").join(allErrors));
		}
	}

	protected void assertGeneratedJavaCode(CompilationTestHelper.Result r, CharSequence expected) {
		assertEquals(expected.toString(),
			EdeltaTestUtils.removeCR(r.getSingleGeneratedCode()));
	}

	protected Class<?> assertGeneratedJavaCodeCompiles(CompilationTestHelper.Result r) {
		return r.getCompiledClass();
	}

	protected ResourceSet createResourceSet(CharSequence... inputs) throws Exception {
		var pairs = createInputPairs(inputs);
		@SuppressWarnings("unchecked")
		var rs = compilationTestHelper
			.resourceSet(((Pair<String, ? extends CharSequence>[])
					unwrapArray(pairs, Pair.class)));
		addEPackageForTests(rs);
		return rs;
	}

	protected List<Pair<String, CharSequence>> createInputPairs(CharSequence[] inputs) {
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
