package edelta.interpreter

import org.eclipse.xtext.xbase.interpreter.impl.XbaseInterpreter
import edelta.edelta.EdeltaEcoreCreateEClassExpression
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext
import org.eclipse.xtext.util.CancelIndicator

class EdeltaInterpreter extends XbaseInterpreter {

	def run(EdeltaEcoreCreateEClassExpression e) {
		evaluate(e)
	}
	
	override protected doEvaluate(XExpression expression, IEvaluationContext context, CancelIndicator indicator) {
		if (expression instanceof EdeltaEcoreCreateEClassExpression) {
			return super.doEvaluate(expression.body, context, indicator)
		}
		return super.doEvaluate(expression, context, indicator)
	}
}
