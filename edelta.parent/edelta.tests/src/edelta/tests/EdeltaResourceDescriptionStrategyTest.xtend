package edelta.tests

import com.google.inject.Inject
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EcorePackage
import org.eclipse.xtext.resource.impl.ResourceDescriptionsProvider
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaResourceDescriptionStrategyTest extends EdeltaAbstractTest {

	@Inject ResourceDescriptionsProvider rdp

	@Test
	def void testEmptyProgram() {
		"".assertExportedEPackages("")
	}

	@Test
	def void testSingleMetamodel() {
		'''
			metamodel "ecore"
		'''.assertExportedEPackages("")
	}

	@Test
	def void testReferenceToOurMetamodel() {
		'''
			metamodel "ecore"
			metamodel "foo"
		'''.assertExportedEPackages("")
	}

	@Test
	def void testCreateEClass() {
		'''
			metamodel "ecore"
			metamodel "foo"
			
			createEClass NewClass in foo {}
		'''.assertExportedEPackages("")
		// our derived state packages must not be exported
	}

	def private assertExportedEPackages(CharSequence input, CharSequence expected) {
		val program = input.parseWithTestEcore
		program.validate
		assertEqualsStrings(
			expected,
			program.exportedEPackageEObjectDescriptions.map[name].join(", ")
		)
	}

	def private getExportedEPackageEObjectDescriptions(EObject o) {
		o.getResourceDescription.getExportedObjectsByType(EcorePackage.eINSTANCE.EPackage)
	}

	def private getResourceDescription(EObject o) {
		val index = rdp.getResourceDescriptions(o.eResource)
		index.getResourceDescription(o.eResource.URI)
	}

}
