package edelta.tests

import com.google.inject.Inject
import com.google.inject.Provider
import edelta.edelta.EdeltaEcoreChangeEClassExpression
import edelta.edelta.EdeltaEcoreCreateEAttributeExpression
import edelta.edelta.EdeltaEcoreCreateEClassExpression
import edelta.edelta.EdeltaEcoreDirectReference
import edelta.edelta.EdeltaEcoreQualifiedReference
import edelta.edelta.EdeltaEcoreReferenceExpression
import edelta.edelta.EdeltaProgram
import edelta.tests.input.Inputs
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.ENamedElement
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.emf.ecore.xmi.XMIResource
import org.eclipse.xtext.resource.XtextResourceSet
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.testing.util.ParseHelper
import org.eclipse.xtext.testing.validation.ValidationTestHelper
import org.eclipse.xtext.xbase.XExpression
import org.junit.runner.RunWith

import static extension org.junit.Assert.*
import edelta.edelta.EdeltaEcoreBaseEClassManipulationWithBlockExpression
import org.eclipse.xtext.xbase.XVariableDeclaration

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProvider)
abstract class EdeltaAbstractTest {

	@Inject
	Provider<XtextResourceSet> resourceSetProvider

	@Inject protected extension ParseHelper<EdeltaProgram>
	@Inject protected extension ValidationTestHelper
	protected extension Inputs = new Inputs

	def protected parseWithTestEcore(CharSequence input) {
		input.parse(resourceSetWithTestEcore)
	}

	def protected parseWithTestEcores(CharSequence input) {
		input.parse(resourceSetWithTestEcores)
	}

	def protected resourceSetWithTestEcore() {
		val resourceSet = resourceSetProvider.get
		addEPackageForTests(resourceSet)
	}

	def protected addEPackageForTests(ResourceSet resourceSet) {
		val resource = resourceSet.createResource(URI.createURI("foo.ecore"))
		resource.contents += EPackageForTests
		resourceSet
	}

	def protected resourceSetWithTestEcores() {
		val resourceSet = resourceSetWithTestEcore
		addEPackageForTests2(resourceSet)
	}

	def protected addEPackageForTests2(ResourceSet resourceSet) {
		val resource = resourceSet.createResource(URI.createURI("bar.ecore"))
		resource.contents += EPackageForTests2
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
		val derivedEPackage = context.eResource.contents.last as EPackage
		derivedEPackage.EClassifiers.last as EClass
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
