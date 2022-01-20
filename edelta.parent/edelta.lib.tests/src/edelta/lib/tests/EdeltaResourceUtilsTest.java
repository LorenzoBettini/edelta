/**
 * 
 */
package edelta.lib.tests;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.eclipse.emf.ecore.ENamedElement;
import org.junit.Before;
import org.junit.Test;

import edelta.lib.EdeltaEPackageManager;
import edelta.lib.EdeltaResourceUtils;

/**
 * Tests for the {@link EdeltaEPackageManager}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaResourceUtilsTest {

	private static final String MODELS = "testdata/models/";

	private EdeltaEPackageManager packageManager;

	@Before
	public void init() {
		packageManager = new EdeltaEPackageManager();
	}

	@Test
	public void testGetEPackage() {
		var r1 = packageManager.loadEcoreFile(MODELS + "My.ecore");
		var r2 = packageManager.loadEcoreFile(MODELS + "My2.ecore");
		var toEmpty = packageManager.loadEcoreFile(MODELS + "Empty.ecore");
		toEmpty.getContents().clear();
		var notEcore = packageManager.loadEcoreFile(MODELS + "MyClass.xmi");
		var packages = EdeltaResourceUtils.getEPackages(List.of(r1, r2, toEmpty, notEcore));
		Assertions.assertThat(packages)
			.extracting(ENamedElement::getName)
			.containsExactly("mypackage", "myotherpackage");
	}

}
