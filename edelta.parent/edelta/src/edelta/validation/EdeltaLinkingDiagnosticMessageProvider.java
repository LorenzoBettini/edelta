package edelta.validation;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.diagnostics.DiagnosticMessage;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XMemberFeatureCall;
import org.eclipse.xtext.xbase.annotations.validation.UnresolvedAnnotationTypeAwareMessageProvider;

import edelta.edelta.EdeltaEcoreReferenceExpression;

/**
 * When the problem is related to an unresolved feature call where the received
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
			if (!featureCall.isOperation()) {
				XExpression receiver = featureCall.getMemberCallTarget();
				if (receiver instanceof EdeltaEcoreReferenceExpression) {
					return null;
				}
			}
		}
		return super.getUnresolvedProxyMessage(context);
	}
}