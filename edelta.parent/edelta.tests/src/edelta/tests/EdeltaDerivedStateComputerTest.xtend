package edelta.tests

import com.google.inject.Inject
import edelta.edelta.EdeltaEcoreQualifiedReference
import edelta.edelta.EdeltaProgram
import edelta.interpreter.EdeltaSafeInterpreter.EdeltaInterpreterRuntimeException
import edelta.resource.EdeltaDerivedStateComputer.EdeltaDerivedStateAdapter
import edelta.tests.additional.TestableEdeltaDerivedStateComputer
import org.eclipse.emf.common.notify.impl.AdapterImpl
import org.eclipse.emf.ecore.EAttribute
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.emf.ecore.resource.impl.ResourceImpl
import org.eclipse.xtext.common.types.JvmGenericType
import org.eclipse.xtext.resource.DerivedStateAwareResource
import org.eclipse.xtext.resource.XtextResource
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith

import static org.assertj.core.api.Assertions.assertThat

import static extension org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderTestableDerivedStateComputer)
class EdeltaDerivedStateComputerTest extends EdeltaAbstractTest {

	@Inject extension TestableEdeltaDerivedStateComputer

	@Rule
	val public ExpectedException thrown = ExpectedException.none();

	@Test
	def void testGetOrInstallAdapterWithNotXtextResource() {
		assertNotNull(getOrInstallAdapter(new ResourceImpl))
	}

	@Test
	def void testGetOrInstallAdapterWithXtextResourceOfADifferentLanguage() {
		val res = new XtextResource
		res.languageName = "foo"
		assertNotNull(getOrInstallAdapter(res))
	}

	@Test
	def void testIsAdapterFor() {
		val adapter = getOrInstallAdapter(new ResourceImpl)
		assertTrue(adapter.isAdapterForType(EdeltaDerivedStateAdapter))
		assertFalse(adapter.isAdapterForType(String))
	}

	@Test
	def void testCopiedEPackages() {
		val program = '''
		package test
		
		metamodel "foo"
		metamodel "bar"
		
		modifyEcore aTest1 epackage foo {}
		modifyEcore aTest2 epackage bar {}
		'''.
		parseWithTestEcores
		val packages = program.eResource.copiedEPackages
		assertThat(packages)
			.extracting([name])
			.containsExactlyInAnyOrder("foo", "bar")
	}

