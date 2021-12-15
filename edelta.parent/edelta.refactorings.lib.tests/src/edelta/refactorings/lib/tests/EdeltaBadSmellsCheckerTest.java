package edelta.refactorings.lib.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.emf.ecore.EPackage;
import org.junit.Before;
import org.junit.Test;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.refactorings.lib.EdeltaBadSmellsChecker;
import edelta.refactorings.lib.tests.utils.InMemoryLoggerAppender;

public class EdeltaBadSmellsCheckerTest extends AbstractTest {
	private EdeltaBadSmellsChecker checker;

	private InMemoryLoggerAppender appender;

	@Before
	public void setup() {
		checker = new EdeltaBadSmellsChecker();
		appender = new InMemoryLoggerAppender();
		appender.setLineSeparator("\n");
		checker.getLogger().addAppender(appender);
	}

	@Test
	public void test_ConstructorArgument() {
		checker = new EdeltaBadSmellsChecker(new EdeltaDefaultRuntime());
		assertThat(checker).isNotNull();
	}

	@Test
	public void test_checkDuplicatedFeatures_whenNoDuplicates() {
		final EPackage p = createEPackage("p", pack -> {
			stdLib.addNewEClass(pack, "C1",
				c -> stdLib.addNewEAttribute(c, "A1", stringDataType));
			stdLib.addNewEClass(pack, "C2",
				c -> stdLib.addNewEAttribute(c, "A1", intDataType));
		});
		checker.checkDuplicatedFeatures(p);
		assertThat(appender.getResult()).isEmpty();
	}

	@Test
	public void test_checkDuplicatedFeatures_withDuplicates() {
		final EPackage p = createEPackage("pack", pack -> {
			stdLib.addNewEClass(pack, "C1",
				c -> stdLib.addNewEAttribute(c, "A1", stringDataType));
			stdLib.addNewEClass(pack, "C2",
				c -> stdLib.addNewEAttribute(c, "A1", stringDataType));
			stdLib.addNewEClass(pack, "C3",
				c -> stdLib.addNewEAttribute(c, "A1", stringDataType));
		});
		checker.checkDuplicatedFeatures(p);
		assertThat(appender.getResult())
			.isEqualTo(
			"WARN: pack.C1.A1: pack.C1.A1, duplicate features: pack.C2.A1, pack.C3.A1\n"
			+ "WARN: pack.C2.A1: pack.C2.A1, duplicate features: pack.C1.A1, pack.C3.A1\n"
			+ "WARN: pack.C3.A1: pack.C3.A1, duplicate features: pack.C1.A1, pack.C2.A1\n"
			+ "");
	}
}
