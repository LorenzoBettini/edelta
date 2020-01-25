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

	override nameToEPackageMap(Resource resource) {
		super.nameToEPackageMap(resource)
	}

	override nameToCopiedEPackageMap(Resource resource) {
		super.nameToCopiedEPackageMap(resource)
	}

	override protected getOrAddDerivedStateEPackage(EPackage referredEPackage, Map<String, EPackage> nameToEPackageMap,
		Map<String, EPackage> nameToCopiedEPackageMap) {
		val result = super.getOrAddDerivedStateEPackage(referredEPackage, nameToEPackageMap, nameToCopiedEPackageMap)
		referredEPackage.eAdapters += new EdeltaEContentAdapter
		return result
	}

}
