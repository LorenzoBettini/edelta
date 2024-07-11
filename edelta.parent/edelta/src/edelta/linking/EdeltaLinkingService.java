package edelta.linking;

import java.util.Collections;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.linking.impl.DefaultLinkingService;
import org.eclipse.xtext.linking.impl.IllegalNodeException;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;

import edelta.edelta.EdeltaPackage;

/**
 * Custom implementation to correctly link to {@link EPackage}s
 * specified through nsURI, which typically contains "." and that
 * would make the qualified name converter to create a qualified name
 * separated by dots.
 * 
 * @author Lorenzo Bettini
 */
public class EdeltaLinkingService extends DefaultLinkingService {

	@Override
	public List<EObject> getLinkedObjects(EObject context, EReference ref, INode node) throws IllegalNodeException {
		if (ref == EdeltaPackage.eINSTANCE.getEdeltaMigration_NsURI()) {
			final String nsUri = getCrossRefNodeAsString(node);
			final QualifiedName nsUriQualifiedName = QualifiedName.create(nsUri);
			final IScope scope = getScope(context, ref);
			final IEObjectDescription eObjectDescription = scope.getSingleElement(nsUriQualifiedName);
			if (eObjectDescription == null) {
				return Collections.emptyList();
			}
			final EObject result = eObjectDescription.getEObjectOrProxy();
			return Collections.singletonList(result);
		}
		return super.getLinkedObjects(context, ref, node);
	}
}
