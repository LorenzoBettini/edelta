/**
 * 
 */
package edelta.lib;

/**
 * A concrete implementation of {@link AbstractEdelta} that does not
 * perform any specific operation, and it is meant to be used NOT from
 * the DSL generator.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaDefaultRuntime extends AbstractEdelta {

	public EdeltaDefaultRuntime() {
		super();
	}

	/**
	 * @param other
	 */
	public EdeltaDefaultRuntime(AbstractEdelta other) {
		super(other);
	}

	/**
	 * @param packageManager
	 */
	public EdeltaDefaultRuntime(EdeltaEPackageManager packageManager) {
		super(packageManager);
	}

}
