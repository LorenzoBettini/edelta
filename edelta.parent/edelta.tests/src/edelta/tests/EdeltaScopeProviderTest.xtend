package edelta.tests

import com.google.inject.Inject
import edelta.edelta.EdeltaEcoreQualifiedReference
import edelta.edelta.EdeltaEcoreReferenceExpression
import edelta.edelta.EdeltaPackage
import org.eclipse.emf.ecore.EAttribute
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
		metamodel "foo"
		
		this.
		'''.parse.lastExpression.getScope(EdeltaPackage.eINSTANCE.edeltaProgram_Main)
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
	def void testScopeForEnamedElementInEcoreReferenceExpression() {
		'''
		metamodel "foo"
		ecoreref 
		'''.parseWithTestEcore.lastExpression.
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
	def void testScopeForEnamedElementInEcoreReferenceExpressionWithTwoMetamodels() {
		'''
		metamodel "foo"
		metamodel "bar"
		ecoreref 
		'''.parseWithTestEcores.lastExpression.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			FooClass
			FooDataType
			FooEnum
			myAttribute
			myReference
			FooEnumLiteral
			BarClass
			BarDataType
			myAttribute
			myReference
			foo
			bar
			''')
	}

	@Test
	def void testScopeForEnamedElementInEcoreReferenceExpressionQualifiedPackage() {
		'''
		metamodel "foo"
		metamodel "bar"
		ecoreref foo.
		'''.parseWithTestEcores.
			lastExpression.
			edeltaEcoreReferenceExpression.reference.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			FooClass
			FooDataType
			FooEnum
			''')
	}

	@Test
	def void testScopeForEnamedElementInEcoreReferenceExpressionQualifiedEClass() {
		'''
		metamodel "foo"
		metamodel "bar"
		ecoreref foo.FooClass.
		'''.parseWithTestEcore.lastExpression.
			edeltaEcoreReferenceExpression.reference.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			myAttribute
			myReference
			''')
	}

	@Test
	def void testScopeForReferenceToEClass() {
		val prog = referenceToEClass.
			parseWithTestEcore
		val expressions = prog.main.expressions
		val eclassExp = expressions.last as EdeltaEcoreReferenceExpression
		assertSame(
			prog.getEClassifierByName("foo", "FooClass"),
			eclassExp.reference.enamedelement
		)
	}

	@Test
	def void testScopeForReferenceToCreatedEClassWithTheSameNameAsAnExistingEClass() {
		// our created EClass with the same name as an existing one must be
		// the one that is actually linked
		val prog = referenceToCreatedEClassWithTheSameNameAsAnExistingEClass.
			parseWithTestEcore
		val expressions = prog.main.expressions
		val eclassExp = expressions.last as EdeltaEcoreReferenceExpression
		assertSame(
			// the one created by the derived state computer
			prog.derivedStateLastEClass,
			eclassExp.reference.enamedelement
		)
	}

	@Test
	def void testScopeForReferenceToCopiedEPackageEClassifierAfterCreatingEClass() {
		val prog = createEClassAndReferenceToExistingEDataType.
			parseWithTestEcore
		val expressions = prog.main.expressions
		val eclassExp = expressions.last as EdeltaEcoreReferenceExpression
		val dataType = eclassExp.reference.enamedelement as EDataType
		// must be a reference to the copied EPackage's datatype
		assertSame(
			prog.copiedEPackages.head.EClassifiers.filter(EDataType).head,
			dataType
		)
	}

	@Test
	def void testScopeForFullyQualifiedReferenceToCopiedEPackageEClassifierAfterCreatingEClass() {
		val prog = createEClassAndReferenceToExistingEDataTypeFullyQualified.
			parseWithTestEcore
		val expressions = prog.main.expressions
		val eclassExp = expressions.last as EdeltaEcoreReferenceExpression
		val dataType = eclassExp.reference.enamedelement as EDataType
		// must be a reference to the copied EPackage's datatype
		assertSame(
			prog.copiedEPackages.head.EClassifiers.filter(EDataType).head,
			dataType
		)
	}

	@Test
	def void testScopeForReferenceToCreatedEAttribute() {
		referenceToCreatedEAttributeSimple.parseWithTestEcore.lastExpression.
			edeltaEcoreReferenceExpression.reference.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			NewClass
			newAttribute
			newAttribute2
			NewClass
			FooClass
			FooDataType
			FooEnum
			newAttribute
			newAttribute2
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
		// newAttributes is the one created in the program
		// we also have copied EPackages, that's why elements appear twice
	}

	@Test
	def void testScopeForReferenceToCreatedEAttributeChangingNameInBody() {
		referenceToCreatedEAttributeRenamed.parseWithTestEcore
			.lastModifyEcoreOperation.body.block.expressions
			.last
			.edeltaEcoreReferenceExpression.reference
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
	def void testScopeForReferenceToEPackageInChangeEClass() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo {}
			changeEClass foo.Test {}
		'''
		.parseWithTestEcore.lastExpression.
			changeEClassExpression.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreBaseEClassManipulationWithBlockExpression_Epackage,
			'''
			foo
			''')
	}

	@Test
	def void testScopeForReferenceToEClassInChangeEClass() {
		'''
			metamodel "foo"
			metamodel "bar"
			
			createEClass NewClass in foo {}
			changeEClass foo.Test {}
		'''
		.parseWithTestEcore.lastExpression.
			changeEClassExpression.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreChangeEClassExpression_Original,
			'''
			FooClass
			''')
		// created EClass are not in the scope for changeEClass
	}

	@Test
	def void testScopeForReferenceToChangedEClassWithTheSameNameAsAnExistingEClass() {
		// our changed EClass with the same name as an existing one must be
		// the one that is actually linked
		val prog = referenceToChangedEClassWithTheSameNameAsAnExistingEClass.
			parseWithTestEcore
		val expressions = prog.main.expressions
		val eclassExp = expressions.last.edeltaEcoreReferenceExpression
		assertSame(
			// the one created by the derived state computer
			prog.derivedStateLastEClass,
			eclassExp.reference.enamedelement
		)
	}

	@Test
	def void testScopeForReferenceToChangedEClassCopiedAttribute() {
		// our changed EClass referred attribute must be the one
		// of the copy, not the original one
		val prog = referenceToChangedEClassCopiedAttribute.
			parseWithTestEcore
		val expressions = prog.main.expressions
		val changeEClass = expressions.last.changeEClassExpression
		val referredAttr = changeEClass.body.expressions.
			last.variableDeclaration.right.edeltaEcoreReferenceExpression.
			reference.enamedelement as EAttribute
		assertSame(
			// the one created by the derived state computer
			prog.derivedStateLastEClass.EStructuralFeatures.head,
			referredAttr
		)
	}

	@Test
	def void testScopeForReferenceToCopiedEClassAfterCreatingEClass() {
		val prog = '''
			metamodel "foo"
			
			createEClass NewClass in foo {
				val c = ecoreref(FooClass)
			}
		'''.
		parseWithTestEcore
		val expressions = prog.main.expressions
		val changeEClass = expressions.last.createEClassExpression
		val referred = changeEClass.body.expressions.
			last.variableDeclaration.right.edeltaEcoreReferenceExpression.
			reference.enamedelement as EClass
		assertSame(
			// the one copied by the derived state computer
			prog.copiedEPackages.head.getEClassiferByName("FooClass"),
			referred
		)
	}

	@Test
	def void testScopeForEnamedElementInEcoreReferenceExpressionReferringToRenamedEClass() {
		'''
		metamodel "foo"
		metamodel "bar"
		changeEClass foo.FooClass newName RenamedClass {}
		ecoreref(foo.RenamedClass.
		'''.parseWithTestEcore.lastExpression.
			edeltaEcoreReferenceExpression.reference.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			myAttribute
			myReference
			myAttribute
			myReference
			''')
		// we renamed FooClass, but its attributes are still visible through
		// the renamed class
		// they're duplicate since we also have the ones of the copied EPackages
		// Note that they appear twice and not 3 times, because we only select
		// those in RenamedClass, not also the ones in FooClass.
		// foo in foo.RenamedClass refers to the derived state EPackage
		// and edelta.util.EdeltaEcoreHelper.getEPackageENamedElementsInternal(EPackage, EObject, boolean)
		// does not consider the passed EPackage as the program imported metamodel
		// so it does not risk using the passed EPackage and the retrieved derived state
		// epackage twice for retrieving EClassifiers.
	}

	@Test
	def void testScopeForEnamedElementInEcoreReferenceExpressionReferringToRenamedEClassInsideChangeEClass() {
		'''
		metamodel "foo"
		metamodel "bar"
		changeEClass foo.FooClass newName RenamedClass {
			ecoreref(RenamedClass.
		}
		'''.parseWithTestEcore.lastExpression.
			changeEClassExpression.body.expressions.last.
			edeltaEcoreReferenceExpression.reference.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			myAttribute
			myReference
			myAttribute
			myReference
			''')
		// we renamed FooClass, but its attributes are still visible through
		// the renamed class
		// they're duplicate since we also have the ones of the copied EPackages
		// Note that they appear twice and not 3 times, because we only select
		// those in RenamedClass, not also the ones in FooClass.
		// foo in foo.RenamedClass refers to the derived state EPackage
		// and edelta.util.EdeltaEcoreHelper.getEPackageENamedElementsInternal(EPackage, EObject, boolean)
		// does not consider the passed EPackage as the program imported metamodel
		// so it does not risk using the passed EPackage and the retrieved derived state
		// epackage twice for retrieving EClassifiers.
	}

	@Test
	def void testScopeForEnamedElementInEcoreReferenceExpressionReferringToRenamedEClassInsideChangeEClass2() {
		'''
		metamodel "foo"
		metamodel "bar"
		changeEClass foo.FooClass {
			name = "RenamedClass"
			ecoreref(RenamedClass.
		}
		'''.parseWithTestEcore.lastExpression.
			changeEClassExpression.body.expressions.last.
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
	def void testScopeForFeaturesOfRenamedEClass() {
		'''
		metamodel "foo"
		metamodel "bar"
		changeEClass foo.FooClass {
			name = "RenamedClass"
			ecoreref(RenamedClass).EStructuralFeatures +=
				newEAttribute("addedAttribute")
			ecoreref(RenamedClass.)
		}
		'''.parseWithTestEcore.lastExpression.
			changeEClassExpression.body.expressions.last.
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
