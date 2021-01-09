package edelta.tests

import com.google.inject.Inject
import com.google.inject.Provider
import edelta.edelta.EdeltaEcoreDirectReference
import edelta.edelta.EdeltaEcoreQualifiedReference
import edelta.edelta.EdeltaEcoreReferenceExpression
import edelta.edelta.EdeltaModifyEcoreOperation
import edelta.edelta.EdeltaProgram
import edelta.resource.derivedstate.EdeltaAccessibleElements
import edelta.tests.input.Inputs
import java.nio.file.Paths
import java.util.List
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.EAttribute
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EEnum
import org.eclipse.emf.ecore.ENamedElement
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EReference
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.emf.ecore.xmi.XMIResource
import org.eclipse.xtext.EcoreUtil2
import org.eclipse.xtext.diagnostics.Severity
import org.eclipse.xtext.resource.XtextResourceSet
import org.eclipse.xtext.testing.util.ParseHelper
import org.eclipse.xtext.testing.validation.ValidationTestHelper
import org.eclipse.xtext.xbase.XAbstractFeatureCall
import org.eclipse.xtext.xbase.XBlockExpression
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.XVariableDeclaration
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations

import static extension org.junit.Assert.*
import static extension edelta.lib.EdeltaLibrary.*
import java.util.function.Consumer

abstract class EdeltaAbstractTest {

	@Inject
	Provider<XtextResourceSet> resourceSetProvider

	@Inject protected extension ParseHelper<EdeltaProgram> parseHelper
	@Inject protected extension ValidationTestHelper validationTestHelper
	@Inject protected extension IJvmModelAssociations

	protected extension Inputs = new Inputs

	protected static String METAMODEL_PATH = "src/edelta/tests/input/models/"
	protected static String ECORE_ECORE = "EcoreForTests.ecore"
	protected static String PERSON_LIST_ECORE = "PersonList.ecore"
	protected static String TEST1_REFS_ECORE = "TestEcoreForReferences1.ecore"
	protected static String TEST2_REFS_ECORE = "TestEcoreForReferences2.ecore"
	protected static String SIMPLE_ECORE = "Simple.ecore"
	protected static String ANOTHER_SIMPLE_ECORE = "AnotherSimple.ecore"

	/**
	 * Parse several input sources using the "foo" EPackage
	 * and returns the parsed program corresponding
	 * to the last input source.
	 */
	def protected parseSeveralWithTestEcore(List<CharSequence> inputs) {
		val rs = resourceSetWithTestEcore
		parseSeveralInputs(inputs, rs)
	}

	/**
	 * Parse several input sources using the "foo" and "bar" EPackages
	 * and returns the parsed program corresponding
	 * to the last input source.
	 */
	def protected parseSeveralWithTestEcores(List<CharSequence> inputs) {
		val rs = resourceSetWithTestEcores
		parseSeveralInputs(inputs, rs)
	}

	protected def EdeltaProgram parseSeveralInputs(List<CharSequence> inputs, ResourceSet rs) {
		var EdeltaProgram program
		for (input : inputs)
			program = input.parse(rs)
		return program
	}

	def protected parseWithTestEcore(CharSequence input) {
		input.parse(resourceSetWithTestEcore)
	}

	def protected parseWithTestEcores(CharSequence input) {
		input.parse(resourceSetWithTestEcores)
	}

	def protected parseWithTestEcoreWithSubPackage(CharSequence input) {
		input.parse(resourceSetWithTestEcoreWithSubPackage)
	}

	def protected parseWithTestEcoresWithReferences(CharSequence input) {
		input.parse(resourceSetWithTestEcoresWithReferences)
	}

	def protected parseWithLoadedEcore(String path, CharSequence input) {
		val resourceSet = resourceSetProvider.get
		// Loads the Ecore package to ensure it is available during loading.
		resourceSet.getResource(createFileURIFromPath(METAMODEL_PATH + ECORE_ECORE), true)
		val uri = createFileURIFromPath(path);
		resourceSet.getResource(uri, true);
		val prog = input.parse(resourceSet)
		return prog
	}
	
	protected def URI createFileURIFromPath(String path) {
		URI.createFileURI(
			Paths.get(path).toAbsolutePath().toString())
	}

	def protected resourceSetWithTestEcore() {
		val resourceSet = resourceSetProvider.get
		addEPackageForTests(resourceSet)
	}

	def protected resourceSetWithTestEcoreWithSubPackage() {
		val resourceSet = resourceSetProvider.get
		addEPackageWithSubPackageForTests(resourceSet)
	}

	def protected resourceSetWithTestEcoresWithReferences() {
		val resourceSet = resourceSetProvider.get
		addEPackagesWithReferencesForTests(resourceSet)
		resourceSet
	}

	protected def void addEPackagesWithReferencesForTests(ResourceSet resourceSet) {
		val packages = EPackagesWithReferencesForTest
		for (p : packages) {
			resourceSet.createTestResource(p.name, p)
		}
	}

