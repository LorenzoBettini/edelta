package edelta.tests;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import edelta.edelta.EdeltaEcoreArgument;
import edelta.edelta.EdeltaEcoreQualifiedArgument;
import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaEcoreSimpleArgument;
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
			getEdeltaEcoreSimpleArgument(exp.getArgument())
				.getElement());
	}

	@Test
	public void testDirectEcoreReferenceIncomplete() throws Exception {
		var exp = getEcoreReferenceExpression("");
		assertNull(
			getEdeltaEcoreSimpleArgument(exp.getArgument())
				.getElement());
	}

	@Test
	public void testQualifiedEcoreReference() throws Exception {
		var prog = parseHelper.parse("""
			modifyEcore aTest epackage foo {
				ecoreref(foo.bar)
			}
		""");
		var arg = getLastEcoreReferenceExpression(prog).getArgument();
		assertThat(arg).asInstanceOf(type(EdeltaEcoreQualifiedArgument.class))
			.extracting(EdeltaEcoreQualifiedArgument::getQualification)
				.isInstanceOf(EdeltaEcoreSimpleArgument.class)
			.extracting(EdeltaEcoreArgument::getElement)
				.isInstanceOf(ENamedElement.class);
	}

	@Test
	public void testQualifiedEcoreReference1() throws Exception {
		var prog = parseHelper.parse("""
			modifyEcore aTest epackage foo {
				ecoreref(foo.MyEClass.myEAttribute)
			}
		""");
		var arg = getLastEcoreReferenceExpression(prog).getArgument();
		assertThat(arg).asInstanceOf(type(EdeltaEcoreQualifiedArgument.class))
			.satisfies(qualifiedArg -> {
				assertThat(NodeModelUtils.findActualNodeFor(qualifiedArg).getText())
					.isEqualTo("foo.MyEClass.myEAttribute");
				var qualification = qualifiedArg.getQualification();
				assertThat(NodeModelUtils.findActualNodeFor(qualification).getText())
					.isEqualTo("foo.MyEClass");
				assertThat(qualification).asInstanceOf(type(EdeltaEcoreQualifiedArgument.class))
					.satisfies(q -> {
						var innerQualification = q.getQualification();
						assertThat(NodeModelUtils.findActualNodeFor(innerQualification).getText())
							.isEqualTo("foo");
						assertThat(innerQualification).isInstanceOf(EdeltaEcoreSimpleArgument.class);
					});
			});
	}

	@Test
	public void testQualifiedEcoreReference2() throws Exception {
		var prog = parseHelper.parse("""
			modifyEcore aTest epackage foo {
				ecoreref(foo.bar)
			}
		""");
		var arg = (EdeltaEcoreQualifiedArgument) getLastEcoreReferenceExpression(prog).getArgument();
		assertEquals("foo", getTextualRepresentation(arg.getQualification()));
		assertEquals("bar", getTextualRepresentationForElement(arg));
		assertEquals("foo.bar", getTextualRepresentation(arg));
	}

	@Test
	public void testQualifiedEcoreReference3() throws Exception {
		var arg = getEdeltaEcoreQualifiedArgument(
			getEcoreReferenceExpression("foo.bar.baz").getArgument());
		assertEquals("foo.bar", getTextualRepresentation(arg.getQualification()));
		assertEquals("baz", getTextualRepresentationForElement(arg));
		assertEquals("foo.bar.baz", getTextualRepresentation(arg));
	}

	@Test
	public void testQualifiedEcoreReferenceIncomplete() throws Exception {
		var ref = getEdeltaEcoreQualifiedArgument(
			getEcoreReferenceExpression("foo.").getArgument());
		assertEquals("foo", getTextualRepresentation(ref.getQualification()));
		assertNull(ref.getElement());
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

	private EdeltaEcoreReferenceExpression getEcoreReferenceExpression(CharSequence ecoreRefArg)
			throws Exception {
		return getEdeltaEcoreReferenceExpression(
			getLastModifyEcoreOperationLastExpression(
				parseHelper.parse(textForEcoreRef(ecoreRefArg))));
	}

	private String textForEcoreRef(CharSequence ecoreRefArg) {
		return "modifyEcore aTest epackage foo { ecoreref(" +
				ecoreRefArg;
	}

	private String getTextualRepresentation(EObject o) {
		return NodeModelUtils.getTokenText(
				NodeModelUtils.findActualNodeFor(o));
	}

	private String getTextualRepresentationForElement(EObject o) {
		return NodeModelUtils.getTokenText(NodeModelUtils
				.findNodesForFeature(o,
					EdeltaPackage.Literals.EDELTA_ECORE_ARGUMENT__ELEMENT)
						.get(0));
	}
}
