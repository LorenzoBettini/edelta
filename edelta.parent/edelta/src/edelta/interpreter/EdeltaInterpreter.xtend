package edelta.interpreter

import edelta.edelta.EdeltaEcoreCreateEClassExpression
import org.eclipse.emf.ecore.EClass
import org.eclipse.xtext.naming.QualifiedName
import org.eclipse.xtext.util.CancelIndicator
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext
import org.eclipse.xtext.xbase.interpreter.impl.XbaseInterpreter
import org.eclipse.xtext.common.types.JvmGenericType
import org.eclipse.xtext.common.types.JvmField
import edelta.lib.EdeltaLibrary
import edelta.edelta.EdeltaEcoreReferenceExpression
import edelta.edelta.EdeltaEcoreReference

class EdeltaInterpreter extends XbaseInterpreter {

	val lib = new EdeltaLibrary

	def run(EdeltaEcoreCreateEClassExpression e, EClass c, JvmGenericType javaType) {
		evaluate(
			e,
			createContext() => [
				newValue(QualifiedName.create("it"), c)
				newValue(QualifiedName.create("this"), javaType)
				newValue(QualifiedName.create(javaType.simpleName), javaType)
			],
			CancelIndicator.NullImpl
		)
	}

	override protected doEvaluate(XExpression expression, IEvaluationContext context, CancelIndicator indicator) {
		if (expression instanceof EdeltaEcoreCreateEClassExpression) {
			return super.doEvaluate(expression.body, context, indicator)
		} else if (expression instanceof EdeltaEcoreReferenceExpression) {
			return doEvaluate(expression.reference, context, indicator)
		} else if (expression instanceof EdeltaEcoreReference) {
			return expression.enamedelement
		}
		return super.doEvaluate(expression, context, indicator)
	}

	override protected featureCallField(JvmField jvmField, Object receiver) {
		if (receiver instanceof JvmGenericType) {
			if (jvmField.simpleName == "lib") {
				return lib
			}
		}
		return super.featureCallField(jvmField, receiver)
	}

}
