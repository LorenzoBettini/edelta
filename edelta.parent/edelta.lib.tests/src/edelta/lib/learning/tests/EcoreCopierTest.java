package edelta.lib.learning.tests;

import static edelta.testutils.EdeltaTestUtils.assertFilesAreEquals;
import static edelta.testutils.EdeltaTestUtils.cleanDirectory;
import static edelta.testutils.EdeltaTestUtils.loadFile;

import java.io.IOException;
import java.nio.file.Paths;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.EcoreUtil.Copier;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.testutils.EdeltaTestUtils;

public class EcoreCopierTest {

	private static final String MODIFIED = "modified/";
	private static final String ORIGINAL = "original/";
	private static final String TESTDATA = "testdata/";
	private static final String OUTPUT = "output/";
	private static final String EXPECTATIONS = "expectations/";

	EdeltaDefaultRuntime runtimeForOriginal;
	EdeltaDefaultRuntime runtimeForModified;

	@BeforeClass
	public static void clearOutput() throws IOException {
		cleanDirectory(OUTPUT);
	}

	@Before
	public void setup() {
		runtimeForOriginal = new EdeltaDefaultRuntime();
		runtimeForModified = new EdeltaDefaultRuntime();
	}

	@Test
	public void testSaveXMI() throws IOException {
		var originalEcore = runtimeForOriginal.loadEcoreFile(TESTDATA + "renamed/" + ORIGINAL + "My.ecore");
		var modifiedEcore = runtimeForModified.loadEcoreFile(TESTDATA + "renamed/" + MODIFIED + "My.ecore");

		// this is actually XMI
		var original = runtimeForOriginal.loadEcoreFile(TESTDATA + "renamed/" + ORIGINAL + "MyRoot.xmi");
		var modified = runtimeForModified.loadEcoreFile(TESTDATA + "renamed/" + MODIFIED + "MyRoot.xmi");

		runtimeForModified.saveModifiedEcores(OUTPUT);
	}

	@Test
	public void testCopyUnchanged() throws IOException {
		var originalEcore = runtimeForOriginal.loadEcoreFile(TESTDATA + "renamed/" + ORIGINAL + "My.ecore");

		// this is actually XMI
		var original = runtimeForOriginal.loadEcoreFile(TESTDATA + "renamed/" + ORIGINAL + "MyRoot.xmi");

		var copier = new EcoreUtil.Copier();
		var root = original.getContents().get(0);
		var copy = copier.copy(root);
		original.getContents().clear();
		original.getContents().add(copy);

		runtimeForModified.saveModifiedEcores(OUTPUT + "renamed/");
		assertFilesAreEquals(
			EXPECTATIONS + "renamed/" +"MyRoot.xmi",
			OUTPUT + "renamed/" + "MyRoot.xmi");
	}
}
