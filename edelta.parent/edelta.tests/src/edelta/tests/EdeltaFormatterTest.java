package edelta.tests;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.formatter.FormatterTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import edelta.tests.injectors.EdeltaInjectorProviderCustom;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
public class EdeltaFormatterTest extends EdeltaAbstractTest {
	@Inject
	private FormatterTestHelper formatterTestHelper;

	/**
	 * Since we use text blocks, which do not contain "\r" in Windows,
	 * we have to make sure that line separators are the ones of the
	 * OS, otherwise we get failures due to some "\r" in Windows, which
	 * would not match the expectations.
	 */
	@Test
	public void testFormatting() {
		this.formatterTestHelper.assertFormatted(it -> {
			it.setToBeFormatted("""
			
			
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
			
			
			""".replace("\n", System.lineSeparator()));
			it.setExpectation(
			"""
			
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
			""".replace("\n", System.lineSeparator()));
		});
	}
}
