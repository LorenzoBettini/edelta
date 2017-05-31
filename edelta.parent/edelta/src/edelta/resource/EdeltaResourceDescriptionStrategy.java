/**
 * 
 */
package edelta.resource;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.util.IAcceptor;
import org.eclipse.xtext.xbase.resource.XbaseResourceDescriptionStrategy;

/**
 * Don't index our EPackages created by the derived state computer
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaResourceDescriptionStrategy extends XbaseResourceDescriptionStrategy {

	@Override
	public boolean createEObjectDescriptions(EObject eObject, IAcceptor<IEObjectDescription> acceptor) {
		if (eObject instanceof EPackage) {
			return false;
		}
		return super.createEObjectDescriptions(eObject, acceptor);
	}
}
