package edelta.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.xtext.EcoreUtil2.getAllContentsOfType;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.forall;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.lastOrNull;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.resource.DerivedStateAwareResource;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import edelta.edelta.EdeltaEcoreQualifiedReference;
import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaProgram;
import edelta.interpreter.EdeltaInterpreterRuntimeException;
import edelta.tests.additional.TestableEdeltaDerivedStateComputer;
import edelta.tests.injectors.EdeltaInjectorProviderTestableDerivedStateComputer;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderTestableDerivedStateComputer.class)
public class EdeltaDerivedStateComputerTest extends EdeltaAbstractTest {
	@Inject
	private TestableEdeltaDerivedStateComputer derivedStateComputer;

	@Test
	public void testCopiedEPackages() throws Exception {
		var program = parseWithTestEcores("""
			package test
			
			metamodel "foo"
			metamodel "bar"
			
			modifyEcore aTest1 epackage foo {}
			modifyEcore aTest2 epackage bar {}
		""");
		var packages = derivedStateHelper.getCopiedEPackagesMap(program.eResource())
				.values();
		assertThat(map(packages, EPackage::getName))
				.containsExactlyInAnyOrder("foo", "bar");
	}

	@Test
	public void testCopiedEPackagesWithSingleModifyEcore() throws Exception {
		var program = parseWithTestEcores("""
			package test
			
			metamodel "foo"
			metamodel "bar"
			
			modifyEcore aTest1 epackage foo {}
		""");
		var packages = derivedStateHelper.getCopiedEPackagesMap(program.eResource())
				.values();
		// even if bar is not modified it is still copied
		assertThat(map(packages, EPackage::getName))
				.containsExactlyInAnyOrder("foo", "bar");
	}

	@Test
	public void testCopiedEPackagesWhenDuplicateImports() throws Exception {
		var program = parseWithTestEcore("""
			package test
			
			metamodel "foo"
			metamodel "foo"
			
			modifyEcore aTest1 epackage foo {}
			modifyEcore aTest2 epackage foo {}
		""");
		var packages = derivedStateHelper.getCopiedEPackagesMap(program.eResource())
				.values();
		assertThat(map(packages, EPackage::getName))
				.containsExactly("foo");
	}

	@Test
	public void testCopiedEPackagesWhenUnresolvedPackages() throws Exception {
		var program = parseWithTestEcore("""
			package test
			
			metamodel "unresolved"
			metamodel "unresolved"
			
			modifyEcore aTest1 epackage unresolved {}
			modifyEcore aTest2 epackage unresolved {}
		""");
		var packages = derivedStateHelper.getCopiedEPackagesMap(program.eResource())
				.values();
		assertThat(packages).hasSize(1);
	}

	@Test
	public void testCopiedEPackagesWithReferences() throws Exception {
		var program = parseWithTestEcoresWithReferences("""
			package test
			
			metamodel "testecoreforreferences1"
			metamodel "testecoreforreferences2"
			
			modifyEcore aTest1 epackage testecoreforreferences1 {}
			modifyEcore aTest2 epackage testecoreforreferences2 {}
		""");
		var packages = derivedStateHelper.getCopiedEPackagesMap(program.eResource())
				.values();
		assertThat(packages).hasSize(2);
		var testecoreforreferences1 = getByName(packages, "testecoreforreferences1");
		var testecoreforreferences2 = getByName(packages, "testecoreforreferences2");
		var person = getEClassByName(testecoreforreferences1, "Person");
		var workplace = getEClassByName(testecoreforreferences2, "WorkPlace");
		assertSame(getEReferenceByName(person, "works").getEOpposite(),
				getEReferenceByName(workplace, "persons"));
		assertSame(getEReferenceByName(person, "works"),
				getEReferenceByName(workplace, "persons").getEOpposite());
	}

	@Test
	public void testInvalidDirectSubPackageAreNotCopied() throws Exception {
		var program = parseWithTestEcoreWithSubPackage("""
			package test
			
			metamodel "mainpackage.mainsubpackage"
			
			modifyEcore aTest epackage mainsubpackage {
				
			}
		""");
		var packages = derivedStateHelper.getCopiedEPackagesMap(program.eResource())
				.values();
		assertThat(packages).isEmpty();
	}

	@Test
	public void testInstallDerivedStateDuringPreIndexingPhase() throws Exception {
		var program = parseWithTestEcore("""
			package test
			
			metamodel "foo"
			
			modifyEcore aTest1 epackage foo {}
		""");
		var resource = ((DerivedStateAwareResource) program.eResource());
		derivedStateComputer.installDerivedState(((DerivedStateAwareResource) program.eResource()), true);
		// only program must be there and the inferred Jvm Type
		// since we don't install anything during preIndexingPhase
		assertEquals("test.__synthetic0",
			((JvmGenericType)
				lastOrNull(resource.getContents())).getIdentifier());
	}

