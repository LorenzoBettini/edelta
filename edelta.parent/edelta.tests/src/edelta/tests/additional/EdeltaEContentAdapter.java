/**
 * 
 */
package edelta.tests.additional;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.util.EContentAdapter;

import edelta.interpreter.EdeltaInterpreterRuntimeException;

/**
 * Used in tests to make sure that the original imported Ecore models are not
 * touched during interpretation.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaEContentAdapter extends EContentAdapter {

	public static class EdeltaEContentAdapterException extends EdeltaInterpreterRuntimeException {
		private static final long serialVersionUID = 1L;

		public EdeltaEContentAdapterException(Notification notification) {
			super("Unexpected notification: " + notification);
		}
	}

	@Override
	public void notifyChanged(Notification notification) {
		super.notifyChanged(notification);
		if (notification.isTouch())
			return;

		throw new EdeltaEContentAdapterException(notification);
	}
}
