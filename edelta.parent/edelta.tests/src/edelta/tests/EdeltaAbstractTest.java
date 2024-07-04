package edelta.tests;

import static edelta.testutils.EdeltaTestUtils.removeCR;
import static java.util.Arrays.asList;
import static org.eclipse.xtext.EcoreUtil2.getAllContentsOfType;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.filter;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.findFirst;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.head;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.join;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.lastOrNull;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.map;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.sort;
import static org.eclipse.xtext.xbase.lib.ListExtensions.map;
import static org.junit.Assert.assertEquals;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.testing.validation.ValidationTestHelper;
import org.eclipse.xtext.validation.Issue;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XVariableDeclaration;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations;
import org.eclipse.xtext.xbase.lib.Extension;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Provider;

import edelta.edelta.EdeltaEcoreDirectReference;
import edelta.edelta.EdeltaEcoreQualifiedReference;
import edelta.edelta.EdeltaEcoreReference;
import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaModifyEcoreOperation;
import edelta.edelta.EdeltaOperation;
import edelta.edelta.EdeltaProgram;
import edelta.lib.EdeltaModelManager;
import edelta.lib.EdeltaStandardLibrary;
import edelta.resource.derivedstate.EdeltaAccessibleElements;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.tests.input.Inputs;

public abstract class EdeltaAbstractTest {
	@Inject
	private Provider<XtextResourceSet> resourceSetProvider;

	@Inject
	@Extension
	protected ParseHelper<EdeltaProgram> parseHelper;

	@Inject
	@Extension
	protected ValidationTestHelper validationTestHelper;

	@Inject
	@Extension
	protected IJvmModelAssociations jvmModelAssociations;

	@Inject
	@Extension
	protected EdeltaDerivedStateHelper derivedStateHelper;

	@Extension
	protected EdeltaStandardLibrary stdLib = new EdeltaStandardLibrary(new EdeltaModelManager());

	@Extension
	protected Inputs inputs = new Inputs();

	protected static String METAMODEL_PATH = "src/edelta/tests/input/models/";

	protected static String ECORE_ECORE = "EcoreForTests.ecore";

	protected static String PERSON_LIST_ECORE = "PersonList.ecore";

	protected static String TEST1_REFS_ECORE = "TestEcoreForReferences1.ecore";

	protected static String TEST2_REFS_ECORE = "TestEcoreForReferences2.ecore";

	protected static String SIMPLE_ECORE = "Simple.ecore";

	protected static String ANOTHER_SIMPLE_ECORE = "AnotherSimple.ecore";

	/**
	 * Parse several input sources using the "foo" EPackage and returns the parsed
	 * program corresponding to the last input source.
	 * @throws Exception 
	 */
	protected EdeltaProgram parseSeveralWithTestEcore(List<? extends CharSequence> inputs) throws Exception {
		return parseSeveralInputs(inputs, resourceSetWithTestEcore());
	}

	/**
	 * Parse several input sources using the "foo" and "bar" EPackages and returns
	 * the parsed program corresponding to the last input source.
	 * @throws Exception 
	 */
	protected EdeltaProgram parseSeveralWithTestEcores(List<CharSequence> inputs) throws Exception {
		return parseSeveralInputs(inputs, resourceSetWithTestEcores());
	}

	protected EdeltaProgram parseSeveralInputs(List<? extends CharSequence> inputs, ResourceSet rs) throws Exception {
		EdeltaProgram program = null;
		for (CharSequence input : inputs) {
			program = parseHelper.parse(input, rs);
		}
		return program;
	}

	protected EdeltaProgram parseWithTestEcore(CharSequence input) throws Exception {
		return parseHelper.parse(input, resourceSetWithTestEcore());
	}

	protected EdeltaProgram parseWithTestEcoreDifferentNsURI(CharSequence input) throws Exception {
		return parseHelper.parse(input, resourceSetWithTestEcoreDifferentNsURI());
	}

	protected EdeltaProgram parseWithTestEcores(CharSequence input) throws Exception {
		return parseHelper.parse(input, resourceSetWithTestEcores());
	}

	protected EdeltaProgram parseWithTestEcoreWithSubPackage(CharSequence input) throws Exception {
		return parseHelper.parse(input, resourceSetWithTestEcoreWithSubPackage());
	}

