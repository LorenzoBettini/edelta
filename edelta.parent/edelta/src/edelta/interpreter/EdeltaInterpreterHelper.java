/**
 * 
 */
package edelta.interpreter;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.util.JavaReflectAccess;

import com.google.inject.Singleton;

import edelta.edelta.EdeltaUseAs;
import edelta.interpreter.EdeltaSafeInterpreter.EdeltaInterpreterRuntimeException;
import edelta.lib.AbstractEdelta;

/**
 * Helper class for the EdeltaInterpreter.
 * 
 * @author Lorenzo Bettini
 *
 */
@Singleton
public class EdeltaInterpreterHelper {

	private static Object defaultInstance = new Object();

	public Object safeInstantiate(JavaReflectAccess javaReflectAccess, EdeltaUseAs useAs, AbstractEdelta other) {
		JvmTypeReference typeRef = useAs.getType();
		if (typeRef == null) {
			return defaultInstance;
		}
		final Class<?> javaType = javaReflectAccess.getRawType(typeRef.getType());
		if (javaType == null) {
			// The returned javaType could be null if the requested (and resolved JvmType
			// type) cannot be loaded through the ClassLoader. This might happen when
			// running the Edelta compiler using xtext-maven-plugin.
			// https://github.com/LorenzoBettini/edelta/issues/69
			throw new EdeltaInterpreterRuntimeException(
					String.format("The type '%s' has been resolved but cannot be loaded by the interpreter. "
							+ "The ClassLoader cannot find it. When this happens using the 'xtext-maven-plugin', "
							+ "please make sure to add the corresponding Maven module as a dependency in the "
							+ "'xtext-maven-plugin' configuration.", typeRef.getIdentifier())) {

				private static final long serialVersionUID = 1L;
			};
		}
		try {
			return javaType.getConstructor(AbstractEdelta.class)
						.newInstance(other);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			return defaultInstance;
		}
	}

}
