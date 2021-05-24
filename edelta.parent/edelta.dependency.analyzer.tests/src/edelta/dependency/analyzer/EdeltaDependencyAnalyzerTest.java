package edelta.dependency.analyzer;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Test;

import GraphMM.Metamodel;

public class EdeltaDependencyAnalyzerTest {

	@Test
	public void test_usedPackages() {
		var analyzer = new EdeltaDependencyAnalizer();
		analyzer.loadEcoreFile("testecores/TestEcoreForUsages1.ecore");
		analyzer.loadEcoreFile("testecores/TestEcoreForUsages2.ecore");
		analyzer.loadEcoreFile("testecores/TestEcoreForUsages3.ecore");
		analyzer.loadEcoreFile("testecores/TestEcoreForUsages4.ecore");
		var package1 = analyzer.getEPackage("testecoreforusages1");
		var package2 = analyzer.getEPackage("testecoreforusages2");
		var package3 = analyzer.getEPackage("testecoreforusages3");
		var package4 = analyzer.getEPackage("testecoreforusages4");
		assertThat(package1).isNotNull();
		assertThat(package2).isNotNull();
		assertThat(package3).isNotNull();
		assertThat(package4).isNotNull();
		var p1Repository = analyzer.analyzeEPackage(package1);
		var metamodels = assertThat(p1Repository.getNodes())
			.asInstanceOf(InstanceOfAssertFactories.list(Metamodel.class));
		metamodels
			.extracting(n -> n.getName())
			.containsExactlyInAnyOrder("testecoreforusages1", "testecoreforusages2");
		metamodels
			.extracting(n -> n.getNsURI())
			.containsExactlyInAnyOrder("http://my.testecoreforusages1.org", "http://my.testecoreforusages2.org");
//		assertThat(EdeltaLibrary.usedPackages(package1))
//			.containsExactlyInAnyOrder(package2);
//		assertThat(EdeltaLibrary.usedPackages(package2))
//			.containsExactlyInAnyOrder(package1);
//		assertThat(EdeltaLibrary.usedPackages(package3))
//			.containsExactlyInAnyOrder(package2);
//		assertThat(EdeltaLibrary.usedPackages(package4))
//			.containsExactlyInAnyOrder(package2, package1);
	}

}
