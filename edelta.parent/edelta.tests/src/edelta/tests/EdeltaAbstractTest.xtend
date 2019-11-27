package edelta.tests

import com.google.inject.Inject
import com.google.inject.Provider
import edelta.edelta.EdeltaEcoreBaseEClassManipulationWithBlockExpression
import edelta.edelta.EdeltaEcoreChangeEClassExpression
import edelta.edelta.EdeltaEcoreCreateEAttributeExpression
import edelta.edelta.EdeltaEcoreCreateEClassExpression
import edelta.edelta.EdeltaEcoreDirectReference
import edelta.edelta.EdeltaEcoreQualifiedReference
import edelta.edelta.EdeltaEcoreReferenceExpression
import edelta.edelta.EdeltaProgram
import edelta.interpreter.IEdeltaInterpreter
import edelta.resource.EdeltaDerivedStateEPackage
import edelta.tests.input.Inputs
import java.nio.file.Paths
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
import org.eclipse.xtext.common.types.JvmGenericType
import org.eclipse.xtext.diagnostics.Severity
import org.eclipse.xtext.resource.XtextResourceSet
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.testing.util.ParseHelper
import org.eclipse.xtext.testing.validation.ValidationTestHelper
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.XVariableDeclaration
import org.eclipse.xtext.xbase.interpreter.impl.DefaultEvaluationResult
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
	@Inject extension IJvmModelAssociations

	protected extension Inputs = new Inputs

	protected static String ECORE_PATH = "src/edelta/tests/input/models/EcoreForTests.ecore"
	protected static String PERSON_LIST_ECORE = "PersonList.ecore"
	protected static String PERSON_LIST_ECORE_PATH = "src/edelta/tests/input/models/" + PERSON_LIST_ECORE

	def protected parseWithTestEcore(CharSequence input) {
		input.parse(resourceSetWithTestEcore)
	}

	def protected parseWithTestEcores(CharSequence input) {
		input.parse(resourceSetWithTestEcores)
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

	def protected addEPackageForTests(ResourceSet resourceSet) {
		resourceSet.createTestResource("foo", EPackageForTests)
	}

	def protected resourceSetWithTestEcores() {
		val resourceSet = resourceSetWithTestEcore
		addEPackageForTests2(resourceSet)
	}

	def protected addEPackageForTests2(ResourceSet resourceSet) {
		resourceSet.createTestResource("bar.", EPackageForTests2)
	}

	def protected createTestResource(ResourceSet resourceSet, String ecoreName, EPackage epackage) {
		val resource = resourceSet.createResource(URI.createURI(ecoreName + ".ecore"))
		resource.contents += epackage
		resourceSet
	}

	def protected EPackageForTests() {
		// if you add something to this ecore, which is created on the fly,
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

	def protected assertAfterInterpretationOfEdeltaManipulationExpression(IEdeltaInterpreter interpreter, EdeltaProgram program, boolean doValidate, (EClass)=>void testExecutor) {
		program.lastExpression.getManipulationEClassExpression => [
			// mimic the behavior of derived state computer that runs the interpreter
			// on a copied EPackage, not on the original one
			val packages = program.getCopiedEPackages.toList
			val eclass = packages.head.EClassifiers.head as EClass
			val inferredJavaClass = program.jvmElements.filter(JvmGenericType).head
			val result = interpreter.run(it, eclass, inferredJavaClass, packages)
			// result can be null due to a timeout
			if (result?.exception !== null)
				throw result.exception
			testExecutor.apply(eclass)
			if (result !== null)
				assertTrue(
					"not expected result of type " + result.class.name,
					result instanceof DefaultEvaluationResult
				)
		]
	}


	def protected getEPackageByName(EdeltaProgram context, String packagename) {
		context.eResource.resourceSet.resources.filter(XMIResource).
			map[contents.head as EPackage].findFirst[name == packagename]
	}

	def protected getEClassifierByName(EdeltaProgram context, String packagename, String classifiername) {
		getEPackageByName(context, packagename).EClassifiers.
			findFirst[name == classifiername]
	}

	def protected lastExpression(EdeltaProgram p) {
		p.main.expressions.last
	}

	def protected getCreateEClassExpression(XExpression e) {
		e as EdeltaEcoreCreateEClassExpression
	}

	def protected getChangeEClassExpression(XExpression e) {
		e as EdeltaEcoreChangeEClassExpression
	}

	def protected getManipulationEClassExpression(XExpression e) {
		e as EdeltaEcoreBaseEClassManipulationWithBlockExpression
	}

	def protected getCreateEAttributExpression(XExpression e) {
		e as EdeltaEcoreCreateEAttributeExpression
	}

	def protected getDerivedStateLastEClass(EObject context) {
		val derivedEPackage = getDerivedStateLastEPackage(context)
		derivedEPackage.EClassifiers.last as EClass
	}

	protected def EdeltaDerivedStateEPackage getDerivedStateLastEPackage(EObject context) {
		context.eResource.contents.last as EdeltaDerivedStateEPackage
	}

	def protected getCopiedEClass(EObject context, String nameToSearch) {
		val p = getLastCopiedEPackage(context)
		getCopiedEClasses(p).findFirst[name == nameToSearch]
	}

	protected def getCopiedEClasses(EPackage p) {
		p.EClassifiers.filter(EClass)
	}

	def protected getLastCopiedEPackage(EObject context) {
		getCopiedEPackages(context).last
	}

	def protected getCopiedEPackages(EObject context) {
		context.eResource.contents.filter(EPackage).filter[!(it instanceof EdeltaDerivedStateEPackage)]
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

	def protected getEdeltaEcoreDirectReference(EObject e) {
		e as EdeltaEcoreDirectReference
	}

	def protected getEdeltaEcoreQualifiedReference(EObject e) {
		e as EdeltaEcoreQualifiedReference
	}

	def protected getVariableDeclaration(XExpression e) {
		e as XVariableDeclaration
	}
}
