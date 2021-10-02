package edelta.interpreter;

import org.eclipse.emf.ecore.EPackage;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaEPackageManager;
import edelta.resource.derivedstate.EdeltaCopiedEPackagesMap;

/**
 * Used by the {@link EdeltaInterpreter} to return {@link EPackage} instances.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaInterpreterEdeltaImpl extends AbstractEdelta {

	/**
	 * Uses the passed {@link EdeltaCopiedEPackagesMap} to create an
	 * {@link EdeltaEPackageManager} that implements
	 * {@link EdeltaEPackageManager#getEPackage(String)} by simply delegating to the
	 * passed map. Note that the passed map will be shared, so that updates to that
	 * map are automatically used.
	 * 
	 * @param copiedEPackagesMap
	 */
	public EdeltaInterpreterEdeltaImpl(final EdeltaCopiedEPackagesMap copiedEPackagesMap) {
		super(new EdeltaEPackageManager() {
			@Override
			public EPackage getEPackage(String packageName) {
				return copiedEPackagesMap.get(packageName);
			}
		});
	}

	/**
	 * @param other
	 */
	public EdeltaInterpreterEdeltaImpl(AbstractEdelta other) {
		super(other);
	}
}