	def protected addEPackageForTests(ResourceSet resourceSet) {
		resourceSet.createTestResource("foo", EPackageForTests)
	}

	def protected addEPackageWithSubPackageForTests(ResourceSet resourceSet) {
		resourceSet.createTestResource("mainpackage", EPackageWithSubPackageForTests)
	}

	def protected resourceSetWithTestEcores() {
		val resourceSet = resourceSetWithTestEcore
		addEPackageForTests2(resourceSet)
	}

	def protected addEPackageForTests2(ResourceSet resourceSet) {
		resourceSet.createTestResource("bar", EPackageForTests2)
	}

	def protected createTestResource(ResourceSet resourceSet, String ecoreName, EPackage epackage) {
		val resource = resourceSet.createResource(URI.createURI(ecoreName + ".ecore"))
		resource.contents += epackage
		resourceSet
	}

	def protected createEPackage(String name, String nsPrefix, String nsURI, Consumer<EPackage> initializer) {
		val pack = EcoreFactory.eINSTANCE.createEPackage
		pack.name = name
		pack.nsPrefix = nsPrefix
		pack.nsURI = nsURI
		initializer.accept(pack)
		return pack
	}

	def protected void createEOperation(EClass c, String name) {
		val op = EcoreFactory.eINSTANCE.createEOperation
		op.name = name
		c.EOperations += op
	}

	/**
	 * IMPORTANT: if you add something to this ecore, which is created on the fly,
	 * and you have a test for the generated Java code, then you must also
	 * update testecores/foo.ecore accordingly
	 */
	def protected EPackageForTests() {
		createEPackage("foo", "foo", "http://foo") [
			addNewEClass("FooClass") [
				addNewEAttribute("myAttribute", null)
				addNewEReference("myReference", null)
				createEOperation("myOp")
			]
			addNewEDataType("FooDataType", null)
			addNewEEnum("FooEnum") [
				addNewEEnumLiteral("FooEnumLiteral")
			]
		]
	}

	def protected EPackageForTests2() {
		createEPackage("bar", "bar", "http://bar") [
			addNewEClass("BarClass") [
				addNewEAttribute("myAttribute", null)
				addNewEReference("myReference", null)
			]
			addNewEDataType("BarDataType", null)
		]
	}

	def protected EPackagesWithReferencesForTest() {
		val p1 = createEPackage(
				"testecoreforreferences1",
				"testecoreforreferences1",
				"http://my.testecoreforreferences1") [
			addNewEClass("Person") [
				addNewEAttribute("name", null)
				addNewEReference("works", null)
			]
		]
		val p2 = createEPackage(
				"testecoreforreferences2",
				"testecoreforreferences2",
				"http://my.testecoreforreferences2") [
			addNewEClass("WorkPlace") [
				addNewEAttribute("address", null)
				addNewEReference("persons", null) [
					upperBound = -1
				]
			]
		]
		val works = p1.getEClassByName("Person").getEReferenceByName("works")
		val persons = p2.getEClassByName("WorkPlace").getEReferenceByName("persons")
		works.EOpposite = persons
		persons.EOpposite = works
		#[p1, p2]
	}

	def protected EPackageWithSubPackageForTests() {
		createEPackage("mainpackage", "mainpackage", "http://mainpackage") [
			addNewEClass("MainFooClass") [
				addNewEAttribute("myAttribute", null)
				addNewEReference("myReference", null)
			]
			addNewEDataType("MainFooDataType", null)
			addNewEEnum("MainFooEnum") [
				addNewEEnumLiteral("FooEnumLiteral")
			]
			addNewEClass("MyClass") [ // this is present also in subpackages with the same name
				addNewEAttribute("myClassAttribute", null)
			]
			addNewESubpackage("mainsubpackage", "mainsubpackage", "http://mainsubpackage") [
				addNewEClass("MainSubPackageFooClass") [
					addNewEAttribute("mySubPackageAttribute", null)
					addNewEReference("mySubPackageReference", null)
				]
				addNewEClass("MyClass") [ // this is present also in subpackages with the same name
					addNewEAttribute("myClassAttribute", null)
				]
				addNewESubpackage("subsubpackage", "subsubpackage", "http://subsubpackage") [
					addNewEClass("MyClass") // this is present also in subpackages with the same name
				]
			]
		]
	}

	def protected assertErrorsAsStrings(EObject o, CharSequence expected) {
		expected.toString.trim.assertEqualsStrings(
			o.validate.filter[severity == Severity.ERROR].
				map[message].sort.join("\n"))
	}

	def protected assertEqualsStrings(CharSequence expected, CharSequence actual) {
		expected.toString.replace("\r", "").
			assertEquals(actual.toString.replace("\r", ""))
	}

	def protected assertNamedElements(Iterable<? extends ENamedElement> elements, CharSequence expected) {
		expected.assertEqualsStrings(
			elements.map[name].join("\n") + "\n"
		)
	}

	def protected assertAccessibleElements(EdeltaAccessibleElements elements, CharSequence expected) {
		expected.assertEqualsStrings(
			elements.map[qualifiedName.toString].sortBy[it].join("\n") + "\n"
		)
	}

