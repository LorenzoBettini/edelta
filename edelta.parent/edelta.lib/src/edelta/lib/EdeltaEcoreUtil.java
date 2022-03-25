/**
 * 
 */
package edelta.lib;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
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
	 * If the value of the {@link EObject} o for the {@link EStructuralFeature}
	 * feature is set it returns a singleton collection with that value, otherwise
	 * it returns an empty collection. For multiple features, it returns the
	 * collection value.
	 * 
	 * This way, it is possible to iterate to the result of
	 * {@link EObject#eGet(EStructuralFeature)}, uniformly without checking for null
	 * values.
	 * 
	 * Moreover, the limit allows the returned collection to contain at most limit
	 * elements. When -1 is passed it means no limit. Typically one passes the
	 * result of {@link EStructuralFeature#getUpperBound()} of the feature of the
	 * object that is meant to contain the (typically) preprocessed values.
	 * 
	 * @param o
	 * @param feature
	 * @param limit size limit for the returned collection, -1 means unlimited
	 * @return
	 */
	public static Collection<Object> wrapAsCollection(EObject o, EStructuralFeature feature, int limit) {
		var value = o.eGet(feature);
		return wrapAsCollection(value, limit);
	}

	/**
	 * If the value is set it returns a singleton collection with that value,
	 * otherwise it returns an empty collection. If it is a {@link Collection}, it
	 * returns the collection value.
	 * 
	 * This way, it is possible to iterate to the result of uniformly without
	 * checking for null values.
	 * 
	 * Moreover, the limit allows the returned collection to contain at most limit
	 * elements. When -1 is passed it means no limit. Typically one passes the
	 * result of {@link EStructuralFeature#getUpperBound()} of the feature of the
	 * object that is meant to contain the (typically) preprocessed values.
	 * 
	 * @param value
	 * @param limit size limit for the returned collection, -1 means unlimited
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Collection<Object> wrapAsCollection(Object value, int limit) {
		Collection<Object> result = Collections.emptyList();
		if (value instanceof Collection<?>)
			result = (Collection<Object>) value;
		else if (value != null)
			result = Collections.singletonList(value);
		if (limit > 0)
			return result.stream().limit(limit).collect(Collectors.toList());
		return result;
	}

	/**
	 * Based on {@link EStructuralFeature#isMany()}: if it's true, it returns the
	 * value as it is, assuming it is a {@link Collection}, otherwise, if it's a
	 * non-empty collection it returns the first value or null. Otherwise it returns
	 * the value itself (which might be null or a single object).
	 * 
	 * This is meant to be used with the result of
	 * {@link #wrapAsCollection(EObject, EStructuralFeature, int)}. For example to call
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

	/**
	 * See {@link #wrapAsCollection(EObject, EStructuralFeature, int)}.
	 * 
	 * @param obj
	 * @param feature
	 * @param limit
	 * @return
	 */
	public static Collection<Object> getValueForFeature(EObject obj, EStructuralFeature feature, int limit) {
		return wrapAsCollection(obj, feature, limit);
	}

	/**
	 * Calls {@link EObject#eSet(EStructuralFeature, Object)} after unwrapping the
	 * passed value with {@link #unwrapCollection(Object, EStructuralFeature)}.
	 * 
	 * If the feature is a many feature, and the value is not a {@link Collection},
	 * it will first turn it into a (possibly empty) collection using
	 * {@link #wrapAsCollection(Object, int)}.
	 * 
	 * The idea is that it should always be safe to set the value for the feature of
	 * the passed {@link EObject} by using this method.
	 * 
	 * @param obj
	 * @param feature
	 * @param value
	 */
	public static void setValueForFeature(EObject obj, EStructuralFeature feature, Object value) {
		Object unwrapCollection = unwrapCollection(value, feature);
		if (feature.isMany() && !(unwrapCollection instanceof Collection<?>)) {
			unwrapCollection = wrapAsCollection(unwrapCollection, feature.getUpperBound());
		}
		obj.eSet(feature, unwrapCollection);
	}

	public static EObject createInstance(EClass type, Consumer<EObject> initializer) {
		var instance = EcoreUtil.create(type);
		initializer.accept(instance);
		return instance;
	}

	@SuppressWarnings("unchecked")
	public static List<EObject> getValueAsList(EObject obj, EStructuralFeature feature) {
		return (List<EObject>) obj.eGet(feature);
	}
}
