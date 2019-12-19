package edelta.tests.additional;

import java.util.Objects;

import org.eclipse.xtext.common.types.access.impl.ClassFinder;
import org.eclipse.xtext.common.types.util.JavaReflectAccess;

import com.google.inject.Inject;

/**
 * Fake implementation that, when we try to load
 * MyCustomEdeltaThatCannotBeLoadedAtRuntime with forName it returns null.
 */
public class MockJavaReflectAccess extends JavaReflectAccess {
	private ClassLoader classLoader;

	@Inject
	@Override
	public void setClassLoader(final ClassLoader classLoader) {
		super.setClassLoader(classLoader);
		this.classLoader = classLoader;
	}

	@Override
	public ClassFinder getClassFinder() {
		return new ClassFinder(this.classLoader) {
			@Override
			public Class<?> forName(final String name) throws ClassNotFoundException {
				if (Objects.equals(name, MyCustomEdeltaThatCannotBeLoadedAtRuntime.class.getName())) {
					return null;
				}
				return super.forName(name);
			}
		};
	}
}