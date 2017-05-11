/**
 * 
 */
package edelta.resource;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.util.IAcceptor;
import org.eclipse.xtext.xbase.resource.XbaseResourceDescriptionStrategy;

/**
 * Don't index our derived state EPackages
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaResourceDescriptionStrategy extends XbaseResourceDescriptionStrategy {

	@Override
	public boolean createEObjectDescriptions(EObject eObject, IAcceptor<IEObjectDescription> acceptor) {
		if (eObject instanceof EdeltaDerivedStateEPackage) {
			return false;
		}
		return super.createEObjectDescriptions(eObject, acceptor);
	}
}
