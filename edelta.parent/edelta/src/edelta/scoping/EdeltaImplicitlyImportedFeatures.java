/**
 * 
 */
package edelta.scoping;

import java.util.List;

import org.eclipse.xtext.xbase.scoping.batch.ImplicitlyImportedFeatures;

import edelta.lib.EdeltaUtils;

/**
 * Make {@link EdeltaUtils} automatically statically imported and its static
 * methods automatically available as extension methods.
 * 
 * @author Lorenzo Bettini
 */
public class EdeltaImplicitlyImportedFeatures extends ImplicitlyImportedFeatures {

	@Override
	protected List<Class<?>> getExtensionClasses() {
		final var extensionClasses = super.getExtensionClasses();
		extensionClasses.add(EdeltaUtils.class);
		return extensionClasses;
	}

	@Override
	protected List<Class<?>> getStaticImportClasses() {
		final var staticImportClasses = super.getStaticImportClasses();
		staticImportClasses.add(EdeltaUtils.class);
		return staticImportClasses;
	}
}