	protected EdeltaProgram parseWithTestEcoresWithReferences(CharSequence input) throws Exception {
		return parseHelper.parse(input, resourceSetWithTestEcoresWithReferences());
	}

	protected EdeltaProgram parseWithLoadedEcore(String path, CharSequence input) throws Exception {
		var resourceSet = resourceSetProvider.get();
		// Loads the Ecore package to ensure it is available during loading.
		resourceSet.getResource(
			createFileURIFromPath(
				EdeltaAbstractTest.METAMODEL_PATH + EdeltaAbstractTest.ECORE_ECORE), true);
		var uri = createFileURIFromPath(path);
		resourceSet.getResource(uri, true);
		var prog = parseHelper.parse(input, resourceSet);
		return prog;
	}

	protected URI createFileURIFromPath(String path) {
		return URI.createFileURI(Paths.get(path).toAbsolutePath().toString());
	}

	protected ResourceSet resourceSetWithTestEcore() {
		return addEPackageForTests(resourceSetProvider.get());
	}

	protected ResourceSet resourceSetWithTestEcoreDifferentNsURI() {
		return addEPackageForTestsDifferentNsURI(resourceSetProvider.get());
	}

	protected ResourceSet resourceSetWithTestEcoreWithSubPackage() {
		return addEPackageWithSubPackageForTests(resourceSetProvider.get());
	}

	protected XtextResourceSet resourceSetWithTestEcoresWithReferences() {
		var resourceSet = resourceSetProvider.get();
		addEPackagesWithReferencesForTests(resourceSet);
		return resourceSet;
	}

	protected void addEPackagesWithReferencesForTests(ResourceSet resourceSet) {
		var packages = EPackagesWithReferencesForTest();
		for (EPackage p : packages) {
			createTestResource(resourceSet, p.getName(), p);
		}
	}

	protected ResourceSet addEPackageForTests(ResourceSet resourceSet) {
		return createTestResource(resourceSet, "foo", EPackageForTests());
	}

	protected ResourceSet addEPackageForTestsDifferentNsURI(ResourceSet resourceSet) {
		return createTestResource(resourceSet, "foo", EPackageForTestsDifferentNsURI());
	}

	protected ResourceSet addEPackageWithSubPackageForTests(ResourceSet resourceSet) {
		return createTestResource(resourceSet, "mainpackage", EPackageWithSubPackageForTests());
	}

	protected ResourceSet resourceSetWithTestEcores() {
		return addEPackageForTests2(resourceSetWithTestEcore());
	}

	protected ResourceSet addEPackageForTests2(ResourceSet resourceSet) {
		return createTestResource(resourceSet, "bar", EPackageForTests2());
	}

	protected ResourceSet createTestResource(ResourceSet resourceSet, String ecoreName, EPackage epackage) {
		var resource = resourceSet.createResource(URI.createURI((ecoreName + ".ecore")));
		resource.getContents().add(epackage);
		return resourceSet;
	}

	protected EPackage createEPackage(String name) {
		return createEPackage(name, p -> {});
	}

	protected EPackage createEPackage(String name, Consumer<EPackage> initializer) {
		return createEPackage(name, "", "", initializer);
	}

	protected EPackage createEPackage(String name, String nsPrefix, String nsURI, Consumer<EPackage> initializer) {
		var pack = EcoreFactory.eINSTANCE.createEPackage();
		pack.setName(name);
		pack.setNsPrefix(nsPrefix);
		pack.setNsURI(nsURI);
		initializer.accept(pack);
		return pack;
	}

	protected void createEOperation(EClass c, String name) {
		var op = EcoreFactory.eINSTANCE.createEOperation();
		op.setName(name);
		c.getEOperations().add(op);
	}

