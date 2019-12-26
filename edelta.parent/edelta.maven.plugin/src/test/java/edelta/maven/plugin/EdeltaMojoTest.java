package edelta.maven.plugin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;

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

	@Test
	public void testProjectWithoutSources() throws Exception {
		File pomPath = new File("target/test-classes/project-without-sources/");
		assertNotNull(pomPath);
		assertTrue(pomPath.exists());

		EdeltaMojo EdeltaMojo = (EdeltaMojo) rule.lookupConfiguredMojo(pomPath, "generate");
		assertNotNull(EdeltaMojo);
		EdeltaMojo.execute();

		File outputDirectory = new File(rule.getVariableValueFromObject(EdeltaMojo, "outputDirectory").toString());
		assertNotNull(outputDirectory);
		assertFalse(outputDirectory.exists());

		File xtextTmpDirectory = new File(rule.getVariableValueFromObject(EdeltaMojo, "tmpClassDirectory").toString());
		assertNotNull(xtextTmpDirectory);
		assertTrue(xtextTmpDirectory.exists());
	}

	@Test
	public void testProjectWithoutEcore() throws Exception {
		File pomPath = new File("target/test-classes/project-without-ecore/");
		assertNotNull(pomPath);
		assertTrue(pomPath.exists());

		EdeltaMojo EdeltaMojo = (EdeltaMojo) rule.lookupConfiguredMojo(pomPath, "generate");
		assertNotNull(EdeltaMojo);
		EdeltaMojo.execute();

		File outputDirectory = 
			new File(
				pomPath.getAbsolutePath(),
				rule.getVariableValueFromObject(EdeltaMojo, "outputDirectory").toString());
		assertNotNull(outputDirectory);
		assertTrue(outputDirectory.exists());
	}

	@Test
	public void testProjectWithEcore() throws Exception {
		File pomPath = new File("target/test-classes/project-with-ecore/");
		assertNotNull(pomPath);
		assertTrue(pomPath.exists());

		EdeltaMojo EdeltaMojo = (EdeltaMojo) rule.lookupConfiguredMojo(pomPath, "generate");
		assertNotNull(EdeltaMojo);
		EdeltaMojo.execute();

		File outputDirectory = 
			new File(
				pomPath.getAbsolutePath(),
				rule.getVariableValueFromObject(EdeltaMojo, "outputDirectory").toString());
		assertNotNull(outputDirectory);
		assertTrue(outputDirectory.exists());
	}

	@Test
	public void testProjectWithOutputDirectory() throws Exception {
		File pomPath = new File("target/test-classes/project-with-output-directory/");
		assertNotNull(pomPath);
		assertTrue(pomPath.exists());

		EdeltaMojo EdeltaMojo = (EdeltaMojo) rule.lookupConfiguredMojo(pomPath, "generate");
		assertNotNull(EdeltaMojo);
		EdeltaMojo.execute();

		File defaultOutputDirectory = 
			new File(
				pomPath.getAbsolutePath(),
				"edelta-gen");
		assertNotNull(defaultOutputDirectory);
		assertFalse(defaultOutputDirectory.exists());
		File outputDirectory = 
			new File(
				pomPath.getAbsolutePath(),
				"alt-gen");
		assertNotNull(outputDirectory);
		assertTrue(outputDirectory.exists());
	}

}
