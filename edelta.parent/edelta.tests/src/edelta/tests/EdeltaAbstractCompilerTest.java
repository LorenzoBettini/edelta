package edelta.tests;

import static org.eclipse.xtext.xbase.lib.Conversions.unwrapArray;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.FileExtensionProvider;
import org.eclipse.xtext.testing.TemporaryFolder;
import org.eclipse.xtext.workspace.FileProjectConfig;
import org.eclipse.xtext.workspace.ProjectConfigAdapter;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.Pair;
import org.eclipse.xtext.xbase.testing.CompilationTestHelper;
import org.junit.Assert;
import org.junit.Rule;

import com.google.common.collect.Iterables;
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
		res.assertNoErrors();
	}

	protected void assertGeneratedJavaCode(CompilationTestHelper.Result r, CharSequence expected) {
		assertEquals(EdeltaTestUtils.removeCR(expected.toString()),
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

	protected void checkCompilationOfSeveralFiles(final List<? extends CharSequence> inputs,
			final List<Pair<String, CharSequence>> expectations) throws Exception {
		compilationTestHelper.compile(
			createResourceSet(((CharSequence[]) Conversions.unwrapArray(inputs, CharSequence.class))),
			it -> {
				assertNoValidationErrors(it);
				for (var expectation : expectations) {
					Assert.assertEquals(expectation.getValue().toString(),
						EdeltaTestUtils.removeCR(it.getGeneratedCode(expectation.getKey())));
				}
				assertGeneratedJavaCodeCompiles(it);
			});
	}

	protected ResourceSet createResourceSetWithEcores(final List<String> ecoreNames, final CharSequence input)
			throws IOException {
		var pairs = CollectionLiterals.newArrayList(
				Pair.of(EdeltaAbstractTest.ECORE_ECORE,
						EdeltaTestUtils.loadFile(EdeltaAbstractTest.METAMODEL_PATH + EdeltaAbstractTest.ECORE_ECORE)),
				Pair.of("Example." + extensionProvider.getPrimaryFileExtension(), input));
		// Ecore files with a path containing "/" (e.g., "ecoreversions/Foo.ecore") are
		// treated specially: they must be placed outside the default "src/" source folder
		// so that their top-level directory becomes a dedicated source folder in the
		// project config. This ensures EdeltaCompilerUtil.getRelativeSourcePath() returns
		// only the path relative to that source folder (e.g., "/Foo.ecore"), without
		// including the source folder name itself in the result.
		var ecoresInSubdirectory = new ArrayList<String>();
		for (var ecoreName : ecoreNames) {
			if (ecoreName.contains("/")) {
				ecoresInSubdirectory.add(ecoreName);
			} else {
				try {
					pairs.add(Pair.of(ecoreName,
							EdeltaTestUtils.loadFile(EdeltaAbstractTest.METAMODEL_PATH + ecoreName)));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		@SuppressWarnings("unchecked")
		final ResourceSet rs = compilationTestHelper
				.resourceSet(pairs.toArray(new Pair[0]));
		addEcoresInSubdirectoryToResourceSet(rs, ecoresInSubdirectory);
		return rs;
	}

	protected ResourceSet createResourceSetWithEcoresAndSeveralInputs(final List<String> ecoreNames,
			final List<CharSequence> inputs) throws IOException {
		var ecorePairs = CollectionLiterals.newArrayList(
				Pair.of(EdeltaAbstractTest.ECORE_ECORE,
						EdeltaTestUtils.loadFile(EdeltaAbstractTest.METAMODEL_PATH + EdeltaAbstractTest.ECORE_ECORE)));
		// Same rationale as in createResourceSetWithEcores: ecore names containing "/"
		// are handled separately so their subdirectory becomes a dedicated source folder.
		var ecoresInSubdirectory = new ArrayList<String>();
		for (var ecoreName : ecoreNames) {
			if (ecoreName.contains("/")) {
				ecoresInSubdirectory.add(ecoreName);
			} else {
				try {
					ecorePairs.add(Pair.of(ecoreName,
							EdeltaTestUtils.loadFile(EdeltaAbstractTest.METAMODEL_PATH + ecoreName)));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		var inputPairs = createInputPairs(inputs.toArray(new CharSequence[0]));
		Iterable<Pair<String, ? extends CharSequence>> concat = Iterables
				.concat(ecorePairs, inputPairs);
		@SuppressWarnings("unchecked")
		final ResourceSet rs = compilationTestHelper
				.resourceSet(((Pair<String, ? extends CharSequence>[]) Conversions.unwrapArray(concat, Pair.class)));
		addEcoresInSubdirectoryToResourceSet(rs, ecoresInSubdirectory);
		return rs;
	}

	/**
	 * Places each ecore-in-subdirectory file at the project root level (outside
	 * {@code src/}) and registers its top-level directory as a dedicated source
	 * folder in the {@link FileProjectConfig}. This guarantees that
	 * {@code IProjectConfig.findSourceFolderContaining(URI)} matches that directory
	 * rather than {@code src/}, so that
	 * {@code EdeltaCompilerUtil.getRelativeSourcePath()} returns only the filename
	 * (and any deeper relative path) without the source folder name as a prefix.
	 */
	private void addEcoresInSubdirectoryToResourceSet(ResourceSet rs, List<String> ecoresInSubdirectory)
			throws IOException {
		if (ecoresInSubdirectory.isEmpty()) {
			return;
		}
		var projectConfig = (FileProjectConfig) ProjectConfigAdapter.findInEmfObject(rs).getProjectConfig();
		var addedSourceFolders = new HashSet<String>();
		for (var ecoreName : ecoresInSubdirectory) {
			var sourceFolderName = ecoreName.substring(0, ecoreName.indexOf('/'));
			if (addedSourceFolders.add(sourceFolderName)) {
				projectConfig.addSourceFolder(sourceFolderName);
			}
			var uri = compilationTestHelper.copyToWorkspace(
					CompilationTestHelper.PROJECT_NAME + "/" + ecoreName,
					EdeltaTestUtils.loadFile(EdeltaAbstractTest.METAMODEL_PATH + ecoreName));
			var resource = rs.createResource(uri);
			resource.load(rs.getLoadOptions());
		}
	}
}
