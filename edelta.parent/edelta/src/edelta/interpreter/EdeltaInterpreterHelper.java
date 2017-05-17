/**
 * 
 */
package edelta.interpreter;

import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.util.JavaReflectAccess;

import com.google.inject.Singleton;

import edelta.edelta.EdeltaUseAs;

/**
 * Helper class for the EdeltaInterpreter.
 * 
 * @author Lorenzo Bettini
 *
 */
@Singleton
public class EdeltaInterpreterHelper {

	private static Object DEFAULT_INSTANCE = new Object();

	public Object safeInstantiate(JavaReflectAccess javaReflectAccess, EdeltaUseAs useAs) {
		JvmTypeReference typeRef = useAs.getType();
		if (typeRef == null) {
			return DEFAULT_INSTANCE;
		}
		final Class<?> javaType = javaReflectAccess.getRawType(typeRef.getType());
		try {
			return javaType.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			return DEFAULT_INSTANCE;
		}
	}
}
