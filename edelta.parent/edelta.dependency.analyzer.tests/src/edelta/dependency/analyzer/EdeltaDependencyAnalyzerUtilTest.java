package edelta.dependency.analyzer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.junit.Test;

import GraphMM.GraphMMFactory;
import GraphMM.Metamodel;

public class EdeltaDependencyAnalyzerUtilTest {

	private static final String TESTECORES = "testecores";

	@Test
	public void testHighlightedNotFound() {
		var repository = GraphMMFactory.eINSTANCE.createRepository();
		assertThatThrownBy(
			() -> EdeltaDependencyAnalyzerUtils.computeMetamodelDependencies(repository))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("No highlighted Metamodel found");
	}

	@Test
	public void testComputeMetamodelDependenciesOutgoing() throws IOException {
		var analyzer = new EdeltaDependencyAnalizer();
		// package4 -> package1, package2
		// package3 -> package2 -> package1,
		// (package2 <-> package1)
		var repository = analyzer.analyzeEPackage(
				TESTECORES + "/independentdependencies/", "testecoreforusages4");
		var result = EdeltaDependencyAnalyzerUtils.computeMetamodelDependencies(repository);
		assertThat(result.getHighlighted().getName())
			.isEqualTo("testecoreforusages4");
		assertThat(result.getDependencies())
			.extracting(Metamodel::getName)
			.containsExactly("testecoreforusages1", "testecoreforusages2");
	}

	@Test
	public void testComputeMetamodelDependenciesIncoming() throws IOException {
		var analyzer = new EdeltaDependencyAnalizer();
		// package4 -> package1, package2
		// package3 -> package2 -> package1,
		// (package2 <-> package1)
		var repository = analyzer.analyzeEPackage(
				TESTECORES + "/independentdependencies/", "testecoreforusages1");
		var result = EdeltaDependencyAnalyzerUtils.computeMetamodelDependencies(repository);
		assertThat(result.getHighlighted().getName())
			.isEqualTo("testecoreforusages1");
		assertThat(result.getDependencies())
			.extracting(Metamodel::getName)
			.containsExactly("testecoreforusages2", "testecoreforusages4");
	}
}
