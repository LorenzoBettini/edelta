package edelta.tests

import com.google.inject.Inject
import org.eclipse.xtext.resource.ILocationInFileProvider
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*
import org.eclipse.xtext.util.ITextRegion

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaLocationInFileProviderTest extends EdeltaAbstractTest {

	@Inject extension ILocationInFileProvider

	@Test
	def void testCreatedEClass() {
		val input = '''
		package test
		
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			addNewEClass("NewClass")
		}
		'''
		val program = input.parseWithTestEcore
		val e = program.lastModifyEcoreOperation.body.blockLastExpression
		val derived = program.lastCopiedEPackageLastEClass
		val originalTextRegion = getSignificantTextRegion(e)
		val derivedTextRegion = getSignificantTextRegion(derived)
		// the derived EClass is mapped to the original creation expression
		assertEquals(originalTextRegion, derivedTextRegion)
	}

	@Test
	def void testOriginalEClass() {
		val input = '''
		package test
		
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
		}
		'''
		val program = input.parseWithTestEcore
		val derived = program.lastCopiedEPackageFirstEClass
		val originalTextRegion = getSignificantTextRegion(derived)
		// the original EClass is not mapped
		assertSame(ITextRegion.EMPTY_REGION, originalTextRegion)
	}

}
