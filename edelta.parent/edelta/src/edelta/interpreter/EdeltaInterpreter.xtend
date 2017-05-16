package edelta.interpreter

import edelta.edelta.EdeltaEcoreCreateEClassExpression
import org.eclipse.emf.ecore.EClass
import org.eclipse.xtext.naming.QualifiedName
import org.eclipse.xtext.util.CancelIndicator
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext
import org.eclipse.xtext.xbase.interpreter.impl.XbaseInterpreter

class EdeltaInterpreter extends XbaseInterpreter {

	def run(EdeltaEcoreCreateEClassExpression e, EClass c) {
		evaluate(e,
			createContext() => [
				newValue(QualifiedName.create("it"), c)
			],
			CancelIndicator.NullImpl
		)
	}

	override protected doEvaluate(XExpression expression, IEvaluationContext context, CancelIndicator indicator) {
		if (expression instanceof EdeltaEcoreCreateEClassExpression) {
			return super.doEvaluate(expression.body, context, indicator)
		}
		return super.doEvaluate(expression, context, indicator)
	}
}
