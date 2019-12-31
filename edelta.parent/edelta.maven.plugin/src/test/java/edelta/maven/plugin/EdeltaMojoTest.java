package edelta.maven.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.MojoRule;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.Lists;

public class EdeltaMojoTest {
	@Rule
	public MojoRule rule = new MojoRule() {
		@Override
		protected void before() throws Throwable {
		}

		@Override
		protected void after() {
		}
	};

	private static final String TEST_PROJECTS_DIR = "target/test-classes";

	private static final List<String> generatedDirs = Lists.newArrayList("target", "edelta-gen", "alt-gen");

	private static void cleanDirectory(File baseDir) throws IOException {
		for (File file : baseDir.listFiles())
			if (generatedDirs.contains(file.getName()))
				FileUtils.deleteDirectory(file);
	}

	private File setupPom(String testProjectDir) throws IOException {
		File pomPath = new File(TEST_PROJECTS_DIR + testProjectDir);
		assertNotNull(pomPath);
		assertTrue(pomPath.exists());
		cleanDirectory(pomPath);
		return pomPath;
	}

	private EdeltaMojo executeMojo(File pomPath)
			throws Exception, ComponentConfigurationException, MojoExecutionException, MojoFailureException {
		EdeltaMojo edeltaMojo = (EdeltaMojo) rule.lookupConfiguredMojo(pomPath, "generate");
		assertNotNull(edeltaMojo);
		edeltaMojo.execute();
		return edeltaMojo;
	}

	@Test
	public void testProjectWithSkip() throws Exception {
		File pomPath = setupPom("/project-with-skip/");
		EdeltaMojo edeltaMojo = executeMojo(pomPath);

		File outputDirectory = new File(rule.getVariableValueFromObject(edeltaMojo, "outputDirectory").toString());
		assertThat(outputDirectory).doesNotExist();
	}

	@Test
	public void testProjectWithoutSources() throws Exception {
		File pomPath = setupPom("/project-without-sources/");
		EdeltaMojo edeltaMojo = executeMojo(pomPath);

		File outputDirectory = new File(rule.getVariableValueFromObject(edeltaMojo, "outputDirectory").toString());
		assertThat(outputDirectory).doesNotExist();

		File xtextTmpDirectory = new File(rule.getVariableValueFromObject(edeltaMojo, "tmpClassDirectory").toString());
		assertThat(xtextTmpDirectory).exists();
	}

	@Test
	public void testProjectWithoutEcore() throws Exception {
		File pomPath = setupPom("/project-without-ecore/");
		EdeltaMojo edeltaMojo = executeMojo(pomPath);

		File outputDirectory = 
			new File(
				pomPath.getAbsolutePath(),
				rule.getVariableValueFromObject(edeltaMojo, "outputDirectory").toString());
		assertDirectoryContainsGeneratedContents(outputDirectory);
	}

	@Test
	public void testProjectWithEcore() throws Exception {
		File pomPath = setupPom("/project-with-ecore/");
		EdeltaMojo edeltaMojo = executeMojo(pomPath);

		File outputDirectory = 
			new File(
				pomPath.getAbsolutePath(),
				rule.getVariableValueFromObject(edeltaMojo, "outputDirectory").toString());
		assertDirectoryContainsGeneratedContents(outputDirectory);
	}

	@Test
	public void testProjectWithOutputDirectory() throws Exception {
		File pomPath = setupPom("/project-with-output-directory/");
		executeMojo(pomPath);

		File defaultOutputDirectory = 
			new File(
				pomPath.getAbsolutePath(),
				"edelta-gen");
		assertThat(defaultOutputDirectory).doesNotExist();
		File outputDirectory = 
			new File(
				pomPath.getAbsolutePath(),
				"alt-gen");
		assertDirectoryContainsGeneratedContents(outputDirectory);
	}

	private void assertDirectoryContainsGeneratedContents(File outputDirectory) {
		assertThat(outputDirectory)
			.isDirectoryContaining("glob:**com");
	}

}
