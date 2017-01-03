package edelta.compiler

import edelta.edelta.EdeltaEAttributeExpression
import edelta.edelta.EdeltaEClassExpression
import edelta.edelta.EdeltaEClassifierExpression
import edelta.edelta.EdeltaEDataTypeExpression
import edelta.edelta.EdeltaEFeatureExpression
import edelta.edelta.EdeltaEReferenceExpression
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.compiler.XbaseCompiler
import org.eclipse.xtext.xbase.compiler.output.ITreeAppendable
import edelta.edelta.EdeltaEcoreCreateEClassExpression
import org.eclipse.emf.ecore.EPackage

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
			EdeltaEFeatureExpression: {
				compileAsStatementIfNotReferenced(appendable, isReferenced) [
					compileEdeltaEFeatureExpression(obj, appendable)
				]
			}
			EdeltaEAttributeExpression: {
				compileAsStatementIfNotReferenced(appendable, isReferenced) [
					compileEdeltaEAttributeExpression(obj, appendable)
				]
			}
			EdeltaEReferenceExpression: {
				compileAsStatementIfNotReferenced(appendable, isReferenced) [
					compileEdeltaEReferenceExpression(obj, appendable)
				]
			}
			EdeltaEcoreCreateEClassExpression: {
				compileAsStatementIfNotReferenced(appendable, isReferenced) [
					compileEdeltaCreateEClassExpression(obj, appendable)
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
			EdeltaEFeatureExpression: {
				compileEdeltaEFeatureExpression(obj, appendable)
			}
			EdeltaEAttributeExpression: {
				compileEdeltaEAttributeExpression(obj, appendable)
			}
			EdeltaEReferenceExpression: {
				compileEdeltaEReferenceExpression(obj, appendable)
			}
			default:
				super.internalToConvertedExpression(obj, appendable)
		}
	}

	private def void compileEdeltaEClassifierExpression(EdeltaEClassifierExpression obj, ITreeAppendable appendable) {
		compileEdeltaEClassifierExpressionCommon(
			obj.eclassifier, appendable, 'getEClassifier'
		)
	}

	private def void compileEdeltaEClassExpression(EdeltaEClassExpression obj, ITreeAppendable appendable) {
		compileEdeltaEClassifierExpressionCommon(
			obj.eclass, appendable, 'getEClass'
		)
	}

	private def void compileEdeltaEDataTypeExpression(EdeltaEDataTypeExpression obj, ITreeAppendable appendable) {
		compileEdeltaEClassifierExpressionCommon(
			obj.edatatype, appendable, 'getEDataType'
		)
	}

	private def void compileEdeltaEClassifierExpressionCommon(EClassifier e, ITreeAppendable appendable, String libMethodName) {
		appendable.append(
			libMethodName +
				'("' +
				getEPackageNameOrNull(e) +
				'", "' +
				e.name +
				'")'
		)
	}

	private def void compileEdeltaEFeatureExpression(EdeltaEFeatureExpression obj, ITreeAppendable appendable) {
		compileEdeltaEFeatureExpressionCommon(
			obj.efeature, appendable, "getEStructuralFeature"
		)
	}

	private def void compileEdeltaEAttributeExpression(EdeltaEAttributeExpression obj, ITreeAppendable appendable) {
		compileEdeltaEFeatureExpressionCommon(
			obj.eattribute, appendable, "getEAttribute"
		)
	}

	private def void compileEdeltaEReferenceExpression(EdeltaEReferenceExpression obj, ITreeAppendable appendable) {
		compileEdeltaEFeatureExpressionCommon(
			obj.ereference, appendable, "getEReference"
		)
	}

	private def void compileEdeltaEFeatureExpressionCommon(EStructuralFeature e, ITreeAppendable appendable, String libMethodName) {
		appendable.append(
			libMethodName + '("' +
				getEPackageNameOrNull(e.EContainingClass) +
				'", "' +
				getEClassNameOrNull(e) +
				'", "' +
				e.name + '")'
		)
	}

	private def void compileEdeltaCreateEClassExpression(EdeltaEcoreCreateEClassExpression obj, ITreeAppendable appendable) {
		appendable.append(
			'createEClass("' +
			getEPackageNameOrNull(obj.epackage) +
			'", "' +
			obj.name +
			'", null' +
			')'
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
		eClassifier?.EPackage.getEPackageNameOrNull
	}

	private def String getEPackageNameOrNull(EPackage e) {
		e?.name
	}

	private def String getEClassNameOrNull(EStructuralFeature eFeature) {
		eFeature.EContainingClass?.name
	}

}
