package edelta.tests

import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.nodemodel.util.NodeModelUtils
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

import static edelta.edelta.EdeltaPackage.Literals.*
import static org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProvider)
class EdeltaParsingTest extends EdeltaAbstractTest {

	@Test
	def void testEmptyProgram() {
		val result = parse('''
		''')
		Assert.assertNotNull(result)
	}

	@Test
	def void testSingleMetamodel() {
		val result = parse('''
			metamodel "ecore"
		''')
		Assert.assertNotNull(result)
	}

	@Test
	def void testTwoMetamodels() {
		val result = parse('''
			metamodel "ecore"
			metamodel "type"
		''')
		Assert.assertNotNull(result)
	}

	@Test
	def void testDirectEcoreReference() {
		getEcoreReferenceExpression("foo") => [
			assertNotNull(reference.edeltaEcoreDirectReference.enamedelement)
		]
	}

	@Test
	def void testDirectEcoreReferenceIncomplete() {
		getEcoreReferenceExpression("") => [
			assertNull(reference.edeltaEcoreDirectReference.enamedelement)
		]
	}

	@Test
	def void testQualifiedEcoreReference() {
		getEcoreReferenceExpression("foo.bar")
		.reference.edeltaEcoreQualifiedReference => [
			assertEquals("foo", qualification.textualRepresentation)
			assertEquals("bar", textualReferenceRepresentation)
			assertEquals("foo.bar", textualRepresentation)
		]
	}

	@Test
	def void testQualifiedEcoreReference2() {
		getEcoreReferenceExpression("foo.bar.baz")
		.reference.edeltaEcoreQualifiedReference => [
			assertEquals("foo.bar", qualification.textualRepresentation)
			assertEquals("baz", textualReferenceRepresentation)
			assertEquals("foo.bar.baz", textualRepresentation)
		]
	}

	@Test
	def void testQualifiedEcoreReferenceIncomplete() {
		getEcoreReferenceExpression("foo.")
		.reference.edeltaEcoreQualifiedReference => [
			assertEquals("foo", qualification.textualRepresentation)
			assertNull(enamedelement)
			assertEquals("foo.", textualRepresentation)
		]
	}

	@Test
	def void testEdeltaUseAsIncomplete() {
		'''
		use
		'''.parse.useAsClauses.head => [
			assertNull(type)
			assertNull(name)
		]
	}

	@Test
	def void testEdeltaUseAsIncompleteNoType() {
		'''
		use as foo
		'''.parse.useAsClauses.head => [
			assertNull(type)
			assertNotNull(name)
		]
	}

	@Test
	def void testEdeltaUseAsIncompleteNoName() {
		'''
		use Foo as 
		'''.parse.useAsClauses.head => [
			assertNotNull(type)
			assertNull(name)
		]
	}

	@Test
	def void testEdeltaUseAs() {
		'''
		use Foo as foo
		'''.parse.useAsClauses.head => [
			assertNotNull(type)
			assertNotNull(name)
		]
	}

	def private getEcoreReferenceExpression(CharSequence ecoreRefArg) {
		textForEcoreRef(ecoreRefArg)
			.parse
			.lastModifyEcoreOperation
			.body
			.block
			.expressions
			.last
			.edeltaEcoreReferenceExpression
	}

	def private textForEcoreRef(CharSequence ecoreRefArg) {
		'''
		modifyEcore aTest epackage foo {
			ecoreref(«ecoreRefArg»
		'''
	}

	def private getTextualRepresentation(EObject o) {
		NodeModelUtils.getTokenText(NodeModelUtils.findActualNodeFor(o))
	}

	def private getTextualReferenceRepresentation(EObject o) {
		NodeModelUtils.getTokenText(
			NodeModelUtils.findNodesForFeature(o, EDELTA_ECORE_REFERENCE__ENAMEDELEMENT).head
		)
	}

}