	@Test
	public void testDerivedStateForModifyEcoreWithMissingPackage() throws Exception {
		var program = parseWithTestEcore("""
			package test

			metamodel "foo"

			modifyEcore aTest
		""");
		var resource = ((DerivedStateAwareResource) program.eResource());
		// only program must be there and the inferred Jvm Type
		assertEquals("test.__synthetic0",
			((JvmGenericType)
				lastOrNull(resource.getContents())).getIdentifier());
	}

	@Test
	public void testDerivedStateIsCorrectlyDiscarded() throws Exception {
		var program = parseWithTestEcore("""
			package test
			
			metamodel "foo"
			
			modifyEcore aTest1 epackage foo {
				addNewEClass("First")
			}
		""");
		var resource = ((DerivedStateAwareResource) program.eResource());
		assertEquals("First", getLastCopiedEPackageLastEClass(program).getName());
		// discard derived state
		program.getModifyEcoreOperations().clear();
		resource.discardDerivedState();
		// only program must be there and the inferred Jvm Type
		assertEquals("test.__synthetic0",
			((JvmGenericType)
				lastOrNull(resource.getContents())).getIdentifier());
	}

	@Test
	public void testDerivedStateIsCorrectlyDiscardedAndUnloaded() throws Exception {
		var program = parseWithTestEcore("""
			package test
			
			metamodel "foo"
			
			modifyEcore aTest1 epackage foo {
				addNewEClass("First")
				ecoreref(First)
			}
		""");
		var resource = ((DerivedStateAwareResource) program.eResource());
		var derivedStateEClass = getLastCopiedEPackageLastEClass(program);
		var eclassRef =
			getEdeltaEcoreReferenceExpression(getLastModifyEcoreOperationLastExpression(program))
			.getReference().getEnamedelement();
		assertSame(derivedStateEClass, eclassRef);
		assertEquals("First", derivedStateEClass.getName());
		assertFalse("should be resolved now", eclassRef.eIsProxy());
		program.getModifyEcoreOperations().remove(0);
		resource.discardDerivedState();
		// the reference to the EClass is still there
		assertSame(derivedStateEClass, eclassRef);
		// but the EClass is now a proxy
		assertTrue("should be a proxy now", eclassRef.eIsProxy());
	}

	@Test
	public void testAdaptersAreRemovedFromDerivedEPackagesAfterUnloading() throws Exception {
		EdeltaProgram program = parseWithTestEcore("""
			package test
			
			metamodel "foo"
			
			modifyEcore aTest1 epackage foo {
				addNewEClass("First")
			}
		""");
		var resource = ((DerivedStateAwareResource) program.eResource());
		var nameToCopiedEPackageMap = derivedStateHelper
				.getCopiedEPackagesMap(resource);
		assertFalse(resource.eAdapters().isEmpty());
		assertFalse(nameToCopiedEPackageMap.values().isEmpty());
		// explicitly add an adapter to the EPackage
		nameToCopiedEPackageMap.values().iterator().next().eAdapters()
			.add(new AdapterImpl());
		assertTrue(forall(nameToCopiedEPackageMap.values(),
				it -> !it.eAdapters().isEmpty()));
		// unload packages
		derivedStateComputer.unloadDerivedPackages(nameToCopiedEPackageMap);
		// maps are not empty yet
		assertFalse(nameToCopiedEPackageMap.values().isEmpty());
		assertFalse(resource.eAdapters().isEmpty());
		// but adapters have been removed from EPackage
		assertTrue(forall(nameToCopiedEPackageMap.values(),
				it -> it.eAdapters().isEmpty()));
	}

	@Test
	public void testMapsAreClearedAfterDiscarding() throws Exception {
		var program = parseWithTestEcore("""
			package test
			
			metamodel "foo"
			
			modifyEcore aTest1 epackage foo {
				addNewEClass("First")
			}
		""");
		var resource = ((DerivedStateAwareResource) program.eResource());
		var nameToCopiedEPackageMap = derivedStateHelper
				.getCopiedEPackagesMap(resource);
		assertFalse(resource.eAdapters().isEmpty());
		assertFalse(nameToCopiedEPackageMap.values().isEmpty());
		// discard derived state
		program.getModifyEcoreOperations().clear();
		resource.discardDerivedState();
		// maps are empty now
		assertTrue(nameToCopiedEPackageMap.values().isEmpty());
		assertFalse(resource.eAdapters().isEmpty());
	}

	@Test
	public void testSourceElementOfNull() throws Exception {
		assertNull(
			derivedStateComputer.getPrimarySourceElement(null));
	}

	@Test
	public void testSourceElementOfNotDerived() throws Exception {
		assertNull(
			derivedStateComputer
				.getPrimarySourceElement(
					parseHelper.parse("package test")));
	}

