/**
 * 
 */
package edelta.lib;

import java.util.Collection;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.EcoreUtil.Copier;

/**
 * Static utility functions acting on Ecore.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaEcoreUtil {

	private EdeltaEcoreUtil() {
		// empty constructor never to be called
	}

	/**
	 * Removes the {@link EClassifier} and recursively its contents.
	 * 
	 * @param eClassifier
	 */
	public static void removeEClassifier(EClassifier eClassifier) {
		EcoreUtil.delete(eClassifier, true);
	}

	@SuppressWarnings("unchecked")
	public static <T extends ENamedElement> T copyENamedElement(T element) {
		// we must not resolve proxies, that's why we don't simply call EcoreUtil.copy
		Copier copier = new Copier(false);
		EObject result = copier.copy(element);
		copier.copyReferences();
		return (T) result;
	}

	/**
	 * Creates copies of all the passed {@link EPackage}s, copying them all together
	 * and resolving proxies while copying, so that possible (even bidirectional)
	 * references among {@link EPackage}s are consistent in the resulting copies.
	 * 
	 * @param epackages
	 * @return
	 */
	public static Collection<EPackage> copyEPackages(Collection<EPackage> epackages) {
		Copier copier = new Copier(true);
		Collection<EPackage> copies = copier.copyAll(epackages);
		copier.copyReferences();
		return copies;
	}
}
