/**
 * generated by Xtext 2.10.0
 */
package edelta.ui.quickfix;

import static edelta.util.EdeltaModelUtil.getContainingBlockXExpression;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.ui.editor.model.edit.IModificationContext;
import org.eclipse.xtext.ui.editor.quickfix.Fix;
import org.eclipse.xtext.ui.editor.quickfix.IssueResolutionAcceptor;
import org.eclipse.xtext.validation.Issue;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.annotations.ui.quickfix.XbaseWithAnnotationsQuickfixProvider;

import com.google.inject.Inject;

import edelta.edelta.EdeltaEcoreReference;
import edelta.edelta.EdeltaProgram;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.util.EdeltaModelUtil;
import edelta.validation.EdeltaValidator;

/**
 * Custom quickfixes.
 * 
 * See
 * https://www.eclipse.org/Xtext/documentation/310_eclipse_support.html#quick-fixes
 */
public class EdeltaQuickfixProvider extends XbaseWithAnnotationsQuickfixProvider {

	private static final String E_OBJECT_GIF = "EObject.gif";

	@Inject
	private EdeltaDerivedStateHelper derivedStateHelper;

	@Fix(EdeltaValidator.INVALID_SUBPACKAGE_IMPORT)
	public void importRootPackage(final Issue issue, final IssueResolutionAcceptor acceptor) {
		final String rootPackageName = issue.getData()[0];
		acceptor.accept(
			issue,
			"Import root EPackage",
			"Import root EPackage \'" + rootPackageName + "\'",
			"EPackage.gif",
			context -> 
				context.getXtextDocument().replace(
					issue.getOffset(),
					issue.getLength(),
					"\"" + rootPackageName + "\"")
		);
	}

	@Fix(EdeltaValidator.INTERPRETER_ACCESS_RENAMED_ELEMENT)
	public void useRenamedElement(final Issue issue, final IssueResolutionAcceptor acceptor) {
		final String renamed = issue.getData()[0];
		acceptor.accept(
			issue,
			"Use renamed element",
			"Use renamed element \'" + renamed + "\'",
			E_OBJECT_GIF,
			context -> 
				context.getXtextDocument().replace(
					issue.getOffset(),
					issue.getLength(),
					renamed)
		);
	}

	@Fix(EdeltaValidator.AMBIGUOUS_REFERENCE)
	public void fixEcoreRefAmbiguity(final Issue issue, final IssueResolutionAcceptor acceptor) {
		final String[] alternatives = issue.getData();
		for (String alternative : alternatives) {
			acceptor.accept(
				issue,
				"Fix ambiguity with \'" + alternative + "\'",
				"Fix ambiguity with \'" + alternative + "\'",
				E_OBJECT_GIF,
				context -> 
					context.getXtextDocument().replace(
						issue.getOffset(),
						issue.getLength(),
						alternative)
			);
		}
	}

	@Fix(EdeltaValidator.DUPLICATE_METAMODEL_IMPORT)
	public void removeDuplicateMetamodelImport(final Issue issue, final IssueResolutionAcceptor acceptor) {
		final int importToRemove = Integer.parseInt(issue.getData()[0]);
		acceptor.accept(
			issue,
			"Remove duplicate metamodel import",
			"Remove duplicate metamodel import",
			"EPackage.gif",
			(EObject element, IModificationContext context) -> {
				INode node = EdeltaModelUtil.getMetamodelImportNodes
					((EdeltaProgram) element).get(importToRemove);
				// the node corresponding to the keyword 'metamodel'
				INode metamodelNode = node.getPreviousSibling().getPreviousSibling();
				int offset = metamodelNode.getOffset();
				// also remove newline, in an OS independent way
				int endOfLineLength = context.getXtextDocument()
					.getLineDelimiter(node.getEndLine()).length();
				int length = node.getTotalEndOffset() - offset +
					endOfLineLength;
				context.getXtextDocument().replace(offset, length, "");
			}
		);
	}

	/**
	 * In case of a forward reference, that is, reference to an element that does
	 * not yet exist in a given context, the quickfix moves the expression
	 * containing the forward reference after the expression that creates such an
	 * element.
	 * 
	 * @param issue
	 * @param acceptor
	 */
	@Fix(EdeltaValidator.INTERPRETER_ACCESS_NOT_YET_EXISTING_ELEMENT)
	public void moveToRightPosition(final Issue issue, final IssueResolutionAcceptor acceptor) {
		acceptor.accept(
			issue,
			"Move to the right position",
			"Move to the right position",
			E_OBJECT_GIF,
			(EObject element, IModificationContext context) -> {
				var forwardEcoreRef = (EdeltaEcoreReference) element;
				// the expression to move
				var expToMove = getContainingBlockXExpression(forwardEcoreRef);
				// the block containing the expression to move
				var containingBlock = (XBlockExpression) expToMove.eContainer();
				// the expression that creates the element referred by the forward reference
				var responsibleExpression =
					derivedStateHelper
					.getLastResponsibleExpression(element, forwardEcoreRef.getEnamedelement());
				var responsibleExpressionBlockExp = getContainingBlockXExpression(responsibleExpression);
				// the block where we want to move our expression containing the forward reference
				var destBlock = (XBlockExpression) responsibleExpression.eContainer();
				var destExpressions = destBlock.getExpressions();
				containingBlock.getExpressions().remove(expToMove);
				var responsibleExpressionPosition =
					destExpressions.indexOf(responsibleExpressionBlockExp);
				/*
				 * it is crucial to copy the expression otherwise the formatter
				 * will not be able to format the code correctly, resulting in
				 * syntax errors after formatting (the expressions will not be
				 * separated)
				 */
				destExpressions.add(responsibleExpressionPosition + 1,
					EcoreUtil.copy(expToMove));
			}
		);
	}

}
