package edelta.tests

import com.google.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.testing.formatter.FormatterTestHelper
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaFormatterTest extends EdeltaAbstractTest {

	@Inject extension FormatterTestHelper

	@Test
	def void testFormatting() {
		assertFormatted[
			expectation = '''
				
				
				import gssi.refactorings.MMrefactorings
				
				// IMPORTANT: ecores must be in source directories
				// otherwise you can't refer to them
				metamodel "ecore"
				metamodel "myexample"
				metamodel "myecore"
				
				use Example as example 
				use MMrefactorings as std
				
				def createClass2(String name) {
					newEClass(name)
				}
				
				def createClass3() {
					val a = newEAttribute(attrname) [
						lowerBound = 1
					]
				}
				
				example.createClass("Foo")
				
				createEClass ANewClass in myecore {
					std.addMandatoryAttr("name", ecoreref(EString), it)
				}
				
				
			'''
			toBeFormatted = '''
				
				
				import gssi.refactorings.MMrefactorings
				
				// IMPORTANT: ecores must be in source directories
				// otherwise you can't refer to them
				
				metamodel "ecore"
				metamodel "myexample"
				metamodel "myecore"
				
				use Example as example 
				use MMrefactorings as std
				
				def createClass2(String name) { 				newEClass(name)
				}
				
				def createClass3() {
					val a= newEAttribute(attrname)[
							lowerBound=1
						]
				}
				
				example.createClass("Foo")
				
				createEClass ANewClass in myecore {
					std.addMandatoryAttr("name", ecoreref(EString), it)
				}
				
				
			'''
		]
	}
}
