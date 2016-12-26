package edelta.lib.exception;

/**
 * The Ecore of an EPackage used during the transformation has not been loaded.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaPackageNotLoadedException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public EdeltaPackageNotLoadedException(String packageName) {
		super(packageName + " not loaded.");
	}

}
