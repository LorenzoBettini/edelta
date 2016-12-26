package edelta.compiler

import edelta.edelta.EdeltaEClassExpression
import edelta.edelta.EdeltaEClassifierExpression
import edelta.edelta.EdeltaEDataTypeExpression
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.compiler.XbaseCompiler
import org.eclipse.xtext.xbase.compiler.output.ITreeAppendable

class EdeltaXbaseCompiler extends XbaseCompiler {

	override protected doInternalToJavaStatement(XExpression obj, ITreeAppendable appendable, boolean isReferenced) {
		switch (obj) {
			EdeltaEClassExpression: {
				compileAsStatementIfNotReferenced(appendable, isReferenced) [
					compileEdeltaEClassExpression(obj, appendable)
				]
			}
			EdeltaEClassifierExpression: {
				compileAsStatementIfNotReferenced(appendable, isReferenced) [
					compileEdeltaEClassifierExpression(obj, appendable)
				]
			}
			EdeltaEDataTypeExpression: {
				compileAsStatementIfNotReferenced(appendable, isReferenced) [
					compileEdeltaEDataTypeExpression(obj, appendable)
				]
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
			EdeltaEClassifierExpression: {
				compileEdeltaEClassifierExpression(obj, appendable)
			}
			EdeltaEDataTypeExpression: {
				compileEdeltaEDataTypeExpression(obj, appendable)
			}
			default:
				super.internalToConvertedExpression(obj, appendable)
		}
	}

	private def void compileEdeltaEClassifierExpression(EdeltaEClassifierExpression obj, ITreeAppendable appendable) {
		val e = obj.eclassifier
		appendable.append(
			'getEClassifier("' + getEPackageNameOrNull(e) + '", "' + e.name + '")'
		)
	}

	private def void compileEdeltaEClassExpression(EdeltaEClassExpression obj, ITreeAppendable appendable) {
		val e = obj.eclass
		appendable.append(
			'getEClass("' + getEPackageNameOrNull(e) + '", "' + e.name + '")'
		)
	}

	private def void compileEdeltaEDataTypeExpression(EdeltaEDataTypeExpression obj, ITreeAppendable appendable) {
		val e = obj.edatatype
		appendable.append(
			'getEDataType("' + getEPackageNameOrNull(e) + '", "' + e.name + '")'
		)
	}

	private def void compileAsStatementIfNotReferenced(ITreeAppendable appendable, boolean isReferenced,
		()=>void compileLambda) {
		if (!isReferenced) {
			appendable.newLine
			compileLambda.apply
			appendable.append(";")
		}
	}

	private def String getEPackageNameOrNull(EClassifier eClassifier) {
		eClassifier.EPackage?.name
	}
}
