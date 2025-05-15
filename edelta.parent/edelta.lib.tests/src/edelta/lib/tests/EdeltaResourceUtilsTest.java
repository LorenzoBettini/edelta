/**
 *
 */
package edelta.lib.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.eclipse.emf.ecore.ENamedElement;
import org.junit.Before;
import org.junit.Test;

import edelta.lib.EdeltaModelManager;
import edelta.lib.EdeltaResourceUtils;

/**
 * Tests for the {@link EdeltaResourceUtils}.
 *
 * @author Lorenzo Bettini
 *
 */
public class EdeltaResourceUtilsTest {

	private static final String MODELS = "../edelta.testdata/testdata/models/";

	private EdeltaModelManager modelManager;

	@Before
	public void init() {
		modelManager = new EdeltaModelManager();
	}

	@Test
	public void testGetEPackage() {
		var r1 = modelManager.loadEcoreFile(MODELS + "My.ecore");
		var r2 = modelManager.loadEcoreFile(MODELS + "My2.ecore");
		var toEmpty = modelManager.loadEcoreFile(MODELS + "Empty.ecore");
		toEmpty.getContents().clear();
		var notEcore = modelManager.loadEcoreFile(MODELS + "MyClass.xmi");
		var packages = EdeltaResourceUtils.getEPackages(List.of(r1, r2, toEmpty, notEcore));
		assertThat(packages)
			.extracting(ENamedElement::getName)
			.containsExactly("mypackage", "myotherpackage");
	}

	@Test
	public void testGetFileName() {
		var r2 = modelManager.loadEcoreFile(MODELS + "My2.ecore");
		assertEquals("My2.ecore", EdeltaResourceUtils.getFileName(r2));
	}

	@Test
	public void testGetRelativePath() {
		var r2 = modelManager.loadEcoreFile(MODELS + "My2.ecore");
		assertEquals("models/My2.ecore", EdeltaResourceUtils.getRelativePath(r2, List.of(MODELS, MODELS+ "/anonmatching")));
		assertEquals(r2.getURI().path(), EdeltaResourceUtils.getRelativePath(r2, List.of(MODELS+ "/anonmatching")));
	}
}
