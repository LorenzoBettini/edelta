package edelta.tests;

import static org.eclipse.xtext.xbase.lib.IterableExtensions.lastOrNull;
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
	public void testNull() { // NOSONAR just check there's no NPE
		recorder.recordOriginalENamedElement(null);
	}

	@Test
	public void testNullENamedElement() {
		var ref = EdeltaFactory.eINSTANCE.createEdeltaEcoreSimpleArgument();
		recorder.recordOriginalENamedElement(ref);
		assertNull(derivedStateHelper.getOriginalEnamedelement(ref));
	}

	@Test
	public void testUnresolvedENamedElement() throws Exception {
		var ref = ecoreReferenceExpression("ecoreref(NonExistant)").getArgument();
		recorder.recordOriginalENamedElement(ref);
		assertNull(derivedStateHelper.getOriginalEnamedelement(ref));
	}

	@Test
	public void testEClassifierDirectReference() throws Exception {
		EdeltaProgram prog = parseWithTestEcore(
			"""
				metamodel "foo"
				
				modifyEcore aTest epackage foo {
				   ecoreref(FooClass)
				}
			"""
		);
		var ref = getLastEcoreReferenceExpression(prog).getArgument();
		recorder.recordOriginalENamedElement(ref);
		var original = getEClassiferByName(lastOrNull(prog.getEPackages()),
				"FooClass");
		assertSame(original, derivedStateHelper.getOriginalEnamedelement(ref));
	}

	@Test
	public void testEClassifierQualifiedReference() throws Exception {
		EdeltaProgram prog = parseWithTestEcore(
			"""
				metamodel "foo"
				
				modifyEcore aTest epackage foo {
				   ecoreref(foo.FooClass)
				}
			"""
		);
		var ref = getEdeltaEcoreQualifiedArgument(
			getLastEcoreReferenceExpression(prog).getArgument());
		recorder.recordOriginalENamedElement(ref);
		var original = getEClassiferByName(lastOrNull(prog.getEPackages()),
				"FooClass");
		assertSame(original, derivedStateHelper.getOriginalEnamedelement(ref));
		var originalPackage = lastOrNull(prog.getEPackages());
		assertSame(originalPackage,
				derivedStateHelper.getOriginalEnamedelement(ref.getQualification()));
	}

	@Test
	public void testCreatedEClassifierDirectReference() throws Exception {
		EdeltaProgram prog = parseWithTestEcore(
			"""
				metamodel "foo"
				
				modifyEcore aTest epackage foo {
				   addNewEClass("NewClass")
				   ecoreref(NewClass)
				}
			"""
		);
		var ref = getLastEcoreReferenceExpression(prog).getArgument();
		recorder.recordOriginalENamedElement(ref);
		assertNull(derivedStateHelper.getOriginalEnamedelement(ref));
	}

	@Test
	public void testCreatedEClassifierQualifiedReference() throws Exception {
		EdeltaProgram prog = parseWithTestEcore(
			"""
				metamodel "foo"
				
				modifyEcore aTest epackage foo {
					addNewEClass("NewClass")
					ecoreref(foo.NewClass)
				}
			"""
		);
		var ref = getEdeltaEcoreQualifiedArgument(
				getLastEcoreReferenceExpression(prog).getArgument());
		recorder.recordOriginalENamedElement(ref);
		assertNull(derivedStateHelper.getOriginalEnamedelement(ref));
		// note that the package actually links to the original EPackage
		// not to the derived EPackage, but that's not a problem
		var originalPackage = lastOrNull(prog.getEPackages());
		assertSame(originalPackage,
			derivedStateHelper.getOriginalEnamedelement(
					ref.getQualification()));
	}
}
