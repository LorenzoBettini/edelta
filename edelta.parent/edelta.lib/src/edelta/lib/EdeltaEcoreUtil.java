/**
 * 
 */
package edelta.lib;

import org.eclipse.emf.ecore.EClassifier;
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

	public static void removeEClassifier(EClassifier eClassifier) {
		EcoreUtil.delete(eClassifier, true);
	}

	public static EClassifier copyEClassifier(EClassifier eClassifier) {
		// we must not resolve proxies, that's why we don't simply call EcoreUtil.copy
		Copier copier = new Copier(false);
		EObject result = copier.copy(eClassifier);
		copier.copyReferences();
		return (EClassifier) result;
	}

}
