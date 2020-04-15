package edelta.tests.additional

import com.google.inject.Singleton
import edelta.resource.EdeltaDerivedStateComputer
import java.util.Map
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.resource.Resource

/** 
 * Some protected methods are made public so that we can call them in the tests
 * and a content adapter is attached on imported metamodels to make sure these
 * are NOT touched during the interpretation.
 * 
 * @author Lorenzo Bettini
 */
@Singleton
class TestableEdeltaDerivedStateComputer extends EdeltaDerivedStateComputer {
	override getOrInstallAdapter(Resource resource) {
		super.getOrInstallAdapter(resource)
	}

	override unloadDerivedPackages(Map<String, EPackage> nameToEPackageMap) {
		super.unloadDerivedPackages(nameToEPackageMap)
	}

	override copiedEPackagesMap(Resource resource) {
		super.copiedEPackagesMap(resource)
	}

	override protected getOrAddDerivedStateEPackage(EPackage originalEPackage,
		Map<String, EPackage> nameToCopiedEPackageMap) {
		val result = super.getOrAddDerivedStateEPackage(originalEPackage, nameToCopiedEPackageMap)
		originalEPackage.eAdapters += new EdeltaEContentAdapter
		return result
	}

}
