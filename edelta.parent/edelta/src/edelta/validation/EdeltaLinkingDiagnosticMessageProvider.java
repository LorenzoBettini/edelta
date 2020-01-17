package edelta.validation;

import java.util.function.Supplier;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.diagnostics.DiagnosticMessage;
import org.eclipse.xtext.xbase.XAssignment;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XMemberFeatureCall;
import org.eclipse.xtext.xbase.annotations.validation.UnresolvedAnnotationTypeAwareMessageProvider;

import edelta.edelta.EdeltaEcoreReferenceExpression;

/**
 * When the problem is related to an unresolved feature call where the receiver
 * is an ecoreref expression, we must intercept that and avoid that it is being
 * generated, since during the interpretation the feature call might then be
 * resolved. We then have to deal with unresolved feature call in our validator.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaLinkingDiagnosticMessageProvider extends UnresolvedAnnotationTypeAwareMessageProvider {

	@Override
	public DiagnosticMessage getUnresolvedProxyMessage(ILinkingDiagnosticContext context) {
		EObject contextObject = context.getContext();
		if (contextObject instanceof XMemberFeatureCall) {
			XMemberFeatureCall featureCall = (XMemberFeatureCall) contextObject;
			return filterErrorMessage(context, featureCall::getMemberCallTarget);
		} else if (contextObject instanceof XAssignment) {
			XAssignment assignment = (XAssignment) contextObject;
			return filterErrorMessage(context, assignment::getAssignable);
		}
		return super.getUnresolvedProxyMessage(context);
	}

	private DiagnosticMessage filterErrorMessage(ILinkingDiagnosticContext context, Supplier<XExpression> receiverSupplier) {
		XExpression receiver = receiverSupplier.get();
		if (receiver instanceof EdeltaEcoreReferenceExpression) {
			return null;
		} else {
			return super.getUnresolvedProxyMessage(context);
		}
	}
}