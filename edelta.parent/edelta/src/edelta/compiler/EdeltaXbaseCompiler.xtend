package edelta.compiler

import org.eclipse.xtext.xbase.compiler.XbaseCompiler
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.compiler.output.ITreeAppendable
import edelta.edelta.EdeltaEClassExpression

class EdeltaXbaseCompiler extends XbaseCompiler {

	override protected doInternalToJavaStatement(XExpression obj, ITreeAppendable appendable, boolean isReferenced) {
		switch (obj) {
			EdeltaEClassExpression: {
				appendable.append('"temp"')
			}
			default:
				super.doInternalToJavaStatement(obj, appendable, isReferenced)
		}
	}

	override protected internalToConvertedExpression(XExpression obj, ITreeAppendable appendable) {
		switch (obj) {
			EdeltaEClassExpression: {
				appendable.append('"temp"')
			}
			default:
				super.internalToConvertedExpression(obj, appendable)
		}
	}

}
