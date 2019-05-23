package edelta.ide.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.typesystem.internal.FeatureScopeTracker;
import org.eclipse.xtext.xbase.typesystem.internal.IFeatureScopeTracker;
import org.eclipse.xtext.xbase.typesystem.internal.OptimizingFeatureScopeTrackerProvider;

/**
 * Workaround for https://github.com/eclipse/xtext-extras/issues/144
 * 
 * @author Lorenzo Bettini
 *
 */
public class OptimizingFeatureScopeTrackerProvider2 extends OptimizingFeatureScopeTrackerProvider {

	@Override
	public IFeatureScopeTracker track(EObject root) {
		return new FeatureScopeTracker() {

		};
	}

}