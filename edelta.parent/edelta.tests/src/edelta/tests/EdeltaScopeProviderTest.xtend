package edelta.tests

import com.google.inject.Inject
import edelta.edelta.EdeltaEcoreQualifiedReference
import edelta.edelta.EdeltaEcoreReferenceExpression
import edelta.edelta.EdeltaPackage
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EDataType
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EReference
import org.eclipse.xtext.scoping.IScopeProvider
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaScopeProviderTest extends EdeltaAbstractTest {

	@Inject extension IScopeProvider

	@Test
	def void testSuperScope() {
		// just check that nothing wrong happens when we call super.getScope
		'''
		modifyEcore aTest epackage foo {
			this.
		}
		'''.parse
			.lastModifyEcoreOperation
			.body
			.blockLastExpression.getScope(EdeltaPackage.eINSTANCE.edeltaModifyEcoreOperation_Body)
	}

	@Test
	def void testScopeForMetamodel() {
		referenceToMetamodel.parseWithTestEcore.
			assertScope(EdeltaPackage.eINSTANCE.edeltaProgram_Metamodels,
			'''
			foo
			'''
			)
		// we skip nsURI references, like http://foo
	}

	@Test
	def void testScopeForMetamodels() {
		referencesToMetamodels.parseWithTestEcores.
			assertScope(EdeltaPackage.eINSTANCE.edeltaProgram_Metamodels,
			'''
			foo
			bar
			'''
			)
	}

	@Test
	def void testScopeForEnamedElementInProgram() {
		referenceToMetamodel.parseWithTestEcore.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			FooClass
			FooDataType
			FooEnum
			myAttribute
			myReference
			FooEnumLiteral
			foo
			''')
	}

	@Test
	def void testScopeForEnamedElementWithSubPackageInProgram() {
		referenceToMetamodelWithSubPackage.parseWithTestEcoreWithSubPackage.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			MainFooClass
			MainFooDataType
			MainFooEnum
			myAttribute
			myReference
			FooEnumLiteral
			mainsubpackage
			MainSubPackageFooClass
			mySubPackageAttribute
			mySubPackageReference
			mainpackage
			''')
	}

	@Test
	def void testScopeForEnamedElementInEcoreReferenceExpression() {
		"ecoreref".ecoreReferenceExpression.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			FooClass
			FooDataType
			FooEnum
			myAttribute
			myReference
			FooEnumLiteral
			FooClass
			FooDataType
			FooEnum
			myAttribute
			myReference
			FooEnumLiteral
			foo
			''')
			// duplicates because of copied EPackage
	}

	@Test
	def void testScopeForEnamedElementInEcoreReferenceExpressionQualifiedPackage() {
		"ecoreref(foo.".ecoreReferenceExpression.reference.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			FooClass
			FooDataType
			FooEnum
			FooClass
			FooDataType
			FooEnum
			''')
			// duplicate EClassifiers because of copied EPackage
	}

	@Test
	def void testScopeForEnamedElementInEcoreReferenceExpressionQualifiedEClass() {
		"ecoreref(foo.FooClass.".ecoreReferenceExpression.reference.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			myAttribute
			myReference
			myAttribute
			myReference
			''')
			// duplicate features because of copied EPackage
	}

	@Test
	def void testScopeForReferenceToCreatedEClassWithTheSameNameAsAnExistingEClass() {
		// our created EClass with the same name as an existing one must be
		// the one that is actually linked
		val prog = '''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("FooClass")
				ecoreref(FooClass)
			}
		'''.parseWithTestEcore
		val eclassExp = prog
			.lastModifyEcoreOperation.body.blockLastExpression as EdeltaEcoreReferenceExpression
		assertSame(
			// the one copied
			prog.copiedEPackages.head.firstEClass,
			eclassExp.reference.enamedelement
		)
	}

	@Test
	def void testScopeForReferenceToCopiedEPackageEClassifierAfterCreatingEClass() {
		val prog = '''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				ecoreref(FooDataType)
			}
		'''.
			parseWithTestEcore
		val eclassExp = prog
			.lastModifyEcoreOperation.body.blockLastExpression as EdeltaEcoreReferenceExpression
		val dataType = eclassExp.reference.enamedelement as EDataType
		// must be a reference to the copied EPackage's datatype
		assertSame(
			prog.copiedEPackages.head.EClassifiers.filter(EDataType).head,
			dataType
		)
	}

	@Test
	def void testScopeForReferenceToCreatedEAttribute() {
		referenceToCreatedEAttributeSimple.parseWithTestEcore
			.lastEcoreReferenceExpression.reference.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			FooClass
			FooDataType
			FooEnum
			NewClass
			myAttribute
			myReference
			FooEnumLiteral
			newAttribute
			newAttribute2
			FooClass
			FooDataType
			FooEnum
			myAttribute
			myReference
			FooEnumLiteral
			foo
			''')
		// newAttributes are the ones created in the program
		// we also have copied EPackages, that's why elements appear twice
	}

	@Test
	def void testScopeForReferenceToCreatedEAttributeChangingNameInBody() {
		referenceToCreatedEAttributeRenamed.parseWithTestEcore
			.lastEcoreReferenceExpression.reference
			.assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			FooClass
			FooDataType
			FooEnum
			NewClass
			myAttribute
			myReference
			FooEnumLiteral
			changed
			FooClass
			FooDataType
			FooEnum
			myAttribute
			myReference
			FooEnumLiteral
			foo
			''')
		// "changed" is the one created in the program (with name "newAttribute", and whose
		// name is changed in the body
	}

	@Test
	def void testScopeForModifyEcore() {
		'''
			metamodel "foo"
			metamodel "bar"
			
			modifyEcore aTest epackage foo {}
		'''.parseWithTestEcores.
			assertScope(EdeltaPackage.eINSTANCE.edeltaModifyEcoreOperation_Epackage,
			'''
			foo
			bar
			'''
			)
	}

	@Test
	def void testScopeForReferenceToCopiedEClassInModifyEcore() {
		val prog = '''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				val c = ecoreref(FooClass)
			}
		'''.
		parseWithTestEcore
		val referred = prog.modifyEcoreOperations.last.body.
			blockLastExpression.
			variableDeclaration.right.edeltaEcoreReferenceExpression.
			reference.enamedelement as EClass
		assertSame(
			// the one copied by the derived state computer
			prog.copiedEPackages.head.getEClassiferByName("FooClass"),
			referred
		)
	}

	@Test
	def void testScopeForRenamedEClassInModifyEcore() {
		'''
		metamodel "foo"
		metamodel "bar"
		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).name = "RenamedClass"
			ecoreref(foo.RenamedClass)
		}
		'''.parseWithTestEcore.lastModifyEcoreOperation.body.blockLastExpression.
			edeltaEcoreReferenceExpression.reference.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			RenamedClass
			FooDataType
			FooEnum
			FooClass
			FooDataType
			FooEnum
			''')
		// we renamed FooClass, and it can be referred
	}

	@Test
	def void testScopeForEnamedElementInEcoreReferenceExpressionReferringToRenamedEClassInModifyEcore() {
		'''
		metamodel "foo"
		metamodel "bar"
		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).name = "RenamedClass"
			ecoreref(foo.RenamedClass.)
		}
		'''.parseWithTestEcore.lastModifyEcoreOperation.body.blockLastExpression.
			edeltaEcoreReferenceExpression.reference.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			myAttribute
			myReference
			''')
		// we renamed FooClass, but its attributes are still visible through
		// the renamed class
	}

	@Test
	def void testScopeForFeaturesOfRenamedEClassInModifyEcore() {
		'''
		metamodel "foo"
		metamodel "bar"
		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).name = "RenamedClass"
			ecoreref(RenamedClass).EStructuralFeatures +=
				newEAttribute("addedAttribute")
			ecoreref(RenamedClass.)
		}
		'''.parseWithTestEcore.lastModifyEcoreOperation.body.blockLastExpression.
			edeltaEcoreReferenceExpression.reference.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			myAttribute
			myReference
			addedAttribute
			''')
		// we renamed FooClass, and added an attribute to the renamed class
	}

	@Test
	def void testLinkForRenamedEClassInModifyEcore() {
		val prog =
		'''
		metamodel "foo"

		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).name = "RenamedClass"
			ecoreref(RenamedClass)
		}
		'''.parseWithTestEcore
		prog.assertNoErrors
		val referred = prog.lastModifyEcoreOperation.body.blockLastExpression.
			edeltaEcoreReferenceExpression.reference
		val copiedEPackage = prog.copiedEPackages.head
		assertSame(
			// the one copied by the derived state computer
			copiedEPackage.getEClassiferByName("RenamedClass"),
			referred.enamedelement as EClass
		)
	}

	@Test
	def void testLinkForRenamedQualifiedEClassInModifyEcore() {
		val prog =
		'''
		metamodel "foo"

		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).name = "RenamedClass"
			ecoreref(foo.RenamedClass)
		}
		'''.parseWithTestEcore
		prog.assertNoErrors
		val referred = prog.lastModifyEcoreOperation.body.blockLastExpression.
			edeltaEcoreReferenceExpression.reference as EdeltaEcoreQualifiedReference
		val copiedEPackage = prog.copiedEPackages.head
		assertSame(
			// the one copied by the derived state computer
			copiedEPackage.getEClassiferByName("RenamedClass"),
			referred.enamedelement as EClass
		)
		assertSame(
			// the original one
			prog.metamodels.last,
			referred.qualification.enamedelement as EPackage
		)
	}

	def private assertScope(EObject context, EReference reference, CharSequence expected) {
		expected.toString.assertEqualsStrings(
			context.getScope(reference).
				allElements.
				map[name].join("\n") + "\n"
		)
	}
}