	/**
	 * IMPORTANT: if you add something to this ecore, which is created on the fly,
	 * and you have a test for the generated Java code, then you must also update
	 * testecores/foo.ecore accordingly
	 */
	protected EPackage EPackageForTests() {
		return createEPackage("foo", "foo", "http://foo", p -> {
			stdLib.addNewEClass(p, "FooClass", c -> {
				stdLib.addNewEAttribute(c, "myAttribute", null);
				stdLib.addNewEReference(c, "myReference", null);
				createEOperation(c, "myOp");
			});
			stdLib.addNewEDataType(p, "FooDataType", null);
			stdLib.addNewEEnum(p, "FooEnum", e -> {
				stdLib.addNewEEnumLiteral(e, "FooEnumLiteral");
			});
		});
	}

	protected EPackage EPackageForTestsDifferentNsURI() {
		return createEPackage("foo", "foo", "http://foo.org/v2", p -> {
			stdLib.addNewEClass(p, "RenamedFooClass", c -> {
				stdLib.addNewEAttribute(c, "myAttribute", null);
				stdLib.addNewEReference(c, "myReference", null);
				createEOperation(c, "myOp");
			});
			stdLib.addNewEDataType(p, "RenamedFooDataType", null);
			stdLib.addNewEEnum(p, "FooEnum", e -> {
				stdLib.addNewEEnumLiteral(e, "FooEnumLiteral");
			});
		});
	}

	protected EPackage EPackageForTests2() {
		return createEPackage("bar", "bar", "http://bar", p -> {
			stdLib.addNewEClass(p, "BarClass", c -> {
				stdLib.addNewEAttribute(c, "myAttribute", null);
				stdLib.addNewEReference(c, "myReference", null);
			});
			stdLib.addNewEDataType(p, "BarDataType", null);
		});
	}

	protected List<EPackage> EPackagesWithReferencesForTest() {
		var p1 = createEPackage(
			"testecoreforreferences1", "testecoreforreferences1",
			"http://my.testecoreforreferences1",
			p -> {
				stdLib.addNewEClass(p, "Person", c -> {
					stdLib.addNewEAttribute(c, "name", null);
					stdLib.addNewEReference(c, "works", null);
				});
			});
		var p2 = createEPackage(
			"testecoreforreferences2", "testecoreforreferences2",
			"http://my.testecoreforreferences2",
			p -> {
				stdLib.addNewEClass(p, "WorkPlace", c -> {
					stdLib.addNewEAttribute(c, "address", null);
					stdLib.addNewEReference(c, "persons", null, r -> {
						r.setUpperBound(-1);
					});
				});
			});
		var works = getEReferenceByName(getEClassByName(p1, "Person"), "works");
		var persons = getEReferenceByName(getEClassByName(p2, "WorkPlace"), "persons");
		works.setEOpposite(persons);
		persons.setEOpposite(works);
		return asList(p1, p2);
	}

	protected EPackage EPackageWithSubPackageForTests() {
		return createEPackage("mainpackage", "mainpackage", "http://mainpackage", p -> {
			stdLib.addNewEClass(p, "MainFooClass", c -> {
				stdLib.addNewEAttribute(c, "myAttribute", null);
				stdLib.addNewEReference(c, "myReference", null);
			});
			stdLib.addNewEDataType(p, "MainFooDataType", null);
			stdLib.addNewEEnum(p, "MainFooEnum", e -> {
				stdLib.addNewEEnumLiteral(e, "FooEnumLiteral");
			});
			// this is present also in subpackages with the same name
			stdLib.addNewEClass(p, "MyClass", c -> {
				stdLib.addNewEAttribute(c, "myClassAttribute", null);
			});
			stdLib.addNewESubpackage(p, "mainsubpackage", "mainsubpackage", "http://mainsubpackage",
				p1 -> {
					stdLib.addNewEClass(p1, "MainSubPackageFooClass", c -> {
						stdLib.addNewEAttribute(c, "mySubPackageAttribute", null);
						stdLib.addNewEReference(c, "mySubPackageReference", null);
					});
					// this is present also in subpackages with the same name
					stdLib.addNewEClass(p1, "MyClass", c -> {
						stdLib.addNewEAttribute(c, "myClassAttribute", null);
					});
					stdLib.addNewESubpackage(p1, "subsubpackage", "subsubpackage", "http://subsubpackage",
						p2 -> {
							// this is present also in subpackages with the same name
							stdLib.addNewEClass(p2, "MyClass");
						});
				});
		});
	}

