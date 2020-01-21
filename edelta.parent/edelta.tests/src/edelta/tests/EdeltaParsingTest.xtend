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
		parse('''
			ecoreref foo
		''').
		lastExpression.
		edeltaEcoreReferenceExpression => [
			assertNotNull(reference.edeltaEcoreDirectReference.enamedelement)
		]
	}

	@Test
	def void testDirectEcoreReferenceIncomplete() {
		parse('''
			ecoreref 
		''').
		lastExpression.
		edeltaEcoreReferenceExpression => [
			assertNull(reference)
		]
	}

	@Test
	def void testDirectEcoreReferenceIncomplete2() {
		parse('''
			ecoreref (
		''').
		lastExpression.
		edeltaEcoreReferenceExpression => [
			assertNull(reference.edeltaEcoreDirectReference.enamedelement)
		]
	}

	@Test
	def void testQualifiedEcoreReference() {
		parse('''
			ecoreref foo.bar
		''').
		lastExpression.
		edeltaEcoreReferenceExpression.reference.edeltaEcoreQualifiedReference => [
			assertEquals("foo", qualification.textualRepresentation)
			assertEquals("bar", textualReferenceRepresentation)
			assertEquals("foo.bar", textualRepresentation)
		]
	}

	@Test
	def void testQualifiedEcoreReference2() {
		parse('''
			ecoreref foo.bar.baz
		''').
		lastExpression.
		edeltaEcoreReferenceExpression.reference.edeltaEcoreQualifiedReference => [
			assertEquals("foo.bar", qualification.textualRepresentation)
			assertEquals("baz", textualReferenceRepresentation)
			assertEquals("foo.bar.baz", textualRepresentation)
		]
	}

	@Test
	def void testQualifiedEcoreReference3() {
		parse('''
			ecoreref (foo.bar.baz)
		''').
		lastExpression.
		edeltaEcoreReferenceExpression.reference.edeltaEcoreQualifiedReference => [
			assertEquals("foo.bar", qualification.textualRepresentation)
			assertEquals("baz", textualReferenceRepresentation)
			assertEquals("foo.bar.baz", textualRepresentation)
		]
	}

	@Test
	def void testQualifiedEcoreReferenceIncomplete() {
		parse('''
			ecoreref foo.
		''').
		lastExpression.
		edeltaEcoreReferenceExpression.reference.edeltaEcoreQualifiedReference => [
			assertEquals("foo", qualification.textualRepresentation)
			assertNull(enamedelement)
			assertEquals("foo.", textualRepresentation)
		]
	}

	@Test
	def void testQualifiedEcoreReferenceIncomplete2() {
		parse('''
			ecoreref (foo.
		''').
		lastExpression.
		edeltaEcoreReferenceExpression.reference.edeltaEcoreQualifiedReference => [
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

	def private getTextualRepresentation(EObject o) {
		NodeModelUtils.getTokenText(NodeModelUtils.findActualNodeFor(o))
	}

	def private getTextualReferenceRepresentation(EObject o) {
		NodeModelUtils.getTokenText(
			NodeModelUtils.findNodesForFeature(o, EDELTA_ECORE_REFERENCE__ENAMEDELEMENT).head
		)
	}

}
