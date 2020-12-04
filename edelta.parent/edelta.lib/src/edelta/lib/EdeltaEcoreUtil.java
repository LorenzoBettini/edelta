/**
 * 
 */
package edelta.lib;

import java.util.Collection;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.util.EcoreUtil;

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
	 * Creates copies of all the passed {@link EPackage}s, copying them all together
	 * and resolving proxies while copying, so that possible (even bidirectional)
	 * references among {@link EPackage}s are consistent in the resulting copies.
	 * 
	 * @param epackages
	 * @return
	 */
	public static Collection<EPackage> copyEPackages(Collection<EPackage> epackages) {
		return EcoreUtil.copyAll(epackages);
	}
}
