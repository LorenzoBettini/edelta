package edelta.tests

import com.google.inject.Inject
import com.google.inject.Provider
import edelta.edelta.EdeltaEcoreDirectReference
import edelta.edelta.EdeltaEcoreQualifiedReference
import edelta.edelta.EdeltaEcoreReferenceExpression
import edelta.edelta.EdeltaModifyEcoreOperation
import edelta.edelta.EdeltaProgram
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
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.emf.ecore.xmi.XMIResource
import org.eclipse.xtext.EcoreUtil2
import org.eclipse.xtext.diagnostics.Severity
import org.eclipse.xtext.resource.XtextResourceSet
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.testing.util.ParseHelper
import org.eclipse.xtext.testing.validation.ValidationTestHelper
import org.eclipse.xtext.xbase.XAbstractFeatureCall
import org.eclipse.xtext.xbase.XBlockExpression
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.XVariableDeclaration
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations
import org.junit.runner.RunWith

import static extension org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProvider)
abstract class EdeltaAbstractTest {

	@Inject
	Provider<XtextResourceSet> resourceSetProvider

	@Inject protected extension ParseHelper<EdeltaProgram>
	@Inject protected extension ValidationTestHelper
	@Inject protected extension IJvmModelAssociations

	protected extension Inputs = new Inputs

	protected static String ECORE_PATH = "src/edelta/tests/input/models/EcoreForTests.ecore"
	protected static String PERSON_LIST_ECORE = "PersonList.ecore"
	protected static String PERSON_LIST_ECORE_PATH = "src/edelta/tests/input/models/" + PERSON_LIST_ECORE

	/**
	 * Parse several input sources and returns the parsed program corresponding
	 * to the last input source.
	 */
	def protected parseSeveralWithTestEcore(List<CharSequence> inputs) {
		val rs = resourceSetWithTestEcore
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

	def protected parseWithLoadedEcore(String path, CharSequence input) {
		val resourceSet = resourceSetProvider.get
		// Loads the Ecore package to ensure it is available during loading.
		resourceSet.getResource(createFileURIFromPath(ECORE_PATH), true)
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

	def protected EPackageForTests() {
		// IMPORTANT: if you add something to this ecore, which is created on the fly,
		// and you have a test for the generated Java code, then you must also
		// update testecores/foo.ecore accordingly
		val fooPackage = EcoreFactory.eINSTANCE.createEPackage => [
			name = "foo"
			nsPrefix = "foo"
			nsURI = "http://foo"
		]
		fooPackage.EClassifiers += EcoreFactory.eINSTANCE.createEClass => [
			name = "FooClass"
			EStructuralFeatures += EcoreFactory.eINSTANCE.createEAttribute => [
				name = "myAttribute"
			]
			EStructuralFeatures += EcoreFactory.eINSTANCE.createEReference => [
				name = "myReference"
			]
		]
		fooPackage.EClassifiers += EcoreFactory.eINSTANCE.createEDataType => [
			name = "FooDataType"
		]
		fooPackage.EClassifiers += EcoreFactory.eINSTANCE.createEEnum => [
			name = "FooEnum"
			ELiterals += EcoreFactory.eINSTANCE.createEEnumLiteral => [
				name = "FooEnumLiteral"
			]
		]
		fooPackage
	}

	def protected EPackageForTests2() {
		val fooPackage = EcoreFactory.eINSTANCE.createEPackage => [
			name = "bar"
			nsPrefix = "bar"
			nsURI = "http://bar"
		]
		fooPackage.EClassifiers += EcoreFactory.eINSTANCE.createEClass => [
			name = "BarClass"
			EStructuralFeatures += EcoreFactory.eINSTANCE.createEAttribute => [
				name = "myAttribute"
			]
			EStructuralFeatures += EcoreFactory.eINSTANCE.createEReference => [
				name = "myReference"
			]
		]
		fooPackage.EClassifiers += EcoreFactory.eINSTANCE.createEDataType => [
			name = "BarDataType"
		]
		fooPackage
	}

	def protected EPackageWithSubPackageForTests() {
		val mainPackage = EcoreFactory.eINSTANCE.createEPackage => [
			name = "mainpackage"
			nsPrefix = "mainpackage"
			nsURI = "http://mainpackage"
		]
		mainPackage.EClassifiers += EcoreFactory.eINSTANCE.createEClass => [
			name = "MainFooClass"
			EStructuralFeatures += EcoreFactory.eINSTANCE.createEAttribute => [
				name = "myAttribute"
			]
			EStructuralFeatures += EcoreFactory.eINSTANCE.createEReference => [
				name = "myReference"
			]
		]
		mainPackage.EClassifiers += EcoreFactory.eINSTANCE.createEDataType => [
			name = "MainFooDataType"
		]
		mainPackage.EClassifiers += EcoreFactory.eINSTANCE.createEEnum => [
			name = "MainFooEnum"
			ELiterals += EcoreFactory.eINSTANCE.createEEnumLiteral => [
				name = "FooEnumLiteral"
			]
		]
		mainPackage.EClassifiers += EcoreFactory.eINSTANCE.createEClass => [
			name = "MyClass" // this is present also in subpackages with the same name
			EStructuralFeatures += EcoreFactory.eINSTANCE.createEAttribute => [
				name = "myClassAttribute"
			]
		]
		mainPackage.ESubpackages += EcoreFactory.eINSTANCE.createEPackage => [
			name = "mainsubpackage"
			nsPrefix = "mainsubpackage"
			nsURI = "http://mainsubpackage"
			EClassifiers += EcoreFactory.eINSTANCE.createEClass => [
				name = "MainSubPackageFooClass"
				EStructuralFeatures += EcoreFactory.eINSTANCE.createEAttribute => [
					name = "mySubPackageAttribute"
				]
				EStructuralFeatures += EcoreFactory.eINSTANCE.createEReference => [
					name = "mySubPackageReference"
				]
			]
			EClassifiers += EcoreFactory.eINSTANCE.createEClass => [
				name = "MyClass" // this is present also in subpackages with the same name
				EStructuralFeatures += EcoreFactory.eINSTANCE.createEAttribute => [
					name = "myClassAttribute"
				]
			]
			ESubpackages += EcoreFactory.eINSTANCE.createEPackage => [
				name = "subsubpackage"
				nsPrefix = "subsubpackage"
				nsURI = "http://subsubpackage"
				EClassifiers += EcoreFactory.eINSTANCE.createEClass => [
					name = "MyClass" // this is present also in subpackages with the same name
				]
			]
		]
		mainPackage
	}

	def protected assertErrorsAsStrings(EObject o, CharSequence expected) {
		expected.toString.trim.assertEqualsStrings(
			o.validate.filter[severity == Severity.ERROR].
				map[message].sort.join("\n"))
	}

	def protected assertEqualsStrings(CharSequence expected, CharSequence actual) {
		expected.toString.replaceAll("\r", "").
			assertEquals(actual.toString.replaceAll("\r", ""))
	}

	def protected assertNamedElements(Iterable<? extends ENamedElement> elements, CharSequence expected) {
		expected.assertEqualsStrings(
			elements.map[name].join("\n") + "\n"
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

	def protected lastModifyEcoreOperation(EdeltaProgram p) {
		p.modifyEcoreOperations.last
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

	def protected getEStructuralFeatureByName(EClassifier e, String nameToSearch) {
		(e as EClass).EStructuralFeatures.findFirst[name == nameToSearch]
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
