/**
 * 
 */
package edelta.services;

import java.util.Collection;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.IDerivedStateComputer;

/**
 * Facade to mappings between source elements, i.e. the EObjects parsed from the
 * source and the derived state Ecore elements, i.e. the inferred elements
 * inferred during
 * {@link IDerivedStateComputer#installDerivedState(org.eclipse.xtext.resource.DerivedStateAwareResource, boolean)}
 * 
 * @author Lorenzo Bettini
 *
 */
public interface IEdeltaEcoreModelAssociations {

	Collection<EPackage> derivedEPackages(Resource resource);

	Collection<EPackage> copiedEPackages(Resource resource);

	EObject getPrimarySourceElement(EObject jvmElement);
}
