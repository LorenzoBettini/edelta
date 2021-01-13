package edelta.tests;

import com.google.inject.Inject;
import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaProgram;
import edelta.tests.injectors.EdeltaInjectorProviderCustom;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(InjectionExtension.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
@SuppressWarnings("all")
public class EdeltaTypeComputerTest extends EdeltaAbstractTest {
	@Inject
	private IBatchTypeResolver typeResolver;

	@ParameterizedTest
	@CsvSource({
		"foo, EPackage",
		"FooClass, EClass",
		"FooDataType, EDataType",
		"FooEnum, EEnum",
		"myAttribute, EAttribute",
		"myReference, EReference",
		"FooEnumLiteral, EEnumLiteral",
		"NonExistant, ENamedElement"
	})
	public void testTypeOfEcoreReference(String ecoreRefArg, String expectedType) throws Exception {
		assertType(
			"ecoreref(" + ecoreRefArg + ")",
			"org.eclipse.emf.ecore." + expectedType
		);
	}

	@ParameterizedTest
	@CsvSource({
		"'val org.eclipse.emf.ecore.EClass c = ecoreref(NonExistant)', EClass",
		"'val org.eclipse.emf.ecore.EClass c = ecoreref(FooClass.NonExistant)', EClass",
		"'val Object c = ecoreref(NonExistant)', ENamedElement"
	})
	public void testTypeOfEcoreReferenceWithExpectation(String input, String expectedType) throws Exception {
		assertTypeOfRightExpression(
			input,
			"org.eclipse.emf.ecore." + expectedType
		);
	}

	@Test
	public void testTypeOfReferenceToNullNamedElement() throws Exception {
		assertENamedElement("ecoreref");
	}

	@Test
	public void testTypeOfReferenceToNullNamedElement2() throws Exception {
		assertENamedElement("ecoreref()");
	}

	@Test
	public void testTypeForRenamedEClassInModifyEcore() throws Exception {
		var prog = parseWithTestEcore(
			"metamodel \"foo\"\n"
			+ "\n"
			+ "modifyEcore aTest epackage foo {\n"
			+ "	ecoreref(foo.FooClass).name = \"RenamedClass\"\n"
			+ "	ecoreref(RenamedClass)\n"
			+ "}"
		);
		var ecoreRefExp = getEdeltaEcoreReferenceExpression(
				getBlockLastExpression(lastModifyEcoreOperation(prog).getBody()));
		assertEquals(EClass.class.getCanonicalName(),
			typeResolver.resolveTypes(ecoreRefExp)
				.getActualType(ecoreRefExp).getIdentifier());
	}

	@Test
	public void testTypeForRenamedQualifiedEClassInModifyEcore() throws Exception {
		var prog = parseWithTestEcore(
			"metamodel \"foo\"\n"
			+ "\n"
			+ "modifyEcore aTest epackage foo {\n"
			+ "	ecoreref(foo.FooClass).name = \"RenamedClass\"\n"
			+ "	ecoreref(foo.RenamedClass)\n"
			+ "}"
		);
		var ecoreRefExp = getEdeltaEcoreReferenceExpression(
				getBlockLastExpression(lastModifyEcoreOperation(prog).getBody()));
		assertEquals(EClass.class.getCanonicalName(),
			typeResolver.resolveTypes(ecoreRefExp)
				.getActualType(ecoreRefExp).getIdentifier());
	}

	private void assertType(CharSequence input, String expectedTypeFQN) throws Exception {
		var ecoreRefExp = ecoreReferenceExpression(input);
		assertEquals(expectedTypeFQN,
			typeResolver.resolveTypes(ecoreRefExp)
				.getActualType(ecoreRefExp).getIdentifier());
	}

	private void assertENamedElement(CharSequence input) throws Exception {
		assertType(input, "org.eclipse.emf.ecore.ENamedElement");
	}

	private void assertTypeOfRightExpression(CharSequence input, String expectedTypeFQN) throws Exception {
		var blockLastExpression = getBlockLastExpression(lastModifyEcoreOperation(
			parseWithTestEcore(
				String.format(
					"metamodel \"foo\"\n"
					+ "modifyEcore aTest epackage foo {\n"
					+ "   %s\n"
					+ "}"
				, input))).getBody());
		Assertions.assertEquals(expectedTypeFQN,
			typeResolver.resolveTypes(blockLastExpression)
				.getActualType(getVariableDeclaration(blockLastExpression).getRight())
					.getIdentifier());
	}
}
