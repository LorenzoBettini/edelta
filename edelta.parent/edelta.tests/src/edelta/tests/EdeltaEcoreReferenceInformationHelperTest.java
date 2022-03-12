package edelta.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaProgram;
import edelta.resource.derivedstate.EdeltaEcoreReferenceState;
import edelta.resource.derivedstate.EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation;
import edelta.tests.injectors.EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter;
import edelta.util.EdeltaEcoreReferenceInformationHelper;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter.class)
public class EdeltaEcoreReferenceInformationHelperTest extends EdeltaAbstractTest {
	@Inject
	private EdeltaEcoreReferenceInformationHelper informationHelper;

	@Test
	public void testWhenAlreadySetThenReturnsTheStoredInformation() throws Exception {
		EdeltaEcoreReferenceExpression ref =
			lastEcoreRef(parseWithTestEcore("""
				metamodel "foo"

				modifyEcore aTest epackage foo {
					ecoreref(foo)
				}
				"""));
		final EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation info1 = informationHelper
				.getOrComputeInformation(ref);
		final EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation info2 = informationHelper
				.getOrComputeInformation(ref);
		assertThat(info1).isNotNull();
		assertThat(info2).isNotNull();
		assertThat(info1).isSameAs(info2);
	}

	@Test
	public void testReferenceToEPackage() throws Exception {
		var ref =
			lastEcoreRef(parseWithTestEcore("""
				metamodel "foo"

				modifyEcore aTest epackage foo {
					ecoreref(foo)
				}
				"""));
		var info = informationHelper
				.getOrComputeInformation(ref);
		assertThat(info)
			.returns("EPackage", EdeltaEcoreReferenceStateInformation::getType)
			.returns("foo", EdeltaEcoreReferenceStateInformation::getEPackageName)
			.returns(null, EdeltaEcoreReferenceStateInformation::getEClassifierName)
			.returns(null, EdeltaEcoreReferenceStateInformation::getENamedElementName);
	}

	@Test
	public void testReferenceToSubPackage() throws Exception {
		var ref =
			lastEcoreRef(parseWithTestEcoreWithSubPackage("""
				metamodel "mainpackage"

				modifyEcore aTest epackage mainpackage {
					ecoreref(mainsubpackage)
				}
				"""));
		var info = informationHelper
				.getOrComputeInformation(ref);
		assertThat(info)
			.returns("EPackage", EdeltaEcoreReferenceStateInformation::getType)
			.returns("mainpackage.mainsubpackage", EdeltaEcoreReferenceStateInformation::getEPackageName)
			.returns(null, EdeltaEcoreReferenceStateInformation::getEClassifierName)
			.returns(null, EdeltaEcoreReferenceStateInformation::getENamedElementName);
	}

	@Test
	public void testReferenceToEStructuralFeatureWithSubPackage() throws Exception {
		var ref =
			lastEcoreRef(parseWithTestEcoreWithSubPackage("""
				metamodel "mainpackage"

				modifyEcore aTest epackage mainpackage {
					ecoreref(mySubPackageAttribute)
				}
				"""));
		var info = informationHelper
				.getOrComputeInformation(ref);
		assertThat(info)
			.returns("EAttribute", EdeltaEcoreReferenceStateInformation::getType)
			.returns("mainpackage.mainsubpackage", EdeltaEcoreReferenceStateInformation::getEPackageName)
			.returns("MainSubPackageFooClass", EdeltaEcoreReferenceStateInformation::getEClassifierName)
			.returns("mySubPackageAttribute", EdeltaEcoreReferenceStateInformation::getENamedElementName);
	}

	@Test
	public void testReferenceToSubPackageWithCycle() throws Exception {
		var ref =
			lastEcoreRef(parseWithTestEcoreWithSubPackage("""
				metamodel "mainpackage"

				modifyEcore aTest epackage mainpackage {
					ecoreref(subsubpackage)
				}
				"""));
		// create cycle in sub package relation
		var subpackage = ((EPackage) ref.getReference().getEnamedelement());
		subpackage.getESubpackages().add(subpackage.getESuperPackage());

		var info = informationHelper
				.getOrComputeInformation(ref);
		// due to the cycle the parent relation is not visited
		assertThat(info)
			.returns("EPackage", EdeltaEcoreReferenceStateInformation::getType)
			.returns("subsubpackage", EdeltaEcoreReferenceStateInformation::getEPackageName)
			.returns(null, EdeltaEcoreReferenceStateInformation::getEClassifierName)
			.returns(null, EdeltaEcoreReferenceStateInformation::getENamedElementName);
	}