	@Test
	public void testCopiedEPackageWithRenamedEClass() throws Exception {
		var program = parseWithTestEcore("""
			package test
			
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				ecoreref(foo.FooClass).name = "Renamed"
			}
		""");
		var derivedEClass = getLastCopiedEPackageFirstEClass(program);
		assertEquals("Renamed", derivedEClass.getName());
	}

	@Test
	public void testDerivedStateForCreatedEAttributeInChangeEClassWithNewName() throws Exception {
		var program = parseWithTestEcore("""
			package test
			
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				ecoreref(foo.FooClass) => [
					name = "Renamed"
					addNewEAttribute("newAttribute", ecoreref(FooDataType))
				]
			}
		""");
		var derivedEClass = getLastCopiedEPackageFirstEClass(program);
		var derivedEAttribute = (EAttribute) lastOrNull(derivedEClass.getEStructuralFeatures());
		assertEquals("newAttribute", derivedEAttribute.getName());
		assertEquals("Renamed", derivedEAttribute.getEContainingClass().getName());
	}

	@Test
	public void testInterpretedCreateEClassAndStealEAttribute() throws Exception {
		var program = parseWithTestEcore(inputs.createEClassStealingAttribute());
		// the interpretation is done on copied epackages
		var ec = getLastCopiedEPackageFirstEClass(program, "NewClass");
		assertEquals("NewClass", ec.getName());
		var attr = ec.getEStructuralFeatures().get(0);
		assertEquals("myAttribute", attr.getName());
		validationTestHelper.validate(program);
		validationTestHelper.assertNoErrors(program);
		// check that the reference is not dangling
		// that is, the attribute is still contained in the original class
		// at this point of the program
		// otherwise in the editor we then get an unresolved error.
		var ecoreref = getEcoreRefInManipulationExpressionBlock(program);
		var eClass = ((EClass) ecoreref.getQualification().getEnamedelement());
		var eAttr = ((EAttribute) ecoreref.getEnamedelement());
		// Note that the interpreter changed the attribute container
		assertEClassContainsFeature(eClass, eAttr, false);
		// but the original enamed element stored in the reference is OK
		eClass = (EClass) derivedStateHelper
				.getOriginalEnamedelement(ecoreref.getQualification());
		eAttr = (EAttribute) derivedStateHelper.getOriginalEnamedelement(ecoreref);
		assertEClassContainsFeature(eClass, eAttr, true);
	}

	@Test
	public void testInterpretedRemovedEClassDoesNotTouchTheOriginalEcore() throws Exception {
		var program = parseWithTestEcore("""
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			addNewEClass("NewClass")
			ecoreref(FooClass).EPackage.EClassifiers.remove(ecoreref(FooClass))
		}
		""");
		var derivedEClass = getLastCopiedEPackageLastEClass(program);
		assertEquals("NewClass", derivedEClass.getName());
		validationTestHelper.validate(program);
		validationTestHelper.assertNoErrors(program);
		var fooClassInCopies =
			getCopiedEPackages(program).iterator().next()
			.getEClassifier("FooClass");
		assertNull(fooClassInCopies);
		var fooClassInProgram =
			getEPackageByName(program, "foo")
			.getEClassifier("FooClass");
		assertNotNull(fooClassInProgram);
	}

	@Test
	public void testContentAdapter() throws Exception {
		var program = parseWithTestEcore("""
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			addNewEClass("NewClass")
			ecoreref(FooClass).EPackage.EClassifiers
				.remove(ecoreref(FooClass))
		}
		""");
		var eClassifier = program.getMetamodels().get(0)
				.getEClassifiers().get(0);
		assertThatThrownBy(() -> eClassifier.setName("bar"))
			.isInstanceOf(EdeltaInterpreterRuntimeException.class)
			.hasMessageContaining("Unexpected notification");
	}

	@Test
	public void testPersonListExampleModifyEcore() throws Exception {
		var prog = parseWithLoadedEcore(
			EdeltaAbstractTest.METAMODEL_PATH + EdeltaAbstractTest.PERSON_LIST_ECORE,
			inputs.personListExampleModifyEcore());
		validationTestHelper.assertNoErrors(prog);
	}

	private EdeltaEcoreQualifiedReference getEcoreRefInManipulationExpressionBlock(EdeltaProgram program)
			throws Exception {
		return getEdeltaEcoreQualifiedReference(
			lastOrNull(getAllContentsOfType(
				lastModifyEcoreOperation(program).getBody(),
				EdeltaEcoreReferenceExpression.class))
					.getReference());
	}

	private void assertEClassContainsFeature(EClass c, EStructuralFeature f, boolean expected)
			throws Exception {
		assertEquals(expected,
			c.getEStructuralFeatures().contains(f));
	}
}
