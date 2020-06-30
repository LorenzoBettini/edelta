package edelta.tests.additional;

import java.util.List;

import com.google.inject.Singleton;

import edelta.edelta.EdeltaModifyEcoreOperation;
import edelta.edelta.EdeltaProgram;
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
	protected void copyEPackages(List<EdeltaModifyEcoreOperation> modifyEcoreOperations,
			EdeltaCopiedEPackagesMap copiedEPackagesMap) {
		final var originalEPackages =
			((EdeltaProgram) modifyEcoreOperations.get(0).eContainer()).getMetamodels();
		originalEPackages.stream()
			.forEach(p -> p.eAdapters().add(new EdeltaEContentAdapter()));
		super.copyEPackages(modifyEcoreOperations, copiedEPackagesMap);
	}

}