	def protected getEPackageByName(EdeltaProgram context, String packagename) {
		context.eResource.resourceSet.resources.filter(XMIResource).
			map[contents.head as EPackage].findFirst[name == packagename]
	}

	def protected getEClassifierByName(EdeltaProgram context, String packagename, String classifiername) {
		getEPackageByName(context, packagename).EClassifiers.
			findFirst[name == classifiername]
	}

	def protected <T extends ENamedElement> getByName(Iterable<T> namedElements, String nameToSearch) {
		return namedElements.findFirst[name == nameToSearch]
	}

	def protected lastModifyEcoreOperation(EdeltaProgram p) {
		p.modifyEcoreOperations.last
	}

	def protected lastOperation(EdeltaProgram p) {
		p.operations.last
	}

	def protected getLastCopiedEPackageLastEClass(EObject context) {
		val copiedEPackage = getLastCopiedEPackage(context)
		copiedEPackage.EClassifiers.last as EClass
	}

	def protected getLastCopiedEPackageFirstEClass(EObject context) {
		val copiedEPackage = getLastCopiedEPackage(context)
		copiedEPackage.EClassifiers.head as EClass
	}

	def protected getLastCopiedEPackageFirstEClass(EObject context, String nameToSearch) {
		val p = getLastCopiedEPackage(context)
		p.EClassifiers.findFirst[name == nameToSearch] as EClass
	}

	def protected getLastCopiedEPackage(EObject context) {
		getCopiedEPackages(context).last
	}

	def protected getCopiedEPackages(EObject context) {
		context.eResource.contents.filter(EPackage)
	}

	def protected getEClassiferByName(EPackage p, String nameToSearch) {
		p.EClassifiers.findFirst[name == nameToSearch]
	}

	def protected getEClassByName(EPackage p, String nameToSearch) {
		p.EClassifiers.filter(EClass).findFirst[name == nameToSearch]
	}

	def protected getEStructuralFeatureByName(EClassifier e, String nameToSearch) {
		(e as EClass).EStructuralFeatures.findFirst[name == nameToSearch]
	}

	def protected getEReferenceByName(EClassifier e, String nameToSearch) {
		(e as EClass).EStructuralFeatures.filter(EReference).findFirst[name == nameToSearch]
	}

	def protected getEAttributeByName(EClassifier e, String nameToSearch) {
		(e as EClass).EStructuralFeatures.
			filter(EAttribute).findFirst[name == nameToSearch]
	}

	def protected getEEnumLiteralByName(EClassifier e, String nameToSearch) {
		(e as EEnum).ELiterals.findFirst[name == nameToSearch]
	}

	def protected getEdeltaEcoreReferenceExpression(XExpression e) {
		e as EdeltaEcoreReferenceExpression
	}

	def protected getEdeltaEcoreReference(XExpression e) {
		e.getEdeltaEcoreReferenceExpression.reference
	}

	def protected getEdeltaEcoreDirectReference(EObject e) {
		e as EdeltaEcoreDirectReference
	}

	def protected getEdeltaEcoreQualifiedReference(EObject e) {
		e as EdeltaEcoreQualifiedReference
	}

	def protected getBlockLastExpression(XExpression e) {
		(e as XBlockExpression).expressions.last
	}

	def protected getBlockFirstExpression(XExpression e) {
		(e as XBlockExpression).expressions.head
	}

	def protected getBlock(XExpression e) {
		e as XBlockExpression
	}

	def protected getVariableDeclaration(XExpression e) {
		e as XVariableDeclaration
	}

	def protected getModifyEcoreOperation(XExpression e) {
		e as EdeltaModifyEcoreOperation
	}

	protected def EClass getLastEClass(EPackage ePackage) {
		ePackage.EClassifiers.last as EClass
	}

	protected def EClass getFirstEClass(EPackage ePackage) {
		ePackage.EClassifiers.head as EClass
	}

	def protected ecoreReferenceExpression(CharSequence ecoreRefString) {
		ecoreRefString
			.parseInsideModifyEcoreWithTestMetamodelFoo
			.lastEcoreReferenceExpression
	}

	def protected parseInsideModifyEcoreWithTestMetamodelFoo(CharSequence body) {
		body
			.inputInsideModifyEcoreWithTestMetamodelFoo
			.parseWithTestEcore
	}

	def protected inputInsideModifyEcoreWithTestMetamodelFoo(CharSequence body) {
		'''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				«body»
			}
		'''
	}

	def protected lastEcoreReferenceExpression(EdeltaProgram p) {
		p.lastModifyEcoreOperation.body
			.blockLastExpression as EdeltaEcoreReferenceExpression
	}

	def protected getAllEcoreReferenceExpressions(EdeltaProgram p) {
		EcoreUtil2.getAllContentsOfType(p, EdeltaEcoreReferenceExpression)
	}

	def protected getFeatureCall(XExpression e) {
		e as XAbstractFeatureCall
	}
}
