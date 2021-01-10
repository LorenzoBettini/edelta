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
	def void testEmptyProgram() throws Exception {
		val result = parse('''
		''')
		Assert.assertNotNull(result)
	}

	@Test
	def void testSingleMetamodel() throws Exception {
		val result = parse('''
			metamodel "ecore"
		''')
		Assert.assertNotNull(result)
	}

	@Test
	def void testTwoMetamodels() throws Exception {
		val result = parse('''
			metamodel "ecore"
			metamodel "type"
		''')
		Assert.assertNotNull(result)
	}

	@Test
	def void testDirectEcoreReference() throws Exception {
		getEcoreReferenceExpression("foo") => [
			assertNotNull(reference.edeltaEcoreDirectReference.enamedelement)
		]
	}

	@Test
	def void testDirectEcoreReferenceIncomplete() throws Exception {
		getEcoreReferenceExpression("") => [
			assertNull(reference.edeltaEcoreDirectReference.enamedelement)
		]
	}

	@Test
	def void testQualifiedEcoreReference() throws Exception {
		getEcoreReferenceExpression("foo.bar")
		.reference.edeltaEcoreQualifiedReference => [
			assertEquals("foo", qualification.textualRepresentation)
			assertEquals("bar", textualReferenceRepresentation)
			assertEquals("foo.bar", textualRepresentation)
		]
	}

	@Test
	def void testQualifiedEcoreReference2() throws Exception {
		getEcoreReferenceExpression("foo.bar.baz")
		.reference.edeltaEcoreQualifiedReference => [
			assertEquals("foo.bar", qualification.textualRepresentation)
			assertEquals("baz", textualReferenceRepresentation)
			assertEquals("foo.bar.baz", textualRepresentation)
		]
	}

	@Test
	def void testQualifiedEcoreReferenceIncomplete() throws Exception {
		getEcoreReferenceExpression("foo.")
		.reference.edeltaEcoreQualifiedReference => [
			assertEquals("foo", qualification.textualRepresentation)
			assertNull(enamedelement)
			assertEquals("foo.", textualRepresentation)
		]
	}

	@Test
	def void testEdeltaUseAsIncomplete() throws Exception {
		'''
		use
		'''.parse.useAsClauses.head => [
			assertNull(type)
			assertNull(name)
		]
	}

	@Test
	def void testEdeltaUseAsIncompleteNoType() throws Exception {
		'''
		use as foo
		'''.parse.useAsClauses.head => [
			assertNull(type)
			assertNotNull(name)
		]
	}

	@Test
	def void testEdeltaUseAsIncompleteNoName() throws Exception {
		'''
		use Foo as 
		'''.parse.useAsClauses.head => [
			assertNotNull(type)
			assertNull(name)
		]
	}

	@Test
	def void testEdeltaUseAs() throws Exception {
		'''
		use Foo as foo
		'''.parse.useAsClauses.head => [
			assertNotNull(type)
			assertNotNull(name)
		]
	}

	def private getEcoreReferenceExpression(CharSequence ecoreRefArg) throws Exception {
		textForEcoreRef(ecoreRefArg)
			.parse
			.lastModifyEcoreOperation
			.body
			.block
			.expressions
			.last
			.edeltaEcoreReferenceExpression
	}

	def private textForEcoreRef(CharSequence ecoreRefArg) throws Exception {
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
