/*
 * generated by Xtext 2.13.0
 */
package edelta.formatting2

import edelta.edelta.EdeltaOperation
import edelta.edelta.EdeltaProgram
import edelta.edelta.EdeltaUseAs
import org.eclipse.xtext.formatting2.IFormattableDocument
import org.eclipse.xtext.xbase.annotations.formatting2.XbaseWithAnnotationsFormatter
import static edelta.edelta.EdeltaPackage.Literals.*
import edelta.edelta.EdeltaModifyEcoreOperation
import edelta.edelta.EdeltaEcoreReferenceExpression

class EdeltaFormatter extends XbaseWithAnnotationsFormatter {

	def dispatch void format(EdeltaProgram edeltaProgram, extension IFormattableDocument document) {
		edeltaProgram.prepend[setNewLines(0, 0, 1); noSpace].append[newLine]
		edeltaProgram.getImportSection.format;
		edeltaProgram.regionFor.keyword("package").append[oneSpace]
		edeltaProgram.regionFor.keywords("metamodel").forEach[append[oneSpace]]
		for (EdeltaUseAs edeltaUseAs : edeltaProgram.getUseAsClauses()) {
			edeltaUseAs.format
			edeltaUseAs.append[setNewLines(1, 1, 2)]
		}
		for (EdeltaOperation edeltaOperation : edeltaProgram.getOperations()) {
			edeltaOperation.format;
			edeltaOperation.append[setNewLines(1, 1, 2)]
		}
		for (modifyEcore : edeltaProgram.modifyEcoreOperations) {
			modifyEcore.format
		}
	}

	def dispatch void format(EdeltaUseAs useAs, extension IFormattableDocument document) {
		useAs.regionFor.keyword("use").append[oneSpace]
		useAs.type.append[oneSpace]
		useAs.type.format
		useAs.regionFor.keyword("as").append[oneSpace]
		useAs.regionFor.keyword("extension").append[oneSpace]
	}

	def dispatch void format(EdeltaOperation operation, extension IFormattableDocument document) {
		operation.regionFor.keyword("def").append[oneSpace]
		operation.regionFor.keyword("(").surround[noSpace]
		if (!operation.params.isEmpty) {
			for (comma : operation.regionFor.keywords(","))
				comma.prepend[noSpace].append[oneSpace]
			for (params : operation.params)
				params.format
			operation.regionFor.keyword(")").prepend[noSpace]
		}
		if (operation.type !== null) {
			operation.regionFor.keyword(")").append[oneSpace]
			operation.type.prepend[oneSpace].append[oneSpace]
			operation.type.format
		} else {
			operation.regionFor.keyword(")").append[oneSpace]
		}
		operation.body.format
	}

	def dispatch void format(EdeltaModifyEcoreOperation operation, extension IFormattableDocument document) {
		operation.regionFor.keyword("modifyEcore").append[oneSpace]
		operation.regionFor.keyword("epackage").surround[oneSpace]
		operation.regionFor.feature(EDELTA_MODIFY_ECORE_OPERATION__EPACKAGE).surround[oneSpace]
		operation.body.format
	}

	def dispatch void format(EdeltaEcoreReferenceExpression ecoreref, extension IFormattableDocument document) {
		ecoreref.regionFor.keyword("(").surround[noSpace]
		for (node : ecoreref.reference.regionFor.keywords("."))
			node.surround[noSpace]
		ecoreref.regionFor.keyword(")").surround[noSpace]
	}

}
