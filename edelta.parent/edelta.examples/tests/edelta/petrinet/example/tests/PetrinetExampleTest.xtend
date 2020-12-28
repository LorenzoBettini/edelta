package edelta.petrinet.example.tests

import edelta.petrinet.example.PetrinetExample
import edelta.testutils.EdeltaTestUtils
import org.junit.Test

class PetrinetExampleTest {

	@Test
	def void testPetrinet() {
		// Create an instance of the generated Java class
		val edelta = new PetrinetExample();
		// Make sure you load all the used Ecores
		edelta.loadEcoreFile("model/Petrinet.ecore");
		// Execute the actual transformations defined in the DSL
		edelta.execute();
		// Save the modified Ecore model into a new path
		edelta.saveModifiedEcores("modified");

		EdeltaTestUtils.assertFileContents(
			"modified/Petrinet.ecore",
			'''
				<?xml version="1.0" encoding="UTF-8"?>
				<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="petrinet" nsURI="http://gssi.it/petrinet" nsPrefix="petrinet">
				  <eClassifiers xsi:type="ecore:EClass" name="NamedElement">
				    <eStructuralFeatures xsi:type="ecore:EAttribute" name="name" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
				  </eClassifiers>
				  <eClassifiers xsi:type="ecore:EClass" name="Petrinet" eSuperTypes="#//NamedElement">
				    <eStructuralFeatures xsi:type="ecore:EReference" name="places" lowerBound="1"
				        upperBound="-1" eType="#//Place" containment="true" eOpposite="#//Place/net"/>
				    <eStructuralFeatures xsi:type="ecore:EReference" name="transitions" lowerBound="1"
				        upperBound="-1" eType="#//Transition" containment="true" eOpposite="#//Transition/net"/>
				  </eClassifiers>
				  <eClassifiers xsi:type="ecore:EClass" name="Place" eSuperTypes="#//NamedElement">
				    <eStructuralFeatures xsi:type="ecore:EReference" name="net" lowerBound="1" eType="#//Petrinet"
				        eOpposite="#//Petrinet/places"/>
				    <eStructuralFeatures xsi:type="ecore:EReference" name="out" upperBound="-1" eType="#//PTArc"
				        containment="true" eOpposite="#//PTArc/src"/>
				    <eStructuralFeatures xsi:type="ecore:EReference" name="in" upperBound="-1" eType="#//TPArc"
				        eOpposite="#//TPArc/dst"/>
				  </eClassifiers>
				  <eClassifiers xsi:type="ecore:EClass" name="Transition" eSuperTypes="#//NamedElement">
				    <eStructuralFeatures xsi:type="ecore:EReference" name="net" lowerBound="1" eType="#//Petrinet"
				        eOpposite="#//Petrinet/transitions"/>
				    <eStructuralFeatures xsi:type="ecore:EReference" name="in" upperBound="-1" eType="#//PTArc"
				        eOpposite="#//PTArc/dst"/>
				    <eStructuralFeatures xsi:type="ecore:EReference" name="out" upperBound="-1" eType="#//TPArc"
				        containment="true" eOpposite="#//TPArc/src"/>
				  </eClassifiers>
				  <eClassifiers xsi:type="ecore:EClass" name="PTArc" eSuperTypes="#//Arc">
				    <eStructuralFeatures xsi:type="ecore:EReference" name="dst" lowerBound="1" eType="#//Transition"
				        eOpposite="#//Transition/in"/>
				    <eStructuralFeatures xsi:type="ecore:EReference" name="src" lowerBound="1" eType="#//Place"
				        eOpposite="#//Place/out"/>
				  </eClassifiers>
				  <eClassifiers xsi:type="ecore:EClass" name="TPArc" eSuperTypes="#//Arc">
				    <eStructuralFeatures xsi:type="ecore:EReference" name="dst" lowerBound="1" eType="#//Place"
				        eOpposite="#//Place/in"/>
				    <eStructuralFeatures xsi:type="ecore:EReference" name="src" lowerBound="1" eType="#//Transition"
				        eOpposite="#//Transition/out"/>
				  </eClassifiers>
				  <eClassifiers xsi:type="ecore:EClass" name="Arc" abstract="true">
				    <eStructuralFeatures xsi:type="ecore:EAttribute" name="weight" lowerBound="1"
				        eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EInt"/>
				  </eClassifiers>
				</ecore:EPackage>
			'''
		)
	}
}
