/**
 * 
 */
package edelta.lib;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
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

	/**
	 * If the value of the {@link EObject} o for the {@link EStructuralFeature}
	 * feature is set it returns a singleton collection with that value, otherwise
	 * it returns an empty collection. For multiple features, it returns the
	 * collection value.
	 * 
	 * This way, it is possible to iterate to the result of
	 * {@link EObject#eGet(EStructuralFeature)}, uniformly without checking for null
	 * values.
	 * 
	 * @param o
	 * @param feature
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Collection<Object> wrapAsCollection(EObject o, EStructuralFeature feature) {
		var value = o.eGet(feature);
		if (value instanceof Collection<?>)
			return (Collection<Object>) value;
		if (value != null)
			return Collections.singletonList(value);
		return Collections.emptyList();
	}

	/**
	 * Based on {@link EStructuralFeature#isMany()}: if it's true, it returns the
	 * value as it is, assuming it is a {@link Collection}, otherwise, if it's a
	 * non-empty collection it returns the first value or null. Otherwise it returns
	 * the value itself (which might be null or a single object).
	 * 
	 * This is meant to be used with the result of
	 * {@link #wrapAsCollection(EObject, EStructuralFeature)}. For example to call
	 * {@link EObject#eSet(EStructuralFeature, Object)} after unwrapping.
	 * 
	 * @param value
	 * @param feature
	 * @return
	 */
	public static Object unwrapCollection(Object value, EStructuralFeature feature) {
		if (feature.isMany())
			return value;
		else if (value instanceof Collection<?>) {
			var collection = (Collection<?>) value;
			Iterator<?> iterator = collection.iterator();
			if (iterator.hasNext())
				return iterator.next();
			else
				return null;
		}
		return value;
	}
}
