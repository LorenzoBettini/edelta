package edelta.tests.additional;

import org.eclipse.emf.ecore.EPackage;

import com.google.inject.Singleton;

import edelta.resource.EdeltaDerivedStateComputer;
import edelta.resource.derivedstate.EdeltaCopiedEPackagesMap;

/**
 * Some protected methods are made public so that we can call them in the tests
 * and a content adapter is attached on imported metamodels to make sure these
 * are NOT touched during the interpretation.
 * 
 * @author Lorenzo Bettini
 */
@Singleton
public class TestableEdeltaDerivedStateComputer extends EdeltaDerivedStateComputer {
	@Override
	public void unloadDerivedPackages(final EdeltaCopiedEPackagesMap copiedEPackagesMap) {
		super.unloadDerivedPackages(copiedEPackagesMap);
	}

	@Override
	protected EPackage getOrAddDerivedStateEPackage(final EPackage originalEPackage,
			final EdeltaCopiedEPackagesMap copiedEPackagesMap) {
		final EPackage result = super.getOrAddDerivedStateEPackage(originalEPackage, copiedEPackagesMap);
		originalEPackage.eAdapters().add(new EdeltaEContentAdapter());
		return result;
	}
}
