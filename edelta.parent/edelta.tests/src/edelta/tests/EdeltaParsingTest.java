package edelta.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaPackage;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProvider.class)
public class EdeltaParsingTest extends EdeltaAbstractTest {
	@Test
	public void testEmptyProgram() throws Exception {
		var result = parseHelper.parse("");
		assertNotNull(result);
	}

	@Test
	public void testSingleMetamodel() throws Exception {
		var result = parseHelper.parse("metamodel \"ecore\"");
		assertNotNull(result);
	}

	@Test
	public void testTwoMetamodels() throws Exception {
		var result = parseHelper.parse("""
			metamodel "ecore"
			metamodel "type"
		""");
		assertNotNull(result);
	}

	@Test
	public void testDirectEcoreReference() throws Exception {
		var exp = getEcoreReferenceExpression("foo");
		assertNotNull(
			getEdeltaEcoreDirectReference(exp.getReference())
				.getEnamedelement());
	}

	@Test
	public void testDirectEcoreReferenceIncomplete() throws Exception {
		var exp = getEcoreReferenceExpression("");
		assertNull(
			getEdeltaEcoreDirectReference(exp.getReference())
				.getEnamedelement());
	}

	@Test
	public void testQualifiedEcoreReference() throws Exception {
		var ref = getEdeltaEcoreQualifiedReference(
			getEcoreReferenceExpression("foo.bar").getReference());
		assertEquals("foo", getTextualRepresentation(ref.getQualification()));
		assertEquals("bar", getTextualReferenceRepresentation(ref));
		assertEquals("foo.bar", getTextualRepresentation(ref));
	}

	@Test
	public void testQualifiedEcoreReference2() throws Exception {
		var ref = getEdeltaEcoreQualifiedReference(
			getEcoreReferenceExpression("foo.bar.baz").getReference());
		assertEquals("foo.bar", getTextualRepresentation(ref.getQualification()));
		assertEquals("baz", getTextualReferenceRepresentation(ref));
		assertEquals("foo.bar.baz", getTextualRepresentation(ref));
	}

	@Test
	public void testQualifiedEcoreReferenceIncomplete() throws Exception {
		var ref = getEdeltaEcoreQualifiedReference(
			getEcoreReferenceExpression("foo.").getReference());
		assertEquals("foo", getTextualRepresentation(ref.getQualification()));
		assertNull(ref.getEnamedelement());
		assertEquals("foo.", getTextualRepresentation(ref));
	}

	@Test
	public void testEdeltaUseAsIncomplete() throws Exception {
		var use = parseHelper.parse("use").getUseAsClauses().get(0);
		assertNull(use.getType());
		assertNull(use.getName());
	}

	@Test
	public void testEdeltaUseAsIncompleteNoType() throws Exception {
		var use = parseHelper.parse("use as foo").getUseAsClauses().get(0);
		assertNull(use.getType());
		assertNotNull(use.getName());
	}

	@Test
	public void testEdeltaUseAsIncompleteNoName() throws Exception {
		var use = parseHelper.parse("use Foo as ").getUseAsClauses().get(0);
		assertNull(use.getName());
		assertNotNull(use.getType());
	}

	@Test
	public void testEdeltaUseAs() throws Exception {
		var use = parseHelper.parse("use Foo as foo").getUseAsClauses().get(0);
		assertNotNull(use.getType());
		assertNotNull(use.getName());
	}

	private EdeltaEcoreReferenceExpression getEcoreReferenceExpression(final CharSequence ecoreRefArg)
			throws Exception {
		return getEdeltaEcoreReferenceExpression(
			getLastModifyEcoreOperationLastExpression(
				parseHelper.parse(textForEcoreRef(ecoreRefArg))));
	}

	private String textForEcoreRef(final CharSequence ecoreRefArg) throws Exception {
		return "modifyEcore aTest epackage foo { ecoreref(" +
				ecoreRefArg;
	}

	private String getTextualRepresentation(final EObject o) {
		return NodeModelUtils.getTokenText(
				NodeModelUtils.findActualNodeFor(o));
	}

	private String getTextualReferenceRepresentation(final EObject o) {
		return NodeModelUtils.getTokenText(NodeModelUtils
				.findNodesForFeature(o,
					EdeltaPackage.Literals.EDELTA_ECORE_REFERENCE__ENAMEDELEMENT)
						.get(0));
	}
}
