package edelta.tests

import com.google.inject.Inject
import edelta.edelta.EdeltaEcoreQualifiedReference
import edelta.edelta.EdeltaEcoreReferenceExpression
import edelta.edelta.EdeltaPackage
import edelta.tests.injectors.EdeltaInjectorProviderCustom
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
	def void testSuperScope() throws Exception {
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
	def void testScopeForMetamodel() throws Exception {
		referenceToMetamodel.parseWithTestEcore.
			assertScope(EdeltaPackage.eINSTANCE.edeltaProgram_Metamodels,
			'''
			foo
			'''
			)
		// we skip nsURI references, like http://foo
	}

	@Test
	def void testScopeForMetamodels() throws Exception {
		referencesToMetamodels.parseWithTestEcores.
			assertScope(EdeltaPackage.eINSTANCE.edeltaProgram_Metamodels,
			'''
			foo
			bar
			'''
			)
	}

	@Test
	def void testScopeForEnamedElementInProgram() throws Exception {
		referenceToMetamodelWithCopiedEPackage
			.parseWithTestEcore.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			foo
			FooClass
			myAttribute
			myReference
			FooDataType
			FooEnum
			FooEnumLiteral
			''')
	}

	@Test
	def void testScopeForEnamedElementWithSubPackageInProgram() throws Exception {
		// MyClass with myClassAttribute
		// is present in the package and in subpackages
		// so it appears several times
		referenceToMetamodelWithSubPackageWithCopiedEPackages
			.parseWithTestEcoreWithSubPackage.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			mainpackage
			MainFooClass
			myAttribute
			myReference
			MainFooDataType
			MainFooEnum
			FooEnumLiteral
			MyClass
			myClassAttribute
			mainsubpackage
			MainSubPackageFooClass
			mySubPackageAttribute
			mySubPackageReference
			MyClass
			myClassAttribute
			subsubpackage
			MyClass
			''')
	}

	@Test
	def void testScopeForEnamedElementInEcoreReferenceExpressionWithSubPackages() throws Exception {
		'''
			metamodel "mainpackage"
			
			modifyEcore aTest epackage mainpackage {
				ecoreref(
			}
		'''.parseWithTestEcoreWithSubPackage
			.lastEcoreReferenceExpression
			.reference
			.assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			mainpackage
			MainFooClass
			myAttribute
			myReference
			MainFooDataType
			MainFooEnum
			FooEnumLiteral
			MyClass
			myClassAttribute
			mainsubpackage
			MainSubPackageFooClass
			mySubPackageAttribute
			mySubPackageReference
			MyClass
			myClassAttribute
			subsubpackage
			MyClass
			''')
	}

	@Test
	def void testScopeForSubPackageInEcoreReferenceExpressionWithSubPackages() throws Exception {
		'''
			metamodel "mainpackage"
			
			modifyEcore aTest epackage mainpackage {
				ecoreref(mainsubpackage.
			}
		'''.parseWithTestEcoreWithSubPackage
			.lastEcoreReferenceExpression
			.reference
			.assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			MainSubPackageFooClass
			MyClass
			subsubpackage
			''')
	}

	@Test
	def void testScopeForSubPackageEClassInEcoreReferenceExpressionWithSubPackages() throws Exception {
		'''
			metamodel "mainpackage"
			
			modifyEcore aTest epackage mainpackage {
				ecoreref(mainsubpackage.MainSubPackageFooClass.
			}
		'''.parseWithTestEcoreWithSubPackage
			.lastEcoreReferenceExpression
			.reference
			.assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			mySubPackageAttribute
			mySubPackageReference
			''')
	}

	@Test
	def void testScopeForSubSubPackageInEcoreReferenceExpressionWithSubPackages() throws Exception {
		'''
			metamodel "mainpackage"
			
			modifyEcore aTest epackage mainpackage {
				ecoreref(mainsubpackage.subsubpackage.
			}
		'''.parseWithTestEcoreWithSubPackage
			.lastEcoreReferenceExpression
			.reference
			.assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			MyClass
			''')
	}

	@Test
	def void testScopeForEnamedElementInEcoreReferenceExpression() throws Exception {
		"ecoreref(".ecoreReferenceExpression.reference.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			foo
			FooClass
			myAttribute
			myReference
			FooDataType
			FooEnum
			FooEnumLiteral
			''')
	}

	@Test
	def void testScopeForEnamedElementInEcoreReferenceExpressionQualifiedPackage() throws Exception {
		"ecoreref(foo.".ecoreReferenceExpression.reference.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			FooClass
			FooDataType
			FooEnum
			''')
	}

	@Test
	def void testScopeForEnamedElementInEcoreReferenceExpressionQualifiedEClass() throws Exception {
		"ecoreref(foo.FooClass.".ecoreReferenceExpression.reference.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			myAttribute
			myReference
			''')
	}

	@Test
	def void testScopeForReferenceToCreatedEClassWithTheSameNameAsAnExistingEClass() throws Exception {
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
			.lastModifyEcoreOperationLastExpression as EdeltaEcoreReferenceExpression
		assertSame(
			// the one copied
			prog.copiedEPackages.head.firstEClass,
			eclassExp.reference.enamedelement
		)
	}

	@Test
	def void testScopeForReferenceToCopiedEPackageEClassifierInModifyEcore() throws Exception {
		val prog = '''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				ecoreref(FooDataType)
			}
		'''.
			parseWithTestEcore
		val eclassExp = prog
			.lastModifyEcoreOperationLastExpression as EdeltaEcoreReferenceExpression
		val dataType = eclassExp.reference.enamedelement as EDataType
		// must be a reference to the copied EPackage's datatype
		assertSame(
			prog.copiedEPackages.head.EClassifiers.filter(EDataType).head,
			dataType
		)
	}

	@Test
	def void testScopeForReferenceToOriginalEPackageEClassifierInOperation() throws Exception {
		val prog = '''
			metamodel "foo"
			
			def anOp() {
				ecoreref(FooDataType)
			}
		'''.
			parseWithTestEcore
		val eclassExp = prog
			.lastOperation.body.blockLastExpression as EdeltaEcoreReferenceExpression
		val dataType = eclassExp.reference.enamedelement as EDataType
		// must be a reference to the original EPackage's datatype
		assertSame(
			prog.metamodels.head.EClassifiers.filter(EDataType).head,
			dataType
		)
	}

	@Test
	def void testScopeForReferenceToCopiedEPackageEClassifierInOperationWhenTheresAModifyEcore() throws Exception {
		val prog = '''
			metamodel "foo"
			
			def anOp() {
				ecoreref(FooDataType)
			}
			
			// this triggers copied EPackage
			modifyEcore aTest epackage foo {
				ecoreref(FooDataType)
			}
		'''.
			parseWithTestEcore
		val eclassExp = prog
			.lastOperation.body.blockLastExpression as EdeltaEcoreReferenceExpression
		val dataType = eclassExp.reference.enamedelement as EDataType
		// must be a reference to the copied EPackage's datatype
		assertSame(
			prog.copiedEPackages.head.EClassifiers.filter(EDataType).head,
			dataType
		)
	}

	@Test
	def void testScopeForReferenceToCreatedEAttribute() throws Exception {
		referenceToCreatedEAttributeSimple.parseWithTestEcore
			.lastEcoreReferenceExpression.reference.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			foo
			FooClass
			myAttribute
			myReference
			FooDataType
			FooEnum
			FooEnumLiteral
			NewClass
			newAttribute
			newAttribute2
			''')
		// newAttributes are the ones created in the program
	}

	@Test
	def void testScopeForReferenceToCreatedEAttributeChangingNameInBody() throws Exception {
		referenceToCreatedEAttributeRenamed.parseWithTestEcore
			.lastEcoreReferenceExpression.reference
			.assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			foo
			FooClass
			myAttribute
			myReference
			FooDataType
			FooEnum
			FooEnumLiteral
			NewClass
			changed
			''')
		// "changed" is the one created in the program (with name "newAttribute", and whose
		// name is changed in the body
	}

	@Test
	def void testScopeForModifyEcore() throws Exception {
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
	def void testScopeForRenamedEClassInModifyEcore() throws Exception {
		'''
		metamodel "foo"
		metamodel "bar"
		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).name = "RenamedClass"
			ecoreref(foo.RenamedClass)
		}
		'''.parseWithTestEcore.lastModifyEcoreOperationLastExpression.
			edeltaEcoreReferenceExpression.reference.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			RenamedClass
			FooDataType
			FooEnum
			''')
		// we renamed FooClass, and it can be referred
	}

	@Test
	def void testScopeForEnamedElementInEcoreReferenceExpressionReferringToRenamedEClassInModifyEcore() throws Exception {
		'''
		metamodel "foo"
		metamodel "bar"
		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).name = "RenamedClass"
			ecoreref(foo.RenamedClass.)
		}
		'''.parseWithTestEcore.lastModifyEcoreOperationLastExpression.
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
	def void testScopeForFeaturesOfRenamedEClassInModifyEcore() throws Exception {
		'''
		metamodel "foo"
		metamodel "bar"
		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).name = "RenamedClass"
			ecoreref(RenamedClass).EStructuralFeatures +=
				newEAttribute("addedAttribute", ecoreref(FooDataType))
			ecoreref(RenamedClass.)
		}
		'''.parseWithTestEcore.lastModifyEcoreOperationLastExpression.
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
	def void testLinkForRenamedEClassInModifyEcore() throws Exception {
		val prog =
		'''
		metamodel "foo"

		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).name = "RenamedClass"
			ecoreref(RenamedClass)
		}
		'''.parseWithTestEcore
		prog.assertNoErrors
		val referred = prog.lastModifyEcoreOperationLastExpression.
			edeltaEcoreReferenceExpression.reference
		val copiedEPackage = prog.copiedEPackages.head
		assertSame(
			// the one copied by the derived state computer
			copiedEPackage.getEClassiferByName("RenamedClass"),
			referred.enamedelement as EClass
		)
	}

	@Test
	def void testLinkForRenamedQualifiedEClassInModifyEcore() throws Exception {
		val prog =
		'''
		metamodel "foo"

		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).name = "RenamedClass"
			ecoreref(foo.RenamedClass)
		}
		'''.parseWithTestEcore
		prog.assertNoErrors
		val referred = prog.lastModifyEcoreOperationLastExpression.
			edeltaEcoreReferenceExpression.reference as EdeltaEcoreQualifiedReference
		val copiedEPackage = prog.copiedEPackages.head
		assertSame(
			// the one copied by the derived state computer
			copiedEPackage.getEClassiferByName("RenamedClass"),
			referred.enamedelement as EClass
		)
		assertSame(
			// the one copied by the derived state computer
			// NOT the original one
			copiedEPackage,
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
