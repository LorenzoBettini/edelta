package edelta.tests

import com.google.inject.Inject
import edelta.edelta.EdeltaEcoreQualifiedReference
import edelta.edelta.EdeltaEcoreReferenceExpression
import edelta.edelta.EdeltaProgram
import edelta.interpreter.EdeltaInterpreterRuntimeException
import edelta.resource.derivedstate.EdeltaDerivedStateHelper
import edelta.tests.additional.TestableEdeltaDerivedStateComputer
import edelta.tests.injectors.EdeltaInjectorProviderTestableDerivedStateComputer
import org.eclipse.emf.common.notify.impl.AdapterImpl
import org.eclipse.emf.ecore.EAttribute
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.xtext.common.types.JvmGenericType
import org.eclipse.xtext.resource.DerivedStateAwareResource
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.assertThatThrownBy

import static extension org.eclipse.xtext.EcoreUtil2.*
import static extension org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderTestableDerivedStateComputer)
class EdeltaDerivedStateComputerTest extends EdeltaAbstractTest {

	@Inject extension TestableEdeltaDerivedStateComputer
	@Inject extension EdeltaDerivedStateHelper

	@Test
	def void testCopiedEPackages() throws Exception {
		val program = '''
		package test
		
		metamodel "foo"
		metamodel "bar"
		
		modifyEcore aTest1 epackage foo {}
		modifyEcore aTest2 epackage bar {}
		'''.
		parseWithTestEcores
		val packages = program.eResource.copiedEPackagesMap.values
		assertThat(packages.map[name])
			.containsExactlyInAnyOrder("foo", "bar")
	}

	@Test
	def void testCopiedEPackagesWithSingleModifyEcore() throws Exception {
		val program = '''
		package test
		
		metamodel "foo"
		metamodel "bar"
		
		modifyEcore aTest1 epackage foo {}
		'''.
		parseWithTestEcores
		val packages = program.eResource.copiedEPackagesMap.values
		// even if bar is not modified it is still copied
		assertThat(packages.map[name])
			.containsExactlyInAnyOrder("foo", "bar")
	}

	@Test
	def void testCopiedEPackagesWhenDuplicateImports() throws Exception {
		val program = '''
		package test
		
		metamodel "foo"
		metamodel "foo"
		
		modifyEcore aTest1 epackage foo {}
		modifyEcore aTest2 epackage foo {}
		'''.
		parseWithTestEcore
		val packages = program.eResource.copiedEPackagesMap.values
		assertThat(packages.map[name])
			.containsExactly("foo")
	}

	@Test
	def void testCopiedEPackagesWhenUnresolvedPackages() throws Exception {
		val program = '''
		package test
		
		metamodel "unresolved"
		metamodel "unresolved"
		
		modifyEcore aTest1 epackage unresolved {}
		modifyEcore aTest2 epackage unresolved {}
		'''.
		parseWithTestEcore
		val packages = program.eResource.copiedEPackagesMap.values
		assertThat(packages).hasSize(1)
	}

	@Test
	def void testCopiedEPackagesWithReferences() throws Exception {
		val program = '''
		package test
		
		metamodel "testecoreforreferences1"
		metamodel "testecoreforreferences2"
		
		modifyEcore aTest1 epackage testecoreforreferences1 {}
		modifyEcore aTest2 epackage testecoreforreferences2 {}
		'''.
		parseWithTestEcoresWithReferences
		val packages = program.eResource.copiedEPackagesMap.values
		assertThat(packages).hasSize(2)
		val testecoreforreferences1 = packages.getByName("testecoreforreferences1")
		val testecoreforreferences2 = packages.getByName("testecoreforreferences2")
		val person = testecoreforreferences1.getEClassByName("Person")
		val workplace = testecoreforreferences2.getEClassByName("WorkPlace")
		assertSame(
			person.getEReferenceByName("works").EOpposite,
			workplace.getEReferenceByName("persons")
		)
		assertSame(
			person.getEReferenceByName("works"),
			workplace.getEReferenceByName("persons").EOpposite
		)
	}

	@Test
	def void testInvalidDirectSubPackageAreNotCopied() throws Exception {
		val program = '''
		package test
		
		metamodel "mainpackage.mainsubpackage"
		
		modifyEcore aTest epackage mainsubpackage {
			
		}
		'''.
		parseWithTestEcoreWithSubPackage
		val packages = program.eResource.copiedEPackagesMap.values
		assertThat(packages).isEmpty
	}

