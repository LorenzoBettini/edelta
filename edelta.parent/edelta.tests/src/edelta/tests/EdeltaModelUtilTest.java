package edelta.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.XIfExpression;
import org.junit.Test;
import org.junit.runner.RunWith;

import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.tests.injectors.EdeltaInjectorProviderCustom;
import edelta.util.EdeltaModelUtil;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
public class EdeltaModelUtilTest extends EdeltaAbstractTest {
	@Test
	public void testGetProgram() throws Exception {
		var prog = parseWithTestEcore(
			"metamodel \"foo\"\n"
			+ "\n"
			+ "modifyEcore aTest epackage foo {}");
		assertSame(prog, EdeltaModelUtil.getProgram(lastModifyEcoreOperation(prog)));
	}

	@Test
	public void testHasCycleInSuperPackageWithNoCycle() throws Exception {
		var ePackage = createEPackage("p", p -> 
			p.getESubpackages().add(createEPackage("p2", p2 ->
				p2.getESubpackages().add(createEPackage("p3"))
			))
		);
		assertFalse(
			EdeltaModelUtil.hasCycleInSuperPackage(
				ePackage.getESubpackages().get(0).getESubpackages().get(0)));
	}

	@Test
	public void testHasCycleInSuperPackageWithCycle() throws Exception {
		var ePackage = createEPackage("p", p -> 
			p.getESubpackages().add(createEPackage("p2", p2 ->
				p2.getESubpackages().add(createEPackage("p3"))
			))
		);
		var subSubPackage = ePackage.getESubpackages()
				.get(0).getESubpackages().get(0);
		// force the cycle
		subSubPackage.getESubpackages().add(ePackage);
		assertTrue(EdeltaModelUtil.hasCycleInSuperPackage(subSubPackage));
	}

	@Test
	public void testFindRootSuperPackage() throws Exception {
		var rootPackage = createEPackage("p", p -> 
			p.getESubpackages().add(createEPackage("p2", p2 ->
				p2.getESubpackages().add(createEPackage("p3"))
			))
		);
		assertThat(EdeltaModelUtil
			.findRootSuperPackage(
					rootPackage.getESubpackages().get(0).getESubpackages().get(0)))
				.isSameAs(rootPackage);
		assertThat(EdeltaModelUtil
			.findRootSuperPackage(rootPackage.getESubpackages().get(0)))
				.isSameAs(rootPackage);
		assertThat(EdeltaModelUtil
			.findRootSuperPackage(rootPackage)).isNull();
	}

	@Test
	public void testGetEcoreReferenceText() throws Exception {
		var prog = parseWithTestEcore(
			"metamodel \"foo\"\n"
			+ "\n"
			+ "modifyEcore aTest epackage foo {\n"
			+ "	ecoreref(FooClass)\n"
			+ "	ecoreref(foo.FooClass)\n"
			+ "	ecoreref(NonExistingClass)\n"
			+ "	ecoreref()\n"
			+ "}");
		var expressions = getLastModifyEcoreOperationBlock(prog).getExpressions();

		assertThat(expressions)
			.extracting(e -> EdeltaModelUtil
				.getEcoreReferenceText(getEdeltaEcoreReference(e)))
			.containsExactly("FooClass", "foo.FooClass", "NonExistingClass", "");
	}

	@Test
	public void testGetMetamodelImportText() throws Exception {
		var input =
			"metamodel \"foo\"\n"
			+ "metamodel \"bar\"\n"
			+ "metamodel \"foo\"";
		var prog = parseWithTestEcore(input);
		assertEquals("\"foo\"", EdeltaModelUtil.getMetamodelImportText(prog, 0));
		assertEquals("\"bar\"", EdeltaModelUtil.getMetamodelImportText(prog, 1));
		var node = EdeltaModelUtil
			.getMetamodelImportNodes(prog).get(1); // metamodel "bar"
		assertEquals(input.indexOf("\"bar\""), node.getOffset());
		assertEquals("\"bar\"".length(), node.getLength());
		assertEquals(input.indexOf("metamodel", 2),
			node.getPreviousSibling()
				.getPreviousSibling().getOffset()); // the second metamodel
	}

	@Test
	public void testHasCycleInHierarchy() throws Exception {
		var ecoreFactory = EcoreFactory.eINSTANCE;
		var c1 = ecoreFactory.createEClass();
		assertThat(EdeltaModelUtil.hasCycleInHierarchy(c1)).isFalse();
		var c2 = ecoreFactory.createEClass();
		c2.getESuperTypes().add(c1);
		assertThat(EdeltaModelUtil.hasCycleInHierarchy(c2)).isFalse();
		var c3 = ecoreFactory.createEClass();
		c3.getESuperTypes().add(c2);
		assertThat(EdeltaModelUtil.hasCycleInHierarchy(c3)).isFalse();
		var c4 = ecoreFactory.createEClass();
		c3.getESuperTypes().add(c4);
		assertThat(EdeltaModelUtil.hasCycleInHierarchy(c3)).isFalse();
		// cycle
		c1.getESuperTypes().add(c3);
		assertThat(EdeltaModelUtil.hasCycleInHierarchy(c4)).isFalse();
		assertThat(EdeltaModelUtil.hasCycleInHierarchy(c3)).isTrue();
		assertThat(EdeltaModelUtil.hasCycleInHierarchy(c2)).isTrue();
		assertThat(EdeltaModelUtil.hasCycleInHierarchy(c1)).isTrue();
	}

	@Test
	public void testGetContainingBlockXExpression() throws Exception {
		var input =
			"metamodel \"foo\"\n"
			+ "\n"
			+ "modifyEcore aTest epackage foo {\n"
			+ "	ecoreref(FooClass) // 0\n"
			+ "	ecoreref(FooClass).abstract = true // 1\n"
			+ "	ecoreref(FooClass).ESuperTypes += null // 2\n"
			+ "	if (true) {\n"
			+ "		ecoreref(FooClass).ESuperTypes += null // 3\n"
			+ "	}\n"
			+ "}";
		var prog = parseWithTestEcore(input);
		var mainBlock = getLastModifyEcoreOperationBlock(prog);
		var ecoreRefs = getAllEcoreReferenceExpressions(prog).stream()
			.map(EdeltaEcoreReferenceExpression::getReference)
			.collect(Collectors.toList());
		var ecoreRef = ecoreRefs.get(0);
		assertThat(EdeltaModelUtil.getContainingBlockXExpression(ecoreRef))
				.isSameAs(ecoreRef.eContainer());
		ecoreRef = ecoreRefs.get(1);
		assertThat(EdeltaModelUtil.getContainingBlockXExpression(ecoreRef))
				.isSameAs(mainBlock.getExpressions().get(1));
		ecoreRef = ecoreRefs.get(2);
		assertThat(EdeltaModelUtil.getContainingBlockXExpression(ecoreRef))
				.isSameAs(mainBlock.getExpressions().get(2));
		ecoreRef = ecoreRefs.get(3);
		assertThat(EdeltaModelUtil.getContainingBlockXExpression(ecoreRef))
			.isSameAs(
				getBlockFirstExpression(
					((XIfExpression) mainBlock.getExpressions().get(3)).getThen()));
	}
}
