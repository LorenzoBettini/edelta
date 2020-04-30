/**
 * 
 */
package edelta.lib;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
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

}
