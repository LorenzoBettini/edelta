package edelta.tests

import com.google.inject.Inject
import edelta.edelta.EdeltaEcoreChangeEClassExpression
import edelta.resource.EdeltaChangeRunner
import org.eclipse.emf.ecore.EClass
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaChangeRunnerTest extends EdeltaAbstractTest {

	@Inject extension EdeltaChangeRunner

	@Test
	def void testNoChanges() {
		referenceToChangedEClassWithTheSameNameAsAnExistingEClass.
			applyChangesAndAssert["FooClass".assertEqualsStrings(name)]
	}

	@Test
	def void testChangeName() {
		referenceToChangedEClassWithANewName.
			applyChangesAndAssert["RenamedClass".assertEqualsStrings(name)]
	}

	def private applyChangesAndAssert(CharSequence input, (EClass)=>void testExecution) {
		val program = input.parseWithTestEcore
		val changeExp = program.
			main.expressions.
			filter(EdeltaEcoreChangeEClassExpression).last
		val original = changeExp.original
		original.performChanges(changeExp)
		testExecution.apply(original)
	}

}
