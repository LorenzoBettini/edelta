package edelta.maven.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
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
		EdeltaMojo edeltaMojo = getEdeltaMojo(pomPath);
		edeltaMojo.execute();
		return edeltaMojo;
	}

	private EdeltaMojo getEdeltaMojo(File pomPath) throws Exception, ComponentConfigurationException {
		EdeltaMojo edeltaMojo = (EdeltaMojo) rule.lookupConfiguredMojo(pomPath, "generate");
		assertNotNull(edeltaMojo);
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
			getOutputDirectory(pomPath, edeltaMojo);
		assertDirectoryContainsGeneratedContents(outputDirectory);
	}

	@Test
	public void testProjectWithEcore() throws Exception {
		File pomPath = setupPom("/project-with-ecore/");
		EdeltaMojo edeltaMojo = executeMojo(pomPath);

		File outputDirectory = 
			getOutputDirectory(pomPath, edeltaMojo);
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

	@Test
	public void testProjectWithError() throws Exception {
		File pomPath = setupPom("/project-with-error/");
		EdeltaMojo edeltaMojo = getEdeltaMojo(pomPath);

		assertThatThrownBy(() -> edeltaMojo.execute())
			.isInstanceOf(MojoExecutionException.class);

		File outputDirectory = 
			getOutputDirectory(pomPath, edeltaMojo);
		assertThat(outputDirectory).doesNotExist();
	}

	@Test
	public void testProjectWithErrorFailOnValidationErrorFalse() throws Exception {
		File pomPath = setupPom("/project-with-error-no-fail/");
		EdeltaMojo edeltaMojo = getEdeltaMojo(pomPath);

		Log spiedLog = spyLog(edeltaMojo);
		edeltaMojo.execute();

		File outputDirectory = 
			getOutputDirectory(pomPath, edeltaMojo);
		assertThat(outputDirectory).exists();

		verify(spiedLog)
			.error(contains("ERROR:The method or field foobar is undefined"));
	}

	@Test
	public void testDebugLogging() throws Exception {
		File pomPath = setupPom("/project-with-error-no-fail/");
		EdeltaMojo edeltaMojo = getEdeltaMojo(pomPath);

		Log spiedLog = spyLog(edeltaMojo);
		when(spiedLog.isDebugEnabled()).thenReturn(true);
		edeltaMojo.execute();
	}

	@Test
	public void testProjectWithSourceRoots() throws Exception {
		File pomPath = setupPom("/project-with-source-roots/");
		EdeltaMojo edeltaMojo = executeMojo(pomPath);

		File outputDirectory = 
			getOutputDirectory(pomPath, edeltaMojo);
		assertDirectoryContainsGeneratedContents(outputDirectory);

		@SuppressWarnings("unchecked")
		List<String> sourceRoots = (List<String>) rule.getVariableValueFromObject(edeltaMojo, "sourceRoots");
		assertThat(sourceRoots).
			allMatch(s -> s.contains("alt-"));
		@SuppressWarnings("unchecked")
		List<String> javaSourceRoots = (List<String>) rule.getVariableValueFromObject(edeltaMojo, "javaSourceRoots");
		assertThat(javaSourceRoots).
			allMatch(s -> s.contains("alt-java"));
	}

	private Log spyLog(EdeltaMojo edeltaMojo) {
		Log spiedLog = spy(edeltaMojo.getLog());
		edeltaMojo.setLog(spiedLog);
		return spiedLog;
	}

	private File getOutputDirectory(File pomPath, EdeltaMojo edeltaMojo) throws IllegalAccessException {
		return new File(
			pomPath.getAbsolutePath(),
			rule.getVariableValueFromObject(edeltaMojo, "outputDirectory").toString());
	}

	private void assertDirectoryContainsGeneratedContents(File outputDirectory) {
		assertThat(outputDirectory)
			.isDirectoryContaining("glob:**com");
	}

}