	@Test
	def void testInstallDerivedStateDuringPreIndexingPhase() throws Exception {
		val program = '''
		package test
		
		metamodel "foo"
		
		modifyEcore aTest1 epackage foo {}
		'''.
		parseWithTestEcore
		val resource = program.eResource as DerivedStateAwareResource
		installDerivedState(program.eResource as DerivedStateAwareResource, true)
		// only program must be there and the inferred Jvm Type
		// since we don't install anything during preIndexingPhase
		assertEquals("test.__synthetic0", (resource.contents.last as JvmGenericType).identifier)
	}

	@Test
	def void testDerivedStateForModifyEcoreWithMissingPackage() throws Exception {
		val program = '''
			package test

			metamodel "foo"

			modifyEcore aTest
		'''.
		parseWithTestEcore
		val resource = program.eResource as DerivedStateAwareResource
		// only program must be there and the inferred Jvm Type
		assertEquals("test.__synthetic0", (resource.contents.last as JvmGenericType).identifier)
	}

	@Test
	def void testDerivedStateIsCorrectlyDiscarded() throws Exception {
		val program = '''
		package test
		
		metamodel "foo"
		
		modifyEcore aTest1 epackage foo {
			addNewEClass("First")
		}
		'''.
		parseWithTestEcore
		val resource = program.eResource as DerivedStateAwareResource
		assertEquals("First", program.lastCopiedEPackageLastEClass.name)
		// discard derived state
		program.modifyEcoreOperations.clear
		resource.discardDerivedState
		// only program must be there and the inferred Jvm Type
		assertEquals("test.__synthetic0", (resource.contents.last as JvmGenericType).identifier)
	}

	@Test
	def void testDerivedStateIsCorrectlyDiscardedAndUnloaded() throws Exception {
		val program = '''
		package test
		
		metamodel "foo"
		
		modifyEcore aTest1 epackage foo {
			addNewEClass("First")
			ecoreref(First)
		}
		'''.
		parseWithTestEcore
		val resource = program.eResource as DerivedStateAwareResource
		val derivedStateEClass = program.lastCopiedEPackageLastEClass
		val eclassRef = program.lastModifyEcoreOperation.body.block.expressions.last.
			edeltaEcoreReferenceExpression.reference.enamedelement
		assertSame(derivedStateEClass, eclassRef)
		assertEquals("First", derivedStateEClass.name)
		assertFalse("should be resolved now", eclassRef.eIsProxy)
		// discard derived state
		program.modifyEcoreOperations.remove(0)
		resource.discardDerivedState
		// the reference to the EClass is still there
		assertSame(derivedStateEClass, eclassRef)
		// but the EClass is now a proxy
		assertTrue("should be a proxy now", eclassRef.eIsProxy)
	}

	@Test
	def void testAdaptersAreRemovedFromDerivedEPackagesAfterUnloading() throws Exception {
		val program = '''
		package test
		
		metamodel "foo"
		
		modifyEcore aTest1 epackage foo {
			addNewEClass("First")
		}
		'''.
		parseWithTestEcore
		val resource = program.eResource as DerivedStateAwareResource
		// val derivedToSourceMap = resource.derivedToSourceMap
		val nameToCopiedEPackageMap = resource.copiedEPackagesMap
		assertFalse(resource.eAdapters.empty)
		// assertFalse(derivedToSourceMap.empty)
		assertFalse(nameToCopiedEPackageMap.empty)
		// explicitly add an adapter to the EPackage
		nameToCopiedEPackageMap.values.head.eAdapters += new AdapterImpl
		assertTrue(nameToCopiedEPackageMap.values.forall[!eAdapters.empty])
		// unload packages
		unloadDerivedPackages(nameToCopiedEPackageMap)
		// maps are not empty yet
		// assertFalse(derivedToSourceMap.empty)
		assertFalse(nameToCopiedEPackageMap.empty)
		assertFalse(resource.eAdapters.empty)
		// but adapters have been removed from EPackage
		assertTrue(nameToCopiedEPackageMap.values.forall[eAdapters.empty])
	}

	@Test
	def void testMapsAreClearedAfterDiscarding() throws Exception {
		val program = '''
		package test
		
		metamodel "foo"
		
		modifyEcore aTest1 epackage foo {
			addNewEClass("First")
		}
		'''.
		parseWithTestEcore
		val resource = program.eResource as DerivedStateAwareResource
		// val derivedToSourceMap = resource.derivedToSourceMap
		val nameToCopiedEPackageMap = resource.copiedEPackagesMap
		// val opToEClassMap = resource.opToEClassMap
		assertFalse(resource.eAdapters.empty)
		// assertFalse(derivedToSourceMap.empty)
		assertFalse(nameToCopiedEPackageMap.empty)
		// assertFalse(opToEClassMap.empty)
		// discard derived state
		program.modifyEcoreOperations.clear
		resource.discardDerivedState
		// maps are empty now
		// assertTrue(derivedToSourceMap.empty)
		assertTrue(nameToCopiedEPackageMap.empty)
		// assertTrue(opToEClassMap.empty)
		assertFalse(resource.eAdapters.empty)
	}

