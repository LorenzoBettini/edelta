package edelta.tests

import com.google.inject.Inject
import edelta.edelta.EdeltaEcoreReferenceExpression
import edelta.edelta.EdeltaPackage
import org.eclipse.emf.ecore.EObject
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
	def void testScopeForReferenceToCreatedEAttribute() {
		referenceToCreatedEAttribute.parseWithTestEcore.lastExpression.
			edeltaEcoreReferenceExpression.reference.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			NewClass
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
		val eclassExp = expressions.last as EdeltaEcoreReferenceExpression
		assertSame(
			// the one created by the derived state computer
			prog.derivedStateLastEClass,
			eclassExp.reference.enamedelement
		)
	}

	@Test
	def void testScopeForReferenceToChangedEClassWithNewName() {
		// our changed EClass with the new name must be
		// the one that is actually linked
		val prog = referenceToChangedEClassWithANewName.
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
	def void testScopeForReferenceToChangedEClassWithNewName2() {
		// our changed EClass with the same name as an existing one must be
		// the one that is actually linked
		referenceToChangedEClassWithANewName.
			parseWithTestEcore.lastExpression.
			edeltaEcoreReferenceExpression.reference.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			RenamedClass
			myAttribute
			myReference
			anotherAttr
			FooClass
			FooDataType
			FooEnum
			myAttribute
			myReference
			FooEnumLiteral
			foo
			''')
			// RenamedClass and FooClass (the original referred) are both returned
			// by the scope provider
			// anotherAttr is created in the changeEClass expression
	}

	@Test
	def void testScopeForEnamedElementInEcoreReferenceExpressionReferringToRenamedEClass() {
		'''
		metamodel "foo"
		metamodel "bar"
		changeEClass foo.FooClass newName RenamedClass {}
		ecoreref foo.RenamedClass.
		'''.parseWithTestEcore.lastExpression.
			edeltaEcoreReferenceExpression.reference.
			assertScope(EdeltaPackage.eINSTANCE.edeltaEcoreReference_Enamedelement,
			'''
			myAttribute
			myReference
			''')
		// we renamed FooClass, but its attributes are still visible through
		// the renamed class
	}

	def private assertScope(EObject context, EReference reference, CharSequence expected) {
		expected.toString.assertEqualsStrings(
			context.getScope(reference).
				allElements.
				map[name].join("\n") + "\n"
		)
	}
}
