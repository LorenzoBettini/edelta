package edelta.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import edelta.navigation.EdeltaNavigationTargetHelper;
import edelta.tests.injectors.EdeltaInjectorProviderCustom;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
public class EdeltaNavigationTargetHelperTest extends EdeltaAbstractTest {
	@Inject
	private EdeltaNavigationTargetHelper navigationTargetHelper;

	@Test
	public void testNotEdeltaEcoreReference() throws Exception {
		assertThat(navigationTargetHelper
			.getTarget(EcoreFactory.eINSTANCE.createEClass()))
			.isNull();
	}

	@Test
	public void testTargetInTheImportedMetamodel() throws Exception {
		var prog = parseWithTestEcore(
			"metamodel \"foo\"\n"
			+ "\n"
			+ "modifyEcore aTest epackage foo {\n"
			+ "   ecoreref(FooClass)\n"
			+ "}");
		var target = navigationTargetHelper
				.getTarget(getFirstOfAllEcoreReferenceExpressions(prog).getReference());
		var original = getEClassiferByName(
				prog.getMetamodels().get(0), "FooClass");
		assertThat(target)
			.isNotNull().isSameAs(original);
	}

	@Test
	public void testTargetAsXExpression() throws Exception {
		var prog = parseWithTestEcore(
			"metamodel \"foo\"\n"
			+ "\n"
			+ "modifyEcore aTest epackage foo {\n"
			+ "	addNewEClass(\"NewClass\")\n"
			+ "	ecoreref(NewClass)\n"
			+ "}");
		var target = navigationTargetHelper
				.getTarget(getFirstOfAllEcoreReferenceExpressions(prog).getReference());
		var exp = getLastModifyEcoreOperationFirstExpression(prog);
		assertThat(target)
			.isNotNull().isSameAs(exp);
	}

	@Test
	public void testTargetOfForwardReference() throws Exception {
		var prog = parseWithTestEcore(
			"metamodel \"foo\"\n"
			+ "\n"
			+ "modifyEcore aTest epackage foo {\n"
			+ "	ecoreref(NewClass)\n"
			+ "	addNewEClass(\"NewClass\")\n"
			+ "}");
		var target = navigationTargetHelper
				.getTarget(getFirstOfAllEcoreReferenceExpressions(prog).getReference());
		var exp = getLastModifyEcoreOperationLastExpression(prog);
		assertThat(target)
			.isNotNull().isSameAs(exp);
	}
}