	protected void assertErrorsAsStrings(EObject o, CharSequence expected) {
		assertEqualsStrings(expected.toString().trim(),
			join(sort(
				map(
					filter(validationTestHelper.validate(o), it -> it.getSeverity() == Severity.ERROR),
					Issue::getMessage)),
			"\n"));
	}

	protected void assertEqualsStrings(CharSequence expected, CharSequence actual) {
		assertEquals(removeCR(expected.toString()),
			removeCR(actual.toString()));
	}

	protected void assertNamedElements(Iterable<? extends ENamedElement> elements, CharSequence expected) {
		var end = "";
		if (expected.toString().endsWith("\n")) {
			end = "\n";
		}
		assertEqualsStrings(expected,
			join(
				map(elements, ENamedElement::getName),
				"\n")
			+ end);
	}

	protected void assertAccessibleElements(EdeltaAccessibleElements elements, CharSequence expected) {
		var end = "";
		if (expected.toString().endsWith("\n")) {
			end = "\n";
		}
		assertEqualsStrings(expected,
			join(
				sort(
					map(elements, it -> it.getQualifiedName().toString())),
				"\n")
			+ end);
	}

	protected EPackage getEPackageByName(EdeltaProgram context, String packagename) {
		return findFirst(
				map(
					Iterables.<XMIResource>filter(
						context.eResource().getResourceSet().getResources(), XMIResource.class),
					it -> (EPackage) head(it.getContents())),
				it -> Objects.equal(it.getName(), packagename));
	}

	protected EClassifier getEClassifierByName(EdeltaProgram context, String packagename, String classifiername) {
		return findFirst(getEPackageByName(context, packagename).getEClassifiers(),
			it -> Objects.equal(it.getName(), classifiername));
	}

	protected <T extends ENamedElement> T getByName(Iterable<T> namedElements, String nameToSearch) {
		return findFirst(namedElements, it -> Objects.equal(it.getName(), nameToSearch));
	}

	protected XExpression getLastModifyEcoreOperationLastExpression(EdeltaProgram p) {
		return getBlockLastExpression(lastModifyEcoreOperation(p).getBody());
	}

	protected XExpression getLastModifyEcoreOperationFirstExpression(EdeltaProgram p) {
		return getBlockFirstExpression(lastModifyEcoreOperation(p).getBody());
	}

	protected XBlockExpression getLastModifyEcoreOperationBlock(EdeltaProgram p) {
		return getBlock(lastModifyEcoreOperation(p).getBody());
	}

	protected EdeltaModifyEcoreOperation lastModifyEcoreOperation(EdeltaProgram p) {
		return lastOrNull(p.getModifyEcoreOperations());
	}

	protected EdeltaOperation lastOperation(EdeltaProgram p) {
		return lastOrNull(p.getOperations());
	}

	protected EClass getLastCopiedEPackageLastEClass(EObject context) {
		return (EClass) lastOrNull(getLastCopiedEPackage(context).getEClassifiers());
	}

	protected EClass getLastCopiedEPackageFirstEClass(EObject context) {
		return (EClass) head(getLastCopiedEPackage(context).getEClassifiers());
	}

	protected EClass getLastCopiedEPackageFirstEClass(EObject context, String nameToSearch) {
		return (EClass) findFirst(
			getLastCopiedEPackage(context).getEClassifiers(),
				it -> Objects.equal(it.getName(), nameToSearch));
	}

	protected EPackage getLastCopiedEPackage(EObject context) {
		return lastOrNull(getCopiedEPackages(context));
	}

	protected Collection<EPackage> getCopiedEPackages(EObject context) {
		return derivedStateHelper.getCopiedEPackagesMap(context.eResource()).values();
	}

	protected EClassifier getEClassiferByName(EPackage p, String nameToSearch) {
		return findFirst(p.getEClassifiers(),
			it -> Objects.equal(it.getName(), nameToSearch));
	}

	protected EClass getEClassByName(EPackage p, String nameToSearch) {
		return findFirst(Iterables.<EClass>filter(p.getEClassifiers(), EClass.class),
			it -> Objects.equal(it.getName(), nameToSearch));
	}

