package edelta.tests;

import static org.eclipse.xtext.xbase.lib.IterableExtensions.last;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import edelta.edelta.EdeltaFactory;
import edelta.edelta.EdeltaProgram;
import edelta.scoping.EdeltaOriginalENamedElementRecorder;
import edelta.tests.injectors.EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter.class)
public class EdeltaOriginalENamedElementRecorderTest extends EdeltaAbstractTest {
	@Inject
	private EdeltaOriginalENamedElementRecorder recorder;

	@Test
	public void testNull() throws Exception { // NOSONAR just check there's no NPE
		recorder.recordOriginalENamedElement(null);
	}

	@Test
	public void testNullENamedElement() throws Exception {
		var ref = EdeltaFactory.eINSTANCE.createEdeltaEcoreDirectReference();
		recorder.recordOriginalENamedElement(ref);
		assertNull(derivedStateHelper.getOriginalEnamedelement(ref));
	}

	@Test
	public void testUnresolvedENamedElement() throws Exception {
		var ref = ecoreReferenceExpression("ecoreref(NonExistant)").getReference();
		recorder.recordOriginalENamedElement(ref);
		assertNull(derivedStateHelper.getOriginalEnamedelement(ref));
	}

	@Test
	public void testEClassifierDirectReference() throws Exception {
		EdeltaProgram prog = parseWithTestEcore(
			"metamodel \"foo\"\n"
			+ "\n"
			+ "modifyEcore aTest epackage foo {\n"
			+ "   ecoreref(FooClass)\n"
			+ "}"
		);
		var ref = lastEcoreReferenceExpression(prog).getReference();
		recorder.recordOriginalENamedElement(ref);
		var original = getEClassiferByName(last(prog.getMetamodels()),
				"FooClass");
		assertSame(original, derivedStateHelper.getOriginalEnamedelement(ref));
	}

	@Test
	public void testEClassifierQualifiedReference() throws Exception {
		EdeltaProgram prog = parseWithTestEcore(
			"metamodel \"foo\"\n"
			+ "\n"
			+ "modifyEcore aTest epackage foo {\n"
			+ "   ecoreref(foo.FooClass)\n"
			+ "}"
		);
		var ref = getEdeltaEcoreQualifiedReference(
			lastEcoreReferenceExpression(prog).getReference());
		recorder.recordOriginalENamedElement(ref);
		var original = getEClassiferByName(last(prog.getMetamodels()),
				"FooClass");
		assertSame(original, derivedStateHelper.getOriginalEnamedelement(ref));
		var originalPackage = last(prog.getMetamodels());
		assertSame(originalPackage,
				derivedStateHelper.getOriginalEnamedelement(ref.getQualification()));
	}

	@Test
	public void testCreatedEClassifierDirectReference() throws Exception {
		EdeltaProgram prog = parseWithTestEcore(
			"metamodel \"foo\"\n"
			+ "\n"
			+ "modifyEcore aTest epackage foo {\n"
			+ "   addNewEClass(\"NewClass\")\n"
			+ "   ecoreref(NewClass)\n"
			+ "}"
		);
		var ref = lastEcoreReferenceExpression(prog).getReference();
		recorder.recordOriginalENamedElement(ref);
		assertNull(derivedStateHelper.getOriginalEnamedelement(ref));
	}

	@Test
	public void testCreatedEClassifierQualifiedReference() throws Exception {
		EdeltaProgram prog = parseWithTestEcore(
			"metamodel \"foo\"\n"
			+ "			\n"
			+ "			modifyEcore aTest epackage foo {\n"
			+ "				addNewEClass(\"NewClass\")\n"
			+ "				ecoreref(foo.NewClass)\n"
			+ "			}"
		);
		var ref = getEdeltaEcoreQualifiedReference(
				lastEcoreReferenceExpression(prog).getReference());
		recorder.recordOriginalENamedElement(ref);
		assertNull(derivedStateHelper.getOriginalEnamedelement(ref));
		// note that the package actually links to the original EPackage
		// not to the derived EPackage, but that's not a problem
		var originalPackage = last(prog.getMetamodels());
		assertSame(originalPackage,
			derivedStateHelper.getOriginalEnamedelement(
					ref.getQualification()));
	}
}
