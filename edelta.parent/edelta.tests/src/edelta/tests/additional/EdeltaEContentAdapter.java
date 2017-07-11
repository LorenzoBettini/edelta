/**
 * 
 */
package edelta.tests.additional;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.util.EContentAdapter;

/**
 * Used in tests to make sure that the original imported Ecore models are not
 * touched during interpretation.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaEContentAdapter extends EContentAdapter {

	public static class EdeltaEContentAdapterException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public EdeltaEContentAdapterException(Notification notification) {
			super("Unexpected notification: " + notification);
		}
	}

	@Override
	public void notifyChanged(Notification notification) {
		super.notifyChanged(notification);

		throw new EdeltaEContentAdapterException(notification);
	}
}