	@Test
	public void testReferenceToEClassifier() throws Exception {
		var ref =
			lastEcoreRef(parseWithTestEcore("""
				metamodel "foo"

				modifyEcore aTest epackage foo {
					ecoreref(FooClass)
				}
				"""));
		var info = informationHelper
				.getOrComputeInformation(ref);
		assertThat(info)
			.returns("EClass", EdeltaEcoreReferenceStateInformation::getType)
			.returns("foo", EdeltaEcoreReferenceStateInformation::getEPackageName)
			.returns("FooClass", EdeltaEcoreReferenceStateInformation::getEClassifierName)
			.returns(null, EdeltaEcoreReferenceStateInformation::getENamedElementName);
	}

	@Test
	public void testReferenceToEEnumLiteral() throws Exception {
		var ref =
			lastEcoreRef(parseWithTestEcore("""
				metamodel "foo"

				modifyEcore aTest epackage foo {
					ecoreref(FooEnumLiteral)
				}
				"""));
		var info = informationHelper
				.getOrComputeInformation(ref);
		assertThat(info)
			.returns("EEnumLiteral", EdeltaEcoreReferenceStateInformation::getType)
			.returns("foo", EdeltaEcoreReferenceStateInformation::getEPackageName)
			.returns("FooEnum", EdeltaEcoreReferenceStateInformation::getEClassifierName)
			.returns("FooEnumLiteral", EdeltaEcoreReferenceStateInformation::getENamedElementName);
	}

	@Test
	public void testReferenceToEStructuralFeature() throws Exception {
		var ref =
			lastEcoreRef(parseWithTestEcore("""
				metamodel "foo"

				modifyEcore aTest epackage foo {
					ecoreref(myReference)
				}
				"""));
		var info = informationHelper
				.getOrComputeInformation(ref);
		assertThat(info)
			.returns("EReference", EdeltaEcoreReferenceStateInformation::getType)
			.returns("foo", EdeltaEcoreReferenceStateInformation::getEPackageName)
			.returns("FooClass", EdeltaEcoreReferenceStateInformation::getEClassifierName)
			.returns("myReference", EdeltaEcoreReferenceStateInformation::getENamedElementName);
	}

	@Test
	public void testReferenceToUnresolved() throws Exception {
		var ref =
			lastEcoreRef(parseWithTestEcore("""
				metamodel "foo"

				modifyEcore aTest epackage foo {
					ecoreref(unknown)
				}
				"""));
		var info = informationHelper
				.getOrComputeInformation(ref);
		assertThat(info)
			.returns("ENamedElement", EdeltaEcoreReferenceStateInformation::getType)
			.returns("", EdeltaEcoreReferenceStateInformation::getEPackageName)
			.returns("", EdeltaEcoreReferenceStateInformation::getEClassifierName)
			.returns("", EdeltaEcoreReferenceStateInformation::getENamedElementName);
	}

	@Test
	public void testAfterChange() throws Exception {
		var ref =
			lastEcoreRef(parseWithTestEcore("""
				metamodel "foo"

				modifyEcore aTest epackage foo {
					ecoreref(myAttribute)
				}
				"""));
		var info = informationHelper
				.getOrComputeInformation(ref);
		assertThat(info)
			.returns("EAttribute", EdeltaEcoreReferenceStateInformation::getType)
			.returns("foo", EdeltaEcoreReferenceStateInformation::getEPackageName)
			.returns("FooClass", EdeltaEcoreReferenceStateInformation::getEClassifierName)
			.returns("myAttribute", EdeltaEcoreReferenceStateInformation::getENamedElementName);
		// change the underlying model
		final EAttribute attr = (EAttribute) ref.getReference().getEnamedelement();
		attr.setName("renamed");
		attr.getEContainingClass().setName("Renamed");
		// but the information stored hasn't changed
		info = informationHelper.getOrComputeInformation(ref);
		assertThat(info)
			.returns("EAttribute", EdeltaEcoreReferenceStateInformation::getType)
			.returns("foo", EdeltaEcoreReferenceStateInformation::getEPackageName)
			.returns("FooClass", EdeltaEcoreReferenceStateInformation::getEClassifierName)
			.returns("myAttribute", EdeltaEcoreReferenceStateInformation::getENamedElementName);
	}

	@Test
	public void testNullENamedElement() throws Exception {
		var ref =
			lastEcoreRef(parseWithTestEcore("""
				metamodel "foo"

				modifyEcore aTest epackage foo {
					ecoreref()
				}
				"""));
		var info = informationHelper
				.getOrComputeInformation(ref);
		assertThat(info)
			.returns("ENamedElement", EdeltaEcoreReferenceStateInformation::getType)
			.returns(null, EdeltaEcoreReferenceStateInformation::getEPackageName)
			.returns(null, EdeltaEcoreReferenceStateInformation::getEClassifierName)
			.returns(null, EdeltaEcoreReferenceStateInformation::getENamedElementName);
	}

	private EdeltaEcoreReferenceExpression lastEcoreRef(final EdeltaProgram p) throws Exception {
		return lastEcoreReferenceExpression(p);
	}
}
