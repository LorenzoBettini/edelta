package edelta.introducingdep.example.tests

import edelta.introducingdep.example.IntroducingDepModifExample
import edelta.testutils.EdeltaTestUtils
import org.junit.Test

class IntroducingDepExampleTest {

	@Test
	def void testIntroducingDep() {
		// Create an instance of the generated Java class
		val edelta = new IntroducingDepModifExample();
		// Make sure you load all the used Ecores
		edelta.loadEcoreFile("model/AnotherSimple.ecore");
		edelta.loadEcoreFile("model/Simple.ecore");
		// Execute the actual transformations defined in the DSL
		edelta.execute();
		// Save the modified Ecore model into a new path
		edelta.saveModifiedEcores("modified");

		EdeltaTestUtils.assertFileContents(
			"modified/Simple.ecore",
			'''
				<?xml version="1.0" encoding="UTF-8"?>
				<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="simple" nsURI="http://www.simple" nsPrefix="simple">
				  <eClassifiers xsi:type="ecore:EClass" name="SimpleClass">
				    <eStructuralFeatures xsi:type="ecore:EReference" name="aReferenceToAnotherSimpleClass"
				        eType="ecore:EClass AnotherSimple.ecore#//AnotherSimpleClass" eOpposite="AnotherSimple.ecore#//AnotherSimpleClass/aReferenceToSimpleClass"/>
				  </eClassifiers>
				</ecore:EPackage>
			'''
		)
		EdeltaTestUtils.assertFileContents(
			"modified/AnotherSimple.ecore",
			'''
				<?xml version="1.0" encoding="UTF-8"?>
				<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="anothersimple" nsURI="http://www.anothersimple" nsPrefix="anothersimple">
				  <eClassifiers xsi:type="ecore:EClass" name="AnotherSimpleClass" eSuperTypes="Simple.ecore#//SimpleClass">
				    <eStructuralFeatures xsi:type="ecore:EReference" name="aReferenceToSimpleClass"
				        eType="ecore:EClass Simple.ecore#//SimpleClass" eOpposite="Simple.ecore#//SimpleClass/aReferenceToAnotherSimpleClass"/>
				  </eClassifiers>
				</ecore:EPackage>
			'''
		)
	}
}
