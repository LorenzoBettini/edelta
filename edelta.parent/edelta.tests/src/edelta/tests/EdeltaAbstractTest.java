package edelta.tests;

import static edelta.lib.EdeltaLibrary.*;
import static java.util.Arrays.asList;
import static org.eclipse.xtext.EcoreUtil2.getAllContentsOfType;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.*;
import static org.eclipse.xtext.xbase.lib.ListExtensions.map;
import static org.junit.Assert.assertEquals;

import java.nio.file.Paths;
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
import edelta.resource.derivedstate.EdeltaAccessibleElements;
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
	protected EdeltaProgram parseSeveralWithTestEcore(List<CharSequence> inputs) throws Exception {
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

	protected EdeltaProgram parseSeveralInputs(List<CharSequence> inputs, ResourceSet rs) throws Exception {
		EdeltaProgram program = null;
		for (CharSequence input : inputs) {
			program = parseHelper.parse(input, rs);
		}
		return program;
	}

	protected EdeltaProgram parseWithTestEcore(CharSequence input) throws Exception {
		return parseHelper.parse(input, resourceSetWithTestEcore());
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
			addNewEClass(p, "FooClass", c -> {
				addNewEAttribute(c, "myAttribute", null);
				addNewEReference(c, "myReference", null);
				createEOperation(c, "myOp");
			});
			addNewEDataType(p, "FooDataType", null);
			addNewEEnum(p, "FooEnum", e -> {
				addNewEEnumLiteral(e, "FooEnumLiteral");
			});
		});
	}

	protected EPackage EPackageForTests2() {
		return createEPackage("bar", "bar", "http://bar", p -> {
			addNewEClass(p, "BarClass", c -> {
				addNewEAttribute(c, "myAttribute", null);
				addNewEReference(c, "myReference", null);
			});
			addNewEDataType(p, "BarDataType", null);
		});
	}

	protected List<EPackage> EPackagesWithReferencesForTest() {
		var p1 = createEPackage(
			"testecoreforreferences1", "testecoreforreferences1",
			"http://my.testecoreforreferences1",
			p -> {
				addNewEClass(p, "Person", c -> {
					addNewEAttribute(c, "name", null);
					addNewEReference(c, "works", null);
				});
			});
		var p2 = createEPackage(
			"testecoreforreferences2", "testecoreforreferences2",
			"http://my.testecoreforreferences2",
			p -> {
				addNewEClass(p, "WorkPlace", c -> {
					addNewEAttribute(c, "address", null);
					addNewEReference(c, "persons", null, r -> {
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
			addNewEClass(p, "MainFooClass", c -> {
				addNewEAttribute(c, "myAttribute", null);
				addNewEReference(c, "myReference", null);
			});
			addNewEDataType(p, "MainFooDataType", null);
			addNewEEnum(p, "MainFooEnum", e -> {
				addNewEEnumLiteral(e, "FooEnumLiteral");
			});
			addNewEClass(p, "MyClass", c -> {
				addNewEAttribute(c, "myClassAttribute", null);
			});
			addNewESubpackage(p, "mainsubpackage", "mainsubpackage", "http://mainsubpackage",
				p1 -> {
					addNewEClass(p1, "MainSubPackageFooClass", c -> {
						addNewEAttribute(c, "mySubPackageAttribute", null);
						addNewEReference(c, "mySubPackageReference", null);
					});
					addNewEClass(p1, "MyClass", c -> {
						addNewEAttribute(c, "myClassAttribute", null);
					});
					addNewESubpackage(p1, "subsubpackage", "subsubpackage", "http://subsubpackage",
						p2 -> {
							addNewEClass(p2, "MyClass");
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
		assertEquals(expected.toString().replace("\r", ""),
			actual.toString().replace("\r", ""));
	}

	protected void assertNamedElements(Iterable<? extends ENamedElement> elements, CharSequence expected) {
		assertEqualsStrings(expected,
			join(
				map(elements, ENamedElement::getName),
				"\n")
			+ "\n");
	}

	protected void assertAccessibleElements(EdeltaAccessibleElements elements, CharSequence expected) {
		assertEqualsStrings(expected,
			join(
				sort(
					map(elements, it -> it.getQualifiedName().toString())),
				"\n")
			+ "\n");
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

	protected EdeltaModifyEcoreOperation lastModifyEcoreOperation(EdeltaProgram p) {
		return last(p.getModifyEcoreOperations());
	}

	protected EdeltaOperation lastOperation(EdeltaProgram p) {
		return last(p.getOperations());
	}

	protected EClass getLastCopiedEPackageLastEClass(EObject context) {
		return (EClass) last(getLastCopiedEPackage(context).getEClassifiers());
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
		return last(getCopiedEPackages(context));
	}

	protected Iterable<EPackage> getCopiedEPackages(EObject context) {
		return Iterables.<EPackage>filter(context.eResource().getContents(), EPackage.class);
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
		return last(((XBlockExpression) e).getExpressions());
	}

	protected XExpression getBlockFirstExpression(XExpression e) {
		return head(((XBlockExpression) e).getExpressions());
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
		return (EClass) last(ePackage.getEClassifiers());
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

	protected List<EdeltaEcoreReferenceExpression> getAllEcoreReferenceExpressions(EdeltaProgram p) {
		return getAllContentsOfType(p, EdeltaEcoreReferenceExpression.class);
	}

	protected XAbstractFeatureCall getFeatureCall(XExpression e) {
		return (XAbstractFeatureCall) e;
	}
}