	protected EStructuralFeature getEStructuralFeatureByName(EClassifier e, String nameToSearch) {
		return findFirst(((EClass) e).getEStructuralFeatures(),
			it -> Objects.equal(it.getName(), nameToSearch));
	}

	protected EReference getEReferenceByName(EClassifier e, String nameToSearch) {
		return findFirst(
			((EClass) e).getEReferences(),
				it -> Objects.equal(it.getName(), nameToSearch));
	}

	protected EAttribute getEAttributeByName(EClassifier e, String nameToSearch) {
		return findFirst(
			((EClass) e).getEAttributes(),
				it -> Objects.equal(it.getName(), nameToSearch));
	}

	protected EEnumLiteral getEEnumLiteralByName(EClassifier e, String nameToSearch) {
		return findFirst(((EEnum) e).getELiterals(),
			it -> Objects.equal(it.getName(), nameToSearch));
	}

	protected EdeltaEcoreReferenceExpression getEdeltaEcoreReferenceExpression(XExpression e) {
		return (EdeltaEcoreReferenceExpression) e;
	}

	protected EdeltaEcoreReference getEdeltaEcoreReference(XExpression e) {
		return getEdeltaEcoreReferenceExpression(e).getReference();
	}

	protected EdeltaEcoreDirectReference getEdeltaEcoreDirectReference(EObject e) {
		return (EdeltaEcoreDirectReference) e;
	}

	protected EdeltaEcoreQualifiedReference getEdeltaEcoreQualifiedReference(EObject e) {
		return (EdeltaEcoreQualifiedReference) e;
	}

	protected XExpression getBlockLastExpression(XExpression e) {
		return lastOrNull(getBlock(e).getExpressions());
	}

	protected XExpression getBlockFirstExpression(XExpression e) {
		return head(getBlock(e).getExpressions());
	}

	protected XBlockExpression getBlock(XExpression e) {
		return (XBlockExpression) e;
	}

	protected XVariableDeclaration getVariableDeclaration(XExpression e) {
		return (XVariableDeclaration) e;
	}

	protected EdeltaModifyEcoreOperation getModifyEcoreOperation(XExpression e) {
		return (EdeltaModifyEcoreOperation) e;
	}

	protected EClass getLastEClass(EPackage ePackage) {
		return (EClass) lastOrNull(ePackage.getEClassifiers());
	}

	protected EClass getFirstEClass(EPackage ePackage) {
		return (EClass) head(ePackage.getEClassifiers());
	}

	protected EdeltaEcoreReferenceExpression ecoreReferenceExpression(CharSequence ecoreRefString) throws Exception {
		return lastEcoreReferenceExpression(
			parseInsideModifyEcoreWithTestMetamodelFoo(ecoreRefString));
	}

	protected EdeltaProgram parseInsideModifyEcoreWithTestMetamodelFoo(CharSequence body) throws Exception {
		return parseWithTestEcore(
			inputInsideModifyEcoreWithTestMetamodelFoo(body));
	}

	protected CharSequence inputInsideModifyEcoreWithTestMetamodelFoo(CharSequence body) {
		return String.format(
			"metamodel \"foo\"\n"
			+ "\n"
			+ "modifyEcore aTest epackage foo {\n"
			+ "  %s\n"
			+ "}", body);
	}

	protected EdeltaEcoreReferenceExpression lastEcoreReferenceExpression(EdeltaProgram p) {
		return (EdeltaEcoreReferenceExpression) getBlockLastExpression(lastModifyEcoreOperation(p).getBody());
	}

	protected EdeltaEcoreReferenceExpression getFirstOfAllEcoreReferenceExpressions(EdeltaProgram p) {
		return head(getAllEcoreReferenceExpressions(p));
	}

	protected EdeltaEcoreReferenceExpression getLastOfAllEcoreReferenceExpressions(EdeltaProgram p) {
		return lastOrNull(getAllEcoreReferenceExpressions(p));
	}

	protected List<EdeltaEcoreReferenceExpression> getAllEcoreReferenceExpressions(EdeltaProgram p) {
		return getAllContentsOfType(p, EdeltaEcoreReferenceExpression.class);
	}

	protected XAbstractFeatureCall getFeatureCall(XExpression e) {
		return (XAbstractFeatureCall) e;
	}
}
