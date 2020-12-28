package edelta.twopackages.example.tests

import edelta.testutils.EdeltaTestUtils
import edelta.twopackages.example.TwoPackagesExample
import org.junit.Test

class TwoPackagesExampleTest {

	@Test
	def void testTwoPackages() {
		// Create an instance of the generated Java class
		val edelta = new TwoPackagesExample();
		// Make sure you load all the used Ecores
		edelta.loadEcoreFile("model/Person.ecore");
		edelta.loadEcoreFile("model/WorkPlace.ecore");
		// Execute the actual transformations defined in the DSL
		edelta.execute();
		// Save the modified Ecore model into a new path
		edelta.saveModifiedEcores("modified");

		EdeltaTestUtils.assertFileContents(
			"modified/Person.ecore",
			'''
			<?xml version="1.0" encoding="UTF-8"?>
			<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="person" nsURI="http://my.person.org" nsPrefix="person">
			  <eClassifiers xsi:type="ecore:EClass" name="Person">
			    <eStructuralFeatures xsi:type="ecore:EAttribute" name="firstname" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
			    <eStructuralFeatures xsi:type="ecore:EAttribute" name="lastname" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
			    <eStructuralFeatures xsi:type="ecore:EReference" name="renamedWorks" lowerBound="1"
			        eType="ecore:EClass WorkPlace.ecore#//WorkPlace" eOpposite="WorkPlace.ecore#//WorkPlace/renamedPersons"/>
			  </eClassifiers>
			</ecore:EPackage>
			'''
		)
		EdeltaTestUtils.assertFileContents(
			"modified/WorkPlace.ecore",
			'''
			<?xml version="1.0" encoding="UTF-8"?>
			<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="workplace" nsURI="http://my.workplace.org" nsPrefix="workplace">
			  <eClassifiers xsi:type="ecore:EClass" name="WorkPlace">
			    <eStructuralFeatures xsi:type="ecore:EReference" name="renamedPersons" upperBound="-1"
			        eType="ecore:EClass Person.ecore#//Person" eOpposite="Person.ecore#//Person/renamedWorks"/>
			    <eStructuralFeatures xsi:type="ecore:EAttribute" name="address" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
			  </eClassifiers>
			</ecore:EPackage>
			'''
		)
	}
}
