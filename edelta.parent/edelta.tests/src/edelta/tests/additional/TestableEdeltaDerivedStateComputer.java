package edelta.tests.additional;

import java.util.Map;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;

import com.google.inject.Singleton;

import edelta.resource.EdeltaDerivedStateComputer;

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
	public EdeltaDerivedStateComputer.EdeltaDerivedStateAdapter getOrInstallAdapter(final Resource resource) {
		return super.getOrInstallAdapter(resource);
	}

	@Override
	public void unloadDerivedPackages(final Map<String, EPackage> nameToEPackageMap) {
		super.unloadDerivedPackages(nameToEPackageMap);
	}

	@Override
	protected EPackage getOrAddDerivedStateEPackage(final EPackage originalEPackage,
			final Map<String, EPackage> nameToCopiedEPackageMap) {
		final EPackage result = super.getOrAddDerivedStateEPackage(originalEPackage, nameToCopiedEPackageMap);
		originalEPackage.eAdapters().add(new EdeltaEContentAdapter());
		return result;
	}
}
