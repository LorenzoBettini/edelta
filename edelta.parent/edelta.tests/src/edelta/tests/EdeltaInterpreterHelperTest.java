package edelta.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.last;
import static org.junit.Assert.assertEquals;

import org.eclipse.xtext.common.types.util.JavaReflectAccess;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import edelta.interpreter.EdeltaInterpreterHelper;
import edelta.interpreter.EdeltaInterpreterRuntimeException;
import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaModelManager;
import edelta.lib.EdeltaRuntime;
import edelta.tests.additional.MyCustomEdelta;
import edelta.tests.additional.MyCustomEdeltaThatCannotBeLoadedAtRuntime;
import edelta.tests.injectors.EdeltaInjectorProviderForJavaReflectAccess;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderForJavaReflectAccess.class)
public class EdeltaInterpreterHelperTest extends EdeltaAbstractTest {
	public static class InstantiateExceptionClass {
		public InstantiateExceptionClass() throws InstantiationException {
			throw new InstantiationException();
		}
	}

	@Inject
	private EdeltaInterpreterHelper interpreterHelper;

	@Inject
	private JavaReflectAccess javaReflectAccess;

	private EdeltaRuntime other;

	@Before
	public void setup() {
		other = new EdeltaDefaultRuntime(new EdeltaModelManager());
	}

	@Test
	public void testSafeInstantiateOfValidUseAs() throws Exception {
		var useAsClause = parseHelper.parse(
			"import edelta.tests.additional.MyCustomEdelta\n"
			+ "\n"
			+ "use MyCustomEdelta as my")
			.getUseAsClauses().get(0);
		assertEquals(MyCustomEdelta.class,
			interpreterHelper
				.safeInstantiate(javaReflectAccess, useAsClause, other).getClass());
	}

	@Test
	public void testSafeInstantiateOfUseAsWithoutType() throws Exception {
		var useAsClause = parseHelper.parse("use as my")
				.getUseAsClauses().get(0);
		assertThat(interpreterHelper
			.safeInstantiate(javaReflectAccess, useAsClause, other).getClass())
				.isNotNull();
	}

	@Test
	public void testSafeInstantiateOfValidUseAsWithoutType() throws Exception {
		var useAsClause = parseHelper.parse(
			"import edelta.tests.EdeltaInterpreterHelperTest.InstantiateExceptionClass\n"
			+ "use InstantiateExceptionClass as my")
				.getUseAsClauses().get(0);
		assertThat(interpreterHelper
			.safeInstantiate(javaReflectAccess, useAsClause, other).getClass())
				.isNotNull();
	}

	@Test
	public void testSafeInstantiateOfUnresolvedUseAsType() throws Exception {
		var useAsClause = parseHelper.parse("use NonExistent as my")
				.getUseAsClauses().get(0);
		assertThatThrownBy(() -> {
			interpreterHelper
				.safeInstantiate(javaReflectAccess, useAsClause, other);
		}).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Cannot resolve proxy");
	}

	@Test
	public void testSafeInstantiateOfValidUseAsButNotFoundAtRuntime() throws Exception {
		// this is a simulation of what would happen if a type is resolved
		// but the interpreter cannot load it with Class.forName
		// because the ClassLoader cannot find it
		// https://github.com/LorenzoBettini/edelta/issues/69
		var useAsClause = parseHelper.parse(
			"import edelta.tests.additional.MyCustomEdeltaThatCannotBeLoadedAtRuntime\n"
					+ "\n"
					+ "use MyCustomEdeltaThatCannotBeLoadedAtRuntime as my")
			.getUseAsClauses().get(0);
		assertThatThrownBy(() -> {
			interpreterHelper
				.safeInstantiate(javaReflectAccess, useAsClause, other);
		})
		.isInstanceOf(EdeltaInterpreterRuntimeException.class)
		.hasMessageContaining(
			"The type \'"+
			MyCustomEdeltaThatCannotBeLoadedAtRuntime.class.getName() +
			"\' has been resolved but cannot be loaded by the interpreter");
	}

	@Test
	public void testFilterOperationsWithNullEPackage() throws Exception {
		var prog = parseHelper.parse(
			"modifyEcore first epackage {}\n"
			+ "modifyEcore second epackage foo {}");
		assertThat(
			interpreterHelper.filterOperations(prog.getModifyEcoreOperations()))
				.containsExactly(last(prog.getModifyEcoreOperations()));
	}

	@Test
	public void testFilterOperationsWithSubPackage() throws Exception {
		var prog = parseWithTestEcoreWithSubPackage(
			"metamodel \"mainpackage.mainsubpackage\"\n"
			+ "\n"
			+ "modifyEcore aTest epackage mainsubpackage {\n"
			+ "	\n"
			+ "}");
		assertThat(
			interpreterHelper
				.filterOperations(prog.getModifyEcoreOperations())).isEmpty();
	}
}
