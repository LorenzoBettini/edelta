package edelta.dependency.analyzer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Test;

import GraphMM.Dependency;
import GraphMM.Metamodel;
import edelta.testutils.EdeltaTestUtils;

public class EdeltaDependencyAnalyzerTest {

	private static final String TESTECORES = "testecores";
	private static final String OUTPUT = "output";
	private static final String EXPECTATIONS = "expectations";

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
		var repository = analyzer.analyzeMainEPackage(package1);
		var metamodels = assertThat(repository.getNodes())
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
		var dependencies = assertThat(repository.getEdges())
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
	}

	@Test
	public void testEPackageNotFound() throws IOException {
		var analyzer = new EdeltaDependencyAnalizer();
		assertThatThrownBy(
			() -> analyzer.analyzeEPackage(TESTECORES + "/unidirectional/", "nonexistent"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("No EPackage with name: nonexistent");
	}

	@Test
	public void testHighlightedTargetDependency() throws IOException {
		var analyzer = new EdeltaDependencyAnalizer();
		// package1 -> package2
		var repository = analyzer.analyzeEPackage(TESTECORES + "/unidirectional/", "testecoreforusages2");
		var package1 = analyzer.getEPackage("testecoreforusages1");
		var package2 = analyzer.getEPackage("testecoreforusages2");
		assertThat(package1).isNotNull();
		assertThat(package2).isNotNull();
		var metamodels = assertThat(repository.getNodes())
			.asInstanceOf(InstanceOfAssertFactories.list(Metamodel.class));
		metamodels
			.extracting(Metamodel::getName)
			.containsExactly(package2.getName(), package1.getName());
		metamodels
			.extracting(Metamodel::getNsURI)
			.containsExactly(package2.getNsURI(), package1.getNsURI());
		metamodels
			.extracting(Metamodel::isHighlighted)
			.containsExactly(true, false);
		var dependencies = assertThat(repository.getEdges())
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
		// package1 <-> package2
		var repository = analyzer.analyzeMainEPackage(package1);
		var metamodels = assertThat(repository.getNodes())
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
		var dependencies = assertThat(repository.getEdges())
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
	}

	@Test
	public void testTransitiveTargetDependency() throws IOException {
		var analyzer = new EdeltaDependencyAnalizer();
		// package3 -> package2 <-> package1
		var repository = analyzer.analyzeEPackage(TESTECORES + "/transitive/", "testecoreforusages3");
		var package1 = analyzer.getEPackage("testecoreforusages1");
		var package2 = analyzer.getEPackage("testecoreforusages2");
		var package3 = analyzer.getEPackage("testecoreforusages3");
		assertThat(package1).isNotNull();
		assertThat(package2).isNotNull();
		assertThat(package3).isNotNull();
		var metamodels = assertThat(repository.getNodes())
			.asInstanceOf(InstanceOfAssertFactories.list(Metamodel.class));
		metamodels
			.extracting(Metamodel::getName)
			.containsExactly(package3.getName(), package2.getName(), package1.getName());
		metamodels
			.extracting(Metamodel::getNsURI)
			.containsExactly(package3.getNsURI(), package2.getNsURI(), package1.getNsURI());
		metamodels
			.extracting(Metamodel::isHighlighted)
			.containsExactly(true, false, false);
		var dependencies = assertThat(repository.getEdges())
			.asInstanceOf(InstanceOfAssertFactories.list(Dependency.class));
		// p3 -> p2, p2 -> p1
		dependencies
			.extracting(d -> d.getSrc().getName())
			.containsExactly(package3.getName(), package2.getName());
		dependencies
			.extracting(d -> d.getTrg().getName())
			.containsExactly(package2.getName(), package1.getName());
		dependencies
			.extracting(Dependency::isBidirectional)
			.containsExactly(false, true);
	}

	@Test
	public void testTwoDependencies() throws IOException {
		var analyzer = new EdeltaDependencyAnalizer();
		// package4 -> package2, package1 (package2 <-> package1)
		var repository = analyzer.analyzeEPackage(TESTECORES + "/twodependencies/", "testecoreforusages4");
		var package1 = analyzer.getEPackage("testecoreforusages1");
		var package2 = analyzer.getEPackage("testecoreforusages2");
		var package4 = analyzer.getEPackage("testecoreforusages4");
		assertThat(package1).isNotNull();
		assertThat(package2).isNotNull();
		assertThat(package4).isNotNull();
		var metamodels = assertThat(repository.getNodes())
			.asInstanceOf(InstanceOfAssertFactories.list(Metamodel.class));
		metamodels
			.extracting(Metamodel::getName)
			.containsExactly(package4.getName(), package1.getName(), package2.getName());
		metamodels
			.extracting(Metamodel::getNsURI)
			.containsExactly(package4.getNsURI(), package1.getNsURI(), package2.getNsURI());
		metamodels
			.extracting(Metamodel::isHighlighted)
			.containsExactly(true, false, false);
		var dependencies = assertThat(repository.getEdges())
			.asInstanceOf(InstanceOfAssertFactories.list(Dependency.class));
		// p4 -> p1, p2 (and p1 <-> p2)
		dependencies
			.extracting(d -> d.getSrc().getName())
			.containsExactly(package4.getName(), package1.getName(), package4.getName());
		dependencies
			.extracting(d -> d.getTrg().getName())
			.containsExactly(package1.getName(), package2.getName(), package2.getName());
		dependencies
			.extracting(Dependency::isBidirectional)
			.containsExactly(false, true, false);
	}

	@Test
	public void testSaveRepository() throws IOException {
		var analyzer = new EdeltaDependencyAnalizer();
		// package4 -> package2, package1 (package2 <-> package1)
		var repository = analyzer.analyzeEPackage(
			TESTECORES + "/twodependencies/", "testecoreforusages4");
		analyzer.saveRepository(
			repository, OUTPUT + "/twodependencies", "dependencies.graphmm");
		EdeltaTestUtils.assertFilesAreEquals(
			EXPECTATIONS + "/twodependencies/dependencies.graphmm",
			OUTPUT + "/twodependencies/dependencies.graphmm");
	}
}
