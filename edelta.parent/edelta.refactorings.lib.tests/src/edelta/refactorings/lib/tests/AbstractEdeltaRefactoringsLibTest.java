package edelta.refactorings.lib.tests;

import static com.google.common.collect.Iterables.filter;
import static edelta.testutils.EdeltaTestUtils.assertFilesAreEquals;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.findFirst;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;

import edelta.lib.EdeltaEngine;
import edelta.lib.EdeltaModelManager;
import edelta.lib.EdeltaStandardLibrary;
import edelta.lib.EdeltaUtils;
import edelta.lib.EdeltaEngine.EdeltaRuntimeProvider;

public abstract class AbstractEdeltaRefactoringsLibTest {
	protected EcoreFactory factory = EcoreFactory.eINSTANCE;

	protected EDataType stringDataType = EcorePackage.eINSTANCE.getEString();

	protected EDataType intDataType = EcorePackage.eINSTANCE.getEInt();

	protected EClass eClassReference = EcorePackage.eINSTANCE.getEClass();

	protected static final String MODIFIED = "modified/";

	protected static final String TESTECORES = "test-input-models/";

	protected static final String EXPECTATIONS = "test-output-expectations/";

	protected EdeltaStandardLibrary stdLib = new EdeltaStandardLibrary(new EdeltaModelManager());

	protected EdeltaEngine setupEngine(
			String subdir,
			Collection<String> ecoreFiles,
			Collection<String> modelFiles,
			EdeltaRuntimeProvider runtimeProvider
		) {
		var basedir = TESTECORES + subdir;
		var engine = new EdeltaEngine(runtimeProvider);
		ecoreFiles
			.forEach(fileName -> engine.loadEcoreFile(basedir + fileName));
		modelFiles
			.forEach(fileName -> engine.loadModelFile(basedir + fileName));
		return engine;
	}

	protected void assertOutputs(
			EdeltaEngine engine,
			String outputdir,
			Collection<String> ecoreFiles,
			Collection<String> modelFiles
		) throws Exception {
		engine.execute();
		var output = MODIFIED + outputdir;
		engine.save(output);
		ecoreFiles.forEach
			(fileName ->
				assertGeneratedFiles(fileName, outputdir, output, fileName));
		modelFiles.forEach
			(fileName ->
				assertGeneratedFiles(fileName, outputdir, output, fileName));
	}

	protected void assertGeneratedFiles(String message, String subdir, String outputDir, String fileName) {
		try {
			assertFilesAreEquals(
				message,
				EXPECTATIONS + subdir + fileName,
				outputDir + fileName);
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getClass().getName() + ": " + e.getMessage());
		}
	}

	protected EPackage createEPackage(final String name) {
		EPackage p = this.factory.createEPackage();
		p.setName(name);
		return p;
	}

	protected EPackage createEPackage(final String name, Consumer<EPackage> init) {
		EPackage p = this.factory.createEPackage();
		p.setName(name);
		init.accept(p);
		return p;
	}

	protected EClass createEClass(final EPackage epackage, final String name) {
		final EClass c = this.createEClassWithoutPackage(name);
		epackage.getEClassifiers().add(c);
		return c;
	}

	protected EClass createEClassWithoutPackage(final String name) {
		EClass c = this.factory.createEClass();
		c.setName(name);
		return c;
	}

	protected EAttribute createEAttribute(final EClass eclass, final String name) {
		EAttribute attr = this.factory.createEAttribute();
		attr.setName(name);
		eclass.getEStructuralFeatures().add(attr);
		return attr;
	}

	protected EReference createEReference(final EClass eclass, final String name) {
		EReference ref = this.factory.createEReference();
		ref.setName(name);
		eclass.getEStructuralFeatures().add(ref);
		return ref;
	}

	protected Iterable<EClass> EClasses(final EPackage p) {
		return filter(p.getEClassifiers(), EClass.class);
	}

	protected EClassifier findEClassifier(final EPackage p, final String byName) {
		return findFirst(p.getEClassifiers(), it -> Objects.equals(it.getName(), byName));
	}

	protected EClass findEClass(final EPackage p, final String byName) {
		return (EClass) findEClassifier(p, byName);
	}

	protected EAttribute findEAttribute(final EClass c, final String byName) {
		return findFirst(c.getEAttributes(), it -> Objects.equals(it.getName(), byName));
	}

	protected EStructuralFeature findEStructuralFeature(final EPackage p, final String className,
			final String featureName) {
		return EdeltaUtils.allEClasses(p).stream()
				.filter(c -> c.getName().equals(className))
				.findFirst()
				.map(c -> c.getEStructuralFeature(featureName))
				.orElse(null);
	}

	protected EReference findEReference(final EPackage p, final String className,
			final String referenceName) {
		return (EReference) findEStructuralFeature(p, className, referenceName);
	}

}
