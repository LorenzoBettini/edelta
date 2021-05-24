package edelta.dependency.analyzer;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Test;

import GraphMM.Dependency;
import GraphMM.Metamodel;

public class EdeltaDependencyAnalyzerTest {

	private static final String TESTECORES = "testecores";

	@Test
	public void testUnidirectionalDependency() {
		var analyzer = new EdeltaDependencyAnalizer();
		analyzer.loadEcoreFile(TESTECORES + "/unidirectional/TestEcoreForUsages1.ecore");
		analyzer.loadEcoreFile(TESTECORES + "/unidirectional/TestEcoreForUsages2.ecore");
		var package1 = analyzer.getEPackage("testecoreforusages1");
		var package2 = analyzer.getEPackage("testecoreforusages2");
		assertThat(package1).isNotNull();
		assertThat(package2).isNotNull();
		// package1 uses package2
		var p1Repository = analyzer.analyzeEPackage(package1);
		var metamodels = assertThat(p1Repository.getNodes())
			.asInstanceOf(InstanceOfAssertFactories.list(Metamodel.class));
		metamodels
			.extracting(Metamodel::getName)
			.containsExactly(package1.getName(), package2.getName());
		metamodels
			.extracting(Metamodel::getNsURI)
			.containsExactly(package1.getNsURI(), package2.getNsURI());
		metamodels
			.extracting(Metamodel::isHighlighted)
			.containsExactly(true, false);
		var dependencies = assertThat(p1Repository.getEdges())
			.asInstanceOf(InstanceOfAssertFactories.list(Dependency.class));
		dependencies
			.extracting(d -> d.getSrc().getName())
			.containsExactly(package1.getName());
		dependencies
			.extracting(d -> d.getTrg().getName())
			.containsExactly(package2.getName());
		dependencies
			.extracting(Dependency::isBidirectional)
			.containsExactly(false);
//		assertThat(EdeltaLibrary.usedPackages(package1))
//			.containsExactlyInAnyOrder(package2);
//		assertThat(EdeltaLibrary.usedPackages(package2))
//			.containsExactlyInAnyOrder(package1);
//		assertThat(EdeltaLibrary.usedPackages(package3))
//			.containsExactlyInAnyOrder(package2);
//		assertThat(EdeltaLibrary.usedPackages(package4))
//			.containsExactlyInAnyOrder(package2, package1);
	}

	@Test
	public void testBidirectionalDependency() {
		var analyzer = new EdeltaDependencyAnalizer();
		analyzer.loadEcoreFile(TESTECORES + "/bidirectional/TestEcoreForUsages1.ecore");
		analyzer.loadEcoreFile(TESTECORES + "/bidirectional/TestEcoreForUsages2.ecore");
		var package1 = analyzer.getEPackage("testecoreforusages1");
		var package2 = analyzer.getEPackage("testecoreforusages2");
		assertThat(package1).isNotNull();
		assertThat(package2).isNotNull();
		// package1 uses package2
		// package2 uses package1
		var p1Repository = analyzer.analyzeEPackage(package1);
		var metamodels = assertThat(p1Repository.getNodes())
			.asInstanceOf(InstanceOfAssertFactories.list(Metamodel.class));
		metamodels
			.extracting(Metamodel::getName)
			.containsExactly(package1.getName(), package2.getName());
		metamodels
			.extracting(Metamodel::getNsURI)
			.containsExactly(package1.getNsURI(), package2.getNsURI());
		metamodels
			.extracting(Metamodel::isHighlighted)
			.containsExactly(true, false);
		var dependencies = assertThat(p1Repository.getEdges())
			.asInstanceOf(InstanceOfAssertFactories.list(Dependency.class));
		dependencies
			.extracting(d -> d.getSrc().getName())
			.containsExactly(package1.getName());
		dependencies
			.extracting(d -> d.getTrg().getName())
			.containsExactly(package2.getName());
		dependencies
			.extracting(Dependency::isBidirectional)
			.containsExactly(true);
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
