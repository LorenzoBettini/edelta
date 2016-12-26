package edelta.compiler

import org.eclipse.xtext.xbase.compiler.XbaseCompiler
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.compiler.output.ITreeAppendable
import edelta.edelta.EdeltaEClassExpression

class EdeltaXbaseCompiler extends XbaseCompiler {

	override protected doInternalToJavaStatement(XExpression obj, ITreeAppendable appendable, boolean isReferenced) {
		switch (obj) {
			EdeltaEClassExpression: {
				if (!isReferenced) {
					appendable.newLine
					compileEdeltaEClassExpression(obj, appendable)
					appendable.append(";")
				}
			}
			default:
				super.doInternalToJavaStatement(obj, appendable, isReferenced)
		}
	}

	override protected internalToConvertedExpression(XExpression obj, ITreeAppendable appendable) {
		switch (obj) {
			EdeltaEClassExpression: {
				compileEdeltaEClassExpression(obj, appendable)
			}
			default:
				super.internalToConvertedExpression(obj, appendable)
		}
	}

	private def void compileEdeltaEClassExpression(EdeltaEClassExpression obj, ITreeAppendable appendable) {
		val eClass = obj.eclass
		appendable.append(
			'getEClass("' + eClass.EPackage?.name + '", "' + eClass.name + '")'
		)
	}
}