	@Test
	def void testSourceElementOfNull() throws Exception {
		assertNull(getPrimarySourceElement(null))
	}

	@Test
	def void testSourceElementOfNotDerived() throws Exception {
		assertNull('''
		package test
		'''.
		parse.getPrimarySourceElement
		)
	}

	@Test
	def void testCopiedEPackageWithRenamedEClass() throws Exception {
		val program = '''
		package test
		
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).name = "Renamed"
		}
		'''.
		parseWithTestEcore
		val derivedEClass = program.lastCopiedEPackageFirstEClass
		assertEquals("Renamed", derivedEClass.name)
	}

	@Test
	def void testDerivedStateForCreatedEAttributeInChangeEClassWithNewName() throws Exception {
		val program = '''
		package test
		
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass) => [
				name = "Renamed"
				addNewEAttribute("newAttribute", ecoreref(FooDataType))
			]
		}
		'''.
		parseWithTestEcore
		val derivedEClass = program.lastCopiedEPackageFirstEClass
		val derivedEAttribute = derivedEClass.EStructuralFeatures.last as EAttribute
		assertEquals("newAttribute", derivedEAttribute.name)
		assertEquals("Renamed", derivedEAttribute.EContainingClass.name)
	}

	@Test
	def void testInterpretedCreateEClassAndStealEAttribute() throws Exception {
		val program = createEClassStealingAttribute.
			parseWithTestEcore
		// the interpretation is done on copied epackages
		val ec = program.getLastCopiedEPackageFirstEClass("NewClass")
		assertEquals("NewClass", ec.name)
		val attr = ec.EStructuralFeatures.head
		assertEquals("myAttribute", attr.name)
		program.validate
		program.assertNoErrors
		// check that the reference is not dangling
		// that is, the attribute is still contained in the original class
		// at this point of the program
		// otherwise in the editor we then get an unresolved error.
		val ecoreref = getEcoreRefInManipulationExpressionBlock(program)
		var eClass = ecoreref.qualification.enamedelement as EClass
		var eAttr = ecoreref.enamedelement as EAttribute
		// Note that the interpreter changed the attribute container
		assertEClassContainsFeature(eClass, eAttr, false)
		// but the original enamed element stored in the reference is OK
		eClass = ecoreref.qualification.originalEnamedelement as EClass
		eAttr = ecoreref.originalEnamedelement as EAttribute
		assertEClassContainsFeature(eClass, eAttr, true)
	}

	@Test
	def void testInterpretedRemovedEClassDoesNotTouchTheOriginalEcore() throws Exception {
		val program = '''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("NewClass")
				ecoreref(FooClass).EPackage.EClassifiers.remove(ecoreref(FooClass))
			}
		'''.
		parseWithTestEcore
		val derivedEClass = program.getLastCopiedEPackageLastEClass
		assertEquals("NewClass", derivedEClass.name)
		program.validate
		program.assertNoErrors
		program.copiedEPackages.head.
			EClassifiers.findFirst[name == "FooClass"].
			assertNull
		program.getEPackageByName("foo").
			EClassifiers.findFirst[name == "FooClass"].
			assertNotNull
	}

	@Test
	def void testContentAdapter() throws Exception {
		val program = '''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("NewClass")
				ecoreref(FooClass).EPackage.EClassifiers.remove(ecoreref(FooClass))
			}
		'''.
		parseWithTestEcore

		assertThatThrownBy[program.metamodels.head.EClassifiers.head.name = "bar"]
			.isInstanceOf(EdeltaInterpreterRuntimeException)
			.hasMessageContaining("Unexpected notification")
	}

	@Test
	def void testPersonListExampleModifyEcore() throws Exception {
		val prog = parseWithLoadedEcore(METAMODEL_PATH + PERSON_LIST_ECORE,
			personListExampleModifyEcore
		)
		prog.assertNoErrors
	}

	private def EdeltaEcoreQualifiedReference getEcoreRefInManipulationExpressionBlock(EdeltaProgram program) throws Exception {
		program.lastModifyEcoreOperation.body.getAllContentsOfType(EdeltaEcoreReferenceExpression)
			.last.reference.edeltaEcoreQualifiedReference
	}

	def private assertEClassContainsFeature(EClass c, EStructuralFeature f, boolean expected) throws Exception {
		assertEquals(expected,
			c.EStructuralFeatures.contains(f)
		)
	}

}