	@Test
	def void testInstallDerivedStateDuringPreIndexingPhase() {
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
	def void testDerivedStateEcoreModifyWithMissingReferredPackage() {
		val program = '''
		package test
		
		modifyEcore aTest1 epackage foo {}
		'''.
		parseWithTestEcore
		val packages = program.eResource.copiedEPackages
		assertThat(packages)
			.hasSize(1)
			.allMatch[p | p.eIsProxy]
	}

	@Test
	def void testDerivedStateForModifyEcoreWithMissingPackage() {
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
	def void testDerivedStateIsCorrectlyDiscarded() {
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
	def void testDerivedStateIsCorrectlyDiscardedAndUnloaded() {
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
	def void testAdaptersAreRemovedFromDerivedEPackagesAfterUnloading() {
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
		val nameToEPackageMap = resource.nameToEPackageMap
		val nameToCopiedEPackageMap = resource.nameToCopiedEPackageMap
		assertFalse(resource.eAdapters.empty)
		// assertFalse(derivedToSourceMap.empty)
		assertFalse(nameToEPackageMap.empty)
		assertFalse(nameToCopiedEPackageMap.empty)
		// explicitly add an adapter to the EPackage
		nameToEPackageMap.values.head.eAdapters += new AdapterImpl
		assertTrue(nameToEPackageMap.values.forall[!eAdapters.empty])
		nameToCopiedEPackageMap.values.head.eAdapters += new AdapterImpl
		assertTrue(nameToCopiedEPackageMap.values.forall[!eAdapters.empty])
		// unload packages
		unloadDerivedPackages(nameToEPackageMap)
		unloadDerivedPackages(nameToCopiedEPackageMap)
		// maps are not empty yet
		// assertFalse(derivedToSourceMap.empty)
		assertFalse(nameToEPackageMap.empty)
		assertFalse(nameToCopiedEPackageMap.empty)
		assertFalse(resource.eAdapters.empty)
		// but adapters have been removed from EPackage
		assertTrue(nameToEPackageMap.values.forall[eAdapters.empty])
		assertTrue(nameToCopiedEPackageMap.values.forall[eAdapters.empty])
	}

	@Test
	def void testMapsAreClearedAfterDiscarding() {
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
		val nameToEPackageMap = resource.nameToEPackageMap
		val nameToCopiedEPackageMap = resource.nameToCopiedEPackageMap
		// val opToEClassMap = resource.opToEClassMap
		assertFalse(resource.eAdapters.empty)
		// assertFalse(derivedToSourceMap.empty)
		assertFalse(nameToEPackageMap.empty)
		assertFalse(nameToCopiedEPackageMap.empty)
		// assertFalse(opToEClassMap.empty)
		// discard derived state
		program.modifyEcoreOperations.clear
		resource.discardDerivedState
		// maps are empty now
		// assertTrue(derivedToSourceMap.empty)
		assertTrue(nameToEPackageMap.empty)
		assertTrue(nameToCopiedEPackageMap.empty)
		// assertTrue(opToEClassMap.empty)
		assertFalse(resource.eAdapters.empty)
	}

	@Test
	def void testSourceElementOfNull() {
		assertNull(getPrimarySourceElement(null))
	}

	@Test
	def void testSourceElementOfNotDerived() {
		assertNull('''
		package test
		'''.
		parse.getPrimarySourceElement
		)
	}

	@Test
	def void testCopiedEPackageWithRenamedEClass() {
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
	def void testDerivedStateForCreatedEAttributeInChangeEClassWithNewName() {
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
	def void testInterpretedCreateEClassAndStealEAttribute() {
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
	def void testInterpretedRemovedEClassDoesNotTouchTheOriginalEcore() {
		val program = '''
			metamodel "foo"
			
			createEClass NewClass in foo {
				ecoreref(FooClass).EPackage.EClassifiers.remove(ecoreref(FooClass))
			}
		'''.
		parseWithTestEcore
		val derivedEClass = program.getDerivedStateLastEClass
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
	def void testInterpretedRemovedEClassDoesNotTouchTheOriginalEcore_Qualified() {
		val program = '''
			metamodel "foo"
			
			createEClass NewClass in foo {
				ecoreref(foo.FooClass).EPackage.EClassifiers.remove(ecoreref(foo.FooClass))
			}
		'''.
		parseWithTestEcore
		val derivedEClass = program.getDerivedStateLastEClass
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
	def void testGetEClassWithTheSameName() {
		val program = '''
			metamodel "foo"
			
			changeEClass foo.FooClass {
			}
		'''.
		parseWithTestEcore
		val original = program.lastExpression.changeEClassExpression.original
		val copies = program.copiedEPackages.toList
		assertNotNull(copies.getEClassWithTheSameName(original))
		val fake = EcoreFactory.eINSTANCE.createEClass => [name="fake"]
		assertNull(copies.getEClassWithTheSameName(fake))
		copies.clear
		assertNull(copies.getEClassWithTheSameName(original))
	}

	@Test
	def void testGetEClassWithTheSameNameNotFound() {
		// make sure there's no NPE
		val program = '''
			metamodel "foo"
			
			changeEClass foo.NonExistant {
			}
		'''.
		parseWithTestEcore
		val original = program.lastExpression.changeEClassExpression.original
		assertNotNull(original)
	}

	@Test
	def void testContentAdapter() {
		val program = '''
			metamodel "foo"
			
			createEClass NewClass in foo {
				ecoreref(foo.FooClass).EPackage.EClassifiers.remove(ecoreref(foo.FooClass))
			}
		'''.
		parseWithTestEcore

		thrown.expect(EdeltaInterpreterRuntimeException);
		thrown.expectMessage("Unexpected notification");

		program.metamodels.head.EClassifiers.head.name = "bar"
	}

	@Test
	def void testDerivedAndCopiedEPackagesForModifyEcore() {
		val program = '''
		package test
		
		metamodel "foo"
		
		modifyEcore aTest epackage foo {}
		'''.
		parseWithTestEcore
		var packages = program.eResource.derivedEPackages
		assertEquals(1, packages.size)
		assertEquals("foo", packages.head.name)
		packages = program.eResource.copiedEPackages
		assertEquals(1, packages.size)
		assertEquals("foo", packages.head.name)
	}

	@Test
	def void testDerivedStateForModifyEcoreCreatedEClass() {
		val program = '''
		package test
		
		metamodel "foo"
		
		modifyEcore aModificationTest epackage foo {
			EClassifiers += newEClass("ANewClass") [
				ESuperTypes += newEClass("Base")
			]
		}
		'''.
		parseWithTestEcore
		val lastEClass = program.lastCopiedEPackageLastEClass
		assertEquals("ANewClass", lastEClass.name)
		assertEquals("foo", lastEClass.EPackage.name)
	}

	@Test
	def void testInterpretedRemovedEClassInModifyEcoreDoesNotTouchTheOriginalEcore() {
		val program = '''
			metamodel "foo"
			
			modifyEcore aModificationTest epackage foo {
				EClassifiers += newEClass("ANewClass")
				ecoreref(FooClass).EPackage.EClassifiers.remove(ecoreref(FooClass))
			}
		'''.
		parseWithTestEcore
		val lastEClass = program.lastCopiedEPackageLastEClass
		assertEquals("ANewClass", lastEClass.name)
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
	def void testPersonListExampleModifyEcore() {
		val prog = parseWithLoadedEcore(PERSON_LIST_ECORE_PATH,
			personListExampleModifyEcore
		)
		prog.assertNoErrors
	}

	protected def EdeltaEcoreQualifiedReference getEcoreRefInManipulationExpressionBlock(EdeltaProgram program) {
		program.lastExpression.getManipulationEClassExpression.body.expressions.head.
			variableDeclaration.right.
			edeltaEcoreReferenceExpression.reference.edeltaEcoreQualifiedReference
	}

	def private assertEClassContainsFeature(EClass c, EStructuralFeature f, boolean expected) {
		assertEquals(expected,
			c.EStructuralFeatures.contains(f)
		)
	}
}
