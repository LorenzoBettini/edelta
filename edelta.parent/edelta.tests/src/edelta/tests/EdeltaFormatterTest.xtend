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
			toBeFormatted = '''
				
				
				import edelta.refactorings.lib.EdeltaRefactorings
				
				// IMPORTANT: ecores must be in source directories
				// otherwise you can't refer to them
				
				package    my.code
				
				metamodel   "ecore"
				metamodel  "myexample"
				metamodel "myecore"
				
				use  Example  as  example 
				use EdeltaRefactorings as  extension  std
				
				def  createClass2 ( String  name  , int  i  ) { 				newEClass(name)
				}
				
				
				def createClass3()    :   String  {
					val a= newEAttribute(attrname)[
							lowerBound=1
						]
				}
				
				modifyEcore   aModification   epackage   myecore    {
					std . addMandatoryAttribute( "name" , 
					ecoreref(EString), it)
					ecoreref (  ecore  .  EString  )  
					ecoreref (    EString  )
					
					val a= newEAttribute(attrname)[
												lowerBound=1
											]
					
					
				}
				
				
			'''
			expectation = '''
				
				import edelta.refactorings.lib.EdeltaRefactorings
				
				// IMPORTANT: ecores must be in source directories
				// otherwise you can't refer to them
				package my.code
				
				metamodel "ecore"
				metamodel "myexample"
				metamodel "myecore"
				
				use Example as example
				use EdeltaRefactorings as extension std
				
				def createClass2(String name, int i) {
					newEClass(name)
				}
				
				def createClass3() : String {
					val a = newEAttribute(attrname) [
						lowerBound = 1
					]
				}
				
				modifyEcore aModification epackage myecore {
					std.addMandatoryAttribute("name", ecoreref(EString), it)
					ecoreref(ecore.EString)
					ecoreref(EString)
				
					val a = newEAttribute(attrname) [
						lowerBound = 1
					]
				
				}
			'''
		]
	}
}
