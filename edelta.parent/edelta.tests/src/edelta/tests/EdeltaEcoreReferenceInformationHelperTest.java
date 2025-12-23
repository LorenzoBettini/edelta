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
		var info1 = informationHelper.getOrComputeInformation(ref);
		var info2 = informationHelper.getOrComputeInformation(ref);
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
			.returns("EPackage", t -> t.getType())
			.returns("foo", t -> t.getEPackageName())
			.returns(null, t -> t.getEClassifierName())
			.returns(null, t -> t.getENamedElementName());
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
			.returns("EPackage", t -> t.getType())
			.returns("mainpackage.mainsubpackage", t -> t.getEPackageName())
			.returns(null, t -> t.getEClassifierName())
			.returns(null, t -> t.getENamedElementName());
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
			.returns("EAttribute", t -> t.getType())
			.returns("mainpackage.mainsubpackage", t -> t.getEPackageName())
			.returns("MainSubPackageFooClass", t -> t.getEClassifierName())
			.returns("mySubPackageAttribute", t -> t.getENamedElementName());
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
		var subpackage = ((EPackage) ref.getArgument().getElement());
		subpackage.getESubpackages().add(subpackage.getESuperPackage());

		var info = informationHelper
				.getOrComputeInformation(ref);
		// due to the cycle the parent relation is not visited
		assertThat(info)
			.returns("EPackage", t -> t.getType())
			.returns("subsubpackage", t -> t.getEPackageName())
			.returns(null, t -> t.getEClassifierName())
			.returns(null, t -> t.getENamedElementName());
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
			.returns("EClass", t -> t.getType())
			.returns("foo", t -> t.getEPackageName())
			.returns("FooClass", t -> t.getEClassifierName())
			.returns(null, t -> t.getENamedElementName());
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
			.returns("EEnumLiteral", t -> t.getType())
			.returns("foo", t -> t.getEPackageName())
			.returns("FooEnum", t -> t.getEClassifierName())
			.returns("FooEnumLiteral", t -> t.getENamedElementName());
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
			.returns("EReference", t -> t.getType())
			.returns("foo", t -> t.getEPackageName())
			.returns("FooClass", t -> t.getEClassifierName())
			.returns("myReference", t -> t.getENamedElementName());
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
			.returns("ENamedElement", t -> t.getType())
			.returns("", t -> t.getEPackageName())
			.returns("", t -> t.getEClassifierName())
			.returns("", t -> t.getENamedElementName());
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
			.returns("EAttribute", t -> t.getType())
			.returns("foo", t -> t.getEPackageName())
			.returns("FooClass", t -> t.getEClassifierName())
			.returns("myAttribute", t -> t.getENamedElementName());
		// change the underlying model
		var attr = (EAttribute) ref.getArgument().getElement();
		attr.setName("renamed");
		attr.getEContainingClass().setName("Renamed");
		// but the information stored hasn't changed
		info = informationHelper.getOrComputeInformation(ref);
		assertThat(info)
			.returns("EAttribute", t -> t.getType())
			.returns("foo", t -> t.getEPackageName())
			.returns("FooClass", t -> t.getEClassifierName())
			.returns("myAttribute", t -> t.getENamedElementName());
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
			.returns("ENamedElement", t -> t.getType())
			.returns(null, t -> t.getEPackageName())
			.returns(null, t -> t.getEClassifierName())
			.returns(null, t -> t.getENamedElementName());
	}

	private EdeltaEcoreReferenceExpression lastEcoreRef(EdeltaProgram p) {
		return getLastEcoreReferenceExpression(p);
	}
}
