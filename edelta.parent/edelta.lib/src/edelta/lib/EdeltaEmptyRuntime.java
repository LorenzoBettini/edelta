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
public class EdeltaEmptyRuntime extends AbstractEdelta {

	public EdeltaEmptyRuntime() {
		super();
	}

	/**
	 * @param other
	 */
	public EdeltaEmptyRuntime(AbstractEdelta other) {
		super(other);
	}

}
